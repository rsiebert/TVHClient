package org.tvheadend.tvhclient;

import org.tvheadend.tvhclient.ChangeLogDialog.ChangeLogDialogInterface;
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

public class MainActivity extends ActionBarActivity implements ChangeLogDialogInterface, ActionBarInterface, FragmentStatusInterface {

    private final static String TAG = MainActivity.class.getSimpleName();

    private ListView drawerList;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;
    private CharSequence drawerTitle;
    private CharSequence mainTitle;
    private CharSequence mainSubTitle;

    private ActionBar actionBar = null;
    private ChangeLogDialog changeLogDialog;

    // Indication weather the layout supports two fragments. This is usually
    // only available on tablets.
    private boolean isDualPane = false;

    // Saves the selected navigation drawer menu position so it can be
    // reselected when the orientation has changed.
    private int selectedDrawerMenuPosition = 0;

    private static final String SELECTED_DRAWER_MENU_POSITION = "selected_drawer_menu_position";
    private static final String MAIN_FRAGMENT_TAG = "main_fragment";
    private static final String RIGHT_FRAGMENT_TAG = "right_fragment";

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

        // Save the current title of the action bar
        mainTitle = drawerTitle = getTitle();
        mainSubTitle = "";

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
                actionBar.setTitle(mainTitle);
                actionBar.setSubtitle(mainSubTitle);
                supportInvalidateOptionsMenu();
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                mainTitle = actionBar.getTitle();
                mainSubTitle = actionBar.getSubtitle();
                actionBar.setTitle(drawerTitle);
                actionBar.setSubtitle("");
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
                handleItemSelection(position);
            }
        });

        // Get any saved values from the bundle
        if (savedInstanceState != null) {
            // Get the previously selected channel item position
            selectedDrawerMenuPosition = savedInstanceState.getInt(SELECTED_DRAWER_MENU_POSITION, 0);
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
                Utils.connect(this, false);
                handleItemSelection(MENU_STATUS);
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
        // When the orientation changes from landscape to portrait the right
        // fragment would crash because the container is null. So we remove
        // it entirely before the orientation change happens.
        final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        Fragment f = getSupportFragmentManager().findFragmentByTag(RIGHT_FRAGMENT_TAG);
        if (f != null) {
            ft.remove(f);
            ft.commit();
        }

        super.onSaveInstanceState(outState);
        // Save the position of the selected menu item from the drawer
        outState.putInt(SELECTED_DRAWER_MENU_POSITION, selectedDrawerMenuPosition);
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

        // Remove any previously active fragment on the right side. In case the
        // connection can't be established the screen is blank.
        if (isDualPane) {
            Fragment f = getSupportFragmentManager().findFragmentByTag(RIGHT_FRAGMENT_TAG);
            if (f != null) {
                getSupportFragmentManager().beginTransaction().remove(f).commit();
            }
        }

        switch (position) {
        case MENU_CHANNELS:
            // Show the channel list
            Fragment clf = Fragment.instantiate(this, ChannelListFragment.class.getName());
            // Inform the channel list fragment if it shall be used in the
            // program guide view
            Bundle bundle = new Bundle();
            bundle.putBoolean(Constants.BUNDLE_SHOWS_ONLY_CHANNELS, false);
            clf.setArguments(bundle);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main_fragment, clf, MAIN_FRAGMENT_TAG)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE).commit();
            break;
        case MENU_COMPLETED_RECORDINGS:
            // Show completed recordings
            break;
        case MENU_SCHEDULED_RECORDINGS:
            // Show scheduled recordings
            break;
        case MENU_FAILED_RECORDINGS:
            // Show failed recordings
            break;
        case MENU_PROGRAM_GUIDE:
            // Show the program guide
            break;
        case MENU_STATUS:
            // Show the status
            Fragment sf = Fragment.instantiate(this, StatusFragment.class.getName());
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main_fragment, sf, MAIN_FRAGMENT_TAG)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .commit();
            break;

        case MENU_SETTINGS:
            // Show the settings
            Intent settingsIntent = new Intent(this, SettingsActivity.class);
            startActivityForResult(settingsIntent, Constants.RESULT_CODE_SETTINGS);
            break;

        case MENU_CONNECTIONS:
            // Show the connections
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
        mainTitle = title;

        // TODO differentiate according to the tags
        if (actionBar != null) {
            actionBar.setTitle(title);
        }
    }

    @Override
    public void setActionBarSubtitle(final String subtitle, final String tag) {
        mainSubTitle = subtitle;

        // TODO differentiate according to the tags
        if (actionBar != null) {
            actionBar.setSubtitle(subtitle);
        }
    }

    @Override
    public void setActionBarIcon(final Channel channel, final String tag) {
        // TODO differentiate according to the tags
        if (actionBar != null && channel != null) {
            // Show or hide the channel icon if required
            boolean showIcon = Utils.showChannelIcons(this);
            actionBar.setDisplayUseLogoEnabled(showIcon);
            if (showIcon) {
                actionBar.setIcon(new BitmapDrawable(getResources(), channel.iconBitmap));
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
            if (channel != null) {
                Fragment f = Fragment.instantiate(this, ProgramListFragment.class.getName());
                Bundle bundle = new Bundle();
                bundle.putLong(Constants.BUNDLE_CHANNEL_ID, channel.id);
                bundle.putBoolean(Constants.BUNDLE_DUAL_PANE, isDualPane);
                f.setArguments(bundle);

                // In dual pane mode show the list of programs on the right side
                // otherwise use the entire screen
                if (isDualPane) {
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.right_fragment, f, RIGHT_FRAGMENT_TAG)
                            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                            .addToBackStack(null)
                            .commit();
                } else {
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.main_fragment, f, MAIN_FRAGMENT_TAG)
                            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                            .addToBackStack(null)
                            .commit();
                }
            }
        }
    }

    @Override
    public void onListItemSelected(int position, Recording recording, String tag) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void onListItemSelected(int position, Program program, String tag) {
        Log.d(TAG, "onListItemSelected (program), pos " + position + ", tag " + tag);
        
        if (tag.equals(ProgramListFragment.class.getSimpleName())) {
            if (program != null) {
                Fragment f = Fragment.instantiate(this, ProgramDetailsFragment.class.getName());
                Bundle bundle = new Bundle();
                bundle.putLong(Constants.BUNDLE_CHANNEL_ID, program.channel.id);
                bundle.putLong(Constants.BUNDLE_PROGRAM_ID, program.id);
                f.setArguments(bundle);

                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.main_fragment, f, MAIN_FRAGMENT_TAG)
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                        .addToBackStack(null)
                        .commit();
            }
        }
    }

    @Override
    public void onListPopulated(String tag) {
        Log.d(TAG, "onListPopulated, tag " + tag);
        
        if (tag.equals(ChannelListFragment.class.getSimpleName())) {
            // When the channel list fragment is done loading the data for the
            // first time we need to select the a channel in the list so that
            // the program list will be loaded for that channel 
            if (isDualPane) {
                Fragment f = getSupportFragmentManager().findFragmentByTag(MAIN_FRAGMENT_TAG);
                if (f instanceof FragmentControlInterface) {
                    ((ChannelListFragment) f).setSelection(0);
                }
            }
        }
    }

    @Override
    public void setCurrentListItemPosition(int position, String tag) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public int getPreviousListItemPosition(String tag) {
        // TODO Auto-generated method stub
        return 0;
    }
}
