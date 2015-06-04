package org.tvheadend.tvhclient;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.tvheadend.tvhclient.ChangeLogDialog.ChangeLogDialogInterface;
import org.tvheadend.tvhclient.adapter.DrawerMenuAdapter;
import org.tvheadend.tvhclient.fragments.ChannelListFragment;
import org.tvheadend.tvhclient.fragments.CompletedRecordingListFragment;
import org.tvheadend.tvhclient.fragments.FailedRecordingListFragment;
import org.tvheadend.tvhclient.fragments.ProgramDetailsFragment;
import org.tvheadend.tvhclient.fragments.ProgramGuideListFragment;
import org.tvheadend.tvhclient.fragments.ProgramGuidePagerFragment;
import org.tvheadend.tvhclient.fragments.ProgramListFragment;
import org.tvheadend.tvhclient.fragments.RecordingDetailsFragment;
import org.tvheadend.tvhclient.fragments.ScheduledRecordingListFragment;
import org.tvheadend.tvhclient.fragments.SeriesRecordingDetailsFragment;
import org.tvheadend.tvhclient.fragments.SeriesRecordingListFragment;
import org.tvheadend.tvhclient.fragments.StatusFragment;
import org.tvheadend.tvhclient.fragments.TimerRecordingDetailsFragment;
import org.tvheadend.tvhclient.fragments.TimerRecordingListFragment;
import org.tvheadend.tvhclient.interfaces.ActionBarInterface;
import org.tvheadend.tvhclient.interfaces.FragmentControlInterface;
import org.tvheadend.tvhclient.interfaces.FragmentScrollInterface;
import org.tvheadend.tvhclient.interfaces.FragmentStatusInterface;
import org.tvheadend.tvhclient.interfaces.HTSListener;
import org.tvheadend.tvhclient.model.Channel;
import org.tvheadend.tvhclient.model.Connection;
import org.tvheadend.tvhclient.model.DrawerMenuItem;
import org.tvheadend.tvhclient.model.Program;
import org.tvheadend.tvhclient.model.Recording;
import org.tvheadend.tvhclient.model.SeriesRecording;
import org.tvheadend.tvhclient.model.TimerRecording;

import android.annotation.SuppressLint;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.ViewDragHelper;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.SearchView;
import android.view.Gravity;
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
import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;
import com.nispok.snackbar.enums.SnackbarType;

@SuppressWarnings("deprecation")
public class MainActivity extends ActionBarActivity implements SearchView.OnQueryTextListener, SearchView.OnSuggestionListener, ChangeLogDialogInterface, ActionBarInterface, FragmentStatusInterface, FragmentScrollInterface, HTSListener {

    @SuppressWarnings("unused")
    private final static String TAG = MainActivity.class.getSimpleName();

    private ListView drawerList;
    private DrawerLayout drawerLayout;
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
    private static final int MENU_PROGRAM_GUIDE = 6;
    private static final int MENU_STATUS = 7;
    private static final int MENU_SETTINGS = 8;
    private static final int MENU_CONNECTIONS = 9;
    private static final int MENU_INFORMATION = 10;
    private static final int MENU_UNLOCKER = 11;

    // Holds the list of selected menu items so the previous fragment can be
    // shown again when the user has pressed the back key.
    public ArrayList<Integer> menuStack = new ArrayList<Integer>();

    // Indicates that a loading channel data is in progress, the next channel
    // can only be loaded when this variable is false
    private boolean isLoadingChannels = false;

    // Holds a list of channels that are currently being loaded
    public List<Channel> channelLoadingList = new ArrayList<Channel>();

    // Holds the number of EPG entries for each channel
    @SuppressLint("UseSparseArrays")
    public Map<Long, Integer> channelEpgCountList = new HashMap<Long, Integer>();

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

    private TVHClientApplication app;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(Utils.getThemeId(this));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_layout);
        Utils.setLanguage(this);

        // Check if dual pane mode shall be activated (two fragments at the same
        // time). This is usually available on tablets
        View v = findViewById(R.id.right_fragment);
        isDualPane = v != null && v.getVisibility() == View.VISIBLE;

        DatabaseHelper.init(this.getApplicationContext());
        changeLogDialog = new ChangeLogDialog(this);

        actionBar = getSupportActionBar();
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        actionBar.setCustomView(R.layout.actionbar_title);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayUseLogoEnabled(Utils.showChannelIcons(this));

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

        // Increase the edge where the user can touch to slide out the
        // navigation drawer
        Field dragger = null;
        try {
            dragger = drawerLayout.getClass().getDeclaredField("mLeftDragger");
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        dragger.setAccessible(true);
        ViewDragHelper draggerObj = null;
        try {
            draggerObj = (ViewDragHelper) dragger.get(drawerLayout);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        Field edgeSize = null;
        try {
            edgeSize = draggerObj.getClass().getDeclaredField("mEdgeSize");
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        edgeSize.setAccessible(true);
        int edge = 20;
        try {
            edge = edgeSize.getInt(draggerObj);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        try {
            edgeSize.setInt(draggerObj, edge * 2);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

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
        View header = (View) inflater.inflate(R.layout.drawer_list_header, drawerList, false);
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

        // Add a listener to the server name to allow changing the current
        // connection. A drop down menu with all connections will be displayed.
        ImageView serverSelection = (ImageView) drawerList.findViewById(R.id.server_selection);
        if (serverSelection != null) {
            final Context context = this;
            serverSelection.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {

                    // Create a list of available connections names that the
                    // selection dialog can display. Additionally preselect the
                    // current active connection.
                    final List<Connection> connList = DatabaseHelper.getInstance().getConnections();
                    if (connList != null) {
                        int currentConnectionListPosition = -1;
                        Connection currentConnection = DatabaseHelper.getInstance().getSelectedConnection();
                        String[] items = new String[connList.size()];
                        for (int i = 0; i < connList.size(); i++) {
                            items[i] = connList.get(i).name;
                            if (currentConnection != null && currentConnection.id == connList.get(i).id) {
                                currentConnectionListPosition = i;
                            }
                        }

                        // Now show the dialog to select a new connection
                        new MaterialDialog.Builder(context)
                            .title(R.string.select_connection)
                            .items(items)
                            .itemsCallbackSingleChoice(currentConnectionListPosition, new MaterialDialog.ListCallbackSingleChoice() {
                                @Override
                                public boolean onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                                    Connection oldConn = DatabaseHelper.getInstance().getSelectedConnection();
                                    Connection newConn = connList.get(which);

                                    // Switch the active connection and reconnect  
                                    // only if a new connection has been selected
                                    if (oldConn != null && newConn != null && oldConn.id != newConn.id) {
                                        // Close the menu when a new connection has been selected
                                        drawerLayout.closeDrawers();
                                        // Set the new connection as the active one
                                        newConn.selected = true;
                                        oldConn.selected = false;
                                        DatabaseHelper.getInstance().updateConnection(oldConn);
                                        DatabaseHelper.getInstance().updateConnection(newConn);
                                        Utils.connect(context, true);
                                    }
                                    return true;
                                }
                            })
                            .show();
                    }
                }
            });
        }

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
            connectionStatus = savedInstanceState.getString(Constants.BUNDLE_CONNECTION_STATUS);
            connectionSettingsShown = savedInstanceState.getBoolean(Constants.BUNDLE_CONNECTION_SETTINGS_SHOWN);
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
            final Connection conn = DatabaseHelper.getInstance().getSelectedConnection();
            if (DatabaseHelper.getInstance().getConnections().isEmpty()) {
                serverName.setText(R.string.no_connection_available);
                serverSelection.setVisibility(View.GONE);
            } else if (DatabaseHelper.getInstance().getConnections().size() == 1) {
                serverName.setText(conn.name);
                serverSelection.setVisibility(View.GONE);
            } else if (conn == null) {
                serverName.setText(R.string.no_connection_active);
                serverSelection.setVisibility(View.GONE);
            } else {
                serverName.setText(conn.name);
                serverSelection.setVisibility(View.VISIBLE);
            }
        }

        // Show or hide certain menu entries
        showDrawerMenu();

        // Update the number of recordings in each category
        drawerAdapter.getItemById(MENU_COMPLETED_RECORDINGS).count = 
                app.getRecordingsByType(Constants.RECORDING_TYPE_COMPLETED).size();
        drawerAdapter.getItemById(MENU_SCHEDULED_RECORDINGS).count = 
                app.getRecordingsByType(Constants.RECORDING_TYPE_SCHEDULED).size();
        drawerAdapter.getItemById(MENU_SERIES_RECORDINGS).count = 
                app.getSeriesRecordings().size();
        drawerAdapter.getItemById(MENU_TIMER_RECORDINGS).count =
                app.getTimerRecordings().size();
        drawerAdapter.getItemById(MENU_FAILED_RECORDINGS).count = 
                app.getRecordingsByType(Constants.RECORDING_TYPE_FAILED).size();
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

        List<DrawerMenuItem> list = new ArrayList<DrawerMenuItem>();
        list.add(new DrawerMenuItem(""));
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
        list.add(new DrawerMenuItem(MENU_PROGRAM_GUIDE, menuItems[6],
                (lightTheme) ? R.drawable.ic_menu_program_guide_light
                        : R.drawable.ic_menu_program_guide_dark));
        list.add(new DrawerMenuItem(MENU_STATUS, menuItems[7],
                (lightTheme) ? R.drawable.ic_menu_status_light : R.drawable.ic_menu_status_dark));

        list.add(new DrawerMenuItem(""));
        list.add(new DrawerMenuItem(MENU_SETTINGS, menuItems[8],
                (lightTheme) ? R.drawable.ic_menu_settings_light : R.drawable.ic_menu_settings_dark));
        list.add(new DrawerMenuItem(MENU_CONNECTIONS, menuItems[9],
                (lightTheme) ? R.drawable.ic_menu_connections_light
                        : R.drawable.ic_menu_connections_dark));
        list.add(new DrawerMenuItem(MENU_INFORMATION, menuItems[10],
                (lightTheme) ? R.drawable.ic_menu_info_light
                        : R.drawable.ic_menu_info_dark));

        list.add(new DrawerMenuItem(""));
        list.add(new DrawerMenuItem(MENU_UNLOCKER, menuItems[11],
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
        connectionStatus = prefs.getString(Constants.LAST_CONNECTION_STATE, Constants.ACTION_CONNECTION_STATE_OK);
        connectionSettingsShown = prefs.getBoolean(Constants.LAST_CONNECTION_SETTINGS_SHOWN, false);

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
                && (DatabaseHelper.getInstance().getConnections().isEmpty() 
                        || DatabaseHelper.getInstance().getSelectedConnection() == null)) {
            connectionSettingsShown = true;
            handleMenuSelection(MENU_CONNECTIONS);
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
                if (DatabaseHelper.getInstance().getConnections().isEmpty() 
                        || DatabaseHelper.getInstance().getSelectedConnection() == null) {
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
        super.onPause();
        app.removeListener(this);

        // Save the previously active connection status and if the connection
        // settings have been already shown
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(Constants.LAST_CONNECTION_STATE, connectionStatus);
        editor.putBoolean(Constants.LAST_CONNECTION_SETTINGS_SHOWN, connectionSettingsShown);
        editor.commit();
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
        outState.putString(Constants.BUNDLE_CONNECTION_STATUS, connectionStatus);
        outState.putBoolean(Constants.BUNDLE_CONNECTION_SETTINGS_SHOWN, connectionSettingsShown);
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
                || selectedMenuPosition == MENU_COMPLETED_RECORDINGS 
                || selectedMenuPosition == MENU_SCHEDULED_RECORDINGS
                || selectedMenuPosition == MENU_FAILED_RECORDINGS
                || selectedMenuPosition == MENU_SERIES_RECORDINGS
                || selectedMenuPosition == MENU_TIMER_RECORDINGS) {
            (menu.findItem(R.id.menu_search)).setVisible(false);
        }

        // Prevent the refresh menu item from going into the overlay menu when
        // the status page is shown
        if (selectedMenuPosition == MENU_STATUS
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            menu.findItem(R.id.menu_refresh).setShowAsActionFlags(
                    MenuItem.SHOW_AS_ACTION_ALWAYS
                            | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        }
        return true;
    }

    @SuppressLint("NewApi")
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main_menu, menu);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
            searchMenuItem = menu.findItem(R.id.menu_search); 
            SearchView searchView = (SearchView) searchMenuItem.getActionView();
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
            searchView.setIconifiedByDefault(true);
            searchView.setOnQueryTextListener(this);
            searchView.setOnSuggestionListener(this);
        }
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
        case R.id.menu_search:
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
                onSearchRequested();
            }
            return true;

        case R.id.menu_refresh:
            // Clear all available data and all channels that are currently
            // loading, reconnect to the server and reload all data
            channelLoadingList.clear();
            channelEpgCountList.clear();
            app.unblockAllChannels();
            Utils.connect(this, true);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * This method is called when the user wants to search for programs. In case
     * the program list of one channel is visible search only within this
     * channel, otherwise search within the entire available program data.
     */
    @Override
    public boolean onSearchRequested() {
        Bundle bundle = new Bundle();
        if (!isDualPane) {
            final Fragment f = getSupportFragmentManager().findFragmentById(R.id.main_fragment);
            if (f instanceof ProgramListFragment && f instanceof FragmentControlInterface) {
                Object o = ((FragmentControlInterface) f).getSelectedItem();
                if (o instanceof Channel) {
                    final Channel ch = (Channel) o;
                    bundle.putLong(Constants.BUNDLE_CHANNEL_ID, ch.id);
                }
            }
        }
        startSearch(null, false, bundle, false);
        return true;
    }

    /**
     * Creates the fragment with the given name and shows it on the given
     * layout. If a bundle was given, it will be passed to the fragment. If
     * required the created fragment will be stored on the back stack so it can
     * be shown later when the user has pressed the back button.
     * 
     * @param name
     * @param layout
     * @param args
     * @param addToBackStack
     */
    private void showFragment(String name, int layout, Bundle args, boolean addToBackStack) {
        Fragment f = Fragment.instantiate(this, name);
        if (args != null) {
            f.setArguments(args);
        }
        if (addToBackStack) {
            getSupportFragmentManager().beginTransaction().replace(layout, f)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .addToBackStack(null)
                    .commit();
        } else {
            getSupportFragmentManager().beginTransaction().replace(layout, f)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .commit();
        }
    }

    /**
     * Creates the fragment with the given name and shows it on the given
     * layout. If a bundle was given, it will be passed to the fragment.
     * 
     * @param name
     * @param layout
     * @param args
     */
    private void showFragment(String name, int layout, Bundle args) {
        showFragment(name, layout, args, false);
    }

    /**
     * Before the new fragments that belong to the selected menu item will be
     * shown, ensure that the old visible fragment is removed from the view and
     * that the correct layout weights are set.
     * 
     * @param position
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
     * @param position
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
            showFragment(ChannelListFragment.class.getName(), R.id.main_fragment, null);
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

        case MENU_CONNECTIONS:
            selectedMenuPosition = defaultMenuPosition;
            Intent connIntent = new Intent(this, SettingsActivity.class);
            connIntent.putExtra(Constants.BUNDLE_MANAGE_CONNECTIONS, true);
            startActivityForResult(connIntent, Constants.RESULT_CODE_SETTINGS);
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
     * @param menuPosition
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
        if (action.equals(Constants.ACTION_LOADING)) {
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
                    }
                }
            });
        } else if (action.equals(Constants.ACTION_CHANNEL_UPDATE)) {
            runOnUiThread(new Runnable() {
                public void run() {
                    final Channel ch = (Channel) obj;
                    if (ch != null && ch.epg != null) {
                        if (channelEpgCountList.containsKey(ch.id)) {
                            // If the program count of the channel has not
                            // changed assume that no more programs have been
                            // loaded. Add the channel to the blocking list to
                            // avoid making unnecessary loading calls. Otherwise
                            // remove the channel from the blocking list
                            if (ch.epg.size() == channelEpgCountList.get(ch.id)) {
                                app.blockChannel(ch);
                            } else {
                                channelEpgCountList.put(ch.id, ch.epg.size());
                                app.unblockChannel(ch);
                            }
                        } else {
                            channelEpgCountList.put(ch.id, ch.epg.size());
                        }
                    }

                    // The channel has been updated (usually by a call to load
                    // more data) so remove it from the loading queue and
                    // continue with the next one.
                    channelLoadingList.remove(ch);
                    isLoadingChannels = false;

                    // Load more programs if the list contains more channels,
                    // otherwise display the number of channels. 
                    if (!channelLoadingList.isEmpty()) {
                        loadMorePrograms();
                    } else {
                        // Get the number of channels from the program guide
                        // pager fragment because it holds the channel fragment
                        // which in turn knows the channel count. 
                        Fragment f = getSupportFragmentManager().findFragmentById(R.id.main_fragment);
                        if (f != null && f instanceof ProgramGuidePagerFragment && f instanceof FragmentControlInterface) {
                            int count = ((FragmentControlInterface) f).getItemCount();
                            String items = getResources().getQuantityString(R.plurals.items, count, count);
                            setActionBarSubtitle(items);
                        }
                    }
                }
            });
        } else if (action.equals(Constants.ACTION_CONNECTION_STATE_OK)) {
            connectionStatus = action;
            runOnUiThread(new Runnable() {
                public void run() {
                    showDrawerMenu();
                }
            });
        } else if (action.equals(Constants.ACTION_CONNECTION_STATE_SERVER_DOWN)
                || action.equals(Constants.ACTION_CONNECTION_STATE_LOST)
                || action.equals(Constants.ACTION_CONNECTION_STATE_TIMEOUT)
                || action.equals(Constants.ACTION_CONNECTION_STATE_REFUSED)
                || action.equals(Constants.ACTION_CONNECTION_STATE_AUTH)
                || action.equals(Constants.ACTION_CONNECTION_STATE_NO_NETWORK)
                || action.equals(Constants.ACTION_CONNECTION_STATE_NO_CONNECTION)) {

            // Go to the status screen if an error has occurred from a previously
            // working connection or no connection at all. Additionally show the
            // error message
            if (connectionStatus.equals(Constants.ACTION_CONNECTION_STATE_OK) || 
                    connectionStatus.equals(Constants.ACTION_CONNECTION_STATE_UNKNOWN)) {

                // Show a textual description about the connection state
                if (connectionStatus.equals(Constants.ACTION_CONNECTION_STATE_SERVER_DOWN)) {
                    showMessage(getString(R.string.err_connect));
                } else if (connectionStatus.equals(Constants.ACTION_CONNECTION_STATE_LOST)) {
                    showMessage(getString(R.string.err_con_lost));
                } else if (connectionStatus.equals(Constants.ACTION_CONNECTION_STATE_TIMEOUT)) {
                    showMessage(getString(R.string.err_con_timeout));
                } else if (connectionStatus.equals(Constants.ACTION_CONNECTION_STATE_REFUSED) || 
                        connectionStatus.equals(Constants.ACTION_CONNECTION_STATE_AUTH)) {
                    showMessage(getString(R.string.err_auth));
                } else if (connectionStatus.equals(Constants.ACTION_CONNECTION_STATE_NO_NETWORK)) {
                    showMessage(getString(R.string.err_no_network));
                } else if (connectionStatus.equals(Constants.ACTION_CONNECTION_STATE_NO_CONNECTION)) {
                    showMessage(getString(R.string.no_connection_available));
                }

                runOnUiThread(new Runnable() {
                    public void run() {
                        channelLoadingList.clear();
                        showDrawerMenu();
                        connectionStatus = action;
                        handleMenuSelection(MENU_STATUS);
                    }
                });
            }

        } else if (action.equals(Constants.ACTION_SHOW_MESSAGE)) {
            final String msg = (String) obj;
            showMessage(msg);
        }
    }

    private void showMessage(final String msg) {
        SnackbarManager.show(Snackbar.with(this)
                .type(SnackbarType.MULTI_LINE)
                .text(msg));
    }

    /**
     * Depending on the connection state and server capabilities show or hide
     * certain navigation menu items.
     * 
     * @param show
     */
    private void showDrawerMenu() {
        boolean show = connectionStatus.equals(Constants.ACTION_CONNECTION_STATE_OK);

        // Enable the main menus in the drawer
        drawerAdapter.getItemById(MENU_CHANNELS).isVisible = show;
        drawerAdapter.getItemById(MENU_COMPLETED_RECORDINGS).isVisible = show;
        drawerAdapter.getItemById(MENU_SCHEDULED_RECORDINGS).isVisible = show;
        drawerAdapter.getItemById(MENU_FAILED_RECORDINGS).isVisible = show;
        drawerAdapter.getItemById(MENU_PROGRAM_GUIDE).isVisible = show;

        // Only show the series recording menu if the server supports it
        drawerAdapter.getItemById(MENU_SERIES_RECORDINGS).isVisible = (show && (app
                .getProtocolVersion() >= Constants.MIN_API_VERSION_SERIES_RECORDINGS));

        // Only show the timer recording menu if the server supports it and the
        // application is unlocked
        drawerAdapter.getItemById(MENU_TIMER_RECORDINGS).isVisible = (show
                && (app.getProtocolVersion() >= Constants.MIN_API_VERSION_TIMER_RECORDINGS)
                && app.isUnlocked());

        // Show the menu item to unlock the application if it was not yet purchased
        drawerAdapter.getItemById(MENU_UNLOCKER).isVisible = !app.isUnlocked();

        // TODO Remove the connection from the menu list and string arrays 
        // Hide the connection menu
        drawerAdapter.getItemById(MENU_CONNECTIONS).isVisible = false;

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
                if (f instanceof ProgramGuidePagerFragment && f instanceof FragmentControlInterface) {
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
                if (f instanceof ProgramGuidePagerFragment && f instanceof FragmentControlInterface) {
                    ((FragmentControlInterface) f).setSelection(
                            programGuideListPosition,
                            programGuideListPositionOffset);
                }
            }
            break;
        }
    }

    @Override
    public void noDataAvailable(Channel channel, String tag) {
        // NOP
    }

    @Override
    public void moreDataRequired(final Channel channel, final String tag) {
        // Do not load a channel when it is already being loaded to avoid
        // loading the same or too many data.
        if (app.isLoading() || channel == null || channelLoadingList.contains(channel)) {
            return;
        }
        channelLoadingList.add(channel);
        loadMorePrograms();
    }

    /**
     * Loads more data for a channel. It is only loaded when there are loading
     * requests pending and no other request is already executed.
     */
    private void loadMorePrograms() {
        // Skip if no channels are available for loading or if a channel is
        // already being loaded
        if (!channelLoadingList.isEmpty() && !isLoadingChannels) {
            final Channel ch = channelLoadingList.get(0);
            if (!app.isChannelBlocked(ch)) {
                isLoadingChannels = true;
                setActionBarSubtitle(getString(R.string.loading_channel, ch.name));
                Utils.loadMorePrograms(this, ch);
            }
        }
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
                Bundle bundle = new Bundle();
                bundle.putLong(Constants.BUNDLE_CHANNEL_ID, channel.id);
                bundle.putBoolean(Constants.BUNDLE_DUAL_PANE, isDualPane);

                if (isDualPane) {
                    showFragment(ProgramListFragment.class.getName(), R.id.right_fragment, bundle);
                } else {
                    showFragment(ProgramListFragment.class.getName(), R.id.main_fragment, bundle, true);
                }
            }
            break;

        case MENU_PROGRAM_GUIDE:
            // If a channel was selected in the program guide screen, start
            // playing the selected channel
            if (channel != null) {
                Intent intent = new Intent(this, ExternalPlaybackActivity.class);
                intent.putExtra(Constants.BUNDLE_CHANNEL_ID, channel.id);
                startActivity(intent);
            }
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
        }
        // When a recording has been selected from the recording list fragment,
        // show its details. In dual mode these are shown in a separate fragment
        // to the right of the recording list, otherwise replace the recording
        // list with the details fragment.
        if (recording != null) {
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
                if (f instanceof ChannelListFragment && f instanceof FragmentControlInterface) {
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
                if (f instanceof CompletedRecordingListFragment && f instanceof FragmentControlInterface) {
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
                if (f instanceof ScheduledRecordingListFragment && f instanceof FragmentControlInterface) {
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
                if (f instanceof SeriesRecordingListFragment && f instanceof FragmentControlInterface) {
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
                if (f instanceof TimerRecordingListFragment && f instanceof FragmentControlInterface) {
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
                if (f instanceof FailedRecordingListFragment && f instanceof FragmentControlInterface) {
                    ((FragmentControlInterface) f).setInitialSelection(failedRecordingListPosition);
                }
            }
            break;

        case MENU_PROGRAM_GUIDE:
            // When the program guide is done loading set the previously
            // selected position of the program guide. The program guide
            // fragment will inform us via the scrolling interface methods where
            // the channel list shall be scrolled to.
            final Fragment f = getSupportFragmentManager().findFragmentById(R.id.main_fragment);
            if (f instanceof ProgramGuidePagerFragment && f instanceof FragmentControlInterface) {
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
            if (f instanceof ChannelListFragment && f instanceof FragmentControlInterface) {
                ((FragmentControlInterface) f).reloadData();
            }
            break;

        case MENU_PROGRAM_GUIDE:
            // Inform the channel list fragment to clear all data from its
            // channel list and show only the channels with the selected tag
            final Fragment cf = getSupportFragmentManager().findFragmentById(R.id.program_guide_channel_fragment);
            if (cf instanceof ChannelListFragment && cf instanceof FragmentControlInterface) {
                ((FragmentControlInterface) cf).reloadData();
            }
            // Additionally inform the program guide fragment to clear all data
            // from its list and show only the programs of the channels that are
            // part of the selected tag
            final Fragment pgf = getSupportFragmentManager().findFragmentById(R.id.main_fragment);
            if (pgf instanceof ProgramGuidePagerFragment && pgf instanceof FragmentControlInterface) {
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
            if ((f instanceof SeriesRecordingListFragment || f instanceof TimerRecordingListFragment)
                    && f instanceof FragmentControlInterface) {
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
    public boolean onQueryTextSubmit(String text) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            searchMenuItem.collapseActionView();
        }
        return false;
    }

    @SuppressLint("NewApi")
    @Override
    public boolean onSuggestionClick(int position) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            searchMenuItem.collapseActionView();
        }
        return false;
    }

    @Override
    public boolean onSuggestionSelect(int position) {
        return false;
    }
}
