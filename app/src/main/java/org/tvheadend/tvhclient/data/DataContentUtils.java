package org.tvheadend.tvhclient.data;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import org.tvheadend.tvhclient.model.Connection;

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
            connection.freeDiskSpace = c.getInt(c.getColumnIndex(DataContract.Connections.FREE_DISC_SPACE));
            connection.totalDiskSpace = c.getInt(c.getColumnIndex(DataContract.Connections.TOTAL_DISC_SPACE));
            connection.htspVersion = c.getString(c.getColumnIndex(DataContract.Connections.HTSP_VERSION));
            connection.serverName = c.getString(c.getColumnIndex(DataContract.Connections.SERVER_NAME));
            connection.serverVersion = c.getString(c.getColumnIndex(DataContract.Connections.SERVER_VERSION));
            connection.webRoot = c.getString(c.getColumnIndex(DataContract.Connections.WEB_ROOT));
            c.close();
        }
        return connection;
    }
}
