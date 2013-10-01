package org.tvheadend.tvhguide;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.tvheadend.tvhguide.R.string;
import org.tvheadend.tvhguide.htsp.HTSService;
import org.tvheadend.tvhguide.model.Program;
import org.tvheadend.tvhguide.model.Recording;
import org.tvheadend.tvhguide.model.SeriesInfo;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;
import android.view.Menu;
import android.view.MenuItem;

public class Utils {

    public static int getThemeId(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Boolean theme = prefs.getBoolean("lightThemePref", false);
        return (theme ? R.style.CustomTheme_Light : R.style.CustomTheme);
    }

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
            s += String.format("%s %02d", season.toLowerCase(Locale.getDefault()), info.seasonNumber);
        }
        if (info.episodeNumber > 0) {
            if (s.length() > 0)
                s += ", ";
            s += String.format("%s %02d", episode.toLowerCase(Locale.getDefault()), info.episodeNumber);
        }
        if (info.partNumber > 0) {
            if (s.length() > 0)
                s += ", ";
            s += String.format("%s %d", part.toLowerCase(Locale.getDefault()), info.partNumber);
        }

        if(s.length() > 0) {
            s = s.substring(0,1).toUpperCase(Locale.getDefault()) + s.substring(1);
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

    /**
     * 
     * @param context
     * @param id
     */
    public static void removeProgram(final Context context, long id) {
        final Intent intent = new Intent(context, HTSService.class);
        intent.setAction(HTSService.ACTION_DVR_DELETE);
        intent.putExtra("id", id);
        
        // Show a confirmation dialog before deleting the recording
        new AlertDialog.Builder(context).setTitle(R.string.menu_record_remove)
        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                context.startService(intent);
            }
        }).setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // NOP
            }
        }).show();
        
        context.startService(intent);
    }

    public static void cancelProgram(final Context context, long id) {
        Intent intent = new Intent(context, HTSService.class);
        intent.setAction(HTSService.ACTION_DVR_CANCEL);
        intent.putExtra("id", id);
        context.startService(intent);
    }

    public static void recordProgram(final Context context, long id, long channelId) {
        Intent intent = new Intent(context, HTSService.class);
        intent.setAction(HTSService.ACTION_DVR_ADD);
        intent.putExtra("eventId", id);
        intent.putExtra("channelId", channelId);
        context.startService(intent);
    }

    public static void setProgramMenu(final Menu menu, final Program program) {
        
        MenuItem recordMenuItem = menu.findItem(R.id.menu_record);
        MenuItem recordCancelMenuItem = menu.findItem(R.id.menu_record_cancel);
        MenuItem recordRemoveMenuItem = menu.findItem(R.id.menu_record_remove);
        MenuItem playMenuItem = menu.findItem(R.id.menu_play);
        MenuItem searchMenuItem = menu.findItem(R.id.menu_search);
        
        // Disable these menus as a default
        searchMenuItem.setVisible(false);
        
        if (program.recording == null) {
            // Show the record menu
            playMenuItem.setVisible(false);
            recordCancelMenuItem.setVisible(false);
            recordRemoveMenuItem.setVisible(false);
        }
        else if (program.isRecording() || program.isScheduled()) {
            // Show the cancel menu
            recordMenuItem.setVisible(false);
            recordRemoveMenuItem.setVisible(false);
        }
        else {
            // Show the delete menu
            recordMenuItem.setVisible(false);
            recordCancelMenuItem.setVisible(false);
        }
    }

    public static void setRecordingMenu(final Menu menu, final Recording rec) {

        // Get the menu items so they can be shown 
        // or hidden depending on the recording state
        MenuItem recordMenuItem = menu.findItem(R.id.menu_record);
        MenuItem recordCancelMenuItem = menu.findItem(R.id.menu_record_cancel);
        MenuItem recordRemoveMenuItem = menu.findItem(R.id.menu_record_remove);
        MenuItem playMenuItem = menu.findItem(R.id.menu_play);
        MenuItem searchMenuItem = menu.findItem(R.id.menu_search);

        // Disable these menus as a default
        searchMenuItem.setVisible(false);
        
        if (rec.isRecording() || rec.isScheduled()) {
            // Show the cancel menu
            recordMenuItem.setVisible(false);
            recordRemoveMenuItem.setVisible(false);
            playMenuItem.setVisible(false);
        }
        else {
            // Show the delete and play menu
            recordMenuItem.setVisible(false);
            recordCancelMenuItem.setVisible(false);
        }
    }
}
