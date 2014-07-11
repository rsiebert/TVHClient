package org.tvheadend.tvhclient;

import org.tvheadend.tvhclient.ChangeLogDialog.ChangeLogDialogInterface;
import org.tvheadend.tvhclient.fragments.ChannelListFragment;
import org.tvheadend.tvhclient.fragments.CompletedRecordingListFragment;
import org.tvheadend.tvhclient.fragments.FailedRecordingListFragment;
import org.tvheadend.tvhclient.fragments.ProgramListFragment;
import org.tvheadend.tvhclient.fragments.RecordingDetailsFragment;
import org.tvheadend.tvhclient.fragments.ScheduledRecordingListFragment;
import org.tvheadend.tvhclient.htsp.HTSService;
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
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class MainActivity extends ActionBarActivity implements ChangeLogDialogInterface, ActionBarInterface, FragmentStatusInterface {

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
    private int[] selectedRecordingListPosition = { 0, 0, 0 };

    // Strings that determine the navigation drawer menu position and the list
    // positions so it can be applied when the orientation has changed.
    private static final String SELECTED_DRAWER_MENU_POSITION = "selected_drawer_menu_position";
    private static final String SELECTED_CHANNEL_LIST_POSITION = "selected_channel_list_position";
    private static final String SELECTED_PROGRAM_LIST_POSITION = "selected_program_list_position";
    private static final String SELECTED_RECORDING_LIST_POSITION = "selected_recording_list_position";

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

        // Get any saved values from the bundle
        if (savedInstanceState != null) {
            Log.d(TAG, "savedInstanceState != null");
            // Get the previously selected list item positions
            selectedDrawerMenuPosition = savedInstanceState.getInt(SELECTED_DRAWER_MENU_POSITION, 0);
            selectedChannelListPosition = savedInstanceState.getInt(SELECTED_CHANNEL_LIST_POSITION, 0);
            selectedProgramListPosition = savedInstanceState.getInt(SELECTED_PROGRAM_LIST_POSITION, 0);
            selectedRecordingListPosition = savedInstanceState.getIntArray(SELECTED_RECORDING_LIST_POSITION);
        }
        
        Log.d(TAG, "savedInstanceState getting channel " + selectedChannelListPosition);
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
                Utils.connect(this, false);
                handleItemSelection(selectedDrawerMenuPosition);
            }
        }
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy");

        // Remove all listeners so no one receives any status information
        TVHClientApplication app = (TVHClientApplication) getApplication();
        app.removeListeners();
        // stop the service when the application is closed
        Intent intent = new Intent(this, HTSService.class);
        stopService(intent);

        super.onDestroy();
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
        drawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Log.d(TAG, "onSaveInstanceState");

        // Save the position of the selected menu item from the drawer
        outState.putInt(SELECTED_DRAWER_MENU_POSITION, selectedDrawerMenuPosition);
        outState.putInt(SELECTED_CHANNEL_LIST_POSITION, selectedChannelListPosition);
        outState.putInt(SELECTED_PROGRAM_LIST_POSITION, selectedProgramListPosition);
        outState.putIntArray(SELECTED_RECORDING_LIST_POSITION, selectedRecordingListPosition);

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
        Log.i(TAG, "handleItemSelection " + position);
        selectedDrawerMenuPosition = position;
        
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
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main_fragment, f)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .commit();
            
            if (isDualPane) {
                f = getSupportFragmentManager().findFragmentById(R.id.right_fragment);
                if (f != null) {
                    Log.d(TAG, "remove right fragment");
                    getSupportFragmentManager().beginTransaction().remove(f).commit();
                }
            }
            
            break;

        case MENU_COMPLETED_RECORDINGS:
            // Show completed recordings
            f = Fragment.instantiate(this, CompletedRecordingListFragment.class.getName());
            bundle.putBoolean(Constants.BUNDLE_DUAL_PANE, isDualPane);
            f.setArguments(bundle);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main_fragment, f)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .commit();
            break;

        case MENU_SCHEDULED_RECORDINGS:
            // Show scheduled recordings
            f = Fragment.instantiate(this, ScheduledRecordingListFragment.class.getName());
            bundle.putBoolean(Constants.BUNDLE_DUAL_PANE, isDualPane);
            f.setArguments(bundle);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main_fragment, f)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .commit();
            break;

        case MENU_FAILED_RECORDINGS:
            // Show failed recordings
            f = Fragment.instantiate(this, FailedRecordingListFragment.class.getName());
            bundle.putBoolean(Constants.BUNDLE_DUAL_PANE, isDualPane);
            f.setArguments(bundle);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main_fragment, f)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .commit();
            break;

        case MENU_PROGRAM_GUIDE:
            // Show the program guide
            // Show the channel list on the left side
            f = Fragment.instantiate(this, ChannelListFragment.class.getName());
            bundle.putBoolean(Constants.BUNDLE_SHOWS_ONLY_CHANNELS, true);
            f.setArguments(bundle);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main_fragment, f)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .commit();
            
            // Show the program list on the right side
            
            
            break;

        case MENU_STATUS:
            // Show the status fragment
            Fragment sf = Fragment.instantiate(this, StatusFragment.class.getName());
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main_fragment, sf)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .commit();
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

        // Highlight the selected item and close the drawer
        drawerList.setItemChecked(position, true);
        drawerLayout.closeDrawer(drawerList);
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
        Log.d(TAG, "showCreateConnectionDialog");

        // Show confirmation dialog to cancel
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.create_new_connections));
        builder.setTitle(getString(R.string.no_connections));

        // Define the action of the yes button
        builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                Log.d(TAG, "onClick yes");
                // Show the manage connections activity where
                // the user can choose a connection
                Intent intent = new Intent(ctx, SettingsAddConnectionActivity.class);
                startActivityForResult(intent, Constants.RESULT_CODE_CONNECTIONS);
            }
        });
        // Define the action of the no button
        builder.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                Log.d(TAG, "onClick no");
                dialog.cancel();
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    /**
     * Called when the change log dialog is closed.
     * Show the channel list as a default.
     */
    @Override
    public void dialogDismissed() {
        Log.d(TAG, "dialogDismissed");

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
    public void setActionBarIcon(int resource, String tag) {
        if (actionBar != null) {
            boolean showIcon = Utils.showChannelIcons(this);
            actionBar.setDisplayUseLogoEnabled(showIcon);
            if (showIcon) {
                actionBar.setIcon(resource);
            }
        }
    }

    @Override
    public void onScrollingChanged(int position, int pos, String tag) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void onScrollStateIdle(String tag) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void moreDataRequired(Channel channel, String tag) {
        Utils.loadMorePrograms(this, channel);
    }

    @Override
    public void noDataAvailable(Channel channel, String tag) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void onListItemSelected(int position, Channel channel, String tag) {
        Log.d(TAG, "onListItemSelected (channel), pos " + position + ", tag " + tag);

        if (tag.equals(ChannelListFragment.class.getSimpleName())) {
            selectedChannelListPosition = position;
            if (channel != null) {
                // Create the program list fragment with the required information
                Fragment f = Fragment.instantiate(this, ProgramListFragment.class.getName());
                Bundle bundle = new Bundle();
                bundle.putLong(Constants.BUNDLE_CHANNEL_ID, channel.id);
                bundle.putBoolean(Constants.BUNDLE_DUAL_PANE, isDualPane);
                f.setArguments(bundle);

                // When a channel has been selected we need to show the list of
                // programs that belong to that channel. In single view mode we
                // replace the channel list fragment with the program list. But
                // in dual pane mode we show it on the right side.
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
        }
    }

    @Override
    public void onListItemSelected(int position, Recording recording, String tag) {
        Log.d(TAG, "onListItemSelected (recording), pos " + position + ", tag " + tag);

        if (tag.equals(CompletedRecordingListFragment.class.getSimpleName())) {
            selectedRecordingListPosition[0] = position;
        } else if (tag.equals(ScheduledRecordingListFragment.class.getSimpleName())) {
            selectedRecordingListPosition[1] = position;
        } else if (tag.equals(FailedRecordingListFragment.class.getSimpleName())) {
            selectedRecordingListPosition[2] = position;
        } 

        if (recording != null) {
            // Create the recording detail fragment with the required
            // information
            Fragment f = Fragment.instantiate(this, RecordingDetailsFragment.class.getName());
            Bundle args = new Bundle();
            args.putLong(Constants.BUNDLE_RECORDING_ID, recording.id);
            args.putBoolean(Constants.BUNDLE_DUAL_PANE, isDualPane);
            f.setArguments(args);

            // When a recording has been selected we need to show the
            // details of that selected recording. In single view mode we
            // replace the recording list fragment with the recording
            // details. But in dual pane mode we show it on the right side.
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
    public void onListItemSelected(int position, Program program, String tag) {
        Log.d(TAG, "onListItemSelected (program), pos " + position + ", tag " + tag);
        
        if (tag.equals(ProgramListFragment.class.getSimpleName())) {
            selectedProgramListPosition = position;
            if (program != null) {             
                // When a program was selected we need to show the details.
                Toast.makeText(this, "Showing Program Details", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onListPopulated(String tag) {
        Log.d(TAG, "onListPopulated, tag " + tag);
        
        if (tag.equals(ChannelListFragment.class.getSimpleName())) {
            // When the channel list fragment is done loading for the first time
            // and we show the program list next to it (dual pane mode) we need
            // to select a channel from the list so that the program list on the
            // right will be loaded for that channel.
            if (isDualPane) {
                Fragment f = getSupportFragmentManager().findFragmentById(R.id.main_fragment);
                if (f instanceof FragmentControlInterface) {
                    Log.d(TAG, "setInitialSelection, pos " + selectedChannelListPosition);
                    ((FragmentControlInterface) f).setInitialSelection(selectedChannelListPosition);
                }
            }
        } else {
            // When the recording list fragment is done loading for the first
            // time and we show the recording details next to it (dual pane mode
            // and wanted) we need to select a recording from the list so that
            // the recording details on the right will be loaded for that
            // recording.
            if (isDualPane) {
                Fragment f = getSupportFragmentManager().findFragmentById(R.id.main_fragment);
                if (f instanceof FragmentControlInterface) {
                    if (tag.equals(CompletedRecordingListFragment.class.getSimpleName())) {
                        ((FragmentControlInterface) f).setInitialSelection(selectedRecordingListPosition[0]);
                    } else if (tag.equals(ScheduledRecordingListFragment.class.getSimpleName())) {
                        ((FragmentControlInterface) f).setInitialSelection(selectedRecordingListPosition[1]);
                    } else if (tag.equals(FailedRecordingListFragment.class.getSimpleName())) {
                        ((FragmentControlInterface) f).setInitialSelection(selectedRecordingListPosition[2]);
                    }
                }
            }
        }
    }
}
