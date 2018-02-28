package org.tvheadend.tvhclient.service;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.AppDatabase;
import org.tvheadend.tvhclient.data.entity.Channel;
import org.tvheadend.tvhclient.data.entity.ChannelTag;
import org.tvheadend.tvhclient.data.entity.Program;
import org.tvheadend.tvhclient.data.entity.Recording;
import org.tvheadend.tvhclient.data.entity.SeriesRecording;
import org.tvheadend.tvhclient.data.entity.ServerProfile;
import org.tvheadend.tvhclient.data.entity.ServerStatus;
import org.tvheadend.tvhclient.data.entity.TagAndChannel;
import org.tvheadend.tvhclient.data.entity.TimerRecording;
import org.tvheadend.tvhclient.service.htsp.HtspFileInputStream;
import org.tvheadend.tvhclient.service.htsp.HtspMessage;
import org.tvheadend.tvhclient.service.htsp.HtspNotConnectedException;
import org.tvheadend.tvhclient.service.htsp.tasks.Authenticator;
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

public class EpgSyncTask implements HtspMessage.Listener, Authenticator.Listener {
    private static final String TAG = EpgSyncTask.class.getSimpleName();

    private final AppDatabase db;
    private final int connectionTimeout;
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

    EpgSyncTask(Context context, @NonNull HtspMessage.Dispatcher dispatcher) {
        this.context = context;
        this.dispatcher = dispatcher;
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        this.db = AppDatabase.getInstance(context.getApplicationContext());
        HandlerThread handlerThread = new HandlerThread("EpgSyncTask Handler Thread");
        handlerThread.start();
        this.handler = new Handler(handlerThread.getLooper());
        this.connectionTimeout = Integer.valueOf(sharedPreferences.getString("connectionTimeout", "5000"));
        this.htspVersion = 9;
    }

    // Authenticator.Listener Methods
    @Override
    public void onAuthenticationStateChange(@NonNull Authenticator.State state) {
        // Send the authentication status to any broadcast listeners
        Intent intent = new Intent("service_status");
        intent.putExtra("authentication_status", state);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

        // Continue with processing like getting all
        // initial data only if we are authenticated
        if (state == Authenticator.State.AUTHENTICATED) {
            initialSyncCompleted = false;
            initialSyncRequired = sharedPreferences.getBoolean("initial_sync_required", true);

            if (initialSyncRequired) {
                startFullInitialSyncWithServer();
            } else {
                startInitialSyncWithServer();
            }
        }
    }

    private void startFullInitialSyncWithServer() {
        Log.d(TAG, "startFullInitialSyncWithServer() called");

        // Send the first sync message to any broadcast listeners
        Intent intent = new Intent("service_status");
        intent.putExtra("sync_status", "Loading initial data...");
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

        // Enable epg sync with the defined number of
        // seconds of data starting from the current time
        HtspMessage enableAsyncMetadataRequest = new HtspMessage();
        enableAsyncMetadataRequest.put("method", "enableAsyncMetadata");
        enableAsyncMetadataRequest.put("epg", 1);
        long epgMaxTime = (3600) + (System.currentTimeMillis() / 1000L);
        enableAsyncMetadataRequest.put("epgMaxTime", epgMaxTime);

        // Only provide metadata that has changed since this time.
        // Whenever the message eventUpdate is received from the server
        // the current time will be stored in the preferences.
        final long lastUpdate = sharedPreferences.getLong("last_update", 0);
        enableAsyncMetadataRequest.put("lastUpdate", lastUpdate);

        try {
            dispatcher.sendMessage(enableAsyncMetadataRequest);
        } catch (HtspNotConnectedException e) {
            Log.d(TAG, "Failed to enable async metadata, HTSP not connected", e);
        }
    }

    private void startInitialSyncWithServer() {
        Log.d(TAG, "startInitialSyncWithServer() called");

        // Send a broadcast to the listeners
        Intent intent = new Intent("service_status");
        intent.putExtra("sync_status", "done");
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

        HtspMessage enableAsyncMetadataRequest = new HtspMessage();
        enableAsyncMetadataRequest.put("method", "enableAsyncMetadata");
        try {
            dispatcher.sendMessage(enableAsyncMetadataRequest);
        } catch (HtspNotConnectedException e) {
            Log.d(TAG, "Failed to enable async metadata, HTSP not connected", e);
        }
    }

    // HtspMessage.Listener Methods
    @Override
    public Handler getHandler() {
        return handler;
    }

    @Override
    public void onMessage(@NonNull HtspMessage message) {
        final String method = message.getString("method");
        Log.d(TAG, "onMessage() called with: message = [" + method + "]");
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
            Log.d(TAG, "getEvents: contains key events");
            for (HtspMessage msg : message.getHtspMessageArray("events")) {
                List<Program> programList = new ArrayList<>();
                programList.add(EpgSyncUtils.convertMessageToProgramModel(new Program(), msg));
                db.programDao().insertAll(programList);
            }
            flushPendingEventOps();
        }
    }

    void handleIntent(Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }
        Log.d(TAG, "handleIntent() called with: intent = [" + intent.getAction() + "]");
        switch (intent.getAction()) {
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
        Log.d(TAG, "handleInitialServerResponse() called with: response = [" + response + "]");
        // Get the values from the database to have access
        // to them later without querying the db too often
        htspVersion = response.getInteger("htspversion", 9);

        ServerStatus serverStatus = db.serverStatusDao().loadServerStatusSync();
        serverStatus.setHtspVersion(htspVersion);
        serverStatus.setServerName(response.getString("servername"));
        serverStatus.setServerVersion(response.getString("serverversion"));
        serverStatus.setWebroot(response.getString("webroot"));
        db.serverStatusDao().update(serverStatus);
    }

    /**
     * Server to client method.
     * A tag has been added on the server.
     *
     * @param msg The message with the new tag data
     */
    private void handleTagAdd(HtspMessage msg) {
        Log.d(TAG, "handleTagAdd() called with: msg = [" + msg + "]");
        ChannelTag tag = EpgSyncUtils.convertMessageToChannelTagModel(new ChannelTag(), msg);
        db.channelTagDao().insert(tag);

        // Get the tag id and all channel ids of the tag so that
        // new entries where the tagId is present can be added to the database
        List<Integer> channelIds = tag.getMembers();
        if (channelIds != null) {
            for (Integer channelId : channelIds) {
                TagAndChannel tagAndChannel = new TagAndChannel();
                tagAndChannel.setTagId(tag.getTagId());
                tagAndChannel.setChannelId(channelId);
                pendingChannelTagRelationOps.add(tagAndChannel);
                //db.tagAndChannelDao().insert(tagAndChannel);
            }
        }

        // Update the icon if required
        final String icon = msg.getString("tagIcon", null);
        if (icon != null) {
            try {
                downloadIconFromFileUrl(icon);
            } catch (Exception e) {
                Log.d(TAG, "handleTagAdd: Could not load icon '" + icon + "'");
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
        ChannelTag tag = EpgSyncUtils.convertMessageToChannelTagModel(new ChannelTag(), msg);
        db.channelTagDao().update(tag);

        // Remove all entries of this tag from the database before
        // adding new ones which are defined in the members variable
        db.tagAndChannelDao().deleteByTagId(tag.getTagId());
        List<Integer> channelIds = tag.getMembers();
        if (channelIds != null) {
            for (Integer channelId : channelIds) {
                TagAndChannel tagAndChannel = new TagAndChannel();
                tagAndChannel.setTagId(tag.getTagId());
                tagAndChannel.setChannelId(channelId);
                pendingChannelTagRelationOps.add(tagAndChannel);
                //db.tagAndChannelDao().insert(tagAndChannel);
            }
        }

        // Update the icon if required
        final String icon = msg.getString("tagIcon");
        if (icon != null) {
            try {
                downloadIconFromFileUrl(icon);
            } catch (Exception e) {
                Log.d(TAG, "handleTagUpdate: Could not load icon '" + icon + "'");
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
        ChannelTag tag = db.channelTagDao().loadChannelTagByIdSync(msg.getInteger("tagId"));
        deleteIconFileFromCache(tag.getTagIcon());
        db.channelTagDao().delete(tag);
        db.tagAndChannelDao().deleteByTagId(tag.getTagId());
    }

    /**
     * Server to client method.
     * A channel has been added on the server.
     *
     * @param msg The message with the new channel data
     */
    private void handleChannelAdd(HtspMessage msg) {
        Channel channel = EpgSyncUtils.convertMessageToChannelModel(new Channel(), msg);
        if (!initialSyncCompleted) {
            if (pendingChannelOps.isEmpty()) {
                Intent intent = new Intent("service_status");
                intent.putExtra("sync_status", "Receiving channels...");
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
            }
            pendingChannelOps.add(channel);
        } else {
            db.channelDao().insert(channel);
        }

        // Get the tag id and all channel ids of the tag so that
        // new entries where the tagId is present can be added to the database
        List<Integer> tagIds = channel.getTags();
        if (tagIds != null) {
            for (Integer tagId : tagIds) {
                TagAndChannel tagAndChannel = new TagAndChannel();
                tagAndChannel.setTagId(tagId);
                tagAndChannel.setChannelId(channel.getChannelId());
                //db.tagAndChannelDao().insert(tagAndChannel);
            }
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
        Channel channel = EpgSyncUtils.convertMessageToChannelModel(new Channel(), msg);
        if (!initialSyncCompleted) {
            pendingChannelOps.add(channel);
        } else {
            db.channelDao().update(channel);
        }

        // Get the tag id and all channel ids of the tag so that
        // new entries where the tagId is present can be added to the database
        List<Integer> tagIds = channel.getTags();
        if (tagIds != null) {
            for (Integer tagId : tagIds) {
                TagAndChannel tagAndChannel = new TagAndChannel();
                tagAndChannel.setTagId(tagId);
                tagAndChannel.setChannelId(channel.getChannelId());
                //db.tagAndChannelDao().insert(tagAndChannel);
            }
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
        Channel channel = db.channelDao().loadChannelByIdSync(msg.getInteger("channelId"));
        deleteIconFileFromCache(channel.getChannelIcon());
        db.channelDao().delete(channel);
    }

    /**
     * Server to client method.
     * A recording has been added on the server.
     *
     * @param msg The message with the new recording data
     */
    private void handleDvrEntryAdd(HtspMessage msg) {
        Recording recording = EpgSyncUtils.convertMessageToRecordingModel(new Recording(), msg);
        if (!initialSyncCompleted) {
            if (pendingRecordedProgramOps.isEmpty()) {
                Intent intent = new Intent("service_status");
                intent.putExtra("sync_status", "Receiving recordings...");
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
        Recording[] recordings = new Recording[1];
        recordings[0] = EpgSyncUtils.convertMessageToRecordingModel(new Recording(), msg);
        if (!initialSyncCompleted) {
            pendingRecordedProgramOps.add(recordings[0]);
        } else {
            db.recordingDao().update(recordings[0]);
        }
    }

    /**
     * Server to client method.
     * A recording has been deleted on the server.
     *
     * @param msg The message with the recording id that was deleted
     */
    private void handleDvrEntryDelete(HtspMessage msg) {
        Recording recording = db.recordingDao().loadRecordingByIdSync(msg.getInteger("id"));
        db.recordingDao().delete(recording);
    }

    /**
     * Server to client method.
     * A series recording has been added on the server.
     *
     * @param msg The message with the new series recording data
     */
    private void handleAutorecEntryAdd(HtspMessage msg) {
        SeriesRecording seriesRecording = EpgSyncUtils.convertMessageToSeriesRecordingModel(new SeriesRecording(), msg);
        db.seriesRecordingDao().insert(seriesRecording);
    }

    /**
     * Server to client method.
     * A series recording has been updated on the server.
     *
     * @param msg The message with the updated series recording data
     */
    private void handleAutorecEntryUpdate(HtspMessage msg) {
        SeriesRecording[] seriesRecordings = new SeriesRecording[1];
        seriesRecordings[0] = EpgSyncUtils.convertMessageToSeriesRecordingModel(new SeriesRecording(), msg);
        db.seriesRecordingDao().update(seriesRecordings[0]);
    }

    /**
     * Server to client method.
     * A series recording has been deleted on the server.
     *
     * @param msg The message with the series recording id that was deleted
     */
    private void handleAutorecEntryDelete(HtspMessage msg) {
        db.seriesRecordingDao().deleteById(msg.getString("id"));
    }

    /**
     * Server to client method.
     * A timer recording has been added on the server.
     *
     * @param msg The message with the new timer recording data
     */
    private void handleTimerRecEntryAdd(HtspMessage msg) {
        TimerRecording recording = EpgSyncUtils.convertMessageToTimerRecordingModel(new TimerRecording(), msg);
        db.timerRecordingDao().insert(recording);
    }

    /**
     * Server to client method.
     * A timer recording has been updated on the server.
     *
     * @param msg The message with the updated timer recording data
     */
    private void handleTimerRecEntryUpdate(HtspMessage msg) {
        TimerRecording[] timerRecordings = new TimerRecording[1];
        timerRecordings[0] = EpgSyncUtils.convertMessageToTimerRecordingModel(new TimerRecording(), msg);
        db.timerRecordingDao().update(timerRecordings[0]);
    }

    /**
     * Server to client method.
     * A timer recording has been deleted on the server.
     *
     * @param msg The message with the recording id that was deleted
     */
    private void handleTimerRecEntryDelete(HtspMessage msg) {
        db.timerRecordingDao().deleteById(msg.getString("id"));
    }

    /**
     * Server to client method.
     * An epg event has been added on the server.
     *
     * @param msg The message with the new epg event data
     */
    private void handleEventAdd(HtspMessage msg) {
        Program program = EpgSyncUtils.convertMessageToProgramModel(new Program(), msg);
        if (!initialSyncCompleted) {
            if (pendingEventOps.isEmpty()) {
                Intent intent = new Intent("service_status");
                intent.putExtra("sync_status", "Receiving event data...");
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
            }
            pendingEventOps.add(program);
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
        Program[] program = new Program[1];
        program[0] = EpgSyncUtils.convertMessageToProgramModel(new Program(), msg);
        if (!initialSyncCompleted) {
            pendingEventOps.add(program[0]);
        } else {
            db.programDao().update(program[0]);
        }
    }

    /**
     * Server to client method.
     * An epg event has been deleted on the server.
     *
     * @param msg The message with the epg event id that was deleted
     */
    private void handleEventDelete(HtspMessage msg) {
        db.programDao().deleteById(msg.getInteger("id"));
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
        Log.d(TAG, "handleSystemTime() called with: message = [" + message + "]");
        ServerStatus serverStatus = db.serverStatusDao().loadServerStatusSync();
        serverStatus.setGmtoffset(message.getInteger("gmtoffset", 0));
        serverStatus.setTime(message.getLong("time", 0));
        db.serverStatusDao().update(serverStatus);
    }

    private void handleInitialSyncCompleted() {
        Log.d(TAG, "handleInitialSyncCompleted() called");

        // Flush all received data to the database
        flushPendingChannelOps();
        flushPendingChannelTagRelationOps();
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

        // The full initial sync is done, send the info to exit the start screen
        if (initialSyncRequired) {
            Intent intent = new Intent("service_status");
            intent.putExtra("sync_status", "done");
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
        }

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

        Intent intent = new Intent("service_status");
        intent.putExtra("sync_status", "saving channels");
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

        final int steps = 25;
        final int listSize = pendingChannelOps.size();
        int fromIndex = 0;
        while (fromIndex < listSize) {
            int toIndex = (fromIndex + steps >= listSize) ? listSize : fromIndex + steps;
            // Apply the batch only as a sublist of the entire list
            // so we can send out the number of saved operations to any listeners
            db.channelDao().insertAll(new ArrayList<>(pendingChannelOps.subList(fromIndex, toIndex)));
            fromIndex = toIndex;

            intent.putExtra("sync_status", "saving channels (" + fromIndex + " of " + listSize + ")");
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
        }
        pendingChannelOps.clear();
    }

    private void flushPendingChannelTagRelationOps() {
        if (pendingChannelTagRelationOps.isEmpty()) {
            return;
        }
        db.tagAndChannelDao().insertAll(pendingChannelTagRelationOps);
        pendingChannelTagRelationOps.clear();
    }

    private void flushPendingChannelLogoFetches() {
        if (pendingChannelLogoFetches.isEmpty()) {
            return;
        }

        Intent intent = new Intent("service_status");
        intent.putExtra("sync_status", "saving channel logos");
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

        int index = 0;
        for (String icon : pendingChannelLogoFetches) {
            index++;
            intent.putExtra("sync_status", "saving channel logo (" + index + " of " + pendingChannelLogoFetches.size());
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
            try {
                downloadIconFromFileUrl(icon);
            } catch (Exception e) {
                Log.d(TAG, "handleChannelUpdate: Could not load icon '" + icon + "'");
            }
        }
        pendingChannelLogoFetches.clear();
    }

    private void flushPendingDvrEntryOps() {
        if (pendingRecordedProgramOps.isEmpty()) {
            return;
        }

        Intent intent = new Intent("service_status");
        intent.putExtra("sync_status", "saving recordings");
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

        final int steps = 25;
        final int listSize = pendingRecordedProgramOps.size();
        int fromIndex = 0;
        while (fromIndex < listSize) {
            int toIndex = (fromIndex + steps >= listSize) ? listSize : fromIndex + steps;
            // Apply the batch only as a sublist of the entire list
            // so we can send out the number of saved operations to any listeners
            db.recordingDao().insertAll(new ArrayList<>(pendingRecordedProgramOps.subList(fromIndex, toIndex)));
            fromIndex = toIndex + 1;

            intent.putExtra("sync_status", "saving recordings (" + fromIndex + " of " + listSize + ")");
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
        }
        pendingRecordedProgramOps.clear();
    }

    private void flushPendingEventOps() {
        if (pendingEventOps.isEmpty()) {
            return;
        }

        Intent intent = new Intent("service_status");
        intent.putExtra("sync_status", "saving epg data");
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

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

            intent.putExtra("sync_status", "saving epg data (" + fromIndex + " of " + listSize + ")");
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
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
    private void downloadIconFromFileUrl(final String url) throws IOException {
        if (url == null || url.length() == 0) {
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

        float scale = context.getResources().getDisplayMetrics().density;
        int width = (int) (64 * scale);
        int height = (int) (64 * scale);

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
     * @return True if removal was successful, false otherwise
     */
    private boolean deleteIconFileFromCache(String url) {
        if (url == null || url.length() == 0) {
            return false;
        }
        File file = new File(context.getCacheDir(), MiscUtils.convertUrlToHashString(url) + ".png");
        return file.exists() && file.delete();
    }

    private void getDiscSpace() {
        Log.d(TAG, "getDiscSpace() called");
        HtspMessage request = new HtspMessage();
        request.put("method", "getDiskSpace");

        HtspMessage response = null;
        try {
            response = dispatcher.sendMessage(request, connectionTimeout);
        } catch (HtspNotConnectedException e) {
            Log.e(TAG, "Failed to send getDiskSpace - not connected", e);
        }

        if (response != null) {
            Log.d(TAG, "getDiscSpace: got response");
            ServerStatus serverStatus = db.serverStatusDao().loadServerStatusSync();
            serverStatus.setFreeDiskSpace(response.getLong("freediskspace", 0));
            serverStatus.setTotalDiskSpace(response.getLong("totaldiskspace", 0));
            db.serverStatusDao().update(serverStatus);
        }
    }

    private void getSystemTime() {
        Log.d(TAG, "getSystemTime() called");
        final HtspMessage request = new HtspMessage();
        request.put("method", "getSysTime");

        try {
            dispatcher.sendMessage(request);
        } catch (HtspNotConnectedException e) {
            Log.e(TAG, "Failed to send getSysTime - not connected", e);
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
            Log.e(TAG, "Failed to send getChannel - not connected", e);
        }

        if (response != null) {
            // Update the icon if required
            final String icon = response.getString("channelIcon", null);
            if (icon != null) {
                try {
                    downloadIconFromFileUrl(icon);
                } catch (Exception e) {
                    Log.d(TAG, "handleChannelUpdate: Could not load icon '" + icon + "'");
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
            Log.e(TAG, "Failed to send getEvent - not connected", e);
        }

        if (response != null) {
            Program program = EpgSyncUtils.convertMessageToProgramModel(new Program(), response);
            db.programDao().insert(program);
        }
    }

    private void getEvents(Intent intent) {
        Log.d(TAG, "getEvents() called with: intent = [" + intent + "]");

        final long eventId = intent.getIntExtra("eventId", 0);
        final long channelId = intent.getIntExtra("channelId", 0);
        final long numFollowing = intent.getIntExtra("numFollowing", 0);

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

        try {
            dispatcher.sendMessage(request, connectionTimeout);
        } catch (HtspNotConnectedException e) {
            Log.e(TAG, "Failed to send getEvents - not connected", e);
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
            Log.e(TAG, "Failed to send epgQuery - not connected", e);
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
            if (eventIdList.size() > 0) {
                // TODO send the list to any broadcast listeners
            }
        }
    }

    private void addDvrEntry(final Intent intent) {
        Log.d(TAG, "addDvrEntry() called with: intent = [" + intent + "]");

        HtspMessage request = EpgSyncUtils.convertIntentToDvrMessage(intent, htspVersion);
        request.put("method", "addDvrEntry");

        HtspMessage response = null;
        try {
            response = dispatcher.sendMessage(request, connectionTimeout);
        } catch (HtspNotConnectedException e) {
            Log.e(TAG, "Failed to send addDvrEntry - not connected", e);
        }

        if (response != null) {
            // Reply message fields:
            // success            u32   required   1 if entry was added, 0 otherwise
            // id                 u32   optional   ID of created DVR entry
            // error              str   optional   English clear text of error message
            if (response.getInteger("success", 0) == 1) {
                sendMessage(context.getString(R.string.success_adding_recording));
            } else {
                sendMessage(context.getString(R.string.error_adding_recording, response.getString("error", "")));
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
            Log.e(TAG, "Failed to send updateDvrEntry - not connected", e);
        }

        if (response != null) {
            // Reply message fields:
            // success            u32   required   1 if update as successful, otherwise 0
            // error              str   optional   Error message if update failed
            if (response.getInteger("success", 0) == 1) {
                sendMessage(context.getString(R.string.success_updating_recording));
            } else {
                sendMessage(context.getString(R.string.error_updating_recording, response.getString("error", "")));
            }
        }
    }

    private void removeDvrEntry(Intent intent) {
        Log.d(TAG, "removeDvrEntry() called with: intent action = [" + intent.getAction() + "]");
        final HtspMessage request = new HtspMessage();
        request.put("method", intent.getAction());
        request.put("id", intent.getIntExtra("id", 0));

        Log.d(TAG, "removeDvrEntry: event id " + intent.getIntExtra("id", 0));
        HtspMessage response = null;
        try {
            response = dispatcher.sendMessage(request, connectionTimeout);
        } catch (HtspNotConnectedException e) {
            Log.e(TAG, "Failed to send " + intent.getAction() + " - not connected", e);
        }

        if (response != null) {
            if (response.getInteger("success", 0) == 1) {
                sendMessage(context.getString(R.string.success_removing_recording));
            } else {
                sendMessage(context.getString(R.string.error_removing_recording, response.getString("error", "")));
            }
        }
    }

    private void addAutorecEntry(final Intent intent) {

        final HtspMessage request = EpgSyncUtils.convertIntentToAutorecMessage(intent, htspVersion);
        request.put("method", "addAutorecEntry");

        HtspMessage response = null;
        try {
            response = dispatcher.sendMessage(request, connectionTimeout);
        } catch (HtspNotConnectedException e) {
            Log.e(TAG, "Failed to send addAutorecEntry - not connected", e);
        }

        if (response != null) {
            // Reply message fields:
            // success            u32   required   1 if entry was added, 0 otherwise
            // id                 str   optional   ID (string!) of created autorec DVR entry
            // error              str   optional   English clear text of error message
            if (response.getInteger("success", 0) == 1) {
                sendMessage(context.getString(R.string.success_adding_recording));
            } else {
                sendMessage(context.getString(R.string.error_adding_recording, response.getString("error", "")));
            }
        }
    }

    private void updateAutorecEntry(final Intent intent) {
        Log.d(TAG, "updateAutorecEntry() called with: intent = [" + intent + "]");

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
            Log.e(TAG, "Failed to send updateAutorecEntry - not connected", e);
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
                    sendMessage(context.getString(R.string.success_updating_recording));
                } else {
                    sendMessage(context.getString(R.string.error_updating_recording, response.getString("error", "")));
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
            Log.e(TAG, "Failed to send deleteAutorecEntry - not connected", e);
        }

        if (response != null) {
            if (response.getInteger("success", 0) == 1) {
                sendMessage(context.getString(R.string.success_removing_recording));
            } else {
                sendMessage(context.getString(R.string.error_removing_recording, response.getString("error", "")));
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
            Log.e(TAG, "Failed to send addTimerecEntry - not connected", e);
        }

        if (response != null) {
            if (response.getInteger("success", 0) == 1) {
                sendMessage(context.getString(R.string.success_adding_recording));
            } else {
                sendMessage(context.getString(R.string.error_adding_recording, response.getString("error", "")));
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
            Log.e(TAG, "Failed to send updateTimerecEntry - not connected", e);
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
                    sendMessage(context.getString(R.string.success_updating_recording));
                } else {
                    sendMessage(context.getString(R.string.error_updating_recording, response.getString("error", "")));
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
            Log.e(TAG, "Failed to send deleteTimerecEntry - not connected", e);
        }

        if (response != null) {
            if (response.getInteger("success", 0) == 1) {
                sendMessage(context.getString(R.string.success_removing_recording));
            } else {
                sendMessage(context.getString(R.string.error_removing_recording, response.getString("error", "")));
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
            Log.e(TAG, "Failed to send getTicket - not connected", e);
        }

        if (response != null) {
            String path = response.getString("path");
            String ticket = response.getString("ticket");
            // TODO create a HttpTicket model or use broadcast
        }
    }

    private void getProfiles() {
        final HtspMessage request = new HtspMessage();
        request.put("method", "getProfiles");
        try {
            dispatcher.sendMessage(request);
        } catch (HtspNotConnectedException e) {
            Log.e(TAG, "Failed to send getProfiles - not connected", e);
        }
    }

    private void getDvrConfigs() {
        final HtspMessage request = new HtspMessage();
        request.put("method", "getDvrConfigs");
        try {
            dispatcher.sendMessage(request, connectionTimeout);
        } catch (HtspNotConnectedException e) {
            Log.e(TAG, "Failed to send getDvrConfigs - not connected", e);
        }
    }

    private void sendMessage(String text) {
        Log.d(TAG, "sendMessage() called with: text = [" + text + "]");
        Intent intent = new Intent("message");
        intent.putExtra("message", text);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    private void getMoreEvents() {
        Log.d(TAG, "getMoreEvents() called");

        List<Channel> channelList = db.channelDao().loadAllChannelsSync();
        for (Channel channel : channelList) {
            Log.d(TAG, "getMoreEvents: getting last program for channel " + channel.getChannelName());
            Program program = db.programDao().loadLastProgramFromChannelSync(channel.getChannelId());
            if (program != null && program.getEventId() > 0) {
                Log.d(TAG, "getMoreEvents: getEvents for program " + program.getTitle());
                Intent intent = new Intent();
                intent.putExtra("eventId", program.getEventId());
                intent.putExtra("channelId", channel.getChannelId());

                // TODO make the value a preference
                intent.putExtra("numFollowing", 25);
                getEvents(intent);
            }
        }
    }

    private void deleteEvents() {
        Log.d(TAG, "deleteEvents() called");

        // Get the time that was one week before now
        // TODO make the value a preference
        long time = new Date().getTime() - 7 * 24 * 60 * 60 * 1000;

        List<Channel> channelList = db.channelDao().loadAllChannelsSync();
        for (Channel channel : channelList) {
            Log.d(TAG, "getMoreEvents: removing programs older than " + time);
            db.programDao().deleteOldProgramsByChannel(channel.getChannelId(), time);
        }
    }
}