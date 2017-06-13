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
import messages.Basemessage.BaseMessage;
import messages.Clienthello;
import messages.Serverhello.ServerHello;
import messages.Serverhello.ServerHello.Handshake;
import numenalibs.co.numenalib.exceptions.NumenaLibraryException;
import numenalibs.co.numenalib.tools.ValuesManager;

import static org.libsodium.jni.Sodium.crypto_box_easy;
import static org.libsodium.jni.Sodium.crypto_box_keypair;
import static org.libsodium.jni.Sodium.crypto_sign_detached;

public class ProtocolManager {


    public ServerHello extractServerHello(byte[] msg) throws NumenaLibraryException {
        ServerHello serverHello = parseServerHello(msg);
        setKeysFromServerHello(serverHello);
        return serverHello;

    }

    private void setKeysFromServerHello(ServerHello srvHello) throws NumenaLibraryException{
        ValuesManager vm = ValuesManager.getInstance();
        ServerHello.Handshake handshake = srvHello.getHandshake();
        ByteString srvPubKey = handshake.getServerConnectionPublicKey();

        vm.setServerConnectionPublicKey(srvPubKey.toByteArray());
        if (!Arrays.equals(vm.getWhitelist(), srvHello.getServerOrganizationPublicKey().toByteArray())) {
            throw new NumenaLibraryException("Failing: No match in whitelist");
        }
        vm.getOrganisationKeys().put("numena", srvHello.getServerOrganizationPublicKey().toByteArray());
        // 3. Verify that the server_organization_public_key belongs to the intended server
        if (!Arrays.equals(vm.getOrganisationKeys().get("numena"), srvHello.getServerOrganizationPublicKey().toByteArray())) {
            throw new NumenaLibraryException("Failing: no matching organisationkey for organisation");
        }
        // 4. Verify that the signature on the ServerHello.Handshake.server_identity_public_key is correct
        ByteString srvIdentPubKey = handshake.getServerIdentityPublicKey();
        vm.setServerIdentityPublicKey(srvIdentPubKey.toByteArray());
    }

    public ServerHello parseServerHello(byte[] msg) throws NumenaLibraryException {
        BaseMessage basemessage = null;
        ServerHello srvHello = null;
        try {
            basemessage = BaseMessage.parseFrom(msg);
            BaseMessage.Type msgtype = basemessage.getType();
            boolean isServerHello = msgtype.equals(Basemessage.BaseMessage.Type.SERVERHELLO);
            if (!isServerHello) {
                throw new NumenaLibraryException("Failing to parse Numena Serverhello. Reason: not correct msg type");
            }
            // 2. Verify if server_organization_public_key is in the list of whitelisted organization keys
            srvHello = basemessage.getServerHello();
            return srvHello;

        } catch (InvalidProtocolBufferException e) {
            throw new NumenaLibraryException("Failing to parse msg as BaseMessage");
        }
    }
}
