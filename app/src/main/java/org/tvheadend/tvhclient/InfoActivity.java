package org.tvheadend.tvhclient;

import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Locale;
import java.util.regex.Pattern;

public class InfoActivity extends AppCompatActivity {

    @SuppressWarnings("unused")
    private final static String TAG = InfoActivity.class.getSimpleName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(Utils.getThemeId(this));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.webview_layout);
        Utils.setLanguage(this);

        // Setup the action bar and show the title
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setTitle(" ");
        }

        WebView webview = (WebView) findViewById(R.id.webview);
        if (webview != null) {
            // Create the string that is later used to display the HTML page.
            // The string contains all feature information and HTML tags.
            // Depending on the theme the correct style sheet will be loaded
            // from the asset folder. 
            StringBuilder sb = new StringBuilder();
            sb.append("<html><head>");
            if (Utils.getThemeId(this) == R.style.CustomTheme_Light) {
                webview.setBackgroundColor(getResources().getColor(android.R.color.transparent));
                sb.append("<link href=\"html/styles_light.css\" type=\"text/css\" rel=\"stylesheet\"/>");
            } else {
                webview.setBackgroundColor(getResources().getColor(android.R.color.transparent));
                sb.append("<link href=\"html/styles_dark.css\" type=\"text/css\" rel=\"stylesheet\"/>");
            }
            sb.append("</head><body>");

            // Open the HTML file of the defined language. This is determined by
            // the locale. If the file doesn't exist, open the default (English)
            InputStream is = null;
            try {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                final Locale current = getResources().getConfiguration().locale;
                final String locale = prefs.getString("languagePref", current.getLanguage()).substring(0, 2);
                is = getAssets().open("html/info_help_" + locale.substring(0, 2) + ".html");
            } catch (IOException ex1) {
                try {
                    is = getAssets().open("html/info_help_en.html");
                } catch (IOException e) {
                    e.printStackTrace();
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
                e.printStackTrace();
                sb.append("Error parsing feature list");
            }

            // Add the closing HTML tags and load show the page
            sb.append("</body></html>");

            // Replace the placeholder in the html file with the real version
            String version;
            String s = sb.toString();
            try {
                String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
                int versionCode = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
                version = versionName + " (" + versionCode + ")";
                s = Pattern.compile("APP_VERSION").matcher(sb).replaceAll(version);
            } catch (NameNotFoundException e) {
                e.printStackTrace();
            }

            webview.loadDataWithBaseURL("file:///android_asset/", s, "text/html","utf-8", null);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.info_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            finish();
            return true;

        case R.id.menu_changelog:
        	final ChangeLogDialog cld = new ChangeLogDialog(this);
            cld.getFullLogDialog().show();
            return true;

        default:
            return super.onOptionsItemSelected(item);
        }
    }
}
