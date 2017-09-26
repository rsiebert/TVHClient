package org.tvheadend.tvhclient;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import org.tvheadend.tvhclient.data.DataContract;

public class DatabaseHelper extends SQLiteOpenHelper {
	private final static String TAG = DatabaseHelper.class.getSimpleName();
    
    // Database version and name declarations
    private static final int DATABASE_VERSION = 9;
    private static final String DATABASE_NAME = "tvhclient";

    private static DatabaseHelper mInstance = null;
    
    public static synchronized DatabaseHelper getInstance(Context context) {
        if (mInstance == null)
            mInstance = new DatabaseHelper(context);
        return mInstance;
    }

    private DatabaseHelper(Context context) {
    	super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * Called when the database is created for the very first time.
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
    	String query = "CREATE TABLE IF NOT EXISTS " + DataContract.Connections.TABLE + " ("
                + DataContract.Connections.ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + DataContract.Connections.NAME + " TEXT NOT NULL,"
                + DataContract.Connections.ADDRESS + " TEXT NOT NULL, "
                + DataContract.Connections.PORT + " INT DEFAULT 9982, "
                + DataContract.Connections.USERNAME + " TEXT NULL, "
                + DataContract.Connections.PASSWORD + " TEXT NULL, "
                + DataContract.Connections.SELECTED + " INT NOT NULL, "
                + DataContract.Connections.CHANNEL_TAG + " INT DEFAULT 0, "
                + DataContract.Connections.STREAMING_PORT + " INT DEFAULT 9981, "
                + DataContract.Connections.WOL_ADDRESS + " TEXT NULL, "
                + DataContract.Connections.WOL_PORT + " INT DEFAULT 9, "
                + DataContract.Connections.WOL_BROADCAST + " INT DEFAULT 0, "
    	        + DataContract.Connections.PLAY_PROFILE_ID + " INT DEFAULT 0, "
                + DataContract.Connections.REC_PROFILE_ID + " INT DEFAULT 0,"
                + DataContract.Connections.CAST_PROFILE_ID + " INT DEFAULT 0);";
        db.execSQL(query);
        
        query = "CREATE TABLE IF NOT EXISTS " + DataContract.Profiles.TABLE + " ("
                + DataContract.Profiles.ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + DataContract.Profiles.ENABLED + " INT DEFAULT 0, "
                + DataContract.Profiles.UUID + " TEXT NULL, "
                + DataContract.Profiles.NAME + " TEXT NULL, "
                + DataContract.Profiles.CONTAINER + " TEXT NULL, "
                + DataContract.Profiles.TRANSCODE + " INT DEFAULT 0, "
                + DataContract.Profiles.RESOLUTION + " TEXT NULL, "
                + DataContract.Profiles.AUDIO_CODEC + " TEXT NULL, "
                + DataContract.Profiles.VIDEO_CODEC + " TEXT NULL, "
                + DataContract.Profiles.SUBTITLE_CODEC + " TEXT NULL);";
        db.execSQL(query);
    }

    /**
     * Called when the database version has changed.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "onUpgrade() called with: db = [" + db + "], oldVersion = [" + oldVersion + "], newVersion = [" + newVersion + "]");

        if (oldVersion < newVersion && newVersion == 2) {
            // Add the channel tag column in database version 2 
            db.execSQL("ALTER TABLE " + DataContract.Connections.TABLE + " ADD COLUMN " + DataContract.Connections.CHANNEL_TAG
                    + " INT DEFAULT 0;");
        }
        if (oldVersion < newVersion && newVersion == 3) {
            // Add the streaming port column in database version 3 
            db.execSQL("ALTER TABLE " + DataContract.Connections.TABLE + " ADD COLUMN " + DataContract.Connections.STREAMING_PORT
                    + " INT DEFAULT 9981;");
        }
        if (oldVersion < newVersion && newVersion == 4) {
            // Add the required columns for WOL. sqlite does only support single
            // alterations of the table
            db.execSQL("ALTER TABLE " + DataContract.Connections.TABLE + " ADD COLUMN " + DataContract.Connections.WOL_ADDRESS
                    + " TEXT NULL;");
            db.execSQL("ALTER TABLE " + DataContract.Connections.TABLE + " ADD COLUMN " + DataContract.Connections.WOL_PORT
                    + " INT DEFAULT 9;");
        }
        if (oldVersion < newVersion && newVersion == 5) {
            // Add the broadcast column for WOL.
            db.execSQL("ALTER TABLE " + DataContract.Connections.TABLE + " ADD COLUMN " + DataContract.Connections.WOL_BROADCAST
                    + " INT DEFAULT 0;");
        }
        if (oldVersion < newVersion && newVersion == 6) {
            // Add the id columns for the profiles.
            db.execSQL("ALTER TABLE " + DataContract.Connections.TABLE + " ADD COLUMN " + DataContract.Connections.PLAY_PROFILE_ID
                    + " INT DEFAULT 0;");
            db.execSQL("ALTER TABLE " + DataContract.Connections.TABLE + " ADD COLUMN " + DataContract.Connections.REC_PROFILE_ID
                    + " INT DEFAULT 0;");
            // Add the new profile table
            final String query = "CREATE TABLE IF NOT EXISTS " + DataContract.Profiles.TABLE + " ("
                    + DataContract.Profiles.ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + DataContract.Profiles.ENABLED + " INT DEFAULT 0, "
                    + DataContract.Profiles.UUID + " TEXT NULL, "
                    + DataContract.Profiles.CONTAINER + " TEXT NULL, "
                    + DataContract.Profiles.TRANSCODE + " INT DEFAULT 0, "
                    + DataContract.Profiles.RESOLUTION + " TEXT NULL, "
                    + DataContract.Profiles.AUDIO_CODEC + " TEXT NULL, "
                    + DataContract.Profiles.VIDEO_CODEC + " TEXT NULL, "
                    + DataContract.Profiles.SUBTITLE_CODEC + " TEXT NULL);";
            db.execSQL(query);
        }
        if (oldVersion < newVersion && newVersion == 7) {
            db.execSQL("ALTER TABLE " + DataContract.Profiles.TABLE + " ADD COLUMN " + DataContract.Profiles.NAME
                    + " TEXT NULL;");
        }
        if (oldVersion < newVersion && newVersion == 8) {
            // Add the id columns for the profiles but check if they exist
            Cursor cursor = db.rawQuery("SELECT * FROM " + DataContract.Connections.TABLE, null);
            int colIndex = cursor.getColumnIndex(DataContract.Connections.PLAY_PROFILE_ID);
            if (colIndex < 0) {
                db.execSQL("ALTER TABLE " + DataContract.Connections.TABLE + " ADD COLUMN "
                        + DataContract.Connections.PLAY_PROFILE_ID + " INT DEFAULT 0;");
            }

            cursor = db.rawQuery("SELECT * FROM " + DataContract.Connections.TABLE, null);
            colIndex = cursor.getColumnIndex(DataContract.Connections.REC_PROFILE_ID);
            if (colIndex < 0) {
                db.execSQL("ALTER TABLE " + DataContract.Connections.TABLE + " ADD COLUMN "
                        + DataContract.Connections.REC_PROFILE_ID + " INT DEFAULT 0;");
            }

            // Add the new profile table
            final String query = "CREATE TABLE IF NOT EXISTS " + DataContract.Profiles.TABLE + " ("
                    + DataContract.Profiles.ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + DataContract.Profiles.ENABLED + " INT DEFAULT 0, "
                    + DataContract.Profiles.UUID + " TEXT NULL, "
                    + DataContract.Profiles.NAME + " TEXT NULL, "
                    + DataContract.Profiles.CONTAINER + " TEXT NULL, "
                    + DataContract.Profiles.TRANSCODE + " INT DEFAULT 0, "
                    + DataContract.Profiles.RESOLUTION + " TEXT NULL, "
                    + DataContract.Profiles.AUDIO_CODEC + " TEXT NULL, "
                    + DataContract.Profiles.VIDEO_CODEC + " TEXT NULL, "
                    + DataContract.Profiles.SUBTITLE_CODEC + " TEXT NULL);";
            db.execSQL(query);
            cursor.close();
        }
        if (oldVersion < newVersion && newVersion == 9) {
            db.execSQL("ALTER TABLE " + DataContract.Connections.TABLE + " ADD COLUMN " +  DataContract.Connections.CAST_PROFILE_ID
                    + " INT DEFAULT 0;");
        }
    }
}
