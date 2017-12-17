package org.tvheadend.tvhclient.utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.tvheadend.tvhclient.R;

import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

public class MiscUtils {

    // Offset that reduces the visibility of the program guide colors a little
    private final static int GENRE_COLOR_ALPHA_EPG_OFFSET = 50;

    private MiscUtils() {
        throw new IllegalAccessError("Utility class");
    }

    /**
     * Returns the cached image file. When no channel icon shall be shown return
     * null instead of the icon. The icon will not be shown anyway, so returning
     * null will drastically reduce memory consumption.
     *
     * @param url The url of the file
     * @return The actual image of the file as a bitmap
     */
    public static Bitmap getCachedIcon(Context context, final String url) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Boolean showIcons = prefs.getBoolean("showIconPref", true);
        if (!showIcons) {
            return null;
        }
        if (url == null || url.length() == 0) {
            return null;
        }
        File file = new File(context.getCacheDir(), convertUrlToHashString(url) + ".png");
        return BitmapFactory.decodeFile(file.toString());
    }

    /**
     * Converts the given url into a unique hash value.
     *
     * @param url The url that shall be converted
     * @return The hash value or the url or an empty string if an error occurred
     */
    public static String convertUrlToHashString(String url) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(url.getBytes());
            byte messageDigest[] = digest.digest();
            StringBuilder hexString = new StringBuilder();
            for (byte md : messageDigest) {
                hexString.append(Integer.toHexString(0xFF & md));
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            // NOP
        }
        return null;
    }

    /**
     * Returns the id of the theme that is currently set in the settings.
     *
     * @param context Context
     * @return Id of the light or dark theme
     */
    public static int getThemeId(final Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Boolean theme = prefs.getBoolean("lightThemePref", true);
        return (theme ? R.style.CustomTheme_Light : R.style.CustomTheme);
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
     * Returns the background color of the genre based on the content type. The
     * first byte of hex number represents the main category.
     *
     * @param context     Activity Context
     * @param contentType Content type number
     * @param offset      Value that defines the transparency
     * @return Color value
     */
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
            int alpha = (int) (((float) prefs.getInt("showGenreColorsVisibilityPref", 70)) / 100.0f * 255.0f);
            if (alpha >= offset) {
                alpha -= offset;
            }
            return Color.argb(alpha, Color.red(c), Color.green(c), Color.blue(c));
        }
    }

    /**
     * If the show genre color setting is activated for a certain screen, then it will be set here
     *
     * @param context Activity context
     * @param view    The view that shows the genre color
     * @param contentType contentType
     */
    public static void setGenreColor(final Context context, View view, final int contentType, final String tag) {
        if (view == null) {
            return;
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean showGenre = false;
        int offset = 0;

        // Check which class is calling and get the setting
        switch (tag) {
            case "ChannelListAdapter":
                showGenre = prefs.getBoolean("showGenreColorsChannelsPref", false);
                break;
            case "ProgramListAdapter":
                showGenre = prefs.getBoolean("showGenreColorsProgramsPref", false);
                break;
            case "SearchResultAdapter":
                showGenre = prefs.getBoolean("showGenreColorsSearchPref", false);
                break;
            case "ProgramGuideItemView":
                showGenre = prefs.getBoolean("showGenreColorsGuidePref", false);
                offset = GENRE_COLOR_ALPHA_EPG_OFFSET;
                break;
        }

        // As a default we show a transparent color. If we have a program then
        // use the provided genre color. If genre colors shall not be shown we
        // also show the transparent color. This is used in the EPG where the
        // background is used as the genre indicator.
        int color = context.getResources().getColor(android.R.color.transparent);
        if (contentType > 0 && showGenre) {
            color = getGenreColor(context, contentType, offset);
        }

        if (view instanceof TextView) {
            if (showGenre) {
                view.setBackgroundColor(color);
                view.setVisibility(View.VISIBLE);
            } else {
                view.setVisibility(View.GONE);
            }
        } else if (view instanceof LinearLayout) {
            // When the program view guide is shown the the channel list on the left uses a
            // different layout. This linear layout shall show no color. The linear
            // layout for each program in the right area shall show the genre color.
            if (tag.equals("ProgramGuideItemView")) {
                // Get the shape where the background color will be set, if the
                // linear layout would be used directly, the borders would be overwritten
                LayerDrawable layers = (LayerDrawable) view.getBackground();
                GradientDrawable shape = (GradientDrawable) (layers.findDrawableByLayerId(R.id.timeline_item_genre));
                shape.setColor(color);
            } else if (tag.equals("ChannelListAdapter")) {
                color = context.getResources().getColor(android.R.color.transparent);
                view.setBackgroundColor(color);
            }
        }
    }
}
