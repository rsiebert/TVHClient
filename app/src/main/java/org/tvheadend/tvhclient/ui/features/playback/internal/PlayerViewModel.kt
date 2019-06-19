package org.tvheadend.tvhclient.ui.features.playback.internal

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.upstream.DefaultAllocator
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.video.VideoListener
import org.tvheadend.htsp.HtspConnection
import org.tvheadend.htsp.HtspConnectionStateListener
import org.tvheadend.tvhclient.MainApplication
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.data.repository.AppRepository
import org.tvheadend.tvhclient.ui.features.playback.internal.utils.Rational
import timber.log.Timber
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import javax.inject.Inject

class PlayerViewModel(application: Application) : AndroidViewModel(application), HtspConnectionStateListener, VideoListener, Player.EventListener {

    @Inject
    lateinit var appContext: Context
    @Inject
    lateinit var sharedPreferences: SharedPreferences
    @Inject
    lateinit var appRepository: AppRepository

    // Connection related
    private val execService: ScheduledExecutorService = Executors.newScheduledThreadPool(10)
    private val htspConnection: HtspConnection
    private var htspSubscriptionDataSourceFactory: HtspSubscriptionDataSource.Factory? = null
    private var htspFileInputStreamDataSourceFactory: HtspFileInputStreamDataSource.Factory? = null
    private var dataSource: HtspDataSourceInterface? = null

    // Player and helpers
    val player: SimpleExoPlayer
    val trackSelector: DefaultTrackSelector
    val adaptiveTrackSelectionFactory = AdaptiveTrackSelection.Factory(DefaultBandwidthMeter())

    // Video dimension and aspect ratio related properties
    val videoAspectRatio: MutableLiveData<Rational> = MutableLiveData()

    // Observable fields
    var playerState: MutableLiveData<Int> = MutableLiveData()
    var playerIsPlaying: MutableLiveData<Boolean> = MutableLiveData()
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

    var pipModeActive : Boolean = false

    init {
        Timber.d("Initializing view model")
        MainApplication.component.inject(this)

        isConnected.postValue(false)
        playerIsPlaying.postValue(false)
        playerState.postValue(Player.STATE_IDLE)

        Timber.d("Starting connection")
        val connection = appRepository.connectionData.activeItem
        val connectionTimeout = Integer.valueOf(sharedPreferences.getString("connection_timeout", application.resources.getString(R.string.pref_default_connection_timeout))!!) * 1000
        htspConnection = HtspConnection(
                connection.username ?: "",
                connection.password ?: "",
                connection.hostname ?: "",
                connection.port,
                connectionTimeout,
                this, null)

        execService.execute {
            htspConnection.openConnection()
            htspConnection.authenticate()
        }

        trackSelector = DefaultTrackSelector(AdaptiveTrackSelection.Factory(null))
        if (sharedPreferences.getBoolean("audio_tunneling_enabled", appContext.resources.getBoolean(R.bool.pref_default_audio_tunneling_enabled))) {
            trackSelector.setTunnelingAudioSessionId(C.generateAudioSessionIdV21(appContext))
        }

        Timber.d("Creating load control")
        val bufferTimeText = sharedPreferences.getString("buffer_playback_ms", appContext.resources.getString(R.string.pref_default_buffer_playback_ms))
        val bufferTime = bufferTimeText!!.toInt()
        val loadControl = DefaultLoadControl(
                DefaultAllocator(true, C.DEFAULT_BUFFER_SEGMENT_SIZE),
                DefaultLoadControl.DEFAULT_MIN_BUFFER_MS,
                DefaultLoadControl.DEFAULT_MAX_BUFFER_MS,
                bufferTime,
                DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS,
                C.DEFAULT_BUFFER_SEGMENT_SIZE,
                true)

        Timber.d("Creating player instance")
        player = ExoPlayerFactory.newSimpleInstance(TvheadendRenderersFactory(appContext), trackSelector, loadControl)
        player.addVideoListener(this)
        player.addListener(this)

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
        loadMediaSourceForChannel(bundle?.getInt("channelId") ?: 0)
        loadMediaSourceForRecording(bundle?.getInt("dvrId") ?: 0)

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

            Timber.d("Preparing player with media source")
            player.prepare(ExtractorMediaSource.Factory(htspSubscriptionDataSourceFactory)
                    .setExtractorsFactory(TvheadendExtractorsFactory())
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
            player.prepare(ExtractorMediaSource.Factory(htspFileInputStreamDataSourceFactory)
                    .setExtractorsFactory(TvheadendExtractorsFactory())
                    .createMediaSource(Uri.parse("htsp://dvrfile/$recordingId")))
            player.playWhenReady = true
        }
    }

    private fun releaseMediaSource() {
        Timber.d("Releasing previous media source")
        player.stop()
        trackSelector.clearSelectionOverrides()
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

    override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters?) {
        // NOP
    }

    override fun onSeekProcessed() {
        // NOP
    }

    override fun onTracksChanged(trackGroups: TrackGroupArray?, trackSelections: TrackSelectionArray?) {
        // NOP
    }

    override fun onPlayerError(error: ExoPlaybackException?) {
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

    override fun onTimelineChanged(timeline: Timeline?, manifest: Any?, reason: Int) {
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

        val time = Math.max(currentTime + offset, startTime)
        val seekPts = time * 1000 - timeshiftStartTime
        return Math.max(seekPts, timeshiftStartPts) / 1000
    }
}
