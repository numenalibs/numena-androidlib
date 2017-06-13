package numenalibs.co.numenalib;



public class Numena {

    private static Numena instance;
    private String something;

    public static Numena getInstance(){
        if(instance == null){
            instance = new Numena();
        }
        return instance;
    }

    public String getSomething(){
        return  something;
    }




}
