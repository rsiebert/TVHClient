package org.tvheadend.tvhclient.ui.features.navigation;

import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;

import com.afollestad.materialdialogs.MaterialDialog;
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
import org.tvheadend.tvhclient.domain.entity.Connection;
import org.tvheadend.tvhclient.data.repository.AppRepository;
import org.tvheadend.tvhclient.data.service.HtspService;
import org.tvheadend.tvhclient.ui.features.channels.ChannelListFragment;
import org.tvheadend.tvhclient.ui.features.channels.ChannelViewModel;
import org.tvheadend.tvhclient.ui.features.dvr.recordings.CompletedRecordingListFragment;
import org.tvheadend.tvhclient.ui.features.dvr.recordings.FailedRecordingListFragment;
import org.tvheadend.tvhclient.ui.features.dvr.recordings.RecordingViewModel;
import org.tvheadend.tvhclient.ui.features.dvr.recordings.RemovedRecordingListFragment;
import org.tvheadend.tvhclient.ui.features.dvr.recordings.ScheduledRecordingListFragment;
import org.tvheadend.tvhclient.ui.features.dvr.series_recordings.SeriesRecordingListFragment;
import org.tvheadend.tvhclient.ui.features.dvr.series_recordings.SeriesRecordingViewModel;
import org.tvheadend.tvhclient.ui.features.dvr.timer_recordings.TimerRecordingListFragment;
import org.tvheadend.tvhclient.ui.features.dvr.timer_recordings.TimerRecordingViewModel;
import org.tvheadend.tvhclient.ui.features.epg.ProgramGuideFragment;
import org.tvheadend.tvhclient.ui.features.information.StatusFragment;
import org.tvheadend.tvhclient.ui.features.information.WebViewFragment;
import org.tvheadend.tvhclient.ui.features.unlocker.UnlockerFragment;
import org.tvheadend.tvhclient.ui.features.settings.ConnectionViewModel;
import org.tvheadend.tvhclient.ui.features.settings.SettingsActivity;
import org.tvheadend.tvhclient.ui.features.startup.SplashActivity;
import org.tvheadend.tvhclient.util.MiscUtils;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.AttrRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

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
    private static final int MENU_SETTINGS = 9;
    public static final int MENU_UNLOCKER = 10;
    public static final int MENU_HELP = 11;

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
        PrimaryDrawerItem settingsItem = new PrimaryDrawerItem()
                .withIdentifier(MENU_SETTINGS).withName(R.string.settings)
                .withIcon(getResourceIdFromAttr(R.attr.ic_menu_settings))
                .withSelectable(false);
        PrimaryDrawerItem extrasItem = new PrimaryDrawerItem()
                .withIdentifier(MENU_UNLOCKER).withName(R.string.pref_unlocker)
                .withIcon(getResourceIdFromAttr(R.attr.ic_menu_extras));
        PrimaryDrawerItem helpItem = new PrimaryDrawerItem()
                .withIdentifier(MENU_HELP).withName(R.string.help_and_support)
                .withIcon(getResourceIdFromAttr(R.attr.ic_menu_help));

        DrawerBuilder drawerBuilder = new DrawerBuilder();
        drawerBuilder.withActivity(activity)
                .withAccountHeader(headerResult)
                .withToolbar(toolbar)
                .withOnDrawerItemClickListener(this)
                .withSavedInstance(savedInstanceState);

        drawerBuilder.addDrawerItems(
                channelItem,
                programGuideItem,
                new DividerDrawerItem(),
                completedRecordingsItem,
                scheduledRecordingsItem,
                seriesRecordingsItem,
                timerRecordingsItem,
                failedRecordingsItem,
                removedRecordingsItem);
        if (!isUnlocked) {
            drawerBuilder.addDrawerItems(
                    new DividerDrawerItem(),
                    extrasItem);
        }
        drawerBuilder.addDrawerItems(
                new DividerDrawerItem(),
                settingsItem,
                helpItem,
                statusItem);

        result = drawerBuilder.build();
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
                headerResult.addProfiles(
                        new CustomProfileDrawerItem()
                                .withIdentifier(c.getId())
                                .withName(c.getName())
                                .withEmail(c.getHostname()));
            }
        } else {
            headerResult.addProfiles(new ProfileDrawerItem().withName(R.string.no_connection_available));
        }
        Connection connection = appRepository.getConnectionData().getActiveItem();
        headerResult.setActiveProfile(connection.getId());
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
        activity.stopService(new Intent(activity, HtspService.class));

        Connection connection = appRepository.getConnectionData().getItemById(id);
        if (connection != null) {
            connection.setActive(true);
            appRepository.getConnectionData().updateItem(connection);
            Intent intent = new Intent(activity, SplashActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            activity.startActivity(intent);
            activity.finish();
        }
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

        ConnectionViewModel connectionViewModel = ViewModelProviders.of(activity).get(ConnectionViewModel.class);
        connectionViewModel.getAllConnections().observe(activity, connections -> showConnectionsInDrawerHeader());

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
        } else if (fragment instanceof UnlockerFragment) {
            setSelection(MENU_UNLOCKER);
        } else if (fragment instanceof WebViewFragment) {
            setSelection(MENU_HELP);
        }
    }

    public Fragment getFragmentFromSelection(int position) {
        Fragment fragment = null;
        Bundle bundle = new Bundle();
        switch (position) {
            case NavigationDrawer.MENU_CHANNELS:
                fragment = new ChannelListFragment();
                break;
            case NavigationDrawer.MENU_PROGRAM_GUIDE:
                fragment = new ProgramGuideFragment();
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
            case NavigationDrawer.MENU_SETTINGS:
                activity.startActivity(new Intent(activity, SettingsActivity.class));
                break;
            case NavigationDrawer.MENU_UNLOCKER:
                fragment = new UnlockerFragment();
                bundle.putString("website", "features");
                fragment.setArguments(bundle);
                break;
            case NavigationDrawer.MENU_HELP:
                fragment = new WebViewFragment();
                bundle.putString("website", "help_and_support");
                fragment.setArguments(bundle);
                break;
        }
        return fragment;
    }
}
