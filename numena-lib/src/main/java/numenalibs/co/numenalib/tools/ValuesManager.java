package numenalibs.co.numenalib.tools;


import java.util.Arrays;
import java.util.HashMap;

import numenalibs.co.numenalib.interfaces.NumenaCommunicatorInterface;

public class ValuesManager implements NumenaCommunicatorInterface {

    private byte[] serverConnectionPublicKey;
    private byte[] serverIdentityPublicKey;
    private byte[] whitelist = Utils.hexStringToByteArray("6c7771fdc6d83b641ad4e994a9c9bdb6e786d1b5e6953eaf1fa63b0223c156e4");;
    private byte[] clientConnectionPublicKey;
    private byte[] clientConnectionSecretKey;
    private byte[] clientIdentityPublicKey;
    private byte[] clientIdentitySecretKey;
    private int localNonce;
    private int remoteNonce;
    private boolean isConnectionToOrganizationServer = false;
    private String connectionUrl = "ws://dev.numena.co:8000";
    HashMap<String, byte[]> organisationKeys = new HashMap<>();
    HashMap<String, byte[]> appKeys = new HashMap<>();


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

    public boolean isConnectionToOrganizationServer() {
        return isConnectionToOrganizationServer;
    }

    public void setConnectionToOrganizationServer(boolean connectionToOrganizationServer) {
        isConnectionToOrganizationServer = connectionToOrganizationServer;
    }

    public byte[] getClientIdentityPublicKey() {
        return clientIdentityPublicKey;
    }

    public void setClientIdentityPublicKey(byte[] clientIdentityPublicKey) {
        this.clientIdentityPublicKey = Arrays.copyOf(clientIdentityPublicKey, clientIdentityPublicKey.length);
    }

    public byte[] getClientIdentitySecretKey() {
        return clientIdentitySecretKey;
    }

    public void setClientIdentitySecretKey(byte[] clientIdentitySecretKey) {
        this.clientIdentitySecretKey =  Arrays.copyOf(clientIdentitySecretKey, clientIdentitySecretKey.length);
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
        this.serverConnectionPublicKey = Arrays.copyOf(serverConnectionPublicKey, serverConnectionPublicKey.length);
    }

    public byte[] getServerIdentityPublicKey() {
        return serverIdentityPublicKey;
    }

    public void setServerIdentityPublicKey(byte[] serverIdentityPublicKey) {
        this.serverIdentityPublicKey = Arrays.copyOf(serverIdentityPublicKey,serverIdentityPublicKey.length);
    }

    public byte[] getWhitelist() {
        return whitelist;
    }

    public void setWhitelist(byte[] whitelist) {
        this.whitelist = Arrays.copyOf(whitelist,whitelist.length);
    }

    public byte[] getClientConnectionPublicKey() {
        return clientConnectionPublicKey;
    }

    public void setClientConnectionPublicKey(byte[] clientConnectionPublicKey) {
        this.clientConnectionPublicKey = Arrays.copyOf(clientConnectionPublicKey,clientConnectionPublicKey.length);
    }

    public byte[] getClientConnectionSecretKey() {
        return clientConnectionSecretKey;
    }

    public void setClientConnectionSecretKey(byte[] clientConnectionSecretKey) {
        this.clientConnectionSecretKey = Arrays.copyOf(clientConnectionSecretKey,clientConnectionSecretKey.length);
    }

    public HashMap<String, byte[]> getOrganisationKeys() {
        return organisationKeys;
    }

    public void setOrganisationKeys(HashMap<String, byte[]> organisationKeys) {
        this.organisationKeys = organisationKeys;
    }

}
