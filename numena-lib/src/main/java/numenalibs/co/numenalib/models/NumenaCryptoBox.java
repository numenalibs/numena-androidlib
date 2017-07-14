package numenalibs.co.numenalib.models;


import android.support.annotation.Nullable;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import numenalibs.co.numenalib.encryption.EncryptionManager;
import numenalibs.co.numenalib.exceptions.NumenaLibraryException;
import numenalibs.co.numenalib.tools.Constants;
import numenalibs.co.numenalib.tools.ValuesManager;

public class NumenaCryptoBox {

    private List<NumenaKey> secretKeyList = new ArrayList<>();
    private EncryptionManager encryptionManager;

    public NumenaCryptoBox(EncryptionManager encryptionManager) {
        this.encryptionManager = encryptionManager;
    }

    public void refreshSecretKeyList(List<NumenaKey> secretKeyList) {
        this.secretKeyList.clear();
        this.secretKeyList.addAll(secretKeyList);
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

}
