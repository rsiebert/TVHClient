package org.tvheadend.tvhclient.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;

import com.mikepenz.materialdrawer.model.interfaces.IProfile;

import org.tvheadend.tvhclient.data.Constants;
import org.tvheadend.tvhclient.data.DatabaseHelper;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.ui.channels.ChannelListFragment;
import org.tvheadend.tvhclient.ui.epg.ProgramGuideActivity;
import org.tvheadend.tvhclient.ui.unlocker.UnlockerActivity;
import org.tvheadend.tvhclient.ui.information.InfoActivity;
import org.tvheadend.tvhclient.ui.information.StatusActivity;
import org.tvheadend.tvhclient.ui.progams.ProgramListFragment;
import org.tvheadend.tvhclient.ui.recordings.CompletedRecordingListFragment;
import org.tvheadend.tvhclient.ui.recordings.FailedRecordingListFragment;
import org.tvheadend.tvhclient.ui.recordings.RemovedRecordingListFragment;
import org.tvheadend.tvhclient.ui.recordings.ScheduledRecordingListFragment;
import org.tvheadend.tvhclient.ui.recordings.SeriesRecordingListFragment;
import org.tvheadend.tvhclient.ui.recordings.TimerRecordingListFragment;
import org.tvheadend.tvhclient.ui.settings.SettingsActivity;
import org.tvheadend.tvhclient.service.HTSListener;
import org.tvheadend.tvhclient.data.model.Connection;
import org.tvheadend.tvhclient.data.tasks.WakeOnLanTaskCallback;
import org.tvheadend.tvhclient.utils.Utils;

import java.util.ArrayList;

// TODO make nav image blasser
// TODO confirmation of added/edited recordings...
// TODO onQueryTextSubmit does nothing currently
// TODO navigation menu marking (status, ...)

public class NavigationDrawerActivity extends MainActivity implements HTSListener, WakeOnLanTaskCallback, NavigationDrawerCallback {

    private CoordinatorLayout coordinatorLayout;
    // Default navigation drawer menu position and the list positions
    private int selectedNavigationMenuId = NavigationDrawer.MENU_UNKNOWN;
    private int defaultMenuPosition = NavigationDrawer.MENU_UNKNOWN;
    // Holds the list of selected menu items so the previous fragment can be
    // shown again when the user has pressed the back key.
    private ArrayList<Integer> menuStack = new ArrayList<>();

    // Remember if the connection setting screen was already shown. When the
    // main activity starts for the first time and no connections are configured
    // or active the connection settings activity will be shown once.
    private boolean connectionSettingsShown = false;
    // Contains the information about the current connection state.
    private String connectionStatus = Constants.ACTION_CONNECTION_STATE_UNKNOWN;

    private TVHClientApplication app;
    private DatabaseHelper databaseHelper;
    private SharedPreferences sharedPreferences;
    private NavigationDrawer navigationDrawer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = (TVHClientApplication) getApplication();

        databaseHelper = DatabaseHelper.getInstance(getApplicationContext());
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        navigationDrawer = new NavigationDrawer(this, savedInstanceState, toolbar, this);
        navigationDrawer.createHeader();
        navigationDrawer.createMenu();

        coordinatorLayout = findViewById(R.id.coordinatorLayout);

        defaultMenuPosition = Integer.parseInt(sharedPreferences.getString("defaultMenuPositionPref", String.valueOf(NavigationDrawer.MENU_STATUS)));

        if (savedInstanceState == null) {
            // When the activity is created it got called by the main activity. Get the initial
            // navigation menu position and show the associated fragment with it. When the device
            // was rotated just restore the position from the saved instance.
            selectedNavigationMenuId = getIntent().getIntExtra("navigation_menu_position", NavigationDrawer.MENU_CHANNELS);
            if (selectedNavigationMenuId >= 0) {
                handleDrawerItemSelected(selectedNavigationMenuId);
            }
        } else {
            // If the saved instance is not null then we return from an orientation
            // change. The drawer menu could be open, so update the recording
            // counts. Also get any saved values from the bundle.
            menuStack = savedInstanceState.getIntegerArrayList("menu_stack");
            selectedNavigationMenuId = savedInstanceState.getInt("navigation_menu_position", NavigationDrawer.MENU_CHANNELS);
            connectionStatus = savedInstanceState.getString("connection_status");
            connectionSettingsShown = savedInstanceState.getBoolean("connection_settings_shown");
        }
    }

    /**
     * Called when a menu item from the navigation drawer was selected. It loads
     * and shows the correct fragment or fragments depending on the selected
     * menu item.
     *
     * @param position Selected position within the menu array
     */
    private void handleDrawerItemSelected(int position) {

        // Save the menu position so we know which one was selected
        selectedNavigationMenuId = position;

        Intent intent;
        Fragment fragment = null;
        switch (position) {
            case NavigationDrawer.MENU_CHANNELS:
                fragment = new ChannelListFragment();
                break;
            case NavigationDrawer.MENU_PROGRAM_GUIDE:
                intent = new Intent(this, ProgramGuideActivity.class);
                startActivity(intent);
                break;
            case NavigationDrawer.MENU_COMPLETED_RECORDINGS:
                fragment = new CompletedRecordingListFragment();
                break;
            case NavigationDrawer.MENU_SCHEDULED_RECORDINGS:
                fragment = new ScheduledRecordingListFragment();
                break;
            case NavigationDrawer.MENU_SERIES_RECORDINGS:
                fragment = new SeriesRecordingListFragment();
                break;
            case NavigationDrawer.MENU_TIMER_RECORDINGS:
                fragment = new TimerRecordingListFragment();
                break;
            case NavigationDrawer.MENU_FAILED_RECORDINGS:
                fragment = new FailedRecordingListFragment();
                break;
            case NavigationDrawer.MENU_REMOVED_RECORDINGS:
                fragment = new RemovedRecordingListFragment();
                break;
            case NavigationDrawer.MENU_STATUS:
                selectedNavigationMenuId = defaultMenuPosition;
                intent = new Intent(this, StatusActivity.class);
                intent.putExtra("connection_status", connectionStatus);
                startActivity(intent);
                break;
            case NavigationDrawer.MENU_INFORMATION:
                selectedNavigationMenuId = defaultMenuPosition;
                intent = new Intent(this, InfoActivity.class);
                startActivity(intent);
                break;
            case NavigationDrawer.MENU_SETTINGS:
                selectedNavigationMenuId = defaultMenuPosition;
                intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                break;
            case NavigationDrawer.MENU_UNLOCKER:
                selectedNavigationMenuId = defaultMenuPosition;
                intent = new Intent(this, UnlockerActivity.class);
                startActivity(intent);
                break;
        }
        if (fragment != null) {
            fragment.setArguments(getIntent().getExtras());
            getSupportFragmentManager().beginTransaction().replace(R.id.main, fragment).commit();
        }
    }

    /**
     * Check if the current fragment is a program list fragment. In case single
     * mode is active we need to return to the channel list fragment otherwise
     * show the fragment that belongs to the previously selected menu item.
     */
    @Override
    public void onBackPressed() {
        navigationDrawer.getDrawer().closeDrawer();
        final Fragment f = getSupportFragmentManager().findFragmentById(R.id.main_fragment);
        if (!isDualPane && (f instanceof ProgramListFragment)) {
            getSupportFragmentManager().popBackStack();
        } else {
            if (menuStack.size() > 0) {
                handleDrawerItemSelected(menuStack.remove(menuStack.size() - 1));
            } else {
                super.onBackPressed();
                finish();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // A connection exists and is active, register to receive
        // information from the server and connect to the server
        app.addListener(this);
        // Show the defined fragment from the menu position or the
        // status if the connection state is not fine
        handleDrawerItemSelected(selectedNavigationMenuId);

        connectionStatus = sharedPreferences.getString("last_connection_state", Constants.ACTION_CONNECTION_STATE_OK);
        connectionSettingsShown = sharedPreferences.getBoolean("last_connection_settings_shown", false);

        // Update the drawer menu so that all available menu items are
        // shown in case the recording counts have changed or the user has
        // bought the unlocked version to enable all features
        navigationDrawer.updateDrawerItemBadges();
        navigationDrawer.updateDrawerHeader();
    }

    @Override
    public void onPause() {
        super.onPause();
        app.removeListener(this);

        // Save the previously active connection status and if the connection
        // settings have been already shown
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("last_connection_state", connectionStatus);
        editor.putBoolean("last_connection_settings_shown", connectionSettingsShown);
        editor.apply();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // add the values which need to be saved from the drawer to the bundle
        outState = navigationDrawer.getDrawer().saveInstanceState(outState);
        // add the values which need to be saved from the accountHeader to the bundle
        outState = navigationDrawer.getHeader().saveInstanceState(outState);
        outState.putIntegerArrayList("menu_stack", menuStack);
        outState.putInt("navigation_menu_position", selectedNavigationMenuId);
        outState.putString("connection_status", connectionStatus);
        outState.putBoolean("connection_settings_shown", connectionSettingsShown);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onMessage(final String action, final Object obj) {
        switch (action) {
            /*
            case Constants.ACTION_CONNECTION_STATE_OK:
                connectionStatus = action;
                runOnUiThread(new Runnable() {
                    public void run() {
                        navigationDrawer.updateDrawerItemBadges();
                    }
                });
                break;
            case Constants.ACTION_CONNECTION_STATE_SERVER_DOWN:
            case Constants.ACTION_CONNECTION_STATE_LOST:
            case Constants.ACTION_CONNECTION_STATE_TIMEOUT:
            case Constants.ACTION_CONNECTION_STATE_REFUSED:
            case Constants.ACTION_CONNECTION_STATE_AUTH:
            case Constants.ACTION_CONNECTION_STATE_NO_NETWORK:
            case Constants.ACTION_CONNECTION_STATE_NO_CONNECTION:

                // Go to the status screen if an error has occurred from a previously
                // working connection or no connection at all. Additionally show the
                // error message
                if (connectionStatus.equals(Constants.ACTION_CONNECTION_STATE_OK) ||
                        connectionStatus.equals(Constants.ACTION_CONNECTION_STATE_UNKNOWN)) {

                    // Show a textual description about the connection state
                    switch (connectionStatus) {
                        case Constants.ACTION_CONNECTION_STATE_SERVER_DOWN:
                            showMessage(getString(R.string.err_connect));
                            break;
                        case Constants.ACTION_CONNECTION_STATE_LOST:
                            showMessage(getString(R.string.err_con_lost));
                            break;
                        case Constants.ACTION_CONNECTION_STATE_TIMEOUT:
                            showMessage(getString(R.string.err_con_timeout));
                            break;
                        case Constants.ACTION_CONNECTION_STATE_REFUSED:
                        case Constants.ACTION_CONNECTION_STATE_AUTH:
                            showMessage(getString(R.string.err_auth));
                            break;
                        case Constants.ACTION_CONNECTION_STATE_NO_NETWORK:
                            showMessage(getString(R.string.err_no_network));
                            break;
                        case Constants.ACTION_CONNECTION_STATE_NO_CONNECTION:
                            showMessage(getString(R.string.no_connection_available));
                            break;
                    }

                    runOnUiThread(new Runnable() {
                        public void run() {
                            connectionStatus = action;
                            handleDrawerItemSelected(MENU_STATUS);
                            navigationDrawer.updateDrawerItemBadges();
                        }
                    });
                }
                break;
                */
            case Constants.ACTION_SHOW_MESSAGE:
                final String msg = (String) obj;
                showMessage(msg);
                break;
            case "dvrEntryAdd":
            case "dvrEntryUpdate":
            case "dvrEntryDelete":
            case "autorecEntryAdd":
            case "autorecEntryDelete":
            case "timerecEntryAdd":
            case "timerecEntryDelete":
                runOnUiThread(new Runnable() {
                    public void run() {
                        navigationDrawer.updateDrawerItemBadges();
                    }
                });
                break;
        }
    }

    private void showMessage(final String msg) {
        Snackbar.make(coordinatorLayout, msg, Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public void notify(String message) {
        if (getCurrentFocus() != null) {
            Snackbar.make(getCurrentFocus(), message, Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    public void onNavigationProfileSelected(IProfile profile) {
        Connection oldConn = databaseHelper.getSelectedConnection();
        Connection newConn = databaseHelper.getConnection(profile.getIdentifier());

        // Switch the active connection and reconnect
        if (oldConn != null && newConn != null) {
            newConn.selected = true;
            oldConn.selected = false;
            databaseHelper.updateConnection(oldConn);
            databaseHelper.updateConnection(newConn);
            Utils.connect(NavigationDrawerActivity.this, true);

            navigationDrawer.updateDrawerItemBadges();
        }
    }

    @Override
    public void onNavigationMenuSelected(int id) {
        if (selectedNavigationMenuId != id) {
            handleDrawerItemSelected(id);
        }
    }
}
