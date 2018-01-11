package org.tvheadend.tvhclient.utils;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.tvheadend.tvhclient.data.Constants;
import org.tvheadend.tvhclient.data.DataStorage;
import org.tvheadend.tvhclient.data.DatabaseHelper;
import org.tvheadend.tvhclient.data.model.ChannelTag;
import org.tvheadend.tvhclient.data.model.Connection;

public class Utils {

    /**
     * Returns the saved channel tag for the active connection.
     *
     * @param activity Activity context
     * @return Channel tag
     */
    public static ChannelTag getChannelTag(final Activity activity) {
        Connection connection = DatabaseHelper.getInstance(activity.getApplicationContext()).getSelectedConnection();
        if (connection != null) {
            return DataStorage.getInstance().getTagFromArray(connection.channelTag);
        }
        return null;
    }

    /**
     * Returns the type how the channels are sorted in the adapter and in which
     * order they will then be shown in the list.
     *
     * @param context Activity context
     * @return Number that defines the channel sort order
     */
    public static int getChannelSortOrder(final Activity context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return Integer.parseInt(prefs.getString("sortChannelsPref", String.valueOf(Constants.CHANNEL_SORT_DEFAULT)));
    }
}
