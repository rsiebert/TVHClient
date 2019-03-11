package org.tvheadend.tvhclient.util.menu;


import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;

import com.afollestad.materialdialogs.MaterialDialog;
import com.crashlytics.android.Crashlytics;

import org.tvheadend.tvhclient.MainApplication;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.repository.AppRepository;
import org.tvheadend.tvhclient.data.service.HtspService;
import org.tvheadend.tvhclient.domain.entity.Connection;
import org.tvheadend.tvhclient.domain.entity.EpgProgram;
import org.tvheadend.tvhclient.domain.entity.Program;
import org.tvheadend.tvhclient.domain.entity.Recording;
import org.tvheadend.tvhclient.domain.entity.SeriesRecording;
import org.tvheadend.tvhclient.domain.entity.ServerProfile;
import org.tvheadend.tvhclient.domain.entity.ServerStatus;
import org.tvheadend.tvhclient.domain.entity.TimerRecording;
import org.tvheadend.tvhclient.ui.features.channels.ChannelDisplayOptionListener;
import org.tvheadend.tvhclient.ui.features.download.DownloadRecordingManager;
import org.tvheadend.tvhclient.ui.features.dvr.RecordingRemovedCallback;
import org.tvheadend.tvhclient.ui.features.notification.NotificationUtils;
import org.tvheadend.tvhclient.ui.features.playback.external.CastChannelActivity;
import org.tvheadend.tvhclient.ui.features.playback.external.CastRecordingActivity;
import org.tvheadend.tvhclient.ui.features.playback.external.PlayChannelActivity;
import org.tvheadend.tvhclient.ui.features.playback.external.PlayRecordingActivity;
import org.tvheadend.tvhclient.ui.features.playback.internal.PlaybackActivity;
import org.tvheadend.tvhclient.ui.features.startup.SplashActivity;
import org.tvheadend.tvhclient.util.MiscUtils;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.fabric.sdk.android.Fabric;
import timber.log.Timber;

public class MenuUtils {

    @Inject
    protected AppRepository appRepository;
    @Inject
    protected SharedPreferences sharedPreferences;

    private final boolean isUnlocked;
    private final WeakReference<Activity> activity;
    private final ServerStatus serverStatus;
    private final Connection connection;

    public MenuUtils(@NonNull Activity activity) {
        MainApplication.getComponent().inject(this);

        this.activity = new WeakReference<>(activity);
        this.isUnlocked = MainApplication.getInstance().isUnlocked();
        this.connection = appRepository.getConnectionData().getActiveItem();
        this.serverStatus = appRepository.getServerStatusData().getActiveItem();
    }

    public boolean handleMenuTimeSelection(int currentSelection, int intervalInHours, int maxIntervalsToShow, @Nullable ChannelDisplayOptionListener callback) {
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
        long timeInMillis = System.currentTimeMillis() + 1000 * 60 * 60 * intervalInHours;

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

    public boolean handleMenuChannelSortOrderSelection(@NonNull ChannelDisplayOptionListener callback) {
        Activity activity = this.activity.get();
        if (activity == null) {
            Timber.d("Weak reference to activity is null");
            return false;
        }
        //noinspection ConstantConditions
        int channelSortOrder = Integer.valueOf(sharedPreferences.getString("channel_sort_order", activity.getResources().getString(R.string.pref_default_channel_sort_order)));
        new MaterialDialog.Builder(activity)
                .title(R.string.select_dvr_config)
                .items(activity.getResources().getStringArray(R.array.pref_sort_channels_names))
                .itemsIds(activity.getResources().getIntArray(R.array.pref_sort_channels_ids))
                .itemsCallbackSingleChoice(channelSortOrder, (dialog, view, which, text) -> {

                    Timber.d("New selected channel sort order changed from " + channelSortOrder + " to " + which);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("channel_sort_order", String.valueOf(which));
                    editor.apply();

                    callback.onChannelSortOrderSelected(which);
                    return true;
                })
                .show();
        return false;
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

    public boolean handleMenuRecordSelection(int eventId) {
        Activity activity = this.activity.get();
        if (activity == null) {
            Timber.d("Weak reference to activity is null");
            return false;
        }
        Timber.d("handleMenuRecordSelection() called with: eventId = [" + eventId + "]");
        final Intent intent = new Intent(activity, HtspService.class);
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
        final Intent intent = new Intent(activity, HtspService.class);
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
                    final Intent intent = new Intent(activity, HtspService.class);
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
                    final Intent intent = new Intent(activity, HtspService.class);
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
                    final Intent intent = new Intent(activity, HtspService.class);
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
                    final Intent intent = new Intent(activity, HtspService.class);
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
                    final Intent intent = new Intent(activity, HtspService.class);
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
                            final Intent intent = new Intent(activity, HtspService.class);
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
                            final Intent intent = new Intent(activity, HtspService.class);
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
                            final Intent intent = new Intent(activity, HtspService.class);
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
                    Intent intent = new Intent(activity, HtspService.class);
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
                    Timber.d("Reconnect requested, stopping service and updating active connection to require a full sync");
                    activity.stopService(new Intent(activity, HtspService.class));

                    if (connection != null) {
                        Timber.d("Updating active connection to request a full sync");
                        connection.setSyncRequired(true);
                        connection.setLastUpdate(0);
                        appRepository.getConnectionData().updateItem(connection);
                    } else {
                        String msg = "Reconnect requested, trying to get active connection from database returned no entry";
                        Timber.e(msg);
                        if (Fabric.isInitialized()) {
                            Crashlytics.logException(new Exception(msg));
                        }
                    }
                    // Finally restart the application to show the startup fragment
                    Intent intent = new Intent(activity, SplashActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    activity.startActivity(intent);
                })
                .show();
        return true;
    }

    public boolean handleMenuAddNotificationSelection(@NonNull EpgProgram program) {
        Timber.d("Adding notification for program " + program.getTitle());

        Activity activity = this.activity.get();
        if (activity == null) {
            Timber.d("Weak reference to activity is null");
            return false;
        }

        //noinspection ConstantConditions
        ServerProfile profile = appRepository.getServerProfileData().getItemById(serverStatus.getRecordingServerProfileId());
        NotificationUtils.addNotification(activity, program, profile);
        return true;
    }

    public boolean handleMenuAddNotificationSelection(@NonNull Program program) {
        Timber.d("Adding notification for program " + program.getTitle());

        Activity activity = this.activity.get();
        if (activity == null) {
            Timber.d("Weak reference to activity is null");
            return false;
        }

        //noinspection ConstantConditions
        ServerProfile profile = appRepository.getServerProfileData().getItemById(serverStatus.getRecordingServerProfileId());
        NotificationUtils.addNotification(activity, program, profile);
        return true;
    }

    public boolean handleMenuPlayChannel(int channelId) {
        Activity activity = this.activity.get();
        if (activity == null) {
            Timber.d("Weak reference to activity is null");
            return false;
        }

        if (isUnlocked && sharedPreferences.getBoolean("internal_player_for_channels_enabled",
                activity.getResources().getBoolean(R.bool.pref_default_internal_player_enabled))) {
            Intent intent = new Intent(activity, PlaybackActivity.class);
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
        if (isUnlocked && sharedPreferences.getBoolean("internal_player_for_recordings_enabled",
                activity.getResources().getBoolean(R.bool.pref_default_internal_player_enabled))) {
            Intent intent = new Intent(activity, PlaybackActivity.class);
            intent.putExtra("dvrId", dvrId);
            activity.startActivity(intent);
        } else {
            Intent intent = new Intent(activity, PlayRecordingActivity.class);
            intent.putExtra("dvrId", dvrId);
            activity.startActivity(intent);
        }
        return true;
    }

    public boolean handleMenuCast(String type, int id) {
        Activity activity = this.activity.get();
        if (activity == null) {
            Timber.d("Weak reference to activity is null");
            return false;
        }
        Intent intent;
        switch (type) {
            case "dvrId":
                intent = new Intent(activity, CastRecordingActivity.class);
                intent.putExtra("dvrId", id);
                activity.startActivity(intent);
                return true;
            case "channelId":
                intent = new Intent(activity, CastChannelActivity.class);
                intent.putExtra("channelId", id);
                activity.startActivity(intent);
                return true;
        }
        return false;
    }

    public void handleMenuPlayChannelIcon(int channelId) {
        Activity activity = this.activity.get();
        if (activity == null) {
            Timber.d("Weak reference to activity is null");
            return;
        }

        //noinspection ConstantConditions
        int channelIconAction = Integer.valueOf(sharedPreferences.getString("channel_icon_action", activity.getResources().getString(R.string.pref_default_channel_icon_action)));
        if (channelIconAction == 1) {
            handleMenuPlayChannel(channelId);
        } else if (channelIconAction == 2) {
            if (MiscUtils.getCastSession(activity) != null) {
                handleMenuCast("channelId", channelId);
            } else {
                handleMenuPlayChannel(channelId);
            }
        }
    }

    public void handleMenuPlayRecordingIcon(int recordingId) {
        Activity activity = this.activity.get();
        if (activity == null) {
            Timber.d("Weak reference to activity is null");
            return;
        }

        //noinspection ConstantConditions
        int channelIconAction = Integer.valueOf(sharedPreferences.getString("channel_icon_action", activity.getResources().getString(R.string.pref_default_channel_icon_action)));
        if (channelIconAction == 1) {
            handleMenuPlayRecording(recordingId);
        } else if (channelIconAction == 2) {
            if (MiscUtils.getCastSession(activity) != null) {
                handleMenuCast("dvrId", recordingId);
            } else {
                handleMenuPlayRecording(recordingId);
            }
        }
    }
}
