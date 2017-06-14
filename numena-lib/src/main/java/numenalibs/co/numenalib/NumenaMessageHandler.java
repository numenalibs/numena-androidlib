package numenalibs.co.numenalib;


import android.util.Log;

import java.util.concurrent.Callable;

import messages.Serverhello.ServerHello;
import messages.Serverhello.ServerHello.Handshake;
import numenalibs.co.numenalib.encryption.EncryptionManager;
import numenalibs.co.numenalib.exceptions.NumenaLibraryException;
import numenalibs.co.numenalib.interfaces.ResultsListener;
import numenalibs.co.numenalib.networking.SingleMessageManager;
import numenalibs.co.numenalib.protocol.ProtocolManager;

public class NumenaMessageHandler {

    private ProtocolManager protocolManager;
    private EncryptionManager encryptionManager;
    private SingleMessageManager singleMessageManager;

    public NumenaMessageHandler(){
        protocolManager = new ProtocolManager();
        encryptionManager = new EncryptionManager();
        singleMessageManager = new SingleMessageManager();

    }

    public void handleServerHello(byte[] msg) throws NumenaLibraryException{
        ServerHello serverHello = protocolManager.extractServerHello(msg);
        Handshake handshake = serverHello.getHandshake();
        encryptionManager.verifyServerhello(serverHello,handshake);
    }

    public void initConnection(){
        ResultsListener listener = setupOpeningListener(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                Log.d("HEY", "HEY");
                return null;
            }
        });
        singleMessageManager.setListener(listener);
        singleMessageManager.openWebsocket();

    }

    private ResultsListener setupOpeningListener(final Callable<Void> inputFunc){
        ResultsListener openingListener = new ResultsListener<byte[]>() {
            @Override
            public void onSuccess(byte[] result) {
                try {
                    handleServerHello(result);
                    inputFunc.call();
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
