package org.tvheadend.tvhclient.ui;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.view.Menu;
import android.view.MenuItem;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.data.tasks.WakeOnLanTaskCallback;
import org.tvheadend.tvhclient.ui.channels.ChannelListFragment;
import org.tvheadend.tvhclient.ui.epg.FragmentControlInterface;
import org.tvheadend.tvhclient.ui.epg.FragmentScrollInterface;
import org.tvheadend.tvhclient.ui.epg.ProgramGuideChannelListFragment;
import org.tvheadend.tvhclient.ui.epg.ProgramGuideListFragment;
import org.tvheadend.tvhclient.ui.epg.ProgramGuidePagerFragment;
import org.tvheadend.tvhclient.ui.misc.InfoFragment;
import org.tvheadend.tvhclient.ui.misc.StatusFragment;
import org.tvheadend.tvhclient.ui.programs.ProgramListFragment;
import org.tvheadend.tvhclient.ui.recordings.CompletedRecordingListFragment;
import org.tvheadend.tvhclient.ui.recordings.FailedRecordingListFragment;
import org.tvheadend.tvhclient.ui.recordings.RemovedRecordingListFragment;
import org.tvheadend.tvhclient.ui.recordings.ScheduledRecordingListFragment;
import org.tvheadend.tvhclient.ui.recordings.SeriesRecordingListFragment;
import org.tvheadend.tvhclient.ui.recordings.TimerRecordingListFragment;
import org.tvheadend.tvhclient.ui.settings.SettingsActivity;
import org.tvheadend.tvhclient.ui.misc.UnlockerFragment;

import java.util.ArrayList;

// TODO make nav image blasser
// TODO confirmation of added/edited recordings...
// TODO onQueryTextSubmit does nothing currently
// TODO navigation menu marking (status, ...)

public class NavigationActivity extends MainActivity implements FragmentScrollInterface, WakeOnLanTaskCallback, NavigationDrawerCallback {

    private int programGuideListPosition = 0;
    private int programGuideListPositionOffset = 0;
    // Default navigation drawer menu position and the list positions
    private int selectedNavigationMenuId = NavigationDrawer.MENU_UNKNOWN;
    // Holds the list of selected menu items so the previous fragment can be
    // shown again when the user has pressed the back key.
    private ArrayList<Integer> menuStack = new ArrayList<>();
    private NavigationDrawer navigationDrawer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        navigationDrawer = new NavigationDrawer(this, savedInstanceState, toolbar, this);
        navigationDrawer.createHeader();
        navigationDrawer.createMenu();

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

        Intent intent;
        Fragment fragment = null;
        switch (position) {
            case NavigationDrawer.MENU_CHANNELS:
                fragment = new ChannelListFragment();
                break;
            case NavigationDrawer.MENU_PROGRAM_GUIDE:
                fragment = new ProgramGuidePagerFragment();
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
                fragment = new StatusFragment();
                break;
            case NavigationDrawer.MENU_INFORMATION:
                fragment = new InfoFragment();
                break;
            case NavigationDrawer.MENU_SETTINGS:
                intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                break;
            case NavigationDrawer.MENU_UNLOCKER:
                fragment = new UnlockerFragment();
                break;
        }

        if (fragment != null) {
            // Save the menu position so we know which one was selected
            selectedNavigationMenuId = position;
            fragment.setArguments(getIntent().getExtras());
            getSupportFragmentManager().beginTransaction().replace(R.id.main, fragment).commit();
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        switch (selectedNavigationMenuId) {
            case NavigationDrawer.MENU_STATUS:
            case NavigationDrawer.MENU_INFORMATION:
            case NavigationDrawer.MENU_UNLOCKER:
                MenuItem mediaRouteMenuItem = menu.findItem(R.id.media_route_menu_item);
                if (mediaRouteMenuItem != null) {
                    mediaRouteMenuItem.setVisible(false);
                }
                MenuItem searchMenuItem = menu.findItem(R.id.menu_search);
                if (searchMenuItem != null) {
                    searchMenuItem.setVisible(false);
                }
                MenuItem reconnectMenuItem = menu.findItem(R.id.menu_refresh);
                if (reconnectMenuItem != null) {
                    reconnectMenuItem.setVisible(false);
                }
                break;
        }

        return true;
    }

    /**
     * Check if the current fragment is a program list fragment. In case single
     * mode is active we need to return to the channel list fragment otherwise
     * show the fragment that belongs to the previously selected menu item.
     */
    @Override
    public void onBackPressed() {
        navigationDrawer.getDrawer().closeDrawer();
        final Fragment f = getSupportFragmentManager().findFragmentById(R.id.main);
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
        TVHClientApplication.getInstance().addListener(navigationDrawer);
        // Show the defined fragment from the menu position or the
        // status if the connection state is not fine
        handleDrawerItemSelected(selectedNavigationMenuId);

        // Update the drawer menu so that all available menu items are
        // shown in case the recording counts have changed or the user has
        // bought the unlocked version to enable all features
        navigationDrawer.updateDrawerItemBadges();
        navigationDrawer.updateDrawerHeader();
    }

    @Override
    public void onPause() {
        super.onPause();
        TVHClientApplication.getInstance().removeListener(navigationDrawer);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // add the values which need to be saved from the drawer to the bundle
        outState = navigationDrawer.getDrawer().saveInstanceState(outState);
        // add the values which need to be saved from the accountHeader to the bundle
        outState = navigationDrawer.getHeader().saveInstanceState(outState);
        outState.putIntegerArrayList("menu_stack", menuStack);
        outState.putInt("navigation_menu_position", selectedNavigationMenuId);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void notify(String message) {
        if (getCurrentFocus() != null) {
            Snackbar.make(getCurrentFocus(), message, Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    public void onNavigationMenuSelected(int id) {
        if (selectedNavigationMenuId != id) {
            handleDrawerItemSelected(id);
        }
    }

    @Override
    public void onScrollingChanged(final int position, final int offset, final String tag) {
        // Save the scroll values so they can be reused after an orientation change.
        programGuideListPosition = position;
        programGuideListPositionOffset = offset;

        if (tag.equals(ProgramGuideChannelListFragment.class.getSimpleName())
                || tag.equals(ProgramGuideListFragment.class.getSimpleName())) {
            // Scrolling was initiated by the channel or program guide list fragment. Keep
            // the currently visible program guide list in sync by scrolling it to the same position
            final Fragment f = getSupportFragmentManager().findFragmentById(R.id.main);
            if (f instanceof FragmentControlInterface) {
                ((FragmentControlInterface) f).setSelection(position, offset);
            }
        }
    }

    @Override
    public void onScrollStateIdle(final String tag) {
        if (tag.equals(ProgramGuideListFragment.class.getSimpleName())
                || tag.equals(ProgramGuideChannelListFragment.class.getSimpleName())) {
            // Scrolling stopped by the program guide or the channel list
            // fragment. Scroll all program guide fragments in the current
            // view pager to the same position.
            final Fragment f = getSupportFragmentManager().findFragmentById(R.id.main);
            if (f instanceof ProgramGuidePagerFragment) {
                ((FragmentControlInterface) f).setSelection(
                        programGuideListPosition,
                        programGuideListPositionOffset);
            }
        }
    }
}
