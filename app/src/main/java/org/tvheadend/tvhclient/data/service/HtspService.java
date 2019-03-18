package org.tvheadend.tvhclient.data.service;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.tvheadend.tvhclient.MainApplication;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.repository.AppRepository;
import org.tvheadend.tvhclient.data.service.htsp.HtspConnection;
import org.tvheadend.tvhclient.data.service.htsp.HtspConnectionStateListener;
import org.tvheadend.tvhclient.data.service.htsp.HtspFileInputStream;
import org.tvheadend.tvhclient.data.service.htsp.HtspMessage;
import org.tvheadend.tvhclient.data.service.htsp.HtspMessageListener;
import org.tvheadend.tvhclient.data.worker.EpgDataUpdateWorker;
import org.tvheadend.tvhclient.domain.entity.Channel;
import org.tvheadend.tvhclient.domain.entity.ChannelTag;
import org.tvheadend.tvhclient.domain.entity.Connection;
import org.tvheadend.tvhclient.domain.entity.Program;
import org.tvheadend.tvhclient.domain.entity.Recording;
import org.tvheadend.tvhclient.domain.entity.SeriesRecording;
import org.tvheadend.tvhclient.domain.entity.ServerProfile;
import org.tvheadend.tvhclient.domain.entity.ServerStatus;
import org.tvheadend.tvhclient.domain.entity.TagAndChannel;
import org.tvheadend.tvhclient.domain.entity.TimerRecording;
import org.tvheadend.tvhclient.ui.common.SnackbarUtils;
import org.tvheadend.tvhclient.ui.features.notification.NotificationUtils;
import org.tvheadend.tvhclient.util.MiscUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationManagerCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import timber.log.Timber;

public class HtspService extends Service implements HtspConnectionStateListener, HtspMessageListener {

    private ScheduledExecutorService execService;
    private HtspConnection htspConnection;
    private Connection connection;

    @Inject
    protected Context appContext;
    @Inject
    protected AppRepository appRepository;
    @Inject
    protected SharedPreferences sharedPreferences;

    private final ArrayList<Program> pendingEventOps = new ArrayList<>();
    private final ArrayList<Channel> pendingChannelOps = new ArrayList<>();
    private final ArrayList<ChannelTag> pendingChannelTagOps = new ArrayList<>();
    private final ArrayList<Recording> pendingRecordingOps = new ArrayList<>();
    private boolean initialSyncWithServerRunning;
    private boolean syncEventsRequired;
    private boolean syncRequired;
    private boolean firstEventReceived = false;
    private int htspVersion = 13;
    private ServerStatus serverStatus;
    private int connectionTimeout;

    @Override
    public void onCreate() {
        Timber.d("Starting service");
        MainApplication.getComponent().inject(this);

        execService = Executors.newScheduledThreadPool(10);
        serverStatus = appRepository.getServerStatusData().getActiveItem();
        htspVersion = serverStatus.getHtspVersion();

        //noinspection ConstantConditions
        connectionTimeout = Integer.valueOf(sharedPreferences.getString("connection_timeout", appContext.getResources().getString(R.string.pref_default_connection_timeout))) * 1000;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final String action = intent.getAction();
        if (action == null || action.isEmpty()) {
            return START_NOT_STICKY;
        }
        Timber.d("Received command " + action + " for service");

        switch (action) {
            case "connect":
                Timber.d("Connection to server requested");
                startHtspConnection();
                break;
            case "reconnect":
                Timber.d("Reconnection to server requested");
                if (htspConnection == null || htspConnection.isNotConnected()) {
                    Timber.d("Reconnecting to server because no previous connection existed or not connected anymore");
                    startHtspConnection();
                } else {
                    Timber.d("Not reconnecting to server because we are still connected");
                }
                break;
            case "getDiskSpace":
                getDiscSpace();
                break;
            case "getSysTime":
                getSystemTime();
                break;
            case "getChannel":
                getChannel(intent);
                break;
            case "getEvent":
                getEvent(intent);
                break;
            case "getEvents":
                getEvents(intent);
                break;
            case "epgQuery":
                getEpgQuery(intent);
                break;
            case "addDvrEntry":
                addDvrEntry(intent);
                break;
            case "updateDvrEntry":
                updateDvrEntry(intent);
                break;
            case "cancelDvrEntry":
            case "deleteDvrEntry":
            case "stopDvrEntry":
                removeDvrEntry(intent);
                break;
            case "addAutorecEntry":
                addAutorecEntry(intent);
                break;
            case "updateAutorecEntry":
                updateAutorecEntry(intent);
                break;
            case "deleteAutorecEntry":
                deleteAutorecEntry(intent);
                break;
            case "addTimerecEntry":
                addTimerrecEntry(intent);
                break;
            case "updateTimerecEntry":
                updateTimerrecEntry(intent);
                break;
            case "deleteTimerecEntry":
                deleteTimerrecEntry(intent);
                break;
            case "getTicket":
                getTicket(intent);
                break;
            case "getProfiles":
                getProfiles();
                break;
            case "getDvrConfigs":
                getDvrConfigs();
                break;
            // Internal calls that are called from the intent service
            case "getMoreEvents":
                getMoreEvents(intent);
                break;
            case "loadChannelIcons":
                loadAllChannelIcons();
                break;
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        Timber.d("Stopping service");
        execService.shutdown();
        stopHtspConnection();
    }

    private void startHtspConnection() {
        Timber.d("Starting connection");
        stopHtspConnection();

        connection = appRepository.getConnectionData().getActiveItem();

        htspConnection = new HtspConnection(
                connection.getUsername(), connection.getPassword(),
                connection.getHostname(), connection.getPort(),
                connectionTimeout,
                this, this);
        // Since this is blocking, spawn to a new thread
        execService.execute(() -> {
            htspConnection.openConnection();
            htspConnection.authenticate();
        });
    }

    private void stopHtspConnection() {
        Timber.d("Stopping connection");
        if (htspConnection != null) {
            htspConnection.closeConnection();
            htspConnection = null;
        }
    }

    @Override
    public void onMessage(HtspMessage message) {
        String method = message.getMethod();
        switch (method) {
            case "tagAdd":
                onTagAdd(message);
                break;
            case "tagUpdate":
                onTagUpdate(message);
                break;
            case "tagDelete":
                onTagDelete(message);
                break;
            case "channelAdd":
                onChannelAdd(message);
                break;
            case "channelUpdate":
                onChannelUpdate(message);
                break;
            case "channelDelete":
                onChannelDelete(message);
                break;
            case "dvrEntryAdd":
                onDvrEntryAdd(message);
                break;
            case "dvrEntryUpdate":
                onDvrEntryUpdate(message);
                break;
            case "dvrEntryDelete":
                onDvrEntryDelete(message);
                break;
            case "timerecEntryAdd":
                onTimerRecEntryAdd(message);
                break;
            case "timerecEntryUpdate":
                onTimerRecEntryUpdate(message);
                break;
            case "timerecEntryDelete":
                onTimerRecEntryDelete(message);
                break;
            case "autorecEntryAdd":
                onAutorecEntryAdd(message);
                break;
            case "autorecEntryUpdate":
                onAutorecEntryUpdate(message);
                break;
            case "autorecEntryDelete":
                onAutorecEntryDelete(message);
                break;
            case "eventAdd":
                onEventAdd(message);
                break;
            case "eventUpdate":
                onEventUpdate(message);
                break;
            case "eventDelete":
                onEventDelete(message);
                break;
            case "initialSyncCompleted":
                onInitialSyncCompleted();
                break;
            case "getSysTime":
                onSystemTime(message);
                break;
            case "getDiskSpace":
                onDiskSpace(message);
                break;
            case "getProfiles":
                onHtspProfiles(message);
                break;
            case "getDvrConfigs":
                onDvrConfigs(message);
                break;
            case "getEvents":
                onGetEvents(message, new Intent());
                break;
            case "serverStatus":
                onServerStatus(message);
                break;
            default:
                break;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onAuthenticationStateChange(@NonNull HtspConnection.AuthenticationState state) {
        Timber.d("Authentication state changed to " + state);

        switch (state) {
            case FAILED:
                sendSyncStateMessage(SyncStateReceiver.State.FAILED,
                        getString(R.string.authentication_failed), "");
                break;
            case FAILED_BAD_CREDENTIALS:
                sendSyncStateMessage(SyncStateReceiver.State.FAILED,
                        getString(R.string.authentication_failed),
                        getString(R.string.bad_username_or_password));
                break;
            case AUTHENTICATING:

                break;
            case AUTHENTICATED:
                sendSyncStateMessage(SyncStateReceiver.State.CONNECTED,
                        getString(R.string.connected_to_server), "");
                startAsyncCommunicationWithServer();
                break;
        }
    }

    @Override
    public void onConnectionStateChange(@NonNull HtspConnection.ConnectionState state) {
        Timber.d("Simple HTSP connection state changed, state is " + state);

        switch (state) {
            case FAILED:
                sendSyncStateMessage(SyncStateReceiver.State.FAILED,
                        getString(R.string.connection_failed),
                        null);
                break;
            case FAILED_CONNECTING_TO_SERVER:
                sendSyncStateMessage(SyncStateReceiver.State.FAILED,
                        getString(R.string.connection_failed),
                        getString(R.string.failed_connecting_to_server));
                break;
            case FAILED_EXCEPTION_OPENING_SOCKET:
                sendSyncStateMessage(SyncStateReceiver.State.FAILED,
                        getString(R.string.connection_failed),
                        getString(R.string.failed_opening_socket));
                break;
            case FAILED_INTERRUPTED:
                sendSyncStateMessage(SyncStateReceiver.State.FAILED,
                        getString(R.string.connection_failed),
                        getString(R.string.failed_during_connection_attempt));
                break;
            case FAILED_UNRESOLVED_ADDRESS:
                sendSyncStateMessage(SyncStateReceiver.State.FAILED,
                        getString(R.string.connection_failed),
                        getString(R.string.failed_to_resolve_address));
                break;
            case CONNECTING:
                sendSyncStateMessage(SyncStateReceiver.State.CONNECTING,
                        getString(R.string.connecting_to_server), "");
                break;
            case CLOSED:
                sendSyncStateMessage(SyncStateReceiver.State.CLOSED,
                        getString(R.string.connection_closed), "");
                break;
        }
    }

    private void startAsyncCommunicationWithServer() {
        Timber.d("Starting async communication with server");

        pendingChannelOps.clear();
        pendingChannelTagOps.clear();
        pendingRecordingOps.clear();
        pendingEventOps.clear();

        initialSyncWithServerRunning = true;

        HtspMessage enableAsyncMetadataRequest = new HtspMessage();
        enableAsyncMetadataRequest.setMethod("enableAsyncMetadata");

        //noinspection ConstantConditions
        long epgMaxTime = Long.parseLong(sharedPreferences.getString("epg_max_time", appContext.getResources().getString(R.string.pref_default_epg_max_time)));
        long currentTimeInSeconds = (System.currentTimeMillis() / 1000L);
        long lastUpdateTime = connection.getLastUpdate();

        syncRequired = connection.isSyncRequired();
        Timber.d("Sync from server required: " + syncRequired);
        syncEventsRequired = syncRequired || ((lastUpdateTime + epgMaxTime) < currentTimeInSeconds);
        Timber.d("Sync events from server required: " + syncEventsRequired);

        // Send the first sync message to any broadcast listeners
        if (syncRequired || syncEventsRequired) {
            Timber.d("Sending status that sync has started");
            sendSyncStateMessage(SyncStateReceiver.State.SYNC_STARTED,
                    getString(R.string.loading_data), "");
        }
        if (syncEventsRequired) {
            Timber.d("Enabling requesting of epg data");
            Timber.d("Adding field to the enableAsyncMetadata request" +
                    ", epgMaxTime is " + (epgMaxTime + currentTimeInSeconds) +
                    ", lastUpdate time is " + (currentTimeInSeconds - 12 * 60 * 60));

            enableAsyncMetadataRequest.put("epg", 1);
            enableAsyncMetadataRequest.put("epgMaxTime", epgMaxTime + currentTimeInSeconds);
            // Only provide metadata that has changed since 12 hours ago.
            // The events past those 12 hours are not relevant and don't need to be sent by the server
            enableAsyncMetadataRequest.put("lastUpdate", (currentTimeInSeconds - 12 * 60 * 60));
        }

        htspConnection.sendMessage(enableAsyncMetadataRequest, response -> Timber.d("Received response for enableAsyncMetadata"));
    }

    private void onInitialSyncCompleted() {
        Timber.d("Received initial sync data from server");

        if (syncRequired) {
            sendSyncStateMessage(SyncStateReceiver.State.SYNC_IN_PROGRESS,
                    getString(R.string.saving_data), "");
        }

        // Save the channels and tags only during a forced sync.
        // This avoids the channel list being updated by the recyclerview
        if (syncRequired) {
            Timber.d("Sync of initial data is required, saving received channels, tags and downloading icons");
            saveAllReceivedChannels();
            saveAllReceivedChannelTags();
            loadAllChannelIcons();
        } else {
            Timber.d("Sync of initial data is not required");
        }

        // Only save any received events when they shall be loaded
        if (syncEventsRequired) {
            Timber.d("Sync of all evens is required, saving events");
            saveAllReceivedEvents();
        } else {
            Timber.d("Sync of all evens is not required");
        }

        // Recordings are always saved to keep up to
        // date with the recording states from the server
        saveAllReceivedRecordings();

        getAdditionalServerData();

        Timber.d("Updating connection status with full sync completed and last update time");
        connection.setSyncRequired(false);
        connection.setLastUpdate(System.currentTimeMillis() / 1000L);
        appRepository.getConnectionData().updateItem(connection);

        // The initial sync is considered to be done at this point.
        // Send the message to the listeners that the sync is done
        if (syncRequired || syncEventsRequired) {
            sendSyncStateMessage(SyncStateReceiver.State.SYNC_DONE,
                    getString(R.string.loading_data_done), "");
        }

        syncRequired = false;
        syncEventsRequired = false;
        initialSyncWithServerRunning = false;

        Timber.d("Deleting events in the database that are older than one day from now");
        long pastTime = System.currentTimeMillis() - (24 * 60 * 60 * 1000);
        appRepository.getProgramData().removeItemsByTime(pastTime);

        Timber.d("Starting background worker to load more epg data");
        OneTimeWorkRequest updateEpgWorker = new OneTimeWorkRequest.Builder(EpgDataUpdateWorker.class)
                .setInitialDelay(5, TimeUnit.SECONDS)
                .build();
        WorkManager.getInstance().enqueueUniqueWork("UpdateEpg", ExistingWorkPolicy.REPLACE, updateEpgWorker);

        Timber.d("Done receiving initial data from server");
    }

    /**
     * Loads additional data from the server that is required after the initial sync is done.
     * This includes the disc space, the server system time and the playback and recording profiles.
     * If the server did not provide all required default profiles, then add them here.
     */
    private void getAdditionalServerData() {
        Timber.d("Loading additional data from server");

        getDiscSpace();
        getSystemTime();
        getProfiles();
        getDvrConfigs();
        getHttpProfiles();

        addMissingHtspPlaybackProfileIfNotExists("htsp");
        addMissingHttpPlaybackProfileIfNotExists("matroska");
        addMissingHttpPlaybackProfileIfNotExists("audio");
        addMissingHttpPlaybackProfileIfNotExists("pass");

        setDefaultProfileSelection();
    }

    private void getDiscSpace() {
        HtspMessage request = new HtspMessage();
        request.setMethod("getDiskSpace");
        htspConnection.sendMessage(request, this::onDiskSpace);
    }

    private void getSystemTime() {
        HtspMessage request = new HtspMessage();
        request.setMethod("getSysTime");
        htspConnection.sendMessage(request, this::onSystemTime);
    }

    private void getDvrConfigs() {
        HtspMessage request = new HtspMessage();
        request.setMethod("getDvrConfigs");
        htspConnection.sendMessage(request, this::onDvrConfigs);
    }

    private void getProfiles() {
        HtspMessage request = new HtspMessage();
        request.setMethod("getProfiles");
        htspConnection.sendMessage(request, this::onHtspProfiles);
    }

    private void getHttpProfiles() {
        if (htspVersion >= 26) {
            HtspMessage request = new HtspMessage();
            request.setMethod("api");
            request.put("path", "profile/list");
            htspConnection.sendMessage(request, this::onHttpProfiles);
        } else {
            Timber.d("Not requesting http profiles because the API version is too low");
        }
    }

    @SuppressWarnings("SameParameterValue")
    private void addMissingHtspPlaybackProfileIfNotExists(String name) {
        boolean profileExists = false;

        String[] profileNames = appRepository.getServerProfileData().getHtspPlaybackProfileNames();
        for (String profileName : profileNames) {
            if (profileName.equals(name)) {
                Timber.d("Default htsp playback profile " + name + " exists already");
                profileExists = true;
            }
        }
        if (!profileExists) {
            Timber.d("Default htsp playback profile " + name + " does not exist, adding manually");
            ServerProfile serverProfile = new ServerProfile();
            serverProfile.setConnectionId(connection.getId());
            serverProfile.setName(name);
            serverProfile.setType("htsp_playback");
            appRepository.getServerProfileData().addItem(serverProfile);
        }
    }

    private void addMissingHttpPlaybackProfileIfNotExists(String name) {
        boolean profileExists = false;

        String[] profileNames = appRepository.getServerProfileData().getHttpPlaybackProfileNames();
        for (String profileName : profileNames) {
            if (profileName.equals(name)) {
                Timber.d("Default http playback profile " + name + " exists already");
                profileExists = true;
            }
        }
        if (!profileExists) {
            Timber.d("Default http playback profile " + name + " does not exist, adding manually");
            ServerProfile serverProfile = new ServerProfile();
            serverProfile.setConnectionId(connection.getId());
            serverProfile.setName(name);
            serverProfile.setType("http_playback");
            appRepository.getServerProfileData().addItem(serverProfile);
        }
    }

    private void setDefaultProfileSelection() {
        Timber.d("Setting default profiles in case none are selected yet");
        if (serverStatus == null) {
            Timber.d("Server status is null, can't set default profile selections");
            return;
        }

        if (serverStatus.getHtspPlaybackServerProfileId() == 0) {
            for (ServerProfile serverProfile : appRepository.getServerProfileData().getHtspPlaybackProfiles()) {
                if (TextUtils.equals(serverProfile.getName(), ("htsp"))) {
                    Timber.d("Setting htsp profile to htsp");
                    serverStatus.setHtspPlaybackServerProfileId(serverProfile.getId());
                    break;
                }
            }
        }
        if (serverStatus.getHttpPlaybackServerProfileId() == 0) {
            for (ServerProfile serverProfile : appRepository.getServerProfileData().getHttpPlaybackProfiles()) {
                if (TextUtils.equals(serverProfile.getName(), ("pass"))) {
                    Timber.d("Setting http profile to pass");
                    serverStatus.setHttpPlaybackServerProfileId(serverProfile.getId());
                    break;
                }
            }
        }
        if (serverStatus.getRecordingServerProfileId() == 0) {
            for (ServerProfile serverProfile : appRepository.getServerProfileData().getRecordingProfiles()) {
                if (TextUtils.equals(serverProfile.getName(), ("Default Profile"))) {
                    Timber.d("Setting recording profile to default");
                    serverStatus.setRecordingServerProfileId(serverProfile.getId());
                    break;
                }
            }
        }
        appRepository.getServerStatusData().updateItem(serverStatus);
    }

    /**
     * Server to client method.
     * A channel tag has been added on the server. Additionally to saving the new tag the
     * number of associated channels will be
     *
     * @param msg The message with the new tag data
     */
    private void onTagAdd(HtspMessage msg) {
        if (!initialSyncWithServerRunning) {
            return;
        }

        // During initial sync no channels are yet saved. So use the temporarily
        // stored channels to calculate the channel count for the channel tag
        ChannelTag addedTag = HtspUtils.convertMessageToChannelTagModel(new ChannelTag(), msg, pendingChannelOps);
        addedTag.setConnectionId(connection.getId());

        Timber.d("Sync is running, adding channel tag");
        pendingChannelTagOps.add(addedTag);
    }

    /**
     * Server to client method.
     * A tag has been updated on the server.
     *
     * @param msg The message with the updated tag data
     */
    private void onTagUpdate(HtspMessage msg) {
        if (!initialSyncWithServerRunning) {
            return;
        }

        ChannelTag channelTag = appRepository.getChannelTagData().getItemById(msg.getInteger("tagId"));
        if (channelTag == null) {
            Timber.d("Could not find a channel tag with id " + msg.getInteger("tagId") + " in the database");
            channelTag = new ChannelTag();
        }

        // During initial sync no channels are yet saved. So use the temporarily
        // stored channels to calculate the channel count for the channel tag
        ChannelTag updatedTag = HtspUtils.convertMessageToChannelTagModel(channelTag, msg, pendingChannelOps);
        updatedTag.setConnectionId(connection.getId());
        updatedTag.setSelected(channelTag.isSelected());

        Timber.d("Sync is running, updating channel tag");
        pendingChannelTagOps.add(updatedTag);

        if (syncRequired && pendingChannelTagOps.size() % 10 == 0) {
            sendSyncStateMessage(SyncStateReceiver.State.SYNC_IN_PROGRESS,
                    getString(R.string.receiving_data),
                    "Received " + pendingChannelTagOps.size() + " channel tags");
        }
    }

    /**
     * Server to client method.
     * A tag has been deleted on the server.
     *
     * @param msg The message with the tag id that was deleted
     */
    private void onTagDelete(HtspMessage msg) {
        if (msg.containsKey("tagId")) {
            ChannelTag tag = appRepository.getChannelTagData().getItemById(msg.getInteger("tagId"));
            if (tag != null) {
                deleteIconFileFromCache(tag.getTagIcon());
                appRepository.getChannelTagData().removeItem(tag);
                appRepository.getTagAndChannelData().removeItemByTagId(tag.getTagId());
            }
        }
    }

    /**
     * Server to client method.
     * A channel has been added on the server.
     *
     * @param msg The message with the new channel data
     */
    private void onChannelAdd(HtspMessage msg) {
        if (!initialSyncWithServerRunning) {
            return;
        }

        Channel channel = HtspUtils.convertMessageToChannelModel(new Channel(), msg);
        channel.setConnectionId(connection.getId());
        channel.setServerOrder(pendingChannelOps.size() + 1);

        Timber.d("Sync is running, adding channel " +
                "name '" + channel.getName() + "', " +
                "id " + channel.getId() + ", " +
                "number " + channel.getDisplayNumber() + ", " +
                "server order " + channel.getServerOrder());

        pendingChannelOps.add(channel);

        if (syncRequired && pendingChannelOps.size() % 25 == 0) {
            sendSyncStateMessage(SyncStateReceiver.State.SYNC_IN_PROGRESS,
                    getString(R.string.receiving_data),
                    "Received " + pendingChannelOps.size() + " channels");
        }
    }

    /**
     * Server to client method.
     * A channel has been updated on the server.
     *
     * @param msg The message with the updated channel data
     */
    private void onChannelUpdate(HtspMessage msg) {
        if (!initialSyncWithServerRunning) {
            return;
        }

        Channel channel = appRepository.getChannelData().getItemById(msg.getInteger("channelId"));
        if (channel == null) {
            Timber.d("Could not find a channel with id " + msg.getInteger("channelId") + " in the database");
            return;
        }
        Channel updatedChannel = HtspUtils.convertMessageToChannelModel(channel, msg);
        appRepository.getChannelData().updateItem(updatedChannel);
    }

    /**
     * Server to client method.
     * A channel has been deleted on the server.
     *
     * @param msg The message with the channel id that was deleted
     */
    private void onChannelDelete(HtspMessage msg) {
        if (msg.containsKey("channelId")) {
            int channelId = msg.getInteger("channelId");

            Channel channel = appRepository.getChannelData().getItemById(channelId);
            if (channel != null) {
                deleteIconFileFromCache(channel.getIcon());
                appRepository.getChannelData().removeItemById(channel.getId());
            }
        }
    }

    /**
     * Server to client method.
     * A recording has been added on the server.
     *
     * @param msg The message with the new recording data
     */
    private void onDvrEntryAdd(HtspMessage msg) {
        Recording recording = HtspUtils.convertMessageToRecordingModel(new Recording(), msg);
        recording.setConnectionId(connection.getId());

        if (initialSyncWithServerRunning) {
            pendingRecordingOps.add(recording);

            if (syncRequired && pendingRecordingOps.size() % 25 == 0) {
                Timber.d("Sync is running, received " + pendingRecordingOps.size() + " recordings");
                sendSyncStateMessage(SyncStateReceiver.State.SYNC_IN_PROGRESS,
                        getString(R.string.receiving_data),
                        "Received " + pendingRecordingOps.size() + " recordings");
            }
        } else {
            appRepository.getRecordingData().addItem(recording);
        }

        NotificationUtils.addNotification(appContext, recording);
    }

    /**
     * Server to client method.
     * A recording has been updated on the server.
     *
     * @param msg The message with the updated recording data
     */
    private void onDvrEntryUpdate(HtspMessage msg) {
        // Get the existing recording
        Recording recording = appRepository.getRecordingData().getItemById(msg.getInteger("id"));
        if (recording == null) {
            Timber.d("Could not find a recording with id " + msg.getInteger("id") + " in the database");
            return;
        }
        Recording updatedRecording = HtspUtils.convertMessageToRecordingModel(recording, msg);
        appRepository.getRecordingData().updateItem(updatedRecording);

        NotificationUtils.removeNotificationById(appContext, recording.getId());
        if (sharedPreferences.getBoolean("notifications_enabled", appContext.getResources().getBoolean(R.bool.pref_default_notifications_enabled))) {
            if (!recording.isScheduled() && !recording.isRecording()) {
                Timber.d("Removing notification for recording " + recording.getTitle());
                ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancel(recording.getId());
            }
        }
    }

    /**
     * Server to client method.
     * A recording has been deleted on the server.
     *
     * @param msg The message with the recording id that was deleted
     */
    private void onDvrEntryDelete(HtspMessage msg) {
        if (msg.containsKey("id")) {
            Recording recording = appRepository.getRecordingData().getItemById(msg.getInteger("id"));
            if (recording != null) {
                appRepository.getRecordingData().removeItem(recording);

            }
        }
    }

    /**
     * Server to client method.
     * A series recording has been added on the server.
     *
     * @param msg The message with the new series recording data
     */
    private void onAutorecEntryAdd(HtspMessage msg) {
        SeriesRecording seriesRecording = HtspUtils.convertMessageToSeriesRecordingModel(new SeriesRecording(), msg);
        seriesRecording.setConnectionId(connection.getId());
        appRepository.getSeriesRecordingData().addItem(seriesRecording);
    }

    /**
     * Server to client method.
     * A series recording has been updated on the server.
     *
     * @param msg The message with the updated series recording data
     */
    private void onAutorecEntryUpdate(HtspMessage msg) {
        String id = msg.getString("id", "");
        if (id.isEmpty()) {
            Timber.d("Could not find a series recording with id " + id + " in the database");
            return;
        }
        SeriesRecording recording = appRepository.getSeriesRecordingData().getItemById(msg.getString("id"));
        SeriesRecording updatedRecording = HtspUtils.convertMessageToSeriesRecordingModel(recording, msg);
        appRepository.getSeriesRecordingData().updateItem(updatedRecording);
    }

    /**
     * Server to client method.
     * A series recording has been deleted on the server.
     *
     * @param msg The message with the series recording id that was deleted
     */
    private void onAutorecEntryDelete(HtspMessage msg) {
        String id = msg.getString("id", "");
        if (!id.isEmpty()) {
            SeriesRecording seriesRecording = appRepository.getSeriesRecordingData().getItemById(msg.getString("id"));
            appRepository.getSeriesRecordingData().removeItem(seriesRecording);
        }
    }

    /**
     * Server to client method.
     * A timer recording has been added on the server.
     *
     * @param msg The message with the new timer recording data
     */
    private void onTimerRecEntryAdd(HtspMessage msg) {
        TimerRecording recording = HtspUtils.convertMessageToTimerRecordingModel(new TimerRecording(), msg);
        recording.setConnectionId(connection.getId());
        appRepository.getTimerRecordingData().addItem(recording);
    }

    /**
     * Server to client method.
     * A timer recording has been updated on the server.
     *
     * @param msg The message with the updated timer recording data
     */
    private void onTimerRecEntryUpdate(HtspMessage msg) {
        String id = msg.getString("id", "");
        if (id.isEmpty()) {
            Timber.d("Could not find a timer recording with id " + id + " in the database");
            return;
        }
        TimerRecording recording = appRepository.getTimerRecordingData().getItemById(id);
        TimerRecording updatedRecording = HtspUtils.convertMessageToTimerRecordingModel(recording, msg);
        appRepository.getTimerRecordingData().updateItem(updatedRecording);
    }

    /**
     * Server to client method.
     * A timer recording has been deleted on the server.
     *
     * @param msg The message with the recording id that was deleted
     */
    private void onTimerRecEntryDelete(HtspMessage msg) {
        String id = msg.getString("id", "");
        if (!id.isEmpty()) {
            TimerRecording timerRecording = appRepository.getTimerRecordingData().getItemById(id);
            appRepository.getTimerRecordingData().removeItem(timerRecording);
        }
    }

    /**
     * Server to client method.
     * An epg event has been added on the server.
     *
     * @param msg The message with the new epg event data
     */
    private void onEventAdd(HtspMessage msg) {
        if (!firstEventReceived && syncRequired) {
            Timber.d("Sync is required and received first event, saving " + pendingChannelOps.size() + " channels");
            appRepository.getChannelData().addItems(pendingChannelOps);

            Timber.d("Updating connection status with full sync completed");
            connection.setSyncRequired(false);
            appRepository.getConnectionData().updateItem(connection);
        }

        firstEventReceived = true;
        Program program = HtspUtils.convertMessageToProgramModel(new Program(), msg);
        program.setConnectionId(connection.getId());

        if (initialSyncWithServerRunning) {
            pendingEventOps.add(program);

            if (syncRequired && pendingEventOps.size() % 50 == 0) {
                Timber.d("Sync is running, received " + pendingEventOps.size() + " program guide events");
                sendSyncStateMessage(SyncStateReceiver.State.SYNC_IN_PROGRESS,
                        getString(R.string.receiving_data),
                        "Received " + pendingEventOps.size() + " program guide events");
            }
        } else {
            Timber.d("Adding event " + program.getTitle());
            appRepository.getProgramData().addItem(program);
        }
    }

    /**
     * Server to client method.
     * An epg event has been updated on the server.
     *
     * @param msg The message with the updated epg event data
     */
    private void onEventUpdate(HtspMessage msg) {
        Program program = appRepository.getProgramData().getItemById(msg.getInteger("eventId"));
        if (program == null) {
            Timber.d("Could not find a program with id " + msg.getInteger("eventId") + " in the database");
            return;
        }
        Program updatedProgram = HtspUtils.convertMessageToProgramModel(program, msg);
        Timber.d("Updating event " + updatedProgram.getTitle());
        appRepository.getProgramData().updateItem(updatedProgram);
    }

    /**
     * Server to client method.
     * An epg event has been deleted on the server.
     *
     * @param msg The message with the epg event id that was deleted
     */
    private void onEventDelete(HtspMessage msg) {
        if (msg.containsKey("id")) {
            appRepository.getProgramData().removeItemById(msg.getInteger("id"));
        }
    }

    /**
     * Handles the given server message that contains a list of events.
     *
     * @param message The message with the events
     */
    private void onGetEvents(HtspMessage message, Intent intent) {

        final boolean useEventList = intent.getBooleanExtra("useEventList", false);
        final String channelName = intent.getStringExtra("channelName");

        if (message.containsKey("events")) {
            List<Program> programs = new ArrayList<>();
            for (Object obj : message.getList("events")) {
                HtspMessage msg = (HtspMessage) obj;
                Program program = HtspUtils.convertMessageToProgramModel(new Program(), msg);
                program.setConnectionId(connection.getId());
                programs.add(program);
            }

            if (useEventList) {
                Timber.d("Adding " + programs.size() + " events to the list for channel " + channelName);
                pendingEventOps.addAll(programs);
            } else {
                Timber.d("Saving " + programs.size() + " events for channel " + channelName);
                appRepository.getProgramData().addItems(programs);
            }
        }
    }

    private void onHtspProfiles(HtspMessage message) {
        Timber.d("Handling htsp playback profiles");
        if (message.containsKey("profiles")) {
            for (Object obj : message.getList("profiles")) {
                HtspMessage msg = (HtspMessage) obj;
                String name = msg.getString("name");

                String[] profileNames = appRepository.getServerProfileData().getHtspPlaybackProfileNames();
                boolean profileExists = false;
                for (String profileName : profileNames) {
                    if (profileName.equals(name)) {
                        profileExists = true;
                        break;
                    }
                }
                if (!profileExists) {
                    ServerProfile serverProfile = new ServerProfile();
                    serverProfile.setConnectionId(connection.getId());
                    serverProfile.setName(name);
                    serverProfile.setUuid(msg.getString("uuid"));
                    serverProfile.setComment(msg.getString("comment"));
                    serverProfile.setType("htsp_playback");

                    Timber.d("Adding htsp playback profile " + serverProfile.getName());
                    appRepository.getServerProfileData().addItem(serverProfile);
                }
            }
        }
    }

    private void onHttpProfiles(HtspMessage message) {
        Timber.d("Handling http playback profiles");
        if (message.containsKey("response")) {
            try {
                JSONObject response = new JSONObject(message.getString("response"));
                if (response.has("entries")) {
                    JSONArray entries = response.getJSONArray("entries");
                    if (entries.length() > 0) {
                        for (int i = 0, totalObject = entries.length(); i < totalObject; i++) {
                            JSONObject profile = entries.getJSONObject(i);
                            if (profile.has("key") && profile.has("val")) {
                                String name = profile.getString("val");

                                String[] profileNames = appRepository.getServerProfileData().getHttpPlaybackProfileNames();
                                boolean profileExists = false;
                                for (String profileName : profileNames) {
                                    if (profileName.equals(name)) {
                                        profileExists = true;
                                        break;
                                    }
                                }
                                if (!profileExists) {
                                    ServerProfile serverProfile = new ServerProfile();
                                    serverProfile.setConnectionId(connection.getId());
                                    serverProfile.setName(name);
                                    serverProfile.setUuid(profile.getString("key"));
                                    serverProfile.setType("http_playback");

                                    Timber.d("Adding http playback profile " + serverProfile.getName());
                                    appRepository.getServerProfileData().addItem(serverProfile);
                                }
                            }
                        }
                    }
                }
            } catch (JSONException e) {
                Timber.d("Error parsing JSON data", e);
            }
        }
    }

    private void onDvrConfigs(HtspMessage message) {
        Timber.d("Handling recording profiles");
        if (message.containsKey("dvrconfigs")) {
            for (Object obj : message.getList("dvrconfigs")) {
                HtspMessage msg = (HtspMessage) obj;
                ServerProfile serverProfile = appRepository.getServerProfileData().getItemById(msg.getString("uuid"));
                if (serverProfile == null) {
                    serverProfile = new ServerProfile();
                }

                serverProfile.setConnectionId(connection.getId());
                serverProfile.setUuid(msg.getString("uuid"));
                String name = msg.getString("name");
                serverProfile.setName(TextUtils.isEmpty(name) ? "Default Profile" : name);
                serverProfile.setComment(msg.getString("comment"));
                serverProfile.setType("recording");

                if (serverProfile.getId() == 0) {
                    Timber.d("Added new recording profile " + serverProfile.getName());
                    appRepository.getServerProfileData().addItem(serverProfile);
                } else {
                    Timber.d("Updated existing recording profile " + serverProfile.getName());
                    appRepository.getServerProfileData().updateItem(serverProfile);
                }
            }
        }
    }

    private void onServerStatus(HtspMessage message) {
        if (serverStatus == null) {
            Timber.d("Server status is null, can't update server status");
            return;
        }
        ServerStatus updatedServerStatus = HtspUtils.convertMessageToServerStatusModel(serverStatus, message);
        updatedServerStatus.setConnectionId(connection.getId());
        updatedServerStatus.setConnectionName(connection.getName());
        Timber.d("Received initial response from server " + updatedServerStatus.getServerName() + ", api version: " + updatedServerStatus.getHtspVersion());

        appRepository.getServerStatusData().updateItem(updatedServerStatus);
    }

    private void onSystemTime(HtspMessage message) {
        if (serverStatus == null) {
            Timber.d("Server status is null, can't update system time");
            return;
        }

        int gmtOffsetFromServer = message.getInteger("gmtoffset", 0) * 60 * 1000;
        int gmtOffset = gmtOffsetFromServer - HtspUtils.getDaylightSavingOffset();
        Timber.d("GMT offset from server is " + gmtOffsetFromServer +
                ", GMT offset considering daylight saving offset is " + gmtOffset);

        serverStatus.setGmtoffset(gmtOffset);
        serverStatus.setTime(message.getLong("time", 0));
        appRepository.getServerStatusData().updateItem(serverStatus);

        Timber.d("Received system time from server " + serverStatus.getServerName()
                + ", server time: " + serverStatus.getTime()
                + ", server gmt offset: " + serverStatus.getGmtoffset());
    }

    private void onDiskSpace(HtspMessage message) {
        if (serverStatus == null) {
            Timber.d("Server status is null, can't update disc space");
            return;
        }

        serverStatus.setFreeDiskSpace(message.getLong("freediskspace", 0));
        serverStatus.setTotalDiskSpace(message.getLong("totaldiskspace", 0));
        appRepository.getServerStatusData().updateItem(serverStatus);

        Timber.d("Received disk space information from server " + serverStatus.getServerName()
                + ", free disk space: " + serverStatus.getFreeDiskSpace()
                + ", total disk space: " + serverStatus.getTotalDiskSpace());
    }

    /**
     * Saves all received channels from the initial sync in the database.
     */
    private void saveAllReceivedChannels() {
        Timber.d("Saving " + pendingChannelOps.size() + " channels");

        if (!pendingChannelOps.isEmpty()) {
            appRepository.getChannelData().addItems(pendingChannelOps);
        }
    }

    /**
     * Saves all received channel tags from the initial sync in the database.
     * Also the relations table between channels and tags are
     * updated so that the filtering by channel tags works properly
     */
    private void saveAllReceivedChannelTags() {
        Timber.d("Saving " + pendingChannelTagOps.size() + " channel tags");

        List<TagAndChannel> pendingRemovedTagAndChannelOps = new ArrayList<>();
        List<TagAndChannel> pendingAddedTagAndChannelOps = new ArrayList<>();

        if (!pendingChannelTagOps.isEmpty()) {
            appRepository.getChannelTagData().addItems(pendingChannelTagOps);
            for (ChannelTag tag : pendingChannelTagOps) {

                if (tag != null) {
                    TagAndChannel tac = appRepository.getTagAndChannelData().getItemById(tag.getTagId());
                    if (tac != null) {
                        pendingRemovedTagAndChannelOps.add(tac);
                    }

                    List<Integer> channelIds = tag.getMembers();
                    if (channelIds != null) {
                        for (Integer channelId : channelIds) {
                            TagAndChannel tagAndChannel = new TagAndChannel();
                            tagAndChannel.setTagId(tag.getTagId());
                            tagAndChannel.setChannelId(channelId);
                            tagAndChannel.setConnectionId(connection.getId());
                            pendingAddedTagAndChannelOps.add(tagAndChannel);
                        }
                    }
                }
            }

            Timber.d("Removing " + pendingRemovedTagAndChannelOps.size() +
                    " and adding " + pendingAddedTagAndChannelOps.size() + " tag and channel relations");
            appRepository.getTagAndChannelData().addAndRemoveItems(pendingAddedTagAndChannelOps, pendingRemovedTagAndChannelOps);
        }
    }

    /**
     * Removes all recordings and saves all received recordings from the initial sync
     * in the database. The removal is done to prevent being out of sync with the server.
     * This could be the case when the app was offline for a while and it did not receive
     * any recording removal information from the server. During the initial sync the
     * server only provides the list of available recordings.
     */
    private void saveAllReceivedRecordings() {
        Timber.d("Removing previously existing recordings and saving " + pendingRecordingOps.size() + " new recordings");

        appRepository.getRecordingData().removeItems();
        if (!pendingRecordingOps.isEmpty()) {
            appRepository.getRecordingData().addItems(pendingRecordingOps);
        }
    }

    private void saveAllReceivedEvents() {
        Timber.d("Saving " + pendingEventOps.size() + " new events");

        if (!pendingEventOps.isEmpty()) {
            appRepository.getProgramData().addItems(pendingEventOps);
        }
    }

    /**
     * Tries to download and save all received channel and channel
     * tag logos from the initial sync in the database.
     */
    private void loadAllChannelIcons() {
        Timber.d("Downloading and saving all channel and channel tag icons...");

        for (Channel channel : appRepository.getChannelData().getItems()) {
            execService.execute(() -> {
                try {
                    Timber.d("Downloading channel icon for channel " + channel.getName());
                    downloadIconFromFileUrl(channel.getIcon());
                } catch (Exception e) {
                    Timber.d("Could not load channel icon for channel '" + channel.getIcon() + "'");
                }
            });
        }
        for (ChannelTag channelTag : appRepository.getChannelTagData().getItems()) {
            execService.execute(() -> {
                try {
                    Timber.d("Downloading channel icon for channel tag " + channelTag.getTagName());
                    downloadIconFromFileUrl(channelTag.getTagIcon());
                } catch (Exception e) {
                    Timber.d("Could not load channel tag icon '" + channelTag.getTagIcon() + "'");
                }
            });
        }
    }

    /**
     * Downloads the file from the given url. If the url starts with http then a
     * buffered input stream is used, otherwise the htsp api is used. The file
     * will be saved in the cache directory using a unique hash value as the file name.
     *
     * @param url The url of the file that shall be downloaded
     * @throws IOException Error message if something went wrong
     */
    // Use the icon loading from the original library?
    private void downloadIconFromFileUrl(final String url) throws IOException {
        if (TextUtils.isEmpty(url)) {
            return;
        }

        File file = new File(getCacheDir(), MiscUtils.convertUrlToHashString(url) + ".png");
        if (file.exists()) {
            Timber.d("Icon file " + file.getAbsolutePath() + " exists already");
            return;
        }

        InputStream is;
        if (url.startsWith("http")) {
            is = new BufferedInputStream(new URL(url).openStream());
        } else if (htspVersion > 9) {
            is = new HtspFileInputStream(htspConnection, url);
        } else {
            return;
        }

        OutputStream os = new FileOutputStream(file);

        // Set the options for a bitmap and decode an input stream into a bitmap
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(is, null, o);
        is.close();

        if (url.startsWith("http")) {
            is = new BufferedInputStream(new URL(url).openStream());
        } else if (htspVersion > 9) {
            is = new HtspFileInputStream(htspConnection, url);
        }

        float scale = appContext.getResources().getDisplayMetrics().density;
        int width = (int) (64 * scale);
        int height = (int) (64 * scale);

        // Set the sample size of the image. This is the number of pixels in
        // either dimension that correspond to a single pixel in the decoded
        // bitmap. For example, inSampleSize == 4 returns an image that is 1/4
        // the width/height of the original, and 1/16 the number of pixels.
        int ratio = Math.max(o.outWidth / width, o.outHeight / height);
        int sampleSize = Integer.highestOneBit((int) Math.floor(ratio));
        o = new BitmapFactory.Options();
        o.inSampleSize = sampleSize;

        // Now decode an input stream into a bitmap and compress it.
        Bitmap bitmap = BitmapFactory.decodeStream(is, null, o);
        if (bitmap != null) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
        }

        os.close();
        is.close();
    }

    /**
     * Removes the cached image file from the file system
     *
     * @param iconUrl The icon url
     */
    private void deleteIconFileFromCache(@Nullable String iconUrl) {
        if (TextUtils.isEmpty(iconUrl)) {
            return;
        }
        String url = MiscUtils.getIconUrl(appContext, iconUrl);
        File file = new File(url);
        if (!file.exists() || !file.delete()) {
            Timber.d("Could not delete icon " + file.getName());
        }
    }

    private void getChannel(final Intent intent) {
        final HtspMessage request = new HtspMessage();
        request.put("method", "getChannel");
        request.put("channelId", intent.getIntExtra("channelId", 0));

        htspConnection.sendMessage(request, response -> {
            if (response != null) {
                // Update the icon if required
                final String icon = response.getString("channelIcon", null);
                if (icon != null) {
                    try {
                        downloadIconFromFileUrl(icon);
                    } catch (Exception e) {
                        Timber.d("Could not load icon '" + icon + "'");
                    }
                }
            } else {
                Timber.d("Response is null");
            }
        });
    }

    private void getEvent(Intent intent) {
        final HtspMessage request = new HtspMessage();
        request.put("method", "getEvent");
        request.put("eventId", intent.getIntExtra("eventId", 0));

        htspConnection.sendMessage(request, response -> {
            if (response != null) {
                Program program = HtspUtils.convertMessageToProgramModel(new Program(), response);
                appRepository.getProgramData().addItem(program);
            } else {
                Timber.d("Response is null");
            }
        });
    }

    /**
     * Request information about a set of events from the server.
     * If no options are specified the entire EPG database will be returned.
     *
     * @param intent Intent with the request message fields
     */
    private void getEvents(Intent intent) {
        final boolean showMessage = intent.getBooleanExtra("showMessage", false);
        HtspMessage request = HtspUtils.convertIntentToEventMessage(intent);
        htspConnection.sendMessage(request, response -> {
            if (response != null) {
                onGetEvents(response, intent);
                if (showMessage) {
                    Timber.d("Showing message");
                    SnackbarUtils.sendSnackbarMessage(this, getString(R.string.loading_more_programs_finished));
                }
            } else {
                Timber.d("Response is null");
            }
        });
    }

    /**
     * Loads a defined number of events for all channels.
     * This method is called by a worker after the initial sync is done.
     * All loaded events are saved in a temporary list and saved in one
     * batch into the database when all events were loaded for all channels.
     *
     * @param intent The intent with the parameters e.g. to define how many events shall be loaded
     */
    private void getMoreEvents(Intent intent) {

        int numberOfProgramsToLoad = intent.getIntExtra("numFollowing", 0);
        List<Channel> channelList = appRepository.getChannelData().getItems();

        Timber.d("Database currently contains " + appRepository.getProgramData().getItemCount() + " events. ");
        Timber.d("Loading " + numberOfProgramsToLoad + " events for each of the " + channelList.size() + " channels");

        for (Channel channel : channelList) {
            Program lastProgram = appRepository.getProgramData().getLastItemByChannelId(channel.getId());

            Intent msgIntent = new Intent();
            msgIntent.putExtra("numFollowing", numberOfProgramsToLoad);
            msgIntent.putExtra("useEventList", true);
            msgIntent.putExtra("channelId", channel.getId());
            msgIntent.putExtra("channelName", channel.getName());

            if (lastProgram != null) {
                Timber.d("Loading more programs for channel " + channel.getName() +
                        " from last program id " + lastProgram.getEventId());
                msgIntent.putExtra("eventId", lastProgram.getNextEventId());
            } else if (channel.getNextEventId() > 0) {
                Timber.d("Loading more programs for channel " + channel.getName() +
                        " starting from channel next event id " + channel.getNextEventId());
                msgIntent.putExtra("eventId", channel.getNextEventId());
            } else {
                Timber.d("Loading more programs for channel " + channel.getName() +
                        " starting from channel event id " + channel.getEventId());
                msgIntent.putExtra("eventId", channel.getEventId());
            }
            getEvents(msgIntent);
        }

        appRepository.getProgramData().addItems(pendingEventOps);
        Timber.d("Saved " + pendingEventOps.size() + " events for all channels. " +
                "Database contains " + appRepository.getProgramData().getItemCount() + " events");
        pendingEventOps.clear();
    }

    private void getEpgQuery(final Intent intent) {
        HtspMessage request = HtspUtils.convertIntentToEpgQueryMessage(intent);
        htspConnection.sendMessage(request, response -> {
            if (response != null) {
                // Contains the ids of those events that were returned by the query
                //noinspection MismatchedQueryAndUpdateOfCollection
                List<Integer> eventIdList = new ArrayList<>();
                if (response.containsKey("events")) {
                    // List of events that match the query. Add the eventIds
                    for (Object obj : response.getList("events")) {
                        HtspMessage msg = (HtspMessage) obj;
                        eventIdList.add(msg.getInteger("eventId"));
                    }
                } else if (response.containsKey("eventIds")) {
                    // List of eventIds that match the query
                    for (Object obj : response.getArrayList("eventIds")) {
                        eventIdList.add((int) obj);
                    }
                }
            } else {
                Timber.d("Response is null");
            }
        });
    }

    private void addDvrEntry(final Intent intent) {
        HtspMessage request = HtspUtils.convertIntentToDvrMessage(intent, htspVersion);
        request.put("method", "addDvrEntry");

        htspConnection.sendMessage(request, response -> {
            if (response != null) {
                // Reply message fields:
                // success            u32   required   1 if entry was added, 0 otherwise
                // id                 u32   optional   ID of created DVR entry
                // error              str   optional   English clear text of error message
                if (response.getInteger("success", 0) == 1) {
                    SnackbarUtils.sendSnackbarMessage(this, getString(R.string.success_adding_recording));
                } else {
                    SnackbarUtils.sendSnackbarMessage(this, getString(R.string.error_adding_recording, response.getString("error", "")));
                }
            } else {
                Timber.d("Response is null");
            }
        });

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.cancel(intent.getIntExtra("eventId", 0));
    }

    private void updateDvrEntry(final Intent intent) {
        HtspMessage request = HtspUtils.convertIntentToDvrMessage(intent, htspVersion);
        request.put("method", "updateDvrEntry");
        request.put("id", intent.getIntExtra("id", 0));

        htspConnection.sendMessage(request, response -> {
            if (response != null) {
                // Reply message fields:
                // success            u32   required   1 if update as successful, otherwise 0
                // error              str   optional   Error message if update failed
                if (response.getInteger("success", 0) == 1) {
                    SnackbarUtils.sendSnackbarMessage(this, getString(R.string.success_updating_recording));
                } else {
                    SnackbarUtils.sendSnackbarMessage(this, getString(R.string.error_updating_recording, response.getString("error", "")));
                }
            } else {
                Timber.d("Response is null");
            }
        });
    }

    private void removeDvrEntry(Intent intent) {
        HtspMessage request = new HtspMessage();
        request.put("method", intent.getAction());
        request.put("id", intent.getIntExtra("id", 0));

        htspConnection.sendMessage(request, response -> {
            if (response != null) {
                Timber.d("Response is not null");
                if (response.getInteger("success", 0) == 1) {
                    SnackbarUtils.sendSnackbarMessage(this, getString(R.string.success_removing_recording));
                } else {
                    SnackbarUtils.sendSnackbarMessage(this, getString(R.string.error_removing_recording, response.getString("error", "")));
                }
            } else {
                Timber.d("Response is null");
            }
        });

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.cancel(intent.getIntExtra("id", 0));
    }

    private void addAutorecEntry(final Intent intent) {
        HtspMessage request = HtspUtils.convertIntentToAutorecMessage(intent, htspVersion);
        request.put("method", "addAutorecEntry");

        htspConnection.sendMessage(request, response -> {
            if (response != null) {
                // Reply message fields:
                // success            u32   required   1 if entry was added, 0 otherwise
                // id                 str   optional   ID (string!) of created autorec DVR entry
                // error              str   optional   English clear text of error message
                if (response.getInteger("success", 0) == 1) {
                    SnackbarUtils.sendSnackbarMessage(this, getString(R.string.success_adding_recording));
                } else {
                    SnackbarUtils.sendSnackbarMessage(this, getString(R.string.error_adding_recording, response.getString("error", "")));
                }
            } else {
                Timber.d("Response is null");
            }
        });
    }

    private void updateAutorecEntry(final Intent intent) {
        HtspMessage request = new HtspMessage();
        if (htspVersion >= 25) {
            request = HtspUtils.convertIntentToAutorecMessage(intent, htspVersion);
            request.put("method", "updateAutorecEntry");
        } else {
            request.put("method", "deleteAutorecEntry");
        }
        request.put("id", intent.getStringExtra("id"));

        htspConnection.sendMessage(request, response -> {
            if (response != null) {
                // Handle the response here because the "updateAutorecEntry" call does
                // not exist on the server. First delete the entry and if this was
                // successful add a new entry with the new values.
                final boolean success = (response.getInteger("success", 0) == 1);
                if (htspVersion < 25 && success) {
                    addAutorecEntry(intent);
                } else {
                    if (success) {
                        SnackbarUtils.sendSnackbarMessage(this, getString(R.string.success_updating_recording));
                    } else {
                        SnackbarUtils.sendSnackbarMessage(this, getString(R.string.error_updating_recording, response.getString("error", "")));
                    }
                }
            } else {
                Timber.d("Response is null");
            }
        });
    }

    private void deleteAutorecEntry(final Intent intent) {
        final HtspMessage request = new HtspMessage();
        request.put("method", "deleteAutorecEntry");
        request.put("id", intent.getStringExtra("id"));

        htspConnection.sendMessage(request, response -> {
            if (response != null) {
                if (response.getInteger("success", 0) == 1) {
                    SnackbarUtils.sendSnackbarMessage(this, getString(R.string.success_removing_recording));
                } else {
                    SnackbarUtils.sendSnackbarMessage(this, getString(R.string.error_removing_recording, response.getString("error", "")));
                }
            } else {
                Timber.d("Response is null");
            }
        });
    }

    private void addTimerrecEntry(final Intent intent) {
        HtspMessage request = HtspUtils.convertIntentToTimerecMessage(intent, htspVersion);
        request.put("method", "addTimerecEntry");

        // Reply message fields:
        // success            u32   required   1 if entry was added, 0 otherwise
        // id                 str   optional   ID (string!) of created timerec DVR entry
        // error              str   optional   English clear text of error message
        htspConnection.sendMessage(request, response -> {
            if (response != null) {
                if (response.getInteger("success", 0) == 1) {
                    SnackbarUtils.sendSnackbarMessage(this, getString(R.string.success_adding_recording));
                } else {
                    SnackbarUtils.sendSnackbarMessage(this, getString(R.string.error_adding_recording, response.getString("error", "")));
                }
            } else {
                Timber.d("Response is null");
            }
        });
    }

    private void updateTimerrecEntry(final Intent intent) {
        HtspMessage request = new HtspMessage();
        if (htspVersion >= 25) {
            request = HtspUtils.convertIntentToTimerecMessage(intent, htspVersion);
            request.put("method", "updateTimerecEntry");
        } else {
            request.put("method", "deleteTimerecEntry");
        }
        request.put("id", intent.getStringExtra("id"));

        htspConnection.sendMessage(request, response -> {
            if (response != null) {
                // Handle the response here because the "updateTimerecEntry" call does
                // not exist on the server. First delete the entry and if this was
                // successful add a new entry with the new values.
                final boolean success = response.getInteger("success", 0) == 1;
                if (htspVersion < 25 && success) {
                    addTimerrecEntry(intent);
                } else {
                    if (success) {
                        SnackbarUtils.sendSnackbarMessage(this, getString(R.string.success_updating_recording));
                    } else {
                        SnackbarUtils.sendSnackbarMessage(this, getString(R.string.error_updating_recording, response.getString("error", "")));
                    }
                }
            } else {
                Timber.d("Response is null");
            }
        });
    }

    private void deleteTimerrecEntry(Intent intent) {
        HtspMessage request = new HtspMessage();
        request.put("method", "deleteTimerecEntry");
        request.put("id", intent.getStringExtra("id"));

        htspConnection.sendMessage(request, response -> {
            if (response != null) {
                if (response.getInteger("success", 0) == 1) {
                    SnackbarUtils.sendSnackbarMessage(this, getString(R.string.success_removing_recording));
                } else {
                    SnackbarUtils.sendSnackbarMessage(this, getString(R.string.error_removing_recording, response.getString("error", "")));
                }
            } else {
                Timber.d("Response is null");
            }
        });
    }

    private void getTicket(Intent intent) {
        final long channelId = intent.getIntExtra("channelId", 0);
        final long dvrId = intent.getIntExtra("dvrId", 0);

        final HtspMessage request = new HtspMessage();
        request.put("method", "getTicket");
        if (channelId > 0) {
            request.put("channelId", channelId);
        }
        if (dvrId > 0) {
            request.put("dvrId", dvrId);
        }
        // Reply message fields:
        // path               str  required   The full path for access URL (no scheme, host or port)
        // ticket             str  required   The ticket to pass in the URL query string
        htspConnection.sendMessage(request, response -> {
            if (response != null) {
                Timber.d("Response is not null");
                Intent ticketIntent = new Intent("ticket");
                ticketIntent.putExtra("path", response.getString("path"));
                ticketIntent.putExtra("ticket", response.getString("ticket"));
                LocalBroadcastManager.getInstance(this).sendBroadcast(ticketIntent);
            } else {
                Timber.d("Response is null");
            }
        });
    }

    private void sendSyncStateMessage(SyncStateReceiver.State state, String message, String details) {
        Intent intent = new Intent(SyncStateReceiver.ACTION);
        intent.putExtra(SyncStateReceiver.STATE, state);
        if (!TextUtils.isEmpty(message)) {
            intent.putExtra(SyncStateReceiver.MESSAGE, message);
        }
        if (!TextUtils.isEmpty(details)) {
            intent.putExtra(SyncStateReceiver.DETAILS, details);
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}
