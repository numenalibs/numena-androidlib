package numenalibs.co.numenalib;



public class Numena {

    private static Numena instance;
    private NumenaMessageHandler numenaCore;

    public static Numena getInstance(){
        if(instance == null){
            instance = new Numena();
            instance.setNumenaCore(new NumenaMessageHandler());
        }
        return instance;
    }

    public NumenaMessageHandler getNumenaCore() {
        return numenaCore;
    }

    public void setNumenaCore(NumenaMessageHandler numenaCore) {
        this.numenaCore = numenaCore;
    }
}
