package numenalibs.co.numenalib;


import android.content.pm.LauncherApps;
import android.util.Log;

import java.util.concurrent.Callable;

import messages.Basemessage;
import messages.Clienthello;
import messages.Serverhello.ServerHello;
import messages.Serverhello.ServerHello.Handshake;
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

    public NumenaMessageHandler(){
        protocolManager = new ProtocolManager();
        encryptionManager = new EncryptionManager();
        singleMessageManager = new SingleMessageManager();

    }

    public ServerHello handleServerHello(byte[] msg) throws NumenaLibraryException{
        ServerHello serverHello = protocolManager.extractServerHello(msg);
        Handshake handshake = serverHello.getHandshake();
        encryptionManager.verifyServerhello(serverHello,handshake);
        return serverHello;
    }

    public Basemessage.BaseMessage buildClientHello(byte[] msg, ServerHello serverHello) throws NumenaLibraryException{
        ValuesManager valuesManager = ValuesManager.getInstance();
        protocolManager.createClientConnectionKeys();
        Clienthello.ClientHello.Handshake handshake = protocolManager.buildClientHelloHandshake(serverHello);
        byte[] dstSignature = null;
        if(valuesManager.isConnectionToOrganizationServer()){
            dstSignature = encryptionManager.makeHandshakeSignature(handshake,valuesManager.getClientConnectionPublicKey());
        }
        Clienthello.ClientHello.SignedHandshake signedHandshake = protocolManager.buildSignedHandshake(handshake,dstSignature);
        byte[] cipherText = encryptionManager.encryptCipherText(signedHandshake);
        Basemessage.BaseMessage baseMessage = protocolManager.packClientHello(cipherText);
        return baseMessage;
    }


    public void initConnection(){
        ResultsListener listener = setupOpeningListener(new MyCallback());
        singleMessageManager.setListener(listener);
        singleMessageManager.openWebsocket();
    }

    class MyCallback extends NumenaMethod {

        @Override
        public Void call() {
            Log.d("Done", "RESULT" + getResult());
            return null;
        }
    }

    private ResultsListener setupOpeningListener(final NumenaMethod numenaMethod){
        ResultsListener openingListener = new ResultsListener<byte[]>() {
            @Override
            public void onSuccess(byte[] result) {
                try {
                    handleServerHello(result);
                    numenaMethod.setResult(result);
                    numenaMethod.call();
                }  catch (Exception e) {
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
