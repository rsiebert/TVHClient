package org.tvheadend.tvhclient.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import org.tvheadend.tvhclient.model.Connection;
import org.tvheadend.tvhclient.model.Profile;

import java.util.ArrayList;
import java.util.List;

public class DataContentProviderHelper {

    private final Context context;

    public DataContentProviderHelper(Context context) {
        this.context = context;
    }

    /**
     * Returns the connection from the database that is marked as the preferred
     * (selected) one.
     *
     * @return Selected connection
     */
    public Connection getSelectedConnection() {
        Cursor c = context.getApplicationContext().getContentResolver().query(
                DataContract.Connections.CONTENT_URI,
                DataContract.Connections.PROJECTION_ALL,
                DataContract.Connections.SELECTED + "=?", new String[] {"1"}, null);
        Connection conn = null;
        if (c.getCount() > 0) {
            c.moveToFirst();
            conn = getConnectionValues(c);
        }
        c.close();
        return conn;
    }

    /**
     * Inserts a new connection with the given parameters into the database
     * @param conn Connetcion
     * @return Id of the newly added connection
     */
    public long addConnection(final Connection conn) {
        ContentValues values = new ContentValues();
        values.put(DataContract.Connections.NAME, conn.name);
        values.put(DataContract.Connections.ADDRESS, conn.address);
        values.put(DataContract.Connections.PORT, conn.port);
        values.put(DataContract.Connections.USERNAME, conn.username);
        values.put(DataContract.Connections.PASSWORD, conn.password);
        values.put(DataContract.Connections.SELECTED, (conn.selected) ? "1" : "0");
        values.put(DataContract.Connections.CHANNEL_TAG, conn.channelTag);
        values.put(DataContract.Connections.STREAMING_PORT, conn.streaming_port);
        values.put(DataContract.Connections.WOL_ADDRESS, conn.wol_mac_address);
        values.put(DataContract.Connections.WOL_PORT, conn.wol_port);
        values.put(DataContract.Connections.WOL_BROADCAST, (conn.wol_broadcast) ? "1" : "0");
        values.put(DataContract.Connections.PLAY_PROFILE_ID, conn.playback_profile_id);
        values.put(DataContract.Connections.REC_PROFILE_ID, conn.recording_profile_id);
        values.put(DataContract.Connections.CAST_PROFILE_ID, conn.cast_profile_id);

        Uri uri = context.getApplicationContext().getContentResolver().insert(DataContract.Connections.CONTENT_URI, values);
        long id = Long.valueOf(uri.getLastPathSegment());
        return id;
    }

    /**
     * Removes a connection with the given id from the database
     * @param id Id of the connection
     * @return True of connection was removed, otherwise false
     */
    public boolean removeConnection(final long id) {
        int rows = context.getApplicationContext().getContentResolver().delete(DataContract.Connections.CONTENT_URI, DataContract.Connections.ID + "=?", new String[]{String.valueOf(id)});
        return (rows > 0);
    }

    /**
     * Updates the connection with the given id and parameters in the database
     * @param conn Connetcion
     * @return True if connection was updated, otherwise false
     */
    public boolean updateConnection(final Connection conn) {

        ContentValues values = new ContentValues();
        values.put(DataContract.Connections.NAME, conn.name);
        values.put(DataContract.Connections.ADDRESS, conn.address);
        values.put(DataContract.Connections.PORT, conn.port);
        values.put(DataContract.Connections.USERNAME, conn.username);
        values.put(DataContract.Connections.PASSWORD, conn.password);
        values.put(DataContract.Connections.SELECTED, (conn.selected) ? "1" : "0");
        values.put(DataContract.Connections.CHANNEL_TAG, conn.channelTag);
        values.put(DataContract.Connections.STREAMING_PORT, conn.streaming_port);
        values.put(DataContract.Connections.WOL_ADDRESS, conn.wol_mac_address);
        values.put(DataContract.Connections.WOL_PORT, conn.wol_port);
        values.put(DataContract.Connections.WOL_BROADCAST, (conn.wol_broadcast) ? "1" : "0");
        values.put(DataContract.Connections.PLAY_PROFILE_ID, conn.playback_profile_id);
        values.put(DataContract.Connections.REC_PROFILE_ID, conn.recording_profile_id);
        values.put(DataContract.Connections.CAST_PROFILE_ID, conn.cast_profile_id);

        int rows = context.getApplicationContext().getContentResolver().update(DataContract.Connections.CONTENT_URI, values, DataContract.Connections.ID + "=?", new String[]{String.valueOf(conn.id)});
        return (rows > 0);
    }

    /**
     * Returns the connection with the given id from the database
     * @param id Id of the connection
     * @return Connection
     */
    public Connection getConnection(final long id) {
        Cursor c = context.getApplicationContext().getContentResolver().query(DataContract.Connections.CONTENT_URI,
                DataContract.Connections.PROJECTION_ALL,
                DataContract.Connections.ID + "=?",
                new String[]{String.valueOf(id)}, null);

        Connection conn = null;
        if (c != null && c.moveToFirst()) {
            conn = getConnectionValues(c);
            c.close();
        }
        return conn;
    }

    /**
     * Returns a list of all connections from the database
     * @return List of all connections
     */
    public List<Connection> getConnections() {
        List<Connection> connList = new ArrayList<>();
        Cursor c = context.getApplicationContext().getContentResolver().query(DataContract.Connections.CONTENT_URI,
                DataContract.Connections.PROJECTION_ALL,
                null, null, null);

        if (c != null && c.moveToFirst()) {
            do {
                Connection conn = getConnectionValues(c);
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
     * @param c Database cursor
     * @return Connection
     */
    private Connection getConnectionValues(final Cursor c) {
        Connection conn = new Connection();
        conn.id = c.getInt(c.getColumnIndex(DataContract.Connections.ID));
        conn.name = c.getString(c.getColumnIndex(DataContract.Connections.NAME));
        conn.address = c.getString(c.getColumnIndex(DataContract.Connections.ADDRESS));
        conn.port = c.getInt(c.getColumnIndex(DataContract.Connections.PORT));
        conn.username = c.getString(c.getColumnIndex(DataContract.Connections.USERNAME));
        conn.password = c.getString(c.getColumnIndex(DataContract.Connections.PASSWORD));
        conn.selected = (c.getInt(c.getColumnIndex(DataContract.Connections.SELECTED)) > 0);
        conn.channelTag = c.getInt(c.getColumnIndex(DataContract.Connections.CHANNEL_TAG));
        conn.streaming_port = c.getInt(c.getColumnIndex(DataContract.Connections.STREAMING_PORT));
        conn.wol_mac_address = c.getString(c.getColumnIndex(DataContract.Connections.WOL_ADDRESS));
        conn.wol_port = c.getInt(c.getColumnIndex(DataContract.Connections.WOL_PORT));
        conn.wol_broadcast = (c.getInt(c.getColumnIndex(DataContract.Connections.WOL_BROADCAST)) > 0);
        conn.playback_profile_id = c.getInt(c.getColumnIndex(DataContract.Connections.PLAY_PROFILE_ID));
        conn.recording_profile_id = c.getInt(c.getColumnIndex(DataContract.Connections.REC_PROFILE_ID));
        conn.cast_profile_id = c.getInt(c.getColumnIndex(DataContract.Connections.CAST_PROFILE_ID));
        return conn;
    }

    /**
     * Inserts a new profile with the given parameters into the database
     * @param p Profile
     * @return True if profile was added, otherwise false
     */
    public long addProfile(final Profile p) {
        ContentValues values = new ContentValues();
        values.put(DataContract.Profiles.ENABLED, (p.enabled) ? "1" : "0");
        values.put(DataContract.Profiles.UUID, p.uuid);
        values.put(DataContract.Profiles.NAME, p.name);
        values.put(DataContract.Profiles.CONTAINER, p.container);
        values.put(DataContract.Profiles.TRANSCODE, (p.transcode) ? "1" : "0");
        values.put(DataContract.Profiles.RESOLUTION, p.resolution);
        values.put(DataContract.Profiles.AUDIO_CODEC, p.audio_codec);
        values.put(DataContract.Profiles.VIDEO_CODEC, p.video_codec);
        values.put(DataContract.Profiles.SUBTITLE_CODEC, p.subtitle_codec);

        Uri uri = context.getApplicationContext().getContentResolver().insert(DataContract.Profiles.CONTENT_URI, values);
        long id = Long.valueOf(uri.getLastPathSegment());
        return id;
    }

    /**
     * Removes a profile with the given id from the database
     * @param id Id of the profile
     * @return True if profile was removed, otherwise false
     */
    public boolean removeProfile(final long id) {
        int rows = context.getApplicationContext().getContentResolver().delete(DataContract.Profiles.CONTENT_URI, DataContract.Profiles.ID + "=?", new String[]{String.valueOf(id)});
        return (rows > 0);
    }

    /**
     * Updates the profile with the given id and parameters in the database
     * @param p Profile
     * @return True if profile was updated, otherwise false
     */
    public boolean updateProfile(final Profile p) {
        ContentValues values = new ContentValues();
        values.put(DataContract.Profiles.ENABLED, (p.enabled) ? "1" : "0");
        values.put(DataContract.Profiles.UUID, p.uuid);
        values.put(DataContract.Profiles.NAME, p.name);
        values.put(DataContract.Profiles.CONTAINER, p.container);
        values.put(DataContract.Profiles.TRANSCODE, (p.transcode) ? "1" : "0");
        values.put(DataContract.Profiles.RESOLUTION, p.resolution);
        values.put(DataContract.Profiles.AUDIO_CODEC, p.audio_codec);
        values.put(DataContract.Profiles.VIDEO_CODEC, p.video_codec);
        values.put(DataContract.Profiles.SUBTITLE_CODEC, p.subtitle_codec);

        long rows = context.getApplicationContext().getContentResolver().update(DataContract.Profiles.CONTENT_URI, values, DataContract.Profiles.ID + "=?", new String[]{String.valueOf(p.id)});
        return (rows > 0);
    }

    /**
     * Returns the profile from the given id from the database
     * @param id Id of the profile
     * @return Profile
     */
    public Profile getProfile(final long id) {
        Cursor c = context.getApplicationContext().getContentResolver().query(
                DataContract.Profiles.CONTENT_URI,
                DataContract.Profiles.PROJECTION_ALL,
                DataContract.Profiles.ID + "=?",
                new String[] { String.valueOf(id) }, null);

        Profile profile = null;
        if (c != null && c.moveToFirst()) {
            profile = new Profile();
            profile.id = c.getInt(c.getColumnIndex(DataContract.Profiles.ID));
            profile.enabled = (c.getInt(c.getColumnIndex(DataContract.Profiles.ENABLED)) > 0);
            profile.uuid = c.getString(c.getColumnIndex(DataContract.Profiles.UUID));
            profile.name = c.getString(c.getColumnIndex(DataContract.Profiles.NAME));
            profile.container = c.getString(c.getColumnIndex(DataContract.Profiles.CONTAINER));
            profile.transcode = (c.getInt(c.getColumnIndex(DataContract.Profiles.TRANSCODE)) > 0);
            profile.resolution = c.getString(c.getColumnIndex(DataContract.Profiles.RESOLUTION));
            profile.audio_codec = c.getString(c.getColumnIndex(DataContract.Profiles.AUDIO_CODEC));
            profile.video_codec = c.getString(c.getColumnIndex(DataContract.Profiles.VIDEO_CODEC));
            profile.subtitle_codec = c.getString(c.getColumnIndex(DataContract.Profiles.SUBTITLE_CODEC));
            c.close();
        }
        return profile;
    }
}
