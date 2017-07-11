package numenalibs.co.numenalib.tools;


import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.util.Log;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class Utils {

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    public static byte[] createNonceArray(long x) {
        ByteBuffer buffer = ByteBuffer.allocate(24);
        buffer.putLong(x);
        return buffer.array();
    }

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
        return " EMPTY INPUT ";
    }

    public static boolean numenaProviderExists(Context context){
        boolean found = false;
        for (PackageInfo pack : context.getPackageManager().getInstalledPackages(PackageManager.GET_PROVIDERS)) {
            ProviderInfo[] providers = pack.providers;
            if (providers != null) {
                for (ProviderInfo provider : providers) {
                    Log.d("Example", "provider: " + provider.authority);
                    found = provider.authority.equals("numenalibs.co.numenalib.tools.NumenaProvider");
                    if(found) break;
                }
            }
            if (found) break;
        }
        return found;
    }

}
