package org.tvheadend.tvhclient.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.model.Program;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Locale;

public class MiscUtils {

    // Offset that reduces the visibility of the program guide colors a little
    private final static int GENRE_COLOR_ALPHA_EPG_OFFSET = 50;

    private MiscUtils() {
        throw new IllegalAccessError("Utility class");
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
     * @param program Program
     */
    public static void setGenreColor(final Context context, View view, final Program program, final String tag) {
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

    /**
     *
     * @param context
     * @param filename
     * @param defaultLocale
     * @return
     */
    public static String loadHtmlFromFile(Context context, String filename, String defaultLocale) {
        // Create the string that is later used to display the HTML page.
        // The string contains all feature information and HTML tags.
        // Depending on the theme the correct style sheet will be loaded
        // from the asset folder.
        StringBuilder sb = new StringBuilder();
        sb.append("<html><head>");
        if (MiscUtils.getThemeId(context) == R.style.CustomTheme_Light) {
            sb.append("<link href=\"html/styles_light.css\" type=\"text/css\" rel=\"stylesheet\"/>");
        } else {
            sb.append("<link href=\"html/styles_dark.css\" type=\"text/css\" rel=\"stylesheet\"/>");
        }
        sb.append("</head><body>");

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        final Locale current = context.getResources().getConfiguration().locale;
        final String locale = prefs.getString("languagePref", current.getLanguage()).substring(0, 2);
        final String htmlFile = "html/" + filename + "_" + locale.substring(0, 2) + ".html";
        final String defaultHtmlFile = "html/" + filename + "_" + defaultLocale + ".html";

        // Open the HTML file of the defined language. This is determined by
        // the locale. If the file doesn't exist, open the default (English)
        InputStream is = null;
        try {
            is = context.getAssets().open(htmlFile);
        } catch (IOException ex1) {
            try {
                is = context.getAssets().open(defaultHtmlFile);
            } catch (IOException ex2) {
                // NOP
            }
        }

        // Try to parse the HTML contents from the given asset file. It
        // contains the feature description with the required HTML tags.
        try {
            String htmlData;
            if (is != null) {
                BufferedReader in = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                while ((htmlData = in.readLine()) != null) {
                    sb.append(htmlData);
                }
                in.close();
            }
        } catch (Exception e) {
            sb.append("Error parsing feature list");
        }

        // Add the closing HTML tags and load show the page
        sb.append("</body></html>");
        return sb.toString();
    }
}
