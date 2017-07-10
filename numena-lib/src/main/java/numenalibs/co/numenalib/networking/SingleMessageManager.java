package numenalibs.co.numenalib.networking;


import android.util.Log;

import de.tavendo.autobahn.WebSocketConnection;
import de.tavendo.autobahn.WebSocketException;
import de.tavendo.autobahn.WebSocketOptions;
import numenalibs.co.numenalib.interfaces.ResultsListener;
import numenalibs.co.numenalib.tools.ValuesManager;

public class SingleMessageManager {

    private WebSocketConnection webSocketConnection;
    private NumenaWebSocketHandler numenaWebSocketHandler;
    private ResultsListener<byte[]> listener;
    private int count = 0;

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
            numenaWebSocketHandler = new NumenaWebSocketHandler(listener);
            WebSocketOptions options = new WebSocketOptions();
            options.setSocketConnectTimeout(5000);
            options.setMaxMessagePayloadSize(10000000); //max size of message
            options.setMaxFramePayloadSize(10000000); //max size of frame
            webSocketConnection.connect(vm.getConnectionUrl(), numenaWebSocketHandler, options);
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
        count++;
        this.listener = listener;
        if(numenaWebSocketHandler != null)
        numenaWebSocketHandler.setListener(listener);
    }

}
