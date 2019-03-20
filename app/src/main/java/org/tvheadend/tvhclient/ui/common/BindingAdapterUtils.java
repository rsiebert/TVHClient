package org.tvheadend.tvhclient.ui.common;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.databinding.BindingAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.domain.entity.Program;
import org.tvheadend.tvhclient.domain.entity.Recording;
import org.tvheadend.tvhclient.util.MiscUtils;

import java.text.SimpleDateFormat;
import java.util.Locale;

import timber.log.Timber;

@SuppressWarnings("unused")
public class BindingAdapterUtils {

    // Constants required for the date calculation
    private static final int TWO_DAYS = 1000 * 3600 * 24 * 2;
    private static final int SIX_DAYS = 1000 * 3600 * 24 * 6;

    @BindingAdapter("marginStart")
    public static void setLayoutWidth(View view, boolean increaseMargin) {
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        if (layoutParams instanceof ViewGroup.MarginLayoutParams) {
            int marginStart = (int) (increaseMargin ?
                    view.getContext().getResources().getDimension(R.dimen.dp_80) :
                    view.getContext().getResources().getDimension(R.dimen.dp_16));

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                ((ViewGroup.MarginLayoutParams) layoutParams).setMarginStart(marginStart);
                view.setLayoutParams(layoutParams);
            } else {
                ((ViewGroup.MarginLayoutParams) layoutParams).setMargins(marginStart,
                        ((ViewGroup.MarginLayoutParams) layoutParams).topMargin,
                        ((ViewGroup.MarginLayoutParams) layoutParams).rightMargin,
                        ((ViewGroup.MarginLayoutParams) layoutParams).bottomMargin);
            }
        }
    }

    @BindingAdapter("layoutWidth")
    public static void setLayoutWidth(View view, int width) {
        RecyclerView.LayoutParams layoutParams = (RecyclerView.LayoutParams) view.getLayoutParams();
        layoutParams.width = width;
        view.setLayoutParams(layoutParams);
    }

    @BindingAdapter("seriesInfoText")
    public static void setSeriesInfoText(TextView view, final Program program) {
        Context context = view.getContext();
        final String season = context.getResources().getString(R.string.season);
        final String episode = context.getResources().getString(R.string.episode);
        final String part = context.getResources().getString(R.string.part);

        String seriesInfo = "";
        if (program != null) {
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
        }
        view.setVisibility(TextUtils.isEmpty(seriesInfo) ? View.GONE : View.VISIBLE);
        view.setText(seriesInfo);
    }

    @BindingAdapter("contentTypeText")
    public static void setContentTypeText(TextView view, int contentType) {
        SparseArray<String> ret = new SparseArray<>();
        Context context = view.getContext();

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
        String contentTypeText = ret.get(contentType, context.getString(R.string.no_data));
        view.setVisibility(contentTypeText.isEmpty() ? View.GONE : View.VISIBLE);
        view.setText(contentTypeText);
    }

    @BindingAdapter("priorityText")
    public static void setPriorityText(TextView view, int priority) {
        String[] priorityNames = view.getContext().getResources().getStringArray(R.array.dvr_priority_names);
        if (priority >= 0 && priority <= 4) {
            view.setText(priorityNames[priority]);
        } else if (priority == 6) {
            view.setText(priorityNames[5]);
        } else {
            view.setText("");
        }
    }

    @BindingAdapter("dataSizeText")
    public static void setDataSizeText(TextView view, Recording recording) {
        Context context = view.getContext();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean showRecordingFileStatus = sharedPreferences.getBoolean("show_recording_file_status_enabled", view.getContext().getResources().getBoolean(R.bool.pref_default_show_recording_file_status_enabled));

        if (showRecordingFileStatus
                && recording != null
                && (!recording.isScheduled() || recording.isScheduled() && recording.isRecording())) {
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
    public static void setDataErrorText(TextView view, Recording recording) {
        Context context = view.getContext();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean showRecordingFileStatus = sharedPreferences.getBoolean("show_recording_file_status_enabled", view.getContext().getResources().getBoolean(R.bool.pref_default_show_recording_file_status_enabled));

        if (showRecordingFileStatus
                && recording != null
                && !TextUtils.isEmpty(recording.getDataErrors())
                && (!recording.isScheduled() || recording.isScheduled() && recording.isRecording())) {
            view.setVisibility(View.VISIBLE);
            view.setText(context.getResources().getString(R.string.data_errors, recording.getDataErrors() == null ? "0" : recording.getDataErrors()));
        } else {
            view.setVisibility(View.GONE);
        }
    }

    @BindingAdapter("subscriptionErrorText")
    public static void setSubscriptionErrorText(TextView view, Recording recording) {
        Context context = view.getContext();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean showRecordingFileStatus = sharedPreferences.getBoolean("show_recording_file_status_enabled", view.getContext().getResources().getBoolean(R.bool.pref_default_show_recording_file_status_enabled));

        if (showRecordingFileStatus
                && recording != null
                && !recording.isScheduled()
                && !TextUtils.isEmpty(recording.getSubscriptionError())) {
            view.setVisibility(View.VISIBLE);
            view.setText(context.getResources().getString(R.string.subscription_error, recording.getSubscriptionError()));
        } else {
            view.setVisibility(View.GONE);
        }
    }

    @BindingAdapter("streamErrorText")
    public static void setStreamErrorText(TextView view, Recording recording) {
        Context context = view.getContext();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean showRecordingFileStatus = sharedPreferences.getBoolean("show_recording_file_status_enabled", view.getContext().getResources().getBoolean(R.bool.pref_default_show_recording_file_status_enabled));

        if (showRecordingFileStatus
                && recording != null
                && !recording.isScheduled()
                && !TextUtils.isEmpty(recording.getStreamErrors())) {
            view.setVisibility(View.VISIBLE);
            view.setText(context.getResources().getString(R.string.stream_errors, recording.getStreamErrors()));
        } else {
            view.setVisibility(View.GONE);
        }
    }

    @BindingAdapter("statusLabelVisibility")
    public static void setStatusLabelVisibility(TextView view, Recording recording) {
        Context context = view.getContext();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean showRecordingFileStatus = sharedPreferences.getBoolean("show_recording_file_status_enabled", context.getResources().getBoolean(R.bool.pref_default_show_recording_file_status_enabled));

        if (showRecordingFileStatus
                && recording != null
                && !recording.isScheduled()) {
            view.setVisibility(View.VISIBLE);
        } else {
            view.setVisibility(View.GONE);
        }
    }

    @BindingAdapter({"disabledText", "htspVersion"})
    public static void setDisabledText(TextView view, Recording recording, int htspVersion) {
        if (recording == null || !recording.isScheduled()) {
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
    public static void setDuplicateText(TextView view, Recording recording, int htspVersion) {
        if (recording == null || !recording.isScheduled()) {
            view.setVisibility(View.GONE);
        } else {
            view.setVisibility(htspVersion < 33 || recording.getDuplicate() == 0 ? View.GONE : View.VISIBLE);
            view.setText(R.string.duplicate_recording);
        }
    }

    @BindingAdapter("failedReasonText")
    public static void setFailedReasonText(TextView view, Recording recording) {
        Context context = view.getContext();
        String failedReasonText = "";

        if (recording != null) {
            if (recording.isAborted()) {
                failedReasonText = context.getResources().getString(R.string.recording_canceled);
            } else if (recording.isMissed()) {
                failedReasonText = context.getResources().getString(R.string.recording_time_missed);
            } else if (recording.isFailed()) {
                failedReasonText = context.getResources().getString(R.string.recording_file_invalid);
            } else if (recording.isFileMissing()) {
                failedReasonText = context.getResources().getString(R.string.recording_file_missing);
            }
        }

        view.setVisibility(
                (!TextUtils.isEmpty(failedReasonText)
                        && recording != null
                        && !recording.isCompleted()) ? View.VISIBLE : View.GONE);
        view.setText(failedReasonText);
    }

    @BindingAdapter("optionalText")
    public static void setOptionalText(TextView view, String text) {
        view.setVisibility(!TextUtils.isEmpty(text) ? View.VISIBLE : View.GONE);
        view.setText(text);
    }

    @BindingAdapter("stateIcon")
    public static void setStateIcon(ImageView view, Recording recording) {
        Drawable drawable = null;
        if (recording != null) {
            if (recording.isFailed()) {
                drawable = view.getContext().getResources().getDrawable(R.drawable.ic_error_small);
            } else if (recording.isCompleted()) {
                drawable = view.getContext().getResources().getDrawable(R.drawable.ic_success_small);
            } else if (recording.isMissed()) {
                drawable = view.getContext().getResources().getDrawable(R.drawable.ic_error_small);
            } else if (recording.isRecording()) {
                drawable = view.getContext().getResources().getDrawable(R.drawable.ic_rec_small);
            } else if (recording.isScheduled()) {
                drawable = view.getContext().getResources().getDrawable(R.drawable.ic_schedule_small);
            }
        }

        view.setVisibility(drawable != null ? View.VISIBLE : View.GONE);
        view.setImageDrawable(drawable);
    }

    @BindingAdapter({"iconUrl", "iconVisibility"})
    public static void setChannelIcon(ImageView view, String iconUrl, boolean visible) {
        if (visible) {
            setChannelIcon(view, iconUrl);
        } else {
            view.setVisibility(View.GONE);
        }
    }

    /**
     * Loads the given program image via Picasso into the image view
     *
     * @param view The view where the icon and visibility shall be applied to
     * @param url  The url of the channel icon
     */
    @BindingAdapter("programImage")
    public static void setProgramImage(ImageView view, String url) {
        if (TextUtils.isEmpty(url)) {
            view.setVisibility(View.GONE);
        } else {

            Transformation transformation = new Transformation() {

                @Override
                public Bitmap transform(Bitmap source) {
                    int targetWidth = view.getWidth();
                    double aspectRatio = (double) source.getHeight() / (double) source.getWidth();
                    int targetHeight = (int) (targetWidth * aspectRatio);
                    Bitmap result = Bitmap.createScaledBitmap(source, targetWidth, targetHeight, false);
                    if (result != source) {
                        // Same bitmap is returned if sizes are the same
                        source.recycle();
                    }
                    return result;
                }

                @Override
                public String key() {
                    return "transformation" + " desiredWidth";
                }
            };

            Picasso.get()
                    .load(url)
                    .transform(transformation)
                    .into(view, new Callback() {
                        @Override
                        public void onSuccess() {
                            view.setVisibility(View.VISIBLE);
                        }

                        @Override
                        public void onError(Exception e) {
                            Timber.d("Could not load image " + url);
                            view.setVisibility(View.GONE);
                        }
                    });
        }
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
            Timber.d("Channel icon '" + iconUrl + "' is empty or null, hiding icon");
            view.setVisibility(View.GONE);
        } else {
            String url = MiscUtils.getIconUrl(view.getContext(), iconUrl);
            Timber.d("Channel icon '" + iconUrl + "' is not empty, loading icon from url '" + url + "'");
            Picasso.get()
                    .load(url)
                    .into(view, new Callback() {
                        @Override
                        public void onSuccess() {
                            Timber.d("Successfully loaded channel icon from url '" + url + "'");
                            view.setVisibility(View.VISIBLE);
                        }

                        @Override
                        public void onError(Exception e) {
                            Timber.d("Error loading channel icon from url '" + url + "'");
                            view.setVisibility(View.GONE);
                        }
                    });
        }
    }

    @BindingAdapter({"iconName", "iconUrl", "iconVisibility"})
    public static void setChannelName(TextView view, String name, String iconUrl, boolean visible) {
        if (visible) {
            setChannelName(view, name, iconUrl);
        } else {
            view.setVisibility(View.GONE);
        }
    }

    /**
     * Shows the channel name in the view if no channel icon exists.
     *
     * @param view    The view where the text and visibility shall be applied to
     * @param name    The name of the channel
     * @param iconUrl The url to the channel icon
     */
    @BindingAdapter({"iconName", "iconUrl"})
    public static void setChannelName(TextView view, String name, String iconUrl) {
        Timber.d("Setting channel name '" + name + "', iconUrl is '" + iconUrl + "'");
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
            boolean lightTheme = sharedPreferences.getBoolean("light_theme_enabled", view.getContext().getResources().getBoolean(R.bool.pref_default_light_theme_enabled));
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
        if (time < 0) {
            view.setText(view.getContext().getString(R.string.any));
            return;
        }

        String localizedTime = "";

        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(view.getContext());
        if (sharedPreferences.getBoolean("localized_date_time_format_enabled", view.getContext().getResources().getBoolean(R.bool.pref_default_localized_date_time_format_enabled))) {
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
        if (date < 0) {
            view.setText(view.getContext().getString(R.string.any));
            return;
        }

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

    /**
     * Calculates the genre color from the given content type and sets it as the
     * background color of the given view
     *
     * @param view            The view that displays the genre color as a background
     * @param contentType     The content type to calculate the color from
     * @param showGenreColors True to show the color, false otherwise
     * @param offset          Positive offset from 0 to 100 to increase the transparency of the color
     */
    @BindingAdapter({"genreColor", "showGenreColor", "genreColorAlphaOffset"})
    public static void setGenreColor(TextView view, int contentType, boolean showGenreColors, int offset) {
        Context context = view.getContext();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        if (showGenreColors) {
            int color = context.getResources().getColor(android.R.color.transparent);
            if (contentType >= 0) {
                // Get the genre color from the content type
                color = R.color.EPG_OTHER;
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

                // Get the color with the desired alpha value
                int c = context.getResources().getColor(color);
                int transparencyValue = sharedPreferences.getInt("genre_color_transparency", Integer.valueOf(view.getContext().getResources().getString(R.string.pref_default_genre_color_transparency)));
                int alpha = (int) (((float) (transparencyValue - offset)) / 100.0f * 255.0f);
                if (alpha < 0) {
                    alpha = 0;
                }
                color = Color.argb(alpha, Color.red(c), Color.green(c), Color.blue(c));
            }

            view.setBackgroundColor(color);
            view.setVisibility(View.VISIBLE);
        } else {
            view.setVisibility(View.GONE);
        }
    }
}
