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

import static numenalibs.co.numenalib.tools.Constants.NUMENA_PROVIDER;
import static numenalibs.co.numenalib.tools.NumenaProvider.NUMENAPROVIDER_URI;


public class NumenaProviderClient {

    private Context context;

    public NumenaProviderClient(Context context) {
        this.context = context;
    }

    /**
     * Extracts a list of identitykeys from the closest provider with NumenaProvider in its' name.
     * @return
     */

    public List<NumenaKey> lookupKeysFromProviders() {
        String tempAuth = null;
        Uri providerUri = null;
        List<NumenaKey> keys = new ArrayList<>();

        for (PackageInfo pack : context.getPackageManager().getInstalledPackages(PackageManager.GET_PROVIDERS)) {
            ProviderInfo[] providers = pack.providers;
            if (providers != null) {
                for (ProviderInfo provider : providers) {
                    if (provider.authority.contains(NUMENA_PROVIDER) && !provider.authority.contains(context.getPackageName())) {
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

    /**
     * Looks up a ContentResolver on the given Uri and extracts data to a list of numenakeys.
     * Throws a numenalibraryexception if there is a problem with resolving the content provider
     * on the given Uri.
     * @param uri
     * @return
     * @throws NumenaLibraryException
     */

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
