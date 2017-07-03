package numenalibs.co.numenalib;


import android.util.Log;

import com.google.protobuf.InvalidProtocolBufferException;

import java.util.List;

import messages.Ackinterface;
import messages.Basemessage;
import messages.Clienthello;
import messages.Databaseinterface;
import messages.Ledgerinterface;
import messages.Serverhello;
import messages.Statusmessage;
import numenalibs.co.numenalib.encryption.EncryptionManager;
import numenalibs.co.numenalib.exceptions.NumenaLibraryException;
import numenalibs.co.numenalib.interfaces.ResultsListener;
import numenalibs.co.numenalib.models.NumenaMethod;
import numenalibs.co.numenalib.networking.SingleMessageManager;
import numenalibs.co.numenalib.protocol.ProtocolManager;
import numenalibs.co.numenalib.tools.Constants;
import numenalibs.co.numenalib.tools.ValuesManager;

public class NumenaMessageHelper {

    private EncryptionManager encryptionManager;
    private ProtocolManager protocolManager;
    private SingleMessageManager singleMessageManager;
    private static int STATE = 0;

    public NumenaMessageHelper(EncryptionManager encryptionManager, ProtocolManager protocolManager,SingleMessageManager singleMessageManager) {
        this.encryptionManager = encryptionManager;
        this.protocolManager = protocolManager;
        this.singleMessageManager = singleMessageManager;
        STATE = Constants.EXPECTING_SERVERHELLO;
    }

    public void initConnection() {
        ResultsListener listener = setupListenerAndCallback(new MessageCallback());
        singleMessageManager.setListener(listener);
        singleMessageManager.openWebsocket();
    }

    /**
     * A method that gets a byte array as input and executes different kind of statements depending on the type
     *
     * @param msg
     * @throws NumenaLibraryException
     */

    public void handleMessage(byte[] msg) throws NumenaLibraryException {
        try {
            if(STATE == Constants.EXPECTING_SERVERHELLO){
                handleServerHelloMessage(msg);
            }else if (STATE == Constants.EXPECTING_MESSAGE){
                byte[] decryptedMsg = encryptionManager.decrypt_message(msg);
                Basemessage.BaseMessage basemessage = Basemessage.BaseMessage.parseFrom(decryptedMsg);
                Basemessage.BaseMessage.Type msgtype = basemessage.getType();
                switch (msgtype) {
                    case STATUS:
                        handleStatusMessage(basemessage);
                        break;
                    case SUBSCRIBE:
                        Log.d("TYPE IS", "SUBSCRIBE");
                        break;
                    case DATABASE:
                        Log.d("TYPE IS", "DATABASE");
                        break;
                    case LEDGER:
                        Log.d("TYPE IS", "LEDGER");
                        handleLedgerMessage(basemessage);
                        break;
                    case ACK:
                        Log.d("TYPE IS", "ACK");
                        break;
                    default:
                        break;
                }
                incrementNonces();
//                ValuesManager valuesManager = ValuesManager.getInstance();
//                buildAndSendGetUsers("e");
//                buildAndSendRegister("librarytest1", valuesManager.getClientIdentityPublicKey(),valuesManager.getOrganisationId(), "TEssst".getBytes());
            }
        } catch (InvalidProtocolBufferException e) {
            throw new NumenaLibraryException("Failed: Could not parse decrypted message as Basemessage");
        }
    }

    /***************************************************************************
     * METHODS FOR HANDLING INCOMING MESSAGES
     * *************************************************************************
     */

    private void handleLedgerMessage(Basemessage.BaseMessage baseMessage){
        Ledgerinterface.LedgerInterface ledgerInterface = baseMessage.getLedger();
        List<Ledgerinterface.LedgerInterface.User> users = ledgerInterface.getResponse().getUsersList();
        for(Ledgerinterface.LedgerInterface.User user : users){
            Log.d("Found a user", new String(user.getUsername().toByteArray()));
        }
    }

    private void handleStatusMessage(Basemessage.BaseMessage baseMessage) {
        Statusmessage.StatusMessage status = baseMessage.getStatus();
        long code = status.getStatusCode();
        if(code == 1){

            Log.d("STATUS", "POSITIVE");
        }else {
            Log.d("STATUS", "NEGATIVE");
        }
    }

    private void handleServerHelloMessage(byte[] result) {
        Serverhello.ServerHello serverHello = null;
        try {
            serverHello = buildServerHello(result);
            Basemessage.BaseMessage baseMessage = buildClientHello(serverHello);
            ResultsListener listener = setupListenerAndCallback(new MessageCallback());
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

    public Serverhello.ServerHello buildServerHello(byte[] msg) throws NumenaLibraryException {
        Serverhello.ServerHello serverHello = protocolManager.extractServerHello(msg);
        Serverhello.ServerHello.Handshake handshake = serverHello.getHandshake();
        encryptionManager.verifyServerhello(serverHello, handshake);
        return serverHello;
    }

    public Basemessage.BaseMessage buildClientHello(Serverhello.ServerHello serverHello) throws NumenaLibraryException {
        ValuesManager valuesManager = ValuesManager.getInstance();
        protocolManager.createClientConnectionKeys();
        Clienthello.ClientHello.Handshake handshake = protocolManager.buildClientHelloHandshake(serverHello);
        byte[] dstSignature = null;
        if (valuesManager.isConnectionToOrganizationServer()) {
            dstSignature = encryptionManager.makeHandshakeSignature(handshake, valuesManager.getClientConnectionPublicKey());
        }
        Clienthello.ClientHello.SignedHandshake signedHandshake = protocolManager.buildSignedHandshake(handshake, dstSignature);
        byte[] cipherText = encryptionManager.encryptCipherText(signedHandshake);
        Basemessage.BaseMessage baseMessage = protocolManager.packClientHello(cipherText);
        return baseMessage;
    }

    public void buildAndSendRegister(String title, byte[] publicKey, byte[] organisationId, byte[] appData ){
        byte[] secretKey = ValuesManager.getInstance().getClientIdentitySecretKey();
        byte[] signedMsg = null;
        Ledgerinterface.LedgerInterface.User ownUser = protocolManager.userProto(title, publicKey, organisationId, appData);
        try {
            signedMsg = encryptionManager.signMessage(ownUser.toByteArray(), secretKey);
        } catch (NumenaLibraryException e) {
            e.printStackTrace();
        }
        Ledgerinterface.LedgerInterface.UserEvent.Builder userEventBuilder = protocolManager.userEventProtoBuilder(ownUser);
        Ledgerinterface.LedgerInterface.UserEvent userEvent = protocolManager.setSignatureOnUserEvent(userEventBuilder, signedMsg);
        Basemessage.BaseMessage baseMessage = protocolManager.register(userEvent);
        sendBaseMessage(baseMessage);
    }

    public void buildAndSendGetUsers(String query){
        ValuesManager valuesManager = ValuesManager.getInstance();
        Basemessage.BaseMessage baseMessage = protocolManager.getUsers(query, valuesManager.getOrganisationId());
        sendBaseMessage(baseMessage);
    }

    private void sendBaseMessage(Basemessage.BaseMessage baseMessage){
        ValuesManager valuesManager = ValuesManager.getInstance();
        int nonce = valuesManager.getLocalNonce();
        try {
            byte[] encryptedMessage = encryptionManager.encryptMessage(baseMessage.toByteArray(),baseMessage.getSerializedSize(),nonce);
            singleMessageManager.sendBinary(encryptedMessage);
        } catch (NumenaLibraryException e) {
            e.printStackTrace();
        }
    }

    private void incrementNonces(){
        ValuesManager valuesManager = ValuesManager.getInstance();
        int currentLocalNonce = valuesManager.getLocalNonce();
        int currentRemoteNonce = valuesManager.getRemoteNonce();
        valuesManager.setLocalNonce(currentLocalNonce + 2);
        valuesManager.setRemoteNonce(currentRemoteNonce + 2);
        Log.d("LOCALNONCE", "" + valuesManager.getLocalNonce());
        Log.d("REMOTENONCE", "" + valuesManager.getRemoteNonce());
    }

    /**
     * Takes a callback and makes the resultlistener call the it upon success.
     * @param numenaMethod
     * @return
     */

    private ResultsListener setupListenerAndCallback(final NumenaMethod numenaMethod) {
        ResultsListener openingListener = new ResultsListener<byte[]>() {
            @Override
            public void onSuccess(byte[] result) {
                try {
                    numenaMethod.setResult(result);
                    numenaMethod.call();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Throwable e, String response) {

            }
        };
        return openingListener;
    }

    class MessageCallback extends NumenaMethod {

        @Override
        public Void call() {
            byte[] result = (byte[]) getResult();
            try {
                handleMessage(result);
            } catch (NumenaLibraryException e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
