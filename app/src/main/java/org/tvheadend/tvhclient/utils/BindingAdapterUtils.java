package org.tvheadend.tvhclient.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.entity.Recording;

import java.text.SimpleDateFormat;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.databinding.BindingAdapter;
import timber.log.Timber;

public class BindingAdapterUtils {

    // Constants required for the date calculation
    private static final int TWO_DAYS = 1000 * 3600 * 24 * 2;
    private static final int SIX_DAYS = 1000 * 3600 * 24 * 6;

    @BindingAdapter("dataSizeText")
    public static void setDataSizeText(TextView view, @NonNull Recording recording) {
        Context context = view.getContext();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean showRecordingFileStatus = sharedPreferences.getBoolean("show_recording_file_status_enabled", false);

        if (showRecordingFileStatus && (!recording.isScheduled() || recording.isScheduled() && recording.isRecording())) {
            view.setVisibility(View.VISIBLE);
            if (recording.getDataSize() > 1048576) {
                view.setText(context.getResources().getString(R.string.data_size, recording.getDataSize() / 1048576, "MB"));
            } else {
                view.setText(context.getResources().getString(R.string.data_size, recording.getDataSize() / 1024, "KB"));
            }
        } else {
            view.setVisibility(View.GONE);
        }
    }

    @BindingAdapter("dataErrorText")
    public static void setDataErrorText(TextView view, @NonNull Recording recording) {
        Context context = view.getContext();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean showRecordingFileStatus = sharedPreferences.getBoolean("show_recording_file_status_enabled", false);

        if (showRecordingFileStatus && (!recording.isScheduled() || recording.isScheduled() && recording.isRecording())) {
            view.setVisibility(View.VISIBLE);
            view.setText(context.getResources().getString(R.string.data_errors, recording.getDataErrors() == null ? "0" : recording.getDataErrors()));
        } else {
            view.setVisibility(View.GONE);
        }
    }

    @BindingAdapter({"disabledText", "htspVersion"})
    public static void setDisabledText(TextView view, @NonNull Recording recording, int htspVersion) {
        if (!recording.isScheduled()) {
            view.setVisibility(View.GONE);
        } else {
            setDisabledText(view, recording.isEnabled(), htspVersion);
        }
    }

    @BindingAdapter({"disabledText", "htspVersion"})
    public static void setDisabledText(TextView view, boolean isEnabled, int htspVersion) {
        view.setVisibility(htspVersion >= 19 && !isEnabled ? View.VISIBLE : View.GONE);
        view.setText(isEnabled ? R.string.recording_enabled : R.string.recording_disabled);
    }

    @BindingAdapter({"duplicateText", "htspVersion"})
    public static void setDuplicateText(TextView view, @NonNull Recording recording, int htspVersion) {
        if (!recording.isScheduled()) {
            view.setVisibility(View.GONE);
        } else {
            view.setVisibility(htspVersion < 33 || recording.getDuplicate() == 0 ? View.GONE : View.VISIBLE);
            view.setText(R.string.duplicate_recording);
        }
    }

    @BindingAdapter("failedReasonText")
    public static void setFailedReasonText(TextView view, @NonNull Recording recording) {
        Context context = view.getContext();
        String failedReasonText;

        if (recording.isAborted()) {
            failedReasonText = context.getResources().getString(R.string.recording_canceled);
        } else if (recording.isMissed()) {
            failedReasonText = context.getResources().getString(R.string.recording_time_missed);
        } else if (recording.isFailed()) {
            failedReasonText = context.getResources().getString(R.string.recording_file_invalid);
        } else if (recording.isFileMissing()) {
            failedReasonText = context.getResources().getString(R.string.recording_file_missing);
        } else {
            failedReasonText = "";
        }

        view.setVisibility((!TextUtils.isEmpty(failedReasonText) && !recording.isCompleted()) ? View.VISIBLE : View.GONE);
        view.setText(failedReasonText);
    }

    @BindingAdapter("optionalText")
    public static void setOptionalText(TextView view, String text) {
        view.setVisibility(!TextUtils.isEmpty(text) ? View.VISIBLE : View.GONE);
        view.setText(text);
    }

    /**
     * Loads the given channel icon via Picasso into the image view
     *
     * @param view    The view where the icon and visibility shall be applied to
     * @param iconUrl The url of the channel icon
     */
    @BindingAdapter("iconUrl")
    public static void setChannelIcon(ImageView view, String iconUrl) {
        if (TextUtils.isEmpty(iconUrl)) {
            view.setVisibility(View.GONE);
        } else {
            String url = UIUtils.getIconUrl(view.getContext(), iconUrl);
            Picasso.get()
                    .load(url)
                    .into(view, new Callback() {
                        @Override
                        public void onSuccess() {
                            view.setVisibility(View.VISIBLE);
                        }

                        @Override
                        public void onError(Exception e) {
                            Timber.d("Could not load channel icon " + url);
                            view.setVisibility(View.GONE);
                        }
                    });
        }
    }

    /**
     * Shows the channel name in the view if no channel icon exists.
     *
     * @param view    The view where the text and visibility shall be applied to
     * @param name    The name of the channel
     * @param iconUrl The url to the channel icon
     */
    @BindingAdapter({"channelName", "channelIcon"})
    public static void setChannelName(TextView view, String name, String iconUrl) {
        view.setVisibility(TextUtils.isEmpty(iconUrl) ? View.VISIBLE : View.GONE);
        view.setText((!TextUtils.isEmpty(name) ? name : view.getContext().getString(R.string.all_channels)));
    }

    /**
     * Set the correct indication when the dual pane mode is active If the item is selected
     * the the arrow will be shown, otherwise only a vertical separation line is displayed.
     *
     * @param view       The view where the theme and background image shall be applied to
     * @param isSelected Determines if the background image shall show a selected state or not
     */
    @BindingAdapter("backgroundImage")
    public static void setDualPaneBackground(ImageView view, boolean isSelected) {
        if (isSelected) {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(view.getContext());
            boolean lightTheme = sharedPreferences.getBoolean("light_theme_enabled", true);
            final int icon = (lightTheme) ? R.drawable.dual_pane_selector_active_light : R.drawable.dual_pane_selector_active_dark;
            view.setBackgroundResource(icon);
        } else {
            final int icon = R.drawable.dual_pane_selector_inactive;
            view.setBackgroundResource(icon);
        }
    }

    /**
     * Converts the given number representing the days into a string with the
     * short names for the days. This string is assigned to the given view.
     *
     * @param view       The view where the short names for the days shall be shown
     * @param daysOfWeek The number representing the days of the week
     */
    @BindingAdapter("daysText")
    public static void getDaysOfWeekText(TextView view, long daysOfWeek) {
        String[] daysOfWeekList = view.getContext().getResources().getStringArray(R.array.day_short_names);
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < 7; i++) {
            String s = (((daysOfWeek >> i) & 1) == 1) ? daysOfWeekList[i] : "";
            if (text.length() > 0 && s.length() > 0) {
                text.append(", ");
            }
            text.append(s);
        }
        view.setText(text.toString());
    }

    /**
     * Converts the given time in milliseconds into a default readable time
     * format, or if set by the preferences, into a localized time format
     *
     * @param view The view where the readable time shall be shown
     * @param time The time in milliseconds
     */
    @BindingAdapter("timeText")
    public static void setLocalizedTime(TextView view, long time) {
        String localizedTime = "";

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(view.getContext());
        if (prefs.getBoolean("localized_date_time_format_enabled", false)) {
            // Show the date as defined with the currently active locale.
            // For the date display the short version will be used
            Locale locale;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                locale = view.getContext().getResources().getConfiguration().getLocales().get(0);
            } else {
                locale = view.getContext().getResources().getConfiguration().locale;
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
        view.setText(localizedTime);
    }

    @BindingAdapter("dateText")
    public static void setLocalizedDate(TextView view, long date) {
        String localizedDate = "";
        Context context = view.getContext();

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
        view.setText(localizedDate);
    }
}
