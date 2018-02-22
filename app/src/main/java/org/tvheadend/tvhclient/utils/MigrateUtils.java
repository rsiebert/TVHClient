package org.tvheadend.tvhclient.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import org.tvheadend.tvhclient.BuildConfig;
import org.tvheadend.tvhclient.data.AppDatabase;
import org.tvheadend.tvhclient.data.dao.ConnectionDao;
import org.tvheadend.tvhclient.data.entity.Connection;

import java.util.ArrayList;
import java.util.List;

public class MigrateUtils {
    private static final String TAG = MigrateUtils.class.getSimpleName();
    private static final int VERSION_101 = 101;

    private MigrateUtils() {
        throw new IllegalAccessError("Utility class");
    }

    public static void doMigrate(Context context) {
        // Lookup the current version and the last migrated version
        int currentApplicationVersion = BuildConfig.BUILD_VERSION;
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        int lastInstalledApplicationVersion = sharedPreferences.getInt("build_version_for_migration", 0);
        Log.i(TAG, "Migrating from " + lastInstalledApplicationVersion + " to " + currentApplicationVersion);

        if (currentApplicationVersion != lastInstalledApplicationVersion) {
            if (lastInstalledApplicationVersion < VERSION_101) {
                migrateConvertStartScreenPreference(context);
                migrateConnectionsFromDatabase(context);
            }
        }

        // Store the current version as the last installed version
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("build_version_for_migration", currentApplicationVersion);
        editor.apply();
    }

    private static void migrateConnectionsFromDatabase(Context context) {
        Log.d(TAG, "migrateConnectionsFromDatabase() called with: context = [" + context + "]");
        SQLiteDatabase db = DatabaseHelper.getInstance(context).getReadableDatabase();

        List<Connection> connectionList = new ArrayList<>();

        // Save the connection credentials in a file and drop the database
        try {
            Log.d(TAG, "migrateConnectionsFromDatabase: database is open " + db.isOpen());
            Cursor c = db.rawQuery("SELECT * FROM connections", null);
            while (c.moveToNext()) {
                Connection connection = new Connection();
                connection.setId(c.getInt(c.getColumnIndex("_id")));
                connection.setName(c.getString(c.getColumnIndex("name")));
                connection.setHostname(c.getString(c.getColumnIndex("address")));
                connection.setPort(c.getInt(c.getColumnIndex("port")));
                connection.setUsername(c.getString(c.getColumnIndex("username")));
                connection.setPassword(c.getString(c.getColumnIndex("password")));
                connection.setActive((c.getInt(c.getColumnIndex("selected")) > 0));
                connection.setStreamingPort(c.getInt(c.getColumnIndex("streaming_port")));
                connection.setWolMacAddress(c.getString(c.getColumnIndex("wol_address")));
                connection.setWolPort(c.getInt(c.getColumnIndex("wol_port")));
                connection.setWolUseBroadcast((c.getInt(c.getColumnIndex("wol_broadcast")) > 0));
                connection.setWolEnabled((!TextUtils.isEmpty(connection.getWolMacAddress())));

                Log.d(TAG, "migrateConnectionsFromDatabase: Added existing connection " + connection.getName());
                connectionList.add(connection);
            }
            c.close();
        } catch (SQLiteException ex) {
            Log.d(TAG, "migrateConnectionsFromDatabase: error executing query " + ex.getLocalizedMessage());
        }
        db.close();

        // delete the entire database so we can restart with version one in room
        Log.d(TAG, "migrateConnectionsFromDatabase: deleting old database");
        context.deleteDatabase("tvhclient");

        if (connectionList.size() > 0) {
            Log.d(TAG, "migrateConnectionsFromDatabase: getting room db");
            AppDatabase roomDb = AppDatabase.getInstance(context);

            Log.d(TAG, "migrateConnectionsFromDatabase: adding old connections to room db");
            new MigrateConnectionsTask(roomDb.connectionDao(), connectionList).execute();
        }
    }

    private static class MigrateConnectionsTask extends AsyncTask<Void, Void, Void> {
        private final ConnectionDao dao;
        private final List<Connection> list;

        MigrateConnectionsTask(ConnectionDao connectionDao, List<Connection> connectionList) {
            this.dao = connectionDao;
            this.list = connectionList;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            dao.insertAll(list);
            return null;
        }
    }
    private static void migrateConvertStartScreenPreference(Context context) {
        Log.d(TAG, "migrateConvertStartScreenPreference() called with: context = [" + context + "]");
        // migrate preferences from old names to new
        // names to have a consistent naming scheme afterwards
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        try {
            int value = Integer.valueOf(sharedPreferences.getString("defaultMenuPositionPref", "0"));
            if (value > 8) {
                // If the value is anything above the status screen
                // menu entry, default to the channel screen
                value = 0;
            } else if (value == 7) {
                // The program guide screen moved from position 7 to 1
                value = 1;
            } else if (value != 0) {
                // If any screens except the channel and program guide
                // were set increase the value by one because the
                // program guide moved to position 2
                value++;
            }

            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("defaultMenuPositionPref", String.valueOf(value));
            editor.apply();
        } catch (NumberFormatException e) {
            // NOP
        }
    }

    private static class DatabaseHelper extends SQLiteOpenHelper {

        private static DatabaseHelper instance = null;

        public static DatabaseHelper getInstance(Context ctx) {
            Log.d(TAG, "getInstance() called with: ctx = [" + ctx + "]");
            if (instance == null)
                instance = new DatabaseHelper(ctx);
            return instance;
        }

        private DatabaseHelper(Context context) {
            super(context, "tvhclient", null, 9);
            Log.d(TAG, "DatabaseHelper() called with: context = [" + context + "]");
        }

        @Override
        public void onCreate(SQLiteDatabase sqLiteDatabase) {
            Log.d(TAG, "onCreate() called with: sqLiteDatabase = [" + sqLiteDatabase + "]");

        }

        @Override
        public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
            Log.d(TAG, "onUpgrade() called with: sqLiteDatabase = [" + sqLiteDatabase + "], i = [" + i + "], i1 = [" + i1 + "]");

        }
    }
}
