/*
 *  Copyright (C) 2013 Robert Siebert
 *
 * This file is part of TVHGuide.
 *
 * TVHGuide is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * TVHGuide is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with TVHGuide.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.tvheadend.tvhclient;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.tvheadend.tvhclient.R.string;
import org.tvheadend.tvhclient.adapter.GenreColorDialogAdapter;
import org.tvheadend.tvhclient.htsp.HTSService;
import org.tvheadend.tvhclient.model.Channel;
import org.tvheadend.tvhclient.model.ChannelTag;
import org.tvheadend.tvhclient.model.Connection;
import org.tvheadend.tvhclient.model.GenreColorDialogItem;
import org.tvheadend.tvhclient.model.Program;
import org.tvheadend.tvhclient.model.Recording;
import org.tvheadend.tvhclient.model.SeriesInfo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.util.DisplayMetrics;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

public class Utils {

    // Constants required for the date calculation
    private static final int twoDays = 1000 * 3600 * 24 * 2;
    private static final int sixDays = 1000 * 3600 * 24 * 6;
    
    // This is the width in pixels from the icon in the program_guide_list.xml
    // We need to subtract this value from the window width to get the real
    // usable width. The same values is also used in the
    // ProgramGuideListFragment class.
    private final static int LAYOUT_ICON_OFFSET = 66;

    // Offset that reduces the visibility of the program guide colors a little
    private final static int GENRE_COLOR_ALPHA_EPG_OFFSET = 50;

    /**
     * Returns the id of the theme that is currently set in the settings.
     * 
     * @param context
     * @return
     */
    public static int getThemeId(final Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Boolean theme = prefs.getBoolean("lightThemePref", true);
        return (theme ? R.style.CustomTheme_Light : R.style.CustomTheme);
    }

    /**
     * Returns the information if channels shall be shown or not
     * 
     * @param context
     * @return
     */
    public static boolean showChannelIcons(final Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Boolean showIcons = prefs.getBoolean("showIconPref", true);
        return showIcons;
    }
    
    /**
     * Combines the episode and series values into a single string
     * @param info
     * @return
     */
    public static String buildSeriesInfoString(final Context context, final SeriesInfo info) {
        String s = "";
        if (info == null) {
            return s;
        }

        if (info.onScreen != null && info.onScreen.length() > 0) {
            return info.onScreen;
        }
        
        final String season = context.getResources().getString(string.season);
        final String episode = context.getResources().getString(string.episode);
        final String part = context.getResources().getString(string.part);
        
        if (info.onScreen.length() > 0) {
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
        if (s.length() > 0) {
            s = s.substring(0,1).toUpperCase(Locale.getDefault()) + s.substring(1);
        }
        return s;
    }
    
    /**
     * Connects to the server with the currently active connection.
     * 
     * @param context
     * @param force
     */
    public static void connect(final Context context, final boolean force) {
        Intent intent = null;
        Connection conn = DatabaseHelper.getInstance().getSelectedConnection();
        // If we got one connection, get the values
        if (conn != null) {
            // Create an intent and pass on the connection details
            intent = new Intent(context, HTSService.class);
            intent.setAction(Constants.ACTION_CONNECT);
            intent.putExtra("hostname", conn.address);
            intent.putExtra("port", conn.port);
            intent.putExtra("username", conn.username);
            intent.putExtra("password", conn.password);
            intent.putExtra("force", force);
        }
        // Start the service with given action and data
        if (intent != null) {
            context.startService(intent);
        }
    }

    /**
     * Removes the program with the given id from the server. A dialog is shown
     * up front to confirm the deletion.
     * 
     * @param context
     * @param id
     */
    public static void confirmRemoveRecording(final Context context, final Recording rec) {
        if (rec == null) {
            return;
        }
        // Show a confirmation dialog before deleting the recording
        new AlertDialog.Builder(context)
        .setTitle(R.string.menu_record_remove)
        .setMessage(context.getString(R.string.delete_recording, rec.title))
        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                removeRecording(context, rec);
            }
        }).setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // NOP
            }
        }).show();
    }

    /**
     * Removes the program with the given id from the server.
     * 
     * @param context
     * @param id
     */
    public static void removeRecording(final Context context, final Recording rec) {
        if (rec == null) {
            return;
        }
        final Intent intent = new Intent(context, HTSService.class);
        intent.setAction(Constants.ACTION_DVR_DELETE);
        intent.putExtra("id", rec.id);
        context.startService(intent);
    }

    /**
     * Notifies the server to cancel the recording with the given id. A dialog is shown
     * up front to confirm the cancellation.
     * 
     * @param context
     * @param id
     */
    public static void confirmCancelRecording(final Context context, final Recording rec) {
        if (rec == null) {
            return;
        }
        // Show a confirmation dialog before deleting the recording
        new AlertDialog.Builder(context)
        .setTitle(R.string.menu_record_cancel)
        .setMessage(context.getString(R.string.cancel_recording, rec.title))
        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                cancelRecording(context, rec);
            }
        }).setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // NOP
            }
        }).show();
    }

    /**
     * Notifies the server to cancel the recording with the given id.
     * 
     * @param context
     * @param id
     */
    public static void cancelRecording(final Context context, final Recording rec) {
        if (rec == null) {
            return;
        }
        final Intent intent = new Intent(context, HTSService.class);
        intent.setAction(Constants.ACTION_DVR_CANCEL);
        intent.putExtra("id", rec.id);
        context.startService(intent);
    }

    /**
     * Tells the server to record the program with the given id.
     * 
     * @param context
     * @param id
     * @param channelId
     */
    public static void recordProgram(final Context context, final Program program) {
        if (program == null || program.channel == null) {
            return;
        }
        Intent intent = new Intent(context, HTSService.class);
        intent.setAction(Constants.ACTION_DVR_ADD);
        intent.putExtra("eventId", program.id);
        intent.putExtra("channelId", program.channel.id);
        context.startService(intent);
    }

    /**
     * Shows or hides certain items from the program menu. This depends on the
     * current state of the program.
     *
     * @param menu
     * @param program
     */
    public static void setProgramMenu(final Menu menu, final Program program) {
        
        MenuItem recordMenuItem = menu.findItem(R.id.menu_record);
        MenuItem recordCancelMenuItem = menu.findItem(R.id.menu_record_cancel);
        MenuItem recordRemoveMenuItem = menu.findItem(R.id.menu_record_remove);
        MenuItem playMenuItem = menu.findItem(R.id.menu_play);
        MenuItem searchMenuItemEpg = menu.findItem(R.id.menu_search_epg);
        MenuItem searchMenuItemImdb = menu.findItem(R.id.menu_search_imdb);

        // Disable all menus if the program is not valid
        if (program == null) {
            recordMenuItem.setVisible(false);
            recordCancelMenuItem.setVisible(false);
            recordRemoveMenuItem.setVisible(false);
            searchMenuItemEpg.setVisible(false);
            searchMenuItemImdb.setVisible(false);
            return;
        } 

        // Show the play menu item when the current 
        // time is between the program start and end time
        long currentTime = new Date().getTime();
        if (program.start != null && program.stop != null && 
                currentTime > program.start.getTime()
                && currentTime < program.stop.getTime()) {
            playMenuItem.setVisible(true);
        } else {
            playMenuItem.setVisible(false);
        }
        
        if (program.recording == null) {
            // Show the record menu
            recordCancelMenuItem.setVisible(false);
            recordRemoveMenuItem.setVisible(false);
        } else if (program.isRecording()) {
            // Show the cancel menu
            recordMenuItem.setVisible(false);
            recordRemoveMenuItem.setVisible(false);
        } else if (program.isScheduled()) {
            // Show the cancel and play menu
            recordMenuItem.setVisible(false);
            recordRemoveMenuItem.setVisible(false);
        } else {
            // Show the delete menu
            recordMenuItem.setVisible(false);
            recordCancelMenuItem.setVisible(false);
        }
    }

    /**
     * Shows or hides certain items from the recording menu. This depends on the
     * current state of the recording.
     * 
     * @param menu
     * @param rec
     */
    public static void setRecordingMenu(final Menu menu, final Recording rec) {

        // Get the menu items so they can be shown 
        // or hidden depending on the recording state
        MenuItem recordMenuItem = menu.findItem(R.id.menu_record);
        MenuItem recordCancelMenuItem = menu.findItem(R.id.menu_record_cancel);
        MenuItem recordRemoveMenuItem = menu.findItem(R.id.menu_record_remove);
        MenuItem playMenuItem = menu.findItem(R.id.menu_play);
        MenuItem searchMenuItemEpg = menu.findItem(R.id.menu_search_epg);
        MenuItem searchMenuItemImdb = menu.findItem(R.id.menu_search_imdb);

        // Disable these menus as a default
        recordMenuItem.setVisible(false);
        recordCancelMenuItem.setVisible(false);
        recordRemoveMenuItem.setVisible(false);
        playMenuItem.setVisible(false);
        searchMenuItemEpg.setVisible(false);
        searchMenuItemImdb.setVisible(false);

        // Disable all menus if the recording is not valid
        if (rec == null) {
            return;
        }

        // Allow searching the recordings
        searchMenuItemEpg.setVisible(true);
        searchMenuItemImdb.setVisible(true);

        if (rec.error == null && rec.state.equals("completed")) {
        	// The recording is available, it can be played and removed
            recordRemoveMenuItem.setVisible(true);
            playMenuItem.setVisible(true);
        } else if (rec.isRecording() || rec.isScheduled()) {
            // The recording is recording or scheduled, it can only be cancelled
            recordCancelMenuItem.setVisible(true);
        } else if (rec.error != null || rec.state.equals("missed")) {
        	// The recording has failed or has been missed, allow removal
        	recordRemoveMenuItem.setVisible(true);
        }
    }

    /**
     * Shows or hides certain icons from the recording menu. This is required
     * because the usual way does not seem to work.
     * 
     * @param context
     * @param menu
     */
    public static void setRecordingMenuIcons(final Context context, final Menu menu) {
        MenuItem recordCancelMenuItem = menu.findItem(R.id.menu_record_cancel);
        MenuItem recordRemoveMenuItem = menu.findItem(R.id.menu_record_remove);
        MenuItem recordCancelAllMenuItem = menu.findItem(R.id.menu_record_cancel_all);
        MenuItem recordRemoveAllMenuItem = menu.findItem(R.id.menu_record_remove_all);
        
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Boolean lightTheme = prefs.getBoolean("lightThemePref", true);
        if (recordCancelMenuItem != null) {
            recordCancelMenuItem.setIcon(lightTheme ? R.drawable.ic_menu_record_cancel_light : R.drawable.ic_menu_record_cancel_dark);
        }
        if (recordRemoveMenuItem != null) {
            recordRemoveMenuItem.setIcon(lightTheme ? R.drawable.ic_menu_record_remove_light : R.drawable.ic_menu_record_remove_dark);
        }
        if (recordCancelAllMenuItem != null) {
            recordCancelAllMenuItem.setIcon(lightTheme ? R.drawable.ic_menu_record_cancel_light : R.drawable.ic_menu_record_cancel_dark);
        }
        if (recordRemoveAllMenuItem != null) {
            recordRemoveAllMenuItem.setIcon(lightTheme ? R.drawable.ic_menu_record_remove_light : R.drawable.ic_menu_record_remove_dark);
        }
    }

    /**
     * Shows an icon for the state of the current recording. If no recording was
     * given, the icon will be hidden.
     * 
     * @param state
     * @param recording
     */
    public static void setState(ImageView state, final Recording recording) {
        if (state == null) {
            return;
        }
        // If no recording was given hide the state icon
        if (recording == null) {
            state.setImageDrawable(null);
            state.setVisibility(ImageView.GONE);
        } else {
            // Show the state icon and set the correct image
            state.setVisibility(ImageView.VISIBLE);

            if (recording.error != null) {
                state.setImageResource(R.drawable.ic_error_small);
            } else if ("completed".equals(recording.state)) {
                state.setImageResource(R.drawable.ic_success_small);
            } else if ("invalid".equals(recording.state)) {
                state.setImageResource(R.drawable.ic_error_small);
            } else if ("missed".equals(recording.state)) {
                state.setImageResource(R.drawable.ic_error_small);
            } else if ("recording".equals(recording.state)) {
                state.setImageResource(R.drawable.ic_rec_small);
            } else if ("scheduled".equals(recording.state)) {
                state.setImageResource(R.drawable.ic_schedule_small);
            } else {
                state.setImageDrawable(null);
                state.setVisibility(ImageView.GONE);
            }
        }
    }

    /**
     * Shows the given duration for the given view. If the duration is zero the
     * view will be hidden.
     * 
     * @param duration
     * @param start
     * @param stop
     */
    public static void setDuration(TextView duration, final Date start, final Date stop) {
        if (duration == null || start == null || stop == null) {
            return;
        }
        duration.setVisibility(View.VISIBLE);
        // Get the start and end times so we can show them
        // and calculate the duration. Then show the duration in minutes
        final double durationTime = ((stop.getTime() - start.getTime()) / 1000 / 60);
        final String s = duration.getContext().getString(R.string.minutes, (int) durationTime);
        duration.setText(duration.getContext().getString(R.string.minutes, (int) durationTime));
        duration.setVisibility((s.length() > 0) ? View.VISIBLE : View.GONE);
    }

    /**
     * Shows the given time for the given view.
     * 
     * @param time
     * @param start
     * @param stop
     */
    public static void setTime(TextView time, final Date start, final Date stop) {
        if (time == null || start == null || stop == null) {
            return;
        }
        time.setVisibility(View.VISIBLE);
        final String startTime = DateFormat.getTimeFormat(time.getContext()).format(start);
        final String endTime = DateFormat.getTimeFormat(time.getContext()).format(stop); 
        time.setText(startTime + " - " + endTime);
    }

    /**
     * Shows the given date. The date for the first days will be shown as words.
     * After one week the date value will be used.
     * 
     * @param date
     * @param start
     */
    public static void setDate(TextView date, final Date start) {
        if (date == null || start == null) {
            return;
        }
        String dateText = "";
        if (DateUtils.isToday(start.getTime())) {
            // Show the string today
            dateText = date.getContext().getString(R.string.today);
        } else if (start.getTime() < System.currentTimeMillis() + twoDays
                && start.getTime() > System.currentTimeMillis() - twoDays) {
            // Show a string like "42 minutes ago"
            dateText = DateUtils.getRelativeTimeSpanString(start.getTime(), System.currentTimeMillis(),
                    DateUtils.DAY_IN_MILLIS).toString();
        } else if (start.getTime() < System.currentTimeMillis() + sixDays
                && start.getTime() > System.currentTimeMillis() - twoDays) {
            // Show the day of the week, like Monday or Tuesday
            SimpleDateFormat sdf = new SimpleDateFormat("EEEE", Locale.US);
            dateText = sdf.format(start.getTime());
        } else {
            // Show the regular date format like 31.07.2013
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.US);
            dateText = sdf.format(start.getTime());
        }

        // Translate the day strings
        if (dateText.equals("today")) {
            date.setText(R.string.today);
        } else if (dateText.equals("tomorrow")) {
            date.setText(R.string.tomorrow);
        } else if (dateText.equals("in 2 days")) {
            date.setText(R.string.in_2_days);
        } else if (dateText.equals("Monday")) {
            date.setText(R.string.monday);
        } else if (dateText.equals("Tuesday")) {
            date.setText(R.string.tuesday);
        } else if (dateText.equals("Wednesday")) {
            date.setText(R.string.wednesday);
        } else if (dateText.equals("Thursday")) {
            date.setText(R.string.thursday);
        } else if (dateText.equals("Friday")) {
            date.setText(R.string.friday);
        } else if (dateText.equals("Saturday")) {
            date.setText(R.string.saturday);
        } else if (dateText.equals("Sunday")) {
            date.setText(R.string.sunday);
        } else if (dateText.equals("yesterday")) {
            date.setText(R.string.yesterday);
        } else if (dateText.equals("2 days ago")) {
            date.setText(R.string.two_days_ago);
        } else {
            date.setText(dateText);
        }
    }

    /**
     * Shows the given series text for the given view. If the text is empty
     * then the view will be hidden.
     * 
     * @param seriesInfoLabel
     * @param seriesInfo
     * @param si
     */
    public static void setSeriesInfo(TextView seriesInfoLabel, TextView seriesInfo, final SeriesInfo si) {
        if (seriesInfo != null) {
            final String s = Utils.buildSeriesInfoString(seriesInfo.getContext(), si);
            seriesInfo.setText(s);
            seriesInfo.setVisibility((s.length() > 0) ? View.VISIBLE : View.GONE);
            if (seriesInfoLabel != null) {
                seriesInfoLabel.setVisibility((s.length() > 0) ? View.VISIBLE : View.GONE);
            }
        }
    }

    /**
     * Shows the given content type text for the given view. If the text is empty
     * then the view will be hidden.
     * 
     * @param contentTypeLabel
     * @param contentType
     * @param ct
     */
    public static void setContentType(TextView contentTypeLabel, TextView contentType, final int ct) {
        if (contentType == null) {
            return;
        }
        final SparseArray<String> ctl = TVHClientApplication.getContentTypes(contentType.getContext());
        final String type = ctl.get(ct, "");
        contentType.setText(type);
        contentType.setVisibility((type.length() > 0) ? View.VISIBLE : View.GONE);
        if (contentTypeLabel != null) {
            contentTypeLabel.setVisibility((type.length() > 0) ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * Shows the channel icon and optionally the channel name. The icon will
     * only be shown when the user has activated the setting and an icon is 
     * actually available. If no icon is available the channel name will be 
     * shown as a placeholder.
     * 
     * @param icon
     * @param iconText
     * @param channel
     * @param ch
     */
    public static void setChannelIcon(ImageView icon, TextView iconText, final Channel ch) {
        if (icon != null && ch != null) {
            // Get the setting if the channel icon shall be shown or not
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(icon.getContext());
            final boolean showIcons = prefs.getBoolean("showIconPref", true);

            // Show the channels icon if available. If not hide the view. 
            if (icon != null) {
                icon.setImageBitmap((ch.iconBitmap != null) ? ch.iconBitmap : null);
                icon.setVisibility((showIcons && ch.iconBitmap != null) ? ImageView.VISIBLE : ImageView.GONE);
            }
            
            // If the channel icon is not available show the channel name as a placeholder.
            if (iconText != null) {
                iconText.setText(ch.name);
                iconText.setVisibility((showIcons && ch.iconBitmap == null) ? ImageView.VISIBLE : ImageView.GONE);
            }
        }
    }

    /**
     * Shows the given description text for the given view. If the text is empty
     * then the view will be hidden.
     * 
     * @param descriptionLabel
     * @param description
     * @param desc
     */
    public static void setDescription(TextView descriptionLabel, TextView description, final String desc) {
        if (description == null) {
            return;
        }
        description.setText(desc);
        description.setVisibility((desc.length() > 0) ? View.VISIBLE : View.GONE);
        if (descriptionLabel != null) {
            descriptionLabel.setVisibility((desc.length() > 0) ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * Shows the reason why a recording has failed. If the text is empty then
     * the view will be hidden.
     * 
     * @param failed_reason
     * @param rec
     */
    public static void setFailedReason(final TextView failed_reason, final Recording rec) {
        if (failed_reason == null) {
            return;
        }

        // Make the text field visible as a default
        failed_reason.setVisibility(View.VISIBLE);

        // Show the reason why it failed
        if (rec.error != null && rec.error.equals("File missing")) {
            failed_reason.setText(failed_reason.getResources().getString(R.string.recording_file_missing));
        } else if (rec.error != null && rec.error.equals("Aborted by user")) {
            failed_reason.setText(failed_reason.getResources().getString(R.string.recording_canceled));
        } else if (rec.state != null && rec.state.equals("missed")) {
            failed_reason.setText(failed_reason.getResources().getString(R.string.recording_time_missed));
        } else if (rec.state != null && rec.state.equals("invalid")) {
            failed_reason.setText(failed_reason.getResources().getString(R.string.recording_file_invalid));
        } else {
            failed_reason.setVisibility(View.GONE);
        }
    }

    /**
     * Shows the progress as a progress bar.
     * 
     * @param progress
     * @param start
     * @param stop
     */
    public static void setProgress(ProgressBar progress, final Date start, final Date stop) {
        if (progress == null || start == null || stop == null) {
            return;
        }
        // Get the start and end times to calculate the progress.
        double durationTime = (stop.getTime() - start.getTime());
        double elapsedTime = new Date().getTime() - start.getTime();
        
        // Show the progress as a percentage
        double percent = 0;
        if (durationTime > 0) {
            percent = elapsedTime / durationTime;
        }
        progress.setProgress((int) Math.floor(percent * 100));
        progress.setVisibility(View.VISIBLE);
    }
    
    /**
     * Shows the progress not as a progress bar but as a text with the
     * percentage symbol.
     * 
     * @param progressText
     * @param start
     * @param stop
     */
    public static void setProgressText(TextView progressText, final Date start, final Date stop) {
        if (progressText == null || start == null || stop == null) {
            return;
        }
        // Get the start and end times to calculate the progress.
        final double durationTime = (stop.getTime() - start.getTime());
        final double elapsedTime = new Date().getTime() - start.getTime();
        
        // Show the progress as a percentage
        double percent = 0;
        if (durationTime > 0) {
            percent = elapsedTime / durationTime;
        }
        int progress = (int) Math.floor(percent * 100);
        if (progress > 0) {
            progressText.setText(progressText.getResources().getString(R.string.progress, progress));
            progressText.setVisibility(View.VISIBLE);
        } else {
            progressText.setVisibility(View.GONE);
        }
    }
    
    /**
     * 
     * @param context
     * @param view
     * @param program
     */
    public static void setGenreColor(final Context context, View view, final Program program, final String tag) {
    	if (view == null) {
            return;
        }
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean showGenre = false;
        int offset = 0;

    	// Check which class is calling and get the setting
        if (tag.equals("ChannelListAdapter")) {
            showGenre = prefs.getBoolean("showGenreColorsChannelsPref", false);
        } else if (tag.equals("ProgramListAdapter")) {
            showGenre = prefs.getBoolean("showGenreColorsProgramsPref", false);
        } else if (tag.equals("SearchResultAdapter")) {
            showGenre = prefs.getBoolean("showGenreColorsSearchPref", false);
        } else if (tag.equals("ProgramGuideItemView")) {
        	showGenre = prefs.getBoolean("showGenreColorsGuidePref", false);
        	offset = GENRE_COLOR_ALPHA_EPG_OFFSET;
        }
        
        // As a default we show a transparent color. If we have a program then
        // use the provided genre color. If genre colors shall not be shown we 
        // also show the transparent color. This is used in the EPG where the 
        // background is used as the genre indicator. 
        int color = context.getResources().getColor(android.R.color.transparent);
        if (program != null && showGenre) {
            color = getGenreColor(context, program.contentType, offset);
        }

        if (view instanceof TextView) {
        	if (showGenre) {
        	    view.setBackgroundColor(color);
        	    view.setVisibility(View.VISIBLE);
        	} else {
        		view.setVisibility(View.GONE);
        	}
        } else if (view instanceof LinearLayout) {
        	// Get the shape where the background color will be set 
	        LayerDrawable layers = (LayerDrawable) view.getBackground();
	        GradientDrawable shape = (GradientDrawable) (layers.findDrawableByLayerId(R.id.timeline_item_genre));
	        shape.setColor(color);
        }
    }

    /**
     * Returns the background color of the genre based on the content type. The
     * first byte of hex number represents the main category.
     * 
     * @param contentType
     * @return
     */
    public static int getGenreColor(final Context context, final int contentType, final int offset) {
        if (contentType == 0) {
            // Return a fully transparent color in case no genre is available
            return context.getResources().getColor(android.R.color.transparent);
        } else {
            // Get the genre color from the content type
            int color = R.color.EPG_OTHER;
            int type = (contentType / 16) - 1;
            switch (type) {
            case 1:
                color = R.color.EPG_MOVIES;
                break;
            case 2:
                color = R.color.EPG_NEWS;
                break;
            case 3:
                color = R.color.EPG_SHOWS;
                break;
            case 4:
                color = R.color.EPG_SPORTS;
                break;
            case 5:
                color = R.color.EPG_CHILD;
                break;
            case 6:
                color = R.color.EPG_MUSIC;
                break;
            case 7:
                color = R.color.EPG_ARTS;
                break;
            case 8:
                color = R.color.EPG_SOCIAL;
                break;
            case 9:
                color = R.color.EPG_SCIENCE;
                break;
            case 10:
                color = R.color.EPG_HOBBY;
                break;
            case 11:
                color = R.color.EPG_SPECIAL;
                break;
            }
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            
            // Get the color with the desired alpha value
        	int c = context.getResources().getColor(color);
        	int alpha = (int) (((float) prefs.getInt("showGenreColorsVisibilityPref", 70)) / 100.0f * 255.0f);
        	if (alpha >= offset) {
        		alpha -= offset;
        	}
        	return Color.argb(alpha, Color.red(c), Color.green(c), Color.blue(c));
        }
    }

    /**
     * Prepares a dialog that shows the available genre colors and the names. In
     * here the data for the adapter is created and the dialog prepared which
     * can be shown later.
     */
    public static void showGenreColorDialog(final Activity context) {
        final String[] s = context.getResources().getStringArray(R.array.pr_content_type0);

        // Fill the list for the adapter
        List<GenreColorDialogItem> items = new ArrayList<GenreColorDialogItem>();
        for (int i = 0; i < s.length; ++i) {
            GenreColorDialogItem item = new GenreColorDialogItem();
            item.color = getGenreColor(context, ((i + 1) * 16), 0);
            item.genre = s[i];
            items.add(item);
        }

        // Create the dialog and set the adapter
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.genre_color_list);
        builder.setAdapter(new GenreColorDialogAdapter(context, items), null);

        AlertDialog genreColorDialog = builder.create();
        genreColorDialog.show();
    }

    /**
     * Gets the last program id from the given channel. The id is used to tell
     * the server where to continue loading programs.
     * 
     * @param context
     * @param channel
     */
    public static void loadMorePrograms(final Context context, final Channel channel) {
        if (channel == null) {
            return;
        }

        Program p = null;
        long nextId = 0;
        synchronized(channel.epg) {
            Iterator<Program> it = channel.epg.iterator();
            while (it.hasNext()) {
                p = it.next();
                // Check if there is a next program available or if the current
                // program has an id for the next one
                if (p.id != nextId && nextId != 0) {
                    break;
                }
                // Get the next id of the program so we can check in
                // the next iteration if this program is the last one.
                nextId = p.nextId;
            }
        }

        if (p == null) {
            return;
        }
        // In case the while loop was not entered get the next id 
        // or if there is none the current id if the program.
        if (nextId == 0) {
            nextId = p.nextId;
        }
        if (nextId == 0) {
            nextId = p.id;
        }

        // Set the required information and start the service command.
        Intent intent = new Intent(context, HTSService.class);
        intent.setAction(Constants.ACTION_GET_EVENTS);
        intent.putExtra("eventId", nextId);
        intent.putExtra("channelId", channel.id);
        intent.putExtra("count", Constants.PREF_PROGRAMS_TO_LOAD);
        context.startService(intent);
    }

    /**
     * Calculates the available display width of one minute in pixels. This
     * depends how wide the screen is and how many hours shall be shown in one
     * screen.
     * 
     * @param context
     * @param tabIndex
     * @param hoursToShow
     * @return
     */
    public static float getPixelsPerMinute(final Activity context, final int tabIndex, final int hoursToShow) {
        // Get the usable width. Subtract the icon width if its visible.
        DisplayMetrics displaymetrics = new DisplayMetrics();
        context.getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        float displayWidth = displaymetrics.widthPixels - ((tabIndex == 0) ? LAYOUT_ICON_OFFSET : 0);
        float pixelsPerMinute = ((float) displayWidth / (60.0f * (float) hoursToShow));
        return pixelsPerMinute;
    }

    public static int getChannelTagId() {
        // Get the selected tag for the active connection in the database. If
        // none exist then use the variable here.
    	if (DatabaseHelper.getInstance() != null) {
	        Connection conn = DatabaseHelper.getInstance().getSelectedConnection();
	        if (conn != null) {
	            return conn.channelTag;
	        }
    	}
        return 0;
    }

    /**
     * Saves the channel tag for the active connection to remember it across
     * application starts and connection changes.
     * 
     * @param channelTagId
     */
    public static void setChannelTagId(final int channelTagId) {
        // Save the selected tag for the active connection in the database
    	if (DatabaseHelper.getInstance() != null) {
	        Connection conn = DatabaseHelper.getInstance().getSelectedConnection();
	        if (conn != null) {
	            conn.channelTag = channelTagId;
	            DatabaseHelper.getInstance().updateConnection(conn);
	        }
	    }
    }

    /**
     * Returns the saved channel tag for the active connection.
     * 
     * @param app
     * @return
     */
    public static ChannelTag getChannelTag(final TVHClientApplication app) {
        List<ChannelTag> ctl = app.getChannelTags();        
        if (ctl.size() > getChannelTagId()) {
            return ctl.get(getChannelTagId());
        }
        return null;
    }

	/**
	 * Change the language to the defined setting. If the default is set then
	 * let the application decide which language shall be used.
	 *
	 * @param context
	 */
	public static void setLanguage(final Activity context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		String locale = prefs.getString("languagePref", "default");
		if (!locale.equals("default")) {
			Configuration config = new Configuration(context.getResources().getConfiguration());
			config.locale = new Locale(locale);
			context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
		}
	}

    /**
     * Returns the type how the channels are sorted in the adapter and in which
     * order they will then be shown in the list.
     * 
     * @param context
     * @return
     */
	public static int getChannelSortOrder(final Activity context) {
	    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return Integer.parseInt(prefs.getString("sortChannelsPref", String.valueOf(Constants.CHANNEL_SORT_DEFAULT)));
	}
}
