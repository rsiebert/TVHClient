package org.tvheadend.tvhclient.ui;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.AttrRes;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.View;

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

import org.tvheadend.tvhclient.data.DataStorage;
import org.tvheadend.tvhclient.data.DatabaseHelper;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.data.model.Connection;
import org.tvheadend.tvhclient.data.model.Recording;
import org.tvheadend.tvhclient.utils.MiscUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NavigationDrawer implements AccountHeader.OnAccountHeaderListener, Drawer.OnDrawerItemClickListener {

    // The index for the navigation drawer menus
    public static final int MENU_UNKNOWN = -1;
    public static final int MENU_CHANNELS = 0;
    public static final int MENU_PROGRAM_GUIDE = 1;
    public static final int MENU_COMPLETED_RECORDINGS = 2;
    public static final int MENU_SCHEDULED_RECORDINGS = 3;
    public static final int MENU_SERIES_RECORDINGS = 4;
    public static final int MENU_TIMER_RECORDINGS = 5;
    public static final int MENU_FAILED_RECORDINGS = 6;
    public static final int MENU_REMOVED_RECORDINGS = 7;
    public static final int MENU_STATUS = 8;
    public static final int MENU_INFORMATION = 9;
    public static final int MENU_SETTINGS = 10;
    public static final int MENU_UNLOCKER = 11;

    private final Bundle savedInstanceState;
    private final Activity activity;
    private final Toolbar toolbar;
    private final NavigationDrawerCallback callback;
    private final boolean isUnlocked;
    private AccountHeader headerResult;
    private Drawer result;

    public NavigationDrawer(Activity activity, Bundle savedInstanceState, Toolbar toolbar, NavigationDrawerCallback callback) {
        this.activity = activity;
        this.savedInstanceState = savedInstanceState;
        this.toolbar = toolbar;
        this.callback = callback;
        this.isUnlocked = TVHClientApplication.getInstance().isUnlocked();
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
                .withIcon(getResourceIdFromAttr( R.attr.ic_menu_program_guide));
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
                .withIcon(getResourceIdFromAttr(R.attr.ic_menu_failed_recordings))
                .withBadgeStyle(badgeStyle);
        PrimaryDrawerItem statusItem = new PrimaryDrawerItem()
                .withIdentifier(MENU_STATUS).withName(R.string.status)
                .withIcon(getResourceIdFromAttr(R.attr.ic_menu_status));
        PrimaryDrawerItem informationItem = new PrimaryDrawerItem()
                .withIdentifier(MENU_INFORMATION).withName(R.string.pref_information)
                .withIcon(getResourceIdFromAttr(R.attr.ic_menu_info));
        PrimaryDrawerItem settingsItem = new PrimaryDrawerItem()
                .withIdentifier(MENU_SETTINGS).withName(R.string.settings)
                .withIcon(getResourceIdFromAttr(R.attr.ic_menu_settings));
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

        if (isUnlocked) {
            result.addItem(extrasItem);
        }
    }

    private int getResourceIdFromAttr(@AttrRes int attr) {
        final TypedValue typedValue = new TypedValue();
        activity.getTheme().resolveAttribute(attr, typedValue, true);
        return typedValue.resourceId;
    }

    public void updateDrawerItemBadges() {
        int channelCount = DataStorage.getInstance().getChannelsFromArray().size();
        int completedRecordingCount = 0;
        int scheduledRecordingCount = 0;
        int failedRecordingCount = 0;
        int removedRecordingCount = 0;
        Map<Integer, Recording> map = DataStorage.getInstance().getRecordingsFromArray();
        for (Recording recording : map.values()) {
            if (recording.isCompleted()) {
                completedRecordingCount++;
            } else if (recording.isScheduled()) {
                scheduledRecordingCount++;
            } else if (recording.isFailed()) {
                failedRecordingCount++;
            } else if (recording.isRemoved()) {
                removedRecordingCount++;
            }
        }
        int seriesRecordingCount = DataStorage.getInstance().getSeriesRecordingsFromArray().size();
        int timerRecordingCount = DataStorage.getInstance().getTimerRecordingsFromArray().size();

        result.updateBadge(MENU_CHANNELS, new StringHolder(channelCount + ""));
        result.updateBadge(MENU_COMPLETED_RECORDINGS, new StringHolder(completedRecordingCount + ""));
        result.updateBadge(MENU_SCHEDULED_RECORDINGS, new StringHolder(scheduledRecordingCount + ""));
        result.updateBadge(MENU_SERIES_RECORDINGS, new StringHolder(seriesRecordingCount + ""));
        result.updateBadge(MENU_TIMER_RECORDINGS, new StringHolder(timerRecordingCount + ""));
        result.updateBadge(MENU_FAILED_RECORDINGS, new StringHolder(failedRecordingCount + ""));
        result.updateBadge(MENU_REMOVED_RECORDINGS, new StringHolder(removedRecordingCount + ""));
    }

    public void updateDrawerHeader() {
        // Remove old profiles from the header
        List<Long> profileIdList = new ArrayList<>();
        for (IProfile profile : headerResult.getProfiles()) {
            profileIdList.add(profile.getIdentifier());
        }
        for (Long id : profileIdList) {
            headerResult.removeProfileByIdentifier(id);
        }
        // Add the existing connections as new profiles
        final List<Connection> connectionList = DatabaseHelper.getInstance(activity.getApplicationContext()).getConnections();
        if (connectionList.size() > 0) {
            for (Connection c : connectionList) {
                headerResult.addProfiles(new ProfileDrawerItem().withIdentifier(c.id).withName(c.name).withEmail(c.address));
            }
        } else {
            headerResult.addProfiles(new ProfileDrawerItem().withName(R.string.no_connection_available));
        }
        headerResult.setActiveProfile(DatabaseHelper.getInstance(activity.getApplicationContext()).getSelectedConnection().id);
    }

    public Drawer getDrawer() {
        return result;
    }

    public AccountHeader getHeader() {
        return headerResult;
    }

    @Override
    public boolean onProfileChanged(View view, IProfile profile, boolean current) {
        result.closeDrawer();
        if (!current && callback != null) {
            callback.onNavigationProfileSelected(profile);
        }
        return true;
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
}
