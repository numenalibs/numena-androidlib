package numenalibs.co.numenalib.interfaces;


import java.util.HashMap;

public interface NumenaCommunicatorInterface {

    public void setAppKeys(HashMap<String, byte[]> keys);

    public HashMap<String, byte[]> getAppKeys();

    public HashMap<String, byte[]> getDeviceKeys();

    public void connectToAidl();

    public void disconnectFromAidl();

}
