package org.tvheadend.tvhclient.activities;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.afollestad.materialdialogs.folderselector.FolderChooserDialog;

import org.tvheadend.tvhclient.Constants;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.fragments.settings.SettingsAdvancedFragment;
import org.tvheadend.tvhclient.fragments.settings.SettingsCastingFragment;
import org.tvheadend.tvhclient.fragments.settings.SettingsFragment;
import org.tvheadend.tvhclient.fragments.settings.SettingsManageConnectionFragment;
import org.tvheadend.tvhclient.fragments.settings.SettingsNotificationFragment;
import org.tvheadend.tvhclient.fragments.settings.SettingsProfilesFragment;
import org.tvheadend.tvhclient.fragments.settings.SettingsShowConnectionsFragment;
import org.tvheadend.tvhclient.fragments.settings.SettingsTranscodingFragment;
import org.tvheadend.tvhclient.fragments.settings.SettingsUserInterfaceFragment;
import org.tvheadend.tvhclient.interfaces.ActionBarInterface;
import org.tvheadend.tvhclient.interfaces.BackPressedInterface;
import org.tvheadend.tvhclient.interfaces.SettingsInterface;
import org.tvheadend.tvhclient.utils.MiscUtils;
import org.tvheadend.tvhclient.utils.Utils;

import java.io.File;

public class SettingsActivity extends AppCompatActivity implements ActionBarInterface, SettingsInterface, FolderChooserDialog.FolderCallback {

    @SuppressWarnings("unused")
    private final static String TAG = SettingsActivity.class.getSimpleName();

    private ActionBar actionBar = null;
    private TextView actionBarTitle;
    private TextView actionBarSubtitle;
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
    private final static int NOTIFICATIONS = 7;
    private final static int CASTING = 8;
    private final static int USER_INTERFACE = 9;
    private final static int ADVANCED = 10;

    private int currentSettingsMode = MAIN_SETTINGS;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(MiscUtils.getThemeId(this));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        Utils.setLanguage(this);

        // Setup the action bar and show the title
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }
        // Get the widgets so we can use them later and do not need to inflate again
        actionBarTitle = (TextView) toolbar.findViewById(R.id.actionbar_title);
        actionBarSubtitle = (TextView) toolbar.findViewById(R.id.actionbar_subtitle);

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
    public void onPostResume() {
        super.onPostResume();
        // When the orientation was changed the last visible fragment is
        // available from the manager. If this is the case get it and show it
        // again.
        fragment = getFragmentManager().findFragmentById(R.id.main);
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
            getFragmentManager().beginTransaction()
                    .replace(R.id.main, fragment)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
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
            break;

        case LIST_CONNECTIONS:
            if (manageConnections) {
                restartNow();
            } else {
                mainSettings();
            }
            break;

        case ADD_CONNECTION:
        case EDIT_CONNECTION:
            fragment = getFragmentManager().findFragmentById(R.id.main);
            if (fragment != null && fragment instanceof BackPressedInterface) {
                ((BackPressedInterface) fragment).onBackPressed();
            }
            break;

        case PROFILES:
        case TRANSCODING:
        case NOTIFICATIONS:
        case CASTING:
        case USER_INTERFACE:
        case ADVANCED:
            // Any changes in these fragments need to be changed when the back
            // or home key was pressed. This is only available in the activity,
            // not in the fragment. Therefore trigger the saving from here.
            fragment = getFragmentManager().findFragmentById(R.id.main);
            if (fragment != null && fragment instanceof BackPressedInterface) {
                ((BackPressedInterface) fragment).onBackPressed();
            }
            getFragmentManager().popBackStack();
            currentSettingsMode = MAIN_SETTINGS;
            break;
        }
    }

    /**
     * Removes the previous fragment from the view and back stack so that
     * navigating back would not show the old fragment again.
     */
    private void removePreviousFragment() {
        Fragment f = getFragmentManager().findFragmentById(R.id.main);
        if (f != null) {
            getFragmentManager()
                    .beginTransaction()
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE)
                    .remove(f)
                    .commit();
        }
    }

    @Override
    public void setActionBarTitle(final String title) {
        if (actionBar != null && actionBarTitle != null) {
            actionBarTitle.setText(title);
        }
    }

    @SuppressLint("RtlHardcoded")
    @Override
    public void setActionBarSubtitle(final String subtitle) {
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
    public void setActionBarIcon(Bitmap bitmap) {
        // NOP
    }

    @Override
    public void setActionBarIcon(int resource) {
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

        getFragmentManager().beginTransaction()
                .replace(R.id.main, new SettingsFragment())
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .commit();
    }

    @Override
    public void showConnections() {
        currentSettingsMode = LIST_CONNECTIONS;
        getFragmentManager().beginTransaction()
                .replace(R.id.main, new SettingsShowConnectionsFragment())
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void showAddConnection() {
        currentSettingsMode = ADD_CONNECTION;
        getFragmentManager().beginTransaction()
                .replace(R.id.main, new SettingsManageConnectionFragment())
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
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
        getFragmentManager().beginTransaction().replace(R.id.main, f)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void showProfiles() {
        currentSettingsMode = PROFILES;
        getFragmentManager().beginTransaction()
                .replace(R.id.main, new SettingsProfilesFragment())
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void showTranscodingSettings() {
        currentSettingsMode = TRANSCODING;
        getFragmentManager().beginTransaction()
                .replace(R.id.main, new SettingsTranscodingFragment())
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void showNotifications() {
        currentSettingsMode = TRANSCODING;
        getFragmentManager().beginTransaction()
                .replace(R.id.main, new SettingsNotificationFragment())
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void showCasting() {
        currentSettingsMode = CASTING;
        getFragmentManager().beginTransaction()
                .replace(R.id.main, new SettingsCastingFragment())
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void showUserInterface() {
        currentSettingsMode = USER_INTERFACE;
        getFragmentManager().beginTransaction()
                .replace(R.id.main, new SettingsUserInterfaceFragment())
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void showAdvanced() {
        currentSettingsMode = ADVANCED;
        getFragmentManager().beginTransaction()
                .replace(R.id.main, new SettingsAdvancedFragment())
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onFolderSelection(@NonNull FolderChooserDialog dialog, @NonNull File folder) {
        String strippedPath = folder.getAbsolutePath().replace(Environment.getExternalStorageDirectory().getAbsolutePath(), "");
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.edit().putString("pref_download_directory", strippedPath).apply();

        Fragment f = getFragmentManager().findFragmentById(R.id.main);
        if (f != null && f.isAdded() && f instanceof SettingsFragment) {
            ((SettingsFragment) f).updateDownloadDirSummary();
        }
    }

    @Override
    public void onFolderChooserDismissed(@NonNull FolderChooserDialog dialog) {
        // NOP
    }
}
