package org.tvheadend.tvhclient.utils;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import org.tvheadend.tvhclient.R;

import java.text.SimpleDateFormat;
import java.util.Locale;

import androidx.databinding.BindingAdapter;
import timber.log.Timber;

public class BindingAdapterUtils {

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
}
