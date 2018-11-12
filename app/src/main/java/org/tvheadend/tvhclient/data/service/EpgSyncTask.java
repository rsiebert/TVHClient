package org.tvheadend.tvhclient.data.service;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.tvheadend.tvhclient.BuildConfig;
import org.tvheadend.tvhclient.MainApplication;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.entity.Channel;
import org.tvheadend.tvhclient.data.entity.ChannelTag;
import org.tvheadend.tvhclient.data.entity.Connection;
import org.tvheadend.tvhclient.data.entity.Program;
import org.tvheadend.tvhclient.data.entity.Recording;
import org.tvheadend.tvhclient.data.entity.SeriesRecording;
import org.tvheadend.tvhclient.data.entity.ServerProfile;
import org.tvheadend.tvhclient.data.entity.ServerStatus;
import org.tvheadend.tvhclient.data.entity.TagAndChannel;
import org.tvheadend.tvhclient.data.entity.TimerRecording;
import org.tvheadend.tvhclient.data.repository.AppRepository;
import org.tvheadend.tvhclient.data.service.htsp.HtspConnection;
import org.tvheadend.tvhclient.data.service.htsp.HtspFileInputStream;
import org.tvheadend.tvhclient.data.service.htsp.HtspMessage;
import org.tvheadend.tvhclient.data.service.htsp.HtspNotConnectedException;
import org.tvheadend.tvhclient.data.service.htsp.tasks.Authenticator;
import org.tvheadend.tvhclient.features.shared.receivers.ServiceStatusReceiver;
import org.tvheadend.tvhclient.features.shared.receivers.SnackbarMessageReceiver;
import org.tvheadend.tvhclient.utils.MiscUtils;
import org.tvheadend.tvhclient.utils.NotificationUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.inject.Inject;

import timber.log.Timber;

public class EpgSyncTask implements HtspMessage.Listener, Authenticator.Listener, HtspConnection.Listener {

    @Inject
    protected AppRepository appRepository;
    @Inject
    protected SharedPreferences sharedPreferences;
    @Inject
    protected Context context;
    private final int connectionTimeout;
    private final Connection connection;
    private int htspVersion;
    // This variable controls if the received data shall be stored
    // in lists so that it can be added at once to the database.
    private boolean initialSyncCompleted;

    private final HtspMessage.Dispatcher dispatcher;
    private final Handler handler;

    private final ArrayList<Channel> pendingChannelOps = new ArrayList<>();
    private final ArrayList<Recording> pendingRecordingOps = new ArrayList<>();
    private final ArrayList<Program> pendingEventOps = new ArrayList<>();
    private final Queue<String> pendingChannelLogoFetches = new ConcurrentLinkedQueue<>();

    EpgSyncTask(@NonNull HtspMessage.Dispatcher dispatcher, Connection connection) {
        MainApplication.getComponent().inject(this);
        HandlerThread handlerThread = new HandlerThread("EpgSyncTask Handler Thread");
        handlerThread.start();

        this.dispatcher = dispatcher;
        this.handler = new Handler(handlerThread.getLooper());
        this.connectionTimeout = Integer.valueOf(sharedPreferences.getString("connectionTimeout", "5")) * 1000;
        this.htspVersion = 13;
        this.connection = connection;
    }

    @Override
    public void onConnectionStateChange(@NonNull HtspConnection.State state) {
        Timber.d("Simple HTSP connection state changed, state is " + state);

        switch (state) {
            case FAILED:
                sendEpgSyncStatusMessage(ServiceStatusReceiver.State.FAILED,
                        context.getString(R.string.connection_failed),
                        null);
                break;
            case FAILED_CONNECTING_TO_SERVER:
                sendEpgSyncStatusMessage(ServiceStatusReceiver.State.FAILED,
                        context.getString(R.string.connection_failed),
                        context.getString(R.string.failed_connecting_to_server));
                break;
            case FAILED_EXCEPTION_OPENING_SOCKET:
                sendEpgSyncStatusMessage(ServiceStatusReceiver.State.FAILED,
                        context.getString(R.string.connection_failed),
                        context.getString(R.string.failed_opening_socket));
                break;
            case FAILED_INTERRUPTED:
                sendEpgSyncStatusMessage(ServiceStatusReceiver.State.FAILED,
                        context.getString(R.string.connection_failed),
                        context.getString(R.string.failed_during_connection_attempt));
                break;
            case FAILED_UNRESOLVED_ADDRESS:
                sendEpgSyncStatusMessage(ServiceStatusReceiver.State.FAILED,
                        context.getString(R.string.connection_failed),
                        context.getString(R.string.failed_to_resolve_address));
                break;
            case CONNECTING:
                sendEpgSyncStatusMessage(ServiceStatusReceiver.State.CONNECTING,
                        context.getString(R.string.connecting_to_server), "");
                break;
            case CLOSED:
                sendEpgSyncStatusMessage(ServiceStatusReceiver.State.CLOSED,
                        context.getString(R.string.connection_closed), "");
                break;
        }
    }

    // Authenticator.Listener Methods
    @Override
    public void onAuthenticationStateChange(@NonNull Authenticator.State state) {
        Timber.d("Authentication state changed to " + state);

        switch (state) {
            case FAILED:
                sendEpgSyncStatusMessage(ServiceStatusReceiver.State.FAILED,
                        context.getString(R.string.authentication_failed), "");
                break;
            case FAILED_BAD_CREDENTIALS:
                sendEpgSyncStatusMessage(ServiceStatusReceiver.State.FAILED,
                        context.getString(R.string.authentication_failed),
                        context.getString(R.string.bad_username_or_password));
                break;
            case AUTHENTICATED:
                sendEpgSyncStatusMessage(ServiceStatusReceiver.State.CONNECTED,
                        context.getString(R.string.connected_to_server), "");
                // Continue with getting all initial data only if we are authenticated
                startAsyncCommunicationWithServer();
                break;
        }
    }

    private void startAsyncCommunicationWithServer() {
        Timber.d("Starting async communication with server");

        initialSyncCompleted = false;

        // Send the first sync message to any broadcast listeners
        sendEpgSyncStatusMessage(ServiceStatusReceiver.State.SYNC_STARTED,
                context.getString(R.string.loading_data), "");

        // Enable epg sync with the defined number of
        // seconds of data starting from the current time
        HtspMessage enableAsyncMetadataRequest = new HtspMessage();
        enableAsyncMetadataRequest.put("method", "enableAsyncMetadata");

        boolean lastUpdateEnabled = sharedPreferences.getBoolean("epg_last_update_enabled", false);
        long epgMaxTime = Long.parseLong(sharedPreferences.getString("epg_max_time", context.getResources().getString(R.string.pref_default_epg_max_time)));
        long currentTimeInSeconds = (System.currentTimeMillis() / 1000L);

        Timber.d("Connection last update: " + connection.getLastUpdate());
        Timber.d("epgMaxTime: " + epgMaxTime);
        Timber.d("currentTimeInSeconds: " + currentTimeInSeconds);
        Timber.d("Connection sync required: " + connection.isSyncRequired());

        // Only fetch new epg data if the last update time plus the epg fetch
        // time is older than the current time or if a sync is required.
        if ((connection.getLastUpdate() + epgMaxTime) < currentTimeInSeconds
                || connection.isSyncRequired()) {
            Timber.d("Requesting epg data during initial sync");

            // Load only the defined time of epg data to reduce the received data during sync
            enableAsyncMetadataRequest.put("epg", 1);
            if (connection.isSyncRequired()) {
                Timber.d("Initial sync is required, loading maximum time of programs");
            } else {
                Timber.d("Initial sync is not required, loading " + epgMaxTime + " seconds of programs");
                enableAsyncMetadataRequest.put("epgMaxTime", epgMaxTime + (System.currentTimeMillis() / 1000L));
            }

            if (lastUpdateEnabled) {
                Timber.d("Setting last update field to " + connection.getLastUpdate());
                enableAsyncMetadataRequest.put("lastUpdate", connection.getLastUpdate());
            } else {
                Timber.d("Skipping lastUpdate field, disabled by preference");
            }
        } else {
            Timber.d("Not requesting epg data during initial sync");
        }

        try {
            dispatcher.sendMessage(enableAsyncMetadataRequest);
        } catch (HtspNotConnectedException e) {
            Timber.d("Failed to enable async metadata, HTSP not connected", e);
        }
    }

    // HtspMessage.Listener Methods
    @Override
    public Handler getHandler() {
        return handler;
    }

    @Override
    public void setConnection(@NonNull HtspConnection connection) {

    }

    @Override
    public void onMessage(@NonNull HtspMessage message) {
        final String method = message.getString("method");
        if (TextUtils.isEmpty(method)) {
            return;
        }
        switch (method) {
            case "hello":
                handleInitialServerResponse(message);
                break;
            case "tagAdd":
                handleTagAdd(message);
                break;
            case "tagUpdate":
                handleTagUpdate(message);
                break;
            case "tagDelete":
                handleTagDelete(message);
                break;
            case "channelAdd":
                handleChannelAdd(message);
                break;
            case "channelUpdate":
                handleChannelUpdate(message);
                break;
            case "channelDelete":
                handleChannelDelete(message);
                break;
            case "dvrEntryAdd":
                handleDvrEntryAdd(message);
                break;
            case "dvrEntryUpdate":
                handleDvrEntryUpdate(message);
                break;
            case "dvrEntryDelete":
                handleDvrEntryDelete(message);
                break;
            case "timerecEntryAdd":
                handleTimerRecEntryAdd(message);
                break;
            case "timerecEntryUpdate":
                handleTimerRecEntryUpdate(message);
                break;
            case "timerecEntryDelete":
                handleTimerRecEntryDelete(message);
                break;
            case "autorecEntryAdd":
                handleAutorecEntryAdd(message);
                break;
            case "autorecEntryUpdate":
                handleAutorecEntryUpdate(message);
                break;
            case "autorecEntryDelete":
                handleAutorecEntryDelete(message);
                break;
            case "eventAdd":
                handleEventAdd(message);
                break;
            case "eventUpdate":
                handleEventUpdate(message);
                storeLastUpdate();
                break;
            case "eventDelete":
                handleEventDelete(message);
                break;
            case "initialSyncCompleted":
                handleInitialSyncCompleted();
                break;
            case "getSysTime":
                handleSystemTime(message);
                break;
            case "getDiskSpace":
                handleDiskSpace(message);
                break;
            case "getProfiles":
                handleHtspProfiles(message);
                break;
            case "getDvrConfigs":
                handleDvrConfigs(message);
                break;
            case "getEvents":
                handleGetEvents(message);
                break;
        }
    }

    private void handleGetEvents(HtspMessage message) {
        if (message.containsKey("events")) {
            for (HtspMessage msg : message.getHtspMessageArray("events")) {
                Program program = EpgSyncUtils.convertMessageToProgramModel(new Program(), msg);
                program.setConnectionId(connection.getId());
                pendingEventOps.add(program);
            }
            flushPendingEventOps();
        }
    }

    void handleIntent(Intent intent) {
        if (intent == null || TextUtils.isEmpty(intent.getAction())) {
            return;
        }

        switch (intent.getAction()) {
            case "getStatus":
                getStatus();
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
            case "getMoreEvents":
                getMoreEvents(intent.getIntExtra("numFollowing", 25));
                break;
            case "getEvent":
                getEvent(intent);
                break;
            case "getEvents":
                getEvents(intent);
                break;
            case "deleteEvents":
                deleteEvents();
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
        }
    }

    private void storeLastUpdate() {
        long unixTime = System.currentTimeMillis() / 1000L;
        Timber.d("Saving last update time: " + unixTime);
        connection.setLastUpdate(unixTime);
        appRepository.getConnectionData().updateItem(connection);
    }

    private void handleInitialServerResponse(HtspMessage response) {
        ServerStatus serverStatus = appRepository.getServerStatusData().getActiveItem();
        ServerStatus updatedServerStatus = EpgSyncUtils.convertMessageToServerStatusModel(serverStatus, response);
        updatedServerStatus.setConnectionId(connection.getId());
        updatedServerStatus.setConnectionName(connection.getName());
        Timber.d("Received initial response from server " + updatedServerStatus.getServerName() + ", api version: " + updatedServerStatus.getHtspVersion());

        htspVersion = updatedServerStatus.getHtspVersion();
        appRepository.getServerStatusData().updateItem(updatedServerStatus);
    }

    /**
     * Server to client method.
     * A channel tag has been added on the server. Additionally to saving the new tag the
     * number of associated channels will be
     *
     * @param msg The message with the new tag data
     */
    private void handleTagAdd(HtspMessage msg) {
        // During initial sync no channels are yet saved. So use the temporarily
        // stored channels to calculate the channel count for the channel tag
        List<Channel> channels = initialSyncCompleted ? appRepository.getChannelData().getItems() : pendingChannelOps;
        ChannelTag tag = EpgSyncUtils.convertMessageToChannelTagModel(new ChannelTag(), msg, channels);
        tag.setConnectionId(connection.getId());
        appRepository.getChannelTagData().addItem(tag);
        // Get the tag id and all channel ids of the tag so that
        // new entries where the tagId is present can be added to the database
        handleTagAndChannelRelation(tag);

        // Update the icon if required
        final String icon = msg.getString("tagIcon", null);
        if (icon != null && connection.isSyncRequired()) {
            pendingChannelLogoFetches.add(icon);
        }
    }

    /**
     * Server to client method.
     * A tag has been updated on the server.
     *
     * @param msg The message with the updated tag data
     */
    private void handleTagUpdate(HtspMessage msg) {
        ChannelTag tag = appRepository.getChannelTagData().getItemById(msg.getInteger("tagId"));
        if (tag == null) {
            Timber.d("Could not find a channel tag with id " + msg.getInteger("tagId") + " in the database");
            return;
        }

        // During initial sync no channels are yet saved. So use the temporarily
        // stored channels to calculate the channel count for the channel tag
        List<Channel> channels = initialSyncCompleted ? appRepository.getChannelData().getItems() : pendingChannelOps;
        ChannelTag updatedTag = EpgSyncUtils.convertMessageToChannelTagModel(tag, msg, channels);
        appRepository.getChannelTagData().updateItem(updatedTag);
        // Remove all entries of this tag from the database before
        // adding new ones which are defined in the members variable
        handleTagAndChannelRelation(updatedTag);

        // Update the icon if required
        final String icon = msg.getString("tagIcon");
        if (icon != null) {
            pendingChannelLogoFetches.add(icon);
        }
    }

    /**
     * Saves the channel to channel tag relations to allow filtering channels by their tags.
     * Prior to that any previous channel to tag relation are removed from the table to
     * prevent having outdated date
     *
     * @param tag The channel tag
     */
    private void handleTagAndChannelRelation(ChannelTag tag) {
        appRepository.getTagAndChannelData().removeItemByTagId(tag.getTagId());

        List<Integer> channelIds = tag.getMembers();
        if (channelIds != null) {
            for (Integer channelId : channelIds) {
                TagAndChannel tagAndChannel = new TagAndChannel();
                tagAndChannel.setTagId(tag.getTagId());
                tagAndChannel.setChannelId(channelId);
                tagAndChannel.setConnectionId(connection.getId());
                appRepository.getTagAndChannelData().addItem(tagAndChannel);
            }
        }
    }

    /**
     * Server to client method.
     * A tag has been deleted on the server.
     *
     * @param msg The message with the tag id that was deleted
     */
    private void handleTagDelete(HtspMessage msg) {
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
    private void handleChannelAdd(HtspMessage msg) {
        Channel channel = EpgSyncUtils.convertMessageToChannelModel(new Channel(), msg);
        channel.setConnectionId(connection.getId());
        if (!initialSyncCompleted) {
            pendingChannelOps.add(channel);
        } else {
            appRepository.getChannelData().addItem(channel);
        }
        // Update the icon only if a full sync was required
        final String icon = msg.getString("channelIcon");
        if (icon != null && connection.isSyncRequired()) {
            pendingChannelLogoFetches.add(icon);
        }
    }

    /**
     * Server to client method.
     * A channel has been updated on the server.
     *
     * @param msg The message with the updated channel data
     */
    private void handleChannelUpdate(HtspMessage msg) {
        Channel channel = appRepository.getChannelData().getItemById(msg.getInteger("channelId"));
        if (channel == null) {
            Timber.d("Could not find a channel with id " + msg.getInteger("channelId") + " in the database");
            return;
        }
        Channel updatedChannel = EpgSyncUtils.convertMessageToChannelModel(channel, msg);
        appRepository.getChannelData().updateItem(updatedChannel);

        // Update the icon only if a full sync was required
        final String icon = msg.getString("channelIcon");
        if (icon != null) {
            pendingChannelLogoFetches.add(icon);
        }
    }

    /**
     * Server to client method.
     * A channel has been deleted on the server.
     *
     * @param msg The message with the channel id that was deleted
     */
    private void handleChannelDelete(HtspMessage msg) {
        if (msg.containsKey("channelId")) {
            int channelId = msg.getInteger("channelId");

            Channel channel = appRepository.getChannelData().getItemById(channelId);
            if (channel != null && !TextUtils.isEmpty(channel.getIcon())) {
                deleteIconFileFromCache(channel.getIcon());
            } else {
                Timber.e("Could not delete channel icon from database");
            }
            if (channel != null) {
                appRepository.getChannelData().removeItem(channel);
            }
        }
    }

    /**
     * Server to client method.
     * A recording has been added on the server.
     *
     * @param msg The message with the new recording data
     */
    private void handleDvrEntryAdd(HtspMessage msg) {
        Recording recording = EpgSyncUtils.convertMessageToRecordingModel(new Recording(), msg);
        recording.setConnectionId(connection.getId());
        if (!initialSyncCompleted) {
            pendingRecordingOps.add(recording);
        } else {
            appRepository.getRecordingData().addItem(recording);
        }

        if (sharedPreferences.getBoolean("notifications_enabled", false)) {
            if (recording.isScheduled() && recording.getStart() > new Date().getTime()) {
                Timber.d("Adding notification for recording " + recording.getTitle());
                Integer offset = Integer.valueOf(sharedPreferences.getString("notification_lead_time", "0"));
                NotificationUtils.addRecordingNotification(context, recording, offset);
            }
        }
    }

    /**
     * Server to client method.
     * A recording has been updated on the server.
     *
     * @param msg The message with the updated recording data
     */
    private void handleDvrEntryUpdate(HtspMessage msg) {
        // Get the existing recording
        Recording recording = appRepository.getRecordingData().getItemById(msg.getInteger("id"));
        if (recording == null) {
            Timber.d("Could not find a recording with id " + msg.getInteger("id") + " in the database");
            return;
        }
        Recording updatedRecording = EpgSyncUtils.convertMessageToRecordingModel(recording, msg);
        appRepository.getRecordingData().updateItem(updatedRecording);

        if (sharedPreferences.getBoolean("notifications_enabled", false)) {
            if (!recording.isScheduled()) {
                Timber.d("Removing notification for recording " + recording.getTitle());
                NotificationUtils.removeRecordingNotification(context, recording.getId());
            }
        }
    }

    /**
     * Server to client method.
     * A recording has been deleted on the server.
     *
     * @param msg The message with the recording id that was deleted
     */
    private void handleDvrEntryDelete(HtspMessage msg) {
        if (msg.containsKey("id")) {
            Recording recording = appRepository.getRecordingData().getItemById(msg.getInteger("id"));
            if (recording != null) {
                appRepository.getRecordingData().removeItem(recording);
                if (sharedPreferences.getBoolean("notifications_enabled", false)) {
                    Timber.d("Removing notification for recording " + recording.getTitle());
                    NotificationUtils.removeRecordingNotification(context, recording.getId());
                }
            }
        }
    }

    /**
     * Server to client method.
     * A series recording has been added on the server.
     *
     * @param msg The message with the new series recording data
     */
    private void handleAutorecEntryAdd(HtspMessage msg) {
        SeriesRecording seriesRecording = EpgSyncUtils.convertMessageToSeriesRecordingModel(new SeriesRecording(), msg);
        seriesRecording.setConnectionId(connection.getId());
        appRepository.getSeriesRecordingData().addItem(seriesRecording);
    }

    /**
     * Server to client method.
     * A series recording has been updated on the server.
     *
     * @param msg The message with the updated series recording data
     */
    private void handleAutorecEntryUpdate(HtspMessage msg) {
        SeriesRecording recording = appRepository.getSeriesRecordingData().getItemById(msg.getString("id"));
        if (recording == null) {
            Timber.d("Could not find a series recording with id " + msg.getString("id") + " in the database");
            return;
        }
        SeriesRecording updatedRecording = EpgSyncUtils.convertMessageToSeriesRecordingModel(recording, msg);
        appRepository.getSeriesRecordingData().updateItem(updatedRecording);
    }

    /**
     * Server to client method.
     * A series recording has been deleted on the server.
     *
     * @param msg The message with the series recording id that was deleted
     */
    private void handleAutorecEntryDelete(HtspMessage msg) {
        if (msg.containsKey("id")) {
            SeriesRecording seriesRecording = appRepository.getSeriesRecordingData().getItemById(msg.getString("id"));
            if (seriesRecording != null) {
                appRepository.getSeriesRecordingData().removeItem(seriesRecording);
            }
        }
    }

    /**
     * Server to client method.
     * A timer recording has been added on the server.
     *
     * @param msg The message with the new timer recording data
     */
    private void handleTimerRecEntryAdd(HtspMessage msg) {
        TimerRecording recording = EpgSyncUtils.convertMessageToTimerRecordingModel(new TimerRecording(), msg);
        recording.setConnectionId(connection.getId());
        appRepository.getTimerRecordingData().addItem(recording);
    }

    /**
     * Server to client method.
     * A timer recording has been updated on the server.
     *
     * @param msg The message with the updated timer recording data
     */
    private void handleTimerRecEntryUpdate(HtspMessage msg) {
        TimerRecording recording = appRepository.getTimerRecordingData().getItemById(msg.getString("id"));
        if (recording == null) {
            Timber.d("Could not find a timer recording with id " + msg.getString("id") + " in the database");
            return;
        }
        TimerRecording updatedRecording = EpgSyncUtils.convertMessageToTimerRecordingModel(recording, msg);
        appRepository.getTimerRecordingData().updateItem(updatedRecording);
    }

    /**
     * Server to client method.
     * A timer recording has been deleted on the server.
     *
     * @param msg The message with the recording id that was deleted
     */
    private void handleTimerRecEntryDelete(HtspMessage msg) {
        if (msg.containsKey("id")) {
            TimerRecording timerRecording = appRepository.getTimerRecordingData().getItemById(msg.getString("id"));
            if (timerRecording != null) {
                appRepository.getTimerRecordingData().removeItem(timerRecording);
            }
        }
    }

    /**
     * Server to client method.
     * An epg event has been added on the server.
     *
     * @param msg The message with the new epg event data
     */
    private void handleEventAdd(HtspMessage msg) {
        Program program = EpgSyncUtils.convertMessageToProgramModel(new Program(), msg);
        program.setConnectionId(connection.getId());
        if (!initialSyncCompleted) {
            pendingEventOps.add(program);
        } else {
            appRepository.getProgramData().addItem(program);
        }
    }

    /**
     * Server to client method.
     * An epg event has been updated on the server.
     *
     * @param msg The message with the updated epg event data
     */
    private void handleEventUpdate(HtspMessage msg) {
        Program program = appRepository.getProgramData().getItemById(msg.getInteger("eventId"));
        if (program == null) {
            Timber.d("Could not find a program with id " + msg.getInteger("eventId") + " in the database");
            return;
        }
        Program updatedProgram = EpgSyncUtils.convertMessageToProgramModel(program, msg);
        appRepository.getProgramData().updateItem(updatedProgram);
    }

    /**
     * Server to client method.
     * An epg event has been deleted on the server.
     *
     * @param msg The message with the epg event id that was deleted
     */
    private void handleEventDelete(HtspMessage msg) {
        if (msg.containsKey("id")) {
            appRepository.getProgramData().removeItemById(msg.getInteger("id"));
        }
    }


    private void handleHtspProfiles(HtspMessage message) {
        Timber.d("Handling htsp playback profiles");
        if (message.containsKey("profiles")) {
            for (HtspMessage msg : message.getHtspMessageArray("profiles")) {
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

    private void handleHttpProfiles(HtspMessage message) {
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

    private void handleDvrConfigs(HtspMessage message) {
        Timber.d("Handling recording profiles");
        if (message.containsKey("dvrconfigs")) {
            for (HtspMessage msg : message.getHtspMessageArray("dvrconfigs")) {
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

    private void handleSystemTime(HtspMessage message) {
        int gmtOffsetFromServer = message.getInteger("gmtoffset", 0) * 60 * 1000;
        int gmtOffset = gmtOffsetFromServer - MiscUtils.getDaylightSavingOffset();
        Timber.d("GMT offset from server is " + gmtOffsetFromServer +
                ", GMT offset considering daylight saving offset is " + gmtOffset);

        ServerStatus serverStatus = appRepository.getServerStatusData().getActiveItem();
        serverStatus.setGmtoffset(gmtOffset);
        serverStatus.setTime(message.getLong("time", 0));
        appRepository.getServerStatusData().updateItem(serverStatus);

        Timber.d("Received system time from server " + serverStatus.getServerName()
                + ", server time: " + serverStatus.getTime()
                + ", server gmt offset: " + serverStatus.getGmtoffset());
    }

    private void handleDiskSpace(HtspMessage message) {
        ServerStatus serverStatus = appRepository.getServerStatusData().getActiveItem();
        serverStatus.setFreeDiskSpace(message.getLong("freediskspace", 0));
        serverStatus.setTotalDiskSpace(message.getLong("totaldiskspace", 0));
        appRepository.getServerStatusData().updateItem(serverStatus);

        Timber.d("Received disk space information from server " + serverStatus.getServerName()
                + ", free disk space: " + serverStatus.getFreeDiskSpace()
                + ", total disk space: " + serverStatus.getTotalDiskSpace());
    }

    private void handleInitialSyncCompleted() {
        Timber.d("Received initial data from server");
        sendEpgSyncStatusMessage(ServiceStatusReceiver.State.SYNC_IN_PROGRESS,
                context.getString(R.string.saving_data), "");

        // Flush all received data to the database
        flushPendingChannelOps();
        flushPendingRecordingsOps();
        flushPendingEventOps();
        flushPendingChannelLogoFetches();

        // Get additional information
        getDiscSpace();
        getSystemTime();
        getProfiles();
        getHttpProfiles();
        getDvrConfigs();

        addMissingHtspPlaybackProfileIfNotExists("htsp");
        addMissingHttpPlaybackProfileIfNotExists("matroska");
        addMissingHttpPlaybackProfileIfNotExists("audio");
        addMissingHttpPlaybackProfileIfNotExists("pass");

        setDefaultProfileSelection();

        connection.setSyncRequired(false);
        appRepository.getConnectionData().updateItem(connection);

        initialSyncCompleted = true;

        sendEpgSyncStatusMessage(ServiceStatusReceiver.State.SYNC_DONE,
                context.getString(R.string.loading_data_done), "");

        Timber.d("Done receiving initial data from server");
    }

    private void setDefaultProfileSelection() {
        Timber.d("Setting default profiles in case none are selected yet");
        ServerStatus serverStatus = appRepository.getServerStatusData().getActiveItem();
        if (serverStatus != null) {
            if (serverStatus.getHtspPlaybackServerProfileId() == 0) {
                for (ServerProfile serverProfile : appRepository.getServerProfileData().getHtspPlaybackProfiles()) {
                    if (serverProfile.getName().equals("htsp")) {
                        Timber.d("Setting htsp profile to htsp");
                        serverStatus.setHtspPlaybackServerProfileId(serverProfile.getId());
                        break;
                    }
                }
            }
            if (serverStatus.getHttpPlaybackServerProfileId() == 0) {
                for (ServerProfile serverProfile : appRepository.getServerProfileData().getHttpPlaybackProfiles()) {
                    if (serverProfile.getName().equals("pass")) {
                        Timber.d("Setting http profile to pass");
                        serverStatus.setHttpPlaybackServerProfileId(serverProfile.getId());
                        break;
                    }
                }
            }
            if (serverStatus.getRecordingServerProfileId() == 0) {
                for (ServerProfile serverProfile : appRepository.getServerProfileData().getRecordingProfiles()) {
                    if (serverProfile.getName().equals("Default Profile")) {
                        Timber.d("Setting recording profile to default");
                        serverStatus.setRecordingServerProfileId(serverProfile.getId());
                        break;
                    }
                }
            }
            appRepository.getServerStatusData().updateItem(serverStatus);
        }
    }

    private void flushPendingChannelLogoFetches() {
        Timber.d("Downloading and saving channel logos...");
        sendEpgSyncStatusMessage(ServiceStatusReceiver.State.SYNC_IN_PROGRESS,
                context.getString(R.string.saving_channel_logos), "");

        if (pendingChannelLogoFetches.isEmpty()) {
            return;
        }

        for (String icon : pendingChannelLogoFetches) {
            try {
                downloadIconFromFileUrl(icon);
            } catch (Exception e) {
                Timber.d("handleChannelUpdate: Could not load icon '" + icon + "'");
            }
        }
        pendingChannelLogoFetches.clear();
    }

    private void flushPendingChannelOps() {
        if (pendingChannelOps.isEmpty()) {
            return;
        }
        Timber.d("Saving " + pendingChannelOps.size() + " channels...");
        sendEpgSyncStatusMessage(ServiceStatusReceiver.State.SYNC_IN_PROGRESS,
                context.getString(R.string.saving_channels), "");

        appRepository.getChannelData().addItems(pendingChannelOps);
        pendingChannelOps.clear();
    }

    private void flushPendingRecordingsOps() {
        // Remove all recordings from the database to prevent being out of sync with the server.
        // This could be the case when the app was offline for a while and it did not receive
        // any recording removal information from the server. During the initial sync the
        // server only provides the list of available recordings.
        appRepository.getRecordingData().removeItems();

        if (pendingRecordingOps.isEmpty()) {
            return;
        }
        Timber.d("Saving " + pendingRecordingOps.size() + " recordings...");
        sendEpgSyncStatusMessage(ServiceStatusReceiver.State.SYNC_IN_PROGRESS,
                context.getString(R.string.saving_recordings), "");

        appRepository.getRecordingData().addItems(pendingRecordingOps);
        pendingRecordingOps.clear();
    }

    private void flushPendingEventOps() {
        if (pendingEventOps.isEmpty()) {
            return;
        }
        Timber.d("Saving " + pendingEventOps.size() + " program data...");
        sendEpgSyncStatusMessage(ServiceStatusReceiver.State.SYNC_IN_PROGRESS,
                context.getString(R.string.saving_program_guide_data), "");

        appRepository.getProgramData().addItems(pendingEventOps);
        pendingEventOps.clear();
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
        File file = new File(context.getCacheDir(), MiscUtils.convertUrlToHashString(url) + ".png");
        if (file.exists()) {
            return;
        }

        InputStream is;
        if (url.startsWith("http")) {
            is = new BufferedInputStream(new URL(url).openStream());
        } else if (htspVersion > 9) {
            is = new HtspFileInputStream(dispatcher, url);
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
            is = new HtspFileInputStream(dispatcher, url);
        }

        float scale = context.getResources().getDisplayMetrics().density;
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
     * @param url The url of the file
     */
    private void deleteIconFileFromCache(String url) {
        if (TextUtils.isEmpty(url)) {
            return;
        }
        File file = new File(context.getCacheDir(), MiscUtils.convertUrlToHashString(url) + ".png");
        if (!file.exists() || !file.delete()) {
            Timber.d("Could not delete icon " + file.getName());
        }
    }

    private void getDiscSpace() {
        HtspMessage request = new HtspMessage();
        request.put("method", "getDiskSpace");

        HtspMessage response = null;
        try {
            response = dispatcher.sendMessage(request, connectionTimeout);
        } catch (HtspNotConnectedException e) {
            Timber.e("Failed to get disk space - not connected", e);
        }

        if (response != null) {
            handleDiskSpace(response);
        } else {
            Timber.d("Response is null");
        }
    }

    private void getSystemTime() {
        final HtspMessage request = new HtspMessage();
        request.put("method", "getSysTime");

        HtspMessage response = null;
        try {
            response = dispatcher.sendMessage(request, connectionTimeout);
        } catch (HtspNotConnectedException e) {
            Timber.e("Failed to get system time - not connected", e);
        }

        if (response != null) {
            handleSystemTime(response);
        } else {
            Timber.d("Response is null");
        }
    }

    private void getChannel(final Intent intent) {
        final HtspMessage request = new HtspMessage();
        request.put("method", "getChannel");
        request.put("channelId", intent.getIntExtra("channelId", 0));

        HtspMessage response = null;
        try {
            response = dispatcher.sendMessage(request, connectionTimeout);
        } catch (HtspNotConnectedException e) {
            Timber.e("Failed to send getChannel - not connected", e);
        }

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
        }
    }

    private void getEvent(Intent intent) {
        final HtspMessage request = new HtspMessage();
        request.put("method", "getEvent");
        request.put("eventId", intent.getIntExtra("eventId", 0));

        HtspMessage response = null;
        try {
            response = dispatcher.sendMessage(request, connectionTimeout);
        } catch (HtspNotConnectedException e) {
            Timber.e("Failed to send getEvent - not connected", e);
        }

        if (response != null) {
            Program program = EpgSyncUtils.convertMessageToProgramModel(new Program(), response);
            appRepository.getProgramData().addItem(program);
        }
    }

    private void getEvents(Intent intent) {

        final long eventId = intent.getIntExtra("eventId", 0);
        final long channelId = intent.getIntExtra("channelId", 0);
        final long numFollowing = intent.getIntExtra("numFollowing", 0);
        final long maxTime = intent.getIntExtra("maxTime", 0);
        final boolean showMessage = intent.getBooleanExtra("showMessage", false);

        final HtspMessage request = new HtspMessage();
        request.put("method", "getEvents");
        if (eventId > 0) {
            request.put("eventId", eventId);
        }
        if (channelId > 0) {
            request.put("channelId", channelId);
        }
        if (numFollowing > 0) {
            request.put("numFollowing", numFollowing);
        }
        if (maxTime > 0) {
            request.put("maxTime", maxTime);
        }

        HtspMessage response = null;
        try {
            if (showMessage) {
                sendSnackbarMessage(context.getString(R.string.loading_more_programs));
            }
            response = dispatcher.sendMessage(request, connectionTimeout);
        } catch (HtspNotConnectedException e) {
            Timber.e("Failed to send getEvents - not connected", e);
        }

        if (response != null) {
            handleGetEvents(response);
            if (showMessage) {
                sendSnackbarMessage(context.getString(R.string.loading_more_programs_finished));
            }
        }
    }

    private void getEpgQuery(final Intent intent) {
        final String query = intent.getStringExtra("query");
        final long channelId = intent.getIntExtra("channelId", 0);
        final long tagId = intent.getIntExtra("tagId", 0);
        final int contentType = intent.getIntExtra("contentType", 0);
        final int minDuration = intent.getIntExtra("minduration", 0);
        final int maxDuration = intent.getIntExtra("maxduration", 0);
        final String language = intent.getStringExtra("language");
        final boolean full = intent.getBooleanExtra("full", false);

        final HtspMessage request = new HtspMessage();
        request.put("method", "epgQuery");
        request.put("query", query);

        if (channelId > 0) {
            request.put("channelId", channelId);
        }
        if (tagId > 0) {
            request.put("tagId", tagId);
        }
        if (contentType > 0) {
            request.put("contentType", contentType);
        }
        if (minDuration > 0) {
            request.put("minDuration", minDuration);
        }
        if (maxDuration > 0) {
            request.put("maxDuration", maxDuration);
        }
        if (language != null) {
            request.put("language", language);
        }
        request.put("full", full);

        HtspMessage response = null;
        try {
            response = dispatcher.sendMessage(request, connectionTimeout);
        } catch (HtspNotConnectedException e) {
            Timber.e("Failed to send epgQuery - not connected", e);
        }

        if (response != null) {
            // Contains the ids of those events that were returned by the query
            List<Integer> eventIdList = new ArrayList<>();
            if (response.containsKey("events")) {
                // List of events that match the query. Add the eventIds
                for (HtspMessage msg : response.getHtspMessageArray("events")) {
                    eventIdList.add(msg.getInteger("eventId"));
                }
            } else if (response.containsKey("eventIds")) {
                // List of eventIds that match the query
                for (Object obj : response.getArrayList("eventIds")) {
                    eventIdList.add((int) obj);
                }
            }
        }
    }

    private void addDvrEntry(final Intent intent) {
        HtspMessage request = EpgSyncUtils.convertIntentToDvrMessage(intent, htspVersion);
        request.put("method", "addDvrEntry");

        HtspMessage response = null;
        try {
            response = dispatcher.sendMessage(request, connectionTimeout);
        } catch (HtspNotConnectedException e) {
            Timber.e("Failed to send addDvrEntry - not connected", e);
        }

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.cancel(intent.getIntExtra("eventId", 0));

        if (response != null) {
            // Reply message fields:
            // success            u32   required   1 if entry was added, 0 otherwise
            // id                 u32   optional   ID of created DVR entry
            // error              str   optional   English clear text of error message
            if (response.getInteger("success", 0) == 1) {
                sendSnackbarMessage(context.getString(R.string.success_adding_recording));
            } else {
                sendSnackbarMessage(context.getString(R.string.error_adding_recording, response.getString("error", "")));
            }
        }
    }

    private void updateDvrEntry(final Intent intent) {
        HtspMessage request = EpgSyncUtils.convertIntentToDvrMessage(intent, htspVersion);
        request.put("method", "updateDvrEntry");
        request.put("id", intent.getIntExtra("id", 0));

        HtspMessage response = null;
        try {
            response = dispatcher.sendMessage(request, connectionTimeout);
        } catch (HtspNotConnectedException e) {
            Timber.e("Failed to send updateDvrEntry - not connected", e);
        }

        if (response != null) {
            // Reply message fields:
            // success            u32   required   1 if update as successful, otherwise 0
            // error              str   optional   Error message if update failed
            if (response.getInteger("success", 0) == 1) {
                sendSnackbarMessage(context.getString(R.string.success_updating_recording));
            } else {
                sendSnackbarMessage(context.getString(R.string.error_updating_recording, response.getString("error", "")));
            }
        }
    }

    private void removeDvrEntry(Intent intent) {
        HtspMessage request = new HtspMessage();
        request.put("method", intent.getAction());
        request.put("id", intent.getIntExtra("id", 0));

        HtspMessage response = null;
        try {
            response = dispatcher.sendMessage(request, connectionTimeout);
        } catch (HtspNotConnectedException e) {
            Timber.e("Failed to send " + intent.getAction() + " - not connected", e);
        }

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.cancel(intent.getIntExtra("id", 0));

        Timber.d("Checking response result");
        if (response != null) {
            Timber.d("Response is not null");
            if (response.getInteger("success", 0) == 1) {
                sendSnackbarMessage(context.getString(R.string.success_removing_recording));
            } else {
                sendSnackbarMessage(context.getString(R.string.error_removing_recording, response.getString("error", "")));
            }
        } else {
            Timber.d("Response is null");
        }
    }

    private void addAutorecEntry(final Intent intent) {
        HtspMessage request = EpgSyncUtils.convertIntentToAutorecMessage(intent, htspVersion);
        request.put("method", "addAutorecEntry");

        HtspMessage response = null;
        try {
            response = dispatcher.sendMessage(request, connectionTimeout);
        } catch (HtspNotConnectedException e) {
            Timber.e("Failed to send addAutorecEntry - not connected", e);
        }

        if (response != null) {
            // Reply message fields:
            // success            u32   required   1 if entry was added, 0 otherwise
            // id                 str   optional   ID (string!) of created autorec DVR entry
            // error              str   optional   English clear text of error message
            if (response.getInteger("success", 0) == 1) {
                sendSnackbarMessage(context.getString(R.string.success_adding_recording));
            } else {
                sendSnackbarMessage(context.getString(R.string.error_adding_recording, response.getString("error", "")));
            }
        }
    }

    private void updateAutorecEntry(final Intent intent) {
        HtspMessage request = new HtspMessage();
        if (htspVersion >= 25) {
            request = EpgSyncUtils.convertIntentToAutorecMessage(intent, htspVersion);
            request.put("method", "updateAutorecEntry");
        } else {
            request.put("method", "deleteAutorecEntry");
        }
        request.put("id", intent.getStringExtra("id"));

        HtspMessage response = null;
        try {
            response = dispatcher.sendMessage(request, connectionTimeout);
        } catch (HtspNotConnectedException e) {
            Timber.e("Failed to send updateAutorecEntry - not connected", e);
        }

        if (response != null) {
            // Handle the response here because the "updateAutorecEntry" call does
            // not exist on the server. First delete the entry and if this was
            // successful add a new entry with the new values.
            final boolean success = (response.getInteger("success", 0) == 1);
            if (htspVersion < 25 && success) {
                addAutorecEntry(intent);
            } else {
                if (success) {
                    sendSnackbarMessage(context.getString(R.string.success_updating_recording));
                } else {
                    sendSnackbarMessage(context.getString(R.string.error_updating_recording, response.getString("error", "")));
                }
            }
        }
    }

    private void deleteAutorecEntry(final Intent intent) {
        final HtspMessage request = new HtspMessage();
        request.put("method", "deleteAutorecEntry");
        request.put("id", intent.getStringExtra("id"));

        HtspMessage response = null;
        try {
            response = dispatcher.sendMessage(request, connectionTimeout);
        } catch (HtspNotConnectedException e) {
            Timber.e("Failed to send deleteAutorecEntry - not connected", e);
        }

        if (response != null) {
            if (response.getInteger("success", 0) == 1) {
                sendSnackbarMessage(context.getString(R.string.success_removing_recording));
            } else {
                sendSnackbarMessage(context.getString(R.string.error_removing_recording, response.getString("error", "")));
            }
        }
    }

    private void addTimerrecEntry(final Intent intent) {
        HtspMessage request = EpgSyncUtils.convertIntentToTimerecMessage(intent, htspVersion);
        request.put("method", "addTimerecEntry");

        // Reply message fields:
        // success            u32   required   1 if entry was added, 0 otherwise
        // id                 str   optional   ID (string!) of created timerec DVR entry
        // error              str   optional   English clear text of error message
        HtspMessage response = null;
        try {
            response = dispatcher.sendMessage(request, connectionTimeout);
        } catch (HtspNotConnectedException e) {
            Timber.e("Failed to send addTimerecEntry - not connected", e);
        }

        if (response != null) {
            if (response.getInteger("success", 0) == 1) {
                sendSnackbarMessage(context.getString(R.string.success_adding_recording));
            } else {
                sendSnackbarMessage(context.getString(R.string.error_adding_recording, response.getString("error", "")));
            }
        }
    }

    private void updateTimerrecEntry(final Intent intent) {
        HtspMessage request = new HtspMessage();
        if (htspVersion >= 25) {
            request = EpgSyncUtils.convertIntentToTimerecMessage(intent, htspVersion);
            request.put("method", "updateTimerecEntry");
        } else {
            request.put("method", "deleteTimerecEntry");
        }
        request.put("id", intent.getStringExtra("id"));

        HtspMessage response = null;
        try {
            response = dispatcher.sendMessage(request, connectionTimeout);
        } catch (HtspNotConnectedException e) {
            Timber.e("Failed to send updateTimerecEntry - not connected", e);
        }

        if (response != null) {
            // Handle the response here because the "updateTimerecEntry" call does
            // not exist on the server. First delete the entry and if this was
            // successful add a new entry with the new values.
            final boolean success = response.getInteger("success", 0) == 1;
            if (htspVersion < 25 && success) {
                addTimerrecEntry(intent);
            } else {
                if (success) {
                    sendSnackbarMessage(context.getString(R.string.success_updating_recording));
                } else {
                    sendSnackbarMessage(context.getString(R.string.error_updating_recording, response.getString("error", "")));
                }
            }
        }
    }

    private void deleteTimerrecEntry(Intent intent) {
        HtspMessage request = new HtspMessage();
        request.put("method", "deleteTimerecEntry");
        request.put("id", intent.getStringExtra("id"));

        HtspMessage response = null;
        try {
            response = dispatcher.sendMessage(request, connectionTimeout);
        } catch (HtspNotConnectedException e) {
            Timber.e("Failed to send deleteTimerecEntry - not connected", e);
        }

        if (response != null) {
            if (response.getInteger("success", 0) == 1) {
                sendSnackbarMessage(context.getString(R.string.success_removing_recording));
            } else {
                sendSnackbarMessage(context.getString(R.string.error_removing_recording, response.getString("error", "")));
            }
        }
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
        HtspMessage response = null;
        try {
            response = dispatcher.sendMessage(request, connectionTimeout);
        } catch (HtspNotConnectedException e) {
            Timber.e("Failed to send getTicket - not connected", e);
        }

        if (response != null) {
            Timber.d("Response is not null");
            Intent ticketIntent = new Intent("ticket");
            ticketIntent.putExtra("path", response.getString("path"));
            ticketIntent.putExtra("ticket", response.getString("ticket"));
            LocalBroadcastManager.getInstance(context).sendBroadcast(ticketIntent);
        }
    }

    private void getProfiles() {
        HtspMessage request = new HtspMessage();
        request.put("method", "getProfiles");

        HtspMessage response = null;
        try {
            response = dispatcher.sendMessage(request, connectionTimeout);
        } catch (HtspNotConnectedException e) {
            Timber.e("Failed to send getHtspProfiles - not connected", e);
        }

        if (response != null) {
            handleHtspProfiles(response);
        }
    }

    private void getHttpProfiles() {
        HtspMessage request = new HtspMessage();
        request.put("method", "api");
        request.put("path", "profile/list");

        HtspMessage response = null;
        try {
            response = dispatcher.sendMessage(request, connectionTimeout);
        } catch (HtspNotConnectedException e) {
            Timber.e("Failed to send getHttpProfiles - not connected", e);
        }

        if (response != null) {
            handleHttpProfiles(response);
        }
    }

    private void getDvrConfigs() {
        HtspMessage request = new HtspMessage();
        request.put("method", "getDvrConfigs");

        HtspMessage response = null;
        try {
            response = dispatcher.sendMessage(request, connectionTimeout);
        } catch (HtspNotConnectedException e) {
            Timber.e("Failed to send getDvrConfigs - not connected", e);
        }

        if (response != null) {
            handleDvrConfigs(response);
        }
    }

    private void getMoreEvents(int numberOfProgramsToLoad) {
        Timber.d("getMoreEvents() called");
        List<Channel> channelList = appRepository.getChannelData().getItems();
        for (Channel channel : channelList) {
            Program program = appRepository.getProgramData().getLastItemByChannelId(channel.getId());
            if (program != null && program.getEventId() > 0) {
                Timber.d("getMoreEvents: loading more events for channel " + channel.getName());
                Intent intent = new Intent();
                intent.putExtra("eventId", program.getNextEventId());
                intent.putExtra("channelId", channel.getId());
                //intent.putExtra("maxTime", 4);
                intent.putExtra("numFollowing", numberOfProgramsToLoad);
                getEvents(intent);
            }
        }
    }

    private void deleteEvents() {
        Timber.d("deleteEvents() called");

        // Get the time that was one week (7 days in millis) before now
        long oneWeekBeforeNow = new Date().getTime() - (7 * 24 * 60 * 60 * 1000);
        appRepository.getProgramData().removeItemsByTime(oneWeekBeforeNow);
    }

    private void getStatus() {
        HtspMessage message = new HtspMessage();
        message.put("method", "hello");
        // The application supports API version up to 33
        message.put("htspversion", 33);
        message.put("clientname", "TVHClient");
        message.put("clientversion", BuildConfig.VERSION_NAME);

        HtspMessage response = null;
        try {
            response = dispatcher.sendMessage(message, connectionTimeout);
        } catch (HtspNotConnectedException e) {
            Timber.e("Failed to send getStatus - not connected", e);
            sendEpgSyncStatusMessage(ServiceStatusReceiver.State.CLOSED,
                    "Not connected to server", null);
        }
        if (response == null) {
            sendEpgSyncStatusMessage(ServiceStatusReceiver.State.CLOSED,
                    "Not connected to server", null);
        }
    }

    private void sendEpgSyncStatusMessage(ServiceStatusReceiver.State state, String msg, String details) {
        Intent intent = new Intent(ServiceStatusReceiver.ACTION);
        intent.putExtra(ServiceStatusReceiver.STATE, state);
        if (!TextUtils.isEmpty(msg)) {
            intent.putExtra(ServiceStatusReceiver.MESSAGE, msg);
        }
        if (!TextUtils.isEmpty(details)) {
            intent.putExtra(ServiceStatusReceiver.DETAILS, details);
        }
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    private void sendSnackbarMessage(String msg) {
        Intent intent = new Intent(SnackbarMessageReceiver.ACTION);
        intent.putExtra(SnackbarMessageReceiver.CONTENT, msg);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }
}