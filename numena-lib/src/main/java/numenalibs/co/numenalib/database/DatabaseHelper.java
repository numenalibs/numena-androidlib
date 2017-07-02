package numenalibs.co.numenalib.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


public class DatabaseHelper extends SQLiteOpenHelper {

    private static String DATABASE_NAME = "Numena.db";
    private static final String TABLE_NAME = "authentication_keys_table";
    private static final String COL_1 = "ID";
    private static final String COL_2 = "KEYVALUE";
    private static final String COL_3 = "KEYHASH";

    public DatabaseHelper(Context context, String name) {
        super(context, DATABASE_NAME, null, 1);
        SQLiteDatabase db = this.getWritableDatabase();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_NAME + "(ID INTEGER PRIMARY KEY AUTOINCREMENT, KEYVALUE BLOB, KEYHASH TEXT)");

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS" + TABLE_NAME);
        onCreate(db);
    }
}
