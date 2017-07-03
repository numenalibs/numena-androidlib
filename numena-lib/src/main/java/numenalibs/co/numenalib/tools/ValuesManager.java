package numenalibs.co.numenalib.tools;


import android.content.Context;
import android.util.Log;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import numenalibs.co.numenalib.database.DatabaseHelper;
import numenalibs.co.numenalib.exceptions.NumenaLibraryException;
import numenalibs.co.numenalib.interfaces.NumenaCommunicatorInterface;
import numenalibs.co.numenalib.models.Key;

public class ValuesManager implements NumenaCommunicatorInterface {

    private byte[] whitelist = Utils.hexStringToByteArray("6c7771fdc6d83b641ad4e994a9c9bdb6e786d1b5e6953eaf1fa63b0223c156e4");;
    private int localNonce;
    private int remoteNonce;
    private boolean isConnectionToOrganizationServer = false;
    private String connectionUrl = "ws://dev.numena.co:8000";
    HashMap<String, byte[]> organisationKeys = new HashMap<>();
    HashMap<String, byte[]> keys = new HashMap<>();

    private static ValuesManager instance;
    private DatabaseHelper databaseHelper;

    public static ValuesManager getInstance(){
        if(instance == null){
            instance = new ValuesManager();
            instance.setLocalNonce(0);
            instance.setRemoteNonce(1);
        }
        return instance;
    }

    public void initDatabase(Context context){
        databaseHelper = new DatabaseHelper(context, "");
    }

    public void refreshKeysFromDatabase(){
        List<Key> keys = databaseHelper.getAllKeys();
        for(Key key : keys){
            Log.d("Keys", "KEYNAME " + key.getKeyName() + " KEYVALUE " + Utils.printByteArray(key.getKeyValue()));
            this.keys.put(key.getKeyName(),key.getKeyValue());
        }
    }

    public boolean identityExists(){
        return databaseHelper.identityKeysExists();
    }

    public void storeKeysInDatabase(){
        for(String key : keys.keySet()){
            byte[] value = keys.get(key);
            String keyHash = null;
            try {
                keyHash = getHash(value);
            } catch (NumenaLibraryException e) {
                e.printStackTrace();
            }
            databaseHelper.insertValuesIntoKeys(key, value,keyHash);
        }
    }

    public String getHash(byte[] key) throws NumenaLibraryException {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance(Constants.SHA256_ENCODING);
            md.update(key);
            return Utils.printByteArray(md.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new NumenaLibraryException("Failing: Cannot get SHA-256 Encoding");
        }
    }

    public void printValues(){
        Log.d("VALUES", "serverConnectionPublicKey " +  Utils.printByteArray(keys.get(Constants.SERVER_CONNECTION_PUBLICKEY)));
        Log.d("VALUES", "serverIdentityPublicKey " +   Utils.printByteArray(keys.get(Constants.SERVER_IDENTITY_PUBLICKEY)));
        Log.d("VALUES", "clientConnectionPublicKey " +  Utils.printByteArray(keys.get(Constants.CLIENT_CONNECTION_PUBLICKEY)));
        Log.d("VALUES", "clientConnectionSecretKey " +   Utils.printByteArray(keys.get(Constants.CLIENT_CONNECTION_SECRETKEY)));
        Log.d("VALUES", "clientIdentityPublicKey " +   Utils.printByteArray(keys.get(Constants.CLIENT_IDENTITY_PUBLICKEY)));
        Log.d("VALUES", "clientIdentitySecretKey " +  Utils.printByteArray(keys.get(Constants.CLIENT_CONNECTION_SECRETKEY)));
        Log.d("VALUES", "localNonce " + localNonce);
        Log.d("VALUES ", "remoteNonce" + remoteNonce);
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
        return keys.get(Constants.CLIENT_IDENTITY_PUBLICKEY);
    }

    public void setClientIdentityPublicKey(byte[] clientIdentityPublicKey) {
        keys.put(Constants.CLIENT_IDENTITY_PUBLICKEY, clientIdentityPublicKey);
    }

    public byte[] getClientIdentitySecretKey() {
        return keys.get(Constants.CLIENT_IDENTITY_SECRETKEY);
    }

    public void setClientIdentitySecretKey(byte[] clientIdentitySecretKey) {
        keys.put(Constants.CLIENT_IDENTITY_SECRETKEY, clientIdentitySecretKey);
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
        return keys.get(Constants.SERVER_CONNECTION_PUBLICKEY);
    }

    public void setServerConnectionPublicKey(byte[] serverConnectionPublicKey) {
        keys.put(Constants.SERVER_CONNECTION_PUBLICKEY, serverConnectionPublicKey);
    }

    public byte[] getServerIdentityPublicKey() {
        return keys.get(Constants.SERVER_IDENTITY_PUBLICKEY);
    }

    public void setServerIdentityPublicKey(byte[] serverIdentityPublicKey) {
        keys.put(Constants.SERVER_IDENTITY_PUBLICKEY, serverIdentityPublicKey);
    }

    public byte[] getWhitelist() {
        return whitelist;
    }

    public void setWhitelist(byte[] whitelist) {
        this.whitelist = Arrays.copyOf(whitelist,whitelist.length);
    }

    public byte[] getClientConnectionPublicKey() {
        return keys.get(Constants.CLIENT_CONNECTION_PUBLICKEY);
    }

    public void setClientConnectionPublicKey(byte[] clientConnectionPublicKey) {
        keys.put(Constants.CLIENT_CONNECTION_PUBLICKEY, clientConnectionPublicKey);
    }

    public byte[] getClientConnectionSecretKey() {
        return keys.get(Constants.CLIENT_CONNECTION_SECRETKEY);
    }

    public void setClientConnectionSecretKey(byte[] clientConnectionSecretKey) {
        keys.put(Constants.CLIENT_CONNECTION_SECRETKEY, clientConnectionSecretKey);
    }

    public HashMap<String, byte[]> getOrganisationKeys() {
        return organisationKeys;
    }

    public void setOrganisationKeys(HashMap<String, byte[]> organisationKeys) {
        this.organisationKeys = organisationKeys;
    }

}
