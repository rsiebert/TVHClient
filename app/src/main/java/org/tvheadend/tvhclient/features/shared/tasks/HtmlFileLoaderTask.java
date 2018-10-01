package org.tvheadend.tvhclient.features.shared.tasks;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.utils.MiscUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.util.Locale;

import androidx.annotation.NonNull;
import timber.log.Timber;

public class HtmlFileLoaderTask extends AsyncTask<Void, Void, String> {

    private final String file;
    private final String defaultLocale;
    private final WeakReference<Context> context;
    private final FileLoaderCallback callback;

    public HtmlFileLoaderTask(Context context, String file, String defaultLocale, @NonNull FileLoaderCallback callback) {
        this.context = new WeakReference<>(context);
        this.file = file;
        this.defaultLocale = defaultLocale;
        this.callback = callback;
    }

    @Override
    protected String doInBackground(Void... voids) {
        Context ctx = context.get();
        if (ctx != null) {
            return loadHtmlFromFile(ctx, file, defaultLocale);
        }
        return null;
    }

    @Override
    protected void onPostExecute(String content) {
        callback.notify(content);
    }

    private String loadHtmlFromFile(Context context, String filename, String defaultLocale) {
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
        // the defaultLocale. If the file doesn't exist, open the default (English)
        InputStream is = null;
        try {
            is = context.getAssets().open(htmlFile);
        } catch (IOException ex1) {
            Timber.e("Could not open file " + htmlFile);
        }
        if (is == null) {
            try {
                is = context.getAssets().open(defaultHtmlFile);
            } catch (IOException ex2) {
                Timber.e("Could not open default file " + defaultHtmlFile);
            }
        }

        // Try to parse the HTML contents from the given asset file. It
        // contains the feature description with the required HTML tags.
        if (is != null) {
            try {
                String htmlData;
                BufferedReader in = new BufferedReader(new InputStreamReader(is, "utf-8"));
                while ((htmlData = in.readLine()) != null) {
                    sb.append(htmlData);
                }
                in.close();

            } catch (UnsupportedEncodingException uee) {
                Timber.e("Could not create buffered reader, unsupported encoding");
                sb.append("Error parsing feature list");
            } catch (IOException ioe) {
                Timber.e("Error while reading contents from input stream or closing it");
                sb.append("Error parsing feature list");
            }
            try {
                is.close();
            } catch (IOException e) {
                Timber.e("Error closing input stream");
            }
        }

        // Add the closing HTML tags and load show the page
        sb.append("</body></html>");
        Timber.d("Done loading file");
        return sb.toString();
    }
}
