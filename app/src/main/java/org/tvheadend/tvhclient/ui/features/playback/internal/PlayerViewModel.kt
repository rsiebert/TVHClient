package org.tvheadend.tvhclient.ui.features.playback.internal

import android.app.Application
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
import com.google.android.exoplayer2.util.EventLogger
import com.google.android.exoplayer2.video.VideoListener
import org.tvheadend.data.entity.Channel
import org.tvheadend.htsp.HtspConnection
import org.tvheadend.htsp.HtspConnectionStateListener
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.ui.base.BaseViewModel
import org.tvheadend.tvhclient.ui.features.playback.internal.utils.Rational
import timber.log.Timber
import java.io.IOException
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import kotlin.math.max


class PlayerViewModel(application: Application) : BaseViewModel(application), HtspConnectionStateListener, VideoListener, Player.EventListener {

    private var channelId: Int = 0
    private var dvrId: Int = 0

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
    val videoAspectRatio: MutableLiveData<Rational> = MutableLiveData()

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

    init {
        Timber.d("Initializing view model")

        isConnected.postValue(false)
        playerIsPlaying.postValue(false)
        playerState.postValue(Player.STATE_IDLE)

        Timber.d("Starting connection")
        val connection = appRepository.connectionData.activeItem
        val connectionTimeout = Integer.valueOf(sharedPreferences.getString("connection_timeout", application.resources.getString(R.string.pref_default_connection_timeout))!!) * 1000
        htspConnection = HtspConnection(
                connection.username ?: "",
                connection.password ?: "",
                connection.serverUrl ?: "",
                connectionTimeout,
                this, null)

        execService.execute {
            htspConnection.openConnection()
            htspConnection.authenticate()
        }

        trackSelector = DefaultTrackSelector(appContext, AdaptiveTrackSelection.Factory())
        if (sharedPreferences.getBoolean("audio_tunneling_enabled", appContext.resources.getBoolean(R.bool.pref_default_audio_tunneling_enabled))) {
            trackSelector.buildUponParameters().setTunnelingAudioSessionId(C.generateAudioSessionIdV21(appContext))
        }

        Timber.d("Creating load control")
        val bufferTime = Integer.valueOf(sharedPreferences.getString("buffer_playback_ms", appContext.resources.getString(R.string.pref_default_buffer_playback_ms))!!)
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
        val rendererFactory = TvheadendRenderersFactory(appContext)
                .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
                .setAllowedVideoJoiningTimeMs(DefaultRenderersFactory.DEFAULT_ALLOWED_VIDEO_JOINING_TIME_MS)

        player = SimpleExoPlayer.Builder(appContext, rendererFactory)
                .setTrackSelector(trackSelector)
                .setLoadControl(loadControl).build()

        player.addVideoListener(this)
        player.addListener(this)
        player.addAnalyticsListener(EventLogger(trackSelector))

        timeUpdateRunnable = Runnable {
            Timber.d("Updating elapsed and remaining times")
            remainingTime.postValue(playbackInformation.remainingTime)
            elapsedTime.postValue(playbackInformation.elapsedTime)
            timeUpdateHandler.postDelayed(timeUpdateRunnable, 1000)
        }
    }

    fun loadMediaSource(bundle: Bundle?) {
        Timber.d("Loading new media source")

        releaseMediaSource()
        channelId = bundle?.getInt("channelId", 0) ?: 0
        dvrId = bundle?.getInt("dvrId", 0) ?: 0

        if (channelId > 0) {
            liveTvIsPlaying.value = true
            loadMediaSourceForChannel(channelId)
        } else if (dvrId > 0) {
            liveTvIsPlaying.value = false
            loadMediaSourceForRecording(dvrId)
        }

        Timber.d("Showing playback information")
        channelIcon.postValue(playbackInformation.channelIcon)
        channelName.postValue(playbackInformation.channelName)
        title.postValue(playbackInformation.title)
        subtitle.postValue(playbackInformation.subtitle)
        nextTitle.postValue(playbackInformation.nextTitle)
    }

    private fun loadMediaSourceForChannel(channelId: Int) {
        Timber.d("Loading media source for channel id $channelId")
        if (channelId > 0) {
            Timber.d("Loading player info")
            playbackInformation = PlaybackInformation(appRepository.channelData.getItemByIdWithPrograms(channelId, Date().time))

            Timber.d("Creating data source")
            val serverStatus = appRepository.serverStatusData.activeItem
            val serverProfile = appRepository.serverProfileData.getItemById(serverStatus.htspPlaybackServerProfileId)
            htspSubscriptionDataSourceFactory = HtspSubscriptionDataSource.Factory(appContext, htspConnection, serverProfile?.name)
            dataSource = htspSubscriptionDataSourceFactory?.currentDataSource

            Timber.d("Loading response header after data source creation in factory")
            val responseHeaders = dataSource?.getResponseHeaders()
            Timber.d("Found ${responseHeaders?.size ?: 0} response headers")

            Timber.d("Preparing player with media source")
            player.prepare(ProgressiveMediaSource.Factory(
                    htspSubscriptionDataSourceFactory,
                    TvheadendExtractorsFactory())
                    .createMediaSource(Uri.parse("htsp://channel/$channelId")))
            player.playWhenReady = true
        }
    }

    private fun loadMediaSourceForRecording(recordingId: Int) {
        Timber.d("Loading media source for recording id $recordingId")
        if (recordingId > 0) {
            Timber.d("Loading player info")
            playbackInformation = PlaybackInformation(appRepository.recordingData.getItemById(recordingId))

            Timber.d("Creating data source")
            htspFileInputStreamDataSourceFactory = HtspFileInputStreamDataSource.Factory(htspConnection)
            dataSource = htspFileInputStreamDataSourceFactory?.currentDataSource

            Timber.d("Preparing player with media source")
            player.prepare(ProgressiveMediaSource.Factory(
                    htspFileInputStreamDataSourceFactory,
                    TvheadendExtractorsFactory())
                    .createMediaSource(Uri.parse("htsp://dvrfile/$recordingId")))
            player.playWhenReady = true
        }
    }

    private fun releaseMediaSource() {
        Timber.d("Releasing previous media source")
        player.stop()
        trackSelector.buildUponParameters().clearSelectionOverrides()
        htspSubscriptionDataSourceFactory?.releaseCurrentDataSource()
        htspFileInputStreamDataSourceFactory?.releaseCurrentDataSource()
    }

    fun setVideoAspectRatio(rational: Rational) {
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
        videoAspectRatio.postValue(Rational(width, height))
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

    private fun getChannelList(): List<Channel> {
        val defaultChannelSortOrder = appContext.resources.getString(R.string.pref_default_channel_sort_order)
        val channelSortOrder = Integer.valueOf(sharedPreferences.getString("channel_sort_order", defaultChannelSortOrder)
                ?: defaultChannelSortOrder)
        return appRepository.channelData.getChannels(channelSortOrder)
    }

    fun playNextChannel() {

        val channels = getChannelList()
        var newChannelId = channelId
        channels.forEachIndexed { index, channel ->
            if (channel.id == channelId) {
                newChannelId = if (index + 1 < channels.size) {
                    channels[index + 1].id
                } else {
                    channels.first().id
                }
            }
        }
        val bundle = Bundle()
        bundle.putInt("channelId", newChannelId)
        loadMediaSource(bundle)
    }

    fun playPreviousChannel() {
        val channels = getChannelList()
        var newChannelId = channelId
        channels.forEachIndexed { index, channel ->
            if (channel.id == channelId) {
                newChannelId = if (index - 1 > 0) {
                    channels[index - 1].id
                } else {
                    channels.last().id
                }
            }
        }
        val bundle = Bundle()
        bundle.putInt("channelId", newChannelId)
        loadMediaSource(bundle)
    }

    override fun onPlayerError(error: ExoPlaybackException) {
        if (error.type == ExoPlaybackException.TYPE_SOURCE) {
            val cause: IOException = error.sourceException
            Timber.d("Player error occurred, cause is $cause")
        }
    }
}
