package org.tvheadend.tvhclient.data.remote;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;

import org.tvheadend.tvhclient.BuildConfig;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.entity.Channel;
import org.tvheadend.tvhclient.data.entity.ChannelTag;
import org.tvheadend.tvhclient.data.entity.Program;
import org.tvheadend.tvhclient.data.entity.Recording;
import org.tvheadend.tvhclient.data.entity.SeriesRecording;
import org.tvheadend.tvhclient.data.entity.ServerProfile;
import org.tvheadend.tvhclient.data.entity.ServerStatus;
import org.tvheadend.tvhclient.data.entity.TagAndChannel;
import org.tvheadend.tvhclient.data.entity.TimerRecording;
import org.tvheadend.tvhclient.data.local.db.AppRoomDatabase;
import org.tvheadend.tvhclient.data.remote.htsp.HtspConnection;
import org.tvheadend.tvhclient.data.remote.htsp.HtspFileInputStream;
import org.tvheadend.tvhclient.data.remote.htsp.HtspMessage;
import org.tvheadend.tvhclient.data.remote.htsp.HtspNotConnectedException;
import org.tvheadend.tvhclient.data.remote.htsp.tasks.Authenticator;
import org.tvheadend.tvhclient.utils.MiscUtils;

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

import timber.log.Timber;

public class EpgSyncTask implements HtspMessage.Listener, Authenticator.Listener, HtspConnection.Listener {
    private static final String TAG = EpgSyncTask.class.getSimpleName();

    private final AppRoomDatabase db;
    private final int connectionTimeout;
    private final int connectionId;
    private int htspVersion;
    private boolean initialSyncCompleted;
    private boolean initialSyncRequired;
    private final Context context;
    private final HtspMessage.Dispatcher dispatcher;
    private final SharedPreferences sharedPreferences;
    private final Handler handler;

    private final ArrayList<Channel> pendingChannelOps = new ArrayList<>();
    private final ArrayList<TagAndChannel> pendingChannelTagRelationOps = new ArrayList<>();
    private final ArrayList<Recording> pendingRecordedProgramOps = new ArrayList<>();
    private final ArrayList<Program> pendingEventOps = new ArrayList<>();
    private final Queue<String> pendingChannelLogoFetches = new ConcurrentLinkedQueue<>();

    public enum State {
        IDLE,
        RECONNECT,
        LOADING,
        SAVING,
        DONE
    }

    EpgSyncTask(Context context, @NonNull HtspMessage.Dispatcher dispatcher, int connectionId) {
        this.context = context;
        this.dispatcher = dispatcher;
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        this.db = AppRoomDatabase.getInstance(context.getApplicationContext());
        HandlerThread handlerThread = new HandlerThread("EpgSyncTask Handler Thread");
        handlerThread.start();
        this.handler = new Handler(handlerThread.getLooper());
        this.connectionTimeout = Integer.valueOf(sharedPreferences.getString("connectionTimeout", "5")) * 1000;
        this.htspVersion = 13;
        this.connectionId = connectionId;
    }


    // Authenticator.Listener Methods
    @Override
    public void onAuthenticationStateChange(@NonNull Authenticator.State state) {
/*
        String details;
        if (state == Authenticator.State.AUTHENTICATING) {
            details = "Authenticating...";
        } else if (state == Authenticator.State.AUTHENTICATED) {
            details = "Authenticated";
        } else if (state == Authenticator.State.FAILED_BAD_CREDENTIALS) {
            details = "Authentication failed, bad username or password";
        } else {
            details = "Authentication failed";
        }
*/
        // Send the authentication status as details to any broadcast listeners
        Intent intent = new Intent("service_status");
        //intent.putExtra("state", state);
        //intent.putExtra("details", details);
        intent.putExtra("authentication_state", state);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

        // Continue with getting all initial data only if we are authenticated
        if (state == Authenticator.State.AUTHENTICATED) {
            initialSyncCompleted = false;
            initialSyncRequired = sharedPreferences.getBoolean("initial_sync_required", true);
            startFullInitialSyncWithServer();
        }
    }

    private void startFullInitialSyncWithServer() {
        Timber.d("startFullInitialSyncWithServer() called");

        // Send the first sync message to any broadcast listeners
        Intent intent = new Intent("service_status");
        intent.putExtra("sync_state", State.LOADING);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

        // Enable epg sync with the defined number of
        // seconds of data starting from the current time
        HtspMessage enableAsyncMetadataRequest = new HtspMessage();
        enableAsyncMetadataRequest.put("method", "enableAsyncMetadata");
        enableAsyncMetadataRequest.put("epg", 1);

        long epgMaxTime = 3600 + (System.currentTimeMillis() / 1000L);
        enableAsyncMetadataRequest.put("epgMaxTime", epgMaxTime);

        // Only provide metadata that has changed since this time.
        // Whenever the message eventUpdate is received from the server
        // the current time will be stored in the preferences.
        final long lastUpdate = sharedPreferences.getLong("last_update", 0);
        enableAsyncMetadataRequest.put("lastUpdate", lastUpdate);

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
    public void onConnectionStateChange(@NonNull HtspConnection.State state) {
        Timber.d("Connection state is " + state);

        // Send the message about the current connection status.
        Intent intent = new Intent("service_status");
        intent.putExtra("connection_state", state);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    @Override
    public void onMessage(@NonNull HtspMessage message) {
        final String method = message.getString("method");

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
                handleProfiles(message);
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
                program.setConnectionId(connectionId);
                pendingEventOps.add(program);
            }
            flushPendingEventOps();
        }
    }

    void handleIntent(Intent intent) {
        if (intent == null || intent.getAction() == null) {
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
                getMoreEvents();
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

    // Internal Methods
    private void storeLastUpdate() {
        long unixTime = System.currentTimeMillis() / 1000L;
        sharedPreferences.edit().putLong("last_update", unixTime).apply();
    }

    private void handleInitialServerResponse(HtspMessage response) {
        Timber.d("Received initial server response");
        // Get the values from the database to have access
        // to them later without querying the db too often
        htspVersion = response.getInteger("htspversion", 13);

        boolean insertNewServerStatus = false;
        ServerStatus serverStatus = db.serverStatusDao().loadServerStatusSync();
        if (serverStatus == null) {
            serverStatus = new ServerStatus();
            serverStatus.setConnectionId(connectionId);
            insertNewServerStatus = true;
        }
        serverStatus.setHtspVersion(htspVersion);
        serverStatus.setServerName(response.getString("servername"));
        serverStatus.setServerVersion(response.getString("serverversion"));
        serverStatus.setWebroot(response.getString("webroot"));

        if (insertNewServerStatus) {
            Timber.d("Received initial server response, adding new server status");
            db.serverStatusDao().insert(serverStatus);
        } else {
            Timber.d("Received initial server response, updating existing server status");
            db.serverStatusDao().update(serverStatus);
        }
    }

    /**
     * Server to client method.
     * A tag has been added on the server.
     *
     * @param msg The message with the new tag data
     */
    private void handleTagAdd(HtspMessage msg) {
        Timber.d("handleTagAdd() called with: msg = [" + msg + "]");
        ChannelTag tag = EpgSyncUtils.convertMessageToChannelTagModel(new ChannelTag(), msg);
        tag.setConnectionId(connectionId);
        db.channelTagDao().insert(tag);

        Intent intent = new Intent("service_status");
        intent.putExtra("sync_state", State.LOADING);
        intent.putExtra("sync_details", "Receiving channel tag...");

        // Get the tag id and all channel ids of the tag so that
        // new entries where the tagId is present can be added to the database
        handleTagAndChannelRelation(tag);

        // Update the icon if required
        final String icon = msg.getString("tagIcon", null);
        if (icon != null) {
            try {
                downloadIconFromFileUrl(icon);
            } catch (Exception e) {
                Timber.d("Could not load icon '" + icon + "'");
            }
        }
    }

    /**
     * Server to client method.
     * A tag has been updated on the server.
     *
     * @param msg The message with the updated tag data
     */
    private void handleTagUpdate(HtspMessage msg) {
        Timber.d("handleTagUpdate() called with: msg = [" + msg + "]");
        ChannelTag tag = db.channelTagDao().loadChannelTagByIdSync(msg.getInteger("tagId"));
        ChannelTag updatedTag = EpgSyncUtils.convertMessageToChannelTagModel(tag, msg);
        db.channelTagDao().update(updatedTag);

        // Remove all entries of this tag from the database before
        // adding new ones which are defined in the members variable
        handleTagAndChannelRelation(updatedTag);

        // Update the icon if required
        final String icon = msg.getString("tagIcon");
        if (icon != null) {
            try {
                downloadIconFromFileUrl(icon);
            } catch (Exception e) {
                Timber.d("handleTagUpdate: Could not load icon '" + icon + "'");
            }
        }
    }

    private void handleTagAndChannelRelation(ChannelTag tag) {
        // TODO channel count to much
        Timber.d("Checking channel to tag relation");
        db.tagAndChannelDao().deleteByTagId(tag.getTagId());
        List<Integer> channelIds = tag.getMembers();
        List<Integer> availableChannelIds = db.channelDao().loadAllChannelIds();
        if (channelIds != null) {
            for (Integer channelId : channelIds) {
                Timber.d("Checking if channel id " + channelId + " is in the list of available channels");
                if (availableChannelIds.contains(channelId)) {
                    Timber.d("Adding new channel to tag relation with channel id: " + channelId + " and tag id: " + tag.getTagId());
                    TagAndChannel tagAndChannel = new TagAndChannel();
                    tagAndChannel.setTagId(tag.getTagId());
                    tagAndChannel.setChannelId(channelId);
                    tagAndChannel.setConnectionId(connectionId);
                    pendingChannelTagRelationOps.add(tagAndChannel);
                }
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
            ChannelTag tag = db.channelTagDao().loadChannelTagByIdSync(msg.getInteger("tagId"));
            deleteIconFileFromCache(tag.getTagIcon());
            db.channelTagDao().deleteById(tag.getTagId());
            db.tagAndChannelDao().deleteByTagId(tag.getTagId());
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
        channel.setConnectionId(connectionId);
        Timber.d("Adding channel " + channel.getName() + " with id " + channel.getId() + ", connection id " + channel.getConnectionId());
        if (!initialSyncCompleted) {
            if (pendingChannelOps.isEmpty()) {
                Intent intent = new Intent("service_status");
                intent.putExtra("sync_state", State.LOADING);
                intent.putExtra("sync_details", "Receiving channels...");
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
            }
            pendingChannelOps.add(channel);
        } else {
            db.channelDao().insert(channel);
        }

        // Update the icon if required
        final String icon = msg.getString("channelIcon", null);
        if (icon != null) {
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
        Channel channel = db.channelDao().loadChannelByIdSync(msg.getInteger("channelId"));
        Channel updatedChannel = EpgSyncUtils.convertMessageToChannelModel(channel, msg);

        Timber.d("Updating channel " + channel.getName() + " with id " + channel.getId() + ", connection id " + channel.getConnectionId());

        if (!initialSyncCompleted) {
            pendingChannelOps.add(updatedChannel);
        } else {
            db.channelDao().update(updatedChannel);
        }

        // Update the icon if required
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

            Channel channel = db.channelDao().loadChannelByIdSync(channelId);
            if (channel != null && !TextUtils.isEmpty(channel.getIcon())) {
                deleteIconFileFromCache(channel.getIcon());
            } else {
                Timber.e("Could not delete channel icon from database");
            }
            db.channelDao().deleteById(channelId);
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
        recording.setConnectionId(connectionId);
        if (!initialSyncCompleted) {
            if (pendingRecordedProgramOps.isEmpty()) {
                Intent intent = new Intent("service_status");
                intent.putExtra("sync_state", State.LOADING);
                intent.putExtra("sync_details", "Receiving recordings...");
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
            }
            pendingRecordedProgramOps.add(recording);
        } else {
            db.recordingDao().insert(recording);
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
        Recording recording = db.recordingDao().loadRecordingByIdSync(msg.getInteger("id"));
        Recording updatedRecording = EpgSyncUtils.convertMessageToRecordingModel(recording, msg);
        if (!initialSyncCompleted) {
            pendingRecordedProgramOps.add(updatedRecording);
        } else {
            db.recordingDao().update(updatedRecording);
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
            db.recordingDao().deleteById(msg.getInteger("id"));
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
        seriesRecording.setConnectionId(connectionId);
        db.seriesRecordingDao().insert(seriesRecording);
    }

    /**
     * Server to client method.
     * A series recording has been updated on the server.
     *
     * @param msg The message with the updated series recording data
     */
    private void handleAutorecEntryUpdate(HtspMessage msg) {
        SeriesRecording recording = db.seriesRecordingDao().loadRecordingByIdSync(msg.getString("id"));
        SeriesRecording updatedRecording = EpgSyncUtils.convertMessageToSeriesRecordingModel(recording, msg);
        db.seriesRecordingDao().update(updatedRecording);
    }

    /**
     * Server to client method.
     * A series recording has been deleted on the server.
     *
     * @param msg The message with the series recording id that was deleted
     */
    private void handleAutorecEntryDelete(HtspMessage msg) {
        if (msg.containsKey("id")) {
            db.seriesRecordingDao().deleteById(msg.getString("id"));
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
        recording.setConnectionId(connectionId);
        db.timerRecordingDao().insert(recording);
    }

    /**
     * Server to client method.
     * A timer recording has been updated on the server.
     *
     * @param msg The message with the updated timer recording data
     */
    private void handleTimerRecEntryUpdate(HtspMessage msg) {
        TimerRecording recording = db.timerRecordingDao().loadRecordingByIdSync(msg.getString("id"));
        TimerRecording updatedRecording = EpgSyncUtils.convertMessageToTimerRecordingModel(recording, msg);
        db.timerRecordingDao().update(updatedRecording);
    }

    /**
     * Server to client method.
     * A timer recording has been deleted on the server.
     *
     * @param msg The message with the recording id that was deleted
     */
    private void handleTimerRecEntryDelete(HtspMessage msg) {
        if (msg.containsKey("id")) {
            db.timerRecordingDao().deleteById(msg.getString("id"));
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
        program.setConnectionId(connectionId);
        if (!initialSyncCompleted) {
            if (pendingEventOps.isEmpty()) {
                Intent intent = new Intent("service_status");
                intent.putExtra("sync_state", State.LOADING);
                intent.putExtra("sync_details", "Receiving program data...");
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
            }
            pendingEventOps.add(program);
            if (pendingEventOps.size() % 50 == 0) {
                Intent intent = new Intent("service_status");
                intent.putExtra("sync_state", State.LOADING);
                intent.putExtra("sync_details", "Received " + pendingEventOps.size() + " programs");
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
            }
        } else {
            db.programDao().insert(program);
        }
    }

    /**
     * Server to client method.
     * An epg event has been updated on the server.
     *
     * @param msg The message with the updated epg event data
     */
    private void handleEventUpdate(HtspMessage msg) {
        Program program = db.programDao().loadProgramByIdSync(msg.getInteger("eventId"));
        Program updatedProgram = EpgSyncUtils.convertMessageToProgramModel(program, msg);
        if (!initialSyncCompleted) {
            pendingEventOps.add(updatedProgram);
        } else {
            db.programDao().update(updatedProgram);
        }
    }

    /**
     * Server to client method.
     * An epg event has been deleted on the server.
     *
     * @param msg The message with the epg event id that was deleted
     */
    private void handleEventDelete(HtspMessage msg) {
        if (msg.containsKey("id")) {
            db.programDao().deleteById(msg.getInteger("id"));
        }
    }


    private void handleProfiles(HtspMessage message) {
        if (message.containsKey("profiles")) {
            ServerStatus serverStatus = db.serverStatusDao().loadServerStatusSync();
            for (HtspMessage msg : message.getHtspMessageArray("profiles")) {
                ServerProfile serverProfile = db.serverProfileDao().loadProfileByUuidSync(msg.getString("uuid"));
                if (serverProfile == null) {
                    serverProfile = new ServerProfile();
                }

                serverProfile.setConnectionId(serverStatus.getConnectionId());
                serverProfile.setUuid(msg.getString("uuid"));
                serverProfile.setName(msg.getString("name"));
                serverProfile.setComment(msg.getString("comment"));
                serverProfile.setType("playback");

                if (serverProfile.getId() == 0) {
                    db.serverProfileDao().insert(serverProfile);
                } else {
                    db.serverProfileDao().update(serverProfile);
                }
            }
        }
    }

    private void handleDvrConfigs(HtspMessage message) {
        if (message.containsKey("dvrconfigs")) {
            ServerStatus serverStatus = db.serverStatusDao().loadServerStatusSync();
            for (HtspMessage msg : message.getHtspMessageArray("dvrconfigs")) {
                ServerProfile serverProfile = db.serverProfileDao().loadProfileByUuidSync(msg.getString("uuid"));
                if (serverProfile == null) {
                    serverProfile = new ServerProfile();
                }

                serverProfile.setConnectionId(serverStatus.getConnectionId());
                serverProfile.setUuid(msg.getString("uuid"));
                String name = msg.getString("name");
                serverProfile.setName(TextUtils.isEmpty(name) ? "Default Profile" : name);
                serverProfile.setComment(msg.getString("comment"));
                serverProfile.setType("recording");

                if (serverProfile.getId() == 0) {
                    db.serverProfileDao().insert(serverProfile);
                } else {
                    db.serverProfileDao().update(serverProfile);
                }
            }
        }
    }

    private void handleSystemTime(HtspMessage message) {
        Timber.d("handleSystemTime() called with: message = [" + message + "]");
        ServerStatus serverStatus = db.serverStatusDao().loadServerStatusSync();
        serverStatus.setGmtoffset(message.getInteger("gmtoffset", 0));
        serverStatus.setTime(message.getLong("time", 0));
        db.serverStatusDao().update(serverStatus);
    }

    private void handleDiskSpace(HtspMessage message) {
        Timber.d("handleDiskSpace() called with: message = [" + message + "]");
        ServerStatus serverStatus = db.serverStatusDao().loadServerStatusSync();
        serverStatus.setFreeDiskSpace(message.getLong("freediskspace", 0));
        serverStatus.setTotalDiskSpace(message.getLong("totaldiskspace", 0));
        db.serverStatusDao().update(serverStatus);
    }

    private void handleInitialSyncCompleted() {
        Timber.d("handleInitialSyncCompleted() called");

        Intent intent = new Intent("service_status");
        intent.putExtra("sync_state", State.SAVING);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

        // Flush all received data to the database
        flushPendingChannelOps();
        flushPendingChannelTagRelationOps();

        // Send the information that we are done when the channel list was saved.
        // This is enough to start showing the channel list
        intent = new Intent("service_status");
        intent.putExtra("sync_state", State.DONE);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

        flushPendingDvrEntryOps();
        flushPendingEventOps();

        // Load the channel icons from the received path only if a full sync was requested
        if (initialSyncRequired) {
            flushPendingChannelLogoFetches();
        } else {
            pendingChannelLogoFetches.clear();
        }

        // Get additional information
        getDiscSpace();
        getSystemTime();
        getProfiles();
        getDvrConfigs();

        // The sync is done save the status
        initialSyncCompleted = true;
        initialSyncRequired = false;
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("initial_sync_required", initialSyncRequired);
        editor.apply();
    }

    private void flushPendingChannelOps() {
        if (pendingChannelOps.isEmpty()) {
            return;
        }

        final int steps = 25;
        final int listSize = pendingChannelOps.size();
        int fromIndex = 0;
        while (fromIndex < listSize) {
            int toIndex = (fromIndex + steps >= listSize) ? listSize : fromIndex + steps;
            // Apply the batch only as a sublist of the entire list
            // so we can send out the number of saved operations to any listeners
            db.channelDao().insertAll(new ArrayList<>(pendingChannelOps.subList(fromIndex, toIndex)));
            fromIndex = toIndex;
        }
        pendingChannelOps.clear();
    }

    private void flushPendingChannelTagRelationOps() {
        if (pendingChannelTagRelationOps.isEmpty()) {
            return;
        }
        Timber.d("Adding " + pendingChannelTagRelationOps.size() + " channel tags relations into database");
        db.tagAndChannelDao().insertAll(pendingChannelTagRelationOps);
        pendingChannelTagRelationOps.clear();
    }

    private void flushPendingChannelLogoFetches() {
        if (pendingChannelLogoFetches.isEmpty()) {
            return;
        }

        int index = 0;
        for (String icon : pendingChannelLogoFetches) {
            index++;
            try {
                downloadIconFromFileUrl(icon);
            } catch (Exception e) {
                Timber.d("handleChannelUpdate: Could not load icon '" + icon + "'");
            }
        }
        pendingChannelLogoFetches.clear();
    }

    private void flushPendingDvrEntryOps() {
        if (pendingRecordedProgramOps.isEmpty()) {
            return;
        }

        // Remove all recordings to avoid having outdated ones in
        // the database the are not existent anymore on the server.
        // This could be the case when the app was offline for a while
        // and we could not get any updates of removed recordings fro mthe server
        db.recordingDao().deleteAll();

        final int steps = 25;
        final int listSize = pendingRecordedProgramOps.size();
        int fromIndex = 0;
        while (fromIndex < listSize) {
            int toIndex = (fromIndex + steps >= listSize) ? listSize : fromIndex + steps;
            // Apply the batch only as a sublist of the entire list
            // so we can send out the number of saved operations to any listeners
            db.recordingDao().insertAll(new ArrayList<>(pendingRecordedProgramOps.subList(fromIndex, toIndex)));
            fromIndex = toIndex + 1;
        }
        pendingRecordedProgramOps.clear();
    }

    private void flushPendingEventOps() {
        if (pendingEventOps.isEmpty()) {
            return;
        }

        // Apply the batch of Operations
        final int steps = 50;
        final int listSize = pendingEventOps.size();
        int fromIndex = 0;
        while (fromIndex < listSize) {
            int toIndex = (fromIndex + steps >= listSize) ? listSize : fromIndex + steps;
            // Apply the batch only as a sublist of the entire list
            // so we can send out the number of saved operations to any listeners
            db.programDao().insertAll(new ArrayList<>(pendingEventOps.subList(fromIndex, toIndex)));
            fromIndex = toIndex;

            Timber.d("flushPendingEventOps: Saving program data (" + fromIndex + " of " + listSize + ")");
        }
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
        if (url == null || url.length() == 0) {
            return;
        }
        File file = new File(context.getCacheDir(), MiscUtils.convertUrlToHashString(url) + ".png");
        if (file.exists()) {
            return;
        }

        InputStream is = null;

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
        if (url == null || url.length() == 0) {
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
            db.programDao().insert(program);
        }
    }

    private void getEvents(Intent intent) {
        Timber.d("getEvents() called with: intent = [" + intent + "]");

        final long eventId = intent.getIntExtra("eventId", 0);
        final long channelId = intent.getIntExtra("channelId", 0);
        final long numFollowing = intent.getIntExtra("numFollowing", 0);
        final long maxTime = intent.getIntExtra("maxTime", 0);
        final boolean showMessage = intent.getBooleanExtra("showMessage", false);

        final HtspMessage request = new HtspMessage();
        request.put("method", "getEvents");
        if (eventId > 0) {
            Timber.d("getEvents: adding event id " + eventId);
            request.put("eventId", eventId);
        }
        if (channelId > 0) {
            Timber.d("getEvents: adding channel id " + channelId);
            request.put("channelId", channelId);
        }
        if (numFollowing > 0) {
            Timber.d("getEvents: adding numFollowing " + numFollowing);
            request.put("numFollowing", numFollowing);
        }
        if (maxTime > 0) {
            Timber.d("getEvents: adding maxTime " + maxTime);
            request.put("maxTime", maxTime);
        }

        HtspMessage response = null;
        try {
            if (showMessage) {
                sendStatusMessage(context.getString(R.string.loading_more_programs));
            }
            response = dispatcher.sendMessage(request, connectionTimeout);
        } catch (HtspNotConnectedException e) {
            Timber.e("Failed to send getEvents - not connected", e);
        }

        if (response != null) {
            Timber.d("getEvents: response is not null");
            handleGetEvents(response);
            if (showMessage) {
                sendStatusMessage(context.getString(R.string.loading_more_programs_finished));
            }
        } else {
            Timber.d("getEvents: response is null");
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
        Timber.d("addDvrEntry() called with: intent = [" + intent + "]");

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
                sendStatusMessage(context.getString(R.string.success_adding_recording));
            } else {
                sendStatusMessage(context.getString(R.string.error_adding_recording, response.getString("error", "")));
            }
        }
    }

    private void updateDvrEntry(final Intent intent) {

        HtspMessage request = EpgSyncUtils.convertIntentToDvrMessage(intent, htspVersion);
        request.put("method", "updateDvrEntry");
        request.put("id", intent.getIntExtra("eventId", 0));

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
                sendStatusMessage(context.getString(R.string.success_updating_recording));
            } else {
                sendStatusMessage(context.getString(R.string.error_updating_recording, response.getString("error", "")));
            }
        }
    }

    private void removeDvrEntry(Intent intent) {
        Timber.d("removeDvrEntry() called with: intent action = [" + intent.getAction() + "]");
        final HtspMessage request = new HtspMessage();
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
                sendStatusMessage(context.getString(R.string.success_removing_recording));
            } else {
                sendStatusMessage(context.getString(R.string.error_removing_recording, response.getString("error", "")));
            }
        } else {
            Timber.d("Response is null");
        }
    }

    private void addAutorecEntry(final Intent intent) {

        final HtspMessage request = EpgSyncUtils.convertIntentToAutorecMessage(intent, htspVersion);
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
                sendStatusMessage(context.getString(R.string.success_adding_recording));
            } else {
                sendStatusMessage(context.getString(R.string.error_adding_recording, response.getString("error", "")));
            }
        }
    }

    private void updateAutorecEntry(final Intent intent) {
        Timber.d("updateAutorecEntry() called with: intent = [" + intent + "]");

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
                    sendStatusMessage(context.getString(R.string.success_updating_recording));
                } else {
                    sendStatusMessage(context.getString(R.string.error_updating_recording, response.getString("error", "")));
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
                sendStatusMessage(context.getString(R.string.success_removing_recording));
            } else {
                sendStatusMessage(context.getString(R.string.error_removing_recording, response.getString("error", "")));
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
                sendStatusMessage(context.getString(R.string.success_adding_recording));
            } else {
                sendStatusMessage(context.getString(R.string.error_adding_recording, response.getString("error", "")));
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
                    sendStatusMessage(context.getString(R.string.success_updating_recording));
                } else {
                    sendStatusMessage(context.getString(R.string.error_updating_recording, response.getString("error", "")));
                }
            }
        }
    }

    private void deleteTimerrecEntry(Intent intent) {
        final HtspMessage request = new HtspMessage();
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
                sendStatusMessage(context.getString(R.string.success_removing_recording));
            } else {
                sendStatusMessage(context.getString(R.string.error_removing_recording, response.getString("error", "")));
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
        final HtspMessage request = new HtspMessage();
        request.put("method", "getProfiles");

        HtspMessage response = null;
        try {
            response = dispatcher.sendMessage(request, connectionTimeout);
        } catch (HtspNotConnectedException e) {
            Timber.e("Failed to send getProfiles - not connected", e);
        }

        if (response != null) {
            handleProfiles(response);
        }
    }

    private void getDvrConfigs() {
        final HtspMessage request = new HtspMessage();
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

    private void getMoreEvents() {
        Timber.d("getMoreEvents() called");
        List<Channel> channelList = db.channelDao().loadAllChannelsSync(0);
        for (Channel channel : channelList) {
            Program program = db.programDao().loadLastProgramFromChannelSync(channel.getId());
            if (program != null && program.getEventId() > 0) {
                Timber.d("getMoreEvents: loading more events for channel " + channel.getName());
                Intent intent = new Intent();
                intent.putExtra("eventId", program.getNextEventId());
                intent.putExtra("channelId", channel.getId());
                //int maxTime = Integer.valueOf(sharedPreferences.getString("epg_hours_to_fetch", "4"));
                //intent.putExtra("maxTime", maxTime);
                intent.putExtra("numFollowing", 25);
                getEvents(intent);
            }
        }
    }

    private void deleteEvents() {
        Timber.d("deleteEvents() called");

        // Get the time that was one week before now
        int days = Integer.valueOf(sharedPreferences.getString("epg_days_before_removal", "7"));
        long time = new Date().getTime() - (days * 24 * 60 * 60 * 1000);

        List<Channel> channelList = db.channelDao().loadAllChannelsSync(0);
        for (Channel channel : channelList) {
            db.programDao().deleteOldProgramsByChannel(channel.getId(), time);
        }
    }

    private void getStatus() {
        Timber.d("getStatus() called");

        HtspMessage message = new HtspMessage();
        message.put("method", "hello");
        message.put("htspversion", 26);
        message.put("clientname", "TVHClient");
        message.put("clientversion", BuildConfig.VERSION_NAME);

        boolean connectedToServer = false;
        HtspMessage response = null;
        try {
            response = dispatcher.sendMessage(message, connectionTimeout);
        } catch (HtspNotConnectedException e) {
            connectedToServer = false;
        }

        if (response != null) {
            connectedToServer = true;
        }

        Timber.d("connectedToServer is " + connectedToServer);
        Intent intent = new Intent("service_status");
        intent.putExtra("sync_state", connectedToServer ? State.DONE : State.RECONNECT);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    private void sendStatusMessage(String msg) {
        Intent intent = new Intent("message");
        intent.putExtra("message", msg);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }
}