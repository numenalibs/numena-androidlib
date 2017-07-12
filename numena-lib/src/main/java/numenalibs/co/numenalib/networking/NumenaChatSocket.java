package numenalibs.co.numenalib.networking;


import de.tavendo.autobahn.WebSocket;
import de.tavendo.autobahn.WebSocketConnection;
import messages.Basemessage;
import messages.Serverhello;
import numenalibs.co.numenalib.NumenaMessageHelper;
import numenalibs.co.numenalib.encryption.EncryptionManager;
import numenalibs.co.numenalib.exceptions.NumenaLibraryException;
import numenalibs.co.numenalib.interfaces.ResultsListener;
import numenalibs.co.numenalib.models.NumenaResponse;
import numenalibs.co.numenalib.protocol.ProtocolManager;
import numenalibs.co.numenalib.tools.Constants;
import numenalibs.co.numenalib.tools.ValuesManager;

public class NumenaChatSocket implements WebSocket.ConnectionHandler{

    private ProtocolManager protocolManager;
    private EncryptionManager encryptionManager;
    private NumenaMessageHelper numenaMessageHelper;
    private WebSocketConnection webSocketConnection;
    private int STATE;

    public NumenaChatSocket(WebSocketConnection webSocketConnection){
        protocolManager = new ProtocolManager();
        encryptionManager = new EncryptionManager();
        this.webSocketConnection = webSocketConnection;
        numenaMessageHelper = new NumenaMessageHelper(protocolManager,encryptionManager);
        STATE = Constants.EXPECTING_SERVERHELLO;
    }

    @Override
    public void onOpen() {

    }

    @Override
    public void onClose(int code, String reason) {

    }

    @Override
    public void onTextMessage(String payload) {

    }

    @Override
    public void onRawTextMessage(byte[] payload) {

    }

    @Override
    public void onBinaryMessage(byte[] payload) {
        if(STATE == Constants.EXPECTING_SERVERHELLO){
            handleServerHelloMessage(payload);
        }


    }

    private void handleServerHelloMessage(byte[] result) {
        Serverhello.ServerHello serverHello = null;
        try {
            serverHello = numenaMessageHelper.buildServerHello(result);
            Basemessage.BaseMessage baseMessage = numenaMessageHelper.buildClientHello(serverHello);
            ValuesManager.getInstance().storeKeysInDatabase();
            ValuesManager.getInstance().refreshKeysFromDatabase();
            STATE = Constants.EXPECTING_MESSAGE;
            webSocketConnection.sendBinaryMessage(baseMessage.toByteArray());
        } catch (NumenaLibraryException e) {
            e.printStackTrace();
        }
    }
}
