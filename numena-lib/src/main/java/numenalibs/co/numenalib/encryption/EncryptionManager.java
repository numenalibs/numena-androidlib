package numenalibs.co.numenalib.encryption;


import android.util.Log;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;


import org.libsodium.jni.NaCl;
import org.libsodium.jni.Sodium;
import org.libsodium.jni.SodiumConstants;

import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.TimeZone;

import messages.Basemessage;
import messages.Clienthello;
import messages.Serverhello.ServerHello;
import messages.Serverhello.ServerHello.Handshake;
import numenalibs.co.numenalib.exceptions.NumenaLibraryException;
import numenalibs.co.numenalib.tools.Constants;
import numenalibs.co.numenalib.tools.Utils;
import numenalibs.co.numenalib.tools.ValuesManager;

import static org.libsodium.jni.Sodium.crypto_box_easy;
import static org.libsodium.jni.Sodium.crypto_box_open_easy;
import static org.libsodium.jni.Sodium.crypto_sign_detached;


public class EncryptionManager {

    public EncryptionManager(){
        Sodium sodium = NaCl.sodium();
    }

    /**
     * Generates a new pair of encryption keys and sets them in the values manager if none are present in the current database.
     */

    public void setupKeys() {
        ValuesManager vm = ValuesManager.getInstance();
        if (!hasIdentityKeys()) {
            long publickeylen = Sodium.crypto_sign_publickeybytes();
            long privatekeylen = Sodium.crypto_sign_secretkeybytes();
            byte[] DEVICE_IDENTITY_PK = new byte[(int) publickeylen];
            byte[] DEVICE_IDENTITY_SK = new byte[(int) privatekeylen];
            Sodium.randombytes(DEVICE_IDENTITY_PK, (int) publickeylen);
            Sodium.randombytes(DEVICE_IDENTITY_SK, (int) privatekeylen);
            Sodium.crypto_sign_keypair(DEVICE_IDENTITY_PK, DEVICE_IDENTITY_SK);
            vm.setClientIdentityPublicKey(DEVICE_IDENTITY_PK);
            vm.setClientIdentitySecretKey(DEVICE_IDENTITY_SK);
        }else {
            vm.refreshKeysFromDatabase();
        }
    }

    /**
     * Checks if the database contains identity keys
     * @return
     */

    public boolean hasIdentityKeys(){
        return ValuesManager.getInstance().identityExists();
    }

    /**
     * Used for signing a message with the provided secretkey
     * @param originalmessage
     * @param secretkey
     * @return
     * @throws NumenaLibraryException
     */

    public byte[] signMessage(byte[] originalmessage, byte[] secretkey) throws NumenaLibraryException {
        byte[] signature = new byte[Sodium.crypto_sign_bytes()];
        int[] signaturelen = new int[1];
        if (crypto_sign_detached(
                signature,
                signaturelen,
                originalmessage,
                originalmessage.length,
                secretkey) != 0) {
            throw new NumenaLibraryException("Failed: Could not sign message");
        }
        return signature;
    }

    /**
     * Verifies a serverhello
     * @param srvHello
     * @param handshake
     * @throws NumenaLibraryException
     */

    public void verifyServerhello(ServerHello srvHello, Handshake handshake) throws NumenaLibraryException {
        ValuesManager vm = ValuesManager.getInstance();
        ByteString srvOrganizationSignature = srvHello.getServerOrganizationSignature();
        byte[] publicKey = vm.getServerIdentityPublicKey();
        if (Sodium.crypto_sign_verify_detached(
                srvOrganizationSignature.toByteArray(),
                publicKey,
                publicKey.length,
                srvHello.getServerOrganizationPublicKey().toByteArray()
        ) != 0) {
            throw new NumenaLibraryException("Failing: Signature on handshake server identity key is not correct");
        }
        // Verify that the ServerHello.handshake was signed using the ServerHello.Handshake.server_identity_public_key
        if (Sodium.crypto_sign_verify_detached(
                srvHello.getHandshakeSignature().toByteArray(),
                handshake.toByteArray(),
                handshake.toByteArray().length,
                vm.getServerIdentityPublicKey()
        ) != 0) {
            throw new NumenaLibraryException("Failing: Handshake not signed");
        }
        // Verify that the ServerHello.handshake.timestamp_now is valid (within the last hour)
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        long secondsSinceEpoch = calendar.getTimeInMillis() / 1000L;
        long handshaketime = handshake.getTimestampNow();
        if (handshaketime < secondsSinceEpoch - 86400 || handshaketime > secondsSinceEpoch + 86400) {
            throw new NumenaLibraryException("Failing: Handshake timestamp is not valid");
        }
    }

    /**
     * Used for creating a handshake signature with the provided handshake and connectionkey
     * @param clientHandshake
     * @param clientConnectionPublicKey
     * @return
     * @throws NumenaLibraryException
     */

    public byte[] makeHandshakeSignature(Clienthello.ClientHello.Handshake clientHandshake, byte[] clientConnectionPublicKey) throws NumenaLibraryException{
        byte[] dstSignature = new byte[SodiumConstants.SIGNATURE_BYTES];
        int[] dstSignatureLength = new int[dstSignature.length];
        if (crypto_sign_detached(
                dstSignature,
                dstSignatureLength,
                clientHandshake.toByteArray(),
                clientHandshake.toByteArray().length,
                clientConnectionPublicKey) != 0) {
            throw new NumenaLibraryException("Failing: could not create signature.");
        }
        return dstSignature;
    }

    /**
     * Encrypts a signedHandshake
     * @param signedHandshake
     * @return
     * @throws NumenaLibraryException
     */

    public byte[] encryptSignedHandshake(Clienthello.ClientHello.SignedHandshake signedHandshake) throws NumenaLibraryException {
        ValuesManager vm = ValuesManager.getInstance();
        byte[] MESSAGE = signedHandshake.toByteArray();
        int MESSAGE_LEN = signedHandshake.getSerializedSize();
        int CIPHERTEXT_LEN = Constants.CRYPTO_BOX_MACBYTES + MESSAGE_LEN;
        byte[] ciphertext = new byte[CIPHERTEXT_LEN];

        // Set encrypted handshake
        byte[] nonce = new byte[SodiumConstants.NONCE_BYTES];
        if (crypto_box_easy(
                ciphertext,
                MESSAGE,
                MESSAGE_LEN,
                nonce,
                vm.getServerConnectionPublicKey(),
                vm.getClientConnectionSecretKey()
        ) != 0) {
            throw new NumenaLibraryException("Failing: Could not encrypt ciphertext");
        }

        return ciphertext;
    }

    /**
     * Used for encrypting an Appmessage
     * @param MESSAGE
     * @param MESSAGE_LEN
     * @param nonceCounter
     * @param publickey
     * @param secretkey
     * @return
     * @throws NumenaLibraryException
     */

    public byte[] encryptAppMessage(byte[] MESSAGE, int MESSAGE_LEN, int nonceCounter, byte[] publickey, byte[] secretkey) throws NumenaLibraryException {
        int CIPHERTEXT_LEN = Constants.CRYPTO_BOX_MACBYTES + MESSAGE_LEN;
        byte[] ciphertext = new byte[CIPHERTEXT_LEN];
        byte[] nonce;
        nonce = Utils.createNonceArray(nonceCounter);

        if (crypto_box_easy(
                ciphertext,
                MESSAGE,
                MESSAGE_LEN,
                nonce,
                publickey,
                secretkey) != 0) {
            throw new NumenaLibraryException("Failing: APPMESSAGE ENCRYPTION FAIL");
        }
        return ciphertext;
    }

    /**
     * Used for decrypting an appMessage
     * @param CIPHERTEXT
     * @param publickey
     * @param secretKey
     * @return
     */

    public byte[] decryptAppMessage(byte[] CIPHERTEXT, byte[] publickey, byte[] secretKey)  {
        byte[] decrypted = new byte[CIPHERTEXT.length - Constants.CRYPTO_BOX_MACBYTES];
        byte[] nonce;
        nonce = Utils.createNonceArray(0);
        if(crypto_box_open_easy(
                decrypted,
                CIPHERTEXT,
                CIPHERTEXT.length,
                nonce,
                publickey,
                secretKey) != 0) {
            return null;
        }

        return decrypted;
    }

    /**
     * Used for encrypting a message connection keys
     * @param MESSAGE
     * @param MESSAGE_LEN
     * @param nonceCounter
     * @return
     * @throws NumenaLibraryException
     */

    public byte[] encryptMessage(byte[] MESSAGE, int MESSAGE_LEN, int nonceCounter) throws NumenaLibraryException {
        int CIPHERTEXT_LEN = Constants.CRYPTO_BOX_MACBYTES + MESSAGE_LEN;
        ValuesManager valuesManager = ValuesManager.getInstance();
        byte[] ciphertext = new byte[CIPHERTEXT_LEN];
        byte[] nonce;
        nonce = Utils.createNonceArray(nonceCounter);

        if (crypto_box_easy(
                ciphertext,
                MESSAGE,
                MESSAGE_LEN,
                nonce,
                valuesManager.getServerConnectionPublicKey(),
                valuesManager.getClientConnectionSecretKey()) != 0) {
            throw new NumenaLibraryException("Failing: ENCRYPTION FAIL");
        }
        return ciphertext;
    }

    /**
     * Used for decrypting a message with connection keys
     * @param CIPHERTEXT
     * @return
     */

    public byte[] decryptMessage(byte[] CIPHERTEXT) {
        ValuesManager valuesManager = ValuesManager.getInstance();
        byte[] decrypted = new byte[CIPHERTEXT.length - Constants.CRYPTO_BOX_MACBYTES];
        byte[] nonce;
        nonce = Utils.createNonceArray(valuesManager.getRemoteNonce());
        crypto_box_open_easy(
                decrypted,
                CIPHERTEXT,
                CIPHERTEXT.length,
                nonce,
                valuesManager.getServerConnectionPublicKey(),
                valuesManager.getClientConnectionSecretKey()
        );

        return decrypted;
    }


}
