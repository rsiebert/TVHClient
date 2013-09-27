package org.tvheadend.tvhguide;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.content.Context;
import android.text.format.DateUtils;

public class Utils {

    /**
     * 
     * @param context
     * @param start
     * @return
     */
    public static String getStartDate(Context context, Date start) {
        String dateText = "";

        if (DateUtils.isToday(start.getTime())) {
            // Show the string today
            dateText = context.getString(R.string.today);
        }
        else if (start.getTime() < System.currentTimeMillis() + 1000 * 60 * 60 * 24 * 2
                && start.getTime() > System.currentTimeMillis() - 1000 * 60 * 60 * 24 * 2) {
            // Show a string like "42 minutes ago"
            dateText = DateUtils.getRelativeTimeSpanString(start.getTime(), System.currentTimeMillis(),
                    DateUtils.DAY_IN_MILLIS).toString();
        }
        else if (start.getTime() < System.currentTimeMillis() + 1000 * 60 * 60 * 24 * 6
                && start.getTime() > System.currentTimeMillis() - 1000 * 60 * 60 * 24 * 2) {
            // Show the day of the week, like Monday or Tuesday
            SimpleDateFormat sdf = new SimpleDateFormat("EEEE", Locale.US);
            dateText = sdf.format(start.getTime());
        }
        else {
            // Show the regular date format like 31.07.2013
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.US);
            dateText = sdf.format(start.getTime());
        }
        return dateText;
    }
}
