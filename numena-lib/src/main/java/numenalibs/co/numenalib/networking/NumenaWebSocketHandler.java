package numenalibs.co.numenalib.networking;


import android.util.Log;

import de.tavendo.autobahn.WebSocket;
import numenalibs.co.numenalib.interfaces.ResultsListener;

public class NumenaWebSocketHandler implements WebSocket.ConnectionHandler {

    ResultsListener<byte[]> listener;

    public NumenaWebSocketHandler(ResultsListener<byte[]> aListener){
        this.listener = aListener;
    }

    public void setListener(ResultsListener<byte[]> listener) {
        this.listener = listener;
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
        ;
    }

    @Override
    public void onBinaryMessage(byte[] payload) {
        listener.onCompletion(payload);
    }

}
