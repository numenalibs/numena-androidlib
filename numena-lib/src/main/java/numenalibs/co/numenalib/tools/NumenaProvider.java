package numenalibs.co.numenalib.tools;


import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import numenalibs.co.numenalib.NumenaLibDebug;
import numenalibs.co.numenalib.database.DatabaseHelper;

public class NumenaProvider extends ContentProvider {

    private DatabaseHelper databaseHelper;

    private static final String AUTH = "numenalibs.co.numenalib.tools.NumenaProvider";
    public static final Uri KEYS_URI = Uri.parse("content://" + AUTH + "/" + DatabaseHelper.TABLE_NAME);

    private final static int KEYS = 1;

    private final static UriMatcher uriMatcher;

    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(AUTH, DatabaseHelper.TABLE_NAME, KEYS);
    }

    public NumenaProvider(){

    }


    @Override
    public boolean onCreate() {
        return !Utils.numenaProviderExists(getContext());
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        return null;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }
}
