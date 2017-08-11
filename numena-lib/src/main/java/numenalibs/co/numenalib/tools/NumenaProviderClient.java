package numenalibs.co.numenalib.tools;

import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import numenalibs.co.numenalib.models.NumenaKey;

import static numenalibs.co.numenalib.tools.NumenaProvider.NUMENAPROVIDER_URI;


public class NumenaProviderClient {

    private Context context;

    public NumenaProviderClient(Context context){
        this.context = context;
    }

    public List<NumenaKey> getAllKeysFromProvider() {
        List<NumenaKey> keys = new ArrayList<>();
        Cursor c = context.getContentResolver().query(NUMENAPROVIDER_URI,null,null,null,null);
        if (c.moveToFirst()) {
            do {
                String id = c.getString(0);
                String keyName = c.getString(1);
                byte[] keyValue = c.getBlob(2);
                String keyHash = c.getString(3);
                keys.add(new NumenaKey(id, keyName, keyHash, keyValue));
            } while (c.moveToNext());
        }
        return keys;
    }

}
