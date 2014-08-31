package org.tvheadend.tvhclient;

import java.util.ArrayList;
import java.util.List;

import org.tvheadend.tvhclient.model.Connection;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

public class DatabaseHelper extends SQLiteOpenHelper {

	private final static String TAG = DatabaseHelper.class.getSimpleName();

    // Database version and name declarations
    public static final int DATABASE_VERSION = 5;
    public static final String DATABASE_NAME = "tvhclient";
    public static final String TABLE_NAME = "connections";

    // Database column names
    public static final String KEY_ID = BaseColumns._ID;
    public static final String KEY_NAME = "name";
    public static final String KEY_ADDRESS = "address";
    public static final String KEY_PORT = "port";
    public static final String KEY_USERNAME = "username";
    public static final String KEY_PASSWORD = "password";
    public static final String KEY_SELECTED = "selected";
    public static final String KEY_CHANNEL_TAG = "channel_tag";
    public static final String KEY_STREAMING_PORT = "streaming_port";
    public static final String KEY_WOL_ADDRESS = "wol_address";
    public static final String KEY_WOL_PORT = "wol_port";
    public static final String KEY_WOL_BROADCAST = "wol_broadcast";

    // Defines a list of columns to retrieve from
    // the Cursor and load into an output row
    public static final String[] COLUMNS = { 
        KEY_ID, 
        KEY_NAME, 
        KEY_ADDRESS, 
        KEY_PORT,
        KEY_USERNAME, 
        KEY_PASSWORD,
        KEY_SELECTED, 
        KEY_CHANNEL_TAG,
        KEY_STREAMING_PORT,
        KEY_WOL_ADDRESS,
        KEY_WOL_PORT,
        KEY_WOL_BROADCAST,
    };

    public static DatabaseHelper instance = null;

    public static DatabaseHelper init(Context ctx) {
        if (instance == null) {
            instance = new DatabaseHelper(ctx);
        }
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

    	final String query = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" 
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + KEY_NAME + " TEXT NOT NULL," 
                + KEY_ADDRESS + " TEXT NOT NULL, " 
                + KEY_PORT + " INT DEFAULT 9982, "
                + KEY_USERNAME + " TEXT NULL, "
                + KEY_PASSWORD + " TEXT NULL, "
                + KEY_SELECTED + " INT NOT NULL, "
                + KEY_CHANNEL_TAG + " INT DEFAULT 0, "
                + KEY_STREAMING_PORT + " INT DEFAULT 9981, "
                + KEY_WOL_ADDRESS + " TEXT NULL, "
                + KEY_WOL_PORT + " INT DEFAULT 9, "
                + KEY_WOL_BROADCAST + " INT DEFAULT 0);";
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
            db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + KEY_CHANNEL_TAG
                    + " INT DEFAULT 0;");
        }
        if (oldVersion < newVersion && newVersion == 3) {
            // Add the streaming port column in database version 3 
            db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + KEY_STREAMING_PORT
                    + " INT DEFAULT 9981;");
        }
        if (oldVersion < newVersion && newVersion == 4) {
            // Add the required columns for WOL. sqlite does only support single
            // alterations of the table
            db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + KEY_WOL_ADDRESS
                    + " TEXT NULL;");
            db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + KEY_WOL_PORT
                    + " INT DEFAULT 9;");
        }
        if (oldVersion < newVersion && newVersion == 5) {
            // Add the broadcast column for WOL.
            db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + KEY_WOL_BROADCAST
                    + " INT DEFAULT 0;");
        }
    }

    /**
     * Inserts a new connection with the given parameters into the database 
     * @param conn
     * @return
     */
    public long addConnection(final Connection conn) {
        ContentValues values = new ContentValues();
        values.put(KEY_NAME, conn.name);
        values.put(KEY_ADDRESS, conn.address);
        values.put(KEY_PORT, conn.port);
        values.put(KEY_USERNAME, conn.username);
        values.put(KEY_PASSWORD, conn.password);
        values.put(KEY_SELECTED, (conn.selected) ? "1" : "0");
        values.put(KEY_CHANNEL_TAG, conn.channelTag);
        values.put(KEY_STREAMING_PORT, conn.streaming_port);
        values.put(KEY_WOL_ADDRESS, conn.wol_address);
        values.put(KEY_WOL_PORT, conn.wol_port);
        values.put(KEY_WOL_BROADCAST, conn.wol_broadcast);

        SQLiteDatabase db = this.getWritableDatabase();
        long newId = db.insert(TABLE_NAME, null, values);
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
        int rows = db.delete(TABLE_NAME, KEY_ID + "=?", whereArgs);
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
        values.put(KEY_NAME, conn.name);
        values.put(KEY_ADDRESS, conn.address);
        values.put(KEY_PORT, conn.port);
        values.put(KEY_USERNAME, conn.username);
        values.put(KEY_PASSWORD, conn.password);
        values.put(KEY_SELECTED, (conn.selected) ? "1" : "0");
        values.put(KEY_CHANNEL_TAG, conn.channelTag);
        values.put(KEY_STREAMING_PORT, conn.streaming_port);
        values.put(KEY_WOL_ADDRESS, conn.wol_address);
        values.put(KEY_WOL_PORT, conn.wol_port);
        values.put(KEY_WOL_BROADCAST, conn.wol_broadcast);

        SQLiteDatabase db = this.getWritableDatabase();
        long rows = db.update(TABLE_NAME, values, KEY_ID + "=" + conn.id, null);
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
        Cursor c = db.query(TABLE_NAME, COLUMNS, KEY_SELECTED + "=?", 
                new String[] { "1" }, null, null, null);

        Connection conn = null;
        if (c.getCount() > 0) {
            c.moveToFirst();
            conn = new Connection();
            conn.id = c.getInt(c.getColumnIndex(KEY_ID));
            conn.name = c.getString(c.getColumnIndex(KEY_NAME));
            conn.address = c.getString(c.getColumnIndex(KEY_ADDRESS));
            conn.port = c.getInt(c.getColumnIndex(KEY_PORT));
            conn.username = c.getString(c.getColumnIndex(KEY_USERNAME));
            conn.password = c.getString(c.getColumnIndex(KEY_PASSWORD));
            conn.selected = (c.getInt(c.getColumnIndex(KEY_SELECTED)) > 0);
            conn.channelTag = c.getInt(c.getColumnIndex(KEY_CHANNEL_TAG));
            conn.streaming_port = c.getInt(c.getColumnIndex(KEY_STREAMING_PORT));
            conn.wol_address = c.getString(c.getColumnIndex(KEY_WOL_ADDRESS));
            conn.wol_port = c.getInt(c.getColumnIndex(KEY_WOL_PORT));
            conn.wol_broadcast = (c.getInt(c.getColumnIndex(KEY_WOL_BROADCAST)) > 0);
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
        Cursor c = db.query(TABLE_NAME, COLUMNS, KEY_ID + "=?", 
                new String[] { String.valueOf(id) }, null, null, null);

        Connection conn = null;
        if (c.moveToFirst()) {
            conn = new Connection();
            conn.id = c.getInt(c.getColumnIndex(KEY_ID));
            conn.name = c.getString(c.getColumnIndex(KEY_NAME));
            conn.address = c.getString(c.getColumnIndex(KEY_ADDRESS));
            conn.port = c.getInt(c.getColumnIndex(KEY_PORT));
            conn.username = c.getString(c.getColumnIndex(KEY_USERNAME));
            conn.password = c.getString(c.getColumnIndex(KEY_PASSWORD));
            conn.selected = (c.getInt(c.getColumnIndex(KEY_SELECTED)) > 0);
            conn.channelTag = c.getInt(c.getColumnIndex(KEY_CHANNEL_TAG));
            conn.streaming_port = c.getInt(c.getColumnIndex(KEY_STREAMING_PORT));
            conn.wol_address = c.getString(c.getColumnIndex(KEY_WOL_ADDRESS));
            conn.wol_port = c.getInt(c.getColumnIndex(KEY_WOL_PORT));
            conn.wol_broadcast = (c.getInt(c.getColumnIndex(KEY_WOL_BROADCAST)) > 0);
        }
        c.close();
        return conn;
    }

    /**
     * Returns a list of all connections from the database
     * @return
     */
    public List<Connection> getConnections() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(TABLE_NAME, COLUMNS, null, null, null, null, KEY_NAME);

        List<Connection> connList = new ArrayList<Connection>();
        Connection conn = null;
        if (c.moveToFirst()) {
            do {
                conn = new Connection();
                conn.id = c.getInt(c.getColumnIndex(KEY_ID));
                conn.name = c.getString(c.getColumnIndex(KEY_NAME));
                conn.address = c.getString(c.getColumnIndex(KEY_ADDRESS));
                conn.port = c.getInt(c.getColumnIndex(KEY_PORT));
                conn.username = c.getString(c.getColumnIndex(KEY_USERNAME));
                conn.password = c.getString(c.getColumnIndex(KEY_PASSWORD));
                conn.selected = (c.getInt(c.getColumnIndex(KEY_SELECTED)) > 0);
                conn.channelTag = c.getInt(c.getColumnIndex(KEY_CHANNEL_TAG));
                conn.streaming_port = c.getInt(c.getColumnIndex(KEY_STREAMING_PORT));
                conn.wol_address = c.getString(c.getColumnIndex(KEY_WOL_ADDRESS));
                conn.wol_port = c.getInt(c.getColumnIndex(KEY_WOL_PORT));
                conn.wol_broadcast = (c.getInt(c.getColumnIndex(KEY_WOL_BROADCAST)) > 0);

                // Add book to books
                connList.add(conn);
            } while (c.moveToNext());
        }
        c.close();
        return connList;
    }
}
