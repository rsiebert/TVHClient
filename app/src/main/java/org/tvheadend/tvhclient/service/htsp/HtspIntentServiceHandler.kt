package org.tvheadend.tvhclient.service.htsp

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import org.tvheadend.api.AuthenticationStateResult
import org.tvheadend.api.ConnectionStateResult
import org.tvheadend.api.ServerConnectionStateListener
import org.tvheadend.api.ServerResponseListener
import org.tvheadend.data.AppRepository
import org.tvheadend.data.entity.Connection
import org.tvheadend.data.entity.EpgProgram
import org.tvheadend.data.entity.Program
import org.tvheadend.data.entity.ServerStatus
import org.tvheadend.htsp.HtspConnection
import org.tvheadend.htsp.HtspConnectionData
import org.tvheadend.htsp.HtspFileInputStream
import org.tvheadend.htsp.HtspMessage
import org.tvheadend.tvhclient.BuildConfig
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.service.ConnectionIntentService
import org.tvheadend.tvhclient.service.ServerTicketReceiver
import org.tvheadend.tvhclient.service.SyncStateResult
import org.tvheadend.tvhclient.util.convertUrlToHashString
import org.tvheadend.tvhclient.util.extensions.sendSyncStateMessage
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import java.io.*
import java.net.URL
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import kotlin.math.floor
import kotlin.math.max

class HtspIntentServiceHandler(val context: Context, val appRepository: AppRepository, val connection: Connection) : ConnectionIntentService.ServiceInterface, ServerConnectionStateListener {

    /**
     * Bridges the now-suspending data-source reads back into this handler's blocking code.
     *
     * HtspIntentServiceHandler runs on a [androidx.core.app.JobIntentService] worker thread, never
     * on the main thread, so blocking here is acceptable. This is the documented runBlocking
     * boundary.
     */
    private fun <T> blocking(block: suspend () -> T): T = runBlocking { block() }

    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val execService: ScheduledExecutorService = Executors.newScheduledThreadPool(10)
    private val htspConnection: HtspConnection
    private val serverStatus: ServerStatus = blocking { appRepository.serverStatusData.getActiveItem() }
    private var htspVersion: Int = 13

    private val pendingEventOps = ArrayList<Program>()
    private val authenticationLock = Object()
    private val responseLock = Object()

    /** The connection timeout in milliseconds, used for the HTSP connection and the wait below. */
    private val connectionTimeoutMillis: Int = Integer.valueOf(
            sharedPreferences.getString("connection_timeout",
                    context.resources.getString(R.string.pref_default_connection_timeout))!!) * 1000

    /**
     * The outcome of the authentication attempt, or null while it is still running. Guarded by
     * [authenticationLock]. The one-shot work waits on this instead of sleeping for a fixed time,
     * so a fast server is not delayed and a slow one is not cut off after five seconds.
     */
    @Volatile
    private var authenticationResult: AuthenticationStateResult? = null

    /**
     * How long the one-shot work waits for an authentication result. It matches the connection
     * timeout plus a margin, so a slow server is given the same grace period the connection itself
     * uses before the work gives up.
     */
    private val authenticationWaitTimeout: Long = connectionTimeoutMillis + 5_000L

    /**
     * Set while the handler is being torn down. A failure that surfaces then was caused by closing
     * our own socket while the handshake was still in flight, not by the server, so it must not be
     * reported to the user as a failed authentication.
     */
    @Volatile
    private var isDestroying = false

    init {
        htspVersion = serverStatus.htspVersion

        val htspConnectionData = HtspConnectionData(
                connection.username,
                connection.password,
                connection.serverUrl,
                BuildConfig.VERSION_NAME,
                BuildConfig.VERSION_CODE,
                connectionTimeoutMillis
        )
        htspConnection = HtspConnection(htspConnectionData, this, null)

        // Since this is blocking, spawn to a new thread
        execService.execute {
            htspConnection.openConnection()
            htspConnection.authenticate()
        }
    }

    override fun onHandleWork(intent: Intent) {
        val action = intent.action
        if (action == null || action.isEmpty()) {
            return
        }

        waitForAuthentication()

        if (htspConnection.isNotConnected || !htspConnection.isAuthenticated) {
            Timber.d("Connection to server failed or authentication failed")
            return
        }

        Timber.d("Executing command $action for service handler")
        when (action) {
            "getMoreEvents" -> getMoreEvents(intent)
            "loadChannelIcons" -> loadAllChannelIcons()
            "getTicket" -> getTicket(intent)
            "cleanupDatabase" -> {
                removeOutdatedProgramsFromDatabase()
                removeDuplicateProgramsFromDatabase()
            }
        }
    }

    /**
     * Waits until the connection reported that it authenticated or failed. Waiting on the actual
     * result instead of a fixed delay means the work never starts against a half-open connection,
     * and a server that needs longer than five seconds is not mistaken for an unreachable one.
     */
    private fun waitForAuthentication() {
        val deadline = System.currentTimeMillis() + authenticationWaitTimeout
        synchronized(authenticationLock) {
            while (authenticationResult == null) {
                val remaining = deadline - System.currentTimeMillis()
                if (remaining <= 0) {
                    Timber.d("Timed out while waiting for the connection to authenticate")
                    return
                }
                try {
                    authenticationLock.wait(remaining)
                } catch (e: InterruptedException) {
                    Timber.e(e, "Waiting for the connection was interrupted")
                    Thread.currentThread().interrupt()
                    return
                }
            }
        }
    }

    override fun onDestroy() {
        Timber.d("Stopping service handler")
        isDestroying = true
        execService.shutdown()
        htspConnection.closeConnection()
    }

    override fun onAuthenticationStateChange(result: AuthenticationStateResult) {
        synchronized(authenticationLock) {
            authenticationResult = result
            authenticationLock.notifyAll()
        }
        // Closing the socket while the handshake is still running makes the connection report a
        // failure. That is not a server error, so it is not forwarded to the user interface.
        if (isDestroying && result is AuthenticationStateResult.Failed) {
            Timber.d("Not reporting the authentication failure caused by tearing down the handler")
            return
        }
        context.sendSyncStateMessage(SyncStateResult.Authenticating(result))
    }

    override fun onConnectionStateChange(result: ConnectionStateResult) {
        context.sendSyncStateMessage(SyncStateResult.Connecting(result))
    }

    /**
     * Handles the given server message that contains a list of events.
     *
     * @param message The message with the events
     */
    private fun onGetEvents(message: HtspMessage, intent: Intent) {
        val channelName = intent.getStringExtra("channelName")

        if (message.containsKey("events")) {
            val programs = ArrayList<Program>()
            for (obj in message.getList("events")) {
                val msg = obj as HtspMessage
                val program = convertMessageToProgramModel(Program(), msg)
                program.connectionId = connection.id

                programs.add(program)
            }
            Timber.d("Added ${programs.size} events to the list for channel $channelName")
            pendingEventOps.addAll(programs)
        }
    }

    private fun getTicket(intent: Intent) {
        val channelId = intent.getIntExtra("channelId", 0).toLong()
        val dvrId = intent.getIntExtra("dvrId", 0).toLong()

        val request = HtspMessage()
        request["method"] = "getTicket"
        if (channelId > 0) {
            request["channelId"] = channelId
        }
        if (dvrId > 0) {
            request["dvrId"] = dvrId
        }

        htspConnection.sendMessage(request, object : ServerResponseListener<HtspMessage> {
            override fun handleResponse(response: HtspMessage) {
                val ticketIntent = Intent(ServerTicketReceiver.ACTION)
                ticketIntent.putExtra(ServerTicketReceiver.PATH, response.getString("path"))
                ticketIntent.putExtra(ServerTicketReceiver.TICKET, response.getString("ticket"))
                LocalBroadcastManager.getInstance(context.applicationContext).sendBroadcast(ticketIntent)
            }
        })
    }

    /**
     * Tries to download and save all received channel and channel
     * tag logos from the initial sync in the database.
     */
    private fun loadAllChannelIcons() {
        Timber.d("Downloading and saving all channel and channel tag icons...")

        val iconUrls = ArrayList<String>()
        blocking { appRepository.channelData.getItems() }.forEach {
            val icon = it.icon
            if (!icon.isNullOrEmpty()) {
                iconUrls.add(icon)
            }
        }

        blocking { appRepository.channelTagData.getItems() }.forEach {
            val icon = it.tagIcon
            if (!icon.isNullOrEmpty()) {
                iconUrls.add(icon)
            }
        }

        var iconUrlCount = 0
        iconUrls.forEach {
            // Determine if events for the last channel in the list are being loaded.
            // This is required to set and release a lock to get all responses
            // before saving the event data
            val isLastIconUrl = ++iconUrlCount == iconUrls.size
            execService.execute {
                try {
                    Timber.d("Downloading icon url $it")
                    downloadIconFromFileUrl(it)
                    // Release the lock so that all data can be saved
                    if (isLastIconUrl) {
                        synchronized(responseLock) {
                            Timber.d("Got response for last icon, releasing lock")
                            responseLock.notify()
                        }
                    }
                } catch (e: Exception) {
                    Timber.d(e, "Could not load icon $it")
                }
            }

            // Wait until the last response from the server was received and the lock released
            if (isLastIconUrl) {
                synchronized(responseLock) {
                    try {
                        Timber.d("Loaded icons, waiting for response")
                        responseLock.wait(5000)
                    } catch (e: InterruptedException) {
                        Timber.e(e, "Timeout while waiting for last icon to load")
                    }
                }
            }
        }
    }

    private fun removeOutdatedProgramsFromDatabase() {
        Timber.d("Deleting programs from the database that are older than one day from now")
        val pastTime = System.currentTimeMillis() - 24 * 60 * 60 * 1000
        appRepository.programData.removeItemsByTime(pastTime)
    }

    private fun removeDuplicateProgramsFromDatabase() {
        Timber.d("Removing duplicate programs from the database")
        val duplicatePrograms = Vector<Int>()
        val channels = blocking { appRepository.channelData.getItems() }
        channels.forEach { channel ->
            val programs = blocking { appRepository.programData.getDuplicatePrograms(channel.id) }
            Timber.d("Loaded ${programs.size} duplicate programs for channel ${channel.name}")

            var lastProgram = EpgProgram()
            programs.forEach { program ->
                if (lastProgram.title == program.title
                        && lastProgram.subtitle == program.subtitle
                        && lastProgram.summary == program.summary
                        && lastProgram.description == program.description
                        && lastProgram.channelId == program.channelId
                        && lastProgram.modifiedTime != program.modifiedTime) {

                    if (lastProgram.modifiedTime < program.modifiedTime) {
                        Timber.d("Channel: ${channel.name}, program id: ${lastProgram.eventId}, title: ${lastProgram.title} is a duplicate")
                        duplicatePrograms.add(lastProgram.eventId)
                    } else {
                        Timber.d("Channel: ${channel.name}, program id: ${program.eventId}, title: ${program.title} is a duplicate")
                        duplicatePrograms.add(program.eventId)
                    }
                }
                lastProgram = program
            }
        }

        Timber.d("Removing ${duplicatePrograms.size} duplicate programs")
        duplicatePrograms.forEach {
            appRepository.programData.removeItemById(it)
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
    @Throws(IOException::class)
    private fun downloadIconFromFileUrl(url: String) {
        if (url.isEmpty()) {
            return
        }

        val file = File(context.cacheDir, convertUrlToHashString(url) + ".png")
        if (file.exists()) {
            Timber.d("Icon file ${file.absolutePath} exists already")
            return
        }

        var inputStream: InputStream
        inputStream = when {
            url.startsWith("http") -> BufferedInputStream(URL(url).openStream())
            htspVersion > 9 -> HtspFileInputStream(htspConnection, url)
            else -> return
        }

        val outputStream = FileOutputStream(file)

        // Set the options for a bitmap and decode an input stream into a bitmap
        var options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeStream(inputStream, null, options)
        inputStream.close()

        if (url.startsWith("http")) {
            inputStream = BufferedInputStream(URL(url).openStream())
        } else if (htspVersion > 9) {
            inputStream = HtspFileInputStream(htspConnection, url)
        }

        val scale = context.resources.displayMetrics.density
        val width = (64 * scale).toInt()
        val height = (64 * scale).toInt()

        // Set the sample size of the image. This is the number of pixels in
        // either dimension that correspond to a single pixel in the decoded
        // bitmap. For example, inSampleSize == 4 returns an image that is 1/4
        // the width/height of the original, and 1/16 the number of pixels.
        val ratio = max(options.outWidth / width, options.outHeight / height)
        val sampleSize = Integer.highestOneBit(floor(ratio.toDouble()).toInt())
        options = BitmapFactory.Options()
        options.inSampleSize = sampleSize

        // Now decode an input stream into a bitmap and compress it.
        val bitmap = BitmapFactory.decodeStream(inputStream, null, options)
        bitmap?.compress(Bitmap.CompressFormat.PNG, 100, outputStream)

        outputStream.close()
        inputStream.close()
    }

    /**
     * Loads a defined number of events for all channels.
     * This method is called by a worker after the initial sync is done.
     * All loaded events are saved in a temporary list and saved in one
     * batch into the database when all events were loaded for all channels.
     *
     * @param intent The intent with the parameters e.g. to define how many events shall be loaded
     */
    private fun getMoreEvents(intent: Intent) {

        val numberOfProgramsToLoad = intent.getIntExtra("numFollowing", 0)
        val channelList = blocking { appRepository.channelData.getItems() }

        Timber.d("Database currently contains ${blocking { appRepository.programData.getItemCount() }} events.")
        Timber.d("Loading $numberOfProgramsToLoad events for each of the ${channelList.size} channels")

        var channelCount = 0
        channelList.forEach {
            // Determine if events for the last channel in the list are being loaded.
            // This is required to set and release a lock to get all responses
            // before saving the event data
            val isLastChannel = ++channelCount == channelList.size

            val msgIntent = Intent()
            msgIntent.putExtra("numFollowing", numberOfProgramsToLoad)
            msgIntent.putExtra("channelId", it.id)
            msgIntent.putExtra("channelName", it.name)

            val lastProgram = blocking { appRepository.programData.getLastItemByChannelId(it.id) }
            when {
                lastProgram != null -> {
                    Timber.d("Loading more programs for channel ${it.name} from last program id ${lastProgram.eventId}")
                    msgIntent.putExtra("eventId", lastProgram.nextEventId)
                }
                it.nextEventId > 0 -> {
                    Timber.d("Loading more programs for channel ${it.name} starting from channel next event id ${it.nextEventId}")
                    msgIntent.putExtra("eventId", it.nextEventId)
                }
                else -> {
                    Timber.d("Loading more programs for channel ${it.name} starting from channel event id ${it.eventId}")
                    msgIntent.putExtra("eventId", it.eventId)
                }
            }

            val request = convertIntentToEventMessage(msgIntent)
            htspConnection.sendMessage(request, object : ServerResponseListener<HtspMessage> {
                override fun handleResponse(response: HtspMessage) {
                    onGetEvents(response, msgIntent)
                    // Release the lock so that all data can be saved
                    if (isLastChannel) {
                        synchronized(responseLock) {
                            Timber.d("Got response for last channel, releasing lock")
                            responseLock.notify()
                        }
                    }
                }
            })

            // Wait until the last response from the server was received and the lock released
            if (isLastChannel) {
                synchronized(responseLock) {
                    try {
                        Timber.d("Loaded more events for last channel, waiting for response")
                        responseLock.wait(5000)
                    } catch (e: InterruptedException) {
                        Timber.e(e, "Timeout while loading events for last channel")
                    }
                }
            }
        }

        Timber.d("Done loading more events")
        appRepository.programData.addItems(pendingEventOps)
        Timber.d("Saved ${pendingEventOps.size} events for all channels. Database contains ${blocking { appRepository.programData.getItemCount() }} events")
        pendingEventOps.clear()
    }
}
