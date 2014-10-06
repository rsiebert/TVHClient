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
import org.tvheadend.tvhclient.fragments.SeriesRecordingListFragment;
import org.tvheadend.tvhclient.fragments.StatusFragment;
import org.tvheadend.tvhclient.interfaces.ActionBarInterface;
import org.tvheadend.tvhclient.interfaces.FragmentControlInterface;
import org.tvheadend.tvhclient.interfaces.FragmentScrollInterface;
import org.tvheadend.tvhclient.interfaces.FragmentStatusInterface;
import org.tvheadend.tvhclient.interfaces.HTSListener;
import org.tvheadend.tvhclient.model.Channel;
import org.tvheadend.tvhclient.model.DrawerMenuItem;
import org.tvheadend.tvhclient.model.Program;
import org.tvheadend.tvhclient.model.Recording;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.ViewDragHelper;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;

public class MainActivity extends ActionBarActivity implements ChangeLogDialogInterface, ActionBarInterface, FragmentStatusInterface, FragmentScrollInterface, HTSListener {

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
    private int menuPosition = MENU_UNKNOWN;
    private int defaultMenuPosition = MENU_UNKNOWN;
    private int channelListPosition = 0;
    private int completedRecordingListPosition = 0;
    private int scheduledRecordingListPosition = 0;
    private int seriesRecordingListPosition = 0;
    private int failedRecordingListPosition = 0;
    private int programGuideListPosition = 0;
    private int programGuideListPositionOffset = 0;

    // The index for the navigation drawer menus
    private static final int MENU_UNKNOWN = -1;
    private static final int MENU_CHANNELS = 0;
    private static final int MENU_COMPLETED_RECORDINGS = 1;
    private static final int MENU_SCHEDULED_RECORDINGS = 2;
    private static final int MENU_SERIES_RECORDINGS = 3;
    private static final int MENU_FAILED_RECORDINGS = 4;
    private static final int MENU_PROGRAM_GUIDE = 5;
    private static final int MENU_STATUS = 6;
    private static final int MENU_SETTINGS = 7;
    private static final int MENU_CONNECTIONS = 8;

    // Holds the stack of menu items
    public ArrayList<Integer> menuStack = new ArrayList<Integer>();

    // Indicates that a loading channel data is in progress, the next channel
    // can only be loaded when this variable is false
    private boolean isLoadingChannels = false;

    // Holds a list of channels that are currently being loaded
    public List<Channel> channelLoadingList = new ArrayList<Channel>();

    // Holds the number of EPG entries for each channel
    public Map<Long, Integer> channelEpgCountList = new HashMap<Long, Integer>();

    // If the saved instance is not null then we return from an orientation
    // change and we do not require to recreate the fragments
    private boolean orientationChangeOccurred = false;

    // When the main activity starts for the first time and no connections are
    // configured or active the connection settings activity will be shown once.
    // This variable controls that. 
    private boolean connectionSettingsShown = false;

    // When the main activity starts for the first time the change log dialog is
    // shown. The setting that it was shown is stored when the activity is quit.
    // So we need to save the information here. 
    private boolean changeLogDialogShown = false;
    
    // Contains the information about the current connection state.
    private String connectionStatus = Constants.ACTION_CONNECTION_STATE_OK;

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
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayUseLogoEnabled(Utils.showChannelIcons(this));
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerList = (ListView) findViewById(R.id.left_drawer);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        defaultMenuPosition = Integer.parseInt(prefs.getString("defaultMenuPositionPref", String.valueOf(MENU_STATUS)));
        
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
                R.drawable.ic_drawer,
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

        // Create the custom adapter for the menus in the navigation drawer.
        // Also set the listener to react to the user selection.
        drawerAdapter = new DrawerMenuAdapter(this, getDrawerMenu(), R.layout.drawer_list_item);
        drawerList.setAdapter(drawerAdapter);
        drawerList.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final DrawerMenuItem item = drawerAdapter.getItem(position);
                // We can't just use the list position for the menu position
                // because the list might contain separators. So we need to get
                // the id if the list item which is the menu position. 
                if (item != null) {
                    if (item.id != MENU_UNKNOWN && menuPosition != item.id) {
                        menuStack.add(menuPosition);
                        handleMenuSelection(item.id);
                    }
                }
            }
        });

        // If the saved instance is not null then we return from an orientation
        // change. The drawer menu could be open, so update the recording
        // counts. Also get any saved values from the bundle.
        if (savedInstanceState != null) {
            updateDrawerMenu();
            menuStack = savedInstanceState.getIntegerArrayList(Constants.MENU_STACK);
            menuPosition = savedInstanceState.getInt(Constants.MENU_POSITION, MENU_UNKNOWN);
            channelListPosition = savedInstanceState.getInt(Constants.CHANNEL_LIST_POSITION, 0);
            completedRecordingListPosition = savedInstanceState.getInt(Constants.COMPLETED_RECORDING_LIST_POSITION, 0);
            scheduledRecordingListPosition = savedInstanceState.getInt(Constants.SCHEDULED_RECORDING_LIST_POSITION, 0);
            seriesRecordingListPosition = savedInstanceState.getInt(Constants.SERIES_RECORDING_LIST_POSITION, 0);
            failedRecordingListPosition = savedInstanceState.getInt(Constants.FAILED_RECORDING_LIST_POSITION, 0);
            connectionStatus = savedInstanceState.getString(Constants.BUNDLE_CONNECTION_STATUS);
        }
    }

    /**
     * 
     */
    private void updateDrawerMenu() {
        TVHClientApplication app = (TVHClientApplication) getApplication();
        drawerAdapter.getItem(MENU_COMPLETED_RECORDINGS).count = 
                app.getRecordings(Constants.RECORDING_TYPE_COMPLETED).size();
        drawerAdapter.getItem(MENU_SCHEDULED_RECORDINGS).count = 
                app.getRecordings(Constants.RECORDING_TYPE_SCHEDULED).size();
        drawerAdapter.getItem(MENU_FAILED_RECORDINGS).count = 
                app.getRecordings(Constants.RECORDING_TYPE_FAILED).size();
        drawerAdapter.notifyDataSetChanged();
    }

    /**
     * 
     * @return
     */
    private List<DrawerMenuItem> getDrawerMenu() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        final boolean lightTheme = prefs.getBoolean("lightThemePref", true);

        String[] menuItems = getResources().getStringArray(R.array.pref_menu_names);

        List<DrawerMenuItem> list = new ArrayList<DrawerMenuItem>();
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
        list.add(new DrawerMenuItem(MENU_FAILED_RECORDINGS, menuItems[4],
                (lightTheme) ? R.drawable.ic_menu_failed_recordings_light
                        : R.drawable.ic_menu_failed_recordings_dark));
        list.add(new DrawerMenuItem(MENU_PROGRAM_GUIDE, menuItems[5],
                (lightTheme) ? R.drawable.ic_menu_program_guide_light
                        : R.drawable.ic_menu_program_guide_dark));
        list.add(new DrawerMenuItem(MENU_STATUS, menuItems[6],
                (lightTheme) ? R.drawable.ic_menu_status_light : R.drawable.ic_menu_status_dark));

        list.add(new DrawerMenuItem());
        list.add(new DrawerMenuItem(MENU_SETTINGS, menuItems[7],
                (lightTheme) ? R.drawable.ic_menu_settings_light : R.drawable.ic_menu_settings_dark));
        list.add(new DrawerMenuItem(MENU_CONNECTIONS, menuItems[8],
                (lightTheme) ? R.drawable.ic_menu_connections_light
                        : R.drawable.ic_menu_connections_dark));

        return list;
    }

    @Override
    public void onBackPressed() {
        // Get the current fragment and check if it is a program list fragment.
        // If yes then we need to pop the stack so we can see the channel list
        // fragment again.
        final Fragment f = getSupportFragmentManager().findFragmentById(R.id.main_fragment);

        if (!isDualPane && (f instanceof ProgramListFragment)) {
            getSupportFragmentManager().popBackStack();
        } else {
            if (menuStack.size() > 0) {
                // Show the previous menu item and the this as the new current
                // menu back to the stack
                handleMenuSelection(menuStack.remove(menuStack.size() - 1));
            } else {
                super.onBackPressed();
            }
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // Get the connection status
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        connectionStatus = prefs.getString(Constants.LAST_CONNECTION_STATE, Constants.ACTION_CONNECTION_STATE_OK);
    }

    @Override
    public void onPostResume() {
        super.onPostResume();
        // Show the change log once when the application was upgraded. Otherwise
        // start normally. This is handled in the other method.
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
     * case after the user has left the setting screen, go to the status screen
     * instead. If a connection is present and selected, register so we can
     * listen for connection status updates. Then connect to the server but do
     * not reload all data.
     */
    private void reconnectAndResume() {
        if (DatabaseHelper.getInstance() != null
                && (DatabaseHelper.getInstance().getConnections().isEmpty() 
                        || DatabaseHelper.getInstance().getSelectedConnection() == null)) {
            // No connection is present or active
            final int menu = (connectionSettingsShown) ? MENU_STATUS : MENU_CONNECTIONS;
            connectionSettingsShown = true;
            handleMenuSelection(menu);
        } else {
            // Connection exists and is active
            TVHClientApplication app = (TVHClientApplication) getApplication();
            app.addListener(this);
            Utils.connect(this, false);

            // Show the contents of the last selected menu position. In case it
            // is not set, use the the default one defined in the settings
            int pos = (menuPosition == MENU_UNKNOWN) ? defaultMenuPosition : menuPosition;
            
            // If the connection status is fine go to the previous selected
            // connection or the default. If it contains a state then show
            // the status fragment.
            handleMenuSelection((connectionStatus.equals(Constants.ACTION_CONNECTION_STATE_OK)) ? pos : MENU_STATUS);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        TVHClientApplication app = (TVHClientApplication) getApplication();
        app.removeListener(this);
        
        // Save the previously active connection status
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(Constants.LAST_CONNECTION_STATE, connectionStatus);
        editor.commit();
    }

//    @Override
//    public void onDestroy() {
//        Log.i(TAG, "onDestroy");
//
//        // Remove all listeners so no one receives any status information
//        TVHClientApplication app = (TVHClientApplication) getApplication();
//        app.removeListeners();
//        // stop the service when the application is closed
//        Intent intent = new Intent(this, HTSService.class);
//        stopService(intent);
//
//        super.onDestroy();
//    }

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
        outState.putInt(Constants.MENU_POSITION, menuPosition);
        outState.putInt(Constants.CHANNEL_LIST_POSITION, channelListPosition);
        outState.putInt(Constants.COMPLETED_RECORDING_LIST_POSITION, completedRecordingListPosition);
        outState.putInt(Constants.SCHEDULED_RECORDING_LIST_POSITION, scheduledRecordingListPosition);
        outState.putInt(Constants.SERIES_RECORDING_LIST_POSITION, seriesRecordingListPosition);
        outState.putInt(Constants.FAILED_RECORDING_LIST_POSITION, failedRecordingListPosition);
        outState.putString(Constants.BUNDLE_CONNECTION_STATUS, connectionStatus);
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // If the navigation drawer is open, hide all menu items
        boolean drawerOpen = drawerLayout.isDrawerOpen(drawerList);
        for (int i = 0; i < menu.size(); i++) {
            menu.getItem(i).setVisible(!drawerOpen);
        }
        // No search is available in the these menus
        if (menuPosition == MENU_STATUS 
                || menuPosition == MENU_COMPLETED_RECORDINGS 
                || menuPosition == MENU_SCHEDULED_RECORDINGS
                || menuPosition == MENU_FAILED_RECORDINGS ) {
            (menu.findItem(R.id.menu_search)).setVisible(false);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main_menu, menu);
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
            onSearchRequested();
            return true;

        case R.id.menu_refresh:
            channelLoadingList.clear();
            channelEpgCountList.clear();
            TVHClientApplication app = (TVHClientApplication) getApplication();
            app.unblockAllChannels();

            // Reconnect to the server and reload all data
            Utils.connect(this, true);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * This method is called when the user wants to search for programs. In case
     * the channel list is visible search only within the selected channel,
     * otherwise search within the entire available program data.
     */
    @Override
    public boolean onSearchRequested() {
        Bundle bundle = new Bundle();
        if (!isDualPane) {
            // If dual pane is not active and the program list is only shown,
            // search only the current channel, otherwise if the channel list is
            // shown search all channels
            final Fragment f = getSupportFragmentManager().findFragmentById(R.id.main_fragment);
            if (f instanceof ProgramListFragment && f instanceof FragmentControlInterface) {
                // Get the selected channel, to limit the search to this channel
                Object o = ((FragmentControlInterface) f).getSelectedItem();
                if (o instanceof Channel) {
                    bundle.putLong(Constants.BUNDLE_CHANNEL_ID, ((Channel) o).id);
                }
            }
        }
        startSearch(null, false, bundle, false);
        return true;
    }

    /**
     * Creates the fragment with the given name and shows it on the given
     * layout. If a bundle was given, it will be passed to the fragment.
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
     * Before the fragments are shown by the menu selection ensure that the
     * correct layout weights are determined and set.
     * 
     * @param position
     */
    private void preHandleMenuSelection(int position) {
        // Set the correct weights of the main layouts
        setLayoutWeights(position);

        // Do not recreate the fragments if an orientation change occurred.
        if (orientationChangeOccurred) {
            orientationChangeOccurred = false;
            return;
        }
        // Highlight the selected item and close the drawer if its open
        if (drawerLayout.isDrawerOpen(drawerList)) {
            drawerList.setItemChecked(position, true);
            drawerLayout.closeDrawer(drawerList);
        }

        // Remove any previously active fragment on the right side. In case the
        // connection can't be established the screen is blank. The main
        // fragment does not need to be removed, because it will be replaced
        // with a fragment from the selected menu.
        if (isDualPane) {
            Fragment rf = getSupportFragmentManager().findFragmentById(R.id.right_fragment);
            if (rf != null) {
                getSupportFragmentManager().beginTransaction().remove(rf).commit();
            }
        }

        // Remove any channel list fragment that was available in the layout for
        // the program guide. When another menu item is called this layout will
        // be set invisible.
        Fragment cf = getSupportFragmentManager().findFragmentById(R.id.program_guide_channel_fragment);
        if (cf != null) {
            getSupportFragmentManager().beginTransaction().remove(cf).commit();
        }
    }

    /**
     * When a menu item from the navigation drawer was selected this method is
     * called. It loads and shows the correct fragment depending on the selected
     * menu item.
     * 
     * @param position
     */
    private void handleMenuSelection(int position) {
        preHandleMenuSelection(position);

        // Save the menu position so we know which one was selected
        menuPosition = position;

        // Update the drawer menu so the correct item gets highlighted
        drawerAdapter.setPosition(menuPosition);
        drawerAdapter.notifyDataSetChanged();

        Bundle bundle = new Bundle();

        switch (position) {
        case MENU_CHANNELS:
            // Show the channel list fragment. In this case it shall not be used
            // in the program guide, so pass over that information
            bundle.putBoolean(Constants.BUNDLE_DUAL_PANE, isDualPane);
            showFragment(ChannelListFragment.class.getName(), R.id.main_fragment, null);
            break;

        case MENU_COMPLETED_RECORDINGS:
            // Show completed recordings
            bundle.putBoolean(Constants.BUNDLE_DUAL_PANE, isDualPane);
            showFragment(CompletedRecordingListFragment.class.getName(), R.id.main_fragment, bundle);
            break;

        case MENU_SCHEDULED_RECORDINGS:
            // Show scheduled recordings
            bundle.putBoolean(Constants.BUNDLE_DUAL_PANE, isDualPane);
            showFragment(ScheduledRecordingListFragment.class.getName(), R.id.main_fragment, bundle);
            break;

        case MENU_SERIES_RECORDINGS:
            // Show series recordings
            bundle.putBoolean(Constants.BUNDLE_DUAL_PANE, isDualPane);
            showFragment(SeriesRecordingListFragment.class.getName(), R.id.main_fragment, bundle);
            break;

        case MENU_FAILED_RECORDINGS:
            // Show failed recordings
            bundle.putBoolean(Constants.BUNDLE_DUAL_PANE, isDualPane);
            showFragment(FailedRecordingListFragment.class.getName(), R.id.main_fragment, bundle);
            break;

        case MENU_PROGRAM_GUIDE:
            // Show the program guide on the right side
            showFragment(ProgramGuidePagerFragment.class.getName(), R.id.main_fragment, null);
            break;

        case MENU_STATUS:
            // Show the status fragment
            bundle.putString(Constants.BUNDLE_CONNECTION_STATUS, connectionStatus);
            showFragment(StatusFragment.class.getName(), R.id.main_fragment, bundle);
            break;

        case MENU_SETTINGS:
            // Show the settings
            menuPosition = defaultMenuPosition;
            Intent settingsIntent = new Intent(this, SettingsActivity.class);
            startActivityForResult(settingsIntent, Constants.RESULT_CODE_SETTINGS);
            break;

        case MENU_CONNECTIONS:
            // Show the connections
            menuPosition = defaultMenuPosition;
            Intent connIntent = new Intent(this, SettingsActivity.class);
            connIntent.putExtra(Constants.BUNDLE_MANAGE_CONNECTIONS, true);
            startActivityForResult(connIntent, Constants.RESULT_CODE_SETTINGS);
            break;
        }
    }

    /**
     * Sets the different weights of the three available layouts from the
     * mainLayout.xml file depending on the menu selection. The availability of
     * the layouts are dependent on the on the screen size.
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

        // This is the layout for the main content. It is always available
        FrameLayout mainLayout = (FrameLayout) findViewById(R.id.main_fragment);
        if (mainLayout != null) {
            LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) mainLayout.getLayoutParams();
            layoutParams.weight = mainLayoutWeight;
        }
        // This is the layout for the details on the right side. It is always
        // available on large tablets and on smaller tablets in landscape mode.
        FrameLayout rightLayout = (FrameLayout) findViewById(R.id.right_fragment);
        if (rightLayout != null) {
            LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) rightLayout.getLayoutParams();
            layoutParams.weight = rightLayoutWeight;
        }
    }

    /**
     * This method is called when an activity has quit and was called with
     * startActivityForResult and this one is now the active one again.
     * Depending on the given request and result code certain action can be
     * done.
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Constants.RESULT_CODE_SETTINGS) {
            if (resultCode == RESULT_OK) {
                // Restart the activity because certain settings have changed
                // like the theme. The reload will be done in the onResume
                // method
                if (data.getBooleanExtra(Constants.BUNDLE_RESTART, false)) {
                    Intent intent = getIntent();
                    finish();
                    startActivity(intent);
                } else {
                    // Reconnect to the server and reload all data if a
                    // connection or some values of the existing one have
                    // changed.
                    if (data.getBooleanExtra(Constants.BUNDLE_RECONNECT, false)) {
                        Utils.connect(this, true);
                        Intent intent = getIntent();
                        finish();
                        startActivity(intent);
                    }
                }
            }
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
                        actionBar.setSubtitle(R.string.loading);
                        // Remove any fragments on the right during update to
                        // prevent seeing old data. These fragments could be the
                        // program list or the recording details. 
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
            // If a channel has been updated (usually by a call to load more
            // data) remove it from the loading queue and continue loading the
            // next one.
            runOnUiThread(new Runnable() {
                public void run() {
                    final Channel ch = (Channel) obj;

                    // Check that the number of EPG entries has changed
                    if (ch != null && ch.epg != null) {
                        if (channelEpgCountList.containsKey(ch.id)) {
                            // If the EPG count has not changed, block the
                            // channel. Otherwise unblock it
                            if (ch.epg.size() == channelEpgCountList.get(ch.id)) {
                                TVHClientApplication app = (TVHClientApplication) getApplication();
                                app.blockChannel(ch);
                            } else {
                                channelEpgCountList.put(ch.id, ch.epg.size());
                                TVHClientApplication app = (TVHClientApplication) getApplication();
                                app.unblockChannel(ch);
                            }
                        } else {
                            channelEpgCountList.put(ch.id, ch.epg.size());
                        }
                    }

                    channelLoadingList.remove(ch);
                    isLoadingChannels = false;

                    if (!channelLoadingList.isEmpty()) {
                        loadMorePrograms();
                    } else {
                        // Display the number of items because the loading is
                        // done. We need to get the number from the program
                        // guide pager fragment because it holds the channel
                        // fragment which in turn knows the channel count. 
                        Fragment f = getSupportFragmentManager().findFragmentById(R.id.main_fragment);
                        if (f != null && f instanceof ProgramGuidePagerFragment && f instanceof FragmentControlInterface) {
                            int count = ((FragmentControlInterface) f).getItemCount();
                            actionBar.setSubtitle(count + " " + getString(R.string.items));
                        }
                    }
                }
            });
        } else if (action.equals(Constants.ACTION_CONNECTION_STATE_OK)) {
            runOnUiThread(new Runnable() {
                public void run() {
                    connectionStatus = action;
                    // Enable the drawer again
                    drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
                    actionBar.setHomeButtonEnabled(true);
                }
            });
        } else if (action.equals(Constants.ACTION_CONNECTION_STATE_SERVER_DOWN)
                || action.equals(Constants.ACTION_CONNECTION_STATE_LOST)
                || action.equals(Constants.ACTION_CONNECTION_STATE_TIMEOUT)
                || action.equals(Constants.ACTION_CONNECTION_STATE_REFUSED)
                || action.equals(Constants.ACTION_CONNECTION_STATE_AUTH)) {
            // Only show the new connection status if the last status was fine
            if (connectionStatus.equals(Constants.ACTION_CONNECTION_STATE_OK)) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        connectionStatus = action;
                        channelLoadingList.clear();
                        handleMenuSelection(MENU_STATUS);
                        // Disable the drawer
                        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
                        actionBar.setHomeButtonEnabled(false);
                    }
                });
            }
        }
    }

    @Override
    public void setActionBarTitle(final String title, final String tag) {
        if (actionBar != null) {
            actionBar.setTitle(title);
        }
    }

    @Override
    public void setActionBarSubtitle(final String subtitle, final String tag) {
        if (actionBar != null) {
            actionBar.setSubtitle(subtitle);
        }
    }

    @Override
    public void setActionBarIcon(final Bitmap bitmap, final String tag) {
        if (actionBar != null && bitmap != null) {
            actionBar.setIcon(new BitmapDrawable(getResources(), bitmap));
        }
    }

    @Override
    public void setActionBarIcon(final int resource, final String tag) {
        if (actionBar != null) {
            actionBar.setIcon(resource);
        }
    }

    @Override
    public void onScrollingChanged(final int position, final int offset, final String tag) {
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
    }

    @Override
    public void onScrollStateIdle(final String tag) {
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
    }

    @Override
    public void noDataAvailable(Channel channel, String tag) {
        // NOP
    }

    @Override
    public void moreDataRequired(final Channel channel, final String tag) {
        // Do not load a channel when it is already being loaded to avoid
        // loading the same or too many data.
        TVHClientApplication app = (TVHClientApplication) getApplication();
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
            TVHClientApplication app = (TVHClientApplication) getApplication();
            if (!app.isChannelBlocked(ch)) {
                isLoadingChannels = true;
                if (actionBar != null) {
                    actionBar.setSubtitle(getString(R.string.loading_channel, ch.name));
                }
                Utils.loadMorePrograms(this, ch);
            }
        }
    }

    @Override
    public void onListItemSelected(final int position, final Channel channel, final String tag) {
        switch (menuPosition) {
        case MENU_CHANNELS:
            // Save the position of the selected channel so it can be restored
            // after an orientation change
            channelListPosition = position;

            // Show the program list fragment, in dual pane mode show the
            // program list on the right side of the channel list, otherwise
            // replace the channel list.
            if (channel != null) {
                // Create the fragment with the required information
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
        }
    }

    @Override
    public void onListItemSelected(final int position, final Recording recording, final String tag) {
        // Save the position of the selected recording type so it can be
        // restored after an orientation change
        switch (menuPosition) {
        case MENU_COMPLETED_RECORDINGS:
            completedRecordingListPosition = position;
            break;
        case MENU_SCHEDULED_RECORDINGS:
            scheduledRecordingListPosition = position;
            break;
        case MENU_SERIES_RECORDINGS:
            seriesRecordingListPosition = position;
            break;
        case MENU_FAILED_RECORDINGS:
            failedRecordingListPosition = position;
            break;
        }
        // When a recording has been selected from the recording list fragment,
        // show its details. In dual mode they are shown as a separate fragment
        // to the right of the recording list, otherwise replace the recording
        // list with the details fragment.
        if (recording != null) {
            Bundle args = new Bundle();
            args.putLong(Constants.BUNDLE_RECORDING_ID, recording.id);
            args.putBoolean(Constants.BUNDLE_SHOW_CONTROLS, !isDualPane);
            
            if (isDualPane) {
                // Create and show the fragment
                showFragment(RecordingDetailsFragment.class.getName(), R.id.right_fragment, args);
            } else {
                // Create the fragment and show it as a dialog.
                DialogFragment newFragment = RecordingDetailsFragment.newInstance(args);
                newFragment.show(getSupportFragmentManager(), "dialog");
            }
        }
    }

    @Override
    public void onListItemSelected(final int position, final Program program, final String tag) {
        if (program != null) {
            Bundle args = new Bundle();
            args.putLong(Constants.BUNDLE_PROGRAM_ID, program.id);
            args.putLong(Constants.BUNDLE_CHANNEL_ID, program.channel.id);
            args.putBoolean(Constants.BUNDLE_SHOW_CONTROLS, true);
            // Create the fragment and show it as a dialog.
            DialogFragment newFragment = ProgramDetailsFragment.newInstance(args);
            newFragment.show(getSupportFragmentManager(), "dialog");
        }
    }

    @Override
    public void onListPopulated(final String tag) {
        switch (menuPosition) {
        case MENU_CHANNELS:
            // When the channel list fragment is done loading and dual pane is 
            // active, preselect a channel so that the program list on the right
            // will be shown for that channel.
            if (isDualPane) {
                if (tag.equals(ChannelListFragment.class.getSimpleName())) { 
                    final Fragment f = getSupportFragmentManager().findFragmentById(R.id.main_fragment);
                    if (f instanceof ChannelListFragment && f instanceof FragmentControlInterface) {
                        ((FragmentControlInterface) f).setInitialSelection(channelListPosition);
                    }
                }
            }
            break;

        case MENU_COMPLETED_RECORDINGS:
            // When the recording list fragment is done loading and dual pane is
            // active, preselect a recording from the list to show the details
            // of this recording on the right side.
            if (isDualPane) {
                final Fragment f = getSupportFragmentManager().findFragmentById(R.id.main_fragment);
                if (f instanceof CompletedRecordingListFragment && f instanceof FragmentControlInterface) {
                    ((FragmentControlInterface) f).setInitialSelection(completedRecordingListPosition);
                }
            }
            break;

        case MENU_SCHEDULED_RECORDINGS:
            // When the recording list fragment is done loading and dual pane is
            // active, preselect a recording from the list to show the details
            // of this recording on the right side.
            if (isDualPane) {
                final Fragment f = getSupportFragmentManager().findFragmentById(R.id.main_fragment);
                if (f instanceof ScheduledRecordingListFragment && f instanceof FragmentControlInterface) {
                    ((FragmentControlInterface) f).setInitialSelection(scheduledRecordingListPosition);
                }
            }
            break;

        case MENU_SERIES_RECORDINGS:
            // When the recording list fragment is done loading and dual pane is
            // active, preselect a recording from the list to show the details
            // of this recording on the right side.
            if (isDualPane) {
                final Fragment f = getSupportFragmentManager().findFragmentById(R.id.main_fragment);
                if (f instanceof SeriesRecordingListFragment && f instanceof FragmentControlInterface) {
                    ((FragmentControlInterface) f).setInitialSelection(seriesRecordingListPosition);
                }
            }
            break;

        case MENU_FAILED_RECORDINGS:
            // When the recording list fragment is done loading and dual pane is
            // active, preselect a recording from the list to show the details
            // of this recording on the right side.
            if (isDualPane) {
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
        switch (menuPosition) {
        case MENU_CHANNELS:
            // Clear all data from the channel list fragment and show only the
            // channels with the selected tag
            final Fragment f = getSupportFragmentManager().findFragmentById(R.id.main_fragment);
            if (f instanceof ChannelListFragment && f instanceof FragmentControlInterface) {
                ((FragmentControlInterface) f).reloadData();
            }
            break;

        case MENU_PROGRAM_GUIDE:
            // Clear all data from the channel list fragment and show only the
            // channels with the selected tag
            final Fragment cf = getSupportFragmentManager().findFragmentById(R.id.program_guide_channel_fragment);
            if (cf instanceof ChannelListFragment && cf instanceof FragmentControlInterface) {
                ((FragmentControlInterface) cf).reloadData();
            }
            // Clear all data from the program guide list fragment and show 
            // only the program data of the channels with the selected tag
            final Fragment pgf = getSupportFragmentManager().findFragmentById(R.id.main_fragment);
            if (pgf instanceof ProgramGuidePagerFragment && pgf instanceof FragmentControlInterface) {
                ((FragmentControlInterface) pgf).reloadData();
            }
            break;
        }
    }
}
