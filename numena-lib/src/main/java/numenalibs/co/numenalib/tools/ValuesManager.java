package numenalibs.co.numenalib.tools;


import java.util.HashMap;

import numenalibs.co.numenalib.interfaces.NumenaCommunicatorInterface;

public class ValuesManager implements NumenaCommunicatorInterface {

    private byte[] serverConnectionPublicKey;
    private byte[] serverIdentityPublicKey;
    private byte[] whitelist = Utils.hexStringToByteArray("6c7771fdc6d83b641ad4e994a9c9bdb6e786d1b5e6953eaf1fa63b0223c156e4");;
    private byte[] clientConnectionPublicKey;
    private byte[] clientConnectionSecretKey;
    private int localNonce;
    private int remoteNonce;
    private String connectionUrl = "ws://dev.numena.co:8000";
    HashMap<String, byte[]> organisationKeys = new HashMap<>();
    HashMap<String, byte[]> appKeys = new HashMap<>();
    public static final int CRYPTO_BOX_MACBYTES = 16;

    private static ValuesManager instance;

    public static ValuesManager getInstance(){
        if(instance == null){
            instance = new ValuesManager();
            instance.setLocalNonce(0);
            instance.setRemoteNonce(1);
        }
        return instance;
    }

    public String getConnectionUrl() {
        return connectionUrl;
    }

    public void setConnectionUrl(String connectionUrl) {
        this.connectionUrl = connectionUrl;
    }

    @Override
    public void setAppKeys(HashMap<String, byte[]> keys) {

    }

    @Override
    public HashMap<String, byte[]> getAppKeys() {
        return null;
    }

    @Override
    public HashMap<String, byte[]> getDeviceKeys() {
        return null;
    }

    @Override
    public void connectToAidl() {

    }

    @Override
    public void disconnectFromAidl() {

    }

    public int getLocalNonce() {
        return localNonce;
    }

    public void setLocalNonce(int localNonce) {
        this.localNonce = localNonce;
    }

    public int getRemoteNonce() {
        return remoteNonce;
    }

    public void setRemoteNonce(int remoteNonce) {
        this.remoteNonce = remoteNonce;
    }

    public byte[] getServerConnectionPublicKey() {
        return serverConnectionPublicKey;
    }

    public void setServerConnectionPublicKey(byte[] serverConnectionPublicKey) {
        this.serverConnectionPublicKey = serverConnectionPublicKey;
    }

    public byte[] getServerIdentityPublicKey() {
        return serverIdentityPublicKey;
    }

    public void setServerIdentityPublicKey(byte[] serverIdentityPublicKey) {
        this.serverIdentityPublicKey = serverIdentityPublicKey;
    }

    public byte[] getWhitelist() {
        return whitelist;
    }

    public void setWhitelist(byte[] whitelist) {
        this.whitelist = whitelist;
    }

    public byte[] getClientConnectionPublicKey() {
        return clientConnectionPublicKey;
    }

    public void setClientConnectionPublicKey(byte[] clientConnectionPublicKey) {
        this.clientConnectionPublicKey = clientConnectionPublicKey;
    }

    public byte[] getClientConnectionSecretKey() {
        return clientConnectionSecretKey;
    }

    public void setClientConnectionSecretKey(byte[] clientConnectionSecretKey) {
        this.clientConnectionSecretKey = clientConnectionSecretKey;
    }

    public HashMap<String, byte[]> getOrganisationKeys() {
        return organisationKeys;
    }

    public void setOrganisationKeys(HashMap<String, byte[]> organisationKeys) {
        this.organisationKeys = organisationKeys;
    }

}
