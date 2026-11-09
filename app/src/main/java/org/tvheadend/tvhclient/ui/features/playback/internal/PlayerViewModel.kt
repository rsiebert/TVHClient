package org.tvheadend.tvhclient.ui.features.playback.internal

import android.app.Application
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.upstream.DefaultAllocator
import org.tvheadend.api.AuthenticationStateResult
import org.tvheadend.api.ConnectionStateResult
import org.tvheadend.api.ServerConnectionStateListener
import org.tvheadend.api.ServerResponseListener
import org.tvheadend.data.entity.Channel
import org.tvheadend.data.entity.DvrCutpoint
import org.tvheadend.htsp.*
import org.tvheadend.tvhclient.AppGraphProvider
import org.tvheadend.tvhclient.BuildConfig
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.service.htsp.convertMessageToCutpointList
import org.tvheadend.tvhclient.ui.base.BaseViewModel
import org.tvheadend.tvhclient.ui.features.playback.PlaybackLogic
import org.tvheadend.tvhclient.ui.features.playback.internal.utils.CustomEventLogger
import org.tvheadend.tvhclient.ui.features.playback.internal.utils.VideoAspect
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*
import java.util.concurrent.ScheduledExecutorService


class PlayerViewModel(application: Application) : BaseViewModel(application), ServerConnectionStateListener, Player.Listener {

    private var channelId: Int = 0
    // Loaded off the main thread by the coroutine in [init].
    private lateinit var channelList: List<Channel>

    // Connection related
    private val execService: ScheduledExecutorService = AppGraphProvider.require().htspExecutor
    private lateinit var htspConnection: HtspConnection
    private var htspSubscriptionDataSourceFactory: HtspSubscriptionDataSource.Factory? = null
    private var htspFileInputStreamDataSourceFactory: HtspFileInputStreamDataSource.Factory? = null
    private var dataSource: HtspDataSourceInterface? = null

    // Player and helpers
    val player: ExoPlayer
    val trackSelector: DefaultTrackSelector

    // Video dimension and aspect ratio related properties
    val videoAspectRatio: MutableLiveData<VideoAspect> = MutableLiveData()

    // Observable fields
    var playerState: MutableLiveData<Int> = MutableLiveData()
    var playerIsPlaying: MutableLiveData<Boolean> = MutableLiveData()
    var liveTvIsPlaying: MutableLiveData<Boolean> = MutableLiveData()
    var isConnected: MutableLiveData<Boolean> = MutableLiveData()
    var channelIcon: MutableLiveData<String> = MutableLiveData()
    var channelName: MutableLiveData<String> = MutableLiveData()
    var title: MutableLiveData<String> = MutableLiveData()
    var subtitle: MutableLiveData<String> = MutableLiveData()
    var nextTitle: MutableLiveData<String> = MutableLiveData()
    var elapsedTime: MutableLiveData<String> = MutableLiveData()
    var remainingTime: MutableLiveData<String> = MutableLiveData()

    // State of the current subscription as reported by the server. This is not the stream
    // data itself but the signal quality, the descrambler information, the queue statistics
    // and the error status of the subscription.
    var subscriptionState: MutableLiveData<HtspSubscriptionState> = MutableLiveData()
    var statusMessage: MutableLiveData<String> = MutableLiveData()

    // Ranges of the current recording that the server has marked as a commercial break
    // or as a cut. They are only loaded for recordings and only used when the user
    // has enabled skipping them.
    var cutpoints: MutableLiveData<List<DvrCutpoint>> = MutableLiveData()

    // Contains the information like icon, title, subtitle, start
    // and stop times either for a channel or a recording
    private lateinit var playbackInformation: PlaybackInformation

    // Handler and runnable to update the playback information every second
    private lateinit var timeUpdateRunnable: Runnable
    private val timeUpdateHandler = Handler(Looper.getMainLooper())

    var pipModeActive: Boolean = false

    private val defaultForceAspectRatio = application.applicationContext.resources.getBoolean(R.bool.pref_default_force_aspect_ratio_for_sd_content_enabled)
    private val defaultChannelSortOrder = application.applicationContext.resources.getString(R.string.pref_default_channel_sort_order)
    private val defaultAudioTunnelingEnabled = application.applicationContext.resources.getBoolean(R.bool.pref_default_audio_tunneling_enabled)
    private val defaultConnectionTimeout = application.resources.getString(R.string.pref_default_connection_timeout)
    private val defaultAutoSkipCutpoints = application.applicationContext.resources.getBoolean(R.bool.pref_default_auto_skip_cutpoints_enabled)

    init {
        Timber.d("Initializing view model")

        isConnected.postValue(false)
        playerIsPlaying.postValue(false)
        playerState.postValue(Player.STATE_IDLE)

        trackSelector = DefaultTrackSelector(application.applicationContext)
        trackSelector.buildUponParameters().setRendererDisabled(C.TRACK_TYPE_TEXT, true)
        if (sharedPreferences.getBoolean("audio_tunneling_enabled", defaultAudioTunnelingEnabled)) {
            trackSelector.buildUponParameters().setTunnelingEnabled(true)
        }

        Timber.d("Creating load control")
        val bufferTime = Integer.valueOf(sharedPreferences.getString("buffer_playback_ms", application.applicationContext.resources.getString(R.string.pref_default_buffer_playback_ms))!!)
        val loadControl = DefaultLoadControl.Builder()
                .setAllocator(DefaultAllocator(true, C.DEFAULT_BUFFER_SEGMENT_SIZE))
                .setBufferDurationsMs(DefaultLoadControl.DEFAULT_MIN_BUFFER_MS,
                        DefaultLoadControl.DEFAULT_MAX_BUFFER_MS,
                        bufferTime,
                        DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS)
                .setTargetBufferBytes(C.DEFAULT_BUFFER_SEGMENT_SIZE)
                .setPrioritizeTimeOverSizeThresholds(true)
                .build()

        Timber.d("Creating player instance")
        val rendererFactory = DefaultRenderersFactory(application.applicationContext)
                .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
                .setAllowedVideoJoiningTimeMs(DefaultRenderersFactory.DEFAULT_ALLOWED_VIDEO_JOINING_TIME_MS)

        player = ExoPlayer.Builder(application.applicationContext, rendererFactory)
                .setTrackSelector(trackSelector)
                .setLoadControl(loadControl).build()

        player.addListener(this)
        player.addAnalyticsListener(CustomEventLogger(trackSelector))

        timeUpdateRunnable = Runnable {
            Timber.d("Updating elapsed and remaining times")
            remainingTime.postValue(playbackInformation.remainingTime)
            elapsedTime.postValue(playbackInformation.elapsedTime)
            skipCutpointsIfRequested()
            timeUpdateHandler.postDelayed(timeUpdateRunnable, 1000)
        }

        // The channel list and the connection are database reads, so they must not run on the
        // main thread during construction.
        viewModelScope.launch {
            val channelSortOrder = Integer.valueOf(sharedPreferences.getString("channel_sort_order", defaultChannelSortOrder) ?: defaultChannelSortOrder)
            channelList = appRepository.channelData.getChannels(channelSortOrder)

            Timber.d("Starting connection")
            val connection = appRepository.connectionData.getActiveItem()
            val connectionTimeout = Integer.valueOf(sharedPreferences.getString("connection_timeout", defaultConnectionTimeout)!!) * 1000

            val htspConnectionData = HtspConnectionData(
                    connection.username,
                    connection.password,
                    connection.serverUrl,
                    BuildConfig.VERSION_NAME,
                    BuildConfig.VERSION_CODE,
                    connectionTimeout
            )
            htspConnection = HtspConnection(htspConnectionData, this@PlayerViewModel, null)

            execService.execute {
                htspConnection.openConnection()
                htspConnection.authenticate()
            }
        }
    }


    suspend fun isPlaybackProfileSelected(bundle: Bundle?): Boolean {
        val channelId = bundle?.getInt("channelId", 0) ?: 0
        if (channelId <= 0) {
            // Only live channels are streamed through a server playback profile. Recordings and
            // local files are read directly, so there is no profile to check for them.
            return true
        }
        val serverStatus = appRepository.serverStatusData.getActiveItem()
        val serverProfile = appRepository.serverProfileData.getItemById(serverStatus.htspPlaybackServerProfileId)
        return serverProfile != null && !serverProfile.name.isNullOrEmpty() && serverProfile.name != "None"
    }

    suspend fun loadMediaSource(context: Context, bundle: Bundle?) {
        Timber.d("Loading new media source")

        releaseMediaSource()

        channelId = bundle?.getInt("channelId", 0) ?: 0
        val dvrId = bundle?.getInt("dvrId", 0) ?: 0
        val localUri = bundle?.getString("uri", "") ?: ""

        // The new media source does not have any status information yet, so forget about
        // the one of the previous source and allow the same message to be shown again.
        lastStatusMessage = null
        subscriptionState.postValue(HtspSubscriptionState())
        cutpoints.postValue(emptyList())

        when {
            channelId > 0 -> loadMediaSourceForChannel(context, channelId)
            dvrId > 0 -> loadMediaSourceForRecording(dvrId)
            localUri.isNotEmpty() -> loadMediaSourceForLocalUri(context, localUri)
        }

        Timber.d("Showing playback information")
        channelIcon.postValue(playbackInformation.channelIcon)
        channelName.postValue(playbackInformation.channelName)
        title.postValue(playbackInformation.title)
        subtitle.postValue(playbackInformation.subtitle)
        nextTitle.postValue(playbackInformation.nextTitle)
    }

    private suspend fun loadMediaSourceForChannel(context: Context, channelId: Int) {
        Timber.d("Loading media source for channel id $channelId")
        playbackInformation = PlaybackInformation(appRepository.channelData.getItemByIdWithPrograms(channelId, Date().time))
        val serverStatus = appRepository.serverStatusData.getActiveItem()
        val serverProfile = appRepository.serverProfileData.getItemById(serverStatus.htspPlaybackServerProfileId)
        val dataSourceFactory = HtspSubscriptionDataSource.Factory(context, htspConnection, serverProfile?.name)
        dataSourceFactory.setStateListener(subscriptionStateListener)
        htspSubscriptionDataSourceFactory = dataSourceFactory
        dataSource = dataSourceFactory.currentDataSource

        Timber.d("Preparing player with media source")
        player.prepare(ProgressiveMediaSource.Factory(
                dataSourceFactory,
                TvheadendExtractorsFactory())
                .createMediaSource(MediaItem.fromUri(Uri.parse("htsp://channel/$channelId"))))

        liveTvIsPlaying.value = true
        player.playWhenReady = true
    }

    private suspend fun loadMediaSourceForRecording(recordingId: Int) {
        Timber.d("Loading media source for recording id $recordingId")
        playbackInformation = PlaybackInformation(appRepository.recordingData.getItemById(recordingId))
        val dataSourceFactory = HtspFileInputStreamDataSource.Factory(htspConnection)
        htspFileInputStreamDataSourceFactory = dataSourceFactory
        dataSource = dataSourceFactory.currentDataSource

        Timber.d("Preparing player with media source")
        player.prepare(ProgressiveMediaSource.Factory(
                dataSourceFactory,
                TvheadendExtractorsFactory())
                .createMediaSource(MediaItem.fromUri(Uri.parse("htsp://dvrfile/$recordingId"))))

        requestCutpoints(recordingId)

        liveTvIsPlaying.value = false
        player.playWhenReady = true
    }

    private fun loadMediaSourceForLocalUri(context: Context, localUri: String) {
        Timber.d("Preparing player with local media source '$localUri'")
        playbackInformation = PlaybackInformation()

        player.prepare(ProgressiveMediaSource.Factory(
                DefaultDataSource.Factory(context))
                .createMediaSource(MediaItem.fromUri(Uri.parse(localUri))))

        liveTvIsPlaying.value = false
        player.playWhenReady = true
    }

    private fun releaseMediaSource() {
        Timber.d("Releasing previous media source")
        player.stop()
        trackSelector.buildUponParameters().clearSelectionOverrides()
        htspSubscriptionDataSourceFactory?.setStateListener(null)
        htspSubscriptionDataSourceFactory?.releaseCurrentDataSource()
        htspFileInputStreamDataSourceFactory?.releaseCurrentDataSource()
    }

    /**
     * Receives the subscription state messages that the server sends while a channel is
     * streamed. These are the signal and descrambler information, the queue statistics and
     * the messages that report why a subscription could not be started or was stopped.
     */
    private val subscriptionStateListener = object : HtspSubscriptionStateListener {
        override fun onSubscriptionStateChanged(state: HtspSubscriptionState) {
            subscriptionState.postValue(state)

            val message = buildStatusMessage(state) ?: return
            if (message != lastStatusMessage) {
                lastStatusMessage = message
                Timber.d("Updating the playback status message to '$message'")
                statusMessage.postValue(message)
            }
        }
    }

    private var lastStatusMessage: String? = null

    /**
     * Converts the status of the subscription into a message for the user. Returns null when
     * the state does not contain anything the user needs to know about.
     */
    private fun buildStatusMessage(state: HtspSubscriptionState): String? {
        val graceTimeout = state.graceTimeout
        if (graceTimeout != null) {
            return getApplication<Application>().getString(R.string.htsp_status_grace_period, graceTimeout)
        }

        val status = state.status ?: return null
        val statusResId = htspStatusToStringRes(status)
        return if (statusResId != null) {
            getApplication<Application>().getString(statusResId)
        } else {
            // The server can send plain text instead of a code, for example "Input/output error".
            getApplication<Application>().getString(R.string.playback_status_error, status)
        }
    }

    /**
     * Requests the ranges that the server has marked in the given recording, for example
     * commercial breaks. Servers that do not support this feature simply do not reply,
     * which leaves the list of cut points empty.
     */
    private suspend fun requestCutpoints(recordingId: Int) {
        val htspVersion = appRepository.serverStatusData.getActiveItem().htspVersion
        if (htspVersion < MIN_HTSP_VERSION_FOR_CUTPOINTS) {
            Timber.d("Not requesting cut points, the server only supports HTSP version $htspVersion")
            return
        }

        Timber.d("Requesting the cut points of the recording $recordingId")
        val request = HtspMessage()
        request["method"] = "getDvrCutpoints"
        request["id"] = recordingId

        htspConnection.sendMessage(request, object : ServerResponseListener<HtspMessage> {
            override fun handleResponse(response: HtspMessage) {
                if (response.containsKey("error")) {
                    Timber.d("Error requesting the cut points: %s", response.getString("error"))
                    return
                }
                val receivedCutpoints = convertMessageToCutpointList(response)
                Timber.d("Received ${receivedCutpoints.size} cut points for the recording $recordingId")
                cutpoints.postValue(receivedCutpoints)
            }
        })
    }

    /**
     * Continues the playback behind the range that the current position falls into, but only
     * if the server marked it as a range that shall be skipped and the user has enabled this.
     */
    private fun skipCutpointsIfRequested() {
        if (!isAutoSkipCutpointsEnabled()) {
            return
        }

        val position = player.currentPosition
        val cutpoint = cutpoints.value?.firstOrNull {
            it.isSkippedDuringPlayback && position >= it.start && position < it.end
        } ?: return

        Timber.d("Skipping the range from ${cutpoint.start} to ${cutpoint.end} at position $position")
        player.seekTo(cutpoint.end)
    }

    private fun isAutoSkipCutpointsEnabled(): Boolean {
        return sharedPreferences.getBoolean("auto_skip_cutpoints_enabled", defaultAutoSkipCutpoints)
    }

    fun setVideoAspectRatio(rational: VideoAspect) {
        if (videoAspectRatio.value != null && videoAspectRatio.value != rational) {
            Timber.d("Updating selected video aspect ratio")
            videoAspectRatio.postValue(rational)
        }
    }

    override fun onAuthenticationStateChange(result: AuthenticationStateResult) {
        when (result) {
            is AuthenticationStateResult.Idle -> {}
            is AuthenticationStateResult.Authenticating -> {
                Timber.d("Authenticating")
            }
            is AuthenticationStateResult.Authenticated -> {
                Timber.d("Authenticated, starting player")
                isConnected.postValue(true)
            }
            is AuthenticationStateResult.Failed -> {
                Timber.d("Authorization failed")
                isConnected.postValue(false)
            }
        }
    }

    override fun onConnectionStateChange(result: ConnectionStateResult) {
        when (result) {
            is ConnectionStateResult.Failed -> {
                Timber.d("Connection failed")
                isConnected.postValue(false)
            }
            else -> {
                Timber.d("Connected, initializing or idle")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        Timber.d("Clearing view model")
        stopPlaybackAndReleaseMediaSource()
    }

    fun stopPlaybackAndReleaseMediaSource() {
        Timber.d("Stopping playback, releasing media source ")
        releaseMediaSource()
        player.release()

        Timber.d("Closing connection")
        // The executor is shared and application scoped, so it is not shut down here.
        // The connection is created by the init coroutine, so it may not exist yet if the
        // activity is finished immediately.
        if (::htspConnection.isInitialized) {
            htspConnection.closeConnection()
        }
    }

    override fun onVideoSizeChanged(videoSize: VideoSize) {
        val width = videoSize.width
        val height = videoSize.height
        val pixelWidthHeightRatio = videoSize.pixelWidthHeightRatio
        Timber.d("Video size changed to width $width, height $height, pixel aspect ratio $pixelWidthHeightRatio")
        val forceAspectRatio = sharedPreferences.getBoolean("force_aspect_ratio_for_sd_content_enabled", defaultForceAspectRatio)
        if (forceAspectRatio) {
            Timber.d("Video aspect shall be forced, checking original video aspect ratio")
        }
        videoAspectRatio.postValue(
            PlaybackLogic.videoAspect(width, height, pixelWidthHeightRatio, forceAspectRatio)
        )
    }

    override fun onRenderedFirstFrame() {
        // NOP
    }

    override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) {
        when (reason) {
            Player.DISCONTINUITY_REASON_AUTO_TRANSITION -> Timber.d("Automatic playback transition from one period in the timeline to the next.")
            Player.DISCONTINUITY_REASON_SEEK -> Timber.d("Seek within the current period or to another period.")
            Player.DISCONTINUITY_REASON_SEEK_ADJUSTMENT -> Timber.d("Seek adjustment due to being unable to seek to the requested position or because the seek was permitted to be inexact.")
            Player.DISCONTINUITY_REASON_INTERNAL -> Timber.d("Discontinuity introduced internally by the source.")
        }
    }

    override fun onRepeatModeChanged(repeatMode: Int) {
        // NOP
    }

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
        // NOP
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        updatePlayerState(playbackState)
    }

    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
        updatePlayerState(player.playbackState)
    }

    private fun updatePlayerState(playbackState: Int) {
        playerState.postValue(playbackState)

        // Show the pause button and hide the play button if the player is playing.
        // Assume the player is playing when the property is true, otherwise it is paused.
        // Also continue or pause the timer that will update the elapsed and remaining time every second
        if (player.playWhenReady && playbackState == Player.STATE_READY) {
            Timber.d("Media is playing")
            playerIsPlaying.postValue(true)
            timeUpdateHandler.post(timeUpdateRunnable)

        } else if (!player.playWhenReady) {
            Timber.d("Player is paused")
            playerIsPlaying.postValue(false)
            timeUpdateHandler.removeCallbacks(timeUpdateRunnable)
        }
    }

    fun pause() {
        if (!pipModeActive) {
            player.playWhenReady = false
            dataSource?.pause()
        }
    }

    fun play() {
        player.playWhenReady = true
        dataSource?.resume()
    }

    fun seekBackward() {
        val time = getSeekPosition(-5000)
        Timber.d("Seeking backward to $time")
        player.seekTo(time)
    }

    fun seekForward() {
        val time = getSeekPosition(5000)
        Timber.d("Seeking forward to $time")
        player.seekTo(time)
    }

    private fun getSeekPosition(offset: Int): Long {
        val position = PlaybackLogic.seekPosition(
            timeshiftStartTime = dataSource?.timeshiftStartTime ?: 0,
            timeshiftStartPts = dataSource?.timeshiftStartPts ?: 0,
            timeshiftOffsetPts = dataSource?.timeshiftOffsetPts ?: 0,
            currentPosition = player.currentPosition,
            now = System.currentTimeMillis(),
            offsetSeconds = offset
        )
        Timber.d("Seek position for offset $offset is $position")
        return position
    }

    suspend fun playNextChannel(context: Context) {
        var newChannelId = channelId
        channelList.forEachIndexed { index, channel ->
            if (channel.id == channelId) {
                newChannelId = if (index + 1 < channelList.size) {
                    channelList[index + 1].id
                } else {
                    channelList.first().id
                }
            }
        }
        val bundle = Bundle()
        bundle.putInt("channelId", newChannelId)
        loadMediaSource(context, bundle)
    }

    suspend fun playPreviousChannel(context: Context) {
        var newChannelId = channelId
        channelList.forEachIndexed { index, channel ->
            if (channel.id == channelId) {
                newChannelId = if (index - 1 > 0) {
                    channelList[index - 1].id
                } else {
                    channelList.last().id
                }
            }
        }
        val bundle = Bundle()
        bundle.putInt("channelId", newChannelId)
        loadMediaSource(context, bundle)
    }

    override fun onPlayerError(error: PlaybackException) {
        Timber.d("Player error occurred with code ${error.errorCode}: ${error.message}")
    }

    companion object {
        // getDvrCutpoints was added to the protocol in this version
        private const val MIN_HTSP_VERSION_FOR_CUTPOINTS = 12
    }
}
