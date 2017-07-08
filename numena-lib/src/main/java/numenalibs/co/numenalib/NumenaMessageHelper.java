package numenalibs.co.numenalib;


import android.util.Log;

import com.google.protobuf.InvalidProtocolBufferException;

import java.util.ArrayList;
import java.util.List;

import messages.Basemessage.BaseMessage;
import messages.Clienthello.ClientHello;
import messages.Ledgerinterface.LedgerInterface;
import messages.Serverhello.ServerHello;
import messages.Statusmessage.StatusMessage;
import numenalibs.co.numenalib.encryption.EncryptionManager;
import numenalibs.co.numenalib.exceptions.NumenaLibraryException;
import numenalibs.co.numenalib.interfaces.ResultsListener;
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
    private boolean connectionEstablished = false;
    public static boolean isLocked = false;
    public boolean initiatingCall = true;


    public NumenaMessageHelper(EncryptionManager encryptionManager, ProtocolManager protocolManager, SingleMessageManager singleMessageManager) {
        this.encryptionManager = encryptionManager;
        this.protocolManager = protocolManager;
        this.singleMessageManager = singleMessageManager;
        STATE = Constants.EXPECTING_SERVERHELLO;
    }

    public void initConnection(ResultsListener<NumenaResponse> clientListener) {
        MessageCallback messageCallback = new MessageCallback();
        messageCallback.setListener(clientListener);
        ResultsListener listener = setupListenerAndCallback(messageCallback);
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
                byte[] decryptedMsg = encryptionManager.decrypt_message(msg);
                BaseMessage basemessage = BaseMessage.parseFrom(decryptedMsg);
                BaseMessage.Type msgtype = basemessage.getType();
                NumenaResponse numenaResponse = new NumenaResponse();
                switch (msgtype) {
                    case STATUS:
                        Log.d("TYPE IS", "STATUS");
                        handleStatusMessage(basemessage, numenaResponse);
                        break;
                    case SUBSCRIBE:
                        Log.d("TYPE IS", "SUBSCRIBE");
                        break;
                    case DATABASE:
                        Log.d("TYPE IS", "DATABASE");
                        break;
                    case LEDGER:
                        Log.d("TYPE IS", "LEDGER");
                        handleLedgerMessage(basemessage, numenaResponse);
                        break;
                    case ACK:
                        Log.d("TYPE IS", "ACK");
                        break;
                    default:
                        break;
                }

                incrementNonces();
                isLocked = false;
                if (!initiatingCall) {
                    BroadCaster.getBroadCaster().broadcastToObservers(Constants.EXECUTEWORKERTHREAD);
                } else {
                    initiatingCall = false;
                }
                listener.onCompletion(numenaResponse);
            }
        } catch (InvalidProtocolBufferException e) {
            throw new NumenaLibraryException("Failed: Could not parse decrypted message as Basemessage");
        }
    }

    /***************************************************************************
     * METHODS FOR HANDLING INCOMING MESSAGES
     * *************************************************************************
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

    private void handleStatusMessage(BaseMessage baseMessage, NumenaResponse numenaResponse) {
        StatusMessage status = baseMessage.getStatus();
        long code = status.getStatusCode();
        if (code == 1) {
            connectionEstablished = true;
            numenaResponse.setStatus(Constants.RESPONSE_SUCCESS);
        } else {
            numenaResponse.setStatus(Constants.RESPONSE_FAILURE);
        }
    }

    private void handleServerHelloMessage(byte[] result, final ResultsListener<NumenaResponse> clientlistener) {
        ServerHello serverHello = null;
        try {
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

    public ServerHello buildServerHello(byte[] msg) throws NumenaLibraryException {
        ServerHello serverHello = protocolManager.extractServerHello(msg);
        ServerHello.Handshake handshake = serverHello.getHandshake();
        encryptionManager.verifyServerhello(serverHello, handshake);
        return serverHello;
    }

    public BaseMessage buildClientHello(ServerHello serverHello) throws NumenaLibraryException {
        ValuesManager valuesManager = ValuesManager.getInstance();
        protocolManager.createClientConnectionKeys();
        ClientHello.Handshake handshake = protocolManager.buildClientHelloHandshake(serverHello);
        byte[] dstSignature = null;
        if (valuesManager.isConnectionToOrganizationServer()) {
            dstSignature = encryptionManager.makeHandshakeSignature(handshake, valuesManager.getClientConnectionPublicKey());
        }
        ClientHello.SignedHandshake signedHandshake = protocolManager.buildSignedHandshake(handshake, dstSignature);
        byte[] cipherText = encryptionManager.encryptCipherText(signedHandshake);
        BaseMessage baseMessage = protocolManager.packClientHello(cipherText);
        return baseMessage;
    }

    public void buildAndSendRegister(byte[] publicKey, byte[] secretKey, final String title, byte[] organisationId, byte[] appData, ResultsListener<NumenaResponse> clientlistener) {
        LedgerInterface.UserEvent userEvent = createUserEvent(publicKey, secretKey, title, organisationId, appData);
        BaseMessage baseMessage = protocolManager.register(userEvent);
        final ResultsListener listener = createNewListener(clientlistener);
        singleMessageManager.setListener(listener);
        sendBaseMessage(baseMessage);
    }

    public void buildAndSendUnRegister(byte[] publicKey, byte[] secretKey, final String title, final byte[] organisationId, final byte[] appData, ResultsListener<NumenaResponse> clientlistener) {
        LedgerInterface.UserEvent userEvent = createUserEvent(publicKey, secretKey, title, organisationId, appData);
        BaseMessage baseMessage = protocolManager.unregister(userEvent);
        final ResultsListener listener = createNewListener(clientlistener);
        singleMessageManager.setListener(listener);
        sendBaseMessage(baseMessage);
    }

    private ResultsListener createNewListener(ResultsListener<NumenaResponse> clientlistener) {
        MessageCallback messageCallback = new MessageCallback();
        messageCallback.setListener(clientlistener);
        ResultsListener listener = setupListenerAndCallback(messageCallback);
        return listener;
    }

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

    public void buildAndSendGetUsers(String query, ResultsListener<NumenaResponse> clientlistener) {
        ValuesManager valuesManager = ValuesManager.getInstance();
        BaseMessage baseMessage = protocolManager.getUsers(query, valuesManager.getOrganisationId());
        ResultsListener listener = createNewListener(clientlistener);
        singleMessageManager.setListener(listener);
        sendBaseMessage(baseMessage);
    }

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
        return connectionEstablished;
    }

    public void setConnectionEstablished(boolean connectionEstablished) {
        this.connectionEstablished = connectionEstablished;
    }

    private void incrementNonces() {
        ValuesManager valuesManager = ValuesManager.getInstance();
        int currentLocalNonce = valuesManager.getLocalNonce();
        int currentRemoteNonce = valuesManager.getRemoteNonce();
        valuesManager.setLocalNonce(currentLocalNonce + 2);
        valuesManager.setRemoteNonce(currentRemoteNonce + 2);
    }

    /**
     * Takes a callback and makes the resultlistener call the it upon success.
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
