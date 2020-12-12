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
import org.tvheadend.data.entity.Connection
import org.tvheadend.data.entity.Program
import org.tvheadend.data.entity.ServerStatus
import org.tvheadend.htsp.*
import org.tvheadend.tvhclient.BuildConfig
import org.tvheadend.tvhclient.R
import org.tvheadend.data.AppRepository
import org.tvheadend.tvhclient.service.ConnectionIntentService
import org.tvheadend.tvhclient.util.convertUrlToHashString
import timber.log.Timber
import java.io.*
import java.net.URL
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import kotlin.math.floor
import kotlin.math.max

class HtspIntentServiceHandler(val context: Context, val appRepository: AppRepository, val connection: Connection) : ConnectionIntentService.ServiceInterface, ServerConnectionStateListener {

    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val execService: ScheduledExecutorService = Executors.newScheduledThreadPool(10)
    private val htspConnection: HtspConnection
    private val serverStatus: ServerStatus
    private var htspVersion: Int = 13

    private val pendingEventOps = ArrayList<Program>()
    private val authenticationLock = Object()
    private val responseLock = Object()

    init {
        serverStatus = appRepository.serverStatusData.activeItem
        htspVersion = serverStatus.htspVersion

        val htspConnectionData = HtspConnectionData(
                connection.username,
                connection.password,
                connection.serverUrl,
                BuildConfig.VERSION_NAME,
                BuildConfig.VERSION_CODE,
                Integer.valueOf(sharedPreferences.getString("connection_timeout", context.resources.getString(R.string.pref_default_connection_timeout))!!) * 1000
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

        synchronized(authenticationLock) {
            try {
                authenticationLock.wait(5000)
            } catch (e: InterruptedException) {
                Timber.e(e, "Timeout waiting while connecting to server")
            }
        }

        if (htspConnection.isNotConnected || !htspConnection.isAuthenticated) {
            Timber.d("Connection to server failed or authentication failed")
            return
        }

        Timber.d("Executing command $action for service")
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

    override fun onDestroy() {
        Timber.d("Stopping service")
        execService.shutdown()
        htspConnection.closeConnection()
    }

    override fun onAuthenticationStateChange(result: AuthenticationStateResult) {
        synchronized(authenticationLock) {
            authenticationLock.notify()
        }
    }

    override fun onConnectionStateChange(result: ConnectionStateResult) {
        // NOP
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
                Timber.d("Response is not null")
                val ticketIntent = Intent("ticket")
                ticketIntent.putExtra("path", response.getString("path"))
                ticketIntent.putExtra("ticket", response.getString("ticket"))
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
        appRepository.channelData.getItems().forEach {
            val icon = it.icon
            if (!icon.isNullOrEmpty()) {
                iconUrls.add(icon)
            }
        }

        appRepository.channelTagData.getItems().forEach {
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
        val channels = appRepository.channelData.getItems()
        channels.forEach { channel ->
            val programs = appRepository.programData.getItemsByChannelId(channel.id)
            var lastProgram = Program()
            programs.forEach { program ->
                if (lastProgram.title == program.title
                        && lastProgram.subtitle == program.subtitle
                        && lastProgram.summary == program.summary
                        && lastProgram.description == program.description
                        && lastProgram.contentType == program.contentType
                        && lastProgram.seasonCount == program.seasonCount
                        && lastProgram.seasonNumber == program.seasonNumber
                        && lastProgram.episodeCount == program.episodeCount
                        && lastProgram.episodeNumber == program.episodeNumber
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
        val channelList = appRepository.channelData.getItems()

        Timber.d("Database currently contains ${appRepository.programData.itemCount} events.")
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

            val lastProgram = appRepository.programData.getLastItemByChannelId(it.id)
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
        Timber.d("Saved ${pendingEventOps.size} events for all channels. Database contains ${appRepository.programData.itemCount} events")
        pendingEventOps.clear()
    }
}
