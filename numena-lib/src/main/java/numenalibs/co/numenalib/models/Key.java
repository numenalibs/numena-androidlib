package numenalibs.co.numenalib.models;

import java.util.Arrays;

public class Key {

    String id, keyName, keyHash;
    byte[] keyValue;

    public Key(String id, String keyName, String keyHash, byte[] keyValue) {
        this.id = id;
        this.keyName = keyName;
        this.keyHash = keyHash;
        this.keyValue = keyValue;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getKeyName() {
        return keyName;
    }

    public void setKeyName(String keyName) {
        this.keyName = keyName;
    }

    public String getKeyHash() {
        return keyHash;
    }

    public void setKeyHash(String keyHash) {
        this.keyHash = keyHash;
    }

    public byte[] getKeyValue() {
        return keyValue;
    }

    public void setKeyValue(byte[] keyValue) {
        this.keyValue = Arrays.copyOf(keyValue, keyValue.length);
    }
}
