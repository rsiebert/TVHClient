package org.tvheadend.tvhclient;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;

import org.tvheadend.tvhclient.interfaces.HTSListener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Locale;

@SuppressWarnings("deprecation")
public class UnlockerActivity extends ActionBarActivity implements HTSListener {

    private final static String TAG = UnlockerActivity.class.getSimpleName();

    private CoordinatorLayout coordinatorLayout;
    private TVHClientApplication app;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(Utils.getThemeId(this));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.webview_layout);
        Utils.setLanguage(this);

        app = (TVHClientApplication) getApplication();
        app.addListener(this);

        // Setup the action bar and show the title
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setTitle("");
        }

        coordinatorLayout = (CoordinatorLayout)findViewById(R.id.coordinatorLayout);

        WebView webview = (WebView) findViewById(R.id.webview);
        if (webview != null) {
            app.log(TAG, "Showing unlocker features");

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

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            final Locale current = getResources().getConfiguration().locale;
            final String locale = prefs.getString("languagePref", current.getLanguage()).substring(0, 2);
            final String htmlFile = "html/features_" + locale.substring(0, 2) + ".html";
            final String defaultHtmlFile = "html/features_en.html";

            // Open the HTML file of the defined language. This is determined by
            // the locale. If the file doesn't exist, open the default (English)
            InputStream is = null;
            try {
                app.log(TAG, "Trying to load html page " + htmlFile + ", system locale is " + current.getLanguage()); 
                is = getAssets().open(htmlFile);
            } catch (IOException ex1) {
                try {
                    app.log(TAG, "Error loading html file, loading " + defaultHtmlFile);
                    is = getAssets().open(defaultHtmlFile);
                } catch (IOException ex2) {
                    app.log(TAG, "Error loading default html file");
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
            } catch (Exception ex) {
                sb.append("Error parsing feature list");
            }

            // Add the closing HTML tags and load show the page
            sb.append("</body></html>");
            webview.loadDataWithBaseURL("file:///android_asset/", sb.toString(), "text/html","utf-8", null);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        app.removeListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.purchase_cancel_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        TVHClientApplication app = (TVHClientApplication) getApplication();
        switch (item.getItemId()) {
        case android.R.id.home:
            finish();
            return true;

        case R.id.menu_purchase:
            app.log(TAG, "Unlocker purchase button pressed");

            // TODO
            // for security, generate your payload here for verification.
            // See the comments on verifyDeveloperPayload() for more info.
            String payload = "";

            // Open the activity where the user can actually make the purchase
            if (app.isBillingSetupDone()) {
                app.log(TAG, "Billing initialized, showing unlocker dialog");
                app.getOpenIabHelper().launchPurchaseFlow(this, Constants.UNLOCKER, 10001, app, payload);
            } else {
                app.log(TAG, "Billing not initialized, not showing unlocker dialog");
                Snackbar.make(coordinatorLayout, getString(R.string.unlocker_not_initialized), Snackbar.LENGTH_LONG).show();
            }

            return true;

        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        app.log(TAG, "onActivityResult requestCode " + requestCode + ", resultCode " + resultCode);
        TVHClientApplication app = (TVHClientApplication) getApplication();
        // Pass on the activity result to the helper for handling
        if (!app.getOpenIabHelper().handleActivityResult(requestCode, resultCode, data)) {
            app.log(TAG, "Unlocker purchase dialog was not shown or closed without action");
            // The billing activity was not shown or did nothing. Nothing needs
            // to be done
            super.onActivityResult(requestCode, resultCode, data);
        }
        // Close this activity
        app.log(TAG, "Exiting unlocker"); 
        finish();
    }

    @Override
    public void onMessage(String action, final Object obj) {
        switch (action) {
            case Constants.ACTION_SHOW_MESSAGE:
                runOnUiThread(new Runnable() {
                    public void run() {
                        String msg = (String) obj;
                        Snackbar.make(coordinatorLayout, msg, Snackbar.LENGTH_SHORT).show();
                    }
                });
                break;
        }
    }
}
