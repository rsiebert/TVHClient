package org.tvheadend.tvhclient.activities;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.gms.cast.ApplicationMetadata;
import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager;
import com.google.android.libraries.cast.companionlibrary.cast.callbacks.VideoCastConsumerImpl;
import com.google.android.libraries.cast.companionlibrary.widgets.IntroductoryOverlay;
import com.google.android.libraries.cast.companionlibrary.widgets.MiniController;
import com.mikepenz.materialdrawer.model.interfaces.IProfile;

import org.tvheadend.tvhclient.ChangeLogDialog;
import org.tvheadend.tvhclient.Constants;
import org.tvheadend.tvhclient.DataStorage;
import org.tvheadend.tvhclient.DatabaseHelper;
import org.tvheadend.tvhclient.Logger;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.fragments.ChannelListFragment;
import org.tvheadend.tvhclient.fragments.ProgramGuideChannelListFragment;
import org.tvheadend.tvhclient.fragments.ProgramGuideListFragment;
import org.tvheadend.tvhclient.fragments.ProgramGuidePagerFragment;
import org.tvheadend.tvhclient.fragments.ProgramListFragment;
import org.tvheadend.tvhclient.fragments.recordings.CompletedRecordingListFragment;
import org.tvheadend.tvhclient.fragments.recordings.FailedRecordingListFragment;
import org.tvheadend.tvhclient.fragments.recordings.RecordingListFragment;
import org.tvheadend.tvhclient.fragments.recordings.RemovedRecordingListFragment;
import org.tvheadend.tvhclient.fragments.recordings.ScheduledRecordingListFragment;
import org.tvheadend.tvhclient.fragments.recordings.SeriesRecordingListFragment;
import org.tvheadend.tvhclient.fragments.recordings.TimerRecordingListFragment;
import org.tvheadend.tvhclient.htsp.HTSListener;
import org.tvheadend.tvhclient.htsp.HTSService;
import org.tvheadend.tvhclient.interfaces.ChangeLogDialogInterface;
import org.tvheadend.tvhclient.interfaces.FragmentControlInterface;
import org.tvheadend.tvhclient.interfaces.FragmentScrollInterface;
import org.tvheadend.tvhclient.interfaces.FragmentStatusInterface;
import org.tvheadend.tvhclient.interfaces.ToolbarInterface;
import org.tvheadend.tvhclient.model.Channel;
import org.tvheadend.tvhclient.model.Connection;
import org.tvheadend.tvhclient.model.Profile;
import org.tvheadend.tvhclient.model.Program;
import org.tvheadend.tvhclient.model.Recording;
import org.tvheadend.tvhclient.tasks.WakeOnLanTask;
import org.tvheadend.tvhclient.tasks.WakeOnLanTaskCallback;
import org.tvheadend.tvhclient.utils.MenuUtils;
import org.tvheadend.tvhclient.utils.MiscUtils;
import org.tvheadend.tvhclient.utils.Utils;

import java.util.ArrayList;
import java.util.List;

import static org.tvheadend.tvhclient.activities.NavigationDrawer.MENU_CHANNELS;
import static org.tvheadend.tvhclient.activities.NavigationDrawer.MENU_COMPLETED_RECORDINGS;
import static org.tvheadend.tvhclient.activities.NavigationDrawer.MENU_FAILED_RECORDINGS;
import static org.tvheadend.tvhclient.activities.NavigationDrawer.MENU_INFORMATION;
import static org.tvheadend.tvhclient.activities.NavigationDrawer.MENU_PROGRAM_GUIDE;
import static org.tvheadend.tvhclient.activities.NavigationDrawer.MENU_REMOVED_RECORDINGS;
import static org.tvheadend.tvhclient.activities.NavigationDrawer.MENU_SCHEDULED_RECORDINGS;
import static org.tvheadend.tvhclient.activities.NavigationDrawer.MENU_SERIES_RECORDINGS;
import static org.tvheadend.tvhclient.activities.NavigationDrawer.MENU_SETTINGS;
import static org.tvheadend.tvhclient.activities.NavigationDrawer.MENU_STATUS;
import static org.tvheadend.tvhclient.activities.NavigationDrawer.MENU_TIMER_RECORDINGS;
import static org.tvheadend.tvhclient.activities.NavigationDrawer.MENU_UNKNOWN;
import static org.tvheadend.tvhclient.activities.NavigationDrawer.MENU_UNLOCKER;

// TODO hide unlocker menu if already unlocked
// TODO move chromecast to another class
// TODO move drawer init to another class
// TODO when in dual pane and back edit menu is visible
// TODO make nav image blasser
// TODO confirmation of added/edited recordings...

public class MainActivity extends AppCompatActivity implements SearchView.OnQueryTextListener, SearchView.OnSuggestionListener, ChangeLogDialogInterface, ToolbarInterface, ToolbarInterfaceLight, FragmentStatusInterface, FragmentScrollInterface, HTSListener, WakeOnLanTaskCallback, NavigationDrawerCallback {

    private final static String TAG = MainActivity.class.getSimpleName();

    private CoordinatorLayout coordinatorLayout;
    private ActionBar actionBar;
    private ChangeLogDialog changeLogDialog;

    // Indication weather the layout supports two fragments. This is usually
    // only available on tablets.
    private boolean isDualPane = false;

    // Default navigation drawer menu position and the list positions
    private int selectedNavigationMenuId = MENU_UNKNOWN;
    private int defaultMenuPosition = MENU_UNKNOWN;
    private int programGuideListPosition = 0;
    private int programGuideListPositionOffset = 0;

    // Holds the list of selected menu items so the previous fragment can be
    // shown again when the user has pressed the back key.
    private ArrayList<Integer> menuStack = new ArrayList<>();

    // Holds a list of channels that are currently being loaded
    private final List<Channel> channelLoadingList = new ArrayList<>();
    private Runnable channelLoadingTask;
    private final Handler channelLoadingHandler = new Handler();

    // Remember if the connection setting screen was already shown. When the
    // main activity starts for the first time and no connections are configured
    // or active the connection settings activity will be shown once.
    private boolean connectionSettingsShown = false;

    // Remember if the change log dialog was shown when the main activity has
    // started for the first time and the application version has changed.
    private boolean changeLogDialogShown = false;

    // Contains the information about the current connection state.
    private String connectionStatus = Constants.ACTION_CONNECTION_STATE_UNKNOWN;

    private TextView actionBarTitle;
    private TextView actionBarSubtitle;
    private ImageView actionBarIcon;

    // Required to start the search when the user has selected a value from the
    // search suggestion list
    private MenuItem searchMenuItem = null;
    private SearchView searchView = null;

    private TVHClientApplication app;
    private DatabaseHelper databaseHelper;

    private VideoCastManager mCastManager;
    private VideoCastConsumerImpl mCastConsumer;
    private MenuItem mMediaRouteMenuItem;
    private IntroductoryOverlay mOverlay;
    private MiniController mMiniController;
    private Logger logger;
    private DataStorage dataStorage;
    private MenuUtils menuUtils;

    private SharedPreferences sharedPreferences;
    private NavigationDrawer navigationDrawer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(MiscUtils.getThemeId(this));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_layout);
        MiscUtils.setLanguage(this);

        VideoCastManager.checkGooglePlayServices(this);

        // Check if dual pane mode shall be activated (two fragments at the same
        // time). This is usually available on tablets
        View v = findViewById(R.id.right_fragment);
        isDualPane = v != null && v.getVisibility() == View.VISIBLE;

        // Get the main toolbar and the floating action button (fab). The fab is
        // hidden as a default and only visible when required for certain actions
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        app = (TVHClientApplication) getApplication();
        logger = Logger.getInstance();
        databaseHelper = DatabaseHelper.getInstance(getApplicationContext());
        dataStorage = DataStorage.getInstance();
        changeLogDialog = new ChangeLogDialog(this);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        menuUtils = new MenuUtils(this);

        navigationDrawer = new NavigationDrawer(this, savedInstanceState, toolbar, this);
        navigationDrawer.createHeader();
        navigationDrawer.createMenu();

        actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayUseLogoEnabled(sharedPreferences.getBoolean("showIconPref", true));
        }

        coordinatorLayout = findViewById(R.id.coordinatorLayout);

        // Get the widgets so we can use them later and do not need to inflate again
        actionBarTitle = toolbar.findViewById(R.id.actionbar_title);
        actionBarSubtitle = toolbar.findViewById(R.id.actionbar_subtitle);
        actionBarIcon = toolbar.findViewById(R.id.actionbar_icon);
        actionBarIcon.setVisibility(View.GONE);

        defaultMenuPosition = Integer.parseInt(sharedPreferences.getString("defaultMenuPositionPref", String.valueOf(MENU_STATUS)));

        if (savedInstanceState == null) {
            // When the activity is created it got called by the main activity. Get the initial
            // navigation menu position and show the associated fragment with it. When the device
            // was rotated just restore the position from the saved instance.
            selectedNavigationMenuId = getIntent().getIntExtra("navigation_menu_position", MENU_CHANNELS);
            if (selectedNavigationMenuId >= 0) {
                handleDrawerItemSelected(selectedNavigationMenuId);
            }
        } else {
            // If the saved instance is not null then we return from an orientation
            // change. The drawer menu could be open, so update the recording
            // counts. Also get any saved values from the bundle.
            menuStack = savedInstanceState.getIntegerArrayList("menu_stack");
            selectedNavigationMenuId = savedInstanceState.getInt("navigation_menu_position", MENU_CHANNELS);
            connectionStatus = savedInstanceState.getString("connection_status");
            connectionSettingsShown = savedInstanceState.getBoolean("connection_settings_shown");
        }

        // Resets the loading indication and updates the action 
        // bar subtitle with the number of available channels
        channelLoadingTask = new Runnable() {
            public void run() {
                // If the program guide is shown get the number of channels from the program guide
                // pager fragment because it holds the channel fragment which in turn knows the channel count. 
                Fragment f = getSupportFragmentManager().findFragmentById(R.id.main_fragment);
                if (f != null && f instanceof ProgramGuidePagerFragment) {
                    int count = ((FragmentControlInterface) f).getItemCount();
                    String items = getResources().getQuantityString(R.plurals.items, count, count);
                    setActionBarSubtitle(items);
                }
            }
        };
        initCasting();
    }

    private void handleDrawerProfileSelected(IProfile profile) {
        Connection oldConn = databaseHelper.getSelectedConnection();
        Connection newConn = databaseHelper.getConnection(profile.getIdentifier());

        // Switch the active connection and reconnect
        if (oldConn != null && newConn != null) {
            newConn.selected = true;
            oldConn.selected = false;
            databaseHelper.updateConnection(oldConn);
            databaseHelper.updateConnection(newConn);
            Utils.connect(MainActivity.this, true);

            navigationDrawer.updateDrawerItemBadges();
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

        FrameLayout rightLayout = findViewById(R.id.right_fragment);
        if (rightLayout != null) {
            if (position == MENU_PROGRAM_GUIDE
                    || position == MENU_STATUS
                    || position == MENU_UNLOCKER) {
                rightLayout.setVisibility(View.GONE);
            } else {
                rightLayout.setVisibility(View.VISIBLE);
            }
        }

        // Remove the current fragment on the right side in case dual pane mode
        // is active. In case the connection can't be established the area on
        // the right side shall be blank, so that no invalid data is displayed.
        // The main fragment does not need to be removed, because it will be
        // replaced with a fragment from the selected menu.
        if (isDualPane) {
            Fragment rf = getSupportFragmentManager().findFragmentById(R.id.right_fragment);
            if (rf != null) {
                getSupportFragmentManager().beginTransaction().remove(rf).commit();
            }
        }

        // Remove the channel list fragment that is shown on the left side when
        // the program guide is visible. When another menu item is called the
        // reserved layout space for the channel list fragment will be
        // invisible.
        Fragment cf = getSupportFragmentManager().findFragmentById(R.id.program_guide_channel_fragment);
        if (cf != null) {
            getSupportFragmentManager().beginTransaction().remove(cf).commit();
        }

        // Save the menu position so we know which one was selected
        selectedNavigationMenuId = position;

        Bundle bundle = new Bundle();
        // Handle navigation view item clicks here.
        switch (position) {
            case MENU_CHANNELS:
                // Show the channel list fragment. If the information to show only
                // the channels as required in the program guide is not passed then
                // the full channel list fragment will be shown.
                showFragment(ChannelListFragment.class.getName(), R.id.main_fragment, bundle);
                break;
            case MENU_PROGRAM_GUIDE:
                // Show the program guide. This fragment will then trigger the
                // display of the channel list fragment on the left side.
                showFragment(ProgramGuidePagerFragment.class.getName(), R.id.main_fragment, null);
                break;
            case MENU_COMPLETED_RECORDINGS:
                showFragment(CompletedRecordingListFragment.class.getName(), R.id.main_fragment, bundle);
                break;
            case MENU_SCHEDULED_RECORDINGS:
                showFragment(ScheduledRecordingListFragment.class.getName(), R.id.main_fragment, bundle);
                break;
            case MENU_SERIES_RECORDINGS:
                showFragment(SeriesRecordingListFragment.class.getName(), R.id.main_fragment, bundle);
                break;
            case MENU_TIMER_RECORDINGS:
                showFragment(TimerRecordingListFragment.class.getName(), R.id.main_fragment, bundle);
                break;
            case MENU_FAILED_RECORDINGS:
                showFragment(FailedRecordingListFragment.class.getName(), R.id.main_fragment, bundle);
                break;
            case MENU_REMOVED_RECORDINGS:
                showFragment(RemovedRecordingListFragment.class.getName(), R.id.main_fragment, bundle);
                break;
            case MENU_STATUS:
                selectedNavigationMenuId = defaultMenuPosition;
                Intent statusIntent = new Intent(this, StatusActivity.class);
                statusIntent.putExtra("connection_status", connectionStatus);
                startActivity(statusIntent);
                break;
            case MENU_INFORMATION:
                selectedNavigationMenuId = defaultMenuPosition;
                Intent infoIntent = new Intent(this, InfoActivity.class);
                startActivity(infoIntent);
                break;
            case MENU_SETTINGS:
                // Show the settings, but do not remember the selected position
                // because it would trigger the settings fragment again
                selectedNavigationMenuId = defaultMenuPosition;
                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                startActivityForResult(settingsIntent, Constants.RESULT_CODE_SETTINGS);
                break;
            case MENU_UNLOCKER:
                selectedNavigationMenuId = defaultMenuPosition;
                Intent unlockerIntent = new Intent(this, UnlockerActivity.class);
                startActivity(unlockerIntent);
                break;
        }
    }

    private void initCasting() {
        logger.log(TAG, "initCasting() called");

        mMiniController = findViewById(R.id.miniController);
        mCastManager = VideoCastManager.getInstance();
        mCastManager.reconnectSessionIfPossible();

        mCastConsumer = new VideoCastConsumerImpl() {
            @Override
            public void onFailed(int resourceId, int statusCode) {
                String reason = "Not Available";
                if (resourceId > 0) {
                    reason = getString(resourceId);
                }
                logger.log(TAG, "onFailed() called with: reason = [" + reason + "], statusCode = [" + statusCode + "]");
            }

            @Override
            public void onApplicationConnected(ApplicationMetadata appMetadata, String sessionId, boolean wasLaunched) {
                logger.log(TAG, "onApplicationConnected() called with: appMetadata = [" + appMetadata + "], sessionId = [" + sessionId + "], wasLaunched = [" + wasLaunched + "]");
                invalidateOptionsMenu();

                Connection conn = databaseHelper.getSelectedConnection();
                Profile profile = (conn != null ? databaseHelper.getProfile(conn.cast_profile_id) : null);

                if (profile == null || !profile.enabled || profile.uuid == null || profile.uuid.length() == 0) {
                    logger.log(TAG, "onApplicationConnected: No casting profile set, disconnecting");
                    new MaterialDialog.Builder(MainActivity.this)
                            .content(R.string.cast_profile_not_set)
                            .positiveText(android.R.string.ok)
                            .callback(new MaterialDialog.ButtonCallback() {
                                @Override
                                public void onPositive(MaterialDialog dialog) {
                                    mCastManager.disconnect();
                                }
                            }).show();
                } else {
                    logger.log(TAG, "onApplicationConnected: Selected casting profile is " + profile.name);
                }
            }

            @Override
            public void onDisconnected() {
                logger.log(TAG, "onDisconnected() called");
                invalidateOptionsMenu();
            }

            @Override
            public void onConnectionSuspended(int cause) {
                logger.log(TAG, "onConnectionSuspended() called with: cause = [" + cause + "]");
                showMessage(getString(R.string.connection_temp_lost));
            }

            @Override
            public void onConnectivityRecovered() {
                logger.log(TAG, "onConnectivityRecovered() called");
                showMessage(getString(R.string.connection_recovered));
            }

            @Override
            public void onCastAvailabilityChanged(boolean castPresent) {
                logger.log(TAG, "onCastAvailabilityChanged() called with: castPresent = [" + castPresent + "]");

                if (mMediaRouteMenuItem != null && castPresent) {
                    showCastInfoOverlay();
                }
            }
        };
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void showCastInfoOverlay() {
        if (mOverlay != null) {
            mOverlay.remove();
        }
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mMediaRouteMenuItem.isVisible()) {
                    mOverlay = new IntroductoryOverlay.Builder(MainActivity.this)
                            .setMenuItem(mMediaRouteMenuItem)
                            .setTitleText(R.string.intro_overlay_text)
                            .setSingleTime()
                            .setOnDismissed(new IntroductoryOverlay.OnOverlayDismissedListener() {
                                @Override
                                public void onOverlayDismissed() {
                                    mOverlay = null;
                                }
                            })
                            .build();
                    mOverlay.show();
                }
            }
        }, 1000);
    }

    @Override
    public boolean dispatchKeyEvent(@NonNull KeyEvent event) {
        return mCastManager.onDispatchVolumeKeyEvent(event, Constants.CAST_VOLUME_INCREMENT)
                || super.dispatchKeyEvent(event);
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

        mCastManager = VideoCastManager.getInstance();
        if (null != mCastManager) {
            mCastManager.addVideoCastConsumer(mCastConsumer);
            mCastManager.incrementUiCounter();
            if (!sharedPreferences.getBoolean("pref_show_cast_minicontroller", false)) {
                mCastManager.removeMiniController(mMiniController);
            }
        }

        connectionStatus = sharedPreferences.getString("last_connection_state", Constants.ACTION_CONNECTION_STATE_OK);
        connectionSettingsShown = sharedPreferences.getBoolean("last_connection_settings_shown", false);

        // Update the drawer menu so that all available menu items are
        // shown in case the recording counts have changed or the user has
        // bought the unlocked version to enable all features
        navigationDrawer.updateDrawerItemBadges();
        navigationDrawer.updateDrawerHeader();
    }


    @Override
    public void onPostResume() {
        super.onPostResume();
        // Show the change log once when the application was upgraded
        if (changeLogDialog.firstRun() && !changeLogDialogShown) {
            changeLogDialogShown = true;
            changeLogDialog.getLogDialog().show();
        } else {
            changeLogDialogDismissed();
        }
    }

    /**
     * Determines what shall be done when the change log dialog has been closed
     * or the the application has been resumed. If no connection is available or
     * not active, go to the settings screen screen once. If this is still the
     * case after the user has left the setting screen or the connection state
     * is not fine, check if the Internet access is available, if not go to the
     * status screen. Otherwise show the defined menu.
     */
    private void reconnectAndResume() {
        if (!connectionSettingsShown
                && (databaseHelper.getConnections().isEmpty()
                || databaseHelper.getSelectedConnection() == null)) {
            connectionSettingsShown = true;

            Intent connIntent = new Intent(this, SettingsActivity.class);
            connIntent.putExtra(Constants.BUNDLE_MANAGE_CONNECTIONS, true);
            startActivityForResult(connIntent, Constants.RESULT_CODE_SETTINGS);

        } else {
            if (!app.isConnected()) {
                connectionStatus = Constants.ACTION_CONNECTION_STATE_NO_NETWORK;
                handleDrawerItemSelected(MENU_STATUS);
            } else {
                // Show the contents of the last selected menu position. In case it
                // is not set, use the the default one defined in the settings
                int pos = (selectedNavigationMenuId == MENU_UNKNOWN) ? defaultMenuPosition : selectedNavigationMenuId;

                // Set the connection state to unknown if no connection was added
                // when the connection fragment was shown. The status fragment is
                // then shown with the information that no connection is available
                if (databaseHelper.getConnections().isEmpty()
                        || databaseHelper.getSelectedConnection() == null) {
                    connectionStatus = Constants.ACTION_CONNECTION_STATE_NO_CONNECTION;
                    handleDrawerItemSelected(MENU_STATUS);
                } else {
                    // A connection exists and is active, register to receive
                    // information from the server and connect to the server
                    app.addListener(this);
                    Utils.connect(this, false);

                    // Show the defined fragment from the menu position or the
                    // status if the connection state is not fine
                    handleDrawerItemSelected((connectionStatus
                            .equals(Constants.ACTION_CONNECTION_STATE_OK)) ? pos
                            : MENU_STATUS);
                }
            }
        }
    }

    @Override
    public void onPause() {
        mCastManager.decrementUiCounter();
        mCastManager.removeVideoCastConsumer(mCastConsumer);

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
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (mMediaRouteMenuItem != null) {
            mMediaRouteMenuItem.setVisible(showCastMenuItem());
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main_menu, menu);

        if (showCastMenuItem()) {
            mMediaRouteMenuItem = mCastManager.addMediaRouterButton(menu, R.id.media_route_menu_item);
        }

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchMenuItem = menu.findItem(R.id.menu_search);
        searchView = (SearchView) searchMenuItem.getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setIconifiedByDefault(true);
        searchView.setOnQueryTextListener(this);
        searchView.setOnSuggestionListener(this);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle your other action bar items...
        switch (item.getItemId()) {
            case R.id.menu_refresh:
                // Clear all available data and all channels that are currently
                // loading, reconnect to the server and reload all data
                channelLoadingList.clear();
                menuUtils.handleMenuReconnectSelection();
                return true;

            case R.id.menu_wol:
                final Connection conn = databaseHelper.getSelectedConnection();
                if (conn != null) {
                    WakeOnLanTask task = new WakeOnLanTask(this, this, conn);
                    task.execute();
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Creates the fragment with the given name and shows it on the given
     * layout. If a bundle was given, it will be passed to the fragment. If
     * required the created fragment will be stored on the back stack so it can
     * be shown later when the user has pressed the back button.
     *
     * @param name   Class name of the fragment
     * @param layout Layout that shall be used
     */
    private void showFragment(String name, int layout, Bundle args) {
        Fragment f = Fragment.instantiate(this, name);
        if (args != null) {
            f.setArguments(args);
        }
        getSupportFragmentManager().beginTransaction().replace(layout, f)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .commit();
    }

    @Override
    public void changeLogDialogDismissed() {
        reconnectAndResume();
    }

    @Override
    public void onMessage(final String action, final Object obj) {
        switch (action) {
            case Constants.ACTION_LOADING:
                final Context ctx = this;
                runOnUiThread(new Runnable() {
                    public void run() {
                        boolean loading = (Boolean) obj;
                        if (loading) {
                            setActionBarTitle(getString(R.string.loading));
                            setActionBarSubtitle("");
                            // When in dual pane mode remove the fragment on the
                            // right to avoid seeing invalid data while the
                            // application is loading data from the server.
                            if (isDualPane) {
                                Fragment rf = getSupportFragmentManager().findFragmentById(R.id.right_fragment);
                                if (rf != null) {
                                    getSupportFragmentManager().beginTransaction().remove(rf).commit();
                                }
                            }
                        } else {
                            // Load the available recording profiles so that they are
                            // available in case the user wants to add a manual recording
                            if (app.isUnlocked()) {
                                Intent intent = new Intent(ctx, HTSService.class);
                                intent.setAction("getDvrConfigs");
                                startService(intent);
                            }
                            // Reload the menu. Only after the initial sync we know the server
                            // version which determines the visibility of the casting icon
                            invalidateOptionsMenu();
                        }
                    }
                });
                break;
            case Constants.ACTION_CONNECTION_STATE_OK:
                connectionStatus = action;
                runOnUiThread(new Runnable() {
                    public void run() {
                        navigationDrawer.updateDrawerItemBadges();
                    }
                });
            case "channelUpdate":
                runOnUiThread(new Runnable() {
                    public void run() {
                        final Channel ch = (Channel) obj;

                        // The channel has been updated (usually by a call to load
                        // more data) so remove it from the loading queue and
                        // continue with the next one.
                        channelLoadingList.remove(ch);

                        // Reset the loading indication either instantly or with a
                        // delay if there is still a channel in the loading queue
                        channelLoadingHandler.removeCallbacks(channelLoadingTask);
                        channelLoadingHandler.postDelayed(channelLoadingTask,
                                (channelLoadingList.isEmpty() ? 10 : 2000));
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
            // If no subtitle string is given hide it from the view and center
            // the title vertically, otherwise place it below the title
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
    public void setActionBarIcon(final Bitmap bitmap) {
        if (actionBarIcon != null && bitmap != null) {
            // Only show the channel tag icon in the channel and program guide
            // screens. In all other screens hide it because it makes no sense
            // to show the tag icon. For example the completed recordings could
            // be from channels from different channel tags.
            if (selectedNavigationMenuId == MENU_CHANNELS || selectedNavigationMenuId == MENU_PROGRAM_GUIDE) {
                actionBarIcon.setVisibility(sharedPreferences.getBoolean("showTagIconPref", false) ? View.VISIBLE : View.GONE);
                actionBarIcon.setBackgroundDrawable(new BitmapDrawable(getResources(), bitmap));
            } else {
                actionBarIcon.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void setActionBarIcon(final int resource) {
        if (actionBarIcon != null) {
            // Only show the channel tag icon in the channel and program guide
            // screens. In all other screens hide it because it makes no sense
            // to show the tag icon. For example the completed recordings could
            // be from channels from different channel tags.
            if (selectedNavigationMenuId == MENU_CHANNELS || selectedNavigationMenuId == MENU_PROGRAM_GUIDE) {
                actionBarIcon.setVisibility(sharedPreferences.getBoolean("showTagIconPref", false) ? View.VISIBLE : View.GONE);
                actionBarIcon.setBackgroundResource(resource);
            } else {
                actionBarIcon.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onScrollingChanged(final int position, final int offset, final String tag) {
        switch (selectedNavigationMenuId) {
            case MENU_PROGRAM_GUIDE:
                // Save the scroll values so they can be reused after an orientation change.
                programGuideListPosition = position;
                programGuideListPositionOffset = offset;

                if (tag.equals(ProgramGuideChannelListFragment.class.getSimpleName())
                        || tag.equals(ProgramGuideListFragment.class.getSimpleName())) {
                    // Scrolling was initiated by the channel or program guide list fragment. Keep
                    // the currently visible program guide list in sync by scrolling it to the same position
                    final Fragment f = getSupportFragmentManager().findFragmentById(R.id.main_fragment);
                    if (f instanceof FragmentControlInterface) {
                        ((FragmentControlInterface) f).setSelection(position, offset);
                    }
                }
                break;
        }
    }

    @Override
    public void onScrollStateIdle(final String tag) {
        switch (selectedNavigationMenuId) {
            case MENU_PROGRAM_GUIDE:
                if (tag.equals(ProgramGuideListFragment.class.getSimpleName())
                        || tag.equals(ProgramGuideChannelListFragment.class.getSimpleName())) {
                    // Scrolling stopped by the program guide or the channel list
                    // fragment. Scroll all program guide fragments in the current
                    // view pager to the same position.
                    final Fragment f = getSupportFragmentManager().findFragmentById(R.id.main_fragment);
                    if (f instanceof ProgramGuidePagerFragment) {
                        ((FragmentControlInterface) f).setSelection(
                                programGuideListPosition,
                                programGuideListPositionOffset);
                    }
                }
                break;
        }
    }

    @Override
    public void moreDataRequired(final Channel channel, final String tag) {
        Log.d(TAG, "moreDataRequired() called with: channel = [" + channel + "], tag = [" + tag + "]");
        if (dataStorage.isLoading() || channel == null) {
            return;
        }

        channelLoadingList.add(channel);
        setActionBarSubtitle(getString(R.string.loading));
        Utils.loadMorePrograms(this, channel);
    }

    @Override
    public void onListItemSelected(final int position, final Program program, final String tag) {
        // When a program has been selected from the program list fragment,
        // show its details. In single or dual pane mode these are shown in a
        // separate dialog fragment
        if (program != null) {
            Intent intent = new Intent(this, DetailsActivity.class);
            intent.putExtra("eventId", program.eventId);
            intent.putExtra("channelId", program.channelId);
            intent.putExtra("type", "program");
            startActivity(intent);
        }
    }

    @Override
    public void onListPopulated(final String tag) {
        switch (selectedNavigationMenuId) {
            case MENU_PROGRAM_GUIDE:
                // When the program guide is done loading set the previously
                // selected position of the program guide. The program guide
                // fragment will inform us via the scrolling interface methods where
                // the channel list shall be scrolled to.
                final Fragment f = getSupportFragmentManager().findFragmentById(R.id.main_fragment);
                if (f instanceof ProgramGuidePagerFragment) {
                    ((FragmentControlInterface) f).setSelection(programGuideListPosition,
                            programGuideListPositionOffset);
                }
                break;
        }
    }

    @Override
    public void channelTagChanged(final String tag) {
        switch (selectedNavigationMenuId) {
            case MENU_PROGRAM_GUIDE:
                // Inform the channel list fragment to clear all data from its
                // channel list and show only the channels with the selected tag
                final Fragment cf = getSupportFragmentManager().findFragmentById(R.id.program_guide_channel_fragment);
                if (cf instanceof ProgramGuideChannelListFragment) {
                    ((FragmentControlInterface) cf).reloadData();
                }
                // Additionally inform the program guide fragment to clear all data
                // from its list and show only the programs of the channels that are
                // part of the selected tag
                final Fragment pgf = getSupportFragmentManager().findFragmentById(R.id.main_fragment);
                if (pgf instanceof ProgramGuidePagerFragment) {
                    ((FragmentControlInterface) pgf).reloadData();
                }
                break;
        }
    }

    @Override
    public boolean onQueryTextChange(String text) {
        return false;
    }

    @SuppressLint("NewApi")
    @Override
    public boolean onQueryTextSubmit(String query) {
        searchMenuItem.collapseActionView();

        // Create the intent that will be passed to the search activity with
        // the query and optionally some additional data.
        Intent searchIntent = new Intent(getApplicationContext(), SearchResultActivity.class);
        searchIntent.putExtra(SearchManager.QUERY, query);
        searchIntent.setAction(Intent.ACTION_SEARCH);

        Bundle bundle = new Bundle();
        final Fragment f = getSupportFragmentManager().findFragmentById(R.id.main_fragment);

        // Pass on the channel id to search only in the selected channel.
        // This will only be the case if no dual pane is active. Otherwise a
        // full channel search would only be possible in the EPG view.
        if (!isDualPane) {
            if (f instanceof ProgramListFragment) {
                Object o = ((FragmentControlInterface) f).getSelectedItem();
                if (o instanceof Channel) {
                    final Channel ch = (Channel) o;
                    bundle.putInt("channelId", ch.channelId);
                }
            }
        }

        // Pass on the recording id if the completed recording screen is
        // visible. The onPrepareOptionsMenu ensures that the search icon is
        // visible in the required screens.
        if (f instanceof RecordingListFragment) {
            Object o = ((FragmentControlInterface) f).getSelectedItem();
            if (o instanceof Recording) {
                final Recording rec = (Recording) o;
                bundle.putLong("dvrId", rec.id);
            }
        }

        searchIntent.putExtra(SearchManager.APP_DATA, bundle);
        startActivity(searchIntent);
        return true;
    }

    @Override
    public boolean onSuggestionClick(int position) {
        searchMenuItem.collapseActionView();
        // Set the search query and return true so that the
        // onQueryTextSubmit is called. This is required to pass additional
        // data to the search activity
        Cursor cursor = (Cursor) searchView.getSuggestionsAdapter().getItem(position);
        String suggestion = cursor.getString(cursor.getColumnIndex(SearchManager.SUGGEST_COLUMN_TEXT_1));
        searchView.setQuery(suggestion, true);
        return true;
    }

    @Override
    public boolean onSuggestionSelect(int position) {
        return false;
    }

    /**
     * Returns true so that the cast menu button can be shown only if casting is
     * enabled and valid profile exists and a valid screen is visible. The check
     * for the profile is required because it could be null in case a new
     * connection was defined and no cast profile was selected yet.
     *
     * @return True if the cast icon shall be show
     */
    private boolean showCastMenuItem() {
        Connection conn = databaseHelper.getSelectedConnection();
        Profile profile = (conn != null ? databaseHelper.getProfile(conn.cast_profile_id) : null);
        return (app.isUnlocked()
                && sharedPreferences.getBoolean("pref_enable_casting", false)
                && profile != null
                && profile.enabled);
    }

    @Override
    public void notify(String message) {
        if (getCurrentFocus() != null) {
            Snackbar.make(getCurrentFocus(), message, Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    public void setTitle(String title) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
        }
    }

    @Override
    public void setSubtitle(String subtitle) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setSubtitle(subtitle);
        }
    }

    @Override
    public void onNavigationProfileSelected(IProfile profile) {
        handleDrawerProfileSelected(profile);
    }

    @Override
    public void onNavigationMenuSelected(int id) {
        if (selectedNavigationMenuId != id) {
            handleDrawerItemSelected(id);
        }
    }
}
