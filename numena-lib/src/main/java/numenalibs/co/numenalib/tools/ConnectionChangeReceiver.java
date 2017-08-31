package numenalibs.co.numenalib.tools;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.widget.Toast;

import numenalibs.co.numenalib.NumenaLibDebug;

public class ConnectionChangeReceiver extends BroadcastReceiver {

    public static boolean hasEthernetConnection;

    @Override
    public void onReceive(Context context, Intent intent) {
        hasEthernetConnection = isOnline(context);
        if(!hasEthernetConnection){
            BroadCaster.getBroadCaster().broadcastToObservers(Constants.RESETCONNECTIONVALUES);
        }else {
            Log.d("CONMANAGEr", "HAS CONNECTION");
        }

    }

    /**
     * Uses the connection service to check if a wifi or mobile network is in use.
     * @param context
     * @return
     */

    public boolean isOnline(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return (netInfo != null && netInfo.isConnected());
    }
}