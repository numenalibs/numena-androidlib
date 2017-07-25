package numenalibs.co.numenalib.networking;


import android.util.Log;

import de.tavendo.autobahn.WebSocketConnection;
import de.tavendo.autobahn.WebSocketException;
import de.tavendo.autobahn.WebSocketOptions;
import numenalibs.co.numenalib.interfaces.ResultsListener;
import numenalibs.co.numenalib.tools.ValuesManager;

import static numenalibs.co.numenalib.tools.Constants.MAXFRAMEPAYLOADSIZE;
import static numenalibs.co.numenalib.tools.Constants.MAXMESSAGEPAYLOADSIZE;
import static numenalibs.co.numenalib.tools.Constants.SOCKETCONNECTTIMEOUT;

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

    public void disconnectWebsocket(){
        if(webSocketConnection != null)
        webSocketConnection.disconnect();
    }

    public boolean isWebsocketConnected(){
        if(webSocketConnection == null){
            return false;
        }
        return webSocketConnection.isConnected();
    }

    private void connectWebsocket() {
        ValuesManager vm = ValuesManager.getInstance();
        try {
            numenaWebSocketHandler = new NumenaWebSocketHandler(listener);
            WebSocketOptions options = new WebSocketOptions();
            options.setSocketConnectTimeout(SOCKETCONNECTTIMEOUT);
            options.setMaxMessagePayloadSize(MAXMESSAGEPAYLOADSIZE);
            options.setMaxFramePayloadSize(MAXFRAMEPAYLOADSIZE);
            webSocketConnection.connect(vm.getConnectionUrl(), numenaWebSocketHandler, options);
        } catch (WebSocketException e) {
            e.printStackTrace();
        }
    }

    public void sendBinary(byte[] msg){
        webSocketConnection.sendBinaryMessage(msg);
    }

    public void setListener(ResultsListener<byte[]> listener) {
        this.listener = listener;
        if(numenaWebSocketHandler != null)
        numenaWebSocketHandler.setListener(listener);
    }

}
