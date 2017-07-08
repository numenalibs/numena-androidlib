package numenalibs.co.numenalib.tools;


import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import java.util.ArrayList;
import java.util.List;

import static numenalibs.co.numenalib.tools.Constants.BROADCASTCODE;

public class BroadCaster {

    private static BroadCaster broadCaster;
    private List<Handler> observers = new ArrayList<>();
    public void registerObserver(Handler observer) {
        observers.add(observer);
    }

    public static BroadCaster getBroadCaster(){
        if(broadCaster == null){
            broadCaster = new BroadCaster();
        }
        return broadCaster;
    }


    public void broadcastToObservers(int broadcastCode){
        for(Handler handler : observers){
            if(handler == null){
                observers.remove(handler);
            }else {
                final Message msg = new Message();
                final Bundle b = new Bundle();
                b.putInt(BROADCASTCODE, broadcastCode);
                msg.setData(b);
                handler.sendMessage(msg);
            }
        }
    }
}
