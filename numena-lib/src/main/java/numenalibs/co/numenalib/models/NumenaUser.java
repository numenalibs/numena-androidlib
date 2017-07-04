package numenalibs.co.numenalib.models;


import java.util.Arrays;

public class NumenaUser extends NumenaObject {

    private String username;
    private byte[] appData;
    private byte[] publicKey;
    private byte[] organisationId;

    public NumenaUser() {

    }

    public NumenaUser(String username, byte[] appData, byte[] publicKey, byte[] organisationId) {
        this.username = username;
        this.appData = Arrays.copyOf(appData,appData.length);
        this.publicKey = Arrays.copyOf(publicKey,publicKey.length);
        this.organisationId = Arrays.copyOf(organisationId,organisationId.length);
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public byte[] getAppData() {
        return appData;
    }

    public void setAppData(byte[] appData) {
        this.appData = appData;
    }

    public byte[] getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(byte[] publicKey) {
        this.publicKey = publicKey;
    }

    public byte[] getOrganisationId() {
        return organisationId;
    }

    public void setOrganisationId(byte[] organisationId) {
        this.organisationId = organisationId;
    }
}
