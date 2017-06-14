package numenalibs.co.numenalib.encryption;


import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

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
import numenalibs.co.numenalib.tools.ValuesManager;

import static org.libsodium.jni.Sodium.crypto_box_easy;
import static org.libsodium.jni.Sodium.crypto_sign_detached;

public class EncryptionManager {

    public void verifyServerhello(ServerHello srvHello, Handshake handshake) throws NumenaLibraryException {
        ValuesManager vm = ValuesManager.getInstance();
        ByteString srvOrganizationSignature = srvHello.getServerOrganizationSignature();
        if (Sodium.crypto_sign_verify_detached(
                srvOrganizationSignature.toByteArray(),
                vm.getServerIdentityPublicKey(),
                vm.getServerIdentityPublicKey().length,
                srvHello.getServerOrganizationPublicKey().toByteArray()
        ) != 0) {
            throw new NumenaLibraryException("Failing: Signature on handshake server identity key is not correct");
        }
        // 5. Verify that the ServerHello.handshake was signed using the ServerHello.Handshake.server_identity_public_key
        if (Sodium.crypto_sign_verify_detached(
                srvHello.getHandshakeSignature().toByteArray(),
                handshake.toByteArray(),
                handshake.toByteArray().length,
                vm.getServerIdentityPublicKey()
        ) != 0) {
            throw new NumenaLibraryException("Failing: Handshake not signed");
        }
        // 6. Verify that the ServerHello.handshake.timestamp_now is valid (within the last hour)
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        long secondsSinceEpoch = calendar.getTimeInMillis() / 1000L;
        long handshaketime = handshake.getTimestampNow();
        if (handshaketime < secondsSinceEpoch - 86400 || handshaketime > secondsSinceEpoch + 86400) {
            throw new NumenaLibraryException("Failing: Handshake timestamp is not valid");
        }
    }

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

    public byte[] encryptCipherText(Clienthello.ClientHello.SignedHandshake signedHandshake) throws NumenaLibraryException {
        ValuesManager vm = ValuesManager.getInstance();
        byte[] MESSAGE = signedHandshake.toByteArray();
        int MESSAGE_LEN = signedHandshake.getSerializedSize();
        int CIPHERTEXT_LEN = Constants.CRYPTO_BOX_MACBYTES + MESSAGE_LEN;
        byte[] ciphertext = new byte[CIPHERTEXT_LEN];

        // 8.4 Set encrypted handshake
        byte[] nonce = new byte[SodiumConstants.NONCE_BYTES];
        /*
         Remember! When testing locally, change srv_connection_pk to client_connection_pk.
         crypto_box_easy(ciphertext, MESSAGE, MESSAGE_LEN, nonce,
                    bob_publickey, alice_secretkey).
         Bob is the server. Alice is us.
          */
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
}
