package numenalibs.co.numenalib.protocol;


import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import org.libsodium.jni.Sodium;
import org.libsodium.jni.SodiumConstants;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.TimeZone;

import messages.Basemessage;
import messages.Clienthello;
import messages.Serverhello;
import numenalibs.co.numenalib.tools.ValuesManager;

import static org.libsodium.jni.Sodium.crypto_box_easy;
import static org.libsodium.jni.Sodium.crypto_box_keypair;
import static org.libsodium.jni.Sodium.crypto_sign_detached;

public class ProtocolManager {




    public boolean receiveServerhello(byte[] msg) throws NoSuchAlgorithmException, InvalidProtocolBufferException {
        // 1. Verify if message_type is ServerHello
        ValuesManager vm = ValuesManager.getInstance();
        Basemessage.BaseMessage basemessage = Basemessage.BaseMessage.parseFrom(msg);
        Basemessage.BaseMessage.Type msgtype = basemessage.getType();
        boolean isServerHello = msgtype.equals(Basemessage.BaseMessage.Type.SERVERHELLO);
        if (!isServerHello) {
            return false;
        }
        // 2. Verify if server_organization_public_key is in the list of whitelisted organization keys
        Serverhello.ServerHello srvHello = basemessage.getServerHello();
        Serverhello.ServerHello.Handshake handshake = srvHello.getHandshake();
        ByteString srvPubKey = handshake.getServerConnectionPublicKey();

        vm.setServerConnectionPublicKey(srvPubKey.toByteArray());
        if (!Arrays.equals(vm.getWhitelist(), srvHello.getServerOrganizationPublicKey().toByteArray())) {
            return false;
        }
        vm.getOrganisationKeys().put("numena", srvHello.getServerOrganizationPublicKey().toByteArray());
        // 3. Verify that the server_organization_public_key belongs to the intended server
        if (!Arrays.equals(vm.getOrganisationKeys().get("numena"), srvHello.getServerOrganizationPublicKey().toByteArray())) {
            return false;
        }
        // 4. Verify that the signature on the ServerHello.Handshake.server_identity_public_key is correct
        ByteString srvIdentPubKey = handshake.getServerIdentityPublicKey();

        vm.setServerIdentityPublicKey(srvIdentPubKey.toByteArray());

        ByteString srvOrganizationSignature = srvHello.getServerOrganizationSignature();
        if (Sodium.crypto_sign_verify_detached(
                srvOrganizationSignature.toByteArray(),
                vm.getServerIdentityPublicKey(),
                srvIdentPubKey.size(),
                srvHello.getServerOrganizationPublicKey().toByteArray()
        ) != 0) {
            return false;
        }
        // 5. Verify that the ServerHello.handshake was signed using the ServerHello.Handshake.server_identity_public_key
        if (Sodium.crypto_sign_verify_detached(
                srvHello.getHandshakeSignature().toByteArray(),
                handshake.toByteArray(),
                handshake.toByteArray().length,
                vm.getServerIdentityPublicKey()
        ) != 0) {
            return false;
        }
        // 6. Verify that the ServerHello.handshake.timestamp_now is valid (within the last hour)
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        long secondsSinceEpoch = calendar.getTimeInMillis() / 1000L;
        long handshaketime = handshake.getTimestampNow();
        if (handshaketime < secondsSinceEpoch - 86400 || handshaketime > secondsSinceEpoch + 86400) {
            return false;
        }
        return true;
    }



}
