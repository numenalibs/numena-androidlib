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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import messages.Appmessage.AppMessage;
import messages.Basemessage;
import messages.Basemessage.BaseMessage;
import messages.Clienthello.ClientHello;
import messages.Databaseinterface;
import messages.Databaseinterface.DatabaseInterface;
import messages.Facademessages;
import messages.Ledgerinterface.LedgerInterface;
import messages.Databaseinterface.DatabaseInterface;
import messages.Databaseinterface.DatabaseInterface.DatabaseObject;
import messages.Databaseinterface.DatabaseInterface.DatabaseObject.Capability;
import messages.Serverhello.ServerHello;
import messages.Serverhello.ServerHello.Handshake;
import numenalibs.co.numenalib.exceptions.NumenaLibraryException;
import numenalibs.co.numenalib.models.NumenaUser;
import numenalibs.co.numenalib.tools.Constants;
import numenalibs.co.numenalib.tools.Utils;
import numenalibs.co.numenalib.tools.ValuesManager;

import static org.libsodium.jni.Sodium.crypto_box_easy;
import static org.libsodium.jni.Sodium.crypto_box_keypair;
import static org.libsodium.jni.Sodium.crypto_sign_detached;

public class ProtocolManager {

    /**
     * Uses methods to parse a serverhello and extract its' values to the ValuesManager
     * @param msg
     * @return
     * @throws NumenaLibraryException
     */


    public ServerHello extractServerHello(byte[] msg) throws NumenaLibraryException {
        ServerHello serverHello = parseServerHello(msg);
        setKeysFromServerHello(serverHello);
        return serverHello;

    }

    /**
     * Gets the values from the serverhello and puts it inside the ValuesManager
     * @param srvHello
     * @throws NumenaLibraryException
     */

    private void setKeysFromServerHello(ServerHello srvHello) throws NumenaLibraryException {
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

    /**
     * Parses a serhello from a byte array
     * @param msg
     * @return
     * @throws NumenaLibraryException
     */

    public ServerHello parseServerHello(byte[] msg) throws NumenaLibraryException {
        BaseMessage basemessage = null;
        ServerHello srvHello = null;
        try {
            basemessage = BaseMessage.parseFrom(msg);
            BaseMessage.Type msgtype = basemessage.getType();
            boolean isServerHello = msgtype.equals(BaseMessage.Type.SERVERHELLO);
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

    /**
     * Creates a pair of connection keys
     * @throws NumenaLibraryException
     */

    public void createClientConnectionKeys() throws NumenaLibraryException {
        byte[] tempclient_connection_pk = new byte[Sodium.crypto_box_publickeybytes()];
        byte[] tempclient_connection_sk = new byte[Sodium.crypto_box_secretkeybytes()];
        if (crypto_box_keypair(tempclient_connection_pk, tempclient_connection_sk) != 0) {
            throw new NumenaLibraryException("Failing to create client connection keypair");
        }
        ValuesManager vm = ValuesManager.getInstance();
        vm.setClientConnectionPublicKey(tempclient_connection_pk);
        vm.setClientConnectionSecretKey(tempclient_connection_sk);
    }

    /**
     * Creates a clientHelloHandshake from a serverhello.
     * @param serverHello
     * @return
     * @throws NumenaLibraryException
     */

    public ClientHello.Handshake buildClientHelloHandshake(ServerHello serverHello) throws NumenaLibraryException {
        //Generate a ClientHello.Handshake message and populate the fields
        ValuesManager vm = ValuesManager.getInstance();
        ClientHello.Handshake.Builder clientHandshakeBuilder = ClientHello.Handshake.newBuilder();
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
        ClientHello.Handshake clientHandshake = clientHandshakeBuilder.build();
        return clientHandshake;
    }

    /**
     * Creates a signedHandshake from input signature and clienthandshake
     * @param clientHandshake
     * @param inputDstSignature
     * @return
     */

    public ClientHello.SignedHandshake buildSignedHandshake(ClientHello.Handshake clientHandshake, byte[] inputDstSignature) {
        ClientHello.SignedHandshake.Builder signedHandshakeBuilder = ClientHello.SignedHandshake.newBuilder();
        signedHandshakeBuilder.setHandshake(clientHandshake);
        ValuesManager valuesManager = ValuesManager.getInstance();
        byte[] dstSignature = new byte[SodiumConstants.SIGNATURE_BYTES];
        if (inputDstSignature != null) {
            dstSignature = inputDstSignature;
            signedHandshakeBuilder.setIdentityPublicKey(ByteString.copyFrom(valuesManager.getClientIdentityPublicKey()));
            signedHandshakeBuilder.setHandshakeSignature(ByteString.copyFrom(dstSignature));
        }
        // Check if we're communicating with organization server
        ClientHello.SignedHandshake signedHandshake = signedHandshakeBuilder.build();
        return signedHandshake;
    }

    /**
     * Creates a baseMessage containing a clientHello
     * @param ciphertext
     * @return
     * @throws NumenaLibraryException
     */

    public BaseMessage packClientHello(byte[] ciphertext) throws NumenaLibraryException {
        ValuesManager valuesManager = ValuesManager.getInstance();
        ClientHello.Builder client_hello_builder = ClientHello.newBuilder();
        client_hello_builder.setClientConnectionPublicKey(ByteString.copyFrom(valuesManager.getClientConnectionPublicKey()));
        client_hello_builder.setEncryptedHandshake(ByteString.copyFrom(ciphertext));
        ClientHello client_hello = client_hello_builder.build();
        BaseMessage.Builder basemessage_builder = BaseMessage.newBuilder();
        basemessage_builder.setType(BaseMessage.Type.CLIENTHELLO);
        basemessage_builder.setClientHello(client_hello);
        return basemessage_builder.build();
    }

    /**
     * Creates a baseMessage containing a ledgerinterface with type GETUSER
     * @param query
     * @param organizationId
     * @return
     */

    public BaseMessage getUsers(String query, byte[] organizationId) {
        byte[] emptyKey = new byte[0];
        // User
        LedgerInterface.User.Builder userbuilder = LedgerInterface.User.newBuilder();
        userbuilder.setUsername(ByteString.copyFrom(query.getBytes()));
        userbuilder.setOrganization(ByteString.copyFrom(organizationId));
        userbuilder.setKey(ByteString.copyFrom(emptyKey));
        LedgerInterface.User user = userbuilder.build();

        // Ledgerinterface
        LedgerInterface.Builder ledger_builder = LedgerInterface.newBuilder();
        ledger_builder.setType(LedgerInterface.Type.GETUSER);
        ledger_builder.setGetUser(user);
        LedgerInterface ledger = ledger_builder.build();

        // BaseMessage
        BaseMessage.Builder basemsg_builder = BaseMessage.newBuilder();
        basemsg_builder.setType(BaseMessage.Type.LEDGER);
        basemsg_builder.setLedger(ledger);
        BaseMessage basemsg = basemsg_builder.build();

        return basemsg;
    }

    /**
     * Creates a baseMessage containing a userevent with type REGISTER
     * @param reg_user
     * @return
     */

    public BaseMessage register(LedgerInterface.UserEvent reg_user) {
        // Ledgerinterface
        LedgerInterface.Builder ledger_builder = LedgerInterface.newBuilder();
        ledger_builder.setType(LedgerInterface.Type.REGISTER);
        ledger_builder.setRegisterUser(reg_user);
        LedgerInterface ledger = ledger_builder.build();

        // BaseMessage
        BaseMessage.Builder basemsg_builder = BaseMessage.newBuilder();
        basemsg_builder.setType(BaseMessage.Type.LEDGER);
        basemsg_builder.setLedger(ledger);
        BaseMessage basemsg = basemsg_builder.build();

        return basemsg;
    }

    /**
     * Creates a baseMessage containing a userevent with type UNREGISTER
     * @param unreg_user
     * @return
     */

    public BaseMessage unregister(LedgerInterface.UserEvent unreg_user) {
        // LedgerInterface
        LedgerInterface.Builder ledger_builder = LedgerInterface.newBuilder();
        ledger_builder.setType(LedgerInterface.Type.UNREGISTER);
        ledger_builder.setUnregisterUser(unreg_user);
        LedgerInterface ledger = ledger_builder.build();

        // BaseMessage
        BaseMessage.Builder basemsg_builder = BaseMessage.newBuilder();
        basemsg_builder.setType(BaseMessage.Type.LEDGER);
        basemsg_builder.setLedger(ledger);
        BaseMessage basemsg = basemsg_builder.build();

        return basemsg;
    }

    /**
     * Creates a LedgerInterface.User from input
     * @param title
     * @param publicKey
     * @param organizationId
     * @param appData
     * @return
     */

    public LedgerInterface.User userProto(String title, byte[] publicKey, byte[] organizationId, byte[] appData) {
        ByteString my_username = ByteString.copyFrom(title.getBytes());
        LedgerInterface.User.Builder user_builder = LedgerInterface.User.newBuilder();
        user_builder.setUsername(my_username);
        user_builder.setKey(ByteString.copyFrom(publicKey));
        user_builder.setOrganization(ByteString.copyFrom(organizationId));
        user_builder.setAppData(ByteString.copyFrom(appData));
        LedgerInterface.User userProto = user_builder.build();
        return userProto;
    }

    /**
     * This method returns a build containing a user ledgerinterface.
     * Usually used to gain a build to sign a message with.
     *
     * @param user
     * @return
     */

    public LedgerInterface.UserEvent.Builder userEventProtoBuilder(LedgerInterface.User user) {
        // UserEvent
        LedgerInterface.UserEvent.Builder remove_UserEvent_builder = LedgerInterface.UserEvent.newBuilder();
        remove_UserEvent_builder.setUser(user);
        return remove_UserEvent_builder;
    }

    /**
     * Sets a signature on a builder and returns a userevent.
     *
     * @param builder
     * @param signature
     * @return
     */

    public LedgerInterface.UserEvent setSignatureOnUserEvent(LedgerInterface.UserEvent.Builder builder, byte[] signature) {
        builder.setSignedMsg(ByteString.copyFrom(signature));
        return builder.build();
    }

    /**
     * Method for generating a basemessage containing a contactevent with a type REMOVECONTACT
     *
     * @param contactEvent
     * @return
     */


    public BaseMessage removeContact(LedgerInterface.ContactEvent contactEvent) {
        // LedgerInterface
        LedgerInterface.Builder ledger_builder = LedgerInterface.newBuilder();
        ledger_builder.setType(LedgerInterface.Type.REMOVECONTACT);
        ledger_builder.setRemoveContact(contactEvent);
        LedgerInterface ledger = ledger_builder.build();

        // BaseMessage
        BaseMessage.Builder basemsg_builder = BaseMessage.newBuilder();
        basemsg_builder.setType(BaseMessage.Type.LEDGER);
        basemsg_builder.setLedger(ledger);
        BaseMessage basemsg = basemsg_builder.build();

        return basemsg;
    }

    /**
     * Method for generating a contactevent from a user to a user.
     *
     * @param encrypted_other_user
     * @param userEvent
     * @return
     */

    public LedgerInterface.ContactEvent contactEvent(byte[] encrypted_other_user, LedgerInterface.UserEvent userEvent) {
        // ContactEvent
        LedgerInterface.ContactEvent.Builder contact_event_builder = LedgerInterface.ContactEvent.newBuilder();
        contact_event_builder.setUser(userEvent);
        contact_event_builder.setContact(ByteString.copyFrom(encrypted_other_user));
        LedgerInterface.ContactEvent contact_event = contact_event_builder.build();
        return contact_event;
    }

    /**
     * Method for generating a basemessage containing a contactevent with a type ADDCONTACT
     *
     * @param contactEvent
     * @return
     */

    public BaseMessage addContact(LedgerInterface.ContactEvent contactEvent) {
        // LedgerInterface
        LedgerInterface.Builder ledger_builder = LedgerInterface.newBuilder();
        ledger_builder.setType(LedgerInterface.Type.ADDCONTACT);
        ledger_builder.setAddContact(contactEvent);
        LedgerInterface ledger = ledger_builder.build();

        // BaseMessage
        BaseMessage.Builder basemsg_builder = BaseMessage.newBuilder();
        basemsg_builder.setType(BaseMessage.Type.LEDGER);
        basemsg_builder.setLedger(ledger);
        BaseMessage basemsg = basemsg_builder.build();

        return basemsg;
    }

    /**
     * Generates a lists of capability verifiers from a list of NumenaUsers
     * @param users
     * @param writePermission
     * @param readPermission
     * @return
     */

    public List<Capability> generateVerifiers(List<NumenaUser> users, boolean writePermission, boolean readPermission) {
        // Capability
        List<Capability> verifiers = new ArrayList<>();

        for (int i = 0; i < users.size(); i++) {
            NumenaUser tempUser = users.get(i);
            byte[] publickKey = tempUser.getPublicKey();
            String userName = tempUser.getUsername();

            Capability.Builder capability_builder = Capability.newBuilder();
            capability_builder.setUsername(ByteString.copyFrom(userName.getBytes()));
            capability_builder.setWrite(writePermission);
            capability_builder.setRead(readPermission);
            ByteString key = ByteString.copyFrom(publickKey);
            capability_builder.setKey(key);
            Capability capability = capability_builder.build();
            verifiers.add(capability);
        }

        return verifiers;

    }

    /**
     * Generates a basemessage containing a databaseinterface with type STORE
     * @param verifiers
     * @param organizationId
     * @param appId
     * @param toBeSaved
     * @return
     */

    public BaseMessage storeObject(List<Capability> verifiers, byte[] organizationId, byte[] appId, byte[] toBeSaved) {
        try {
            // DatabaseObject
            DatabaseObject.Builder database_object_builder = DatabaseObject.newBuilder();

            database_object_builder.setEncryptedMessage(ByteString.copyFrom(toBeSaved));
            database_object_builder.setAppId(ByteString.copyFrom(appId));
            database_object_builder.setOrgId(ByteString.copyFrom(organizationId));
            database_object_builder.addAllVerifier(verifiers);
            database_object_builder.setTimestamp(System.currentTimeMillis());
            //database_object_builder.setExpiration();
            MessageDigest md = MessageDigest.getInstance(Constants.SHA256_ENCODING);
            md.update(toBeSaved);

            byte[] msg_hash = md.digest();
            database_object_builder.setMessageHash(ByteString.copyFrom(msg_hash));
            //database_object_builder.setPreviousMessageHash();
            DatabaseObject database_object = database_object_builder.build();

            // StoreObject
            DatabaseInterface.StoreObject.Builder store_obj_builder = DatabaseInterface.StoreObject.newBuilder();
            store_obj_builder.setObject(database_object);
            DatabaseInterface.StoreObject store_obj = store_obj_builder.build();

            // DatabaseInterface
            DatabaseInterface.Builder database_interface_builder = DatabaseInterface.newBuilder();
            database_interface_builder.setType(DatabaseInterface.Type.STORE);
            database_interface_builder.setStoreObject(store_obj);
            DatabaseInterface database_interface = database_interface_builder.build();

            // BaseMessage
            BaseMessage.Builder base_msg_builder = BaseMessage.newBuilder();
            base_msg_builder.setType(BaseMessage.Type.DATABASE);
            base_msg_builder.setDatabase(database_interface);
            BaseMessage base_msg = base_msg_builder.build();

            return base_msg;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Generates a basemessage containing a databaseinteface with type GET
     * @param own_id_pkey
     * @param appId
     * @param message_hash
     * @param limit
     * @return
     */

    public BaseMessage getObject(byte[] own_id_pkey, byte[] appId, byte[] message_hash, int limit) {
        DatabaseInterface.GetObject.Builder get_obj_builder = DatabaseInterface.GetObject.newBuilder();
        // GetObject
        get_obj_builder.setKey(ByteString.copyFrom(own_id_pkey));
        get_obj_builder.setAppId(ByteString.copyFrom(appId));
        get_obj_builder.setMessageHash(ByteString.copyFrom(message_hash));
        get_obj_builder.setLimit(limit);

        DatabaseInterface.GetObject get_obj = get_obj_builder.build();

        // DatabaseInterface
        DatabaseInterface.Builder database_interface_builder = DatabaseInterface.newBuilder();
        database_interface_builder.setType(Databaseinterface.DatabaseInterface.Type.GET);
        database_interface_builder.setGetObject(get_obj);
        DatabaseInterface database_interface = database_interface_builder.build();

        // BaseMessage
        Basemessage.BaseMessage.Builder base_msg_builder = Basemessage.BaseMessage.newBuilder();
        base_msg_builder.setType(Basemessage.BaseMessage.Type.DATABASE);
        base_msg_builder.setDatabase(database_interface);
        Basemessage.BaseMessage base_msg = base_msg_builder.build();

        return base_msg;
    }

    /**
     * Used for generating a Subscribe builder containing appid, organization and publickey
     * @param appId
     * @param organization
     * @param publicKey
     * @return
     */

    public Facademessages.Subscribe.Builder subscribeProtoBuilder(byte[] appId, byte[] organization, byte[] publicKey) {
        Facademessages.Subscribe.Builder subBuilder = Facademessages.Subscribe.newBuilder();
        subBuilder.setAppId(ByteString.copyFrom(appId));
        subBuilder.setOrgId(ByteString.copyFrom(organization));
        subBuilder.setQueue(ByteString.copyFrom(publicKey));
        subBuilder.setSignedMsg(ByteString.copyFrom(appId));
        return subBuilder;
    }

    /**
     * Sets a signature on a subscribe builder
     * @param subBuilder
     * @param signature
     * @return
     */

    public Facademessages.Subscribe setSignatureOnSubscribe(Facademessages.Subscribe.Builder subBuilder, byte[] signature) {
        subBuilder.setMsgSignature(ByteString.copyFrom(signature));
        return subBuilder.build();
    }

    /**
     * Used for generating a basemessage containing a FacadeMessage.Subscribe with type SUBSCRIBE
     * @param sub
     * @return
     */

    public BaseMessage subscribe(Facademessages.Subscribe sub) {
        BaseMessage.Builder baseBuilder = BaseMessage.newBuilder();
        baseBuilder.setType(Basemessage.BaseMessage.Type.SUBSCRIBE);
        baseBuilder.setSubscribe(sub);
        BaseMessage baseMessage = baseBuilder.build();
        return baseMessage;
    }

    /**
     * Used for generating an Appmessage
     * @param encryptedContent
     * @param signature
     * @param appPKey
     * @return
     */

    public AppMessage appMessage(byte[] encryptedContent, byte[] signature, byte[] appPKey) {
        AppMessage.Builder appmsg_builder = AppMessage.newBuilder();
        appmsg_builder.setContent(ByteString.copyFrom(encryptedContent));
        appmsg_builder.setSignature(ByteString.copyFrom(signature));
        appmsg_builder.setTemppublicKey(ByteString.copyFrom(appPKey));
        AppMessage appmsg = appmsg_builder.build();
        return appmsg;
    }

}
