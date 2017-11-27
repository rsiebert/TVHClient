package org.tvheadend.tvhclient.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.tvheadend.tvhclient.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Locale;

public class MiscUtils {
    private static final String TAG = MiscUtils.class.getSimpleName();

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
