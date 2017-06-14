package numenalibs.co.numenalib.protocol;


import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import org.libsodium.jni.Sodium;
import org.libsodium.jni.SodiumConstants;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.TimeZone;

import messages.Basemessage;
import messages.Basemessage.BaseMessage;
import messages.Clienthello;
import messages.Serverhello.ServerHello;
import messages.Serverhello.ServerHello.Handshake;
import numenalibs.co.numenalib.exceptions.NumenaLibraryException;
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

//    public Basemessage.BaseMessage packClientHello(byte[] srv_bytes, boolean isConnectionToOrganizationServer) throws packClientHelloException, NoSuchAlgorithmException, InvalidProtocolBufferException {
//
//        // 7. Generate a ClientHello.Handshake message and populate the fields
//        Clienthello.ClientHello.Handshake.Builder client_handshake_builder = Clienthello.ClientHello.Handshake.newBuilder();
//        // 7.1 Message type
//        client_handshake_builder.setMessageType("ClientHello.Handshake");
//        // 7.2 client_connection_public_key
//
//        byte[] tempclient_connection_pk = new byte[Sodium.crypto_box_publickeybytes()];
//        byte[] tempclient_connection_sk = new byte[Sodium.crypto_box_secretkeybytes()];
//        if (crypto_box_keypair(tempclient_connection_pk, tempclient_connection_sk) != 0) {
//            throw new packClientHelloException();
//        }
//        updateClientConnectionKeys(tempclient_connection_pk, tempclient_connection_sk);
//        ByteString client_connection_pk = ByteString.copyFrom(getAuthentication().getClientConnectionKeys().getPublickey());
//
//        client_handshake_builder.setClientConnectionPublicKey(client_connection_pk);
//        // 7.3 Hashed server hello
//        MessageDigest md = MessageDigest.getInstance("SHA-256");
//        byte[] srvHello = Basemessage.BaseMessage.parseFrom(srv_bytes).getServerHello().toByteArray();
//        md.update(srvHello);
//        ByteString hashed_srv_bytes = ByteString.copyFrom(md.digest());
//        client_handshake_builder.setHashedServerHello(hashed_srv_bytes);
//        // 7.4 Length of server hello
//        client_handshake_builder.setLengthOfServerHello(srvHello.length);
//        // 8. Create a ClientHello.SignedHandshake
//        Clienthello.ClientHello.SignedHandshake.Builder signed_handshake_builder = Clienthello.ClientHello.SignedHandshake.newBuilder();
//        // 8.1 Set handshake
//        Clienthello.ClientHello.Handshake client_handshake = client_handshake_builder.build();
//        signed_handshake_builder.setHandshake(client_handshake);
//        byte[] dst_signature = new byte[SodiumConstants.SIGNATURE_BYTES];
//        int[] dst_signature_length = new int[dst_signature.length];
//        // 8.2 Check if we're communicating with organization server
//        if (isConnectionToOrganizationServer) {
//            // 8.2.1 Set identity_public_key to device_identity_key
//            signed_handshake_builder.setIdentityPublicKey(ByteString.copyFrom(getAuthentication().getClientIdentityKeys().getPublickey()));
//            if (crypto_sign_detached(
//                    dst_signature,
//                    dst_signature_length,
//                    client_handshake.toByteArray(),
//                    client_handshake.toByteArray().length,
//                    getAuthentication().getClientConnectionKeys().getPublickey()) != 0) {
//                throw new packClientHelloException();
//            }
//            // 8.2.1 Set handshake signature
//            signed_handshake_builder.setHandshakeSignature(ByteString.copyFrom(dst_signature));
//        }
//        Clienthello.ClientHello.SignedHandshake signedHandshake = signed_handshake_builder.build();
//        byte[] MESSAGE = signedHandshake.toByteArray();
//        int MESSAGE_LEN = signedHandshake.getSerializedSize();
//        int CIPHERTEXT_LEN = crypto_box_MACBYTES + MESSAGE_LEN;
//        byte[] ciphertext = new byte[CIPHERTEXT_LEN];
//        Clienthello.ClientHello.Builder client_hello_builder = Clienthello.ClientHello.newBuilder();
//        // 8.3 Set client connection public key
//        client_hello_builder.setClientConnectionPublicKey(ByteString.copyFrom(getAuthentication().getClientConnectionKeys().getPublickey()));
//        // 8.4 Set encrypted handshake
//        byte[] nonce = new byte[SodiumConstants.NONCE_BYTES];
//        updateSrvConnectionPK(Basemessage.BaseMessage.parseFrom(srv_bytes).getServerHello()
//                .getHandshake().getServerConnectionPublicKey().toByteArray());
//        /*
//         Remember! When testing locally, change srv_connection_pk to client_connection_pk.
//         crypto_box_easy(ciphertext, MESSAGE, MESSAGE_LEN, nonce,
//                    bob_publickey, alice_secretkey).
//         Bob is the server. Alice is us.
//          */
//        if (crypto_box_easy(
//                ciphertext,
//                MESSAGE,
//                MESSAGE_LEN,
//                nonce,
//                getAuthentication().getServerConnectionKeys().getPublickey(),
//                getAuthentication().getClientConnectionKeys().getSecretkey()
//        ) != 0) {
//            throw new packClientHelloException();
//        }
//        client_hello_builder.setEncryptedHandshake(ByteString.copyFrom(ciphertext));
//        Clienthello.ClientHello client_hello = client_hello_builder.build();
//        Basemessage.BaseMessage.Builder basemessage_builder = Basemessage.BaseMessage.newBuilder();
//        basemessage_builder.setType(Basemessage.BaseMessage.Type.CLIENTHELLO);
//        basemessage_builder.setClientHello(client_hello);
//        return basemessage_builder.build();
//    }
}
