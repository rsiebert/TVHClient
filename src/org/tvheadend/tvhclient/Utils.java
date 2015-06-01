package org.tvheadend.tvhclient;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
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
import org.tvheadend.tvhclient.model.Profile;
import org.tvheadend.tvhclient.model.Program;
import org.tvheadend.tvhclient.model.Recording;
import org.tvheadend.tvhclient.model.SeriesInfo;
import org.tvheadend.tvhclient.model.SeriesRecording;
import org.tvheadend.tvhclient.model.TimerRecording;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;

public class Utils {

    @SuppressWarnings("unused")
    private final static String TAG = Utils.class.getSimpleName();

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
     * Returns the information if channels shall be shown or not
     * 
     * @param context
     * @return
     */
    public static boolean showChannelTagIcon(final Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Boolean showIcons = prefs.getBoolean("showTagIconPref", false);
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
        // Create an intent and pass on the connection details
        Intent intent = new Intent(context, HTSService.class);
        intent.setAction(Constants.ACTION_CONNECT);

        final Connection conn = DatabaseHelper.getInstance().getSelectedConnection();
        // If we got one connection, get the values
        if (conn != null) {
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
     * 
     * @param context
     * @param rec
     */
    public static void confirmRemoveRecording(final Context context, final Recording rec) {
        confirmRemoveRecording(context, 
                Constants.ACTION_DELETE_DVR_ENTRY, 
                rec.title, 
                String.valueOf(rec.id));
    }

    /**
     * 
     * @param context
     * @param srec
     */
    public static void confirmRemoveRecording(final Context context, final SeriesRecording srec) {
        confirmRemoveRecording(context, 
                Constants.ACTION_DELETE_SERIES_DVR_ENTRY, 
                srec.title, 
                srec.id);
    }

    /**
     * 
     * @param context
     * @param trec
     */
    public static void confirmRemoveRecording(final Context context, final TimerRecording trec) {
        confirmRemoveRecording(context, 
                Constants.ACTION_DELETE_TIMER_REC_ENTRY, 
                (trec.name.length() > 0 ? trec.name : trec.title), 
                trec.id);
    }

    /**
     * Removes the recording with the given id from the server. A dialog is
     * shown up front to confirm the deletion.
     * 
     * @param context
     * @param type
     * @param title
     * @param id
     */
    public static void confirmRemoveRecording(final Context context, final String type, final String title, final String id) {

        String message = "";
        if (type == Constants.ACTION_DELETE_DVR_ENTRY) {
            message = context.getString(R.string.remove_recording, title);
        } else if (type == Constants.ACTION_DELETE_SERIES_DVR_ENTRY) {
            message = context.getString(R.string.remove_series_recording, title);
        } else if (type == Constants.ACTION_DELETE_TIMER_REC_ENTRY) {
            message = context.getString(R.string.remove_timer_recording, title);
        }

        // Show a confirmation dialog before deleting the recording
        new MaterialDialog.Builder(context)
                .title(R.string.record_remove)
                .content(message).positiveText(R.string.remove)
                .negativeText(R.string.cancel)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        removeRecording(context, id, type);
                    }

                    @Override
                    public void onNegative(MaterialDialog dialog) {
                        // NOP
                    }
                }).show();
    }

    /**
     * 
     * @param context
     * @param id
     * @param type
     */
    public static void removeRecording(final Context context, final String id, final String type) {
        final Intent intent = new Intent(context, HTSService.class);
        intent.setAction(type);
        intent.putExtra("id", id);
        context.startService(intent);
    }

    /**
     * Notifies the server to cancel the recording with the given id. A dialog is shown
     * up front to confirm the cancellation.
     * 
     * @param context
     * @param rec
     */
    public static void confirmCancelRecording(final Context context, final Recording rec) {
        if (rec == null) {
            return;
        }
        // Show a confirmation dialog before deleting the recording
        // Show a confirmation dialog before deleting the recording
        new MaterialDialog.Builder(context)
                .title(R.string.record_cancel)
                .content(context.getString(R.string.cancel_recording, rec.title))
                .positiveText(R.string.remove)
                .negativeText(R.string.cancel)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        cancelRecording(context, rec);
                    }

                    @Override
                    public void onNegative(MaterialDialog dialog) {
                        // NOP
                    }
                }).show();
    }

    /**
     * Notifies the server to cancel the recording with the given id.
     * 
     * @param context
     * @param rec
     */
    public static void cancelRecording(final Context context, final Recording rec) {
        if (rec == null) {
            return;
        }
        final Intent intent = new Intent(context, HTSService.class);
        intent.setAction(Constants.ACTION_CANCEL_DVR_ENTRY);
        intent.putExtra("id", rec.id);
        context.startService(intent);
    }

    /**
     * Tells the server to record the program with the given id. If the
     * useSeriesRecording is set then a series recording rule will be created to
     * record that program repeatedly.
     * 
     * @param context
     * @param program
     * @param useSeriesRecording
     */
    public static void recordProgram(final Activity activity, final Program program, final boolean useSeriesRecording) {
        if (program == null || program.channel == null) {
            return;
        }
        Intent intent = new Intent(activity, HTSService.class);
        if (!useSeriesRecording) {
            intent.setAction(Constants.ACTION_ADD_DVR_ENTRY);
            intent.putExtra("eventId", program.id);
        } else {
            intent.setAction(Constants.ACTION_ADD_SERIES_DVR_ENTRY);
            intent.putExtra("title", program.title);
        }

        // Add the recording profile if available and enabled
        final TVHClientApplication app = (TVHClientApplication) activity.getApplication();
        final Connection conn = DatabaseHelper.getInstance().getSelectedConnection();
        final Profile p = DatabaseHelper.getInstance().getProfile(conn.recording_profile_id);
        if (p != null 
                && p.enabled
                && app.getProtocolVersion() >= Constants.MIN_API_VERSION_PROFILES
                && app.isUnlocked()) {
            intent.putExtra("configName", p.name);
        }

        intent.putExtra("channelId", program.channel.id);
        activity.startService(intent);
    }

    /**
     * Shows or hides certain items from the program menu. This depends on the
     * current state of the program.
     *
     * @param menu
     * @param program
     */
    public static void setProgramMenu(final Menu menu, final Program program) {
        MenuItem recordOnceMenuItem = menu.findItem(R.id.menu_record_once);
        MenuItem recordSeriesMenuItem = menu.findItem(R.id.menu_record_series);
        MenuItem recordCancelMenuItem = menu.findItem(R.id.menu_record_cancel);
        MenuItem recordRemoveMenuItem = menu.findItem(R.id.menu_record_remove);
        MenuItem playMenuItem = menu.findItem(R.id.menu_play);
        MenuItem searchMenuItemEpg = menu.findItem(R.id.menu_search_epg);
        MenuItem searchMenuItemImdb = menu.findItem(R.id.menu_search_imdb);

        // Disable these menus as a default
        recordOnceMenuItem.setVisible(false);
        recordSeriesMenuItem.setVisible(false);
        recordCancelMenuItem.setVisible(false);
        recordRemoveMenuItem.setVisible(false);
        searchMenuItemEpg.setVisible(false);
        searchMenuItemImdb.setVisible(false);

        // Exit if the recording is not valid
        if (program == null) {
            return;
        }

        // Allow searching the program
        searchMenuItemEpg.setVisible(true);
        searchMenuItemImdb.setVisible(true);

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
            recordOnceMenuItem.setVisible(true);
            recordSeriesMenuItem.setVisible(true);
        } else if (program.isRecording()) {
            // Show the play and cancel menu
            playMenuItem.setVisible(true);
            recordCancelMenuItem.setVisible(true);
        } else if (program.isScheduled()) {
            // Show the cancel menu
            recordCancelMenuItem.setVisible(true);
        } else {
            // Show the delete menu
            recordRemoveMenuItem.setVisible(true);
        }
    }

    /**
     * Shows an icon for the state of the current recording. If no recording was
     * given, the icon will be hidden.
     * 
     * @param activity
     * @param state
     * @param p
     */
    public static void setState(Activity activity, ImageView state, final Program p) {
        if (state == null) {
            return;
        }

        // If no recording was given hide the state icon
        if (p == null || p.recording == null) {
            state.setImageDrawable(null);
            state.setVisibility(ImageView.GONE);
        } else {
            // Show the state icon and set the correct image
            state.setVisibility(ImageView.VISIBLE);

            TVHClientApplication app = (TVHClientApplication) activity.getApplication();
            Recording rec = app.getRecording(p.recording.id);

            if (rec == null || rec.error != null) {
                state.setImageResource(R.drawable.ic_error_small);
            } else if ("completed".equals(rec.state)) {
                state.setImageResource(R.drawable.ic_success_small);
            } else if ("invalid".equals(rec.state)) {
                state.setImageResource(R.drawable.ic_error_small);
            } else if ("missed".equals(rec.state)) {
                state.setImageResource(R.drawable.ic_error_small);
            } else if ("recording".equals(rec.state)) {
                state.setImageResource(R.drawable.ic_rec_small);
            } else if ("scheduled".equals(rec.state)) {
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
     * 
     * @param dayOfWeekLabel
     * @param dayOfWeek
     * @param dow
     */
    public static void setDaysOfWeek(Context context, TextView dayOfWeekLabel, TextView dayOfWeek, long dow) {
        if (dayOfWeek == null) {
            return;
        }
        String dowValue = "";

        // Use different strings if either no days or all days are chosen. If
        // certain days are selected check which ones.
        if (dow == 0) {
            dowValue = context.getString(R.string.no_days);
        } else if (dow == 127) {
            dowValue = context.getString(R.string.all_days);
        } else {
            String[] dayNames = context.getResources().getStringArray(R.array.day_short_names);

            // Use bit shifting to check if the first bit it set. The values are:
            // 0 = no days, 1 = Monday, 2 = Tuesday, 4 = Wednesday, 8 = Thursday,
            // 16 = Friday, 32 = Saturday, 64 = Sunday
            for (int i = 0; i < 7; ++i) {
                if ((dow & 1) == 1) {
                    dowValue += dayNames[i] + ", ";
                }
                dow = (dow >> 1);
            }
            // Remove the last comma or set the default value
            final int idx = dowValue.lastIndexOf(',');
            if (idx > 0) {
                dowValue = dowValue.substring(0, idx);
            }
        }

        dayOfWeek.setText(dowValue);
        dayOfWeek.setVisibility((dowValue.length() > 0) ? View.VISIBLE : View.GONE);
        if (dayOfWeekLabel != null) {
            dayOfWeekLabel.setVisibility((dowValue.length() > 0) ? View.VISIBLE : View.GONE);
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
        if (icon != null) {
            // Get the setting if the channel icon shall be shown or not
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(icon.getContext());
            final boolean showIcons = prefs.getBoolean("showIconPref", true);

            if (ch != null) {
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
            } else {
                // Show a blank icon if no channel icon exists and they shall be shown. 
                icon.setImageBitmap(null);
                icon.setVisibility(showIcons ? ImageView.VISIBLE : ImageView.GONE);
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
        new MaterialDialog.Builder(context)
                .title(R.string.genre_color_list)
                .adapter(new GenreColorDialogAdapter(context, items), null)
                .show();
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
        intent.putExtra(Constants.BUNDLE_PROGRAM_ID, nextId);
        intent.putExtra(Constants.BUNDLE_CHANNEL_ID, channel.id);
        intent.putExtra(Constants.BUNDLE_COUNT, Constants.PREF_PROGRAMS_TO_LOAD);
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

    /**
     * Returns the id of the channel tag that is saved in the current connection
     * 
     * @return
     */
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

    /**
     * Returns the public key that is required to make in-app purchases. The key
     * which is ciphered is located in the assets folder.
     * 
     * @return
     */
	public static String getPublicKey(Context context) {
        StringBuilder sb = new StringBuilder();
        try {
            String keyData;
            InputStream is = context.getAssets().open("public_key");
            BufferedReader in = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            while ((keyData = in.readLine()) != null) {
                sb.append(keyData);
            }
            in.close();
        } catch (Exception ex) {
            // NOP
        }

        final String SALT = "rsiebert80@gmail.com";

        // Note that this is not plain public key but public key encoded with
        // x() method. As symmetric ciphering is used in x() the same method is
        // used for both ciphering and deciphering. Additionally result of the
        // ciphering is converted to Base64 string => for deciphering with need
        // to convert it back. Generally, x(fromBase64(toBase64(x(PK, salt))),
        // salt) == PK To cipher use toX(), to decipher - fromX()
        return fromX(sb.toString(), SALT);
    }

    /**
     * Method deciphers previously ciphered message
     * @param message ciphered message
     * @param salt salt which was used for ciphering
     * @return deciphered message
     */
    static String fromX(String message, String salt) {
        return x(new String(Base64.decode(message, 0)), salt);
    }

    /**
     * Method ciphers message. Later {@link #fromX} method might be used for deciphering
     * @param message message to be ciphered
     * @param salt salt to be used for ciphering
     * @return ciphered message
     */
    static String toX(String message, String salt) {
        return new String(Base64.encode(x(message, salt).getBytes(), 0));
    }

    /**
     * Symmetric algorithm used for ciphering/deciphering. Note that in your application you probably want to modify
     * the algorithm used for ciphering/deciphering.
     * @param message message
     * @param salt salt
     * @return ciphered/deciphered message
     */
    static String x(String message, String salt) {
        final char[] m = message.toCharArray();
        final char[] s = salt.toCharArray();

        final int ml = m.length;
        final int sl = s.length;
        final char[] result = new char[ml];

        for (int i = 0; i < ml; i++) {
            result[i] = (char) (m[i] ^ s[i % sl]);
        }
        return new String(result);
    }
}
