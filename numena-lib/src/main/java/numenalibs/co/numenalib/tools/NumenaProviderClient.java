package numenalibs.co.numenalib.tools;

import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import numenalibs.co.numenalib.database.DatabaseHelper;
import numenalibs.co.numenalib.exceptions.NumenaLibraryException;
import numenalibs.co.numenalib.models.NumenaKey;

import static numenalibs.co.numenalib.tools.NumenaProvider.NUMENAPROVIDER_URI;


public class NumenaProviderClient {

    private Context context;

    public NumenaProviderClient(Context context) {
        this.context = context;
    }

    public List<NumenaKey> lookupKeysFromProviders() {
        String tempAuth = null;
        Uri providerUri = null;
        List<NumenaKey> keys = new ArrayList<>();

        for (PackageInfo pack : context.getPackageManager().getInstalledPackages(PackageManager.GET_PROVIDERS)) {
            ProviderInfo[] providers = pack.providers;
            if (providers != null) {
                for (ProviderInfo provider : providers) {
                    if (provider.authority.contains("NumenaProvider") && !provider.authority.contains(context.getPackageName())) {
                        Log.d("Example", "provider: " + provider.authority);
                        tempAuth = provider.authority;
                        providerUri = Uri.parse("content://" + tempAuth + "/" + DatabaseHelper.TABLE_NAME);
                        try {
                            keys.clear();
                            keys.addAll(getAllKeysFromProvider(providerUri));
                        } catch (NumenaLibraryException e) {
                            Log.d("No keys found", e.getMessage());
                        }
                    }
                }
            }
        }
        return keys;
    }

    private List<NumenaKey> getAllKeysFromProvider(Uri uri) throws NumenaLibraryException {
        List<NumenaKey> keys = new ArrayList<>();
        try {
            Cursor c = context.getContentResolver().query(uri, null, null, null, null);
            if (c.moveToFirst()) {
                do {
                    String id = c.getString(0);
                    String keyName = c.getString(1);
                    byte[] keyValue = c.getBlob(2);
                    String keyHash = c.getString(3);
                    keys.add(new NumenaKey(id, keyName, keyHash, keyValue));
                } while (c.moveToNext());
            }
        } catch (Exception e) {
            throw new NumenaLibraryException("Failing: Could not lookup keys in database.");
        }
        return keys;
    }

}
