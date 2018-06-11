package org.tvheadend.tvhclient.features.shared;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.SparseArray;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.entity.Program;
import org.tvheadend.tvhclient.data.entity.Recording;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javax.annotation.Nullable;

import static org.tvheadend.tvhclient.utils.MiscUtils.convertUrlToHashString;

public class UIUtils {

    // Constants required for the date calculation
    private static final int TWO_DAYS = 1000 * 3600 * 24 * 2;
    private static final int SIX_DAYS = 1000 * 3600 * 24 * 6;

    private UIUtils() {
        throw new IllegalAccessError("Utility class");
    }

    public static int getGenreColor(final Context context, final int contentType, final int offset) {
        if (contentType < 0) {
            // Return a fully transparent color in case no genre is available
            return context.getResources().getColor(android.R.color.transparent);
        } else {
            // Get the genre color from the content type
            int color = R.color.EPG_OTHER;
            int type = (contentType / 16);
            switch (type) {
                case 0:
                    color = R.color.EPG_MOVIES;
                    break;
                case 1:
                    color = R.color.EPG_NEWS;
                    break;
                case 2:
                    color = R.color.EPG_SHOWS;
                    break;
                case 3:
                    color = R.color.EPG_SPORTS;
                    break;
                case 4:
                    color = R.color.EPG_CHILD;
                    break;
                case 5:
                    color = R.color.EPG_MUSIC;
                    break;
                case 6:
                    color = R.color.EPG_ARTS;
                    break;
                case 7:
                    color = R.color.EPG_SOCIAL;
                    break;
                case 8:
                    color = R.color.EPG_SCIENCE;
                    break;
                case 9:
                    color = R.color.EPG_HOBBY;
                    break;
                case 10:
                    color = R.color.EPG_SPECIAL;
                    break;
            }
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

            // Get the color with the desired alpha value
            int c = context.getResources().getColor(color);
            int alpha = (int) (((float) prefs.getInt("genre_color_transparency", 70)) / 100.0f * 255.0f);
            if (alpha >= offset) {
                alpha -= offset;
            }
            return Color.argb(alpha, Color.red(c), Color.green(c), Color.blue(c));
        }
    }

    public static Drawable getRecordingState(Context context, @Nullable final Recording recording) {
        if (recording == null) {
            return null;
        } else if (recording.isFailed()) {
            return context.getResources().getDrawable(R.drawable.ic_error_small);
        } else if (recording.isCompleted()) {
            return context.getResources().getDrawable(R.drawable.ic_success_small);
        } else if (recording.isMissed()) {
            return context.getResources().getDrawable(R.drawable.ic_error_small);
        } else if (recording.isRecording()) {
            return context.getResources().getDrawable(R.drawable.ic_rec_small);
        } else if (recording.isScheduled()) {
            return context.getResources().getDrawable(R.drawable.ic_schedule_small);
        } else {
            return null;
        }
    }

    public static String getRecordingFailedReasonText(Context context, @Nullable final Recording recording) {
        if (recording == null) {
            return null;
        } else if (recording.isAborted()) {
            return context.getResources().getString(R.string.recording_canceled);
        } else if (recording.isMissed()) {
            return context.getResources().getString(R.string.recording_time_missed);
        } else if (recording.isFailed()) {
            return context.getResources().getString(R.string.recording_file_invalid);
        } else {
            return null;
        }
    }

    public static String getDate(Context context, long date) {
        String localizedDate = "";

        if (DateUtils.isToday(date)) {
            // Show the string today
            localizedDate = context.getString(R.string.today);

        } else if (date < System.currentTimeMillis() + TWO_DAYS
                && date > System.currentTimeMillis() - TWO_DAYS) {
            // Show a string like "42 minutes ago"
            localizedDate = DateUtils.getRelativeTimeSpanString(
                    date, System.currentTimeMillis(),
                    DateUtils.DAY_IN_MILLIS).toString();

        } else if (date < System.currentTimeMillis() + SIX_DAYS
                && date > System.currentTimeMillis() - TWO_DAYS) {
            // Show the day of the week, like Monday or Tuesday
            SimpleDateFormat sdf = new SimpleDateFormat("EEEE", Locale.US);
            localizedDate = sdf.format(date);
        }

        // Translate the day strings, if the string is empty
        // use the day month year date representation
        switch (localizedDate) {
            case "today":
                localizedDate = context.getString(R.string.today);
                break;
            case "tomorrow":
                localizedDate = context.getString(R.string.tomorrow);
                break;
            case "in 2 days":
                localizedDate = context.getString(R.string.in_2_days);
                break;
            case "Monday":
                localizedDate = context.getString(R.string.monday);
                break;
            case "Tuesday":
                localizedDate = context.getString(R.string.tuesday);
                break;
            case "Wednesday":
                localizedDate = context.getString(R.string.wednesday);
                break;
            case "Thursday":
                localizedDate = context.getString(R.string.thursday);
                break;
            case "Friday":
                localizedDate = context.getString(R.string.friday);
                break;
            case "Saturday":
                localizedDate = context.getString(R.string.saturday);
                break;
            case "Sunday":
                localizedDate = context.getString(R.string.sunday);
                break;
            case "yesterday":
                localizedDate = context.getString(R.string.yesterday);
                break;
            case "2 days ago":
                localizedDate = context.getString(R.string.two_days_ago);
                break;
            default:
                final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                if (prefs.getBoolean("localized_date_time_format_enabled", false)) {
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
                        localizedDate = df.format(date);
                    }
                } else {
                    // Show the date using the default format like 31.07.2013
                    SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.US);
                    localizedDate = sdf.format(date);
                }
                break;
        }
        return localizedDate;
    }

    public static String getTimeText(Context context, long time) {
        String localizedTime = "";

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (prefs.getBoolean("localized_date_time_format_enabled", false)) {
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
                localizedTime = df.format(time);
            }
        } else {
            // Show the date using the default format like 31.07.2013
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.US);
            localizedTime = sdf.format(time);
        }
        return localizedTime;
    }

    public static String getProgressText(Context context, long start, long stop) {
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
            return context.getResources().getString(R.string.progress, progress);
        } else {
            return "";
        }
    }

    public static String getSeriesInfo(Context context, @NonNull final Program program) {

        final String season = context.getResources().getString(R.string.season);
        final String episode = context.getResources().getString(R.string.episode);
        final String part = context.getResources().getString(R.string.part);

        String seriesInfo = "";
        if (!TextUtils.isEmpty(program.getEpisodeOnscreen())) {
            seriesInfo = program.getEpisodeOnscreen();
        } else {
            if (program.getSeasonNumber() > 0) {
                seriesInfo += String.format(Locale.getDefault(), "%s %02d",
                        season.toLowerCase(Locale.getDefault()), program.getSeasonNumber());
            }
            if (program.getEpisodeNumber() > 0) {
                if (seriesInfo.length() > 0)
                    seriesInfo += ", ";
                seriesInfo += String.format(Locale.getDefault(), "%s %02d",
                        episode.toLowerCase(Locale.getDefault()), program.getEpisodeNumber());
            }
            if (program.getPartNumber() > 0) {
                if (seriesInfo.length() > 0)
                    seriesInfo += ", ";
                seriesInfo += String.format(Locale.getDefault(), "%s %d",
                        part.toLowerCase(Locale.getDefault()), program.getPartNumber());
            }
            if (seriesInfo.length() > 0) {
                seriesInfo = seriesInfo.substring(0, 1).toUpperCase(
                        Locale.getDefault()) + seriesInfo.substring(1);
            }
        }
        return seriesInfo;
    }

    public static String getContentTypeText(Context context, int contentType) {
        SparseArray<String> ret = new SparseArray<>();

        String[] s = context.getResources().getStringArray(R.array.pr_content_type0);
        for (int i = 0; i < s.length; i++) {
            ret.append(i, s[i]);
        }
        s = context.getResources().getStringArray(R.array.pr_content_type1);
        for (int i = 0; i < s.length; i++) {
            ret.append(0x10 + i, s[i]);
        }
        s = context.getResources().getStringArray(R.array.pr_content_type2);
        for (int i = 0; i < s.length; i++) {
            ret.append(0x20 + i, s[i]);
        }
        s = context.getResources().getStringArray(R.array.pr_content_type3);
        for (int i = 0; i < s.length; i++) {
            ret.append(0x30 + i, s[i]);
        }
        s = context.getResources().getStringArray(R.array.pr_content_type4);
        for (int i = 0; i < s.length; i++) {
            ret.append(0x40 + i, s[i]);
        }
        s = context.getResources().getStringArray(R.array.pr_content_type5);
        for (int i = 0; i < s.length; i++) {
            ret.append(0x50 + i, s[i]);
        }
        s = context.getResources().getStringArray(R.array.pr_content_type6);
        for (int i = 0; i < s.length; i++) {
            ret.append(0x60 + i, s[i]);
        }
        s = context.getResources().getStringArray(R.array.pr_content_type7);
        for (int i = 0; i < s.length; i++) {
            ret.append(0x70 + i, s[i]);
        }
        s = context.getResources().getStringArray(R.array.pr_content_type8);
        for (int i = 0; i < s.length; i++) {
            ret.append(0x80 + i, s[i]);
        }
        s = context.getResources().getStringArray(R.array.pr_content_type9);
        for (int i = 0; i < s.length; i++) {
            ret.append(0x90 + i, s[i]);
        }
        s = context.getResources().getStringArray(R.array.pr_content_type10);
        for (int i = 0; i < s.length; i++) {
            ret.append(0xa0 + i, s[i]);
        }
        s = context.getResources().getStringArray(R.array.pr_content_type11);
        for (int i = 0; i < s.length; i++) {
            ret.append(0xb0 + i, s[i]);
        }
        return ret.get(contentType, context.getString(R.string.no_data));
    }

    public static String getIconUrl(Context context, @Nullable final String url) {
        if (TextUtils.isEmpty(url)) {
            return null;
        }
        return "file://" + context.getCacheDir() + "/" + convertUrlToHashString(url) + ".png";
    }

    public static String getDaysOfWeekText(Context context, long daysOfWeek) {
        String[] daysOfWeekList = context.getResources().getStringArray(R.array.day_short_names);
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < 7; i++) {
            String s = (((daysOfWeek >> i) & 1) == 1) ? daysOfWeekList[i] : "";
            if (text.length() > 0 && s.length() > 0) {
                text.append(", ");
            }
            text.append(s);
        }
        return text.toString();
    }
}
