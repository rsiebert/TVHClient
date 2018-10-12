package org.tvheadend.tvhclient.features.navigation;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.AttrRes;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.View;

import com.afollestad.materialdialogs.MaterialDialog;
import com.crashlytics.android.answers.ContentViewEvent;
import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.holder.BadgeStyle;
import com.mikepenz.materialdrawer.holder.StringHolder;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IProfile;

import org.tvheadend.tvhclient.MainApplication;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.entity.Connection;
import org.tvheadend.tvhclient.data.repository.AppRepository;
import org.tvheadend.tvhclient.data.service.EpgSyncService;
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
import org.tvheadend.tvhclient.features.logging.AnswersWrapper;
import org.tvheadend.tvhclient.features.purchase.UnlockerFragment;
import org.tvheadend.tvhclient.features.settings.SettingsActivity;
import org.tvheadend.tvhclient.features.startup.SplashActivity;
import org.tvheadend.tvhclient.utils.MiscUtils;

import java.util.ArrayList;
import java.util.List;

public class NavigationDrawer implements AccountHeader.OnAccountHeaderListener, Drawer.OnDrawerItemClickListener {

    // The index for the navigation drawer menus
    public static final int MENU_CHANNELS = 0;
    private static final int MENU_PROGRAM_GUIDE = 1;
    private static final int MENU_COMPLETED_RECORDINGS = 2;
    private static final int MENU_SCHEDULED_RECORDINGS = 3;
    private static final int MENU_SERIES_RECORDINGS = 4;
    private static final int MENU_TIMER_RECORDINGS = 5;
    private static final int MENU_FAILED_RECORDINGS = 6;
    private static final int MENU_REMOVED_RECORDINGS = 7;
    public static final int MENU_STATUS = 8;
    public static final int MENU_INFORMATION = 9;
    private static final int MENU_SETTINGS = 10;
    public static final int MENU_UNLOCKER = 11;

    private final Bundle savedInstanceState;
    private final AppCompatActivity activity;
    private final Toolbar toolbar;
    private final NavigationDrawerCallback callback;
    private final boolean isUnlocked;
    private AccountHeader headerResult;
    private Drawer result;
    private final AppRepository appRepository;

    public NavigationDrawer(AppCompatActivity activity, Bundle savedInstanceState, Toolbar toolbar, AppRepository appRepository, NavigationDrawerCallback callback) {
        this.activity = activity;
        this.savedInstanceState = savedInstanceState;
        this.toolbar = toolbar;
        this.callback = callback;
        this.isUnlocked = MainApplication.getInstance().isUnlocked();
        this.appRepository = appRepository;
    }

    public void createHeader() {
        headerResult = new AccountHeaderBuilder()
                .withActivity(activity)
                .withCompactStyle(true)
                .withSelectionListEnabledForSingleProfile(false)
                .withProfileImagesVisible(false)
                .withHeaderBackground(MiscUtils.getThemeId(activity) == R.style.CustomTheme_Light ? R.drawable.header_light : R.drawable.header_dark)
                .withOnAccountHeaderListener(this)
                .withSavedInstance(savedInstanceState)
                .build();
    }

    public void createMenu() {
        BadgeStyle badgeStyle = new BadgeStyle()
                .withColorRes(getResourceIdFromAttr(R.attr.material_drawer_badge));

        PrimaryDrawerItem channelItem = new PrimaryDrawerItem()
                .withIdentifier(MENU_CHANNELS).withName(R.string.channels)
                .withIcon(getResourceIdFromAttr(R.attr.ic_menu_channels))
                .withBadgeStyle(badgeStyle);
        PrimaryDrawerItem programGuideItem = new PrimaryDrawerItem()
                .withIdentifier(MENU_PROGRAM_GUIDE).withName(R.string.pref_program_guide)
                .withIcon(getResourceIdFromAttr(R.attr.ic_menu_program_guide));
        PrimaryDrawerItem completedRecordingsItem = new PrimaryDrawerItem()
                .withIdentifier(MENU_COMPLETED_RECORDINGS).withName(R.string.completed_recordings)
                .withIcon(getResourceIdFromAttr(R.attr.ic_menu_completed_recordings))
                .withBadgeStyle(badgeStyle);
        PrimaryDrawerItem scheduledRecordingsItem = new PrimaryDrawerItem()
                .withIdentifier(MENU_SCHEDULED_RECORDINGS).withName(R.string.scheduled_recordings)
                .withIcon(getResourceIdFromAttr(R.attr.ic_menu_scheduled_recordings))
                .withBadgeStyle(badgeStyle);
        PrimaryDrawerItem seriesRecordingsItem = new PrimaryDrawerItem()
                .withIdentifier(MENU_SERIES_RECORDINGS).withName(R.string.series_recordings)
                .withIcon(getResourceIdFromAttr(R.attr.ic_menu_scheduled_recordings))
                .withBadgeStyle(badgeStyle);
        PrimaryDrawerItem timerRecordingsItem = new PrimaryDrawerItem()
                .withIdentifier(MENU_TIMER_RECORDINGS).withName(R.string.timer_recordings)
                .withIcon(getResourceIdFromAttr(R.attr.ic_menu_scheduled_recordings))
                .withBadgeStyle(badgeStyle);
        PrimaryDrawerItem failedRecordingsItem = new PrimaryDrawerItem()
                .withIdentifier(MENU_FAILED_RECORDINGS).withName(R.string.failed_recordings)
                .withIcon(getResourceIdFromAttr(R.attr.ic_menu_failed_recordings))
                .withBadgeStyle(badgeStyle);
        PrimaryDrawerItem removedRecordingsItem = new PrimaryDrawerItem()
                .withIdentifier(MENU_REMOVED_RECORDINGS).withName(R.string.removed_recordings)
                .withIcon(getResourceIdFromAttr(R.attr.ic_menu_removed_recordings))
                .withBadgeStyle(badgeStyle);
        PrimaryDrawerItem statusItem = new PrimaryDrawerItem()
                .withIdentifier(MENU_STATUS).withName(R.string.status)
                .withIcon(getResourceIdFromAttr(R.attr.ic_menu_status));
        PrimaryDrawerItem informationItem = new PrimaryDrawerItem()
                .withIdentifier(MENU_INFORMATION).withName(R.string.pref_information)
                .withIcon(getResourceIdFromAttr(R.attr.ic_menu_info));
        PrimaryDrawerItem settingsItem = new PrimaryDrawerItem()
                .withIdentifier(MENU_SETTINGS).withName(R.string.settings)
                .withIcon(getResourceIdFromAttr(R.attr.ic_menu_settings))
                .withSelectable(false);
        PrimaryDrawerItem extrasItem = new PrimaryDrawerItem()
                .withIdentifier(MENU_UNLOCKER).withName(R.string.pref_unlocker)
                .withIcon(getResourceIdFromAttr(R.attr.ic_menu_extras));

        result = new DrawerBuilder()
                .withActivity(activity)
                .withAccountHeader(headerResult)
                .withToolbar(toolbar)
                .addDrawerItems(channelItem,
                        programGuideItem,
                        new DividerDrawerItem(),
                        completedRecordingsItem,
                        scheduledRecordingsItem,
                        seriesRecordingsItem,
                        timerRecordingsItem,
                        failedRecordingsItem,
                        removedRecordingsItem,
                        new DividerDrawerItem(),
                        statusItem,
                        informationItem,
                        settingsItem
                )
                .withOnDrawerItemClickListener(this)
                .withSavedInstance(savedInstanceState)
                .build();

        if (!isUnlocked) {
            result.addItem(extrasItem);
        }
    }

    private int getResourceIdFromAttr(@AttrRes int attr) {
        final TypedValue typedValue = new TypedValue();
        activity.getTheme().resolveAttribute(attr, typedValue, true);
        return typedValue.resourceId;
    }

    public void showConnectionsInDrawerHeader() {
        // Remove old profiles from the header
        List<Long> profileIdList = new ArrayList<>();
        for (IProfile profile : headerResult.getProfiles()) {
            profileIdList.add(profile.getIdentifier());
        }
        for (Long id : profileIdList) {
            headerResult.removeProfileByIdentifier(id);
        }
        // Add the existing connections as new profiles
        final List<Connection> connectionList = appRepository.getConnectionData().getItems();
        if (connectionList.size() > 0) {
            for (Connection c : connectionList) {
                headerResult.addProfiles(new ProfileDrawerItem().withIdentifier(c.getId()).withName(c.getName()).withEmail(c.getHostname()));
            }
        } else {
            headerResult.addProfiles(new ProfileDrawerItem().withName(R.string.no_connection_available));
        }
        Connection connection = appRepository.getConnectionData().getActiveItem();
        if (connection != null) {
            headerResult.setActiveProfile(connection.getId());
        }
    }

    @Override
    public boolean onProfileChanged(View view, IProfile profile, boolean current) {
        result.closeDrawer();

        // Do nothing if the same profile has been selected
        if (current) {
            return true;
        }

        new MaterialDialog.Builder(activity)
                .title(R.string.dialog_title_connect_to_server)
                .negativeText(R.string.cancel)
                .positiveText(R.string.dialog_button_connect)
                .onPositive((dialog, which) -> {
                    headerResult.setActiveProfile(profile.getIdentifier());
                    handleNewServerSelected((int) profile.getIdentifier());
                })
                .onNegative(((dialog, which) -> {
                    Connection connection = appRepository.getConnectionData().getActiveItem();
                    headerResult.setActiveProfile(connection.getId());
                }))
                .cancelable(false)
                .canceledOnTouchOutside(false)
                .show();

        return false;
    }

    /**
     * A new server was selected in the navigation header.
     * Stops the service and sets the newly selected connection as the active one.
     * The app will be restarted to show the data from the new server.
     *
     * @param id The connection id of the newly selected server
     */
    private void handleNewServerSelected(int id) {
        activity.stopService(new Intent(activity, EpgSyncService.class));

        Connection connection = appRepository.getConnectionData().getItemById(id);
        connection.setActive(true);
        appRepository.getConnectionData().updateItem(connection);

        Intent intent = new Intent(activity, SplashActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.finish();
    }

    @Override
    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
        result.closeDrawer();
        int id = (int) drawerItem.getIdentifier();
        if (callback != null) {
            callback.onNavigationMenuSelected(id);
        }
        return true;
    }

    private void setSelection(int id) {
        result.setSelection(id, false);
    }

    public Bundle saveInstanceState(Bundle outState) {
        outState = result.saveInstanceState(outState);
        outState = headerResult.saveInstanceState(outState);
        return outState;
    }

    public void startObservingViewModels() {

        ChannelViewModel channelViewModel = ViewModelProviders.of(activity).get(ChannelViewModel.class);
        channelViewModel.getNumberOfChannels().observe(activity, count -> result.updateBadge(MENU_CHANNELS, new StringHolder(count + "")));

        SeriesRecordingViewModel seriesRecordingViewModel = ViewModelProviders.of(activity).get(SeriesRecordingViewModel.class);
        seriesRecordingViewModel.getNumberOfRecordings().observe(activity, count -> result.updateBadge(MENU_SERIES_RECORDINGS, new StringHolder(count + "")));

        TimerRecordingViewModel timerRecordingViewModel = ViewModelProviders.of(activity).get(TimerRecordingViewModel.class);
        timerRecordingViewModel.getNumberOfRecordings().observe(activity, count -> result.updateBadge(MENU_TIMER_RECORDINGS, new StringHolder(count + "")));

        RecordingViewModel recordingViewModel = ViewModelProviders.of(activity).get(RecordingViewModel.class);
        recordingViewModel.getNumberOfCompletedRecordings().observe(activity, count -> result.updateBadge(MENU_COMPLETED_RECORDINGS, new StringHolder(count + "")));
        recordingViewModel.getNumberOfScheduledRecordings().observe(activity, count -> result.updateBadge(MENU_SCHEDULED_RECORDINGS, new StringHolder(count + "")));
        recordingViewModel.getNumberOfFailedRecordings().observe(activity, count -> result.updateBadge(MENU_FAILED_RECORDINGS, new StringHolder(count + "")));
        recordingViewModel.getNumberOfRemovedRecordings().observe(activity, count -> result.updateBadge(MENU_REMOVED_RECORDINGS, new StringHolder(count + "")));
    }

    public void handleSelection(Fragment fragment) {
        if (fragment instanceof ChannelListFragment) {
            setSelection(MENU_CHANNELS);
        } else if (fragment instanceof ProgramGuideFragment) {
            setSelection(MENU_PROGRAM_GUIDE);
        } else if (fragment instanceof CompletedRecordingListFragment) {
            setSelection(MENU_COMPLETED_RECORDINGS);
        } else if (fragment instanceof ScheduledRecordingListFragment) {
            setSelection(MENU_SCHEDULED_RECORDINGS);
        } else if (fragment instanceof SeriesRecordingListFragment) {
            setSelection(MENU_SERIES_RECORDINGS);
        } else if (fragment instanceof TimerRecordingListFragment) {
            setSelection(MENU_TIMER_RECORDINGS);
        } else if (fragment instanceof FailedRecordingListFragment) {
            setSelection(MENU_FAILED_RECORDINGS);
        } else if (fragment instanceof RemovedRecordingListFragment) {
            setSelection(MENU_REMOVED_RECORDINGS);
        } else if (fragment instanceof StatusFragment) {
            setSelection(MENU_STATUS);
        } else if (fragment instanceof InfoFragment) {
            setSelection(MENU_INFORMATION);
        } else if (fragment instanceof UnlockerFragment) {
            setSelection(MENU_UNLOCKER);
        }
    }

    public Fragment getFragmentFromSelection(int position) {
        Intent intent;
        Fragment fragment = null;
        switch (position) {
            case NavigationDrawer.MENU_CHANNELS:
                AnswersWrapper.getInstance().logContentView(new ContentViewEvent()
                        .putContentName("Channel screen"));
                fragment = new ChannelListFragment();
                break;
            case NavigationDrawer.MENU_PROGRAM_GUIDE:
                AnswersWrapper.getInstance().logContentView(new ContentViewEvent()
                        .putContentName("Program guide screen"));
                fragment = new ProgramGuideFragment();
                break;
            case NavigationDrawer.MENU_COMPLETED_RECORDINGS:
                AnswersWrapper.getInstance().logContentView(new ContentViewEvent()
                        .putContentName("Completed recordings screen"));
                fragment = new CompletedRecordingListFragment();
                break;
            case NavigationDrawer.MENU_SCHEDULED_RECORDINGS:
                AnswersWrapper.getInstance().logContentView(new ContentViewEvent()
                        .putContentName("Scheduled recordings screen"));
                fragment = new ScheduledRecordingListFragment();
                break;
            case NavigationDrawer.MENU_SERIES_RECORDINGS:
                AnswersWrapper.getInstance().logContentView(new ContentViewEvent()
                        .putContentName("Series recordings screen"));
                fragment = new SeriesRecordingListFragment();
                break;
            case NavigationDrawer.MENU_TIMER_RECORDINGS:
                AnswersWrapper.getInstance().logContentView(new ContentViewEvent()
                        .putContentName("Timer recordings screen"));
                fragment = new TimerRecordingListFragment();
                break;
            case NavigationDrawer.MENU_FAILED_RECORDINGS:
                AnswersWrapper.getInstance().logContentView(new ContentViewEvent()
                        .putContentName("Failed recordings screen"));
                fragment = new FailedRecordingListFragment();
                break;
            case NavigationDrawer.MENU_REMOVED_RECORDINGS:
                AnswersWrapper.getInstance().logContentView(new ContentViewEvent()
                        .putContentName("Removed recordings screen"));
                fragment = new RemovedRecordingListFragment();
                break;
            case NavigationDrawer.MENU_STATUS:
                AnswersWrapper.getInstance().logContentView(new ContentViewEvent()
                        .putContentName("Status screen"));
                fragment = new StatusFragment();
                break;
            case NavigationDrawer.MENU_INFORMATION:
                AnswersWrapper.getInstance().logContentView(new ContentViewEvent()
                        .putContentName("Information screen"));
                fragment = new InfoFragment();
                break;
            case NavigationDrawer.MENU_SETTINGS:
                AnswersWrapper.getInstance().logContentView(new ContentViewEvent()
                        .putContentName("Settings screen"));
                intent = new Intent(activity, SettingsActivity.class);
                activity.startActivity(intent);
                break;
            case NavigationDrawer.MENU_UNLOCKER:
                AnswersWrapper.getInstance().logContentView(new ContentViewEvent()
                        .putContentName("Unlocker screen"));
                fragment = new UnlockerFragment();
                break;
        }
        return fragment;
    }
}
