package org.tvheadend.tvhclient.data;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import org.tvheadend.tvhclient.model.Connection;
import org.tvheadend.tvhclient.model.ServerInfo;

public class DataContentUtils {
    private static final String TAG = DataContentUtils.class.getName();

    private DataContentUtils() {
        throw new IllegalAccessError("Utility class");
    }

    public static Connection getActiveConnection(Context context) {
        Log.d(TAG, "getActiveConnection() called with: context = [" + context + "]");

        Cursor c = context.getApplicationContext().getContentResolver().query(
                DataContract.Connections.CONTENT_URI,
                DataContract.Connections.PROJECTION_ALL,
                DataContract.Connections.SELECTED + "=?", new String[] {"1"}, null);

        Connection connection = null;
        if (c != null && c.getCount() > 0) {
            c.moveToFirst();
            connection = new Connection();
            connection.id = c.getLong(c.getColumnIndex(DataContract.Connections.ID));
            connection.address = c.getString(c.getColumnIndex(DataContract.Connections.ADDRESS));
            connection.port = c.getInt(c.getColumnIndex(DataContract.Connections.PORT));
            connection.username = c.getString(c.getColumnIndex(DataContract.Connections.USERNAME));
            connection.password = c.getString(c.getColumnIndex(DataContract.Connections.PASSWORD));
            c.close();
        }
        return connection;
    }

    public static ServerInfo getActiveServerInfo(Context context) {
        Log.d(TAG, "getActiveServerInfo() called with: context = [" + context + "]");

        Cursor c = context.getApplicationContext().getContentResolver().query(
                ContentUris.withAppendedId(DataContract.ServerInfo.CONTENT_URI_ACTIVE,
                        DataContentProvider.SERVER_STATS_ID_ACTIVE),
                null, null, null, null);

        ServerInfo serverInfo = null;
        if (c != null && c.getCount() > 0) {
            serverInfo = new ServerInfo();
            serverInfo.id = c.getLong(c.getColumnIndex(DataContract.ServerInfo.ID));
            serverInfo.freeDiskSpace = c.getInt(c.getColumnIndex(DataContract.ServerInfo.FREE_DISC_SPACE));
            serverInfo.totalDiskSpace = c.getInt(c.getColumnIndex(DataContract.ServerInfo.TOTAL_DISC_SPACE));
            serverInfo.serverName = c.getString(c.getColumnIndex(DataContract.ServerInfo.SERVER_NAME));
            serverInfo.serverVersion = c.getString(c.getColumnIndex(DataContract.ServerInfo.SERVER_VERSION));
            c.close();
        }
        return serverInfo;
    }
}
