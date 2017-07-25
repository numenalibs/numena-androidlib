package numenalibs.co.numenalib;

import android.util.Log;

import numenalibs.co.numenalib.tools.Constants;

public class NumenaLibDebug {

    private static final String TAG = Constants.NUMENADEBUGLABEL;

    public static void d(String message){
        Log.d(TAG,message);
    }
}
