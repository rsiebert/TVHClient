package org.tvheadend.tvhclient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;

@SuppressWarnings("deprecation")
public class InfoActivity extends ActionBarActivity {

    @SuppressWarnings("unused")
    private final static String TAG = InfoActivity.class.getSimpleName();

    private ActionBar actionBar = null;
    private WebView webview;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(Utils.getThemeId(this));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.webview_layout);
        Utils.setLanguage(this);

        // Setup the action bar and show the title
        actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setTitle(" ");

        webview = (WebView) findViewById(R.id.webview);
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
                String locale = prefs.getString("languagePref", "en").substring(0, 2);
                is = getAssets().open("html/info_help_" + locale.substring(0, 2) + ".html");
            } catch (IOException ex1) {
                try {
                    is = getAssets().open("html/info_help_en.html");
                } catch (IOException ex2) {
                    
                }
            }

            // Try to parse the HTML contents from the given asset file. It
            // contains the feature description with the required HTML tags.
            try {
                String htmlData;
                BufferedReader in = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                while ((htmlData = in.readLine()) != null) {
                    sb.append(htmlData);
                }
                in.close();
            } catch (Exception ex) {
                sb.append("Error parsing feature list");
            }

            // Add the closing HTML tags and load show the page
            sb.append("</body></html>");
            webview.loadDataWithBaseURL("file:///android_asset/", sb.toString(), "text/html","utf-8", null);
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
