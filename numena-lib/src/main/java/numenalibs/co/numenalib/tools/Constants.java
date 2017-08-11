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
    public static final int CONTACTTYPE_ADD = 5;
    public static final int CONTACTTYPE_REMOVE = 6;
    public static final String URL_DEV = "ws://dev.numena.co:8000";
    public static final String WHITELISTTEXT = "6c7771fdc6d83b641ad4e994a9c9bdb6e786d1b5e6953eaf1fa63b0223c156e4";
    public static final String NUMENAORG = "Numena";
    public static final String NUMENADEBUGLABEL ="NUMENADEBUG";
    public static final int SOCKETCONNECTTIMEOUT = 15000;
    public static final int MAXMESSAGEPAYLOADSIZE = 10000000;
    public static final int MAXFRAMEPAYLOADSIZE = 10000000;
    public static final String NO_CONNECTION_AVAILABLE = "NOT CONNECTED TO SOCKET";
    public static final String PREF_PACKAGE_NAME= "numenalibs.co.numenalib";
    public static final String SHARED_PREF_NAME= "numenakeyvalues";
    public static final String SHARED_VALUE_IDKEY_PUBLIC= "idkey_public";
    public static final String SHARED_VALUE_IDKEY_SECRET= "idkey_secret";


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

    /**
     * BROADCASTCODES
     */
    public static final String BROADCASTCODE = "BROADCASTCODE";
    public static final int EXECUTEWORKERTHREAD = 123;
    public static final int RESETCONNECTIONVALUES = 124;
}
