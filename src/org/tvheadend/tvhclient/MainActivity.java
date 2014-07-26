package org.tvheadend.tvhclient;

import java.util.ArrayList;
import java.util.List;

import org.tvheadend.tvhclient.ChangeLogDialog.ChangeLogDialogInterface;
import org.tvheadend.tvhclient.fragments.ChannelListFragment;
import org.tvheadend.tvhclient.fragments.CompletedRecordingListFragment;
import org.tvheadend.tvhclient.fragments.FailedRecordingListFragment;
import org.tvheadend.tvhclient.fragments.ProgramGuideListFragment;
import org.tvheadend.tvhclient.fragments.ProgramGuidePagerFragment;
import org.tvheadend.tvhclient.fragments.ProgramListFragment;
import org.tvheadend.tvhclient.fragments.RecordingDetailsFragment;
import org.tvheadend.tvhclient.fragments.ScheduledRecordingListFragment;
import org.tvheadend.tvhclient.fragments.StatusFragment;
import org.tvheadend.tvhclient.htsp.HTSListener;
import org.tvheadend.tvhclient.interfaces.ActionBarInterface;
import org.tvheadend.tvhclient.interfaces.FragmentControlInterface;
import org.tvheadend.tvhclient.interfaces.FragmentStatusInterface;
import org.tvheadend.tvhclient.model.Channel;
import org.tvheadend.tvhclient.model.Program;
import org.tvheadend.tvhclient.model.Recording;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

public class MainActivity extends ActionBarActivity implements ChangeLogDialogInterface, ActionBarInterface, FragmentStatusInterface, HTSListener {

    private final static String TAG = MainActivity.class.getSimpleName();

    private ListView drawerList;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;

    private ActionBar actionBar = null;
    private ChangeLogDialog changeLogDialog;

    // Indication weather the layout supports two fragments. This is usually
    // only available on tablets.
    private boolean isDualPane = false;

    // Default navigation drawer menu position and the list positions
    private int selectedDrawerMenuPosition = MENU_CHANNELS;
    private int selectedChannelListPosition = 0;
    private int selectedProgramListPosition = 0;
    private int selectedCompletedRecordingListPosition = 0;
    private int selectedScheduledRecordingListPosition = 0;
    private int selectedFailedRecordingListPosition = 0;
    private int selectedProgramGuideListPosition = 0;
    private int selectedProgramGuideListPositionOffset = 0;

    // Strings that determine the navigation drawer menu position and the list
    // positions so it can be applied when the orientation has changed.
    private static final String SELECTED_DRAWER_MENU_POSITION = "selected_drawer_menu_position";
    private static final String SELECTED_CHANNEL_LIST_POSITION = "selected_channel_list_position";
    private static final String SELECTED_PROGRAM_LIST_POSITION = "selected_program_list_position";
    private static final String SELECTED_COMPLETED_RECORDING_LIST_POSITION = "selected_completed_recording_list_position";
    private static final String SELECTED_SCHEDULED_RECORDING_LIST_POSITION = "selected_scheduled_recording_list_position";
    private static final String SELECTED_FAILED_RECORDING_LIST_POSITION = "selected_failed_recording_list_position";

    // The index for the navigation drawer menus 
    private static final int MENU_CHANNELS = 0;
    private static final int MENU_COMPLETED_RECORDINGS = 1;
    private static final int MENU_SCHEDULED_RECORDINGS = 2;
    private static final int MENU_FAILED_RECORDINGS = 3;
    private static final int MENU_PROGRAM_GUIDE = 4;
    private static final int MENU_STATUS = 5;
    private static final int MENU_SETTINGS = 6;
    private static final int MENU_CONNECTIONS = 7;
    private static final int MENU_RELOAD = 8;

    // Indicates that a loading is in progress, the next channel can only be
    // loaded when this variable is false
    private boolean isLoadingChannels = false;

    // Holds a list of channels that are currently being loaded
    public List<Channel> channelLoadingList = new ArrayList<Channel>();

    private boolean orientationChangeOccurred = true;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(Utils.getThemeId(this));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_layout);
        Utils.setLanguage(this);
        Log.i(TAG, "onCreate");

        // Check if the layout supports dual view (two fragments at the same
        // time). This is usually available on tablets
        View v = findViewById(R.id.right_fragment);
        isDualPane = v != null && v.getVisibility() == View.VISIBLE;

        DatabaseHelper.init(this.getApplicationContext());
        changeLogDialog = new ChangeLogDialog(this);

        // Setup action bar for tabs
        actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayUseLogoEnabled(Utils.showChannelIcons(this));

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerList = (ListView) findViewById(R.id.left_drawer);
 
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Boolean lightTheme = prefs.getBoolean("lightThemePref", true);
        drawerList.setBackgroundColor((lightTheme) ? 
                getResources().getColor(R.color.menu_background_color_light) : 
                    getResources().getColor(R.color.menu_background_color_dark));
        
        // Listens for drawer open and close events so we can modify the
        // contents of the action bar when the drawer is visible, such as to
        // change the title and remove action items that are contextual to the
        // main content.
        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout,
                R.drawable.ic_drawer, R.string.drawer_open,
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
            }
        };
        // Set the drawer toggle as the DrawerListener
        drawerLayout.setDrawerListener(drawerToggle);

        // Create the custom adapter for the menus in the navigation drawer.
        // Also set the listener to react to the user selection.
        String[] drawerMenuArray = getResources().getStringArray(R.array.drawer_menu_names);
        drawerList.setAdapter(new ArrayAdapter<String>(this, R.layout.drawer_list_item, drawerMenuArray));
        drawerList.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.i(TAG, "onItemClick " + position + ", " + selectedDrawerMenuPosition);
                if (selectedDrawerMenuPosition != position) {
                    handleItemSelection(position);
                }
            }
        });

        // If the saved instance is not null then we return from an orientation
        // change and we do not require to recreate the fragments
        orientationChangeOccurred  = (savedInstanceState == null);
        
        // Get any saved values from the bundle
        if (savedInstanceState != null) {
            selectedDrawerMenuPosition = savedInstanceState.getInt(SELECTED_DRAWER_MENU_POSITION, 0);
            selectedChannelListPosition = savedInstanceState.getInt(SELECTED_CHANNEL_LIST_POSITION, 0);
            selectedProgramListPosition = savedInstanceState.getInt(SELECTED_PROGRAM_LIST_POSITION, 0);
            selectedCompletedRecordingListPosition = savedInstanceState.getInt(SELECTED_COMPLETED_RECORDING_LIST_POSITION);
            selectedScheduledRecordingListPosition = savedInstanceState.getInt(SELECTED_SCHEDULED_RECORDING_LIST_POSITION);
            selectedFailedRecordingListPosition = savedInstanceState.getInt(SELECTED_FAILED_RECORDING_LIST_POSITION);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG, "onResume");

        // Show the change log once when the application was upgraded. Otherwise
        // start normally by checking if a connection has been added. If not the
        // show the dialog to add one or connect to the service and set the
        // selected navigation drawer item. 
        if (changeLogDialog.firstRun()) {
            changeLogDialog.getLogDialog().show();
        } else {
            if (DatabaseHelper.getInstance() != null
                    && DatabaseHelper.getInstance().getConnections().isEmpty()) {
                actionBar.setSubtitle(getString(R.string.no_connections));
                showCreateConnectionDialog(this);
            } else {
                TVHClientApplication app = (TVHClientApplication) getApplication();
                app.addListener(this);
                Utils.connect(this, false);
                handleItemSelection(selectedDrawerMenuPosition);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        TVHClientApplication app = (TVHClientApplication) getApplication();
        app.removeListener(this);
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
        drawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // Save the position of the selected menu item from the drawer
        outState.putInt(SELECTED_DRAWER_MENU_POSITION, selectedDrawerMenuPosition);
        outState.putInt(SELECTED_CHANNEL_LIST_POSITION, selectedChannelListPosition);
        outState.putInt(SELECTED_PROGRAM_LIST_POSITION, selectedProgramListPosition);
        outState.putInt(SELECTED_COMPLETED_RECORDING_LIST_POSITION, selectedCompletedRecordingListPosition);
        outState.putInt(SELECTED_SCHEDULED_RECORDING_LIST_POSITION, selectedScheduledRecordingListPosition);
        outState.putInt(SELECTED_FAILED_RECORDING_LIST_POSITION, selectedFailedRecordingListPosition);
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // If the navigation drawer is open, hide certain action items related
        // to the content view
        boolean drawerOpen = drawerLayout.isDrawerOpen(drawerList);
        // Hide certain menu items when the drawer is open
        MenuItem searchMenuItem = menu.findItem(R.id.menu_search);
        if (searchMenuItem != null) {
            searchMenuItem.setVisible(!drawerOpen);
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

        default:
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Loads and shows the correct fragment depending on the selected navigation
     * menu item.
     * 
     * @param position
     */
    private void handleItemSelection(int position) {
        Log.d(TAG, "handleItemSelection " + position);
        selectedDrawerMenuPosition = position;

        // TODO improve
        setLayoutWeights(position);

        Log.d(TAG, "orientationChangeOccurred " + orientationChangeOccurred);
        if (!orientationChangeOccurred) {
            orientationChangeOccurred = true;
            return;
        }

        // Highlight the selected item and close the drawer
        drawerList.setItemChecked(position, true);
        drawerLayout.closeDrawer(drawerList);

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

        Bundle bundle = new Bundle();
        Fragment f = null;

        switch (position) {
        case MENU_CHANNELS:
            // Show the channel list fragment. In this case it shall not be used
            // in the program guide, so pass over that information
            f = Fragment.instantiate(this, ChannelListFragment.class.getName());
            bundle.putBoolean(Constants.BUNDLE_SHOWS_ONLY_CHANNELS, false);
            f.setArguments(bundle);
            getSupportFragmentManager().beginTransaction().replace(R.id.main_fragment, f)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE).commit();
            break;

        case MENU_COMPLETED_RECORDINGS:
            // Show completed recordings
            f = Fragment.instantiate(this, CompletedRecordingListFragment.class.getName());
            bundle.putBoolean(Constants.BUNDLE_DUAL_PANE, isDualPane);
            f.setArguments(bundle);
            getSupportFragmentManager().beginTransaction().replace(R.id.main_fragment, f)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE).commit();
            break;

        case MENU_SCHEDULED_RECORDINGS:
            // Show scheduled recordings
            f = Fragment.instantiate(this, ScheduledRecordingListFragment.class.getName());
            bundle.putBoolean(Constants.BUNDLE_DUAL_PANE, isDualPane);
            f.setArguments(bundle);
            getSupportFragmentManager().beginTransaction().replace(R.id.main_fragment, f)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE).commit();
            break;

        case MENU_FAILED_RECORDINGS:
            // Show failed recordings
            f = Fragment.instantiate(this, FailedRecordingListFragment.class.getName());
            bundle.putBoolean(Constants.BUNDLE_DUAL_PANE, isDualPane);
            f.setArguments(bundle);
            getSupportFragmentManager().beginTransaction().replace(R.id.main_fragment, f)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE).commit();
            break;

        case MENU_PROGRAM_GUIDE:
            // Show the list of channels on the left side
            f = Fragment.instantiate(this, ChannelListFragment.class.getName());
            bundle.putBoolean(Constants.BUNDLE_SHOWS_ONLY_CHANNELS, true);
            f.setArguments(bundle);
            getSupportFragmentManager().beginTransaction().replace(R.id.main_fragment, f)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE).commit();
            
            // Show the program guide on the right side
            f = Fragment.instantiate(this, ProgramGuidePagerFragment.class.getName());
            getSupportFragmentManager().beginTransaction().replace(R.id.right_fragment, f)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE).commit();
            break;

        case MENU_STATUS:
            // Show the status fragment
            Fragment sf = Fragment.instantiate(this, StatusFragment.class.getName());
            getSupportFragmentManager().beginTransaction().replace(R.id.main_fragment, sf)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE).commit();
            break;

        case MENU_SETTINGS:
            // Show the settings
            selectedDrawerMenuPosition = MENU_CHANNELS;
            Intent settingsIntent = new Intent(this, SettingsActivity.class);
            startActivityForResult(settingsIntent, Constants.RESULT_CODE_SETTINGS);
            break;

        case MENU_CONNECTIONS:
            // Show the connections
            selectedDrawerMenuPosition = MENU_CHANNELS;
            Intent connIntent = new Intent(this, SettingsManageConnectionsActivity.class);
            startActivityForResult(connIntent, Constants.RESULT_CODE_CONNECTIONS);
            break;

        case MENU_RELOAD:
            // Reload the data
            Utils.connect(this, true);
            break;
        }
    }
    
    /**
     * 
     * @param position
     */
    private void setLayoutWeights(int position) {
        // The default weights
        float mainLayoutWeight = 0;
        float rightLayoutWeight = 0;
        
        // Set the different weights depending on the menu selection. The
        // availability of the layouts are handled by the layout files in the
        // layout folders.
        switch (position) {
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
        case MENU_FAILED_RECORDINGS:

            if (isDualPane) {
                mainLayoutWeight = 5;
                rightLayoutWeight = 5;
            } else {
                rightLayoutWeight = 1;
            }
            break;

        case MENU_PROGRAM_GUIDE:
            // Get the width of the screen so set the weight as a pixel value
            DisplayMetrics displaymetrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
            int displayWidth = displaymetrics.widthPixels;

            mainLayoutWeight = 84;
            rightLayoutWeight = displayWidth - 84;
            break;

        case MENU_STATUS:
            mainLayoutWeight = 1;
            break;

        default:
            break;
        }

        // This is the layout for the main content
        FrameLayout mainLayout = (FrameLayout) findViewById(R.id.main_fragment);
        if (mainLayout != null) {
            LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) mainLayout.getLayoutParams();
            layoutParams.weight = mainLayoutWeight;
        }
        // This is the layout for the details on the right side
        FrameLayout rightLayout = (FrameLayout) findViewById(R.id.right_fragment);
        if (rightLayout != null) {
            LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) rightLayout.getLayoutParams();
            layoutParams.weight = rightLayoutWeight;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult " + requestCode + ", " + resultCode);

        if (requestCode == Constants.RESULT_CODE_CONNECTIONS) {
            // Reload all data from the server because the connection or some
            // values of the existing connection has changed
            if (resultCode == RESULT_OK) {
                if (data.getBooleanExtra(Constants.BUNDLE_RECONNECT, false)) {
                    Utils.connect(this, true);
                }
            }
        } else if (requestCode == Constants.RESULT_CODE_SETTINGS) {
            // Reload all data from the server because the connection or some
            // values of the existing connection has changed. Also restart the
            // activity if certain settings have changed like the theme.
            if (resultCode == RESULT_OK) {
                if (data.getBooleanExtra(Constants.BUNDLE_RECONNECT, false)) {
                    Utils.connect(this, true);
                }
                if (data.getBooleanExtra(Constants.BUNDLE_RESTART, false)) {
                    Intent intent = getIntent();
                    finish();
                    startActivity(intent);
                }
            }
        }
    }

    /**
     * Shows a dialog to the user where he can choose to go directly to the
     * connection screen. This dialog is only shown after the start of the
     * application when no connection is available.
     */
    private void showCreateConnectionDialog(final Context ctx) {
        // Show confirmation dialog to cancel
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.create_new_connections));
        builder.setTitle(getString(R.string.no_connections));

        // Define the action of the yes button
        builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // Show the manage connections activity where
                // the user can choose a connection
                Intent intent = new Intent(ctx, SettingsAddConnectionActivity.class);
                startActivityForResult(intent, Constants.RESULT_CODE_CONNECTIONS);
            }
        });
        // Define the action of the no button
        builder.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    /**
     * This method is part of the HTSListener interface. Whenever the HTSService
     * sends a new message the correct action will then be executed here.
     */
    @Override
    public void onMessage(final String action, final Object obj) {
        if (action.equals(TVHClientApplication.ACTION_CHANNEL_UPDATE)) {
            runOnUiThread(new Runnable() {
                public void run() {
                    final Channel channel = (Channel) obj;
                    Log.i(TAG, "removing from queue: channel '" + channel.name);
                    channelLoadingList.remove(channel);
                    isLoadingChannels = false;
                    loadMorePrograms();
                }
            });
        } 
    }

    /**
     * Called when the change log dialog is closed.
     * Show the channel list as a default.
     */
    @Override
    public void dialogDismissed() {
        if (DatabaseHelper.getInstance() != null
                && DatabaseHelper.getInstance().getConnections().isEmpty()) {
            actionBar.setSubtitle(getString(R.string.no_connections));
            showCreateConnectionDialog(this);
        } else {
            Utils.connect(this, false);
            handleItemSelection(MENU_STATUS);
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
        if (actionBar != null) {
            boolean showIcon = Utils.showChannelIcons(this);
            actionBar.setDisplayUseLogoEnabled(showIcon);
            if (showIcon) {
                actionBar.setIcon(new BitmapDrawable(getResources(), bitmap));
            }
        }
    }

    @Override
    public void setActionBarIcon(final int resource, final String tag) {
        if (actionBar != null) {
            boolean showIcon = Utils.showChannelIcons(this);
            actionBar.setDisplayUseLogoEnabled(showIcon);
            if (showIcon) {
                actionBar.setIcon(resource);
            }
        }
    }

    @Override
    public void onScrollingChanged(final int position, final int offset, final String tag) {
        // Execute the required actions depending on the selected menu
        switch (selectedDrawerMenuPosition) {
        case MENU_PROGRAM_GUIDE:
            // Save the scroll values so they can be reused after an orientation change.
            selectedProgramGuideListPosition = position;
            selectedProgramGuideListPositionOffset = offset;

            if (tag.equals(ProgramGuideListFragment.class.getSimpleName())) {
                // Scrolling was initiated by the program guide fragment. Keep
                // the channel list in sync by scrolling it to the same position
                final Fragment f = getSupportFragmentManager().findFragmentById(R.id.main_fragment);
                if (f instanceof ChannelListFragment && f instanceof FragmentControlInterface) {
                    ((FragmentControlInterface) f).setSelectionFromTop(position, offset);
                }
            } else if (tag.equals(ChannelListFragment.class.getSimpleName())) {
                // Scrolling was initiated by the channel list fragment. Keep
                // the currently visible program guide list in sync by scrolling
                // it to the same position
                final Fragment f = getSupportFragmentManager().findFragmentById(R.id.right_fragment);
                if (f instanceof ProgramGuidePagerFragment && f instanceof FragmentControlInterface) {
                    ((FragmentControlInterface) f).setSelectionFromTop(position, offset);
                }
            }
            break;
        }
    }

    @Override
    public void onScrollStateIdle(final String tag) {
        // Execute the required actions depending on the selected menu
        switch (selectedDrawerMenuPosition) {
        case MENU_PROGRAM_GUIDE:
            if (tag.equals(ProgramGuideListFragment.class.getSimpleName()) || tag.equals(ChannelListFragment.class.getSimpleName())) {
                // Scrolling stopped by the program guide or the channel list
                // fragment. Scroll all program guide fragments in the current
                // view pager to the same position.
                final Fragment f = getSupportFragmentManager().findFragmentById(R.id.right_fragment);
                if (f instanceof ProgramGuidePagerFragment && f instanceof FragmentControlInterface) {
                    ((FragmentControlInterface) f).setSelectionFromTop(
                            selectedProgramGuideListPosition,
                            selectedProgramGuideListPositionOffset);
                }
            }
            break;
        }
    }

    @Override
    public void moreDataRequired(final Channel channel, final String tag) {
        Log.i(TAG, "moreDataRequired " + tag);

        // Do not load a channel when it is already being loaded to avoid
        // loading the same or too many data.
        if (channel == null || channelLoadingList.contains(channel)) {
            return;
        }
        Log.i(TAG, "adding to queue: channel '" + channel.name);
        channelLoadingList.add(channel);
        loadMorePrograms();
    }
    
    /**
     * 
     */
    private void loadMorePrograms() {
        Log.i(TAG, "startLoadingPrograms");
        // Ignore the channel if its is already being loaded
        if (!channelLoadingList.isEmpty() && !isLoadingChannels) {
            final Channel ch = channelLoadingList.get(0);
            isLoadingChannels = true;
            Log.i(TAG, "loading data: channel " + ch.name);
            Utils.loadMorePrograms(this, ch);
        }
    }

    @Override
    public void noDataAvailable(final Channel channel, final String tag) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void onListItemSelected(final int position, final Channel channel, final String tag) {
        // Execute the required actions depending on the selected menu
        switch (selectedDrawerMenuPosition) {
        case MENU_CHANNELS:
            selectedChannelListPosition = position;
            
            // When a channel item has been selected show the programs of that
            // channel. In dual mode they are shown next to the channel list,
            // otherwise replace the channel list.
            if (channel != null) {
                // Create the fragment with the required information
                final Fragment f = Fragment.instantiate(this, ProgramListFragment.class.getName());
                Bundle bundle = new Bundle();
                bundle.putLong(Constants.BUNDLE_CHANNEL_ID, channel.id);
                bundle.putBoolean(Constants.BUNDLE_DUAL_PANE, isDualPane);
                f.setArguments(bundle);
                
                // Show the fragment
                if (isDualPane) {
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.right_fragment, f)
                            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                            .commit();
                } else {
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.main_fragment, f)
                            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                            .commit();
                }
            }
            break;
        }
    }

    @Override
    public void onListItemSelected(final int position, final Recording recording, final String tag) {
        // Save the position of the selected recording depending of the type
        switch (selectedDrawerMenuPosition) {
        case MENU_COMPLETED_RECORDINGS:
            selectedCompletedRecordingListPosition = position;
            break;
        case MENU_SCHEDULED_RECORDINGS:
            selectedScheduledRecordingListPosition = position;
            break;
        case MENU_FAILED_RECORDINGS:
            selectedFailedRecordingListPosition = position;
            break;
        }

        // When a recording item has been selected show its recording details of
        // it. In dual mode they are shown next to the recording list, otherwise
        // replace the recording list.
        if (recording != null) {
            // Create the fragment with the required information
            final Fragment f = Fragment.instantiate(this, RecordingDetailsFragment.class.getName());
            Bundle args = new Bundle();
            args.putLong(Constants.BUNDLE_RECORDING_ID, recording.id);
            args.putBoolean(Constants.BUNDLE_DUAL_PANE, isDualPane);
            f.setArguments(args);

            // Show the fragment
            if (isDualPane) {
                getSupportFragmentManager().beginTransaction().replace(R.id.right_fragment, f)
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE).commit();
            } else {
                getSupportFragmentManager().beginTransaction().replace(R.id.main_fragment, f)
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE).commit();
            }
        }
    }

    @Override
    public void onListItemSelected(final int position, final Program program, final String tag) {
        Log.d(TAG, "onListItemSelected (program), pos " + position + ", tag " + tag);
        
        // A program was selected, show its details.
        selectedProgramListPosition = position;
        if (program != null) {
            Toast.makeText(this, "Showing Program Details for '" + program.title + "'", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onListPopulated(final String tag) {
        // Execute the required actions depending on the selected menu
        switch (selectedDrawerMenuPosition) {
        case MENU_CHANNELS:
            // When the channel list fragment is done loading for the first time
            // and we show the program list next to it (dual pane mode) we need
            // to select a channel from the list so that the program list on the
            // right will be loaded for that channel.
            if (isDualPane) {
                final Fragment f = getSupportFragmentManager().findFragmentById(R.id.main_fragment);
                if (f instanceof ChannelListFragment && f instanceof FragmentControlInterface) {
                    ((FragmentControlInterface) f).setInitialSelection(selectedChannelListPosition);
                }
            }
            break;

        case MENU_COMPLETED_RECORDINGS:
            // When the recording list fragment is done loading and dual pane is
            // active, show the details of the first or previously selected
            // recording in the right side.
            if (isDualPane) {
                final Fragment f = getSupportFragmentManager().findFragmentById(R.id.main_fragment);
                if (f instanceof CompletedRecordingListFragment && f instanceof FragmentControlInterface) {
                    ((FragmentControlInterface) f).setInitialSelection(selectedCompletedRecordingListPosition);
                }
            }
            break;

        case MENU_SCHEDULED_RECORDINGS:
            // When the recording list fragment is done loading and dual pane is
            // active, show the details of the first or previously selected
            // recording in the right side.
            if (isDualPane) {
                final Fragment f = getSupportFragmentManager().findFragmentById(R.id.main_fragment);
                if (f instanceof ScheduledRecordingListFragment && f instanceof FragmentControlInterface) {
                    ((FragmentControlInterface) f).setInitialSelection(selectedScheduledRecordingListPosition);
                }
            }
            break;
                
        case MENU_FAILED_RECORDINGS:
            // When the recording list fragment is done loading and dual pane is
            // active, show the details of the first or previously selected
            // recording in the right side.
            if (isDualPane) {
                final Fragment f = getSupportFragmentManager().findFragmentById(R.id.main_fragment);
                if (f instanceof FailedRecordingListFragment && f instanceof FragmentControlInterface) {
                    ((FragmentControlInterface) f).setInitialSelection(selectedFailedRecordingListPosition);
                }
            }
            break;

        case MENU_PROGRAM_GUIDE:
            // Set the selected position of the program guide
            final Fragment f = getSupportFragmentManager().findFragmentById(R.id.main_fragment);
            if (f instanceof ProgramGuideListFragment && f instanceof FragmentControlInterface) {
                Log.d(TAG, "setInitialSelection, pos " + selectedChannelListPosition);
                ((FragmentControlInterface) f).setInitialSelection(selectedChannelListPosition);
            }
            break;
        }
    }

    @Override
    public void channelTagChanged(final String tag) {
        // Execute the required actions depending on the selected menu
        switch (selectedDrawerMenuPosition) {
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
            final Fragment cf = getSupportFragmentManager().findFragmentById(R.id.main_fragment);
            if (cf instanceof ChannelListFragment && cf instanceof FragmentControlInterface) {
                ((FragmentControlInterface) cf).reloadData();
            }
            // Clear all data from the program guide list fragment and show 
            // only the program data of the channels with the selected tag
            final Fragment pgf = getSupportFragmentManager().findFragmentById(R.id.right_fragment);
            if (pgf instanceof ProgramGuideListFragment && pgf instanceof FragmentControlInterface) {
                ((FragmentControlInterface) pgf).reloadData();
            }
            break;
        }
    }
}
