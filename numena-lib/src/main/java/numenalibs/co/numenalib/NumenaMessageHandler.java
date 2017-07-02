package numenalibs.co.numenalib;


import android.util.Log;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import org.libsodium.jni.NaCl;
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
import messages.Statusmessage;
import numenalibs.co.numenalib.encryption.EncryptionManager;
import numenalibs.co.numenalib.exceptions.NumenaLibraryException;
import numenalibs.co.numenalib.interfaces.ResultsListener;
import numenalibs.co.numenalib.models.NumenaMethod;
import numenalibs.co.numenalib.networking.SingleMessageManager;
import numenalibs.co.numenalib.protocol.ProtocolManager;
import numenalibs.co.numenalib.tools.Constants;
import numenalibs.co.numenalib.tools.Utils;
import numenalibs.co.numenalib.tools.ValuesManager;

import static org.libsodium.jni.Sodium.crypto_box_easy;
import static org.libsodium.jni.Sodium.crypto_box_keypair;
import static org.libsodium.jni.Sodium.crypto_sign_detached;


public class NumenaMessageHandler {

    private ProtocolManager protocolManager;
    private EncryptionManager encryptionManager;
    private SingleMessageManager singleMessageManager;
    public static final int crypto_box_MACBYTES = 16;
    public long publickeylen;
    public long privatekeylen;

    public NumenaMessageHandler() {
        protocolManager = new ProtocolManager();
        encryptionManager = new EncryptionManager();
        singleMessageManager = new SingleMessageManager();
        setupKeys();

    }

    public Serverhello.ServerHello handleServerHello(byte[] msg) throws NumenaLibraryException {
        Serverhello.ServerHello serverHello = protocolManager.extractServerHello(msg);
        Serverhello.ServerHello.Handshake handshake = serverHello.getHandshake();
        encryptionManager.verifyServerhello(serverHello, handshake);
        return serverHello;
    }

    public Basemessage.BaseMessage buildClientHello(byte[] msg, Serverhello.ServerHello serverHello) throws NumenaLibraryException {
        ValuesManager valuesManager = ValuesManager.getInstance();
        protocolManager.createClientConnectionKeys();
        Clienthello.ClientHello.Handshake handshake = protocolManager.buildClientHelloHandshake(serverHello);
        byte[] dstSignature = null;
        if (valuesManager.isConnectionToOrganizationServer()) {
            dstSignature = encryptionManager.makeHandshakeSignature(handshake, valuesManager.getClientConnectionPublicKey());
        }
        Clienthello.ClientHello.SignedHandshake signedHandshake = protocolManager.buildSignedHandshake(handshake, dstSignature);
        byte[] cipherText = encryptionManager.encryptCipherText(signedHandshake);
        Basemessage.BaseMessage baseMessage = protocolManager.packClientHello(cipherText);
        return baseMessage;
    }

    private void setupKeys() {
        Sodium sodium = NaCl.sodium();
        ValuesManager vm = ValuesManager.getInstance();
        long publickeylen = Sodium.crypto_sign_publickeybytes();
        long privatekeylen = Sodium.crypto_sign_secretkeybytes();
        byte[] DEVICE_IDENTITY_PK = new byte[(int) publickeylen];
        byte[] DEVICE_IDENTITY_SK = new byte[(int) privatekeylen];
        Sodium.randombytes(DEVICE_IDENTITY_PK, (int) publickeylen);
        Sodium.randombytes(DEVICE_IDENTITY_SK, (int) privatekeylen);
        Sodium.crypto_sign_keypair(DEVICE_IDENTITY_PK, DEVICE_IDENTITY_SK);
        vm.setClientIdentityPublicKey(DEVICE_IDENTITY_PK);
        vm.setClientIdentitySecretKey(DEVICE_IDENTITY_SK);
    }


    public void initConnection() {
        ResultsListener listener = setupListenerAndCallback(new MyCallback());
        singleMessageManager.setListener(listener);
        singleMessageManager.openWebsocket();
    }

    class MyCallback extends NumenaMethod {

        @Override
        public Void call() {
            byte[] result = (byte[]) getResult();
            handleResult(result);
            Log.d("Done", "RESULT" + getResult());
            return null;
        }
    }

    class MyCallback2 extends NumenaMethod {

        @Override
        public Void call() {
            byte[] result = (byte[]) getResult();
            checkStatus(result);
            return null;
        }
    }

    private void checkStatus(byte[] msg) {
        Log.d("CHECKSTATUS", Utils.printByteArray(msg));
        byte[] decryptedmsg = encryptionManager.decrypt_message(msg);
        Basemessage.BaseMessage basemessage = null;
        try {
            basemessage = Basemessage.BaseMessage.parseFrom(decryptedmsg);
            Basemessage.BaseMessage.Type msgtype = basemessage.getType();
            Log.d("CALLBACK2", "ASDOKASODKASOD");
            if (msgtype == Basemessage.BaseMessage.Type.STATUS) {
                Statusmessage.StatusMessage status = basemessage.getStatus();
                long code = status.getStatusCode();
                Log.d("code", "value: " + code);
            }
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }
    }

    private void handleResult(byte[] result) {
        Serverhello.ServerHello serverHello = null;
        try {
            receiveServerhello(result);
            Basemessage.BaseMessage baseMessage = packClientHello(result, false);
            ValuesManager valuesManager = ValuesManager.getInstance();
            byte[] msgEnc = encryptionManager.encryptMessage(baseMessage.toByteArray(), baseMessage.getSerializedSize(), valuesManager.getLocalNonce());
            ResultsListener listener = setupListenerAndCallback(new MyCallback2());
            singleMessageManager.setListener(listener);
            singleMessageManager.sendBinary(baseMessage.toByteArray());
        } catch (NumenaLibraryException e) {
            e.printStackTrace();
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    private ResultsListener setupListenerAndCallback(final NumenaMethod numenaMethod) {
        ResultsListener openingListener = new ResultsListener<byte[]>() {
            @Override
            public void onSuccess(byte[] result) {
                try {
                    numenaMethod.setResult(result);
                    numenaMethod.call();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Throwable e, String response) {

            }
        };
        return openingListener;
    }


    public Basemessage.BaseMessage packClientHello(byte[] srv_bytes, boolean isConnectionToOrganizationServer) throws NumenaLibraryException, NoSuchAlgorithmException, InvalidProtocolBufferException {

        ValuesManager valuesManager = ValuesManager.getInstance();
        // 7. Generate a ClientHello.Handshake message and populate the fields
        Clienthello.ClientHello.Handshake.Builder client_handshake_builder = Clienthello.ClientHello.Handshake.newBuilder();
        // 7.1 Message type
        client_handshake_builder.setMessageType("ClientHello.Handshake");
        // 7.2 client_connection_public_key

        byte[] tempclient_connection_pk = new byte[Sodium.crypto_box_publickeybytes()];
        byte[] tempclient_connection_sk = new byte[Sodium.crypto_box_secretkeybytes()];
        if (crypto_box_keypair(tempclient_connection_pk, tempclient_connection_sk) != 0) {
            throw new NumenaLibraryException("You done goof");
        }
        valuesManager.setClientConnectionPublicKey(tempclient_connection_pk);
        valuesManager.setClientConnectionSecretKey(tempclient_connection_sk);
        ByteString client_connection_pk = ByteString.copyFrom(valuesManager.getClientConnectionPublicKey());

        client_handshake_builder.setClientConnectionPublicKey(client_connection_pk);
        // 7.3 Hashed server hello
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] srvHello = Basemessage.BaseMessage.parseFrom(srv_bytes).getServerHello().toByteArray();
        md.update(srvHello);
        ByteString hashed_srv_bytes = ByteString.copyFrom(md.digest());
        client_handshake_builder.setHashedServerHello(hashed_srv_bytes);
        // 7.4 Length of server hello
        client_handshake_builder.setLengthOfServerHello(srvHello.length);
        // 8. Create a ClientHello.SignedHandshake
        Clienthello.ClientHello.SignedHandshake.Builder signed_handshake_builder = Clienthello.ClientHello.SignedHandshake.newBuilder();
        // 8.1 Set handshake
        Clienthello.ClientHello.Handshake client_handshake = client_handshake_builder.build();
        signed_handshake_builder.setHandshake(client_handshake);
        byte[] dst_signature = new byte[SodiumConstants.SIGNATURE_BYTES];
        int[] dst_signature_length = new int[dst_signature.length];
        // 8.2 Check if we're communicating with organization server
        if (isConnectionToOrganizationServer) {
            Log.d("STUFF", "ISCONNECTIONTO");
            // 8.2.1 Set identity_public_key to device_identity_key
            signed_handshake_builder.setIdentityPublicKey(ByteString.copyFrom(valuesManager.getClientIdentityPublicKey()));
            if (crypto_sign_detached(
                    dst_signature,
                    dst_signature_length,
                    client_handshake.toByteArray(),
                    client_handshake.toByteArray().length,
                    valuesManager.getClientConnectionPublicKey()) != 0) {
                throw new NumenaLibraryException("SIGN FAIL");
            }
            // 8.2.1 Set handshake signature
            signed_handshake_builder.setHandshakeSignature(ByteString.copyFrom(dst_signature));
        }
        Clienthello.ClientHello.SignedHandshake signedHandshake = signed_handshake_builder.build();
        byte[] MESSAGE = signedHandshake.toByteArray();
        int MESSAGE_LEN = signedHandshake.getSerializedSize();
        int CIPHERTEXT_LEN = crypto_box_MACBYTES + MESSAGE_LEN;
        byte[] ciphertext = new byte[CIPHERTEXT_LEN];
        Clienthello.ClientHello.Builder client_hello_builder = Clienthello.ClientHello.newBuilder();
        // 8.3 Set client connection public key
        client_hello_builder.setClientConnectionPublicKey(ByteString.copyFrom(valuesManager.getClientConnectionPublicKey()));
        // 8.4 Set encrypted handshake
        byte[] nonce = new byte[SodiumConstants.NONCE_BYTES];
//        valuesManager.setServerConnectionPublicKey(Basemessage.BaseMessage.parseFrom(srv_bytes).getServerHello()
//                .getHandshake().getServerConnectionPublicKey().toByteArray());
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
                valuesManager.getServerConnectionPublicKey(),
                valuesManager.getClientConnectionSecretKey()
        ) != 0) {
            throw new NumenaLibraryException("FAILLLLSLSL");
        }
        client_hello_builder.setEncryptedHandshake(ByteString.copyFrom(ciphertext));
        Clienthello.ClientHello client_hello = client_hello_builder.build();
        Basemessage.BaseMessage.Builder basemessage_builder = Basemessage.BaseMessage.newBuilder();
        basemessage_builder.setType(Basemessage.BaseMessage.Type.CLIENTHELLO);
        basemessage_builder.setClientHello(client_hello);
        return basemessage_builder.build();
    }

    public boolean receiveServerhello(byte[] msg) throws NoSuchAlgorithmException, InvalidProtocolBufferException {
        ValuesManager valuesManager = ValuesManager.getInstance();
        Log.d("WHITELIST", "LOL " );
        // 1. Verify if message_type is ServerHello
        Basemessage.BaseMessage basemessage = Basemessage.BaseMessage.parseFrom(msg);
        Basemessage.BaseMessage.Type msgtype = basemessage.getType();
        Log.d("WHITELIST", "LOL " + Utils.printByteArray(valuesManager.getWhitelist()));
        boolean isServerHello = msgtype.equals(Basemessage.BaseMessage.Type.SERVERHELLO);
        if (!isServerHello) {
            return false;
        }
        // 2. Verify if server_organization_public_key is in the list of whitelisted organization keys
        Serverhello.ServerHello srvHello = basemessage.getServerHello();
        Serverhello.ServerHello.Handshake handshake = srvHello.getHandshake();
        ByteString srvPubKey = handshake.getServerConnectionPublicKey();
        valuesManager.setServerConnectionPublicKey(srvPubKey.toByteArray());
        if (!Arrays.equals(valuesManager.getWhitelist(), srvHello.getServerOrganizationPublicKey().toByteArray())) {
            return false;
        }
        // 4. Verify that the signature on the ServerHello.Handshake.server_identity_public_key is correct
        ByteString srvIdentPubKey = handshake.getServerIdentityPublicKey();

        valuesManager.setServerIdentityPublicKey(srvIdentPubKey.toByteArray());

        ByteString srvOrganizationSignature = srvHello.getServerOrganizationSignature();
        if (Sodium.crypto_sign_verify_detached(
                srvOrganizationSignature.toByteArray(),
                valuesManager.getServerIdentityPublicKey(),
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
                valuesManager.getServerIdentityPublicKey()
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
