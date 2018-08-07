package org.tvheadend.tvhclient.features.navigation;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.FrameLayout;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.features.MainActivity;
import org.tvheadend.tvhclient.features.channels.ChannelListFragment;
import org.tvheadend.tvhclient.features.channels.ChannelViewModel;
import org.tvheadend.tvhclient.features.dvr.recordings.CompletedRecordingListFragment;
import org.tvheadend.tvhclient.features.dvr.recordings.FailedRecordingListFragment;
import org.tvheadend.tvhclient.features.dvr.recordings.RecordingViewModel;
import org.tvheadend.tvhclient.features.dvr.recordings.RemovedRecordingListFragment;
import org.tvheadend.tvhclient.features.dvr.recordings.ScheduledRecordingListFragment;
import org.tvheadend.tvhclient.features.dvr.series_recordings.SeriesRecordingListFragment;
import org.tvheadend.tvhclient.features.dvr.series_recordings.SeriesRecordingViewModel;
import org.tvheadend.tvhclient.features.dvr.timer_recordings.TimerRecordingListFragment;
import org.tvheadend.tvhclient.features.dvr.timer_recordings.TimerRecordingViewModel;
import org.tvheadend.tvhclient.features.epg.ProgramGuideFragment;
import org.tvheadend.tvhclient.features.information.InfoFragment;
import org.tvheadend.tvhclient.features.information.StatusFragment;
import org.tvheadend.tvhclient.features.purchase.UnlockerFragment;
import org.tvheadend.tvhclient.features.settings.SettingsActivity;
import org.tvheadend.tvhclient.features.shared.tasks.WakeOnLanTaskCallback;

import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

// TODO make nav image blasser
// TODO icons in dual pane mode

public class NavigationActivity extends MainActivity implements WakeOnLanTaskCallback, NavigationDrawerCallback {

    private int selectedNavigationMenuId;
    private NavigationDrawer navigationDrawer;
    @BindView(R.id.main)
    FrameLayout mainFrameLayout;
    @Nullable
    @BindView(R.id.details)
    FrameLayout detailsFrameLayout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ButterKnife.bind(this);

        navigationDrawer = new NavigationDrawer(this, savedInstanceState, toolbar, appRepository, this);
        navigationDrawer.createHeader();
        navigationDrawer.createMenu();

        // When the activity is created it got called by the main activity. Get the initial
        // navigation menu position and show the associated fragment with it. When the device
        // was rotated just restore the position from the saved instance.
        if (savedInstanceState == null) {
            Bundle bundle = getIntent().getExtras();
            selectedNavigationMenuId = bundle != null ? bundle.getInt("startScreen") : 0;
            handleDrawerItemSelected(selectedNavigationMenuId);
        } else {
            selectedNavigationMenuId = savedInstanceState.getInt("navigationMenuId", NavigationDrawer.MENU_CHANNELS);
        }

        getSupportFragmentManager().addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                Fragment current = getSupportFragmentManager().findFragmentById(R.id.main);
                if (current instanceof ChannelListFragment) {
                    navigationDrawer.setSelection(NavigationDrawer.MENU_CHANNELS);
                } else if (current instanceof CompletedRecordingListFragment) {
                    navigationDrawer.setSelection(NavigationDrawer.MENU_COMPLETED_RECORDINGS);
                } else if (current instanceof ScheduledRecordingListFragment) {
                    navigationDrawer.setSelection(NavigationDrawer.MENU_SCHEDULED_RECORDINGS);
                } else if (current instanceof SeriesRecordingListFragment) {
                    navigationDrawer.setSelection(NavigationDrawer.MENU_SERIES_RECORDINGS);
                } else if (current instanceof TimerRecordingListFragment) {
                    navigationDrawer.setSelection(NavigationDrawer.MENU_TIMER_RECORDINGS);
                } else if (current instanceof FailedRecordingListFragment) {
                    navigationDrawer.setSelection(NavigationDrawer.MENU_FAILED_RECORDINGS);
                } else if (current instanceof RemovedRecordingListFragment) {
                    navigationDrawer.setSelection(NavigationDrawer.MENU_REMOVED_RECORDINGS);
                } else if (current instanceof StatusFragment) {
                    navigationDrawer.setSelection(NavigationDrawer.MENU_STATUS);
                } else if (current instanceof InfoFragment) {
                    navigationDrawer.setSelection(NavigationDrawer.MENU_INFORMATION);
                } else if (current instanceof UnlockerFragment) {
                    navigationDrawer.setSelection(NavigationDrawer.MENU_UNLOCKER);
                }
            }
        });

        // Update the drawer menu so that all available menu items are
        // shown in case the recording counts have changed or the user has
        // bought the unlocked version to enable all features
        navigationDrawer.updateDrawerHeader();
        startObservingViewModels();
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() <= 1) {
            finish();
        } else {
            super.onBackPressed();
        }
    }

    /**
     *
     */
    private void startObservingViewModels() {

        ChannelViewModel channelViewModel = ViewModelProviders.of(this).get(ChannelViewModel.class);
        channelViewModel.getNumberOfChannels().observe(this, count -> {
            navigationDrawer.updateChannelBadge(count);
        });

        SeriesRecordingViewModel seriesRecordingViewModel = ViewModelProviders.of(this).get(SeriesRecordingViewModel.class);
        seriesRecordingViewModel.getNumberOfRecordings().observe(this, count -> {
            navigationDrawer.updateSeriesRecordingBadge(count);
        });

        TimerRecordingViewModel timerRecordingViewModel = ViewModelProviders.of(this).get(TimerRecordingViewModel.class);
        timerRecordingViewModel.getNumberOfRecordings().observe(this, count -> {
            navigationDrawer.updateTimerRecordingBadge(count);
        });

        RecordingViewModel recordingViewModel = ViewModelProviders.of(this).get(RecordingViewModel.class);
        recordingViewModel.getNumberOfCompletedRecordings().observe(this, count -> {
            navigationDrawer.updateCompletedRecordingBadge(count);
        });
        recordingViewModel.getNumberOfScheduledRecordings().observe(this, count -> {
            navigationDrawer.updateScheduledRecordingBadge(count);
        });
        recordingViewModel.getNumberOfFailedRecordings().observe(this, count -> {
            navigationDrawer.updateFailedRecordingBadge(count);
        });
        recordingViewModel.getNumberOfRemovedRecordings().observe(this, count -> {
            navigationDrawer.updateRemovedRecordingBadge(count);
        });
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
                Answers.getInstance().logContentView(new ContentViewEvent()
                        .putContentName("Channel screen"));
                fragment = new ChannelListFragment();
                break;
            case NavigationDrawer.MENU_PROGRAM_GUIDE:
                Answers.getInstance().logContentView(new ContentViewEvent()
                        .putContentName("Program guide screen"));
                fragment = new ProgramGuideFragment();
                break;
            case NavigationDrawer.MENU_COMPLETED_RECORDINGS:
                Answers.getInstance().logContentView(new ContentViewEvent()
                        .putContentName("Completed recordings screen"));
                fragment = new CompletedRecordingListFragment();
                break;
            case NavigationDrawer.MENU_SCHEDULED_RECORDINGS:
                Answers.getInstance().logContentView(new ContentViewEvent()
                        .putContentName("Scheduled recordings screen"));
                fragment = new ScheduledRecordingListFragment();
                break;
            case NavigationDrawer.MENU_SERIES_RECORDINGS:
                Answers.getInstance().logContentView(new ContentViewEvent()
                        .putContentName("Series recordings screen"));
                fragment = new SeriesRecordingListFragment();
                break;
            case NavigationDrawer.MENU_TIMER_RECORDINGS:
                Answers.getInstance().logContentView(new ContentViewEvent()
                        .putContentName("Timer recordings screen"));
                fragment = new TimerRecordingListFragment();
                break;
            case NavigationDrawer.MENU_FAILED_RECORDINGS:
                Answers.getInstance().logContentView(new ContentViewEvent()
                        .putContentName("Failed recordings screen"));
                fragment = new FailedRecordingListFragment();
                break;
            case NavigationDrawer.MENU_REMOVED_RECORDINGS:
                Answers.getInstance().logContentView(new ContentViewEvent()
                        .putContentName("Removed recordings screen"));
                fragment = new RemovedRecordingListFragment();
                break;
            case NavigationDrawer.MENU_STATUS:
                Answers.getInstance().logContentView(new ContentViewEvent()
                        .putContentName("Status screen"));
                fragment = new StatusFragment();
                break;
            case NavigationDrawer.MENU_INFORMATION:
                Answers.getInstance().logContentView(new ContentViewEvent()
                        .putContentName("Information screen"));
                fragment = new InfoFragment();
                break;
            case NavigationDrawer.MENU_SETTINGS:
                Answers.getInstance().logContentView(new ContentViewEvent()
                        .putContentName("Settings screen"));
                intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                break;
            case NavigationDrawer.MENU_UNLOCKER:
                Answers.getInstance().logContentView(new ContentViewEvent()
                        .putContentName("Unlocker screen"));
                fragment = new UnlockerFragment();
                break;
        }

        if (fragment != null) {
            // Save the menu position so we know which one was selected
            selectedNavigationMenuId = position;

            if (isDualPane) {
                removeDetailsFragment();
            }

            // Show the new fragment that represents the selected menu entry.
            fragment.setArguments(getIntent().getExtras());
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.main, fragment)
                    .addToBackStack(null)
                    .commit();
        }
    }

    /**
     * Remove the old details fragment if there is one so that it is not visible when
     * the new main fragment is loaded. It takes a while until the new details
     * fragment is visible. This prevents showing wrong data when switching screens.
     */
    private void removeDetailsFragment() {
        Fragment detailsFragment = getSupportFragmentManager().findFragmentById(R.id.details);
        if (detailsFragment != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .remove(detailsFragment)
                    .commit();
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

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // add the values which need to be saved from the drawer and header to the bundle
        outState = navigationDrawer.saveInstanceState(outState);
        outState.putInt("navigationMenuId", selectedNavigationMenuId);
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
        Timber.d("onNavigationMenuSelected() called with: id = [" + id + "], selectedNavigationMenuId " + selectedNavigationMenuId);
        if (selectedNavigationMenuId != id) {
            handleDrawerItemSelected(id);
        }
    }
}
