package numenalibs.co.numenalib;


import android.content.Context;

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
        instance.getMessageHandler().initNumenaValues(context);
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
