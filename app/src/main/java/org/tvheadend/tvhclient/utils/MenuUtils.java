package org.tvheadend.tvhclient.utils;


import android.app.Activity;
import android.app.SearchManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.Menu;
import android.view.MenuItem;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;

import org.tvheadend.tvhclient.MainApplication;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.entity.ChannelTag;
import org.tvheadend.tvhclient.data.entity.Connection;
import org.tvheadend.tvhclient.data.entity.Program;
import org.tvheadend.tvhclient.data.entity.Recording;
import org.tvheadend.tvhclient.data.entity.SeriesRecording;
import org.tvheadend.tvhclient.data.entity.ServerProfile;
import org.tvheadend.tvhclient.data.entity.ServerStatus;
import org.tvheadend.tvhclient.data.entity.TimerRecording;
import org.tvheadend.tvhclient.data.repository.AppRepository;
import org.tvheadend.tvhclient.data.service.EpgSyncService;
import org.tvheadend.tvhclient.features.download.DownloadRecordingManager;
import org.tvheadend.tvhclient.features.search.SearchActivity;
import org.tvheadend.tvhclient.features.shared.adapter.ChannelTagListAdapter;
import org.tvheadend.tvhclient.features.shared.adapter.GenreColorDialogAdapter;
import org.tvheadend.tvhclient.features.shared.callbacks.ChannelTagSelectionCallback;
import org.tvheadend.tvhclient.features.shared.callbacks.ChannelTimeSelectionCallback;
import org.tvheadend.tvhclient.features.shared.callbacks.RecordingRemovedCallback;
import org.tvheadend.tvhclient.features.startup.SplashActivity;
import org.tvheadend.tvhclient.features.streaming.external.PlayChannelActivity;
import org.tvheadend.tvhclient.features.streaming.external.PlayRecordingActivity;
import org.tvheadend.tvhclient.features.streaming.internal.HtspPlaybackActivity;

import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import timber.log.Timber;

public class MenuUtils {

    private final boolean isUnlocked;
    @Inject
    protected AppRepository appRepository;
    @Inject
    protected SharedPreferences sharedPreferences;
    private final ServerStatus serverStatus;
    private final WeakReference<Activity> activity;

    public MenuUtils(@NonNull Activity activity) {
        MainApplication.getComponent().inject(this);

        this.activity = new WeakReference<>(activity);
        this.isUnlocked = MainApplication.getInstance().isUnlocked();
        this.serverStatus = appRepository.getServerStatusData().getActiveItem();
    }

    /**
     * Prepares a dialog that shows the available genre colors and the names. In
     * here the data for the adapter is created and the dialog prepared which
     * can be shown later.
     */
    public boolean handleMenuGenreColorSelection() {
        Activity activity = this.activity.get();
        if (activity == null) {
            return false;
        }
        final String[] s = activity.getResources().getStringArray(R.array.pr_content_type0);

        // Fill the list for the adapter
        final List<GenreColorDialogAdapter.GenreColorDialogItem> items = new ArrayList<>();
        for (int i = 0; i < s.length; ++i) {
            GenreColorDialogAdapter.GenreColorDialogItem genreColor = new GenreColorDialogAdapter.GenreColorDialogItem();
            genreColor.color = UIUtils.getGenreColor(activity, ((i + 1) * 16), 0);
            genreColor.genre = s[i];
            items.add(genreColor);
        }
        new MaterialDialog.Builder(activity)
                .title(R.string.genre_color_list)
                .adapter(new GenreColorDialogAdapter(items), null)
                .show();
        return true;
    }

    public boolean handleMenuTimeSelection(int currentSelection, int intervalInHours, int maxIntervalsToShow, @Nullable ChannelTimeSelectionCallback callback) {
        Activity activity = this.activity.get();
        if (activity == null) {
            return false;
        }

        SimpleDateFormat startDateFormat = new SimpleDateFormat("dd.MM.yyyy - HH.00", Locale.US);
        SimpleDateFormat endDateFormat = new SimpleDateFormat("HH.00", Locale.US);

        String[] times = new String[maxIntervalsToShow];
        times[0] = activity.getString(R.string.current_time);

        // Set the time that shall be shown next in the dialog. This is the
        // current time plus the value of the intervalInHours in milliseconds
        long timeInMillis = Calendar.getInstance().getTimeInMillis() + 1000 * 60 * 60 * intervalInHours;

        // Add the date and time to the list. Remove Increase the time in
        // milliseconds for each iteration by the defined intervalInHours
        for (int i = 1; i < maxIntervalsToShow; i++) {
            String startTime = startDateFormat.format(timeInMillis);
            timeInMillis += 1000 * 60 * 60 * intervalInHours;
            String endTime = endDateFormat.format(timeInMillis);
            times[i] = startTime + " - " + endTime;
        }

        new MaterialDialog.Builder(activity)
                .title(R.string.select_time)
                .items(times)
                .itemsCallbackSingleChoice(currentSelection, (dialog, itemView, which, text) -> {
                    if (callback != null) {
                        callback.onTimeSelected(which);
                    }
                    return true;
                })
                .build()
                .show();
        return true;
    }

    public boolean handleMenuChannelTagsSelection(int channelTagId, @Nullable ChannelTagSelectionCallback callback) {
        Activity activity = this.activity.get();
        if (activity == null) {
            return false;
        }

        // Create a default tag (All channels)
        ChannelTag tag = new ChannelTag();
        tag.setTagId(0);
        tag.setTagName(activity.getString(R.string.all_channels));

        // Add the default tag to the beginning of the list to indicate
        // that no tag is selected and all channels shall be shown
        List<ChannelTag> channelTagList = appRepository.getChannelTagData().getItems();
        channelTagList.add(0, tag);

        ChannelTagListAdapter channelTagListAdapter = new ChannelTagListAdapter(
                activity, channelTagList, channelTagId,
                appRepository.getChannelData().getItems().size());

        // Show the dialog that shows all available channel tags. When the
        // user has selected a tag, restart the loader to loadRecordingById the updated channel list
        final MaterialDialog dialog = new MaterialDialog.Builder(activity)
                .title(R.string.tags)
                .adapter(channelTagListAdapter, null)
                .build();

        // Set the callback to handle clicks. This needs to be done after the
        // dialog creation so that the inner method has access to the dialog variable
        channelTagListAdapter.setCallback(which -> {
            if (callback != null) {
                callback.onChannelTagIdSelected(which);
            }
            if (dialog != null) {
                dialog.dismiss();
            }
        });
        dialog.show();
        return true;
    }

    public boolean handleMenuDownloadSelection(int dvrId) {
        Timber.d("Stating download of recording id " + dvrId);
        Activity activity = this.activity.get();
        if (activity == null) {
            Timber.d("Weak reference to activity is null");
            return false;
        }
        new DownloadRecordingManager(activity, dvrId);
        return true;
    }

    public boolean handleMenuSearchImdbWebsite(String title) {
        Activity activity = this.activity.get();
        if (activity == null) {
            Timber.d("Weak reference to activity is null");
            return false;
        }
        try {
            String url = URLEncoder.encode(title, "utf-8");
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("imdb:///find?s=tt&q=" + url));
            PackageManager packageManager = activity.getPackageManager();
            if (packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).isEmpty()) {
                intent.setData(Uri.parse("http://www.imdb.com/find?s=tt&q=" + url));
            }
            activity.startActivity(intent);
            activity.finish();
        } catch (UnsupportedEncodingException e) {
            // NOP
        }
        return true;
    }

    public boolean handleMenuSearchFileAffinityWebsite(String title) {
        Activity activity = this.activity.get();
        if (activity == null) {
            Timber.d("Weak reference to activity is null");
            return false;
        }
        try {
            String url = URLEncoder.encode(title, "utf-8");
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://www.filmaffinity.com/es/search.php?stext=" + url));
            activity.startActivity(intent);
            activity.finish();
        } catch (UnsupportedEncodingException e) {
            // NOP
        }
        return true;
    }

    public boolean handleMenuSearchEpgSelection(String title) {
        return handleMenuSearchEpgSelection(title, 0);
    }

    public boolean handleMenuSearchEpgSelection(String title, int channelId) {
        Activity activity = this.activity.get();
        if (activity == null) {
            Timber.d("Weak reference to activity is null");
            return false;
        }
        Intent intent = new Intent(activity, SearchActivity.class);
        intent.setAction(Intent.ACTION_SEARCH);
        intent.putExtra(SearchManager.QUERY, title);
        intent.putExtra("type", "program_guide");
        intent.putExtra("channelId", channelId);
        activity.startActivity(intent);
        activity.finish();
        return true;
    }

    public boolean handleMenuRecordSelection(int eventId) {
        Activity activity = this.activity.get();
        if (activity == null) {
            Timber.d("Weak reference to activity is null");
            return false;
        }
        Timber.d("handleMenuRecordSelection() called with: eventId = [" + eventId + "]");
        final Intent intent = new Intent(activity, EpgSyncService.class);
        intent.setAction("addDvrEntry");
        intent.putExtra("eventId", eventId);

        ServerProfile profile = appRepository.getServerProfileData().getItemById(serverStatus.getRecordingServerProfileId());
        if (MiscUtils.isServerProfileEnabled(profile, serverStatus)) {
            intent.putExtra("configName", profile.getName());
        }
        activity.startService(intent);
        return true;
    }

    public boolean handleMenuSeriesRecordSelection(String title) {
        Activity activity = this.activity.get();
        if (activity == null) {
            Timber.d("Weak reference to activity is null");
            return false;
        }
        final Intent intent = new Intent(activity, EpgSyncService.class);
        intent.setAction("addAutorecEntry");
        intent.putExtra("title", title);

        ServerProfile profile = appRepository.getServerProfileData().getItemById(serverStatus.getRecordingServerProfileId());
        if (MiscUtils.isServerProfileEnabled(profile, serverStatus)) {
            intent.putExtra("configName", profile.getName());
        }
        activity.startService(intent);
        return true;
    }

    public boolean handleMenuStopRecordingSelection(@Nullable Recording recording, @Nullable RecordingRemovedCallback callback) {
        Activity activity = this.activity.get();
        if (activity == null || recording == null) {
            Timber.d("Weak reference to activity is null");
            return false;
        }
        Timber.d("Stopping recording " + recording.getTitle());
        // Show a confirmation dialog before stopping the recording
        new MaterialDialog.Builder(activity)
                .title(R.string.record_stop)
                .content(activity.getString(R.string.stop_recording, recording.getTitle()))
                .negativeText(R.string.cancel)
                .positiveText(R.string.stop)
                .onPositive((dialog, which) -> {
                    final Intent intent = new Intent(activity, EpgSyncService.class);
                    intent.setAction("stopDvrEntry");
                    intent.putExtra("id", recording.getId());
                    activity.startService(intent);
                    if (callback != null) {
                        callback.onRecordingRemoved();
                    }
                })
                .show();
        return true;
    }

    public boolean handleMenuRemoveRecordingSelection(@Nullable Recording recording, @Nullable RecordingRemovedCallback callback) {
        Activity activity = this.activity.get();
        if (activity == null || recording == null) {
            Timber.d("Weak reference to activity is null");
            return false;
        }
        Timber.d("Removing recording " + recording.getTitle());
        // Show a confirmation dialog before removing the recording
        new MaterialDialog.Builder(activity)
                .title(R.string.record_remove)
                .content(activity.getString(R.string.remove_recording, recording.getTitle()))
                .negativeText(R.string.cancel)
                .positiveText(R.string.remove)
                .onPositive((dialog, which) -> {
                    final Intent intent = new Intent(activity, EpgSyncService.class);
                    intent.setAction("deleteDvrEntry");
                    intent.putExtra("id", recording.getId());
                    activity.startService(intent);
                    if (callback != null) {
                        callback.onRecordingRemoved();
                    }
                })
                .show();
        return true;
    }

    public boolean handleMenuCancelRecordingSelection(@Nullable Recording recording, @Nullable RecordingRemovedCallback callback) {
        Activity activity = this.activity.get();
        if (activity == null || recording == null) {
            Timber.d("Weak reference to activity is null");
            return false;
        }
        Timber.d("Cancelling recording " + recording.getTitle());
        // Show a confirmation dialog before cancelling the recording
        new MaterialDialog.Builder(activity)
                .title(R.string.record_remove)
                .content(activity.getString(R.string.cancel_recording, recording.getTitle()))
                .negativeText(R.string.cancel)
                .positiveText(R.string.remove)
                .onPositive((dialog, which) -> {
                    final Intent intent = new Intent(activity, EpgSyncService.class);
                    intent.setAction("cancelDvrEntry");
                    intent.putExtra("id", recording.getId());
                    activity.startService(intent);
                    if (callback != null) {
                        callback.onRecordingRemoved();
                    }
                })
                .show();
        return true;
    }

    public boolean handleMenuRemoveSeriesRecordingSelection(@Nullable SeriesRecording recording, @Nullable RecordingRemovedCallback callback) {
        Activity activity = this.activity.get();
        if (activity == null || recording == null) {
            Timber.d("Weak reference to activity is null");
            return false;
        }
        Timber.d("Removing series recording " + recording.getTitle());
        // Show a confirmation dialog before removing the recording
        new MaterialDialog.Builder(activity)
                .title(R.string.record_remove)
                .content(activity.getString(R.string.remove_series_recording, recording.getTitle()))
                .negativeText(R.string.cancel)
                .positiveText(R.string.remove)
                .onPositive((dialog, which) -> {
                    final Intent intent = new Intent(activity, EpgSyncService.class);
                    intent.setAction("deleteAutorecEntry");
                    intent.putExtra("id", recording.getId());
                    activity.startService(intent);
                    if (callback != null) {
                        callback.onRecordingRemoved();
                    }
                })
                .show();
        return true;
    }

    public boolean handleMenuRemoveTimerRecordingSelection(@Nullable TimerRecording recording, @Nullable RecordingRemovedCallback callback) {
        Activity activity = this.activity.get();
        if (activity == null || recording == null) {
            Timber.d("Weak reference to activity is null");
            return false;
        }

        final String name = (recording.getName() != null && recording.getName().length() > 0) ? recording.getName() : "";
        final String title = recording.getTitle() != null ? recording.getTitle() : "";
        final String displayTitle = (name.length() > 0 ? name : title);
        Timber.d("Removing timer recording " + displayTitle);

        // Show a confirmation dialog before removing the recording
        new MaterialDialog.Builder(activity)
                .title(R.string.record_remove)
                .content(activity.getString(R.string.remove_timer_recording, displayTitle))
                .negativeText(R.string.cancel)
                .positiveText(R.string.remove)
                .onPositive((dialog, which) -> {
                    final Intent intent = new Intent(activity, EpgSyncService.class);
                    intent.setAction("deleteTimerecEntry");
                    intent.putExtra("id", recording.getId());
                    activity.startService(intent);
                    if (callback != null) {
                        callback.onRecordingRemoved();
                    }
                })
                .show();
        return true;
    }

    public boolean handleMenuRemoveAllRecordingsSelection(@NonNull List<Recording> items) {
        Activity activity = this.activity.get();
        if (activity == null) {
            Timber.d("Weak reference to activity is null");
            return false;
        }
        new MaterialDialog.Builder(activity)
                .title(R.string.record_remove_all)
                .content(R.string.confirm_remove_all)
                .positiveText(activity.getString(R.string.remove))
                .negativeText(activity.getString(R.string.cancel))
                .onPositive((dialog, which) -> new Thread() {
                    public void run() {
                        for (Recording item : items) {
                            final Intent intent = new Intent(activity, EpgSyncService.class);
                            intent.putExtra("id", item.getId());
                            if (item.isRecording() || item.isScheduled()) {
                                intent.setAction("cancelDvrEntry");
                            } else {
                                intent.setAction("deleteDvrEntry");
                            }
                            activity.startService(intent);
                            try {
                                sleep(500);
                            } catch (InterruptedException e) {
                                // NOP
                            }
                        }
                    }
                }.start()).show();
        return true;
    }

    public boolean handleMenuRemoveAllSeriesRecordingSelection(@NonNull List<SeriesRecording> items) {
        Activity activity = this.activity.get();
        if (activity == null) {
            Timber.d("Weak reference to activity is null");
            return false;
        }
        new MaterialDialog.Builder(activity)
                .title(R.string.record_remove_all)
                .content(R.string.remove_all_recordings)
                .positiveText(activity.getString(R.string.remove))
                .negativeText(activity.getString(R.string.cancel))
                .onPositive((dialog, which) -> new Thread() {
                    public void run() {
                        for (SeriesRecording item : items) {
                            final Intent intent = new Intent(activity, EpgSyncService.class);
                            intent.setAction("deleteAutorecEntry");
                            intent.putExtra("id", item.getId());
                            activity.startService(intent);
                            try {
                                sleep(500);
                            } catch (InterruptedException e) {
                                // NOP
                            }
                        }
                    }
                }.start()).show();
        return true;
    }

    public boolean handleMenuRemoveAllTimerRecordingSelection(@NonNull List<TimerRecording> items) {
        Activity activity = this.activity.get();
        if (activity == null) {
            Timber.d("Weak reference to activity is null");
            return false;
        }
        new MaterialDialog.Builder(activity)
                .title(R.string.record_remove_all)
                .content(R.string.remove_all_recordings)
                .positiveText(activity.getString(R.string.remove))
                .negativeText(activity.getString(R.string.cancel))
                .onPositive((dialog, which) -> new Thread() {
                    public void run() {
                        for (TimerRecording item : items) {
                            final Intent intent = new Intent(activity, EpgSyncService.class);
                            intent.setAction("deleteTimerecEntry");
                            intent.putExtra("id", item.getId());
                            activity.startService(intent);
                            try {
                                sleep(500);
                            } catch (InterruptedException e) {
                                // NOP
                            }
                        }
                    }
                }.start()).show();
        return true;
    }

    public boolean handleMenuCustomRecordSelection(final int eventId, final int channelId) {
        Activity activity = this.activity.get();
        if (activity == null) {
            Timber.d("Weak reference to activity is null");
            return false;
        }

        final String[] dvrConfigList = appRepository.getServerProfileData().getRecordingProfileNames();

        // Get the selected recording profile to highlight the
        // correct item in the list of the selection dialog
        int dvrConfigNameValue = 0;

        ServerProfile serverProfile = appRepository.getServerProfileData().getItemById(serverStatus.getRecordingServerProfileId());
        if (serverProfile != null) {
            for (int i = 0; i < dvrConfigList.length; i++) {
                if (dvrConfigList[i].equals(serverProfile.getName())) {
                    dvrConfigNameValue = i;
                    break;
                }
            }
        }
        // Create the dialog to show the available profiles
        new MaterialDialog.Builder(activity)
                .title(R.string.select_dvr_config)
                .items(dvrConfigList)
                .itemsCallbackSingleChoice(dvrConfigNameValue, (dialog, view, which, text) -> {
                    // Pass over the
                    Intent intent = new Intent(activity, EpgSyncService.class);
                    intent.setAction("addDvrEntry");
                    intent.putExtra("eventId", eventId);
                    intent.putExtra("channelId", channelId);
                    intent.putExtra("configName", dvrConfigList[which]);
                    activity.startService(intent);
                    return true;
                })
                .show();
        return true;
    }

    public void onPreparePopupMenu(@NonNull Menu menu, @Nullable Program program, @Nullable Recording recording, boolean isNetworkAvailable) {
        Activity activity = this.activity.get();
        if (activity == null) {
            Timber.d("Weak reference to activity is null");
            return;
        }

        // Hide the menus because the ones in the toolbar are not hidden when set in the xml
        for (int i = 0; i < menu.size(); i++) {
            menu.getItem(i).setVisible(false);
        }

        MenuItem recordOnceMenuItem = menu.findItem(R.id.menu_record_once);
        MenuItem recordOnceAndEditMenuItem = menu.findItem(R.id.menu_record_once_and_edit);
        MenuItem recordOnceCustomProfileMenuItem = menu.findItem(R.id.menu_record_once_custom_profile);
        MenuItem recordSeriesMenuItem = menu.findItem(R.id.menu_record_series);
        MenuItem recordRemoveMenuItem = menu.findItem(R.id.menu_record_remove);
        MenuItem recordStopMenuItem = menu.findItem(R.id.menu_record_stop);
        MenuItem recordCancelMenuItem = menu.findItem(R.id.menu_record_cancel);
        MenuItem playMenuItem = menu.findItem(R.id.menu_play);
        MenuItem castMenuItem = menu.findItem(R.id.menu_cast);
        MenuItem addReminderMenuItem = menu.findItem(R.id.menu_add_notification);

        if (isNetworkAvailable) {
            if (recording == null || (!recording.isRecording()
                    && !recording.isScheduled()
                    && !recording.isCompleted())) {
                Timber.d("Recording is not recording or scheduled");
                recordOnceMenuItem.setVisible(true);
                recordOnceAndEditMenuItem.setVisible(isUnlocked);
                recordOnceCustomProfileMenuItem.setVisible(isUnlocked);
                recordSeriesMenuItem.setVisible(serverStatus.getHtspVersion() >= 13);

            } else if (recording.isCompleted()) {
                Timber.d("Recording is completed ");
                playMenuItem.setVisible(true);
                CastSession castSession = CastContext.getSharedInstance(activity).getSessionManager().getCurrentCastSession();
                castMenuItem.setVisible(castSession != null);
                recordRemoveMenuItem.setVisible(true);

            } else if (recording.isScheduled() && !recording.isRecording()) {
                Timber.d("Recording is scheduled");
                recordCancelMenuItem.setVisible(true);

            } else if (recording.isRecording()) {
                Timber.d("Recording is being recorded");
                playMenuItem.setVisible(true);
                CastSession castSession = CastContext.getSharedInstance(activity).getSessionManager().getCurrentCastSession();
                castMenuItem.setVisible(castSession != null);
                recordStopMenuItem.setVisible(true);

            } else if (recording.isFailed() || recording.isFileMissing() || recording.isMissed() || recording.isAborted()) {
                Timber.d("Recording is something else");
                recordRemoveMenuItem.setVisible(true);
            }
        }
        // Show the add reminder menu only for programs and
        // recordings where the start time is in the future.
        if (isUnlocked && sharedPreferences.getBoolean("notifications_enabled", true)) {
            long currentTime = new Date().getTime();
            long startTime = currentTime;
            if (program != null) {
                startTime = program.getStart();
            }
            addReminderMenuItem.setVisible(startTime > currentTime);
        }
    }

    public void onPreparePopupSearchMenu(@NonNull Menu menu, boolean isNetworkAvailable) {
        Activity activity = this.activity.get();
        if (activity == null) {
            Timber.d("Weak reference to activity is null");
            return;
        }

        MenuItem searchImdbMenuItem = menu.findItem(R.id.menu_search_imdb);
        if (searchImdbMenuItem != null) {
            searchImdbMenuItem.setVisible(
                    isNetworkAvailable && sharedPreferences.getBoolean("search_on_imdb_menu_enabled", true));
        }

        MenuItem searchFileAffinityMenuItem = menu.findItem(R.id.menu_search_fileaffinity);
        if (searchFileAffinityMenuItem != null) {
            searchFileAffinityMenuItem.setVisible(
                    isNetworkAvailable && sharedPreferences.getBoolean("search_on_fileaffinity_menu_enabled", true));
        }

        MenuItem searchEpgMenuItem = menu.findItem(R.id.menu_search_epg);
        if (searchEpgMenuItem != null) {
            searchEpgMenuItem.setVisible(
                    isNetworkAvailable && sharedPreferences.getBoolean("search_epg_menu_enabled", true));
        }
    }

    public boolean handleMenuReconnectSelection() {
        Activity activity = this.activity.get();
        if (activity == null) {
            Timber.d("Weak reference to activity is null");
            return false;
        }

        new MaterialDialog.Builder(activity)
                .title(R.string.dialog_title_reconnect_to_server)
                .content(R.string.dialog_content_reconnect_to_server)
                .negativeText(R.string.cancel)
                .positiveText(R.string.reconnect)
                .onPositive((dialog, which) -> {
                    Timber.d("Reconnect requested, stopping service and clearing last update information");

                    activity.stopService(new Intent(activity, EpgSyncService.class));
                    // Update the connection with the information that a new sync is required.
                    Connection connection = appRepository.getConnectionData().getActiveItem();
                    connection.setSyncRequired(true);
                    connection.setLastUpdate(0);
                    appRepository.getConnectionData().updateItem(connection);
                    // Finally restart the application to show the startup fragment
                    Intent intent = new Intent(activity, SplashActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    activity.startActivity(intent);
                })
                .show();
        return true;
    }

    public boolean handleMenuAddNotificationSelection(@NonNull Program program) {
        Timber.d("Adding notification for program " + program.getTitle());

        Activity activity = this.activity.get();
        if (activity == null) {
            Timber.d("Weak reference to activity is null");
            return false;
        }

        Integer offset = Integer.valueOf(sharedPreferences.getString("notification_lead_time", "0"));
        ServerProfile profile = appRepository.getServerProfileData().getItemById(serverStatus.getRecordingServerProfileId());
        NotificationUtils.addProgramNotification(activity, program, offset, profile, serverStatus);
        return true;
    }

    public boolean handleMenuPlayChannel(int channelId) {
        Activity activity = this.activity.get();
        if (activity == null) {
            Timber.d("Weak reference to activity is null");
            return false;
        }

        if (isUnlocked && sharedPreferences.getBoolean("internal_player_enabled", false)) {
            Intent intent = new Intent(activity, HtspPlaybackActivity.class);
            intent.putExtra("channelId", channelId);
            activity.startActivity(intent);
        } else {
            Intent intent = new Intent(activity, PlayChannelActivity.class);
            intent.putExtra("channelId", channelId);
            activity.startActivity(intent);
        }
        return true;
    }

    public boolean handleMenuPlayRecording(int dvrId) {
        Activity activity = this.activity.get();
        if (activity == null) {
            Timber.d("Weak reference to activity is null");
            return false;
        }
        if (isUnlocked && sharedPreferences.getBoolean("internal_player_enabled", false)) {
            Intent intent = new Intent(activity, HtspPlaybackActivity.class);
            intent.putExtra("dvrId", dvrId);
            activity.startActivity(intent);
        } else {
            Intent intent = new Intent(activity, PlayRecordingActivity.class);
            intent.putExtra("dvrId", dvrId);
            activity.startActivity(intent);
        }
        return true;
    }
}
