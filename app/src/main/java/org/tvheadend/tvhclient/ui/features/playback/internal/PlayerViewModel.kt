package org.tvheadend.tvhclient.ui.features.playback.internal

import android.app.Application
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import androidx.lifecycle.MutableLiveData
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.DefaultAllocator
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.video.VideoListener
import org.tvheadend.data.entity.Channel
import org.tvheadend.htsp.HtspConnection
import org.tvheadend.htsp.HtspConnectionData
import org.tvheadend.htsp.HtspConnectionStateListener
import org.tvheadend.tvhclient.BuildConfig
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.ui.base.BaseViewModel
import org.tvheadend.tvhclient.ui.features.playback.internal.utils.CustomEventLogger
import org.tvheadend.tvhclient.ui.features.playback.internal.utils.VideoAspect
import timber.log.Timber
import java.text.DecimalFormat
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import kotlin.math.max


class PlayerViewModel(application: Application) : BaseViewModel(application), HtspConnectionStateListener, VideoListener, Player.EventListener {

    private var channelId: Int = 0
    private val channelList: List<Channel>

    // Connection related
    private val execService: ScheduledExecutorService = Executors.newScheduledThreadPool(10)
    private val htspConnection: HtspConnection
    private var htspSubscriptionDataSourceFactory: HtspSubscriptionDataSource.Factory? = null
    private var htspFileInputStreamDataSourceFactory: HtspFileInputStreamDataSource.Factory? = null
    private var dataSource: HtspDataSourceInterface? = null

    // Player and helpers
    val player: SimpleExoPlayer
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

    // Contains the information like icon, title, subtitle, start
    // and stop times either for a channel or a recording
    private lateinit var playbackInformation: PlaybackInformation

    // Handler and runnable to update the playback information every second
    private lateinit var timeUpdateRunnable: Runnable
    private val timeUpdateHandler = Handler()

    var pipModeActive: Boolean = false

    private val defaultForceAspectRatio = application.applicationContext.resources.getBoolean(R.bool.pref_default_force_aspect_ratio_for_sd_content_enabled)
    private val defaultChannelSortOrder = application.applicationContext.resources.getString(R.string.pref_default_channel_sort_order)
    private val defaultAudioTunnelingEnabled = application.applicationContext.resources.getBoolean(R.bool.pref_default_audio_tunneling_enabled)
    private val defaultConnectionTimeout = application.resources.getString(R.string.pref_default_connection_timeout)

    init {
        Timber.d("Initializing view model")

        isConnected.postValue(false)
        playerIsPlaying.postValue(false)
        playerState.postValue(Player.STATE_IDLE)

        val channelSortOrder = Integer.valueOf(sharedPreferences.getString("channel_sort_order", defaultChannelSortOrder) ?: defaultChannelSortOrder)
        channelList = appRepository.channelData.getChannels(channelSortOrder)

        Timber.d("Starting connection")
        val connection = appRepository.connectionData.activeItem
        val connectionTimeout = Integer.valueOf(sharedPreferences.getString("connection_timeout", defaultConnectionTimeout)!!) * 1000

        val htspConnectionData = HtspConnectionData(
                connection.username,
                connection.password,
                connection.serverUrl,
                BuildConfig.VERSION_NAME,
                BuildConfig.VERSION_CODE,
                connectionTimeout
        )
        htspConnection = HtspConnection(htspConnectionData, this, null)

        execService.execute {
            htspConnection.openConnection()
            htspConnection.authenticate()
        }

        trackSelector = DefaultTrackSelector(application.applicationContext, AdaptiveTrackSelection.Factory())
        if (sharedPreferences.getBoolean("audio_tunneling_enabled", defaultAudioTunnelingEnabled)) {
            trackSelector.buildUponParameters().setTunnelingAudioSessionId(C.generateAudioSessionIdV21(application.applicationContext))
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
                .createDefaultLoadControl()

        Timber.d("Creating player instance")
        val rendererFactory = DefaultRenderersFactory(application.applicationContext)
                .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
                .setAllowedVideoJoiningTimeMs(DefaultRenderersFactory.DEFAULT_ALLOWED_VIDEO_JOINING_TIME_MS)

        player = SimpleExoPlayer.Builder(application.applicationContext, rendererFactory)
                .setTrackSelector(trackSelector)
                .setLoadControl(loadControl).build()

        player.addVideoListener(this)
        player.addListener(this)
        player.addAnalyticsListener(CustomEventLogger(trackSelector))

        timeUpdateRunnable = Runnable {
            Timber.d("Updating elapsed and remaining times")
            remainingTime.postValue(playbackInformation.remainingTime)
            elapsedTime.postValue(playbackInformation.elapsedTime)
            timeUpdateHandler.postDelayed(timeUpdateRunnable, 1000)
        }
    }


    fun isPlaybackProfileSelected(bundle: Bundle?): Boolean {
        val channelId = bundle?.getInt("channelId", 0) ?: 0
        if (channelId > 0) {
            val serverStatus = appRepository.serverStatusData.activeItem
            val serverProfile = appRepository.serverProfileData.getItemById(serverStatus.htspPlaybackServerProfileId)
            if (serverProfile != null && !serverProfile.name.isNullOrEmpty() && serverProfile.name != "None") {
                return true
            }
        }
        return false
    }

    fun loadMediaSource(context: Context, bundle: Bundle?) {
        Timber.d("Loading new media source")

        releaseMediaSource()

        channelId = bundle?.getInt("channelId", 0) ?: 0
        val dvrId = bundle?.getInt("dvrId", 0) ?: 0
        val localUri = bundle?.getString("uri", "") ?: ""

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

    private fun loadMediaSourceForChannel(context: Context, channelId: Int) {
        Timber.d("Loading media source for channel id $channelId")
        playbackInformation = PlaybackInformation(appRepository.channelData.getItemByIdWithPrograms(channelId, Date().time))
        val serverStatus = appRepository.serverStatusData.activeItem
        val serverProfile = appRepository.serverProfileData.getItemById(serverStatus.htspPlaybackServerProfileId)
        htspSubscriptionDataSourceFactory = HtspSubscriptionDataSource.Factory(context, htspConnection, serverProfile?.name)
        dataSource = htspSubscriptionDataSourceFactory?.currentDataSource

        Timber.d("Preparing player with media source")
        player.prepare(ProgressiveMediaSource.Factory(
                htspSubscriptionDataSourceFactory,
                TvheadendExtractorsFactory())
                .createMediaSource(Uri.parse("htsp://channel/$channelId")))

        liveTvIsPlaying.value = true
        player.playWhenReady = true
    }

    private fun loadMediaSourceForRecording(recordingId: Int) {
        Timber.d("Loading media source for recording id $recordingId")
        playbackInformation = PlaybackInformation(appRepository.recordingData.getItemById(recordingId))
        htspFileInputStreamDataSourceFactory = HtspFileInputStreamDataSource.Factory(htspConnection)
        dataSource = htspFileInputStreamDataSourceFactory?.currentDataSource

        Timber.d("Preparing player with media source")
        player.prepare(ProgressiveMediaSource.Factory(
                htspFileInputStreamDataSourceFactory,
                TvheadendExtractorsFactory())
                .createMediaSource(Uri.parse("htsp://dvrfile/$recordingId")))

        liveTvIsPlaying.value = false
        player.playWhenReady = true
    }

    private fun loadMediaSourceForLocalUri(context: Context, localUri: String) {
        Timber.d("Preparing player with local media source '$localUri'")
        playbackInformation = PlaybackInformation()

        player.prepare(ProgressiveMediaSource.Factory(
                DefaultDataSourceFactory(context, "Exoplayer-local"))
                .createMediaSource(Uri.parse(localUri)))

        liveTvIsPlaying.value = false
        player.playWhenReady = true
    }

    private fun releaseMediaSource() {
        Timber.d("Releasing previous media source")
        player.stop()
        trackSelector.buildUponParameters().clearSelectionOverrides()
        htspSubscriptionDataSourceFactory?.releaseCurrentDataSource()
        htspFileInputStreamDataSourceFactory?.releaseCurrentDataSource()
    }

    fun setVideoAspectRatio(rational: VideoAspect) {
        if (videoAspectRatio.value != null && videoAspectRatio.value != rational) {
            Timber.d("Updating selected video aspect ratio")
            videoAspectRatio.postValue(rational)
        }
    }

    override fun onConnectionStateChange(state: HtspConnection.ConnectionState) {
        when (state) {
            HtspConnection.ConnectionState.FAILED,
            HtspConnection.ConnectionState.FAILED_INTERRUPTED,
            HtspConnection.ConnectionState.FAILED_CONNECTING_TO_SERVER,
            HtspConnection.ConnectionState.FAILED_UNRESOLVED_ADDRESS,
            HtspConnection.ConnectionState.FAILED_EXCEPTION_OPENING_SOCKET -> {
                Timber.d("Connection failed")
                isConnected.postValue(false)
            }
            else -> {
                Timber.d("Connected, initializing or idle")
            }
        }
    }

    override fun onAuthenticationStateChange(state: HtspConnection.AuthenticationState) {
        when (state) {
            HtspConnection.AuthenticationState.FAILED,
            HtspConnection.AuthenticationState.FAILED_BAD_CREDENTIALS -> {
                Timber.d("Authorization failed")
                isConnected.postValue(false)
            }
            HtspConnection.AuthenticationState.AUTHENTICATED -> {
                Timber.d("Authenticated, starting player")
                isConnected.postValue(true)
            }
            else -> {
                Timber.d("Initializing or authenticating")
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
        execService.shutdown()
        htspConnection.closeConnection()
    }

    override fun onVideoSizeChanged(width: Int, height: Int, unappliedRotationDegrees: Int, pixelWidthHeightRatio: Float) {
        Timber.d("Video size changed to width $width, height $height, pixel aspect ratio $pixelWidthHeightRatio")
        var newPixelWidthHeightRatio = pixelWidthHeightRatio

        val forceAspectRatio = sharedPreferences.getBoolean("force_aspect_ratio_for_sd_content_enabled", defaultForceAspectRatio)
        if (forceAspectRatio) {
            Timber.d("Video aspect shall be forced, checking original video aspect ratio")

            val aspectRatio = DecimalFormat("#.##").format(width.toFloat() / height.toFloat())
            if (aspectRatio == "1,25") {
                newPixelWidthHeightRatio = ((16f / 9f) * height.toFloat()) / width.toFloat()
                Timber.d("Video aspect ratio is 5:4, updating pixel aspect ratio to $newPixelWidthHeightRatio")
            }
        }
        videoAspectRatio.postValue(VideoAspect((width * newPixelWidthHeightRatio).toInt(), height))
    }

    override fun onRenderedFirstFrame() {
        // NOP
    }

    override fun onSeekProcessed() {
        // NOP
    }

    override fun onLoadingChanged(isLoading: Boolean) {
        // NOP
    }

    override fun onPositionDiscontinuity(reason: Int) {
        when (reason) {
            Player.DISCONTINUITY_REASON_PERIOD_TRANSITION -> Timber.d("Automatic playback transition from one period in the timeline to the next.")
            Player.DISCONTINUITY_REASON_SEEK -> Timber.d("Seek within the current period or to another period.")
            Player.DISCONTINUITY_REASON_SEEK_ADJUSTMENT -> Timber.d("Seek adjustment due to being unable to seek to the requested position or because the seek was permitted to be inexact.")
            Player.DISCONTINUITY_REASON_AD_INSERTION -> Timber.d("Discontinuity to or from an ad within one period in the timeline.")
            Player.DISCONTINUITY_REASON_INTERNAL -> Timber.d("Discontinuity introduced internally by the source.")
        }
    }

    override fun onRepeatModeChanged(repeatMode: Int) {
        // NOP
    }

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
        // NOP
    }

    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
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
        val timeshiftStartTime = dataSource?.timeshiftStartTime ?: 0
        val timeshiftStartPts = dataSource?.timeshiftStartPts ?: 0
        val timeshiftOffsetPts = dataSource?.timeshiftOffsetPts ?: 0

        val startTime = if (timeshiftStartTime != Long.MIN_VALUE)
            (timeshiftStartTime / 1000) else 0
        Timber.d("Timeshift start time is $startTime")

        val currentTime = if (timeshiftOffsetPts != Long.MIN_VALUE)
            System.currentTimeMillis() + timeshiftOffsetPts / 1000 else player.currentPosition
        Timber.d("Timeshift current time is $currentTime")

        val time = max(currentTime + offset, startTime)
        val seekPts = time * 1000 - timeshiftStartTime
        return max(seekPts, timeshiftStartPts) / 1000
    }

    fun playNextChannel(context: Context) {
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

    fun playPreviousChannel(context: Context) {
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

    override fun onPlayerError(playbackException: ExoPlaybackException) {
        when (playbackException.type) {
            ExoPlaybackException.TYPE_SOURCE -> {
                Timber.d("Player error occurred while loading media source: ${playbackException.sourceException}")
            }
            ExoPlaybackException.TYPE_RENDERER -> {
                Timber.d("Player error occurred in the renderer")
            }
            ExoPlaybackException.TYPE_REMOTE -> {
                Timber.d("Player error occurred in a remote component")
            }
            ExoPlaybackException.TYPE_OUT_OF_MEMORY -> {
                Timber.d("Player error out of memory")
            }
            ExoPlaybackException.TYPE_UNEXPECTED -> {
                Timber.d("Player error unexpected runtime exception")
            }
        }
    }
}
