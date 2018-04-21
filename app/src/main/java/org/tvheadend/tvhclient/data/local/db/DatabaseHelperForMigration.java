package org.tvheadend.tvhclient.data.local.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Helper class to get access to the old SQLite database.
 * Required to move the old connection information to the new room database
 */
public class DatabaseHelperForMigration extends SQLiteOpenHelper {

    private static DatabaseHelperForMigration instance = null;

    public static DatabaseHelperForMigration getInstance(Context ctx) {
        if (instance == null) {
            instance = new DatabaseHelperForMigration(ctx);
        }
        return instance;
    }

    private DatabaseHelperForMigration(Context context) {
        super(context, "tvhclient", null, 9);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        // NOP
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        // NOP
    }
}
