package numenalibs.co.numenalib.tools;


import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class Utils {

    /**
     * Takes a hexString and formats it to a byteArray
     * @param s
     * @return
     */

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    /**
     * Creates a byte array from the long input
     * @param x
     * @return
     */

    public static byte[] createNonceArray(long x) {
        ByteBuffer buffer = ByteBuffer.allocate(24);
        buffer.putLong(x);
        return buffer.array();
    }

    /**
     * Formats a byte array in a string with ISO encoding.
     * @param value
     * @return
     */


    public static String formatWithIsoEncoding(byte[] value){
        try {
            return new String(value, Constants.ISO_ENCODING);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Formats a string with iso encoding to a byte array.
     * @param value
     * @return
     */

    public static byte[] formatBackIsoEncoding(String value){
        try {
            return value.getBytes(Constants.ISO_ENCODING);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     *
     * @param input
     * @return
     */

    public static String printByteArray(byte[] input) {
        if(input != null) {
            String[] output = new String[input.length];
            for (int i = 0; i < input.length; i++) {
                int positive = input[i] & 0xff;
                String hex = String.format("%02x", positive);
                output[i] = hex;
            }
            return Arrays.toString(output);
        }
        return Constants.EMPTY_INPUT;
    }

}
