package numenalibs.co.numenalib;


import org.libsodium.jni.NaCl;
import org.libsodium.jni.Sodium;

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

    public NumenaMessageHandler getMessageHandler() {
        return messageHandler;
    }

    public void setMessageHandler(NumenaMessageHandler messageHandler) {
        this.messageHandler = messageHandler;
    }


}
