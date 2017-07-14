package numenalibs.co.numenalib.models;


public class NumenaKeyPair {

    private NumenaKey publicKey, secretKey;

    public NumenaKeyPair(NumenaKey publicKey, NumenaKey secretKey){
        this.publicKey = publicKey;
        this.secretKey = secretKey;
    }

    public NumenaKey getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(NumenaKey publicKey) {
        this.publicKey = publicKey;
    }

    public NumenaKey getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(NumenaKey secretKey) {
        this.secretKey = secretKey;
    }
}
