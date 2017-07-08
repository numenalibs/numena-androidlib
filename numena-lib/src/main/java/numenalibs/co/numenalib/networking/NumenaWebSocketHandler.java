package numenalibs.co.numenalib.networking;


import android.util.Log;

import de.tavendo.autobahn.WebSocket;
import numenalibs.co.numenalib.NumenaMessageHelper;
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
        Log.d("CONNECTION", "OPENED");

    }

    @Override
    public void onClose(int code, String reason) {
        NumenaMessageHelper.connectionEstablished = false;
        Log.d("CONNECTION", "onClose");
    }

    @Override
    public void onTextMessage(String payload) {
        Log.d("CONNECTION", "onTextMessage");
    }

    @Override
    public void onRawTextMessage(byte[] payload) {
        Log.d("CONNECTION", "onRawTextMessage");
    }

    @Override
    public void onBinaryMessage(byte[] payload) {
        listener.onCompletion(payload);
    }

}
