package numenalibs.co.numenalib;


import messages.Serverhello.ServerHello;
import messages.Serverhello.ServerHello.Handshake;
import numenalibs.co.numenalib.encryption.EncryptionManager;
import numenalibs.co.numenalib.exceptions.NumenaLibraryException;
import numenalibs.co.numenalib.protocol.ProtocolManager;

public class NumenaMessageHandler {

    private ProtocolManager protocolManager;
    private EncryptionManager encryptionManager;

    public NumenaMessageHandler(){
        protocolManager = new ProtocolManager();
        encryptionManager = new EncryptionManager();

    }

    public void handleServerHello(byte[] msg) throws NumenaLibraryException{
        ServerHello serverHello = protocolManager.extractServerHello(msg);
        Handshake handshake = serverHello.getHandshake();
        encryptionManager.verifyServerhello(serverHello,handshake);
    }
}
