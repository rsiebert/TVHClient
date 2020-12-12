package org.tvheadend.tvhclient.service.http

import android.app.Service
import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import okhttp3.Request
import org.json.JSONException
import org.json.JSONObject
import org.tvheadend.api.*
import org.tvheadend.data.AppRepository
import org.tvheadend.data.entity.*
import org.tvheadend.http.HttpConnection
import org.tvheadend.http.HttpConnectionData
import org.tvheadend.tvhclient.BuildConfig
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.service.ConnectionService
import org.tvheadend.tvhclient.service.SyncState
import org.tvheadend.tvhclient.service.SyncStateReceiver
import org.tvheadend.tvhclient.service.SyncStateResult
import org.tvheadend.tvhclient.service.htsp.convertMessageToChannelTagModel
import timber.log.Timber
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService


class HttpServiceHandler(val context: Context, val appRepository: AppRepository, val connection: Connection) : ConnectionService.ServiceInterface, ServerConnectionStateListener, ServerMessageListener<JSONObject>, ServerRequestInterface {

    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    private var httpConnectionData: HttpConnectionData
    private var htspVersion: Int = 13
    private var httpConnection: HttpConnection? = null
    private val execService: ScheduledExecutorService = Executors.newScheduledThreadPool(10)

    private val pendingEventOps = ArrayList<Program>()
    private val pendingChannelOps = ArrayList<Channel>()
    private val pendingChannelTagOps = ArrayList<ChannelTag>()
    private val pendingRecordingOps = ArrayList<Recording>()

    private var pendingHtspProfiles: MutableList<ServerProfile>
    private var pendingHttpProfiles: MutableList<ServerProfile>
    private var pendingRecordingProfiles: MutableList<ServerProfile>

    private var initialSyncWithServerRunning: Boolean = false
    private var syncEventsRequired: Boolean = false
    private var syncRequired: Boolean = false
    private var firstEventReceived = false

    init {
        httpConnectionData = HttpConnectionData(
                connection.username,
                connection.password,
                connection.streamingUrl,
                BuildConfig.VERSION_NAME,
                BuildConfig.VERSION_CODE,
                Integer.valueOf(sharedPreferences.getString("connection_timeout", context.resources.getString(R.string.pref_default_connection_timeout))!!) * 1000
        )

        pendingHtspProfiles = appRepository.serverProfileData.htspPlaybackProfiles.toMutableList()
        Timber.d("Loaded ${pendingHtspProfiles.size} htsp profiles")

        pendingHttpProfiles = appRepository.serverProfileData.httpPlaybackProfiles.toMutableList()
        Timber.d("Loaded ${pendingHttpProfiles.size} http profiles")

        pendingRecordingProfiles = appRepository.serverProfileData.recordingProfiles.toMutableList()
        Timber.d("Loaded ${pendingRecordingProfiles.size} recording profiles")
    }

    override fun onStartCommand(intent: Intent?): Int {
        val action = intent?.action ?: return Service.START_NOT_STICKY
        if (action.isEmpty()) {
            return Service.START_NOT_STICKY
        }
        Timber.d("Received command $action for service")

        when (action) {
            "connect" -> {
                Timber.d("Connection to server requested")
                startHttpConnection()
            }
            "reconnect" -> {
                Timber.d("Reconnection to server requested")
                when {
                    httpConnection?.isConnecting == true -> {
                        Timber.d("Not reconnecting to server because we are currently connecting")
                    }
                    httpConnection?.isNotConnected == false -> {
                        Timber.d("Not reconnecting to server because we are still connected")
                    }
                    else -> {
                        Timber.d("Reconnecting to server because we are not connected anymore")
                        startHttpConnection()
                    }
                }
            }
            else -> {
                httpConnection?.let {
                    if (!it.isNotConnected && it.isAuthenticated) {
                        Timber.d("Connection to server exists, executing action $action")
                        when (action) {
                            "getDiskSpace" -> getDiscSpace()
                            "getSysTime" -> getSystemTime()
                            "getChannel" -> getChannel(intent)
                            "getEvent" -> getEvent(intent)
                            "getEvents" -> getEvents(intent)
                            "epgQuery" -> getEpgQuery(intent)
                            "addDvrEntry" -> addDvrEntry(intent)
                            "updateDvrEntry" -> updateDvrEntry(intent)
                            "cancelDvrEntry" -> cancelDvrEntry(intent)
                            "deleteDvrEntry" -> deleteDvrEntry(intent)
                            "stopDvrEntry" -> stopDvrEntry(intent)
                            "addAutorecEntry" -> addAutorecEntry(intent)
                            "updateAutorecEntry" -> updateAutorecEntry(intent)
                            "deleteAutorecEntry" -> deleteAutorecEntry(intent)
                            "addTimerecEntry" -> addTimerrecEntry(intent)
                            "updateTimerecEntry" -> updateTimerrecEntry(intent)
                            "deleteTimerecEntry" -> deleteTimerrecEntry(intent)
                            "getTicket" -> getTicket(intent)
                            "getProfiles" -> getProfiles()
                            "getDvrConfigs" -> getDvrConfigs()
                            "getSubscriptions" -> getSubscriptions()
                            "getInputs" -> getInputs()
                            // Internal calls that are called from the intent service
                            "getMoreEvents" -> getMoreEvents(intent)
                            "loadChannelIcons" -> {
                                // TODO loadAllChannelIcons()
                                // TODO loadAllChannelTagIcons()
                            }
                        }
                    } else {
                        Timber.d("Not connected to server, not executing action $action")
                    }
                }
            }
        }
        return Service.START_NOT_STICKY
    }

    override fun onDestroy() {
        Timber.d("Stopping service")
        execService.shutdown()
        httpConnection?.closeConnection()
    }

    private fun startHttpConnection() {
        httpConnection?.closeConnection()
        Timber.d("Connecting to ${connection.name}, serverUrl is ${connection.streamingUrl}")
        httpConnection = HttpConnection(httpConnectionData, this, this)
        // Since this is blocking, spawn to a new thread
        execService.execute {
            httpConnection?.openConnection()
            httpConnection?.authenticate()
        }
    }

    override fun onAuthenticationStateChange(result: AuthenticationStateResult) {
        SyncStateResult.Authenticating(result)
        if (result is AuthenticationStateResult.Authenticated) {
            startAsyncCommunicationWithServer()
        }
    }

    override fun onConnectionStateChange(result: ConnectionStateResult) {
        sendSyncStateMessage(SyncStateResult.Connecting(result))
    }

    private fun startAsyncCommunicationWithServer() {
        Timber.d("Starting async communication with server")

        pendingChannelOps.clear()
        pendingChannelTagOps.clear()
        pendingRecordingOps.clear()
        pendingEventOps.clear()

        initialSyncWithServerRunning = true

        val epgMaxTime = java.lang.Long.parseLong(sharedPreferences.getString("epg_max_time", context.resources.getString(R.string.pref_default_epg_max_time))!!)
        val currentTimeInSeconds = System.currentTimeMillis() / 1000L
        val lastUpdateTime = connection.lastUpdate

        syncRequired = connection.isSyncRequired
        Timber.d("Sync from server required: $syncRequired")
        syncEventsRequired = syncRequired || lastUpdateTime + epgMaxTime < currentTimeInSeconds
        Timber.d("Sync events from server required: $syncEventsRequired")

        // Send the first sync message to any broadcast listeners
        if (syncRequired || syncEventsRequired) {
            Timber.d("Sending status that sync has started")
            sendSyncStateMessage(SyncStateResult.Syncing(SyncState.Started()))
        }
        if (syncEventsRequired) {
            Timber.d("Enabling requesting of epg data")

        }

        httpConnection?.sendMessage(Request.Builder().url("${connection.streamingUrl}/api/channel/grid").tag("channelAdd").build())
        httpConnection?.sendMessage(Request.Builder().url("${connection.streamingUrl}/api/channeltag/grid").tag("tagAdd").build())
        httpConnection?.sendMessage(Request.Builder().url("${connection.streamingUrl}/api/dvr/entry/grid").tag("dvrEntryAdd").build())
        httpConnection?.sendMessage(Request.Builder().url("${connection.streamingUrl}/api/autorec/grid").tag("autorecEntryAdd").build())
        httpConnection?.sendMessage(Request.Builder().url("${connection.streamingUrl}/api/timerec/grid").tag("timerecEntryAdd").build())

        httpConnection?.sendMessage(Request.Builder().url("${connection.streamingUrl}").tag("initialSyncCompleted").build())
    }

    override fun onMessage(response: JSONObject, method: String) {
        when (method) {
            "tagAdd" -> onTagAdd(response)
            /*
            "tagUpdate" -> onTagUpdate(response)
            "tagDelete" -> onTagDelete(response)
             */
            "channelAdd" -> onChannelAdd(response)
            /*
            "channelUpdate" -> onChannelUpdate(response)
            "channelDelete" -> onChannelDelete(response)
            */
            "dvrEntryAdd" -> onDvrEntryAdd(response)
            /*
            "dvrEntryUpdate" -> onDvrEntryUpdate(response)
            "dvrEntryDelete" -> onDvrEntryDelete(response)
            */
            "timerecEntryAdd" -> onTimerRecEntryAdd(response)
            /*
            "timerecEntryUpdate" -> onTimerRecEntryUpdate(response)
            "timerecEntryDelete" -> onTimerRecEntryDelete(response)
            */
            "autorecEntryAdd" -> onAutorecEntryAdd(response)
            /*
            "autorecEntryUpdate" -> onAutorecEntryUpdate(response)
            "autorecEntryDelete" -> onAutorecEntryDelete(response)
            "eventAdd" -> onEventAdd(response)
            "eventUpdate" -> onEventUpdate(response)
            "eventDelete" -> onEventDelete(response)
            */
            "initialSyncCompleted" -> onInitialSyncCompleted()
            /*
            "getSysTime" -> onSystemTime(response)
            "getDiskSpace" -> onDiskSpace(response)
            "getProfiles" -> onHtspProfiles(response)
            "getDvrConfigs" -> onDvrConfigs(response)
            "getEvents" -> onGetEvents(response, Intent())
            "serverStatus" -> onServerStatus(response)
             */
            else -> {
            }
        }
    }

    private fun onChannelAdd(response: JSONObject) {
        if (!initialSyncWithServerRunning) return

        Timber.d("Trying to parse JSON channel data")
        try {
            val array = response.getJSONArray("entries")
            for (i in 0 until array.length()) {
                Timber.d("Parsing data from one channel")
                val channel = convertMessageToChannelModel(Channel(), array.getJSONObject(i))
                channel.connectionId = connection.id
                channel.serverOrder = pendingChannelOps.size + 1
                Timber.d("Sync is running, adding channel name '${channel.name}', id '${channel.id}', number '${channel.displayNumber}', server order '${channel.serverOrder}")
                pendingChannelOps.add(channel)
            }
        } catch (e: JSONException) {
            Timber.d(e, "Error parsing JSON channel data")
        }
        if (syncRequired) {
            sendSyncStateMessage(SyncStateResult.Syncing(SyncState.InProgress("Received ${pendingChannelOps.size} channels")))
        }
    }

    private fun onTagAdd(response: JSONObject) {
        if (!initialSyncWithServerRunning) return
        Timber.d("Trying to parse JSON channel tag data")
        try {
            val array = response.getJSONArray("entries")
            for (i in 0 until array.length()) {
                Timber.d("Parsing data from one channel tag")

                // During initial sync no channels are yet saved. So use the temporarily
                // stored channels to calculate the channel count for the channel tag
                val addedTag = convertMessageToChannelTagModel(ChannelTag(), response, pendingChannelOps)
                addedTag.connectionId = connection.id

                Timber.d("Sync is running, adding channel tag")
                pendingChannelTagOps.add(addedTag)
            }
        } catch (e: JSONException) {
            Timber.d(e, "Error parsing JSON channel tag data")
        }
    }

    private fun onDvrEntryAdd(response: JSONObject) {
        TODO("Not yet implemented")
    }

    private fun onTimerRecEntryAdd(response: JSONObject) {
        TODO("Not yet implemented")
    }

    private fun onAutorecEntryAdd(response: JSONObject) {
        TODO("Not yet implemented")
    }

    override fun getDiscSpace() {
        // This is not available in the server API
    }

    override fun getSystemTime() {
        TODO("Not yet implemented")
    }

    override fun getChannel(intent: Intent) {
        TODO("Not yet implemented")
    }

    override fun getEvent(intent: Intent) {
        TODO("Not yet implemented")
    }

    override fun getEvents(intent: Intent) {
        TODO("Not yet implemented")
    }

    override fun getEpgQuery(intent: Intent) {
        TODO("Not yet implemented")
    }

    override fun addDvrEntry(intent: Intent) {
        TODO("Not yet implemented")
    }

    override fun updateDvrEntry(intent: Intent) {
        TODO("Not yet implemented")
    }

    override fun cancelDvrEntry(intent: Intent) {
        TODO("Not yet implemented")
    }

    override fun deleteDvrEntry(intent: Intent) {
        TODO("Not yet implemented")
    }

    override fun stopDvrEntry(intent: Intent) {
        TODO("Not yet implemented")
    }

    override fun addAutorecEntry(intent: Intent) {
        TODO("Not yet implemented")
    }

    override fun updateAutorecEntry(intent: Intent) {
        TODO("Not yet implemented")
    }

    override fun deleteAutorecEntry(intent: Intent) {
        TODO("Not yet implemented")
    }

    override fun addTimerrecEntry(intent: Intent) {
        TODO("Not yet implemented")
    }

    override fun updateTimerrecEntry(intent: Intent) {
        TODO("Not yet implemented")
    }

    override fun deleteTimerrecEntry(intent: Intent) {
        TODO("Not yet implemented")
    }

    override fun getTicket(intent: Intent) {
        TODO("Not yet implemented")
    }

    override fun getProfiles() {
        TODO("Not yet implemented")
    }

    override fun getDvrConfigs() {
        TODO("Not yet implemented")
    }

    override fun getSubscriptions() {
        TODO("Not yet implemented")
    }

    override fun getInputs() {
        TODO("Not yet implemented")
    }

    override fun getMoreEvents(intent: Intent) {
        TODO("Not yet implemented")
    }

    private fun sendSyncStateMessage(state: SyncStateResult, message: String = "", details: String = "") {

        val intent = Intent(SyncStateReceiver.ACTION)
        intent.putExtra(SyncStateReceiver.STATE, state)
        if (message.isNotEmpty()) {
            intent.putExtra(SyncStateReceiver.MESSAGE, message)
        }
        if (details.isNotEmpty()) {
            intent.putExtra(SyncStateReceiver.DETAILS, details)
        }
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }

    private fun onInitialSyncCompleted() {
        Timber.d("Received initial sync data from server")

        if (syncRequired) {
            sendSyncStateMessage(SyncStateResult.Syncing(SyncState.InProgress()))
        }

        // Save the channels and tags only during a forced sync.
        // This avoids the channel list being updated by the recyclerview
        if (syncRequired) {
            Timber.d("Sync of initial data is required, saving received channels, tags and downloading icons")
            saveAllReceivedChannels()
            saveAllReceivedChannelTags()
            // TODO loadAllChannelIcons(pendingChannelOps)
            // TODO loadAllChannelTagIcons(pendingChannelTagOps)
        } else {
            Timber.d("Sync of initial data is not required")
        }

        // Only save any received events when they shall be loaded
        if (syncEventsRequired) {
            Timber.d("Sync of all evens is required, saving events")
            saveAllReceivedEvents()
        } else {
            Timber.d("Sync of all evens is not required")
        }

        // Recordings are always saved to keep up to
        // date with the recording states from the server
        saveAllReceivedRecordings()

        // TODO getAdditionalServerData()

        // TODO startBackgroundWorkers()

        Timber.d("Updating connection status that initial sync is completed")
        connection.isSyncRequired = false
        if (syncEventsRequired) {
            Timber.d("Updating last update time of full sync")
            connection.lastUpdate = System.currentTimeMillis() / 1000L
        }
        appRepository.connectionData.updateItem(connection)

        // The initial sync is considered to be done at this point.
        // Send the message to the listeners that the sync is done
        if (syncRequired || syncEventsRequired) {
            sendSyncStateMessage(SyncStateResult.Syncing(SyncState.Done()))
        }

        syncRequired = false
        syncEventsRequired = false
        initialSyncWithServerRunning = false

        Timber.d("Done receiving initial data from server")
    }

    /**
     * Loads additional data from the server that is required after the initial sync is done.
     * This includes the disc space, the server system time and the playback and recording profiles.
     * If the server did not provide all required default profiles, then add them here.
     */
    private fun getAdditionalServerData() {
        Timber.d("Loading additional data from server")

        //getDiscSpace()
        getSystemTime()
        getProfiles()
        getDvrConfigs()
        // TODO getHttpProfiles()

        // TODO setDefaultProfileSelection()
    }

    /**
     * Saves all received channels from the initial sync in the database.
     */
    private fun saveAllReceivedChannels() {
        Timber.d("Saving ${pendingChannelOps.size} channels")
        if (pendingChannelOps.isNotEmpty()) {
            appRepository.channelData.addItems(pendingChannelOps)
        }
    }

    /**
     * Saves all received channel tags from the initial sync in the database.
     * Also the relations table between channels and tags are
     * updated so that the filtering by channel tags works properly
     */
    private fun saveAllReceivedChannelTags() {
        Timber.d("Saving ${pendingChannelTagOps.size} channel tags")

        val pendingRemovedTagAndChannelOps = ArrayList<TagAndChannel>()
        val pendingAddedTagAndChannelOps = ArrayList<TagAndChannel>()

        if (pendingChannelTagOps.isNotEmpty()) {
            appRepository.channelTagData.addItems(pendingChannelTagOps)
            for (tag in pendingChannelTagOps) {

                val tac = appRepository.tagAndChannelData.getItemById(tag.tagId)
                if (tac != null) {
                    pendingRemovedTagAndChannelOps.add(tac)
                }

                val channelIds = tag.members
                if (channelIds != null) {
                    for (channelId in channelIds) {
                        val tagAndChannel = TagAndChannel()
                        tagAndChannel.tagId = tag.tagId
                        tagAndChannel.channelId = channelId
                        tagAndChannel.connectionId = connection.id
                        pendingAddedTagAndChannelOps.add(tagAndChannel)
                    }
                }
            }

            Timber.d("Removing ${pendingRemovedTagAndChannelOps.size} and adding ${pendingAddedTagAndChannelOps.size} tag and channel relations")
            appRepository.tagAndChannelData.addAndRemoveItems(pendingAddedTagAndChannelOps, pendingRemovedTagAndChannelOps)
        }
    }

    /**
     * Removes all recordings and saves all received recordings from the initial sync
     * in the database. The removal is done to prevent being out of sync with the server.
     * This could be the case when the app was offline for a while and it did not receive
     * any recording removal information from the server. During the initial sync the
     * server only provides the list of available recordings.
     */
    private fun saveAllReceivedRecordings() {
        Timber.d("Removing previously existing recordings and saving ${pendingRecordingOps.size} new recordings")
        appRepository.recordingData.removeAndAddItems(pendingRecordingOps)
    }

    private fun saveAllReceivedEvents() {
        Timber.d("Saving ${pendingEventOps.size} new events")
        if (pendingEventOps.isNotEmpty()) {
            appRepository.programData.addItems(pendingEventOps)
        }
    }
}
