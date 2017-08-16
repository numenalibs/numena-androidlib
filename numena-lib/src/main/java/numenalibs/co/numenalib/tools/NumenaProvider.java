package numenalibs.co.numenalib.tools;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import numenalibs.co.numenalib.database.DatabaseHelper;

public class NumenaProvider extends ContentProvider {


    private DatabaseHelper dataBaseHelper = null;
    private static final String AUTH = "numenalibs.co.numenalib.tools.NumenaProvider";
    public static final Uri NUMENAPROVIDER_URI = Uri.parse("content://" + AUTH + "/" + DatabaseHelper.TABLE_NAME);
    final static int KEYS = 1;

    SQLiteDatabase db;

    private static final UriMatcher uriMatcher;

    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(AUTH, DatabaseHelper.TABLE_NAME, KEYS);
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public boolean onCreate() {
        Context context = getContext();
        dataBaseHelper = new DatabaseHelper(context, "");
        return true;
    }

    /**
     * Looks up all keys belonging to the apps database and returns the cursor
     * @param uri
     * @param projection
     * @param selection
     * @param selectionArgs
     * @param sortOrder
     * @return
     */

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return dataBaseHelper.getCursorOfKeys();
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

}
