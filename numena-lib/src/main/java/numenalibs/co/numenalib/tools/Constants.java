package numenalibs.co.numenalib.tools;


import org.libsodium.jni.Sodium;

public class Constants {

    /**
     * RANDOM CONSTANTS
     */

    public final static String MESSAGETYPE_CLIENTHELLO_HANDSHAKE = "ClientHello.Handshake";
    public final static String SHA256_ENCODING = "SHA-256";
    public static final int CRYPTO_BOX_MACBYTES = 16;
    public static final int EXPECTING_SERVERHELLO = 1;
    public static final int EXPECTING_MESSAGE = 2;
    public static final String RESPONSE_SUCCESS = "SUCCESIVE_CALL";
    public static final String RESPONSE_FAILURE = "FAILED_CALL";

    /**
     * KEYNAMES
     */

    public static final String SERVER_CONNECTION_PUBLICKEY = "SERVER_CONNECTION_PUBLICKEY";
    public static final String SERVER_IDENTITY_PUBLICKEY = "SERVER_IDENTITY_PUBLICKEY";
    public static final String CLIENT_CONNECTION_PUBLICKEY = "CLIENT_CONNECTION_PUBLICKEY";
    public static final String CLIENT_CONNECTION_SECRETKEY = "CLIENT_CONNECTION_SECRETKEY";
    public static final String CLIENT_IDENTITY_PUBLICKEY = "CLIENT_IDENTITY_PUBLICKEY";
    public static final String CLIENT_IDENTITY_SECRETKEY = "CLIENT_IDENTITY_SECRETKEY";
    public static final String LOCAL_NONCE = "LOCAL_NONCE";
    public static final String REMOTE_NONCE = "REMOTE_NONCE";
}
