package org.tvheadend.tvhclient.activities;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
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
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.gms.cast.ApplicationMetadata;
import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager;
import com.google.android.libraries.cast.companionlibrary.cast.callbacks.VideoCastConsumerImpl;
import com.google.android.libraries.cast.companionlibrary.widgets.IntroductoryOverlay;
import com.google.android.libraries.cast.companionlibrary.widgets.MiniController;

import org.tvheadend.tvhclient.ChangeLogDialog;
import org.tvheadend.tvhclient.Constants;
import org.tvheadend.tvhclient.DataStorage;
import org.tvheadend.tvhclient.DatabaseHelper;
import org.tvheadend.tvhclient.Logger;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.adapter.DrawerMenuAdapter;
import org.tvheadend.tvhclient.fragments.ChannelListFragment;
import org.tvheadend.tvhclient.fragments.ProgramDetailsFragment;
import org.tvheadend.tvhclient.fragments.ProgramGuideListFragment;
import org.tvheadend.tvhclient.fragments.ProgramGuidePagerFragment;
import org.tvheadend.tvhclient.fragments.ProgramListFragment;
import org.tvheadend.tvhclient.fragments.StatusFragment;
import org.tvheadend.tvhclient.fragments.recordings.CompletedRecordingListFragment;
import org.tvheadend.tvhclient.fragments.recordings.FailedRecordingListFragment;
import org.tvheadend.tvhclient.fragments.recordings.RecordingDetailsFragment;
import org.tvheadend.tvhclient.fragments.recordings.RecordingListFragment;
import org.tvheadend.tvhclient.fragments.recordings.RemovedRecordingListFragment;
import org.tvheadend.tvhclient.fragments.recordings.ScheduledRecordingListFragment;
import org.tvheadend.tvhclient.fragments.recordings.SeriesRecordingDetailsFragment;
import org.tvheadend.tvhclient.fragments.recordings.SeriesRecordingListFragment;
import org.tvheadend.tvhclient.fragments.recordings.TimerRecordingDetailsFragment;
import org.tvheadend.tvhclient.fragments.recordings.TimerRecordingListFragment;
import org.tvheadend.tvhclient.htsp.HTSService;
import org.tvheadend.tvhclient.interfaces.ActionBarInterface;
import org.tvheadend.tvhclient.interfaces.ChangeLogDialogInterface;
import org.tvheadend.tvhclient.interfaces.FragmentControlInterface;
import org.tvheadend.tvhclient.interfaces.FragmentScrollInterface;
import org.tvheadend.tvhclient.interfaces.FragmentStatusInterface;
import org.tvheadend.tvhclient.interfaces.HTSListener;
import org.tvheadend.tvhclient.model.Channel;
import org.tvheadend.tvhclient.model.Connection;
import org.tvheadend.tvhclient.model.DrawerMenuItem;
import org.tvheadend.tvhclient.model.Profile;
import org.tvheadend.tvhclient.model.Program;
import org.tvheadend.tvhclient.model.Recording;
import org.tvheadend.tvhclient.model.SeriesRecording;
import org.tvheadend.tvhclient.model.TimerRecording;
import org.tvheadend.tvhclient.tasks.WakeOnLanTask;
import org.tvheadend.tvhclient.tasks.WakeOnLanTaskCallback;
import org.tvheadend.tvhclient.utils.MenuUtils;
import org.tvheadend.tvhclient.utils.MiscUtils;
import org.tvheadend.tvhclient.utils.Utils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SearchView.OnQueryTextListener, SearchView.OnSuggestionListener, ChangeLogDialogInterface, ActionBarInterface, FragmentStatusInterface, FragmentScrollInterface, HTSListener, WakeOnLanTaskCallback {

    private final static String TAG = MainActivity.class.getSimpleName();

    private ListView drawerList;
    private DrawerLayout drawerLayout;
    private CoordinatorLayout coordinatorLayout;
    private DrawerMenuAdapter drawerAdapter;
    private ActionBarDrawerToggle drawerToggle;

    private ActionBar actionBar = null;
    private ChangeLogDialog changeLogDialog;

    // Indication weather the layout supports two fragments. This is usually
    // only available on tablets.
    private boolean isDualPane = false;

    // Default navigation drawer menu position and the list positions
    private int selectedMenuPosition = MENU_UNKNOWN;
    private int defaultMenuPosition = MENU_UNKNOWN;
    private int channelListPosition = 0;
    private int completedRecordingListPosition = 0;
    private int scheduledRecordingListPosition = 0;
    private int seriesRecordingListPosition = 0;
    private int timerRecordingListPosition = 0;
    private int failedRecordingListPosition = 0;
    private int removedRecordingListPosition = 0;
    private int programGuideListPosition = 0;
    private int programGuideListPositionOffset = 0;

    // The index for the navigation drawer menus
    private static final int MENU_UNKNOWN = -1;
    private static final int MENU_CHANNELS = 0;
    private static final int MENU_COMPLETED_RECORDINGS = 1;
    private static final int MENU_SCHEDULED_RECORDINGS = 2;
    private static final int MENU_SERIES_RECORDINGS = 3;
    private static final int MENU_TIMER_RECORDINGS = 4;
    private static final int MENU_FAILED_RECORDINGS = 5;
    private static final int MENU_REMOVED_RECORDINGS = 6;
    private static final int MENU_PROGRAM_GUIDE = 7;
    private static final int MENU_STATUS = 8;
    private static final int MENU_SETTINGS = 9;
    private static final int MENU_INFORMATION = 10;
    private static final int MENU_UNLOCKER = 11;

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

    // The list of available menu items in the navigation drawer
    private String[] menuItems;

    private TextView actionBarTitle;
    private TextView actionBarSubtitle;
    private ImageView actionBarIcon;

    // Required to start the search when the user has selected a value from the
    // search suggestion list
    private MenuItem searchMenuItem = null;
    private SearchView searchView = null;

    private TVHClientApplication app;
    private DatabaseHelper dbh;

    private int channelTimeSelection = 0;
    private long showProgramsFromTime = new Date().getTime();

    private VideoCastManager mCastManager;
    private VideoCastConsumerImpl mCastConsumer;
    private MenuItem mMediaRouteMenuItem;
    private IntroductoryOverlay mOverlay;
    private MiniController mMiniController;
    private Logger logger;
    private DataStorage ds;
    private MenuUtils mMenuUtils;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(MiscUtils.getThemeId(this));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_layout);
        Utils.setLanguage(this);

        VideoCastManager.checkGooglePlayServices(this);

        // Check if dual pane mode shall be activated (two fragments at the same
        // time). This is usually available on tablets
        View v = findViewById(R.id.right_fragment);
        isDualPane = v != null && v.getVisibility() == View.VISIBLE;

        logger = Logger.getInstance();
        dbh = DatabaseHelper.getInstance(this);
        ds = DataStorage.getInstance();
        changeLogDialog = new ChangeLogDialog(this);

        mMenuUtils = new MenuUtils(this);

        actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
            actionBar.setCustomView(R.layout.actionbar_title);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayUseLogoEnabled(Utils.showChannelIcons(this));
        }

        coordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinatorLayout);

        // Get the widgets so we can use them later and do not need to inflate again
        actionBarTitle = (TextView) actionBar.getCustomView().findViewById(R.id.actionbar_title);
        actionBarSubtitle = (TextView) actionBar.getCustomView().findViewById(R.id.actionbar_subtitle);
        actionBarIcon = (ImageView) actionBar.getCustomView().findViewById(R.id.actionbar_icon);
        actionBarIcon.setVisibility(View.GONE);

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerList = (ListView) findViewById(R.id.left_drawer);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        defaultMenuPosition = Integer.parseInt(prefs.getString("defaultMenuPositionPref", String.valueOf(MENU_STATUS)));

        menuItems = getResources().getStringArray(R.array.pref_menu_names);

        // The drawer does not support setting the background automatically from
        // the defined theme. This needs to be done manually. Set the correct
        // background depending on the used theme.
        final boolean lightTheme = prefs.getBoolean("lightThemePref", true);
        drawerList.setBackgroundColor((lightTheme) ?
                getResources().getColor(R.color.drawer_background_light) :
                getResources().getColor(R.color.drawer_background_dark));

        drawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

        // Listens for drawer open and close events so we can modify the
        // contents of the action bar when the drawer is visible, such as to
        // change the title and remove action items that are contextual to the
        // main content.
        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout,
                R.string.drawer_open,
                R.string.drawer_close) {

            @Override
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                supportInvalidateOptionsMenu();
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                supportInvalidateOptionsMenu();
                updateDrawerMenu();
            }
        };
        // Set the drawer toggle as the DrawerListener
        drawerLayout.setDrawerListener(drawerToggle);

        // Add a header view to the drawer menu. 
        LayoutInflater inflater = getLayoutInflater();
        View header = inflater.inflate(R.layout.drawer_list_header, drawerList, false);
        drawerList.addHeaderView(header, null, false);

        // Create the custom adapter for the menus in the navigation drawer.
        // Also set the listener to react to the user selection.
        drawerAdapter = new DrawerMenuAdapter(this, getDrawerMenu());
        drawerList.setAdapter(drawerAdapter);
        drawerList.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Decrease the position by one before getting the item. This is
                // required because the first item in the drawer list is the
                // header view. We don't want this.
                final DrawerMenuItem item = drawerAdapter.getItem(--position);
                // We can't just use the list position for the menu position
                // because the list might contain separators. So we need to get
                // the id if the list item which is the menu position. 
                if (item != null && (item.id != MENU_UNKNOWN && selectedMenuPosition != item.id)) {
                    menuStack.add(selectedMenuPosition);
                    handleMenuSelection(item.id);
                }
                drawerLayout.closeDrawer(drawerList);
            }
        });

        app = (TVHClientApplication) getApplication();

        // If the saved instance is not null then we return from an orientation
        // change. The drawer menu could be open, so update the recording
        // counts. Also get any saved values from the bundle.
        if (savedInstanceState != null) {
            menuStack = savedInstanceState.getIntegerArrayList(Constants.MENU_STACK);
            selectedMenuPosition = savedInstanceState.getInt(Constants.MENU_POSITION, MENU_UNKNOWN);
            channelListPosition = savedInstanceState.getInt(Constants.CHANNEL_LIST_POSITION, 0);
            completedRecordingListPosition = savedInstanceState.getInt(Constants.COMPLETED_RECORDING_LIST_POSITION, 0);
            scheduledRecordingListPosition = savedInstanceState.getInt(Constants.SCHEDULED_RECORDING_LIST_POSITION, 0);
            seriesRecordingListPosition = savedInstanceState.getInt(Constants.SERIES_RECORDING_LIST_POSITION, 0);
            timerRecordingListPosition = savedInstanceState.getInt(Constants.TIMER_RECORDING_LIST_POSITION, 0);
            failedRecordingListPosition = savedInstanceState.getInt(Constants.FAILED_RECORDING_LIST_POSITION, 0);
            removedRecordingListPosition = savedInstanceState.getInt(Constants.REMOVED_RECORDING_LIST_POSITION, 0);
            connectionStatus = savedInstanceState.getString(Constants.BUNDLE_CONNECTION_STATUS);
            connectionSettingsShown = savedInstanceState.getBoolean(Constants.BUNDLE_CONNECTION_SETTINGS_SHOWN);
            channelTimeSelection = savedInstanceState.getInt(Constants.BUNDLE_CHANNEL_TIME_SELECTION);
            showProgramsFromTime = savedInstanceState.getLong(Constants.BUNDLE_SHOW_PROGRAMS_FROM_TIME);
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

    private void initCasting() {
        logger.log(TAG, "initCasting() called");

        mMiniController = (MiniController) findViewById(R.id.miniController);
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

                Connection conn = dbh.getSelectedConnection();
                Profile profile = (conn != null ? dbh.getProfile(conn.cast_profile_id) : null);

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
     * 
     */
    private void showConnectionSelectionDialog() {
        // Create a list of available connections names that the
        // selection dialog can display. Additionally preselect the
        // current active connection.
        final List<Connection> connList = dbh.getConnections();
        if (connList != null) {
            int currentConnectionListPosition = -1;
            Connection currentConnection = dbh.getSelectedConnection();
            String[] items = new String[connList.size()];
            for (int i = 0; i < connList.size(); i++) {
                items[i] = connList.get(i).name;
                if (currentConnection != null && currentConnection.id == connList.get(i).id) {
                    currentConnectionListPosition = i;
                }
            }
    
            // Now show the dialog to select a new connection
            new MaterialDialog.Builder(this)
                .title(R.string.select_connection)
                .items(items)
                .itemsCallbackSingleChoice(currentConnectionListPosition, new MaterialDialog.ListCallbackSingleChoice() {
                    @Override
                    public boolean onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                        Connection oldConn = dbh.getSelectedConnection();
                        Connection newConn = connList.get(which);
    
                        // Switch the active connection and reconnect  
                        // only if a new connection has been selected
                        if (oldConn != null && newConn != null && oldConn.id != newConn.id) {
                            // Close the menu when a new connection has been selected
                            drawerLayout.closeDrawers();
                            // Set the new connection as the active one
                            newConn.selected = true;
                            oldConn.selected = false;
                            dbh.updateConnection(oldConn);
                            dbh.updateConnection(newConn);
                            Utils.connect(MainActivity.this, true);
                        }
                        return true;
                    }
                })
                .show();
        }
    }

    /**
     * Shows or hides the allowed main menu entries depending on the app status
     * and server capabilities. Also updates the server connection status and
     * the recording counts in the main menu.
     */
    private void updateDrawerMenu() {

        TextView serverName = (TextView) drawerList.findViewById(R.id.server_name);
        ImageView serverSelection = (ImageView) drawerList.findViewById(R.id.server_selection);

        // Update the server connection status
        if (serverName != null && serverSelection != null) {

            // Remove any previous listeners
            serverName.setOnClickListener(null);
            serverSelection.setOnClickListener(null);

            final Connection conn = dbh.getSelectedConnection();
            if (dbh.getConnections().isEmpty()) {
                serverName.setText(R.string.no_connection_available);
                serverSelection.setVisibility(View.GONE);
            } else if (dbh.getConnections().size() == 1) {
                serverName.setText(conn != null ? conn.name : "");
                serverSelection.setVisibility(View.GONE);
            } else if (conn == null) {
                serverName.setText(R.string.no_connection_active);
                serverSelection.setVisibility(View.GONE);
            } else {
                serverName.setText(conn.name);
                serverSelection.setVisibility(View.VISIBLE);

                // Add a listener to the server name to allow changing the current
                // connection. A drop down menu with all connections will be displayed.
                serverSelection.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        showConnectionSelectionDialog();
                    }
                });

                // Also add a listener to the server name, not only to the selection icon
                serverName.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        showConnectionSelectionDialog();
                    }
                });
            }
        }

        // Show or hide certain menu entries
        showDrawerMenu();

        // Update the number of recordings in each category
        drawerAdapter.getItemById(MENU_COMPLETED_RECORDINGS).count = 
                ds.getRecordingsByType(Constants.RECORDING_TYPE_COMPLETED).size();
        drawerAdapter.getItemById(MENU_SCHEDULED_RECORDINGS).count = 
                ds.getRecordingsByType(Constants.RECORDING_TYPE_SCHEDULED).size();
        drawerAdapter.getItemById(MENU_SERIES_RECORDINGS).count = 
                ds.getSeriesRecordings().size();
        drawerAdapter.getItemById(MENU_TIMER_RECORDINGS).count =
                ds.getTimerRecordings().size();
        drawerAdapter.getItemById(MENU_FAILED_RECORDINGS).count = 
                ds.getRecordingsByType(Constants.RECORDING_TYPE_FAILED).size();
        drawerAdapter.getItemById(MENU_REMOVED_RECORDINGS).count =
                ds.getRecordingsByType(Constants.RECORDING_TYPE_REMOVED).size();
        drawerAdapter.notifyDataSetChanged();
    }

    /**
     * Creates the list of available main menu items. The menu name and a light
     * or dark icon depending on the selected theme is set. Additionally
     * separators between a group of menu items is also specified.
     * 
     * @return List of main menu items
     */
    private List<DrawerMenuItem> getDrawerMenu() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        final boolean lightTheme = prefs.getBoolean("lightThemePref", true);

        List<DrawerMenuItem> list = new ArrayList<>();
        list.add(new DrawerMenuItem());
        list.add(new DrawerMenuItem(MENU_CHANNELS, menuItems[0],
                (lightTheme) ? R.drawable.ic_menu_channels_light : R.drawable.ic_menu_channels_dark));
        list.add(new DrawerMenuItem(MENU_COMPLETED_RECORDINGS, menuItems[1],
                (lightTheme) ? R.drawable.ic_menu_completed_recordings_light
                        : R.drawable.ic_menu_completed_recordings_dark));
        list.add(new DrawerMenuItem(MENU_SCHEDULED_RECORDINGS, menuItems[2],
                (lightTheme) ? R.drawable.ic_menu_scheduled_recordings_light
                        : R.drawable.ic_menu_scheduled_recordings_dark));
        list.add(new DrawerMenuItem(MENU_SERIES_RECORDINGS, menuItems[3],
                (lightTheme) ? R.drawable.ic_menu_scheduled_recordings_light
                        : R.drawable.ic_menu_scheduled_recordings_dark));
        list.add(new DrawerMenuItem(MENU_TIMER_RECORDINGS, menuItems[4],
                (lightTheme) ? R.drawable.ic_menu_scheduled_recordings_light
                        : R.drawable.ic_menu_scheduled_recordings_dark));
        list.add(new DrawerMenuItem(MENU_FAILED_RECORDINGS, menuItems[5],
                (lightTheme) ? R.drawable.ic_menu_failed_recordings_light
                        : R.drawable.ic_menu_failed_recordings_dark));
        list.add(new DrawerMenuItem(MENU_REMOVED_RECORDINGS, menuItems[6],
                (lightTheme) ? R.drawable.ic_menu_failed_recordings_light
                        : R.drawable.ic_menu_failed_recordings_dark));
        list.add(new DrawerMenuItem(MENU_PROGRAM_GUIDE, menuItems[7],
                (lightTheme) ? R.drawable.ic_menu_program_guide_light
                        : R.drawable.ic_menu_program_guide_dark));
        list.add(new DrawerMenuItem(MENU_STATUS, menuItems[8],
                (lightTheme) ? R.drawable.ic_menu_status_light : R.drawable.ic_menu_status_dark));

        list.add(new DrawerMenuItem());
        list.add(new DrawerMenuItem(MENU_SETTINGS, menuItems[9],
                (lightTheme) ? R.drawable.ic_menu_settings_light : R.drawable.ic_menu_settings_dark));
        list.add(new DrawerMenuItem(MENU_INFORMATION, menuItems[11],
                (lightTheme) ? R.drawable.ic_menu_info_light
                        : R.drawable.ic_menu_info_dark));

        list.add(new DrawerMenuItem());
        list.add(new DrawerMenuItem(MENU_UNLOCKER, menuItems[12],
                (lightTheme) ? R.drawable.item_active_light
                        : R.drawable.item_active_dark));
        return list;
    }

    /**
     * Check if the current fragment is a program list fragment. In case single
     * mode is active we need to return to the channel list fragment otherwise
     * show the fragment that belongs to the previously selected menu item.
     */
    @Override
    public void onBackPressed() {
        drawerLayout.closeDrawers();
        final Fragment f = getSupportFragmentManager().findFragmentById(R.id.main_fragment);
        if (!isDualPane && (f instanceof ProgramListFragment)) {
            getSupportFragmentManager().popBackStack();
        } else {
            if (menuStack.size() > 0) {
                handleMenuSelection(menuStack.remove(menuStack.size() - 1));
            } else {
                super.onBackPressed();
                finish();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        mCastManager = VideoCastManager.getInstance();
        if (null != mCastManager) {
            mCastManager.addVideoCastConsumer(mCastConsumer);
            mCastManager.incrementUiCounter();
            if (!prefs.getBoolean("pref_show_cast_minicontroller", false)) {
                mCastManager.removeMiniController(mMiniController);
            }
        }

        connectionStatus = prefs.getString(Constants.LAST_CONNECTION_STATE, Constants.ACTION_CONNECTION_STATE_OK);
        connectionSettingsShown = prefs.getBoolean(Constants.LAST_CONNECTION_SETTINGS_SHOWN, false);

        // Update the time so that the correct programs are shown
        showProgramsFromTime = new Date().getTime();

        // Update the drawer menu so that all available menu items are
        // shown in case the recording counts have changed or the user has
        // bought the unlocked version to enable all features
        updateDrawerMenu();
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
                && (dbh.getConnections().isEmpty() 
                        || dbh.getSelectedConnection() == null)) {
            connectionSettingsShown = true;

            Intent connIntent = new Intent(this, SettingsActivity.class);
            connIntent.putExtra(Constants.BUNDLE_MANAGE_CONNECTIONS, true);
            startActivityForResult(connIntent, Constants.RESULT_CODE_SETTINGS);

        } else {
            if (!app.isConnected()) {
                connectionStatus = Constants.ACTION_CONNECTION_STATE_NO_NETWORK;
                handleMenuSelection(MENU_STATUS);
            } else {
                // Show the contents of the last selected menu position. In case it
                // is not set, use the the default one defined in the settings
                int pos = (selectedMenuPosition == MENU_UNKNOWN) ? defaultMenuPosition : selectedMenuPosition;

                // Set the connection state to unknown if no connection was added
                // when the connection fragment was shown. The status fragment is
                // then shown with the information that no connection is available
                if (dbh.getConnections().isEmpty() 
                        || dbh.getSelectedConnection() == null) {
                    connectionStatus = Constants.ACTION_CONNECTION_STATE_NO_CONNECTION;
                    handleMenuSelection(MENU_STATUS);
                } else {
                    // A connection exists and is active, register to receive
                    // information from the server and connect to the server
                    app.addListener(this);
                    Utils.connect(this, false);

                    // Show the defined fragment from the menu position or the
                    // status if the connection state is not fine
                    handleMenuSelection((connectionStatus
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
        editor.putString(Constants.LAST_CONNECTION_STATE, connectionStatus);
        editor.putBoolean(Constants.LAST_CONNECTION_SETTINGS_SHOWN, connectionSettingsShown);
        editor.apply();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggles
        drawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putIntegerArrayList(Constants.MENU_STACK, menuStack);
        outState.putInt(Constants.MENU_POSITION, selectedMenuPosition);
        outState.putInt(Constants.CHANNEL_LIST_POSITION, channelListPosition);
        outState.putInt(Constants.COMPLETED_RECORDING_LIST_POSITION, completedRecordingListPosition);
        outState.putInt(Constants.SCHEDULED_RECORDING_LIST_POSITION, scheduledRecordingListPosition);
        outState.putInt(Constants.SERIES_RECORDING_LIST_POSITION, seriesRecordingListPosition);
        outState.putInt(Constants.TIMER_RECORDING_LIST_POSITION, timerRecordingListPosition);
        outState.putInt(Constants.FAILED_RECORDING_LIST_POSITION, failedRecordingListPosition);
        outState.putInt(Constants.REMOVED_RECORDING_LIST_POSITION, removedRecordingListPosition);
        outState.putString(Constants.BUNDLE_CONNECTION_STATUS, connectionStatus);
        outState.putBoolean(Constants.BUNDLE_CONNECTION_SETTINGS_SHOWN, connectionSettingsShown);
        outState.putInt(Constants.BUNDLE_CHANNEL_TIME_SELECTION, channelTimeSelection);
        outState.putLong(Constants.BUNDLE_SHOW_PROGRAMS_FROM_TIME, showProgramsFromTime);
        super.onSaveInstanceState(outState);
    }

    @SuppressLint({ "InlinedApi", "NewApi" })
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        // If the navigation drawer is open, hide all menu items
        boolean drawerOpen = drawerLayout.isDrawerOpen(drawerList);
        for (int i = 0; i < menu.size(); i++) {
            menu.getItem(i).setVisible(!drawerOpen);
        }

        // Do not show the search menu on these screens
        if (selectedMenuPosition == MENU_STATUS 
                || (selectedMenuPosition == MENU_COMPLETED_RECORDINGS && ds.getRecordingsByType(Constants.RECORDING_TYPE_COMPLETED).size() == 0)
                || selectedMenuPosition == MENU_SCHEDULED_RECORDINGS
                || selectedMenuPosition == MENU_FAILED_RECORDINGS
                || selectedMenuPosition == MENU_REMOVED_RECORDINGS
                || selectedMenuPosition == MENU_SERIES_RECORDINGS
                || selectedMenuPosition == MENU_TIMER_RECORDINGS) {
            (menu.findItem(R.id.menu_search)).setVisible(false);
        }

        // Hide the wake on lan menu from the status fragment. Only show it in
        // the status screen and if the app is unlocked and an address is set
        (menu.findItem(R.id.menu_wol)).setVisible(false);
        if (selectedMenuPosition == MENU_STATUS) {
            final Connection conn = dbh.getSelectedConnection();
            if (app.isUnlocked() && conn != null && conn.wol_mac_address.length() > 0) {
                (menu.findItem(R.id.menu_wol)).setVisible(true);
            }
        }

        // Prevent the refresh menu item from going into the overlay menu when
        // the status page is shown
        if (selectedMenuPosition == MENU_STATUS) {
            menu.findItem(R.id.menu_refresh).setShowAsActionFlags(
                    MenuItem.SHOW_AS_ACTION_ALWAYS
                            | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        }

        if (mMediaRouteMenuItem != null) {
            mMediaRouteMenuItem.setVisible(showCastMenuItem());
        }

        return true;
    }

    @SuppressLint("NewApi")
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
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        // Handle your other action bar items...
        switch (item.getItemId()) {
        case R.id.menu_refresh:
            // Clear all available data and all channels that are currently
            // loading, reconnect to the server and reload all data
            channelLoadingList.clear();
            Utils.connect(this, true);
            return true;

        case R.id.menu_wol:
            final Connection conn = dbh.getSelectedConnection();
            if (conn != null) {
                WakeOnLanTask task= new WakeOnLanTask(this, this, conn);
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
     * @param name Class name of the fragment
     * @param layout Layout that shall be used
     * @param addToBackStack True to add the fragment to the back stack, false otherwise
     */
    private void showFragment(String name, int layout, Bundle args, boolean addToBackStack) {
        Fragment f = Fragment.instantiate(this, name);
        if (args != null) {
            f.setArguments(args);
        }
        if (addToBackStack) {
            getSupportFragmentManager().beginTransaction().replace(layout, f)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .addToBackStack(null)
                    .commit();
        } else {
            getSupportFragmentManager().beginTransaction().replace(layout, f)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .commit();
        }
    }

    /**
     * Creates the fragment with the given name and shows it on the given
     * layout. If a bundle was given, it will be passed to the fragment.
     * 
     * @param name Class name of the fragment
     * @param layout Layout that shall be used
     * @param args Additional arguments that can be passed to the fragment
     */
    private void showFragment(String name, int layout, Bundle args) {
        showFragment(name, layout, args, false);
    }

    /**
     * Before the new fragments that belong to the selected menu item will be
     * shown, ensure that the old visible fragment is removed from the view and
     * that the correct layout weights are set.
     * 
     * @param position Selected position within the menu array
     */
    private void preHandleMenuSelection(int position) {
        setLayoutWeights(position);

        // Highlight the selected menu item
        if (drawerLayout.isDrawerOpen(drawerList)) {
            drawerList.setItemChecked(position, true);
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
    }

    /**
     * Called when a menu item from the navigation drawer was selected. It loads
     * and shows the correct fragment or fragments depending on the selected
     * menu item.
     * 
     * @param position Selected position within the menu array
     */
    private void handleMenuSelection(int position) {
        preHandleMenuSelection(position);

        // Save the menu position so we know which one was selected
        selectedMenuPosition = position;

        // Update the drawer menu so the correct item gets highlighted
        drawerAdapter.setPosition(selectedMenuPosition);
        drawerAdapter.notifyDataSetChanged();

        Bundle bundle = new Bundle();

        switch (position) {
        case MENU_CHANNELS:
            // Show the channel list fragment. If the information to show only
            // the channels as required in the program guide is not passed then
            // the full channel list fragment will be shown.
            bundle.putBoolean(Constants.BUNDLE_DUAL_PANE, isDualPane);
            bundle.putInt(Constants.BUNDLE_CHANNEL_TIME_SELECTION, channelTimeSelection);
            bundle.putLong(Constants.BUNDLE_SHOW_PROGRAMS_FROM_TIME, showProgramsFromTime);
            showFragment(ChannelListFragment.class.getName(), R.id.main_fragment, bundle);
            break;

        case MENU_COMPLETED_RECORDINGS:
            bundle.putBoolean(Constants.BUNDLE_DUAL_PANE, isDualPane);
            showFragment(CompletedRecordingListFragment.class.getName(), R.id.main_fragment, bundle);
            break;

        case MENU_SCHEDULED_RECORDINGS:
            bundle.putBoolean(Constants.BUNDLE_DUAL_PANE, isDualPane);
            showFragment(ScheduledRecordingListFragment.class.getName(), R.id.main_fragment, bundle);
            break;

        case MENU_SERIES_RECORDINGS:
            bundle.putBoolean(Constants.BUNDLE_DUAL_PANE, isDualPane);
            showFragment(SeriesRecordingListFragment.class.getName(), R.id.main_fragment, bundle);
            break;

        case MENU_TIMER_RECORDINGS:
            bundle.putBoolean(Constants.BUNDLE_DUAL_PANE, isDualPane);
            showFragment(TimerRecordingListFragment.class.getName(), R.id.main_fragment, bundle);
            break;

        case MENU_FAILED_RECORDINGS:
            bundle.putBoolean(Constants.BUNDLE_DUAL_PANE, isDualPane);
            showFragment(FailedRecordingListFragment.class.getName(), R.id.main_fragment, bundle);
            break;

        case MENU_REMOVED_RECORDINGS:
            bundle.putBoolean(Constants.BUNDLE_DUAL_PANE, isDualPane);
            showFragment(RemovedRecordingListFragment.class.getName(), R.id.main_fragment, bundle);
            break;

        case MENU_PROGRAM_GUIDE:
            // Show the program guide. This fragment will then trigger the
            // display of the channel list fragment on the left side.
            showFragment(ProgramGuidePagerFragment.class.getName(), R.id.main_fragment, null);
            break;

        case MENU_STATUS:
            bundle.putString(Constants.BUNDLE_CONNECTION_STATUS, connectionStatus);
            showFragment(StatusFragment.class.getName(), R.id.main_fragment, bundle);
            break;

        case MENU_SETTINGS:
            // Show the settings, but do not remember the selected position
            // because it would trigger the settings fragment again
            selectedMenuPosition = defaultMenuPosition;
            Intent settingsIntent = new Intent(this, SettingsActivity.class);
            startActivityForResult(settingsIntent, Constants.RESULT_CODE_SETTINGS);
            break;

        case MENU_UNLOCKER:
            selectedMenuPosition = defaultMenuPosition;
            Intent unlockerIntent = new Intent(this, UnlockerActivity.class);
            startActivity(unlockerIntent);
            break;

        case MENU_INFORMATION:
            selectedMenuPosition = defaultMenuPosition;
            Intent infoIntent = new Intent(this, InfoActivity.class);
            startActivity(infoIntent);
            break;
        }
    }

    /**
     * Sets the different weights of the three available layouts from the main
     * layout file depending on the menu selection. The availability of the
     * layouts depend on the on the screen size. This is determined
     * automatically
     * 
     * @param menuPosition Selected position within the menu array
     */
    private void setLayoutWeights(int menuPosition) {
        // The default layout weights
        float mainLayoutWeight = 0;
        float rightLayoutWeight = 0;

        switch (menuPosition) {
        case MENU_CHANNELS:
            if (isDualPane) {
                mainLayoutWeight = 4;
                rightLayoutWeight = 6;
            } else {
                mainLayoutWeight = 1;
            }
            break;

        case MENU_COMPLETED_RECORDINGS:
        case MENU_SCHEDULED_RECORDINGS:
        case MENU_SERIES_RECORDINGS:
        case MENU_TIMER_RECORDINGS:
        case MENU_FAILED_RECORDINGS:
        case MENU_REMOVED_RECORDINGS:
            if (isDualPane) {
                mainLayoutWeight = 6;
                rightLayoutWeight = 4;
            } else {
                mainLayoutWeight = 1;
            }
            break;

        case MENU_PROGRAM_GUIDE:
        case MENU_STATUS:
            mainLayoutWeight = 1;
            break;

        default:
            break;
        }

        // This is the layout for the main content. It is always visible
        FrameLayout mainLayout = (FrameLayout) findViewById(R.id.main_fragment);
        if (mainLayout != null) {
            LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) mainLayout.getLayoutParams();
            layoutParams.weight = mainLayoutWeight;
        }

        // This is the layout for the details on the right side. It is only
        // available on large tablets and on smaller tablets in landscape mode.
        FrameLayout rightLayout = (FrameLayout) findViewById(R.id.right_fragment);
        if (rightLayout != null) {
            LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) rightLayout.getLayoutParams();
            layoutParams.weight = rightLayoutWeight;
        }
    }

    /**
     * Called when an activity has quit that was called with the method
     * startActivityForResult. Depending on the given request and result code
     * certain action can be done.
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Constants.RESULT_CODE_SETTINGS) {
            if (resultCode == RESULT_OK) {
                // Restart the activity when certain settings have changed
                if (data.getBooleanExtra(Constants.BUNDLE_RESTART, false)) {
                    Intent intent = getIntent();
                    finish();
                    startActivity(intent);
                } else {
                    // Reconnect to the server and reload all data if a
                    // connection or certain settings have changed.
                    if (data.getBooleanExtra(Constants.BUNDLE_RECONNECT, false)) {
                        Utils.connect(this, true);
                        Intent intent = getIntent();
                        finish();
                        startActivity(intent);
                    }
                }
            }
        } else {
            // No custom request code was returned, nothing to to 
            super.onActivityResult(requestCode, resultCode, data);
        }
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
                                intent.setAction(Constants.ACTION_GET_DVR_CONFIG);
                                startService(intent);
                            }
                            // Reload the menu. Only after the initial sync we know the server
                            // version which determines the visibility of the casting icon
                            invalidateOptionsMenu();
                        }
                    }
                });
                break;
            case Constants.ACTION_CHANNEL_UPDATE:
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
            case Constants.ACTION_CONNECTION_STATE_OK:
                connectionStatus = action;
                runOnUiThread(new Runnable() {
                    public void run() {
                        showDrawerMenu();
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
                            showDrawerMenu();
                            connectionStatus = action;
                            handleMenuSelection(MENU_STATUS);
                        }
                    });
                }

                break;
            case Constants.ACTION_SHOW_MESSAGE:
                final String msg = (String) obj;
                showMessage(msg);
                break;
        }
    }

    private void showMessage(final String msg) {
        Snackbar.make(coordinatorLayout, msg, Snackbar.LENGTH_SHORT).show();
    }

    /**
     * Depending on the connection state and server capabilities show or hide
     * certain navigation menu items.
     */
    private void showDrawerMenu() {
        boolean show = connectionStatus.equals(Constants.ACTION_CONNECTION_STATE_OK) && !ds.isLoading();

        // Enable the main menus in the drawer
        drawerAdapter.getItemById(MENU_CHANNELS).isVisible = show;
        drawerAdapter.getItemById(MENU_COMPLETED_RECORDINGS).isVisible = show;
        drawerAdapter.getItemById(MENU_SCHEDULED_RECORDINGS).isVisible = show;
        drawerAdapter.getItemById(MENU_FAILED_RECORDINGS).isVisible = show;
        drawerAdapter.getItemById(MENU_REMOVED_RECORDINGS).isVisible = show;
        drawerAdapter.getItemById(MENU_PROGRAM_GUIDE).isVisible = show;

        // Only show the series recording menu if the server supports it
        drawerAdapter.getItemById(MENU_SERIES_RECORDINGS).isVisible = (show && (ds
                .getProtocolVersion() >= Constants.MIN_API_VERSION_SERIES_RECORDINGS));

        // Only show the timer recording menu if the server supports it and the
        // application is unlocked
        drawerAdapter.getItemById(MENU_TIMER_RECORDINGS).isVisible = (show
                && (ds.getProtocolVersion() >= Constants.MIN_API_VERSION_TIMER_RECORDINGS)
                && app.isUnlocked());

        // Show the menu item to unlock the application if it was not yet purchased
        drawerAdapter.getItemById(MENU_UNLOCKER).isVisible = !app.isUnlocked();

        // Replace the adapter contents so the view will be updated
        drawerList.setAdapter(null);
        drawerList.setAdapter(drawerAdapter);
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
            if (selectedMenuPosition == MENU_CHANNELS || selectedMenuPosition == MENU_PROGRAM_GUIDE) {
                actionBarIcon.setVisibility(Utils.showChannelTagIcon(this) ? View.VISIBLE : View.GONE);
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
            if (selectedMenuPosition == MENU_CHANNELS || selectedMenuPosition == MENU_PROGRAM_GUIDE) {
                actionBarIcon.setVisibility(Utils.showChannelTagIcon(this) ? View.VISIBLE : View.GONE);
                actionBarIcon.setBackgroundResource(resource);
            } else {
                actionBarIcon.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onScrollingChanged(final int position, final int offset, final String tag) {
        switch (selectedMenuPosition) {
        case MENU_CHANNELS:
            // Save the position of the selected channel so it can be restored
            // after an orientation change
            channelListPosition = position;
            break;

        case MENU_PROGRAM_GUIDE:
            // Save the scroll values so they can be reused after an orientation change.
            programGuideListPosition = position;
            programGuideListPositionOffset = offset;

            if (tag.equals(ChannelListFragment.class.getSimpleName())
                    || tag.equals(ProgramGuideListFragment.class.getSimpleName())) {
                // Scrolling was initiated by the channel or program guide list fragment. Keep
                // the currently visible program guide list in sync by scrolling it to the same position
                final Fragment f = getSupportFragmentManager().findFragmentById(R.id.main_fragment);
                if (f instanceof ProgramGuidePagerFragment) {
                    ((FragmentControlInterface) f).setSelection(position, offset);
                }
            }
            break;
        }
    }

    @Override
    public void onScrollStateIdle(final String tag) {
        switch (selectedMenuPosition) {
        case MENU_PROGRAM_GUIDE:
            if (tag.equals(ProgramGuideListFragment.class.getSimpleName()) 
                    || tag.equals(ChannelListFragment.class.getSimpleName())) {
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
        if (ds.isLoading() || channel == null) {
            return;
        }

        channelLoadingList.add(channel);
        setActionBarSubtitle(getString(R.string.loading));
        Utils.loadMorePrograms(this, channel);
    }

    @Override
    public void onListItemSelected(final int position, final Channel channel, final String tag) {
        switch (selectedMenuPosition) {
        case MENU_CHANNELS:
            // Save the position of the selected channel so it can be restored
            // after an orientation change
            channelListPosition = position;

            // Show the program list fragment. In dual pane mode the program
            // list shall be shown on the right side of the channel list,
            // otherwise replace the channel list.
            if (channel != null) {
                // Play the channel when the channel icon has 
                // been clicked, otherwise show the channel details
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                if (prefs.getBoolean("playWhenChannelIconSelectedPref", true)
                        && tag.equals(Constants.TAG_CHANNEL_ICON)) {
                    mMenuUtils.handleMenuPlaySelection(channel.id, -1);

                } else {
                    Bundle bundle = new Bundle();
                    bundle.putLong(Constants.BUNDLE_CHANNEL_ID, channel.id);
                    bundle.putBoolean(Constants.BUNDLE_DUAL_PANE, isDualPane);
                    bundle.putLong(Constants.BUNDLE_SHOW_PROGRAMS_FROM_TIME, showProgramsFromTime);
    
                    if (isDualPane) {
                        showFragment(ProgramListFragment.class.getName(), R.id.right_fragment, bundle);
                    } else {
                        showFragment(ProgramListFragment.class.getName(), R.id.main_fragment, bundle, true);
                    }
                }
            }
            break;

        case MENU_PROGRAM_GUIDE:
            // If a channel was selected in the program guide screen, start
            // playing the selected channel
            mMenuUtils.handleMenuPlaySelection(channel.id, -1);
            break;
        }
    }

    @Override
    public void onListItemSelected(final int position, final Recording recording, final String tag) {
        // Save the position of the selected recording type so it can be
        // restored after an orientation change
        switch (selectedMenuPosition) {
        case MENU_COMPLETED_RECORDINGS:
            completedRecordingListPosition = position;
            break;
        case MENU_SCHEDULED_RECORDINGS:
            scheduledRecordingListPosition = position;
            break;
        case MENU_FAILED_RECORDINGS:
            failedRecordingListPosition = position;
            break;
        case MENU_REMOVED_RECORDINGS:
            removedRecordingListPosition = position;
            break;
        }

        if (recording != null) {
            // If the channel icon of a recording was selected and the setting 
            // is activated play the recording instead of showing its details
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            if (prefs.getBoolean("playWhenChannelIconSelectedPref", true) && 
                    tag.equals(Constants.TAG_CHANNEL_ICON)) {
                mMenuUtils.handleMenuPlaySelection(-1, recording.id);
            } else {
                // When a recording has been selected from the recording list fragment,
                // show its details. In dual mode these are shown in a separate fragment
                // to the right of the recording list, otherwise replace the recording
                // list with the details fragment.
                Bundle args = new Bundle();
                args.putLong(Constants.BUNDLE_RECORDING_ID, recording.id);
                args.putBoolean(Constants.BUNDLE_SHOW_CONTROLS, !isDualPane);

                if (isDualPane) {
                    showFragment(RecordingDetailsFragment.class.getName(), R.id.right_fragment, args);
                } else {
                    DialogFragment newFragment = RecordingDetailsFragment.newInstance(args);
                    newFragment.show(getSupportFragmentManager(), "dialog");
                }
            }
        }
    }

    @Override
    public void onListItemSelected(final int position, final SeriesRecording seriesRecording, final String tag) {
        // Save the position of the selected recording type so it can be
        // restored after an orientation change
        switch (selectedMenuPosition) {
        case MENU_SERIES_RECORDINGS:
            seriesRecordingListPosition = position;
            break;
        }
        // When a series recording has been selected from the recording list fragment,
        // show its details. In dual mode these are shown in a separate fragment
        // to the right of the series recording list, otherwise replace the recording
        // list with the details fragment.
        if (seriesRecording != null) {
            Bundle args = new Bundle();
            args.putString(Constants.BUNDLE_SERIES_RECORDING_ID, seriesRecording.id);
            args.putBoolean(Constants.BUNDLE_SHOW_CONTROLS, !isDualPane);

            if (isDualPane) {
                showFragment(SeriesRecordingDetailsFragment.class.getName(), R.id.right_fragment, args);
            } else {
                DialogFragment newFragment = SeriesRecordingDetailsFragment.newInstance(args);
                newFragment.show(getSupportFragmentManager(), "dialog");
            }
        }
    }

    @Override
    public void onListItemSelected(final int position, final TimerRecording timerRecording, final String tag) {
        // Save the position of the selected recording type so it can be
        // restored after an orientation change
        switch (selectedMenuPosition) {
        case MENU_TIMER_RECORDINGS:
            timerRecordingListPosition = position;
            break;
        }
        // When a timer recording has been selected from the recording list fragment,
        // show its details. In dual mode these are shown in a separate fragment
        // to the right of the series recording list, otherwise replace the recording
        // list with the details fragment.
        if (timerRecording != null) {
            Bundle args = new Bundle();
            args.putString(Constants.BUNDLE_TIMER_RECORDING_ID, timerRecording.id);
            args.putBoolean(Constants.BUNDLE_SHOW_CONTROLS, !isDualPane);

            if (isDualPane) {
                showFragment(TimerRecordingDetailsFragment.class.getName(), R.id.right_fragment, args);
            } else {
                DialogFragment newFragment = TimerRecordingDetailsFragment.newInstance(args);
                newFragment.show(getSupportFragmentManager(), "dialog");
            }
        }
    }

    @Override
    public void onListItemSelected(final int position, final Program program, final String tag) {
        // When a program has been selected from the program list fragment,
        // show its details. In single or dual pane mode these are shown in a
        // separate dialog fragment
        if (program != null) {
            Bundle args = new Bundle();
            args.putLong(Constants.BUNDLE_PROGRAM_ID, program.id);
            args.putLong(Constants.BUNDLE_CHANNEL_ID, program.channel.id);
            args.putBoolean(Constants.BUNDLE_SHOW_CONTROLS, true);

            DialogFragment newFragment = ProgramDetailsFragment.newInstance(args);
            newFragment.show(getSupportFragmentManager(), "dialog");
        }
    }

    @Override
    public void onListPopulated(final String tag) {

        switch (selectedMenuPosition) {
        case MENU_CHANNELS:
            // When the channel list fragment is done loading and dual pane is 
            // active, preselect a channel so that the program list on the right
            // will be shown for that channel. If no dual pane is active scroll
            // to the selected channel
            if (tag.equals(ChannelListFragment.class.getSimpleName())) { 
                final Fragment f = getSupportFragmentManager().findFragmentById(R.id.main_fragment);
                if (f instanceof ChannelListFragment) {
                    if (isDualPane) {
                        ((FragmentControlInterface) f).setInitialSelection(channelListPosition);
                    } else {
                        ((FragmentControlInterface) f).setSelection(channelListPosition, 0);
                    }
                }
            }
            break;

        case MENU_COMPLETED_RECORDINGS:
            // When the recording list fragment is done loading and dual pane is
            // active, preselect a recording from the list to show the details
            // of this recording on the right side. Before doing that remove the
            // right fragment in case the list in the main fragment is
            // empty to avoid showing invalid data.
            if (isDualPane) {
                final Fragment rf = getSupportFragmentManager().findFragmentById(R.id.right_fragment);
                if (rf != null) {
                    getSupportFragmentManager().beginTransaction().remove(rf).commit();
                }
                final Fragment f = getSupportFragmentManager().findFragmentById(R.id.main_fragment);
                if (f instanceof CompletedRecordingListFragment) {
                    ((FragmentControlInterface) f).setInitialSelection(completedRecordingListPosition);
                }
            }
            break;

        case MENU_SCHEDULED_RECORDINGS:
            // When the recording list fragment is done loading and dual pane is
            // active, preselect a recording from the list to show the details
            // of this recording on the right side. Before doing that remove the
            // right fragment in case the list in the main fragment is
            // empty to avoid showing invalid data.
            if (isDualPane) {
                final Fragment rf = getSupportFragmentManager().findFragmentById(R.id.right_fragment);
                if (rf != null) {
                    getSupportFragmentManager().beginTransaction().remove(rf).commit();
                }
                final Fragment f = getSupportFragmentManager().findFragmentById(R.id.main_fragment);
                if (f instanceof ScheduledRecordingListFragment) {
                    ((FragmentControlInterface) f).setInitialSelection(scheduledRecordingListPosition);
                }
            }
            break;

        case MENU_SERIES_RECORDINGS:
            // When the recording list fragment is done loading and dual pane is
            // active, preselect a recording from the list to show the details
            // of this recording on the right side. Before doing that remove the
            // right fragment in case the list in the main fragment is
            // empty to avoid showing invalid data.
            if (isDualPane) {
                final Fragment rf = getSupportFragmentManager().findFragmentById(R.id.right_fragment);
                if (rf != null) {
                    getSupportFragmentManager().beginTransaction().remove(rf).commit();
                }
                final Fragment f = getSupportFragmentManager().findFragmentById(R.id.main_fragment);
                if (f instanceof SeriesRecordingListFragment) {
                    ((FragmentControlInterface) f).setInitialSelection(seriesRecordingListPosition);
                }
            }
            break;

        case MENU_TIMER_RECORDINGS:
            // When the recording list fragment is done loading and dual pane is
            // active, preselect a recording from the list to show the details
            // of this recording on the right side. Before doing that remove the
            // right fragment in case the list in the main fragment is
            // empty to avoid showing invalid data.
            if (isDualPane) {
                final Fragment rf = getSupportFragmentManager().findFragmentById(R.id.right_fragment);
                if (rf != null) {
                    getSupportFragmentManager().beginTransaction().remove(rf).commit();
                }
                final Fragment f = getSupportFragmentManager().findFragmentById(R.id.main_fragment);
                if (f instanceof TimerRecordingListFragment) {
                    ((FragmentControlInterface) f).setInitialSelection(timerRecordingListPosition);
                }
            }
            break;

        case MENU_FAILED_RECORDINGS:
            // When the recording list fragment is done loading and dual pane is
            // active, preselect a recording from the list to show the details
            // of this recording on the right side. Before doing that remove the
            // right fragment in case the list in the main fragment is
            // empty to avoid showing invalid data.
            if (isDualPane) {
                final Fragment rf = getSupportFragmentManager().findFragmentById(R.id.right_fragment);
                if (rf != null) {
                    getSupportFragmentManager().beginTransaction().remove(rf).commit();
                }
                final Fragment f = getSupportFragmentManager().findFragmentById(R.id.main_fragment);
                if (f instanceof FailedRecordingListFragment) {
                    ((FragmentControlInterface) f).setInitialSelection(failedRecordingListPosition);
                }
            }
            break;

        case MENU_REMOVED_RECORDINGS:
            // When the recording list fragment is done loading and dual pane is
            // active, preselect a recording from the list to show the details
            // of this recording on the right side. Before doing that remove the
            // right fragment in case the list in the main fragment is
            // empty to avoid showing invalid data.
            if (isDualPane) {
                final Fragment rf = getSupportFragmentManager().findFragmentById(R.id.right_fragment);
                if (rf != null) {
                    getSupportFragmentManager().beginTransaction().remove(rf).commit();
                }
                final Fragment f = getSupportFragmentManager().findFragmentById(R.id.main_fragment);
                if (f instanceof RemovedRecordingListFragment) {
                    ((FragmentControlInterface) f).setInitialSelection(removedRecordingListPosition);
                }
            }
            break;

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
        switch (selectedMenuPosition) {
        case MENU_CHANNELS:
            // Inform the channel list fragment to clear all data from its
            // channel list and show only the channels with the selected tag
            final Fragment f = getSupportFragmentManager().findFragmentById(R.id.main_fragment);
            if (f instanceof ChannelListFragment) {
                ((FragmentControlInterface) f).reloadData();
            }
            break;

        case MENU_PROGRAM_GUIDE:
            // Inform the channel list fragment to clear all data from its
            // channel list and show only the channels with the selected tag
            final Fragment cf = getSupportFragmentManager().findFragmentById(R.id.program_guide_channel_fragment);
            if (cf instanceof ChannelListFragment) {
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
    public void listDataInvalid(String tag) {
        switch (selectedMenuPosition) {
        case MENU_SERIES_RECORDINGS:
        case MENU_TIMER_RECORDINGS:
            // Inform the defined fragment to reload and update all data in its
            // list view. This is currently only required in the timer and
            // series recording fragments because there is no update service
            // call. So the old recording needs to be removed before the new is
            // added, to avoid having two identical entries the list. 
            final Fragment f = getSupportFragmentManager().findFragmentById(R.id.main_fragment);
            if (f instanceof SeriesRecordingListFragment || f instanceof TimerRecordingListFragment) {
                ((FragmentControlInterface) f).reloadData();
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
                    bundle.putLong(Constants.BUNDLE_CHANNEL_ID, ch.id);
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
                bundle.putLong(Constants.BUNDLE_RECORDING_ID, rec.id);
            }
        }

        searchIntent.putExtra(SearchManager.APP_DATA, bundle);
        startActivity(searchIntent);
        return true;
    }

    @SuppressLint("NewApi")
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

    @Override
    public void onChannelTimeSelected(int selection, long time) {
        channelTimeSelection = selection;
        showProgramsFromTime = time;
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
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Connection conn = dbh.getSelectedConnection();
        Profile profile = (conn != null ? dbh.getProfile(conn.cast_profile_id) : null);
        return (app.isUnlocked()
                && prefs.getBoolean("pref_enable_casting", false)
                && profile != null 
                && profile.enabled);
    }

    @Override
    public void notify(String message) {
        if (getCurrentFocus() != null) {
            Snackbar.make(getCurrentFocus(), message, Snackbar.LENGTH_LONG).show();
        }
    }
}
