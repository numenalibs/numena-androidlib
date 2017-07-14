package numenalibs.co.numenalib.models;


import android.support.annotation.Nullable;

import org.libsodium.jni.Sodium;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import messages.Appmessage;
import numenalibs.co.numenalib.encryption.EncryptionManager;
import numenalibs.co.numenalib.exceptions.NumenaLibraryException;
import numenalibs.co.numenalib.protocol.ProtocolManager;
import numenalibs.co.numenalib.tools.Constants;
import numenalibs.co.numenalib.tools.ValuesManager;

public class NumenaCryptoBox {

    private List<NumenaKey> secretKeyList = new ArrayList<>();
    private EncryptionManager encryptionManager;
    private ProtocolManager protocolManager;

    public NumenaCryptoBox(EncryptionManager encryptionManager, ProtocolManager protocolManager) {
        this.encryptionManager = encryptionManager;
        this.protocolManager = protocolManager;
    }

    public void refreshSecretKeyList(List<NumenaKey> secretKeyList) {
        this.secretKeyList.clear();
        this.secretKeyList.addAll(secretKeyList);
    }

    /**
     * Generates two new AppKeys and returns a NumenaKeyPair holding them
     * @param publickeyName
     * @param secretKeyName
     * @return
     */

    public NumenaKeyPair generateAppKey(String publickeyName, String secretKeyName){
        byte[] PK = new byte[32];
        byte[] SK = new byte[32];
        Sodium.randombytes(PK, 32);
        Sodium.randombytes(SK, 32);
        Sodium.crypto_box_keypair(PK, SK);
        String pKeyHash = null;
        String sKeyHash = null;
        try {
            pKeyHash = ValuesManager.getInstance().getHash(PK);
            sKeyHash = ValuesManager.getInstance().getHash(SK);

        } catch (NumenaLibraryException e) {
            e.printStackTrace();
        }

        NumenaKey publicKey = new NumenaKey(publickeyName,pKeyHash,PK);
        NumenaKey secretKey = new NumenaKey(secretKeyName,sKeyHash,SK);
        NumenaKeyPair numenaKeyPair = new NumenaKeyPair(publicKey,secretKey);
        return numenaKeyPair;
    }

    /**
     * Method for decrypting an appMessage
     * If secret is nulled, then NumenaCryptoBox will try to with all available secretKeys in its'
     * secretKeyList
     * @param content
     * @param senderPublicKey
     * @param secretKey
     * @return
     */

    public byte[] decryptAppMessage(byte[] content, byte[] senderPublicKey, @Nullable byte[] secretKey) {
        byte[] decryptedContent = null;
            if (secretKey == null) {
                try {
                   decryptedContent = encryptionManager.decrypt_appMessage(content, senderPublicKey, secretKey);
                } catch (NumenaLibraryException e) {
                    e.printStackTrace();
                }
            } else {
                for(NumenaKey key : secretKeyList){
                    try {
                       decryptedContent = encryptionManager.decrypt_appMessage(content,senderPublicKey,key.getKeyValue());
                    } catch (NumenaLibraryException e) {
                        e.printStackTrace();
                    }
                }
            }
        return decryptedContent;
    }

    /**
     * Creates a byte array containing content encrypted with the provided encryptionKeys
     * @param myPublicKey
     * @param encryptionPublicKey
     * @param encryptionSecretKey
     * @param content
     * @return
     */

    public byte[] createEncryptedAppMessage(byte[] myPublicKey, byte[] encryptionPublicKey, byte[] encryptionSecretKey, byte[] content){
        byte[] encryptedContent = new byte[0];
        try {
            encryptedContent = encryptionManager.encryptAppMessage(content, content.length, 0, encryptionPublicKey, encryptionSecretKey);
        } catch (NumenaLibraryException e) {
            e.printStackTrace();
        }
        byte[] signature = null;
        try {
            signature = encryptionManager.signMessage(content, encryptionSecretKey);
        } catch (NumenaLibraryException e) {
            e.printStackTrace();
        }
        Appmessage.AppMessage appMessage = protocolManager.appMessage(encryptedContent, signature, myPublicKey);
        return appMessage.toByteArray();
    }

}
