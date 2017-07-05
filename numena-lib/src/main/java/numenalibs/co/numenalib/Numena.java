package numenalibs.co.numenalib;


import android.content.Context;

import org.libsodium.jni.NaCl;
import org.libsodium.jni.Sodium;

import numenalibs.co.numenalib.interfaces.ResultsListener;
import numenalibs.co.numenalib.tools.ValuesManager;

public class Numena {

    private static Numena instance;
    private NumenaMessageHandler messageHandler;

    public static Numena getInstance(){
        if(instance == null){
            instance = new Numena();
            instance.setMessageHandler(new NumenaMessageHandler());
        }
        return instance;
    }

    public void setupNumenaLibrary(Context context){
        instance.getMessageHandler().startNumenaCommunication(context);
    }

    public NumenaMessageHandler getMessageHandler() {
        return messageHandler;
    }

    public ValuesManager getValuesManager(){
        return ValuesManager.getInstance();
    }

    public void setMessageHandler(NumenaMessageHandler messageHandler) {
        this.messageHandler = messageHandler;
    }


}
