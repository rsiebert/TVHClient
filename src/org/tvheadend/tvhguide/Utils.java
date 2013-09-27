package org.tvheadend.tvhguide;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.tvheadend.tvhguide.R.string;
import org.tvheadend.tvhguide.htsp.HTSService;
import org.tvheadend.tvhguide.model.SeriesInfo;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
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
    
    /**
     * 
     * @param info
     * @return
     */
    public static String buildSeriesInfoString(Context context, SeriesInfo info) {
        
        if (info.onScreen != null && info.onScreen.length() > 0)
            return info.onScreen;

        String s = "";
        String season = context.getResources().getString(string.pr_season);
        String episode = context.getResources().getString(string.pr_episode);
        String part = context.getResources().getString(string.pr_part);
        
        if(info.onScreen.length() > 0) {
            return info.onScreen;
        }
        
        if (info.seasonNumber > 0) {
            if (s.length() > 0)
                s += ", ";
            s += String.format("%s %02d", season.toLowerCase(), info.seasonNumber);
        }
        if (info.episodeNumber > 0) {
            if (s.length() > 0)
                s += ", ";
            s += String.format("%s %02d", episode.toLowerCase(), info.episodeNumber);
        }
        if (info.partNumber > 0) {
            if (s.length() > 0)
                s += ", ";
            s += String.format("%s %d", part.toLowerCase(), info.partNumber);
        }

        if(s.length() > 0) {
            s = s.substring(0,1).toUpperCase() + s.substring(1);
        }
        
        return s;
    }
    
    /**
     * 
     * @param context
     * @param force
     */
    public static void connect(Context context, boolean force) {

        // Get the preferences object and retrieve the login credentials
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String hostname = prefs.getString("serverHostPref", "localhost");
        int port = Integer.parseInt(prefs.getString("serverPortPref", "9982"));
        String username = prefs.getString("usernamePref", "");
        String password = prefs.getString("passwordPref", "");

        // Create an intent and pass on the data
        Intent intent = new Intent(context, HTSService.class);
        intent.setAction(HTSService.ACTION_CONNECT);
        intent.putExtra("hostname", hostname);
        intent.putExtra("port", port);
        intent.putExtra("username", username);
        intent.putExtra("password", password);
        intent.putExtra("force", force);

        // Start the service with given action and data
        context.startService(intent);
    }
}
