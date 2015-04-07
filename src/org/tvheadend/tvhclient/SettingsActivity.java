package org.tvheadend.tvhclient;

import org.tvheadend.tvhclient.fragments.SettingsFragment;
import org.tvheadend.tvhclient.fragments.SettingsManageConnectionFragment;
import org.tvheadend.tvhclient.fragments.SettingsProfilesFragment;
import org.tvheadend.tvhclient.fragments.SettingsShowConnectionsFragment;
import org.tvheadend.tvhclient.fragments.SettingsTranscodingFragment;
import org.tvheadend.tvhclient.interfaces.ActionBarInterface;
import org.tvheadend.tvhclient.interfaces.SettingsInterface;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class SettingsActivity extends ActionBarActivity implements ActionBarInterface, SettingsInterface {

    @SuppressWarnings("unused")
    private final static String TAG = SettingsActivity.class.getSimpleName();

    private ActionBar actionBar = null;
    private TextView actionBarTitle;
    private TextView actionBarSubtitle;
    private ImageView actionBarIcon;
    private Fragment fragment;

    private static boolean restart = false;
    private static boolean reconnect = false;
    private boolean manageConnections = false;

    private final static int MAIN_SETTINGS = 1;
    private final static int LIST_CONNECTIONS = 2;
    private final static int EDIT_CONNECTION = 3;
    private final static int ADD_CONNECTION = 4;
    private final static int PROFILES = 5;
    private final static int TRANSCODING = 6;

    private int currentSettingsMode = MAIN_SETTINGS; 

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(Utils.getThemeId(this));
        super.onCreate(savedInstanceState);
        Utils.setLanguage(this);

        // Setup the action bar and show the title
        actionBar = getSupportActionBar();
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        actionBar.setCustomView(R.layout.actionbar_title);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);

        // Get the widgets so we can use them later and do not need to inflate again
        actionBarTitle = (TextView) actionBar.getCustomView().findViewById(R.id.actionbar_title);
        actionBarSubtitle = (TextView) actionBar.getCustomView().findViewById(R.id.actionbar_subtitle);
        actionBarIcon = (ImageView) actionBar.getCustomView().findViewById(R.id.actionbar_icon);
        actionBarIcon.setVisibility(View.GONE);
        
        // Get any saved values from the bundle
        if (savedInstanceState != null) {
            currentSettingsMode = savedInstanceState.getInt(Constants.BUNDLE_SETTINGS_MODE);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(Constants.BUNDLE_SETTINGS_MODE, currentSettingsMode);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onResume() {
        super.onResume();
        // When the orientation was changed the last visible fragment is
        // available from the manager. If this is the case get it and show it
        // again.
        fragment = (Fragment) getSupportFragmentManager().findFragmentById(android.R.id.content);
        if (fragment == null) {
            // Get the information if the connection fragment shall be shown.
            // This is the case when the user has selected the connection menu
            // from the navigation drawer.
            Bundle bundle = getIntent().getExtras();
            if (bundle != null) {
                manageConnections = bundle.getBoolean(Constants.BUNDLE_MANAGE_CONNECTIONS, false);
            }
            // Now show the manage connection or the general settings fragment
            if (manageConnections) {
                showConnections();
            } else {
                mainSettings();
            }
        } else {
            // Show the available fragment
            getSupportFragmentManager().beginTransaction()
                    .replace(android.R.id.content, fragment)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .commit();
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            onBackPressed();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        // Depending on the current mode either close the current subscreen by
        // popping the fragment back stack or exit the main settings screen
        switch (currentSettingsMode) {
        case MAIN_SETTINGS:
            restartNow();
            return;

        case LIST_CONNECTIONS:
            if (manageConnections) {
                restartNow();
            } else {
                mainSettings();
            }
            return;

        case ADD_CONNECTION:
        case EDIT_CONNECTION:
            getSupportFragmentManager().popBackStack();
            currentSettingsMode = LIST_CONNECTIONS;
            return;

        case PROFILES:
        case TRANSCODING:
            getSupportFragmentManager().popBackStack();
            currentSettingsMode = MAIN_SETTINGS;
            return;
        }
    }

    /**
     * Removes the previous fragment from the view and back stack so that
     * navigating back would not show the old fragment again.
     */
    private void removePreviousFragment() {
        Fragment f = (Fragment) getSupportFragmentManager().findFragmentById(android.R.id.content);
        if (f != null) {
            getSupportFragmentManager().beginTransaction().remove(f).commit();
        }
    }

    @Override
    public void setActionBarTitle(final String title, final String tag) {
        if (actionBar != null && actionBarTitle != null) {
            actionBarTitle.setText(title);
        }
    }

    @SuppressLint("RtlHardcoded")
    @Override
    public void setActionBarSubtitle(final String subtitle, final String tag) {
        if (actionBar != null && actionBarSubtitle != null) {
            actionBarSubtitle.setText(subtitle);
            if (subtitle.length() == 0) {
                actionBarSubtitle.setVisibility(View.GONE);
                actionBarTitle.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
            } else {
                actionBarSubtitle.setVisibility(View.VISIBLE);
                actionBarTitle.setGravity(Gravity.LEFT | Gravity.BOTTOM);
            }
        }
    }

    @Override
    public void setActionBarIcon(Bitmap bitmap, String tag) {
        // NOP
    }

    @Override
    public void setActionBarIcon(int resource, String tag) {
        // NOP
    }

    @Override
    public void restart() {
        restart = true;
    }

    @Override
    public void reconnect() {
        reconnect = true;
    }

    @Override
    public void restartNow() {
        Intent intent = getIntent();
        intent.putExtra(Constants.BUNDLE_RESTART, restart);
        intent.putExtra(Constants.BUNDLE_RECONNECT, reconnect);
        setResult(RESULT_OK, intent);
        finish();
    }

    private void mainSettings() {
        currentSettingsMode = MAIN_SETTINGS;
        removePreviousFragment();

        getSupportFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .commit();
    }

    @Override
    public void showConnections() {
        currentSettingsMode = LIST_CONNECTIONS;
        getSupportFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsShowConnectionsFragment())
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void showAddConnection() {
        currentSettingsMode = ADD_CONNECTION;
        getSupportFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsManageConnectionFragment())
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void showEditConnection(long id) {
        currentSettingsMode = EDIT_CONNECTION;
        Bundle bundle = new Bundle();
        bundle.putLong(Constants.BUNDLE_CONNECTION_ID, id);
        Fragment f = Fragment.instantiate(this, SettingsManageConnectionFragment.class.getName());
        f.setArguments(bundle);
        getSupportFragmentManager().beginTransaction().replace(android.R.id.content, f)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void showProfiles() {
        currentSettingsMode = PROFILES;
        getSupportFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsProfilesFragment())
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void showTranscodingSettings() {
        currentSettingsMode = TRANSCODING;
        getSupportFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsTranscodingFragment())
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void done(int resultCode) {
        // This method is provided by the settings interface and is called from
        // the profile, transcoding and connection fragments. If the result is
        // ok then something has changed, otherwise these fragments shall just
        // be removed. 
        if (resultCode == Activity.RESULT_OK) {
            reconnect = true;
        }
        onBackPressed();
    }
}
