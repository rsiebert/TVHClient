package org.tvheadend.tvhclient.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.tvheadend.tvhclient.Constants;
import org.tvheadend.tvhclient.DataStorage;
import org.tvheadend.tvhclient.DatabaseHelper;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.htsp.HTSService;
import org.tvheadend.tvhclient.model.Channel;
import org.tvheadend.tvhclient.model.ChannelTag;
import org.tvheadend.tvhclient.model.Connection;
import org.tvheadend.tvhclient.model.Program;
import org.tvheadend.tvhclient.model.Recording;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Utils {

    private final static String TAG = Utils.class.getSimpleName();

    // Constants required for the date calculation
    private static final int twoDays = 1000 * 3600 * 24 * 2;
    private static final int sixDays = 1000 * 3600 * 24 * 6;
    
    // This is the width in pixels from the icon in the program_guide_list.xml
    // We need to subtract this value from the window width to get the real
    // usable width. The same values is also used in the
    // ProgramGuideListFragment class.
    private final static int LAYOUT_ICON_OFFSET = 66;

    /**
     * Connects to the server with the currently active connection.
     *
     * @param context Context
     * @param force   Set to true to close and reconnect to the server, then get all data. Set it to false to only reload the data.
     */
    public static void connect(final Context context, final boolean force) {
        // Create an intent and pass on the connection details
        Intent intent = new Intent(context, HTSService.class);
        intent.setAction(Constants.ACTION_CONNECT);

        final DatabaseHelper databaseHelper = DatabaseHelper.getInstance(context.getApplicationContext());
        final Connection conn = databaseHelper.getSelectedConnection();
        // If we got one connection, get the values
        if (conn != null) {
            intent.putExtra("hostname", conn.address);
            intent.putExtra("port", conn.port);
            intent.putExtra("username", conn.username);
            intent.putExtra("password", conn.password);
            intent.putExtra("force", force);
        }
        // Start the service with given action and data
        context.startService(intent);
    }

    /**
     * Shows an icon for the state of the current recording. If no recording was
     * given, the icon will be hidden.
     *
     * @param activity Activity context
     * @param state    Widget that shall show the program state
     * @param p        Program
     */
    public static void setState(Activity activity, ImageView state, final Program p) {
        if (state == null) {
            return;
        }

        // If no recording was given hide the state icon
        if (p == null || p.dvrId == 0) {
            state.setImageDrawable(null);
            state.setVisibility(ImageView.GONE);
        } else {
            // Show the state icon and set the correct image
            state.setVisibility(ImageView.VISIBLE);

            Recording rec = DataStorage.getInstance().getRecordingFromArray(p.dvrId);

            if (rec == null || rec.isFailed()) {
                state.setImageResource(R.drawable.ic_error_small);
            } else if (rec.isCompleted()) {
                state.setImageResource(R.drawable.ic_success_small);
            } else if (rec.isMissed()) {
                state.setImageResource(R.drawable.ic_error_small);
            } else if (rec.isRecording()) {
                state.setImageResource(R.drawable.ic_rec_small);
            } else if (rec.isScheduled()) {
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
     * @param duration Duration
     * @param start    Start time
     * @param stop     Stop time
     */
    public static void setDuration(TextView duration, final long start, final long stop) {
        if (duration == null) {
            return;
        }
        duration.setVisibility(View.VISIBLE);
        // Get the start and end times so we can show them
        // and calculate the duration. Then show the duration in minutes
        final double durationTime = ((stop - start) / 1000 / 60);
        final String s = duration.getContext().getString(R.string.minutes, (int) durationTime);
        duration.setText(duration.getContext().getString(R.string.minutes, (int) durationTime));
        duration.setVisibility((s.length() > 0) ? View.VISIBLE : View.GONE);
    }

    /**
     * Shows the given time for the given view.
     * 
     * @param time     Time
     * @param start   Start time
     * @param stop     Stop time
     */
    public static void setTime(TextView time, final long start, final long stop) {
        if (time == null) {
            return;
        }

        time.setVisibility(View.VISIBLE);
        String startTime = ""; // DateFormat.getTimeFormat(time.getContext()).format(start);
        String endTime = ""; // DateFormat.getTimeFormat(time.getContext()).format(stop);

        Context context = time.getContext();
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (prefs.getBoolean("useLocalizedDateTimeFormatPref", false)) {
            // Show the date as defined with the currently active locale.
            // For the date display the short version will be used
            Locale locale;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                locale = context.getResources().getConfiguration().getLocales().get(0);
            } else {
                locale = context.getResources().getConfiguration().locale;
            }
            if (locale != null) {
                final java.text.DateFormat df = java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT, locale);
                startTime = df.format(start);
                endTime = df.format(stop);
            }
        } else {
            // Show the date using the default format like 31.07.2013
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.US);
            startTime = sdf.format(start);
            endTime = sdf.format(stop);
        }

        String value = startTime + " - " + endTime;
        time.setText(value);
    }

    /**
     * Shows the given date. The date for the first days will be shown as words.
     * After one week the date value will be used.
     *
     * @param date  Date
     * @param start Start time
     */
    public static void setDate(TextView date, final long start) {
        if (date == null) {
            return;
        }

        String dateText = "";
        if (DateUtils.isToday(start)) {
            // Show the string today
            dateText = date.getContext().getString(R.string.today);
        } else if (start < System.currentTimeMillis() + twoDays
                && start > System.currentTimeMillis() - twoDays) {
            // Show a string like "42 minutes ago"
            dateText = DateUtils.getRelativeTimeSpanString(start, System.currentTimeMillis(),
                    DateUtils.DAY_IN_MILLIS).toString();
        } else if (start < System.currentTimeMillis() + sixDays
                && start > System.currentTimeMillis() - twoDays) {
            // Show the day of the week, like Monday or Tuesday
            SimpleDateFormat sdf = new SimpleDateFormat("EEEE", Locale.US);
            dateText = sdf.format(start);
        }

        // Translate the day strings, if the string is empty
        // use the day month year date representation
        switch (dateText) {
            case "today":
                date.setText(R.string.today);
                break;
            case "tomorrow":
                date.setText(R.string.tomorrow);
                break;
            case "in 2 days":
                date.setText(R.string.in_2_days);
                break;
            case "Monday":
                date.setText(R.string.monday);
                break;
            case "Tuesday":
                date.setText(R.string.tuesday);
                break;
            case "Wednesday":
                date.setText(R.string.wednesday);
                break;
            case "Thursday":
                date.setText(R.string.thursday);
                break;
            case "Friday":
                date.setText(R.string.friday);
                break;
            case "Saturday":
                date.setText(R.string.saturday);
                break;
            case "Sunday":
                date.setText(R.string.sunday);
                break;
            case "yesterday":
                date.setText(R.string.yesterday);
                break;
            case "2 days ago":
                date.setText(R.string.two_days_ago);
                break;
            default:
                Context context = date.getContext();
                final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                if (prefs.getBoolean("useLocalizedDateTimeFormatPref", false)) {
                    // Show the date as defined with the currently active locale.
                    // For the date display the short version will be used
                    Locale locale;
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        locale = context.getResources().getConfiguration().getLocales().get(0);
                    } else {
                        locale = context.getResources().getConfiguration().locale;
                    }
                    if (locale != null) {
                        final java.text.DateFormat df = java.text.DateFormat.getDateInstance(java.text.DateFormat.SHORT, locale);
                        dateText = df.format(start);
                    }
                } else {
                    // Show the date using the default format like 31.07.2013
                    SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.US);
                    dateText = sdf.format(start);
                }

                date.setText(dateText);
                break;
        }
    }

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
     * @param context
     * @param seriesInfoLabel Widget that show the series information header
     * @param seriesInfo Widget that shall show the series information
     * @param p
     */
    public static void setSeriesInfo(Context context, TextView seriesInfoLabel, TextView seriesInfo, final Program p) {

        final String season = context.getResources().getString(R.string.season);
        final String episode = context.getResources().getString(R.string.episode);
        final String part = context.getResources().getString(R.string.part);

        String seasonInfo = "";
        if (!TextUtils.isEmpty(p.episodeOnscreen)) {
            seasonInfo = p.episodeOnscreen;
        } else {
            if (p.seasonNumber > 0) {
                seasonInfo += String.format(Locale.getDefault(), "%s %02d",
                        season.toLowerCase(Locale.getDefault()), p.seasonNumber);
            }
            if (p.episodeNumber > 0) {
                if (seasonInfo.length() > 0)
                    seasonInfo += ", ";
                seasonInfo += String.format(Locale.getDefault(), "%s %02d",
                        episode.toLowerCase(Locale.getDefault()), p.episodeNumber);
            }
            if (p.partNumber > 0) {
                if (seasonInfo.length() > 0)
                    seasonInfo += ", ";
                seasonInfo += String.format(Locale.getDefault(), "%s %d",
                        part.toLowerCase(Locale.getDefault()), p.partNumber);
            }
            if (seasonInfo.length() > 0) {
                seasonInfo = seasonInfo.substring(0, 1).toUpperCase(
                        Locale.getDefault()) + seasonInfo.substring(1);
            }
        }

        if (seriesInfo != null) {
            seriesInfo.setText(seasonInfo);
            seriesInfo.setVisibility(!TextUtils.isEmpty(seasonInfo) ? View.VISIBLE : View.GONE);
            if (seriesInfoLabel != null) {
                seriesInfoLabel.setVisibility(!TextUtils.isEmpty(seasonInfo) ? View.VISIBLE : View.GONE);
            }
        }
    }

    /**
     * Shows the given content type text for the given view. If the text is empty
     * then the view will be hidden.
     * 
     * @param contentTypeLabel Widget that show the content type header
     * @param contentType      Widget that shall show the content type information
     * @param ct               Content type
     */
    public static void setContentType(TextView contentTypeLabel, TextView contentType, final int ct) {
        if (contentType == null) {
            return;
        }
        final SparseArray<String> ctl = getContentTypes(contentType.getContext());
        final String type = ctl.get(ct, "");
        contentType.setText(type);
        contentType.setVisibility((type.length() > 0) ? View.VISIBLE : View.GONE);
        if (contentTypeLabel != null) {
            contentTypeLabel.setVisibility((type.length() > 0) ? View.VISIBLE : View.GONE);
        }
    }

    private static SparseArray<String> getContentTypes(Context ctx) {
        SparseArray<String> ret = new SparseArray<>();

        String[] s = ctx.getResources().getStringArray(R.array.pr_content_type0);
        for (int i = 0; i < s.length; i++) {
            ret.append(i, s[i]);
        }

        s = ctx.getResources().getStringArray(R.array.pr_content_type1);
        for (int i = 0; i < s.length; i++) {
            ret.append(0x10 + i, s[i]);
        }

        s = ctx.getResources().getStringArray(R.array.pr_content_type2);
        for (int i = 0; i < s.length; i++) {
            ret.append(0x20 + i, s[i]);
        }

        s = ctx.getResources().getStringArray(R.array.pr_content_type3);
        for (int i = 0; i < s.length; i++) {
            ret.append(0x30 + i, s[i]);
        }

        s = ctx.getResources().getStringArray(R.array.pr_content_type4);
        for (int i = 0; i < s.length; i++) {
            ret.append(0x40 + i, s[i]);
        }

        s = ctx.getResources().getStringArray(R.array.pr_content_type5);
        for (int i = 0; i < s.length; i++) {
            ret.append(0x50 + i, s[i]);
        }

        s = ctx.getResources().getStringArray(R.array.pr_content_type6);
        for (int i = 0; i < s.length; i++) {
            ret.append(0x60 + i, s[i]);
        }

        s = ctx.getResources().getStringArray(R.array.pr_content_type7);
        for (int i = 0; i < s.length; i++) {
            ret.append(0x70 + i, s[i]);
        }

        s = ctx.getResources().getStringArray(R.array.pr_content_type8);
        for (int i = 0; i < s.length; i++) {
            ret.append(0x80 + i, s[i]);
        }

        s = ctx.getResources().getStringArray(R.array.pr_content_type9);
        for (int i = 0; i < s.length; i++) {
            ret.append(0x90 + i, s[i]);
        }

        s = ctx.getResources().getStringArray(R.array.pr_content_type10);
        for (int i = 0; i < s.length; i++) {
            ret.append(0xa0 + i, s[i]);
        }

        s = ctx.getResources().getStringArray(R.array.pr_content_type11);
        for (int i = 0; i < s.length; i++) {
            ret.append(0xb0 + i, s[i]);
        }

        return ret;
    }

    /**
     * Shows the channel icon and optionally the channel name. The icon will
     * only be shown when the user has activated the setting and an icon is
     * actually available. If no icon is available the channel name will be
     * shown as a placeholder.
     *
     * @param icon     Widget that shall show the channel icon
     * @param iconText Widget that shows the channel name if no icon is available
     * @param ch       Channel
     */
    public static void setChannelIcon(Context context, ImageView icon, TextView iconText, final Channel ch) {
        if (icon != null) {
            // Get the setting if the channel icon shall be shown or not
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(icon.getContext());
            final boolean showChannelIcons = prefs.getBoolean("showIconPref", true);
            if (ch != null) {
                // Show the channels icon if available. If not hide the view.
                Bitmap iconBitmap = MiscUtils.getCachedIcon(context, ch.channelIcon);
                icon.setImageBitmap(iconBitmap);
                icon.setVisibility((showChannelIcons && iconBitmap != null) ? ImageView.VISIBLE : ImageView.GONE);

                // If the channel icon is not available show the channel name as a placeholder.
                if (iconText != null) {
                    iconText.setText(ch.channelName);
                    iconText.setVisibility((showChannelIcons && iconBitmap == null) ? ImageView.VISIBLE : ImageView.GONE);
                }
            } else {
                // Show a blank icon if no channel icon exists and they shall be shown. 
                icon.setImageBitmap(null);
                icon.setVisibility(showChannelIcons ? ImageView.VISIBLE : ImageView.GONE);
            }
        }
    }

    /**
     * Shows the given description text for the given view. If the text is empty
     * then the view will be hidden.
     * 
     * @param descriptionLabel  Widget that show the description header
     * @param description       Widget that shall show the description
     * @param desc              Description text
     */
    public static void setDescription(TextView descriptionLabel, TextView description, final String desc) {
        if (description == null) {
            return;
        }
        description.setText(desc);
        description.setVisibility((desc != null && desc.length() > 0) ? View.VISIBLE : View.GONE);
        if (descriptionLabel != null) {
            descriptionLabel.setVisibility((desc != null && desc.length() > 0) ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * Shows the reason why a recording has failed. If the text is empty then
     * the view will be hidden.
     *
     * @param failed_reason Widget that shows the failed reason text
     * @param rec           Recording
     */
    public static void setFailedReason(final TextView failed_reason, final Recording rec) {
        if (failed_reason == null) {
            return;
        }

        // Make the text field visible as a default
        failed_reason.setVisibility(View.VISIBLE);

        // Show the reason why it failed
        if (rec.isRemoved()) {
            // failed_reason.setText(failed_reason.getResources().getString(R.string.recording_file_missing));
            failed_reason.setVisibility(View.GONE);
        } else if (rec.isAborted()) {
            failed_reason.setText(failed_reason.getResources().getString(R.string.recording_canceled));
        } else if (rec.isMissed()) {
            failed_reason.setText(failed_reason.getResources().getString(R.string.recording_time_missed));
        } else if (rec.isFailed()) {
            failed_reason.setText(failed_reason.getResources().getString(R.string.recording_file_invalid));
        } else {
            failed_reason.setVisibility(View.GONE);
        }
    }

    public static void setProgress(ProgressBar progress, final long start, final long stop) {
        if (progress == null) {
            return;
        }
        // Get the start and end times to calculate the progress.
        double durationTime = (stop - start);
        double elapsedTime = new Date().getTime() - start;

        // Show the progress as a percentage
        double percent = 0;
        if (durationTime > 0) {
            percent = elapsedTime / durationTime;
        }
        progress.setProgress((int) Math.floor(percent * 100));
        progress.setVisibility(View.VISIBLE);
    }

    public static void setProgressText(TextView progressText, final long start, final long stop) {
        if (progressText == null) {
            return;
        }

        // Get the start and end times to calculate the progress.
        final double durationTime = (stop - start);
        final double elapsedTime = new Date().getTime() - start;

        // Show the progress as a percentage
        double percent = 0;
        if (durationTime > 0) {
            percent = elapsedTime / durationTime;
        }
        int progress = (int) Math.floor(percent * 100);
        if (progress > 100) {
            progress = 100;
        }
        if (progress > 0) {
            progressText.setText(progressText.getResources().getString(R.string.progress, progress));
            progressText.setVisibility(View.VISIBLE);
        } else {
            progressText.setVisibility(View.GONE);
        }
    }

    /**
     * Gets the last program id from the given channel. The id is used to tell
     * the server where to continue loading programs.
     * 
     * @param context Activity context
     * @param channel Channel
     */
    public static void loadMorePrograms(final Context context, final Channel channel) {
        Log.d(TAG, "loadMorePrograms() called with: context = [" + context + "], channel = [" + channel + "]");

        if (channel == null) {
            return;
        }

        int nextId = channel.eventId;
        Log.d(TAG, "loadMorePrograms: nextid " + nextId);

        while (nextId != 0) {
            Log.d(TAG, "loadMorePrograms: nextid " + nextId);
            Program p = DataStorage.getInstance().getProgramFromArray(nextId);
            if (p != null && p.nextEventId > 0) {
                nextId = p.nextEventId;
            } else {
                break;
            }
        }

        Log.d(TAG, "loadMorePrograms: loading from eventId " + nextId);
        // Set the required information and start the service command.
        Intent intent = new Intent(context, HTSService.class);
        intent.setAction("getEvents");
        intent.putExtra("eventId", nextId);
        intent.putExtra("channelId", channel.channelId);
        intent.putExtra("count", 15);
        context.startService(intent);
    }

    /**
     * Returns the id of the channel tag that is saved in the current connection
     * 
     * @return Id of the channel tag
     */
    private static int getChannelTagId(final Context context) {
        // Get the selected tag for the active connection in the database. If
        // none exist then use the variable here.
        final DatabaseHelper databaseHelper = DatabaseHelper.getInstance(context.getApplicationContext());
    	if (databaseHelper != null) {
	        Connection conn = databaseHelper.getSelectedConnection();
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
     * @param channelTagId Id of the channel tag
     */
    public static void setChannelTagId(final Context context, final int channelTagId) {
        // Save the selected tag for the active connection in the database
        final DatabaseHelper databaseHelper = DatabaseHelper.getInstance(context.getApplicationContext());
    	if (databaseHelper != null) {
	        Connection conn = databaseHelper.getSelectedConnection();
	        if (conn != null) {
	            conn.channelTag = channelTagId;
	            databaseHelper.updateConnection(conn);
	        }
	    }
    }

    /**
     * Returns the saved channel tag for the active connection.
     * 
     * @param activity Activity context
     * @return Channel tag
     */
    public static ChannelTag getChannelTag(final Activity activity) {
        return DataStorage.getInstance().getTagFromArray(getChannelTagId(activity));
    }

	/**
	 * Change the language to the defined setting. If the default is set then
	 * let the application decide which language shall be used.
	 *
	 * @param context Activity context
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
     * @param context Activity context
     * @return Number that defines the channel sort order
     */
	public static int getChannelSortOrder(final Activity context) {
	    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return Integer.parseInt(prefs.getString("sortChannelsPref", String.valueOf(Constants.CHANNEL_SORT_DEFAULT)));
	}

    /**
     * Converts the given time in milliseconds to a human readable time value.
     * Adds leading zeros to the hour or minute values in case they are lower
     * then ten.
     * 
     * @return time in hh:mm format
     */
    public static String getTimeStringFromValue(final Activity activity, final long time) {
        if (time < 0) {
            return (activity.getString(R.string.not_set));
        }
        String minutes = String.valueOf(time % 60);
        if (minutes.length() == 1) {
            minutes = "0" + minutes;
        }
        String hours = String.valueOf(time / 60);
        if (hours.length() == 1) {
            hours = "0" + hours;
        }
        return (hours + ":" + minutes);
    }
}
