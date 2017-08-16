package numenalibs.co.numenalib;


import android.util.Log;

import com.google.protobuf.InvalidProtocolBufferException;

import java.util.ArrayList;
import java.util.List;

import messages.Basemessage;
import messages.Basemessage.BaseMessage;
import messages.Clienthello.ClientHello;
import messages.Databaseinterface;
import messages.Facademessages;
import messages.Ledgerinterface.LedgerInterface;
import messages.Serverhello.ServerHello;
import messages.Statusmessage.StatusMessage;
import numenalibs.co.numenalib.encryption.EncryptionManager;
import numenalibs.co.numenalib.exceptions.NumenaLibraryException;
import numenalibs.co.numenalib.interfaces.ResultsListener;
import numenalibs.co.numenalib.interfaces.NumenaChatHandler;
import numenalibs.co.numenalib.models.NumenaMethod;
import numenalibs.co.numenalib.models.NumenaObject;
import numenalibs.co.numenalib.models.NumenaResponse;
import numenalibs.co.numenalib.models.NumenaUser;
import numenalibs.co.numenalib.networking.SingleMessageManager;
import numenalibs.co.numenalib.protocol.ProtocolManager;
import numenalibs.co.numenalib.tools.BroadCaster;
import numenalibs.co.numenalib.tools.Constants;
import numenalibs.co.numenalib.tools.ValuesManager;


public class NumenaMessageHelper {

    private EncryptionManager encryptionManager;
    private ProtocolManager protocolManager;
    private SingleMessageManager singleMessageManager;
    private static int STATE = 0;
    public static boolean isLocked = false;
    public boolean initiatingCall = true;
    private NumenaChatHandler numenaChatHandler;


    public NumenaMessageHelper(EncryptionManager encryptionManager, ProtocolManager protocolManager, SingleMessageManager singleMessageManager) {
        this.encryptionManager = encryptionManager;
        this.protocolManager = protocolManager;
        this.singleMessageManager = singleMessageManager;
        STATE = Constants.EXPECTING_SERVERHELLO;
    }


    public void resetValues(){
        STATE = Constants.EXPECTING_SERVERHELLO;
        initiatingCall = true;
        ValuesManager.getInstance().resetNonces();
    }

    /**
     * Closes the connection and resets the initial values used when opening a the websocket.
     */

    public void closeConnection(){
        resetValues();
        singleMessageManager.disconnectWebsocket();
    }

    /**
     * Initialises the connection to the current server URL
     *
     * @param clientListener
     */

    public void initConnection(ResultsListener<NumenaResponse> clientListener) {
        ResultsListener listener = createNewListener(clientListener);
        singleMessageManager.setListener(listener);
        singleMessageManager.openWebsocket();
    }

    /**
     * A method that gets a byte array as input and executes different kind of statements depending on the type
     *
     * @param msg
     * @throws NumenaLibraryException
     */

    public void handleMessage(byte[] msg, final ResultsListener<NumenaResponse> listener) throws NumenaLibraryException {
        try {
            if (STATE == Constants.EXPECTING_SERVERHELLO) {
                handleServerHelloMessage(msg, listener);
            } else if (STATE == Constants.EXPECTING_MESSAGE) {
                byte[] decryptedMsg = encryptionManager.decryptMessage(msg);
                BaseMessage basemessage = BaseMessage.parseFrom(decryptedMsg);
                BaseMessage.Type msgtype = basemessage.getType();
                NumenaResponse numenaResponse = new NumenaResponse();
                boolean reportToListener = true;
                boolean incrementLocalNonce = true;
                boolean incremenetRemoteNonce = true;
                switch (msgtype) {
                    case STATUS:
                        handleStatusMessage(basemessage, numenaResponse);
                        break;
                    case SUBSCRIBE:
                        break;
                    case DATABASE:
                        handleDatabaseMessage(basemessage,numenaResponse);
                        incrementLocalNonce = false;
                        reportToListener = false;
                        break;
                    case LEDGER:
                        handleLedgerMessage(basemessage, numenaResponse);
                        break;
                    case ACK:
                        reportToListener = false;
                        break;
                    default:
                        break;
                }

                incrementNonces(incrementLocalNonce,incremenetRemoteNonce);
                isLocked = false;
                if (!initiatingCall) {
                    BroadCaster.getBroadCaster().broadcastToObservers(Constants.EXECUTEWORKERTHREAD);
                } else {
                    initiatingCall = false;
                }
                if(reportToListener) listener.onCompletion(numenaResponse);
            }
        } catch (InvalidProtocolBufferException e) {
            throw new NumenaLibraryException("Failed: Could not parse decrypted message as Basemessage");
        }
    }

    /***************************************************************************
     * METHODS FOR HANDLING INCOMING MESSAGES
     * *************************************************************************
     */

    /**
     * Method for handler a databaseMessage
     * Needs a NumenaChatHandler, created in the GUI to execute onMessage
     * @param baseMessage
     * @param numenaResponse
     */

    private void handleDatabaseMessage(BaseMessage baseMessage, NumenaResponse numenaResponse){
        numenaResponse.setStatus(Constants.RESPONSE_SUCCESS);
        if(numenaChatHandler != null){
            Databaseinterface.DatabaseInterface databaseInterface = baseMessage.getDatabase();
            List<Databaseinterface.DatabaseInterface.DatabaseObject> objects = databaseInterface.getResponseList();
            Databaseinterface.DatabaseInterface.DatabaseObject object = objects.get(0);
            numenaChatHandler.onMessage(object.getEncryptedMessage().toByteArray());
        }
    }

    /**
     * Method for handling a ledgerMessage
     * It adds a list of NumenaObjects to the NumenaResponse
     *
     * @param baseMessage
     * @param numenaResponse
     */

    private void handleLedgerMessage(BaseMessage baseMessage, NumenaResponse numenaResponse) {
        LedgerInterface ledgerInterface = baseMessage.getLedger();
        List<LedgerInterface.User> users = ledgerInterface.getResponse().getUsersList();
        numenaResponse.setStatus(Constants.RESPONSE_SUCCESS);
        List<NumenaObject> numenaUsers = new ArrayList<>();
        for (LedgerInterface.User user : users) {
            String username = new String(user.getUsername().toByteArray());
            byte[] appData = user.getAppData().toByteArray();
            byte[] organisationId = user.getOrganization().toByteArray();
            byte[] publicKey = user.getKey().toByteArray();
            numenaUsers.add(new NumenaUser(username, appData, publicKey, organisationId));
        }
        numenaResponse.setNumenaObjects(numenaUsers);
    }

    /**
     * Method for handling a status message.
     * It sets a response type on the NumenaResponse according to the statuscode
     *
     * @param baseMessage
     * @param numenaResponse
     */

    private void handleStatusMessage(BaseMessage baseMessage, NumenaResponse numenaResponse) {
        StatusMessage status = baseMessage.getStatus();
        long code = status.getStatusCode();
        if (code == 1) {
            numenaResponse.setStatus(Constants.RESPONSE_SUCCESS);
        } else {
            numenaResponse.setStatus(Constants.RESPONSE_FAILURE);
        }
    }

    /**
     * Handles the initial msg when connection is established and returns a clienthello to the server
     *
     * @param result
     * @param clientlistener
     */

    private void handleServerHelloMessage(byte[] result, final ResultsListener<NumenaResponse> clientlistener) {
        ServerHello serverHello = null;
        try {
            initiatingCall = true;
            serverHello = buildServerHello(result);
            BaseMessage baseMessage = buildClientHello(serverHello);
            ResultsListener listener = createNewListener(clientlistener);
            singleMessageManager.setListener(listener);
            STATE = Constants.EXPECTING_MESSAGE;
            ValuesManager.getInstance().storeKeysInDatabase();
            ValuesManager.getInstance().refreshKeysFromDatabase();
            singleMessageManager.sendBinary(baseMessage.toByteArray());
        } catch (NumenaLibraryException e) {
            e.printStackTrace();
        }
    }

    /************************************************************************************
     * METHODS FOR BUILDING MESSAGES
     * *********************************************************************************
     */

    /**
     * Builds a FacadeMessage.subcribe, sets a signature and sends it.
     * @param identityPublicKey
     * @param identitySecretKey
     * @param organisationId
     * @param appId
     * @param chatHandler
     * @param clientlistener
     */

    public void buildAndSendSubscribe(byte[] identityPublicKey, byte[] identitySecretKey, byte[] organisationId, byte[] appId, NumenaChatHandler chatHandler, ResultsListener<NumenaResponse> clientlistener) {
        Facademessages.Subscribe.Builder subBuilder = protocolManager.subscribeProtoBuilder(appId, organisationId, identityPublicKey);
        byte[] signature = null;
        try {
            signature = encryptionManager.signMessage(appId, identitySecretKey);
        } catch (NumenaLibraryException e) {
            e.printStackTrace();
        }
        Facademessages.Subscribe sub = protocolManager.setSignatureOnSubscribe(subBuilder, signature);
        Basemessage.BaseMessage baseMessage = protocolManager.subscribe(sub);
        final ResultsListener listener = createNewListener(clientlistener);
        singleMessageManager.setListener(listener);
        sendBaseMessage(baseMessage);
        this.numenaChatHandler = chatHandler;
    }


    /**
     * Builds a serverhello from the bytearray msg gotten when connection is established
     *
     * @param msg
     * @return
     * @throws NumenaLibraryException
     */

    public ServerHello buildServerHello(byte[] msg) throws NumenaLibraryException {
        ServerHello serverHello = protocolManager.extractServerHello(msg);
        ServerHello.Handshake handshake = serverHello.getHandshake();
        encryptionManager.verifyServerhello(serverHello, handshake);
        return serverHello;
    }

    /**
     * Builds a client hello with the values from the last serverhello
     *
     * @param serverHello
     * @return
     * @throws NumenaLibraryException
     */

    public BaseMessage buildClientHello(ServerHello serverHello) throws NumenaLibraryException {
        ValuesManager valuesManager = ValuesManager.getInstance();
        protocolManager.createClientConnectionKeys();
        ClientHello.Handshake handshake = protocolManager.buildClientHelloHandshake(serverHello);
        byte[] dstSignature = null;
        if (valuesManager.isConnectionToOrganizationServer()) {
            dstSignature = encryptionManager.makeHandshakeSignature(handshake, valuesManager.getClientConnectionPublicKey());
        }
        ClientHello.SignedHandshake signedHandshake = protocolManager.buildSignedHandshake(handshake, dstSignature);
        byte[] cipherText = encryptionManager.encryptSignedHandshake(signedHandshake);
        BaseMessage baseMessage = protocolManager.packClientHello(cipherText);
        return baseMessage;
    }

    /**
     * Builds a basemessage containing a ledgerinterface with type REGISTER
     * and sends it
     *
     * @param publicKey
     * @param secretKey
     * @param title
     * @param organisationId
     * @param appData
     * @param clientlistener
     */

    public void buildAndSendRegister(byte[] publicKey, byte[] secretKey, final String title, byte[] organisationId, byte[] appData, ResultsListener<NumenaResponse> clientlistener) {
        LedgerInterface.UserEvent userEvent = createUserEvent(publicKey, secretKey, title, organisationId, appData);
        BaseMessage baseMessage = protocolManager.register(userEvent);
        final ResultsListener listener = createNewListener(clientlistener);
        singleMessageManager.setListener(listener);
        sendBaseMessage(baseMessage);
    }

    /**
     * Builds a contactEvent containing the other contact and own user.
     * @param self
     * @param contact
     * @return
     */

    public LedgerInterface.ContactEvent buildContactEvent(NumenaUser self, NumenaUser contact) {
        LedgerInterface.UserEvent userEvent = createUserEvent(self.getPublicKey(), self.getSecretKey(), self.getUsername(), self.getOrganisationId(), self.getAppData());
        LedgerInterface.User protoContact = protocolManager.userProto(contact.getUsername(), contact.getPublicKey(), contact.getOrganisationId(), contact.getAppData());
        byte[] encryptedContactBytes = null;
        try {
            encryptedContactBytes = encryptionManager.encryptMessage(protoContact.toByteArray(), protoContact.getSerializedSize(), 0);
        } catch (NumenaLibraryException e) {
            e.printStackTrace();
        }
        LedgerInterface.ContactEvent contactEvent = protocolManager.contactEvent(encryptedContactBytes, userEvent);
        return contactEvent;
    }

    /**
     * Builds a contact event with type REMOVE and sends it
     * @param self
     * @param contact
     * @param clientlistener
     */

    public void buildAndSendRemoveContact(NumenaUser self, NumenaUser contact, ResultsListener<NumenaResponse> clientlistener) {
        LedgerInterface.ContactEvent contactEvent = buildContactEvent(self, contact);
        BaseMessage baseMessage = protocolManager.removeContact(contactEvent);
        final ResultsListener listener = createNewListener(clientlistener);
        singleMessageManager.setListener(listener);
        sendBaseMessage(baseMessage);

    }

    /**
     * Builds a contact event with type ADD and sends it
     * @param self
     * @param contact
     * @param clientlistener
     */

    public void buildAndSendAddContact(NumenaUser self, NumenaUser contact, ResultsListener<NumenaResponse> clientlistener) {
        LedgerInterface.ContactEvent contactEvent = buildContactEvent(self, contact);
        BaseMessage baseMessage = protocolManager.addContact(contactEvent);
        final ResultsListener listener = createNewListener(clientlistener);
        singleMessageManager.setListener(listener);
        sendBaseMessage(baseMessage);

    }

    /**
     * Builds a databaseobject with type STORE. Uses the list of NumenaUsers as verifiers.
     * The READ/WRITE permission is not individual but set on all verifiers.
     * @param userList
     * @param content
     * @param organisationId
     * @param appId
     * @param writePermission
     * @param readPermission
     * @param clientlistener
     */

    public void buildAndStoreObject(List<NumenaUser> userList, byte[] content, byte[] organisationId, byte[] appId, boolean writePermission, boolean readPermission, ResultsListener<NumenaResponse> clientlistener) {
        List<Databaseinterface.DatabaseInterface.DatabaseObject.Capability> verifiers = protocolManager.generateVerifiers(userList, writePermission, readPermission);
        BaseMessage baseMessage = protocolManager.storeObject(verifiers, organisationId, appId, content);
        final ResultsListener listener = createNewListener(clientlistener);
        singleMessageManager.setListener(listener);
        sendBaseMessage(baseMessage);
    }

    /**
     * Builds a databaseObject with type GET.
     * @param publicKey
     * @param appId
     * @param messageHash
     * @param limit
     * @param clientlistener
     */

    public void buildAndGetObject(byte[] publicKey, byte[] appId, byte[] messageHash, int limit, ResultsListener<NumenaResponse> clientlistener){
        BaseMessage baseMessage = protocolManager.getObject(publicKey,appId,messageHash,limit);
        final ResultsListener listener = createNewListener(clientlistener);
        singleMessageManager.setListener(listener);
        sendBaseMessage(baseMessage);
    }

    /**
     * Builds a basemessage containing a ledgerinterface with type UNREGISTER
     * and sends it
     *
     * @param publicKey
     * @param secretKey
     * @param title
     * @param organisationId
     * @param appData
     * @param clientlistener
     */

    public void buildAndSendUnRegister(byte[] publicKey, byte[] secretKey, final String title, final byte[] organisationId, final byte[] appData, ResultsListener<NumenaResponse> clientlistener) {
        LedgerInterface.UserEvent userEvent = createUserEvent(publicKey, secretKey, title, organisationId, appData);
        BaseMessage baseMessage = protocolManager.unregister(userEvent);
        final ResultsListener listener = createNewListener(clientlistener);
        singleMessageManager.setListener(listener);
        sendBaseMessage(baseMessage);
    }

    /**
     * Creates a new listener
     *
     * @param clientlistener
     * @return
     */

    private ResultsListener createNewListener(ResultsListener<NumenaResponse> clientlistener) {
        MessageCallback messageCallback = new MessageCallback();
        messageCallback.setListener(clientlistener);
        ResultsListener listener = setupListenerAndCallback(messageCallback);
        return listener;
    }

    /**
     * Method used for creating a userEvent
     *
     * @param publicKey
     * @param secretKey
     * @param title
     * @param organisationId
     * @param appData
     * @return
     */

    private LedgerInterface.UserEvent createUserEvent(byte[] publicKey, byte[] secretKey, final String title, final byte[] organisationId, final byte[] appData) {
        byte[] signedMsg = null;
        LedgerInterface.User ownUser = protocolManager.userProto(title, publicKey, organisationId, appData);
        try {
            signedMsg = encryptionManager.signMessage(ownUser.toByteArray(), secretKey);
        } catch (NumenaLibraryException e) {
            e.printStackTrace();
        }
        LedgerInterface.UserEvent.Builder userEventBuilder = protocolManager.userEventProtoBuilder(ownUser);
        LedgerInterface.UserEvent userEvent = protocolManager.setSignatureOnUserEvent(userEventBuilder, signedMsg);
        return userEvent;
    }

    /**
     * Builds a base message containing a ledgerinterface with TYPE GETUSER
     * and sends it
     *
     * @param query
     * @param clientlistener
     */

    public void buildAndSendGetUsers(String query, byte[] organisationId, ResultsListener<NumenaResponse> clientlistener) {
        BaseMessage baseMessage = protocolManager.getUsers(query, organisationId);
        ResultsListener listener = createNewListener(clientlistener);
        singleMessageManager.setListener(listener);
        sendBaseMessage(baseMessage);
    }

    /**
     * Encrypts and sends a basemessage
     *
     * @param baseMessage
     */

    private void sendBaseMessage(BaseMessage baseMessage) {
        ValuesManager valuesManager = ValuesManager.getInstance();
        int nonce = valuesManager.getLocalNonce();
        try {
            byte[] encryptedMessage = encryptionManager.encryptMessage(baseMessage.toByteArray(), baseMessage.getSerializedSize(), nonce);
            singleMessageManager.sendBinary(encryptedMessage);
        } catch (NumenaLibraryException e) {
            e.printStackTrace();
        }
    }

    public boolean isConnectionEstablished() {
        return singleMessageManager.isWebsocketConnected();
    }

    /**
     * Incrementing nonce values, for both remote and local nonce
     */

    private void incrementNonces(boolean incrementLocalNonce, boolean incrementRemoteNonce) {
        ValuesManager valuesManager = ValuesManager.getInstance();
        int currentLocalNonce = valuesManager.getLocalNonce();
        int currentRemoteNonce = valuesManager.getRemoteNonce();
        if(incrementLocalNonce) valuesManager.setLocalNonce(currentLocalNonce + 2);
        if(incrementRemoteNonce) valuesManager.setRemoteNonce(currentRemoteNonce + 2);
    }

    /**
     * Takes a callback and makes the resultlistener call it upon success.
     *
     * @param numenaMethod
     * @return
     */

    private ResultsListener setupListenerAndCallback(final NumenaMethod numenaMethod) {
        ResultsListener openingListener = new ResultsListener<byte[]>() {
            @Override
            public void onCompletion(byte[] result) {
                try {
                    numenaMethod.setResult(result);
                    numenaMethod.call();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Throwable throwable) {

            }
        };
        return openingListener;
    }

    class MessageCallback extends NumenaMethod {

        @Override
        public Void call() {
            byte[] result = (byte[]) getResult();
            ResultsListener<NumenaResponse> listener = (ResultsListener<NumenaResponse>) getListener();
            try {
                handleMessage(result, listener);
            } catch (NumenaLibraryException e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
