package numenalibs.co.numenalib.protocol;


import android.util.Log;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import org.json.JSONException;
import org.json.JSONObject;
import org.libsodium.jni.Sodium;
import org.libsodium.jni.SodiumConstants;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.TimeZone;

import messages.Basemessage;
import messages.Basemessage.BaseMessage;
import messages.Clienthello;
import messages.Ledgerinterface;
import messages.Serverhello.ServerHello;
import messages.Serverhello.ServerHello.Handshake;
import numenalibs.co.numenalib.exceptions.NumenaLibraryException;
import numenalibs.co.numenalib.tools.Constants;
import numenalibs.co.numenalib.tools.ValuesManager;

import static org.libsodium.jni.Sodium.crypto_box_easy;
import static org.libsodium.jni.Sodium.crypto_box_keypair;
import static org.libsodium.jni.Sodium.crypto_sign_detached;

public class ProtocolManager {


    public ServerHello extractServerHello(byte[] msg) throws NumenaLibraryException {
        ServerHello serverHello = parseServerHello(msg);
        setKeysFromServerHello(serverHello);
        return serverHello;

    }

    private void setKeysFromServerHello(ServerHello srvHello) throws NumenaLibraryException{
        ValuesManager vm = ValuesManager.getInstance();
        ServerHello.Handshake handshake = srvHello.getHandshake();
        ByteString srvPubKey = handshake.getServerConnectionPublicKey();

        vm.setServerConnectionPublicKey(srvPubKey.toByteArray());
        if (!Arrays.equals(vm.getWhitelist(), srvHello.getServerOrganizationPublicKey().toByteArray())) {
            throw new NumenaLibraryException("Failing: No match in whitelist");
        }
        vm.getOrganisationKeys().put("numena", srvHello.getServerOrganizationPublicKey().toByteArray());

        /* this step makes no sense at all.
        //Verify that the server_organization_public_key belongs to the intended server
        if (!Arrays.equals(vm.getOrganisationKeys().get("numena"), srvHello.getServerOrganizationPublicKey().toByteArray())) {
            throw new NumenaLibraryException("Failing: No matching organisationkey for organisation");
        }
        */
        //Verify that the signature on the ServerHello.Handshake.server_identity_public_key is correct
        ByteString srvIdentPubKey = handshake.getServerIdentityPublicKey();
        vm.setServerIdentityPublicKey(srvIdentPubKey.toByteArray());
    }

    public ServerHello parseServerHello(byte[] msg) throws NumenaLibraryException {
        BaseMessage basemessage = null;
        ServerHello srvHello = null;
        try {
            basemessage = BaseMessage.parseFrom(msg);
            BaseMessage.Type msgtype = basemessage.getType();
            boolean isServerHello = msgtype.equals(Basemessage.BaseMessage.Type.SERVERHELLO);
            if (!isServerHello) {
                throw new NumenaLibraryException("Failing to parse Numena Serverhello. Reason: not correct msg type");
            }
            //Verify if server_organization_public_key is in the list of whitelisted organization keys
            srvHello = basemessage.getServerHello();
            return srvHello;

        } catch (InvalidProtocolBufferException e) {
            throw new NumenaLibraryException("Failing to parse message as BaseMessage");
        }
    }

    public void createClientConnectionKeys()throws NumenaLibraryException{
        byte[] tempclient_connection_pk = new byte[Sodium.crypto_box_publickeybytes()];
        byte[] tempclient_connection_sk = new byte[Sodium.crypto_box_secretkeybytes()];
        if (crypto_box_keypair(tempclient_connection_pk, tempclient_connection_sk) != 0) {
            throw new NumenaLibraryException("Failing to create client connection keypair");
        }
        ValuesManager vm = ValuesManager.getInstance();
        vm.setClientConnectionPublicKey(tempclient_connection_pk);
        vm.setClientConnectionSecretKey(tempclient_connection_sk);
    }

    public Clienthello.ClientHello.Handshake buildClientHelloHandshake(ServerHello serverHello) throws NumenaLibraryException{
        //Generate a ClientHello.Handshake message and populate the fields
        ValuesManager vm = ValuesManager.getInstance();
        Clienthello.ClientHello.Handshake.Builder clientHandshakeBuilder = Clienthello.ClientHello.Handshake.newBuilder();
        //Message type
        clientHandshakeBuilder.setMessageType(Constants.MESSAGETYPE_CLIENTHELLO_HANDSHAKE);
        //client_connection_public_key
        ByteString clientConnectionPk = ByteString.copyFrom(vm.getClientConnectionPublicKey());
        clientHandshakeBuilder.setClientConnectionPublicKey(clientConnectionPk);
        //Hashed server hello
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance(Constants.SHA256_ENCODING);
        } catch (NoSuchAlgorithmException e) {
            throw new NumenaLibraryException("Failing: Cannot get SHA-256 Encoding");
        }
        byte[] srvHello = serverHello.toByteArray();
        md.update(srvHello);
        ByteString hashedSrvBytes = ByteString.copyFrom(md.digest());
        clientHandshakeBuilder.setHashedServerHello(hashedSrvBytes);
        //Length of server hello
        clientHandshakeBuilder.setLengthOfServerHello(srvHello.length);
        Clienthello.ClientHello.Handshake clientHandshake = clientHandshakeBuilder.build();
        return  clientHandshake;
    }

    public Clienthello.ClientHello.SignedHandshake buildSignedHandshake(Clienthello.ClientHello.Handshake clientHandshake, byte[] inputDstSignature){
        Clienthello.ClientHello.SignedHandshake.Builder signedHandshakeBuilder = Clienthello.ClientHello.SignedHandshake.newBuilder();
        signedHandshakeBuilder.setHandshake(clientHandshake);
        ValuesManager valuesManager = ValuesManager.getInstance();
        byte[] dstSignature = new byte[SodiumConstants.SIGNATURE_BYTES];
        if(inputDstSignature != null){
            dstSignature = inputDstSignature;
            signedHandshakeBuilder.setIdentityPublicKey(ByteString.copyFrom(valuesManager.getClientIdentityPublicKey()));
            signedHandshakeBuilder.setHandshakeSignature(ByteString.copyFrom(dstSignature));
        }
        // 8.2 Check if we're communicating with organization server
        Clienthello.ClientHello.SignedHandshake signedHandshake = signedHandshakeBuilder.build();
        return signedHandshake;
    }



    public Basemessage.BaseMessage packClientHello(byte[] ciphertext) throws NumenaLibraryException{
        ValuesManager valuesManager = ValuesManager.getInstance();
        Clienthello.ClientHello.Builder client_hello_builder = Clienthello.ClientHello.newBuilder();
        client_hello_builder.setClientConnectionPublicKey(ByteString.copyFrom(valuesManager.getClientConnectionPublicKey()));
        client_hello_builder.setEncryptedHandshake(ByteString.copyFrom(ciphertext));
        Clienthello.ClientHello client_hello = client_hello_builder.build();
        Basemessage.BaseMessage.Builder basemessage_builder = Basemessage.BaseMessage.newBuilder();
        basemessage_builder.setType(Basemessage.BaseMessage.Type.CLIENTHELLO);
        basemessage_builder.setClientHello(client_hello);
        return basemessage_builder.build();
    }

    public Basemessage.BaseMessage getUsers(String query, byte[] organizationId) {
        byte[] emptyKey = new byte[0];
        // User
        Ledgerinterface.LedgerInterface.User.Builder userbuilder = Ledgerinterface.LedgerInterface.User.newBuilder();
        userbuilder.setUsername(ByteString.copyFrom(query.getBytes()));
        userbuilder.setOrganization(ByteString.copyFrom(organizationId));
        userbuilder.setKey(ByteString.copyFrom(emptyKey));
        Ledgerinterface.LedgerInterface.User user = userbuilder.build();

        // Ledgerinterface
        Ledgerinterface.LedgerInterface.Builder ledger_builder = Ledgerinterface.LedgerInterface.newBuilder();
        ledger_builder.setType(Ledgerinterface.LedgerInterface.Type.GETUSER);
        ledger_builder.setGetUser(user);
        Ledgerinterface.LedgerInterface ledger = ledger_builder.build();

        // BaseMessage
        Basemessage.BaseMessage.Builder basemsg_builder = Basemessage.BaseMessage.newBuilder();
        basemsg_builder.setType(Basemessage.BaseMessage.Type.LEDGER);
        basemsg_builder.setLedger(ledger);
        Basemessage.BaseMessage basemsg = basemsg_builder.build();

        return basemsg;
    }

    public Basemessage.BaseMessage register(Ledgerinterface.LedgerInterface.UserEvent reg_user) {
        // Ledgerinterface
        Ledgerinterface.LedgerInterface.Builder ledger_builder = Ledgerinterface.LedgerInterface.newBuilder();
        ledger_builder.setType(Ledgerinterface.LedgerInterface.Type.REGISTER);
        ledger_builder.setRegisterUser(reg_user);
        Ledgerinterface.LedgerInterface ledger = ledger_builder.build();

        // BaseMessage
        Basemessage.BaseMessage.Builder basemsg_builder = Basemessage.BaseMessage.newBuilder();
        basemsg_builder.setType(Basemessage.BaseMessage.Type.LEDGER);
        basemsg_builder.setLedger(ledger);
        Basemessage.BaseMessage basemsg = basemsg_builder.build();

        return basemsg;
    }

    public Basemessage.BaseMessage unregister(Ledgerinterface.LedgerInterface.UserEvent unreg_user) {
        // LedgerInterface
        Ledgerinterface.LedgerInterface.Builder ledger_builder = Ledgerinterface.LedgerInterface.newBuilder();
        ledger_builder.setType(Ledgerinterface.LedgerInterface.Type.UNREGISTER);
        ledger_builder.setUnregisterUser(unreg_user);
        Ledgerinterface.LedgerInterface ledger = ledger_builder.build();

        // BaseMessage
        Basemessage.BaseMessage.Builder basemsg_builder = Basemessage.BaseMessage.newBuilder();
        basemsg_builder.setType(Basemessage.BaseMessage.Type.LEDGER);
        basemsg_builder.setLedger(ledger);
        Basemessage.BaseMessage basemsg = basemsg_builder.build();

        return basemsg;
    }

    public Ledgerinterface.LedgerInterface.User userProto(String title, byte[] publicKey, byte[] organizationId, byte[] appPbKey) {
        ByteString my_username = ByteString.copyFrom(title.getBytes());
        Ledgerinterface.LedgerInterface.User.Builder user_builder = Ledgerinterface.LedgerInterface.User.newBuilder();
        user_builder.setUsername(my_username);
        user_builder.setKey(ByteString.copyFrom(publicKey));
        user_builder.setOrganization(ByteString.copyFrom(organizationId));
        user_builder.setAppData(ByteString.copyFrom(appPbKey));
        Ledgerinterface.LedgerInterface.User userProto = user_builder.build();
        return userProto;
    }

    /**
     * This method returns a build containing a user ledgerinterface.
     * Usually used to gain a build to sign a message with.
     * @param user
     * @return
     */

    public Ledgerinterface.LedgerInterface.UserEvent.Builder userEventProtoBuilder(Ledgerinterface.LedgerInterface.User user) {
        // UserEvent
        Ledgerinterface.LedgerInterface.UserEvent.Builder remove_UserEvent_builder = Ledgerinterface.LedgerInterface.UserEvent.newBuilder();
        remove_UserEvent_builder.setUser(user);
        return remove_UserEvent_builder;
    }

    /**
     * Sets a signature on a builder and returns a userevent.
     * @param builder
     * @param signature
     * @return
     */

    public Ledgerinterface.LedgerInterface.UserEvent setSignatureOnUserEvent(Ledgerinterface.LedgerInterface.UserEvent.Builder builder, byte[] signature) {
        builder.setSignedMsg(ByteString.copyFrom(signature));
        return builder.build();
    }

}
