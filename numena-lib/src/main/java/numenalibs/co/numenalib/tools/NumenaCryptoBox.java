package numenalibs.co.numenalib.tools;


import android.support.annotation.Nullable;

import com.google.protobuf.InvalidProtocolBufferException;

import org.libsodium.jni.Sodium;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import messages.Appmessage;
import numenalibs.co.numenalib.encryption.EncryptionManager;
import numenalibs.co.numenalib.exceptions.NumenaLibraryException;
import numenalibs.co.numenalib.models.NumenaKey;
import numenalibs.co.numenalib.models.NumenaKeyPair;
import numenalibs.co.numenalib.protocol.ProtocolManager;

import static numenalibs.co.numenalib.tools.Constants.ENCRYPTION_KEY_LENGTH;

public class NumenaCryptoBox {

    private List<NumenaKey> secretKeyList = new ArrayList<>();
    private EncryptionManager encryptionManager;
    private ProtocolManager protocolManager;
    private Map<String, NumenaKey> secretKeyMap = new HashMap<>();

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
     *
     * @param publickeyName
     * @param secretKeyName
     * @return
     */

    public NumenaKeyPair generateAppKey(String publickeyName, String secretKeyName) {
        byte[] PK = new byte[ENCRYPTION_KEY_LENGTH];
        byte[] SK = new byte[ENCRYPTION_KEY_LENGTH];
        Sodium.randombytes(PK, ENCRYPTION_KEY_LENGTH);
        Sodium.randombytes(SK, ENCRYPTION_KEY_LENGTH);
        Sodium.crypto_box_keypair(PK, SK);
        String pKeyHash = null;
        String sKeyHash = null;
        try {
            pKeyHash = ValuesManager.getInstance().getHash(PK);
            sKeyHash = ValuesManager.getInstance().getHash(SK);
        } catch (NumenaLibraryException e) {
            e.printStackTrace();
        }

        NumenaKey publicKey = new NumenaKey(publickeyName, pKeyHash, PK);
        NumenaKey secretKey = new NumenaKey(secretKeyName, sKeyHash, SK);
        NumenaKeyPair numenaKeyPair = new NumenaKeyPair(publicKey, secretKey);
        return numenaKeyPair;
    }

    /**
     * Method for decrypting an appMessage
     * If secret is nulled, then NumenaCryptoBox will try to with all available secretKeys in its'
     * secretKeyList
     *
     * @param appMessageContent
     * @param secretKey
     * @return
     */

    public byte[] decryptAppMessage(byte[] appMessageContent, @Nullable byte[] secretKey) throws NumenaLibraryException {
        byte[] content = null;
        byte[] decryptedContent = null;
        byte[] senderPublicKey = null;
        try {
            Appmessage.AppMessage appMessage = Appmessage.AppMessage.parseFrom(appMessageContent);
            content = appMessage.getContent().toByteArray();
            senderPublicKey = appMessage.getTemppublicKey().toByteArray();
        } catch (InvalidProtocolBufferException e) {
            throw new NumenaLibraryException("Failing: Could not parse to App message");
        }
        if (secretKey != null) {
            decryptedContent = encryptionManager.decryptAppMessage(content, senderPublicKey, secretKey);
        } else {
            String senderKayHash = ValuesManager.getInstance().getHash(senderPublicKey);
            if (secretKeyMap.containsKey(senderKayHash)) {
                NumenaKey key = secretKeyMap.get(senderKayHash);
                decryptedContent = encryptionManager.decryptAppMessage(content, senderPublicKey, key.getKeyValue());
            } else {
                for (NumenaKey key : secretKeyList) {
                    decryptedContent = encryptionManager.decryptAppMessage(content, senderPublicKey, key.getKeyValue());
                    if (decryptedContent != null) {
                        secretKeyMap.put(senderKayHash, key);
                        break;
                    }
                }
            }
        }

        if (decryptedContent == null) {
            throw new NumenaLibraryException("Failing: Could not decrypt content with provided keys");
        }

        return decryptedContent;
    }

    /**
     * Creates a byte array containing content encrypted with the provided encryptionKeys
     *
     * @param myPublicKey
     * @param encryptionPublicKey
     * @param encryptionSecretKey
     * @param content
     * @return
     */

    public byte[] createEncryptedAppMessage(byte[] myPublicKey, byte[] encryptionPublicKey, byte[] encryptionSecretKey, byte[] content) {
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
