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
import numenalibs.co.numenalib.models.NumenaKey;

public class ValuesManager implements NumenaCommunicatorInterface {

    private byte[] whitelist = Utils.hexStringToByteArray(Constants.WHITELISTTEXT);;
    private int localNonce;
    private int remoteNonce;
    private boolean isConnectionToOrganizationServer = false;
    private String connectionUrl = Constants.URL_DEV;
    HashMap<String, byte[]> organisationKeys = new HashMap<>();
    HashMap<String, byte[]> keys = new HashMap<>();
    private byte[] organisationId = Constants.NUMENAORG.getBytes();
    private static ValuesManager instance;
    private DatabaseHelper databaseHelper;

    public static ValuesManager getInstance(){
        if(instance == null){
            instance = new ValuesManager();
            instance.resetNonces();
        }
        return instance;
    }

    public void resetNonces(){
        instance.setLocalNonce(0);
        instance.setRemoteNonce(1);
    }

    public void initDatabase(Context context){
        databaseHelper = new DatabaseHelper(context, "");
    }

    public void refreshKeysFromDatabase(){
        List<NumenaKey> keys = databaseHelper.getAllKeys();
        for(NumenaKey key : keys){
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

    public byte[] getOrganisationId() {
        return organisationId;
    }

    public void setOrganisationId(byte[] organisationId) {
        this.organisationId = Arrays.copyOf(organisationId,organisationId.length);
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
