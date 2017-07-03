package numenalibs.co.numenalib;


import android.content.Context;
import android.util.Log;

import com.google.protobuf.InvalidProtocolBufferException;

import org.libsodium.jni.NaCl;
import org.libsodium.jni.Sodium;


import messages.Basemessage;
import messages.Clienthello;
import messages.Serverhello;
import messages.Statusmessage;
import numenalibs.co.numenalib.encryption.EncryptionManager;
import numenalibs.co.numenalib.exceptions.NumenaLibraryException;
import numenalibs.co.numenalib.interfaces.ResultsListener;
import numenalibs.co.numenalib.models.NumenaMethod;
import numenalibs.co.numenalib.networking.SingleMessageManager;
import numenalibs.co.numenalib.protocol.ProtocolManager;
import numenalibs.co.numenalib.tools.ValuesManager;


public class NumenaMessageHandler {

    private ProtocolManager protocolManager;
    private EncryptionManager encryptionManager;
    private SingleMessageManager singleMessageManager;

    public NumenaMessageHandler() {
        protocolManager = new ProtocolManager();
        encryptionManager = new EncryptionManager();
        singleMessageManager = new SingleMessageManager();
    }

    public void setupNumenaDatabase(Context context) {
        ValuesManager valuesManager = ValuesManager.getInstance();
        valuesManager.initDatabase(context);
        setupKeys();
    }

    public Serverhello.ServerHello handleServerHello(byte[] msg) throws NumenaLibraryException {
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

    private void setupKeys() {
        ValuesManager vm = ValuesManager.getInstance();
        if (!hasIdentityKeys()) {
            long publickeylen = Sodium.crypto_sign_publickeybytes();
            long privatekeylen = Sodium.crypto_sign_secretkeybytes();
            byte[] DEVICE_IDENTITY_PK = new byte[(int) publickeylen];
            byte[] DEVICE_IDENTITY_SK = new byte[(int) privatekeylen];
            Sodium.randombytes(DEVICE_IDENTITY_PK, (int) publickeylen);
            Sodium.randombytes(DEVICE_IDENTITY_SK, (int) privatekeylen);
            Sodium.crypto_sign_keypair(DEVICE_IDENTITY_PK, DEVICE_IDENTITY_SK);
            vm.setClientIdentityPublicKey(DEVICE_IDENTITY_PK);
            vm.setClientIdentitySecretKey(DEVICE_IDENTITY_SK);
        }else {
            vm.refreshKeysFromDatabase();
        }
    }

    public boolean hasIdentityKeys(){
        return ValuesManager.getInstance().identityExists();
    }


    public void initConnection() {
        ResultsListener listener = setupListenerAndCallback(new MyCallback());
        singleMessageManager.setListener(listener);
        singleMessageManager.openWebsocket();
    }

    class MyCallback extends NumenaMethod {

        @Override
        public Void call() {
            byte[] result = (byte[]) getResult();
            handleResult(result);
            return null;
        }
    }

    class MyCallback2 extends NumenaMethod {

        @Override
        public Void call() {
            byte[] result = (byte[]) getResult();
            checkStatus(result);
            return null;
        }
    }

    private void checkStatus(byte[] msg) {
        byte[] decryptedmsg = encryptionManager.decrypt_message(msg);
        Basemessage.BaseMessage basemessage = null;
        try {
            basemessage = Basemessage.BaseMessage.parseFrom(decryptedmsg);
            Basemessage.BaseMessage.Type msgtype = basemessage.getType();
            if (msgtype == Basemessage.BaseMessage.Type.STATUS) {
                Statusmessage.StatusMessage status = basemessage.getStatus();
                long code = status.getStatusCode();
                Log.d("code", "value: " + code);
                ValuesManager valuesManager = ValuesManager.getInstance();
                valuesManager.storeKeysInDatabase();
                valuesManager.refreshKeysFromDatabase();
            }
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }
    }

    private void handleResult(byte[] result) {
        Serverhello.ServerHello serverHello = null;
        try {
            serverHello = handleServerHello(result);
            Basemessage.BaseMessage baseMessage = buildClientHello(serverHello);
            ResultsListener listener = setupListenerAndCallback(new MyCallback2());
            singleMessageManager.setListener(listener);
            singleMessageManager.sendBinary(baseMessage.toByteArray());
        } catch (NumenaLibraryException e) {
            e.printStackTrace();
        }
    }

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
}
