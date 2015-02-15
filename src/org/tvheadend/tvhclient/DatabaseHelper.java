package org.tvheadend.tvhclient;

import java.util.ArrayList;
import java.util.List;

import org.tvheadend.tvhclient.model.Connection;
import org.tvheadend.tvhclient.model.Profile;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;
import android.widget.Toast;

public class DatabaseHelper extends SQLiteOpenHelper {

	private final static String TAG = DatabaseHelper.class.getSimpleName();

    // Database version and name declarations
    public static final int DATABASE_VERSION = 8;
    public static final String DATABASE_NAME = "tvhclient";
    public static final String TABLE_CONN_NAME = "connections";
    public static final String TABLE_PROFILE_NAME = "profiles";

    // Database column names for the connection table
    public static final String KEY_CONN_ID = BaseColumns._ID;
    public static final String KEY_CONN_NAME = "name";
    public static final String KEY_CONN_ADDRESS = "address";
    public static final String KEY_CONN_PORT = "port";
    public static final String KEY_CONN_USERNAME = "username";
    public static final String KEY_CONN_PASSWORD = "password";
    public static final String KEY_CONN_SELECTED = "selected";
    public static final String KEY_CONN_CHANNEL_TAG = "channel_tag";
    public static final String KEY_CONN_STREAMING_PORT = "streaming_port";
    public static final String KEY_CONN_WOL_ADDRESS = "wol_address";
    public static final String KEY_CONN_WOL_PORT = "wol_port";
    public static final String KEY_CONN_WOL_BROADCAST = "wol_broadcast";
    public static final String KEY_CONN_PLAY_PROFILE_ID = "playback_profile_id";
    public static final String KEY_CONN_REC_PROFILE_ID = "recording_profile_id";

    // Database column names for the profile table
    public static final String KEY_PROFILE_ID = BaseColumns._ID;
    public static final String KEY_PROFILE_ENABLED = "profile_enabled"; // use the new profile if htsp version > X
    public static final String KEY_PROFILE_UUID = "profile_uuid";       // The uuid of the profile
    public static final String KEY_PROFILE_NAME = "profile_name";       // The name of the profile
    public static final String KEY_PROFILE_CONTAINER = "container";
    public static final String KEY_PROFILE_TRANSCODE = "transcode";
    public static final String KEY_PROFILE_RESOLUTION = "resolution";
    public static final String KEY_PROFILE_VIDEO_CODEC = "video_codec";
    public static final String KEY_PROFILE_AUDIO_CODEC = "acode_codec";
    public static final String KEY_PROFILE_SUBTITLE_CODEC = "subtitle_codec";

    // Defines a list of columns to retrieve from
    // the Cursor and load into an output row
    public static final String[] CONN_COLUMNS = { 
        KEY_CONN_ID, 
        KEY_CONN_NAME, 
        KEY_CONN_ADDRESS, 
        KEY_CONN_PORT,
        KEY_CONN_USERNAME, 
        KEY_CONN_PASSWORD,
        KEY_CONN_SELECTED, 
        KEY_CONN_CHANNEL_TAG,
        KEY_CONN_STREAMING_PORT,
        KEY_CONN_WOL_ADDRESS,
        KEY_CONN_WOL_PORT,
        KEY_CONN_WOL_BROADCAST,
        KEY_CONN_PLAY_PROFILE_ID,
        KEY_CONN_REC_PROFILE_ID,
    };

    // Defines a list of columns to retrieve from
    // the Cursor and load into an output row
    public static final String[] PROFILE_COLUMNS = { 
        KEY_PROFILE_ID,
        KEY_PROFILE_ENABLED,
        KEY_PROFILE_UUID,
        KEY_PROFILE_NAME,
        KEY_PROFILE_CONTAINER,
        KEY_PROFILE_TRANSCODE, 
        KEY_PROFILE_RESOLUTION,
        KEY_PROFILE_AUDIO_CODEC,
        KEY_PROFILE_VIDEO_CODEC,
        KEY_PROFILE_SUBTITLE_CODEC,
    };

    public static DatabaseHelper instance = null;

    private static Context context;

    public static DatabaseHelper init(Context ctx) {
        if (instance == null) {
            instance = new DatabaseHelper(ctx);
        }
        context = ctx;
        return instance;
    }

    public static DatabaseHelper getInstance() {
        return instance;
    }

    private DatabaseHelper(Context context) {
    	super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * Called when the database is created for the very first time.
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, "onCreate");

    	String query = "CREATE TABLE IF NOT EXISTS " + TABLE_CONN_NAME + " (" 
                + KEY_CONN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + KEY_CONN_NAME + " TEXT NOT NULL," 
                + KEY_CONN_ADDRESS + " TEXT NOT NULL, " 
                + KEY_CONN_PORT + " INT DEFAULT 9982, "
                + KEY_CONN_USERNAME + " TEXT NULL, "
                + KEY_CONN_PASSWORD + " TEXT NULL, "
                + KEY_CONN_SELECTED + " INT NOT NULL, "
                + KEY_CONN_CHANNEL_TAG + " INT DEFAULT 0, "
                + KEY_CONN_STREAMING_PORT + " INT DEFAULT 9981, "
                + KEY_CONN_WOL_ADDRESS + " TEXT NULL, "
                + KEY_CONN_WOL_PORT + " INT DEFAULT 9, "
                + KEY_CONN_WOL_BROADCAST + " INT DEFAULT 0, "
    	        + KEY_CONN_PLAY_PROFILE_ID + " INT DEFAULT 0, "
                + KEY_CONN_REC_PROFILE_ID + " INT DEFAULT 0);";
        db.execSQL(query);
        
        query = "CREATE TABLE IF NOT EXISTS " + TABLE_PROFILE_NAME + " (" 
                + KEY_PROFILE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + KEY_PROFILE_ENABLED + " INT DEFAULT 0, "
                + KEY_PROFILE_UUID + " TEXT NULL, "
                + KEY_PROFILE_NAME + " TEXT NULL, "
                + KEY_PROFILE_CONTAINER + " TEXT NULL, "
                + KEY_PROFILE_TRANSCODE + " INT DEFAULT 0, "
                + KEY_PROFILE_RESOLUTION + " TEXT NULL, "
                + KEY_PROFILE_AUDIO_CODEC + " TEXT NULL, "
                + KEY_PROFILE_VIDEO_CODEC + " TEXT NULL, "
                + KEY_PROFILE_SUBTITLE_CODEC + " TEXT NULL);";
        db.execSQL(query);
    }

    /**
     * Called when the database version has changed.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "onUpgrade, from version " + oldVersion + " to " + newVersion);

        if (oldVersion < newVersion && newVersion == 2) {
            // Add the channel tag column in database version 2 
            db.execSQL("ALTER TABLE " + TABLE_CONN_NAME + " ADD COLUMN " + KEY_CONN_CHANNEL_TAG
                    + " INT DEFAULT 0;");
        }
        if (oldVersion < newVersion && newVersion == 3) {
            // Add the streaming port column in database version 3 
            db.execSQL("ALTER TABLE " + TABLE_CONN_NAME + " ADD COLUMN " + KEY_CONN_STREAMING_PORT
                    + " INT DEFAULT 9981;");
        }
        if (oldVersion < newVersion && newVersion == 4) {
            // Add the required columns for WOL. sqlite does only support single
            // alterations of the table
            db.execSQL("ALTER TABLE " + TABLE_CONN_NAME + " ADD COLUMN " + KEY_CONN_WOL_ADDRESS
                    + " TEXT NULL;");
            db.execSQL("ALTER TABLE " + TABLE_CONN_NAME + " ADD COLUMN " + KEY_CONN_WOL_PORT
                    + " INT DEFAULT 9;");
        }
        if (oldVersion < newVersion && newVersion == 5) {
            // Add the broadcast column for WOL.
            db.execSQL("ALTER TABLE " + TABLE_CONN_NAME + " ADD COLUMN " + KEY_CONN_WOL_BROADCAST
                    + " INT DEFAULT 0;");
        }
        if (oldVersion < newVersion && newVersion == 6) {
            // Add the id columns for the profiles.
            db.execSQL("ALTER TABLE " + TABLE_CONN_NAME + " ADD COLUMN " + KEY_CONN_PLAY_PROFILE_ID
                    + " INT DEFAULT 0;");
            db.execSQL("ALTER TABLE " + TABLE_CONN_NAME + " ADD COLUMN " + KEY_CONN_REC_PROFILE_ID
                    + " INT DEFAULT 0;");
            // Add the new profile table
            final String query = "CREATE TABLE IF NOT EXISTS " + TABLE_PROFILE_NAME + " (" 
                    + KEY_PROFILE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + KEY_PROFILE_ENABLED + " INT DEFAULT 0, "
                    + KEY_PROFILE_UUID + " TEXT NULL, "
                    + KEY_PROFILE_CONTAINER + " TEXT NULL, "
                    + KEY_PROFILE_TRANSCODE + " INT DEFAULT 0, "
                    + KEY_PROFILE_RESOLUTION + " TEXT NULL, "
                    + KEY_PROFILE_AUDIO_CODEC + " TEXT NULL, "
                    + KEY_PROFILE_VIDEO_CODEC + " TEXT NULL, "
                    + KEY_PROFILE_SUBTITLE_CODEC + " TEXT NULL);";
            db.execSQL(query);
        }
        if (oldVersion < newVersion && newVersion == 7) {
            db.execSQL("ALTER TABLE " + TABLE_PROFILE_NAME + " ADD COLUMN " + KEY_PROFILE_NAME
                    + " TEXT NULL;");
        }
        if (oldVersion < newVersion && newVersion == 8) {
            // Add the id columns for the profiles.
            db.execSQL("ALTER TABLE " + TABLE_CONN_NAME + " ADD COLUMN " + KEY_CONN_PLAY_PROFILE_ID
                    + " INT DEFAULT 0;");
            db.execSQL("ALTER TABLE " + TABLE_CONN_NAME + " ADD COLUMN " + KEY_CONN_REC_PROFILE_ID
                    + " INT DEFAULT 0;");
            // Add the new profile table
            final String query = "CREATE TABLE IF NOT EXISTS " + TABLE_PROFILE_NAME + " (" 
                    + KEY_PROFILE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + KEY_PROFILE_ENABLED + " INT DEFAULT 0, "
                    + KEY_PROFILE_UUID + " TEXT NULL, "
                    + KEY_PROFILE_NAME + " TEXT NULL, "
                    + KEY_PROFILE_CONTAINER + " TEXT NULL, "
                    + KEY_PROFILE_TRANSCODE + " INT DEFAULT 0, "
                    + KEY_PROFILE_RESOLUTION + " TEXT NULL, "
                    + KEY_PROFILE_AUDIO_CODEC + " TEXT NULL, "
                    + KEY_PROFILE_VIDEO_CODEC + " TEXT NULL, "
                    + KEY_PROFILE_SUBTITLE_CODEC + " TEXT NULL);";
            db.execSQL(query);
        }
    }

    /**
     * Inserts a new connection with the given parameters into the database 
     * @param conn
     * @return
     */
    public long addConnection(final Connection conn) {
        ContentValues values = new ContentValues();
        values.put(KEY_CONN_NAME, conn.name);
        values.put(KEY_CONN_ADDRESS, conn.address);
        values.put(KEY_CONN_PORT, conn.port);
        values.put(KEY_CONN_USERNAME, conn.username);
        values.put(KEY_CONN_PASSWORD, conn.password);
        values.put(KEY_CONN_SELECTED, (conn.selected) ? "1" : "0");
        values.put(KEY_CONN_CHANNEL_TAG, conn.channelTag);
        values.put(KEY_CONN_STREAMING_PORT, conn.streaming_port);
        values.put(KEY_CONN_WOL_ADDRESS, conn.wol_address);
        values.put(KEY_CONN_WOL_PORT, conn.wol_port);
        values.put(KEY_CONN_WOL_BROADCAST, conn.wol_broadcast);
        values.put(KEY_CONN_PLAY_PROFILE_ID, conn.playback_profile_id);
        values.put(KEY_CONN_REC_PROFILE_ID, conn.recording_profile_id);

        SQLiteDatabase db = this.getWritableDatabase();
        long newId = db.insert(TABLE_CONN_NAME, null, values);
        db.close();
        return newId;
    }

    /**
     * Removes a connection with the given id from the database
     * @param id
     * @return
     */
    public boolean removeConnection(final long id) {
        String[] whereArgs = { String.valueOf(id) };
        SQLiteDatabase db = this.getWritableDatabase();
        int rows = db.delete(TABLE_CONN_NAME, KEY_CONN_ID + "=?", whereArgs);
        db.close();
        return (rows > 0);
    }

    /**
     * Updates the connection with the given id and parameters in the database 
     * @param conn
     * @return
     */
    public boolean updateConnection(final Connection conn) {

        ContentValues values = new ContentValues();
        values.put(KEY_CONN_NAME, conn.name);
        values.put(KEY_CONN_ADDRESS, conn.address);
        values.put(KEY_CONN_PORT, conn.port);
        values.put(KEY_CONN_USERNAME, conn.username);
        values.put(KEY_CONN_PASSWORD, conn.password);
        values.put(KEY_CONN_SELECTED, (conn.selected) ? "1" : "0");
        values.put(KEY_CONN_CHANNEL_TAG, conn.channelTag);
        values.put(KEY_CONN_STREAMING_PORT, conn.streaming_port);
        values.put(KEY_CONN_WOL_ADDRESS, conn.wol_address);
        values.put(KEY_CONN_WOL_PORT, conn.wol_port);
        values.put(KEY_CONN_WOL_BROADCAST, conn.wol_broadcast);
        values.put(KEY_CONN_PLAY_PROFILE_ID, conn.playback_profile_id);
        values.put(KEY_CONN_REC_PROFILE_ID, conn.recording_profile_id);

        SQLiteDatabase db = this.getWritableDatabase();
        long rows = db.update(TABLE_CONN_NAME, values, KEY_CONN_ID + "=" + conn.id, null);
        db.close();
        return (rows > 0);
    }

    /**
     * Returns the connection from the database that is marked as the preferred
     * (selected) one.
     * 
     * @return
     */
    public Connection getSelectedConnection() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(TABLE_CONN_NAME, CONN_COLUMNS, KEY_CONN_SELECTED + "=?", 
                new String[] { "1" }, null, null, null);

        Connection conn = null;
        if (c.getCount() > 0) {
            c.moveToFirst();
            conn = getConnectionValues(c);
        }
        c.close();
        return conn;
    }

    /**
     * Returns the connection with the given id from the database
     * @param id
     * @return
     */
    public Connection getConnection(final long id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(TABLE_CONN_NAME, CONN_COLUMNS, KEY_CONN_ID + "=?", 
                new String[] { String.valueOf(id) }, null, null, null);

        Connection conn = null;
        if (c.moveToFirst()) {
            conn = getConnectionValues(c);
        }
        c.close();
        return conn;
    }

    /**
     * Returns a list of all connections from the database
     * @return
     */
    public List<Connection> getConnections() {
        List<Connection> connList = new ArrayList<Connection>();
        Cursor c = null;

        try {
            SQLiteDatabase db = this.getReadableDatabase();
            c = db.query(TABLE_CONN_NAME, CONN_COLUMNS, null, null, null, null, KEY_CONN_NAME);
        } catch (SQLiteException ex) {
            Log.d(TAG, "getConnections, exception " + ex.getLocalizedMessage());
            if (context != null) {
                Toast.makeText(context,
                        "An error occurred while getting the list of available connections!\n"
                                + "There was probably an issue while updating the app.\n" 
                                + "Please uninstall and reinstall the app again to fix this.\n" 
                                + "Thank you!",
                        Toast.LENGTH_LONG).show();
            }
        }

        Connection conn = null;
        if (c != null && c.moveToFirst()) {
            do {
                conn = getConnectionValues(c);
                connList.add(conn);
            } while (c.moveToNext());
        }
        if (c != null) {
            c.close();
        }
        return connList;
    }

    /**
     * 
     * @param c
     * @return
     */
    private Connection getConnectionValues(final Cursor c) {
        Connection conn = new Connection();
        conn.id = c.getInt(c.getColumnIndex(KEY_CONN_ID));
        conn.name = c.getString(c.getColumnIndex(KEY_CONN_NAME));
        conn.address = c.getString(c.getColumnIndex(KEY_CONN_ADDRESS));
        conn.port = c.getInt(c.getColumnIndex(KEY_CONN_PORT));
        conn.username = c.getString(c.getColumnIndex(KEY_CONN_USERNAME));
        conn.password = c.getString(c.getColumnIndex(KEY_CONN_PASSWORD));
        conn.selected = (c.getInt(c.getColumnIndex(KEY_CONN_SELECTED)) > 0);
        conn.channelTag = c.getInt(c.getColumnIndex(KEY_CONN_CHANNEL_TAG));
        conn.streaming_port = c.getInt(c.getColumnIndex(KEY_CONN_STREAMING_PORT));
        conn.wol_address = c.getString(c.getColumnIndex(KEY_CONN_WOL_ADDRESS));
        conn.wol_port = c.getInt(c.getColumnIndex(KEY_CONN_WOL_PORT));
        conn.wol_broadcast = (c.getInt(c.getColumnIndex(KEY_CONN_WOL_BROADCAST)) > 0);
        conn.playback_profile_id = c.getInt(c.getColumnIndex(KEY_CONN_PLAY_PROFILE_ID));
        conn.recording_profile_id = c.getInt(c.getColumnIndex(KEY_CONN_REC_PROFILE_ID));
        return conn;
    }
    
    /**
     * Inserts a new profile with the given parameters into the database 
     * @param p
     * @return
     */
    public long addProfile(final Profile p) {
        ContentValues values = new ContentValues();
        values.put(KEY_PROFILE_ENABLED, (p.enabled) ? "1" : "0");
        values.put(KEY_PROFILE_UUID, p.uuid);
        values.put(KEY_PROFILE_NAME, p.name);
        values.put(KEY_PROFILE_CONTAINER, p.container);
        values.put(KEY_PROFILE_TRANSCODE, (p.transcode) ? "1" : "0");
        values.put(KEY_PROFILE_RESOLUTION, p.resolution);
        values.put(KEY_PROFILE_AUDIO_CODEC, p.audio_codec);
        values.put(KEY_PROFILE_VIDEO_CODEC, p.video_codec);
        values.put(KEY_PROFILE_SUBTITLE_CODEC, p.subtitle_codec);

        SQLiteDatabase db = this.getWritableDatabase();
        long newId = db.insert(TABLE_PROFILE_NAME, null, values);
        db.close();
        return newId;
    }

    /**
     * Removes a profile with the given id from the database
     * @param id
     * @return
     */
    public boolean removeProfile(final long id) {
        String[] whereArgs = { String.valueOf(id) };
        SQLiteDatabase db = this.getWritableDatabase();
        int rows = db.delete(TABLE_PROFILE_NAME, KEY_PROFILE_ID + "=?", whereArgs);
        db.close();
        return (rows > 0);
    }

    /**
     * Updates the profile with the given id and parameters in the database 
     * @param p
     * @return
     */
    public boolean updateProfile(final Profile p) {
        ContentValues values = new ContentValues();
        values.put(KEY_PROFILE_ENABLED, (p.enabled) ? "1" : "0");
        values.put(KEY_PROFILE_UUID, p.uuid);
        values.put(KEY_PROFILE_NAME, p.name);
        values.put(KEY_PROFILE_CONTAINER, p.container);
        values.put(KEY_PROFILE_TRANSCODE, (p.transcode) ? "1" : "0");
        values.put(KEY_PROFILE_RESOLUTION, p.resolution);
        values.put(KEY_PROFILE_AUDIO_CODEC, p.audio_codec);
        values.put(KEY_PROFILE_VIDEO_CODEC, p.video_codec);
        values.put(KEY_PROFILE_SUBTITLE_CODEC, p.subtitle_codec);

        SQLiteDatabase db = this.getWritableDatabase();
        long rows = db.update(TABLE_PROFILE_NAME, values, KEY_PROFILE_ID + "=" + p.id, null);
        db.close();
        return (rows > 0);
    }
    /**
     * Returns the profile from the given id from the database
     * @param id
     * @return
     */
    public Profile getProfile(final long id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(TABLE_PROFILE_NAME, PROFILE_COLUMNS, KEY_PROFILE_ID + "=?", 
                new String[] { String.valueOf(id) }, null, null, null);

        Profile profile = null;
        if (c.moveToFirst()) {
            profile = new Profile();
            profile.id = c.getInt(c.getColumnIndex(KEY_PROFILE_ID));
            profile.enabled = (c.getInt(c.getColumnIndex(KEY_PROFILE_ENABLED)) > 0);
            profile.uuid = c.getString(c.getColumnIndex(KEY_PROFILE_UUID));
            profile.name = c.getString(c.getColumnIndex(KEY_PROFILE_NAME));
            profile.container = c.getString(c.getColumnIndex(KEY_PROFILE_CONTAINER));
            profile.transcode = (c.getInt(c.getColumnIndex(KEY_PROFILE_TRANSCODE)) > 0);
            profile.resolution = c.getString(c.getColumnIndex(KEY_PROFILE_RESOLUTION));
            profile.audio_codec = c.getString(c.getColumnIndex(KEY_PROFILE_AUDIO_CODEC));
            profile.video_codec = c.getString(c.getColumnIndex(KEY_PROFILE_VIDEO_CODEC));
            profile.subtitle_codec = c.getString(c.getColumnIndex(KEY_PROFILE_SUBTITLE_CODEC));
        }
        c.close();
        return profile;
    }
}
