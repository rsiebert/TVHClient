package org.tvheadend.tvhclient.utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.TextView;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.Constants;
import org.tvheadend.tvhclient.data.DataStorage;
import org.tvheadend.tvhclient.data.DatabaseHelper;
import org.tvheadend.tvhclient.data.model.ChannelTag;
import org.tvheadend.tvhclient.data.model.Connection;

public class Utils {

    /**
     * Calculates from the day of week index the name of the day.
     *
     * @param dayOfWeekLabel Widget that shows the day of week header
     * @param dayOfWeek      Widget that shows the day
     * @param dow            Index that defines the day of week
     */
    public static void setDaysOfWeek(Context context, TextView dayOfWeekLabel, TextView dayOfWeek, long dow) {
        if (dayOfWeek == null) {
            return;
        }
        StringBuilder dowValue = new StringBuilder();

        // Use different strings if either no days or all days are chosen. If
        // certain days are selected check which ones.
        if (dow == 0) {
            dowValue = new StringBuilder(context.getString(R.string.no_days));
        } else if (dow == 127) {
            dowValue = new StringBuilder(context.getString(R.string.all_days));
        } else {
            String[] dayNames = context.getResources().getStringArray(R.array.day_short_names);

            // Use bit shifting to check if the first bit it set. The values are:
            // 0 = no days, 1 = Monday, 2 = Tuesday, 4 = Wednesday, 8 = Thursday,
            // 16 = Friday, 32 = Saturday, 64 = Sunday
            for (int i = 0; i < 7; ++i) {
                if ((dow & 1) == 1) {
                    dowValue.append(dayNames[i]).append(", ");
                }
                dow = (dow >> 1);
            }
            // Remove the last comma or set the default value
            final int idx = dowValue.toString().lastIndexOf(',');
            if (idx > 0) {
                dowValue = new StringBuilder(dowValue.substring(0, idx));
            }
        }

        dayOfWeek.setText(dowValue.toString());
        dayOfWeek.setVisibility((dowValue.length() > 0) ? View.VISIBLE : View.GONE);
        if (dayOfWeekLabel != null) {
            dayOfWeekLabel.setVisibility((dowValue.length() > 0) ? View.VISIBLE : View.GONE);
        }
    }

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
