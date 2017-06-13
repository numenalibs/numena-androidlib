package numenalibs.co.numenalib.encryption;


import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import org.libsodium.jni.Sodium;

import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.TimeZone;

import messages.Serverhello.ServerHello;
import messages.Serverhello.ServerHello.Handshake;
import numenalibs.co.numenalib.exceptions.NumenaLibraryException;
import numenalibs.co.numenalib.tools.ValuesManager;

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
}
