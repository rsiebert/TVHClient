package org.tvheadend.tvhclient.ui.navigation;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.AttrRes;
import android.support.v7.widget.Toolbar;
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

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.data.entity.Connection;
import org.tvheadend.tvhclient.data.repository.ConnectionRepository;
import org.tvheadend.tvhclient.ui.startup.StartupActivity;
import org.tvheadend.tvhclient.utils.MiscUtils;

import java.util.ArrayList;
import java.util.List;

public class NavigationDrawer implements AccountHeader.OnAccountHeaderListener, Drawer.OnDrawerItemClickListener {
    private String TAG = getClass().getSimpleName();

    // The index for the navigation drawer menus
    static final int MENU_UNKNOWN = -1;
    static final int MENU_CHANNELS = 0;
    static final int MENU_PROGRAM_GUIDE = 1;
    static final int MENU_COMPLETED_RECORDINGS = 2;
    static final int MENU_SCHEDULED_RECORDINGS = 3;
    static final int MENU_SERIES_RECORDINGS = 4;
    static final int MENU_TIMER_RECORDINGS = 5;
    static final int MENU_FAILED_RECORDINGS = 6;
    static final int MENU_REMOVED_RECORDINGS = 7;
    static final int MENU_STATUS = 8;
    static final int MENU_INFORMATION = 9;
    static final int MENU_SETTINGS = 10;
    static final int MENU_UNLOCKER = 11;

    private final Bundle savedInstanceState;
    private final Activity activity;
    private final Toolbar toolbar;
    private final NavigationDrawerCallback callback;
    private final boolean isUnlocked;
    private AccountHeader headerResult;
    private Drawer result;
    private ConnectionRepository connectionRepository;

    NavigationDrawer(Activity activity, Bundle savedInstanceState, Toolbar toolbar, NavigationDrawerCallback callback) {
        this.activity = activity;
        this.savedInstanceState = savedInstanceState;
        this.toolbar = toolbar;
        this.callback = callback;
        this.isUnlocked = TVHClientApplication.getInstance().isUnlocked();
        this.connectionRepository = new ConnectionRepository(activity);
    }

    void createHeader() {
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

    void createMenu() {
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

    void updateDrawerHeader() {
        // Remove old profiles from the header
        List<Long> profileIdList = new ArrayList<>();
        for (IProfile profile : headerResult.getProfiles()) {
            profileIdList.add(profile.getIdentifier());
        }
        for (Long id : profileIdList) {
            headerResult.removeProfileByIdentifier(id);
        }
        // Add the existing connections as new profiles
        final List<Connection> connectionList = connectionRepository.getAllConnectionsSync();
        if (connectionList.size() > 0) {
            for (Connection c : connectionList) {
                headerResult.addProfiles(new ProfileDrawerItem().withIdentifier(c.getId()).withName(c.getName()).withEmail(c.getHostname()));
            }
        } else {
            headerResult.addProfiles(new ProfileDrawerItem().withName(R.string.no_connection_available));
        }
        Connection connection = connectionRepository.getActiveConnectionSync();
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
                .title("Connect to new server?")
                .content("Do you want to connect to the newly selected server?\n" +
                        "The application will be restarted and a new initial sync will be performed.")
                .negativeText(R.string.cancel)
                .positiveText("Connect")
                .onPositive((dialog, which) -> {
                    // Update the currently active connection
                    Connection connection = connectionRepository.getConnectionByIdSync((int) profile.getIdentifier());
                    connection.setActive(true);
                    connectionRepository.updateConnectionSync(connection);

                    // Save the information that a new sync is required
                    // Then restart the application to show the sync fragment
                    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putBoolean("initial_sync_required", true);
                    editor.apply();

                    // Restart the application
                    Intent intent = new Intent(activity, StartupActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    activity.startActivity(intent);
                    activity.finish();
                })
                .show();

        return false;
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

    void updateSeriesRecordingBadge(int size) {
        result.updateBadge(MENU_SERIES_RECORDINGS, new StringHolder(size + ""));
    }

    void updateTimerRecordingBadge(int size) {
        result.updateBadge(MENU_TIMER_RECORDINGS, new StringHolder(size + ""));
    }

    void updateCompletedRecordingBadge(int size) {
        result.updateBadge(MENU_COMPLETED_RECORDINGS, new StringHolder(size + ""));
    }

    void updateScheduledRecordingBadge(int size) {
        result.updateBadge(MENU_SCHEDULED_RECORDINGS, new StringHolder(size + ""));
    }

    void updateFailedRecordingBadge(int size) {
        result.updateBadge(MENU_FAILED_RECORDINGS, new StringHolder(size + ""));
    }

    void updateRemovedRecordingBadge(int size) {
        result.updateBadge(MENU_REMOVED_RECORDINGS, new StringHolder(size + ""));
    }

    void updateChannelBadge(int size) {
        result.updateBadge(MENU_CHANNELS, new StringHolder(size + ""));
    }

    public void setSelection(int id) {
        result.setSelection(id, false);
    }

    Bundle saveInstanceState(Bundle outState) {
        outState = result.saveInstanceState(outState);
        outState = headerResult.saveInstanceState(outState);
        return outState;
    }
}
