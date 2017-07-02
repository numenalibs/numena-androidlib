package numenalibs.co.numenalib.networking;


import android.util.Log;

import de.tavendo.autobahn.WebSocket;
import de.tavendo.autobahn.WebSocketConnection;
import de.tavendo.autobahn.WebSocketException;
import numenalibs.co.numenalib.interfaces.ResultsListener;
import numenalibs.co.numenalib.tools.ValuesManager;

public class SingleMessageManager {

    private WebSocketConnection webSocketConnection;
    private NumenaWebSocketHandler numenaWebSocketHandler;
    private ResultsListener<byte[]> listener;

    public void openWebsocket() {
        if (webSocketConnection == null) {
            webSocketConnection = new WebSocketConnection();
        }
        if (!webSocketConnection.isConnected()) {
            connectWebsocket();
        }
    }

    private void connectWebsocket() {
        ValuesManager vm = ValuesManager.getInstance();
        try {
            Log.d("SINGLEMESSAGEHANDLER", "OPENING");
            numenaWebSocketHandler = new NumenaWebSocketHandler(listener);
            webSocketConnection.connect(vm.getConnectionUrl(), numenaWebSocketHandler);
        } catch (WebSocketException e) {
            e.printStackTrace();
        }
    }

    public void sendBinary(byte[] msg){
        webSocketConnection.sendBinaryMessage(msg);
    }

    public ResultsListener<byte[]> getListener() {
        return listener;
    }

    public void setListener(ResultsListener<byte[]> listener) {
        this.listener = listener;
        if(numenaWebSocketHandler != null)
        numenaWebSocketHandler.setListener(listener);
    }

}
