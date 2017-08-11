package numenalibs.co.numenalib.tools;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class ConnectionChangeReceiver extends BroadcastReceiver {

    public static boolean hasEthernetConnection;

    @Override
    public void onReceive(Context context, Intent intent) {
        hasEthernetConnection = isOnline(context);
        if(!isOnline(context)){
            BroadCaster.getBroadCaster().broadcastToObservers(Constants.RESETCONNECTIONVALUES);
        }
    }

    public boolean isOnline(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return (netInfo != null && netInfo.isConnected());
    }
}