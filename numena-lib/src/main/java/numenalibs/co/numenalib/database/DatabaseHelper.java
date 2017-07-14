package numenalibs.co.numenalib.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import numenalibs.co.numenalib.models.NumenaKey;
import numenalibs.co.numenalib.tools.Constants;


public class DatabaseHelper extends SQLiteOpenHelper {

    private static String DATABASE_NAME = "Numena.db";
    public static final String TABLE_NAME = "authentication_keys_table";
    private static final String COL_1 = "ID";
    private static final String COL_2 = "KEYNAME";
    private static final String COL_3 = "KEYVALUE";
    private static final String COL_4 = "KEYHASH";
    private SQLiteDatabase db;

    public DatabaseHelper(Context context, String name) {
        super(context, DATABASE_NAME, null, 1);
        db = this.getWritableDatabase();

    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d("CREATING", " DATABASE");
        db.execSQL("CREATE TABLE " + TABLE_NAME + "(ID INTEGER PRIMARY KEY AUTOINCREMENT, KEYNAME TEXT, KEYVALUE BLOB, KEYHASH TEXT);");
    }


    public void insertValuesIntoKeys(String keyName, byte[] keyValue, String keyHash) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(COL_2, keyName);
        v.put(COL_3, keyValue);
        v.put(COL_4, keyHash);
        int count = db.update(TABLE_NAME, v, COL_2 + "=?", new String[]{keyName});
        if (count == 0) {
            db.insertWithOnConflict(TABLE_NAME, null, v, SQLiteDatabase.CONFLICT_REPLACE);
        }
        db.close();

    }

    public boolean identityKeysExists() {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_NAME + " WHERE KEYNAME = " + "'" + Constants.CLIENT_IDENTITY_PUBLICKEY + "'" + " OR KEYNAME = " + "'" + Constants.CLIENT_IDENTITY_SECRETKEY + "'" + ";";
        Cursor c = db.rawQuery(query, null);
        return c.getCount() >= 2;
    }

    public List<NumenaKey> getAllKeys() {
        SQLiteDatabase db = this.getWritableDatabase();
        List<NumenaKey> keys = new ArrayList<>();
        Cursor c = db.rawQuery("SELECT * FROM " + TABLE_NAME + ";", null);
        if (c.moveToFirst()) {
            do {
                //assing values
                String id = c.getString(0);
                String keyName = c.getString(1);
                byte[] keyValue = c.getBlob(2);
                String keyHash = c.getString(3);
                keys.add(new NumenaKey(id, keyName, keyHash, keyValue));
            } while (c.moveToNext());
        }
        db.close();
        return keys;
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS" + TABLE_NAME);
        onCreate(db);
    }
}
