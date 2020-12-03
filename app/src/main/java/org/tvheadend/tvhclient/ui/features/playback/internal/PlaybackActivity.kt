package org.tvheadend.tvhclient.ui.features.playback.internal

import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Point
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.view.Surface
import android.view.SurfaceView
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerView
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.ui.common.onAttach
import org.tvheadend.tvhclient.ui.common.setOptionalDescriptionText
import org.tvheadend.tvhclient.ui.features.MainActivity
import org.tvheadend.tvhclient.ui.features.playback.internal.utils.TrackInformationDialog
import org.tvheadend.tvhclient.ui.features.playback.internal.utils.TrackSelectionDialog
import org.tvheadend.tvhclient.ui.features.playback.internal.utils.VideoAspect
import org.tvheadend.tvhclient.util.extensions.*
import org.tvheadend.tvhclient.util.getIconUrl
import org.tvheadend.tvhclient.util.getThemeId
import timber.log.Timber

// TODO disable subtitles as a default

class PlaybackActivity : AppCompatActivity() {

    private lateinit var playerStatus: TextView
    private lateinit var playerView: PlayerView
    private lateinit var exoPlayerFrame: FrameLayout
    private lateinit var exoPlayerSurfaceView: SurfaceView
    private lateinit var remainingTime: TextView
    private lateinit var elapsedTime: TextView
    private lateinit var nextProgramTitle: TextView
    private lateinit var programSubtitle: TextView
    private lateinit var programTitle: TextView
    private lateinit var channelName: TextView
    private lateinit var channelIcon: ImageView
    private lateinit var playerSettings: ImageButton
    private lateinit var playerInformation: ImageButton
    private lateinit var playerToggleFullscreen: ImageButton
    private lateinit var playerAspectRatio: ImageButton
    private lateinit var playPreviousChannel: ImageButton
    private lateinit var playNextChannel: ImageButton
    private lateinit var playerForward: ImageButton
    private lateinit var playerPlay: ImageButton
    private lateinit var playerPause: ImageButton
    private lateinit var playerRewind: ImageButton

    private var timeshiftSupported: Boolean = false
    private lateinit var viewModel: PlayerViewModel

    private val videoAspectRatioNameList = listOf("5:4", "4:3", "16:9", "16:10", "18:9")
    private val videoAspectRatioList = listOf(VideoAspect(5, 4), VideoAspect(4, 3), VideoAspect(16, 9), VideoAspect(16, 10), VideoAspect(18, 9))
    private var selectedVideoAspectRatio: VideoAspect? = null
    private var selectedVideoAspectIndex = -1

    private var orientationSensorListener: SensorEventListener? = null
    private var sensorManager: SensorManager? = null
    private var orientation: Sensor? = null
    private var forceOrientation = false
    private var value0 = -10000f
    private var value1 = -10000f

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(getThemeId(this))
        super.onCreate(savedInstanceState)
        //playerBinding = PlayerOverlayViewBinding.inflate(layoutInflater)
        //playerControlBinding = ExoPlayerControlViewBinding.inflate(layoutInflater)
        //playerViewBinding = ExoPlayerViewBinding.inflate(layoutInflater)
        //val view = playerBinding.root
        setContentView(R.layout.player_overlay_view)
        Timber.d("Creating")

        playerView = findViewById<View>(R.id.player_view) as PlayerView
        playerStatus = findViewById<View>(R.id.player_status) as TextView

        exoPlayerSurfaceView = findViewById<View>(R.id.exo_player_surface_view) as SurfaceView
        exoPlayerFrame = findViewById<View>(R.id.exo_player_frame) as FrameLayout

        channelIcon = findViewById<View>(R.id.channel_icon) as ImageView
        channelName = findViewById<View>(R.id.channel_name) as TextView
        programTitle = findViewById<View>(R.id.program_title) as TextView
        programSubtitle = findViewById<View>(R.id.program_subtitle) as TextView
        nextProgramTitle = findViewById<View>(R.id.next_program_title) as TextView
        elapsedTime = findViewById<View>(R.id.elapsed_time) as TextView
        remainingTime = findViewById<View>(R.id.remaining_time) as TextView

        playerRewind = findViewById<View>(R.id.player_rewind) as ImageButton
        playerPause = findViewById<View>(R.id.player_pause) as ImageButton
        playerPlay = findViewById<View>(R.id.player_play) as ImageButton
        playerForward = findViewById<View>(R.id.player_forward) as ImageButton
        playNextChannel = findViewById<View>(R.id.play_next_channel) as ImageButton
        playPreviousChannel = findViewById<View>(R.id.play_previous_channel) as ImageButton
        playerAspectRatio = findViewById<View>(R.id.player_aspect_ratio) as ImageButton
        playerToggleFullscreen = findViewById<View>(R.id.player_toggle_fullscreen) as ImageButton
        playerInformation = findViewById<View>(R.id.player_information) as ImageButton
        playerSettings = findViewById<View>(R.id.player_settings) as ImageButton

        timeshiftSupported = PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean("timeshift_enabled", resources.getBoolean(R.bool.pref_default_timeshift_enabled))

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager?
        orientation = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        orientationSensorListener = object : SensorEventListener {
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // NOP
            }

            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    if (value0 == it.values[0] && value1 == it.values[1]) {
                        return
                    }
                    val orientation = -1
                    var value = orientation

                    if (value0 < 0 && it.values[0] > 0) {
                        // Setting rotation to 270°: Landscape reverse
                        value = Surface.ROTATION_270
                    } else if (value0 > 0 && it.values[0] < 0) {
                        // Setting rotation to 90°: Landscape
                        value = Surface.ROTATION_90
                    } else if (value1 < 0 && it.values[1] > 0) {
                        // Setting rotation to 180°: Portrait reverse
                        value = Surface.ROTATION_180
                    } else if (value1 > 0 && it.values[1] < 0) {
                        // Setting rotation to 0°: Portrait
                        value = Surface.ROTATION_0
                    }

                    if (orientation != value && !forceOrientation) {
                        if ((requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                        && (value == Surface.ROTATION_90 || value == Surface.ROTATION_270))
                                || (requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                        && (value == Surface.ROTATION_0 || value == Surface.ROTATION_180))) {
                            Timber.d("Changing orientation")
                            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                        }
                    }

                    value0 = it.values[0]
                    value1 = it.values[1]
                }
            }
        }

        playerStatus.setText(R.string.connecting_to_server)
        playerRewind.invisible()
        playerPause.invisible()
        playerPlay.invisible()
        playerForward.invisible()
        playNextChannel.invisible()
        playPreviousChannel.invisible()
        playerAspectRatio.invisible()
        playerToggleFullscreen.invisible()
        playerInformation.invisible()
        playerSettings.invisible()

        playerPlay.setOnClickListener { onPlayButtonSelected() }
        playerPause.setOnClickListener { onPauseButtonSelected() }
        playerRewind.setOnClickListener { onRewindButtonSelected() }
        playerForward.setOnClickListener { onForwardButtonSelected() }
        playerSettings.setOnClickListener { onSettingsButtonSelected() }
        playerInformation.setOnClickListener { onInformationButtonSelected() }
        playerAspectRatio.setOnClickListener { onChangeAspectRatioSelected() }
        playerToggleFullscreen.setOnClickListener { onToggleFullscreenSelected() }
        playNextChannel.setOnClickListener { onPlayNextChannelButtonSelected() }
        playPreviousChannel.setOnClickListener { onPlayPreviousChannelButtonSelected() }

        Timber.d("Getting view model")
        viewModel = ViewModelProvider(this).get(PlayerViewModel::class.java)
        viewModel.player.setVideoSurfaceView(exoPlayerSurfaceView)
        playerView.player = viewModel.player

        Timber.d("Observing authentication status")
        viewModel.isConnected.observe(this, { isConnected ->
            if (isConnected) {
                if (!viewModel.isPlaybackProfileSelected(intent.extras)) {
                    Timber.d("No playback profile was selected")
                    playerStatus.setText(R.string.no_playback_profile_selected)
                } else {
                    Timber.d("Connected to server")
                    playerStatus.setText(R.string.connected_to_server)
                    viewModel.loadMediaSource(applicationContext, intent.extras)
                }
            } else {
                Timber.d("Not connected to server")
                playerStatus.setText(R.string.connection_failed)
            }
        })

        Timber.d("Observing video aspect ratio value")
        viewModel.videoAspectRatio.observe(this, { ratio ->
            Timber.d("Received video aspect ratio value")
            selectedVideoAspectRatio = ratio
            updateVideoAspectRatio(ratio)
        })

        Timber.d("Observing player playback state")
        viewModel.playerState.observe(this, { state ->
            Timber.d("Received player playback state $state")
            when (state) {
                Player.STATE_IDLE -> {
                    playerStatus.visible()
                    exoPlayerSurfaceView.gone()
                }
                Player.STATE_BUFFERING -> {
                    playerStatus.visible()
                    exoPlayerSurfaceView.gone()
                    playerStatus.setText(R.string.player_is_loading_more_data)
                }
                Player.STATE_READY, Player.STATE_ENDED -> {
                    playerStatus.gone()
                    exoPlayerSurfaceView.visible()

                    playerAspectRatio.visible()
                    playerToggleFullscreen.visible()
                    playerInformation.visible()
                    playerSettings.visible()
                }
            }
        })

        Timber.d("Observing player is playing state")
        viewModel.playerIsPlaying.observe(this, { isPlaying ->
            Timber.d("Received player is playing $isPlaying")
            playerPlay.visibleOrInvisible(!isPlaying)
            playerPause.visibleOrInvisible(isPlaying)
            playerForward.visibleOrInvisible(isPlaying && timeshiftSupported)
            playerRewind.visibleOrInvisible(isPlaying && timeshiftSupported)
        })

        Timber.d("Observing live TV playing")
        viewModel.liveTvIsPlaying.observe(this, { isPlaying ->
            Timber.d("Received live TV is playing $isPlaying")
            playPreviousChannel.visibleOrGone(isPlaying)
            playNextChannel.visibleOrGone(isPlaying)
        })

        Timber.d("Observing playback information")
        viewModel.channelIcon.observe(this, { icon ->
            Timber.d("Received channel icon $icon")
            Picasso.get()
                    .load(getIconUrl(this, icon))
                    .into(channelIcon, object : Callback {
                        override fun onSuccess() {
                            channelName.gone()
                            channelIcon.visible()
                        }

                        override fun onError(e: Exception) {
                            channelName.visible()
                            channelIcon.gone()
                        }
                    })
        })

        viewModel.channelName.observe(this, { name ->
            Timber.d("Received channel name $name")
            channelName.text = if (!name.isNullOrEmpty()) name else getString(R.string.all_channels)
        })
        viewModel.title.observe(this, { title ->
            Timber.d("Received title $title")
            setOptionalDescriptionText(programTitle, title)
        })
        viewModel.subtitle.observe(this, { subtitle ->
            Timber.d("Received subtitle $subtitle")
            setOptionalDescriptionText(programSubtitle, subtitle)
            programSubtitle.visibleOrGone(subtitle.isNotEmpty())
        })
        viewModel.nextTitle.observe(this, { nextTitle ->
            Timber.d("Received next title $nextTitle")
            setOptionalDescriptionText(nextProgramTitle, nextTitle)
            nextProgramTitle.visibleOrGone(nextTitle.isNotEmpty())
        })
        viewModel.elapsedTime.observe(this, { time ->
            elapsedTime.text = time
        })
        viewModel.remainingTime.observe(this, { time ->
            remainingTime.text = time
        })
    }

    override fun attachBaseContext(context: Context) {
        super.attachBaseContext(onAttach(context))
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Timber.d("New intent")
        setIntent(intent)

        Timber.d("Getting channel id or recording id from bundle")
        viewModel.loadMediaSource(applicationContext, intent.extras)
    }

    override fun onPause() {
        Timber.d("Pausing")
        viewModel.pause()
        if (orientation != null) {
            sensorManager?.unregisterListener(orientationSensorListener)
        }
        super.onPause()
    }

    override fun onStop() {
        super.onStop()
        Timber.d("Stopping")
        if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                && packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
            Timber.d("Finishing playback activity")
            viewModel.stopPlaybackAndReleaseMediaSource()
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        Timber.d("Resuming")
        if (orientation != null) {
            sensorManager?.registerListener(orientationSensorListener, orientation, SensorManager.SENSOR_DELAY_GAME)
        }
        viewModel.play()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Timber.d("Configuration changed")
        val ratio = selectedVideoAspectRatio
        if (ratio != null) {
            updateVideoAspectRatio(ratio)
        }
        when (newConfig.orientation) {
            Configuration.ORIENTATION_PORTRAIT -> {
                Timber.d("Player is in portrait mode")
                playerToggleFullscreen.setImageResource(R.drawable.ic_player_fullscreen)
            }
            Configuration.ORIENTATION_LANDSCAPE -> {
                Timber.d("Player is in landscape mode")
                playerToggleFullscreen.setImageResource(R.drawable.ic_player_fullscreen_exit)
            }
        }

    }

    private fun updateVideoAspectRatio(videoAspect: VideoAspect) {
        Timber.d("Updating video dimensions")

        val display = windowManager.defaultDisplay
        val size = Point()
        display.getSize(size)
        val screenWidth = size.x
        val screenHeight = size.y

        var width = videoAspect.width
        var height = videoAspect.height
        val ratio = width.toFloat() / height.toFloat()
        val orientation = resources.configuration.orientation

        Timber.d("Current video dimensions are $width:$height, ratio: $ratio")

        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            if (width != screenWidth) {
                width = screenWidth
            }
            height = (width.toFloat() / ratio).toInt()
            Timber.d("New portrait video dimensions are $width:$height")

        } else if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            if (height != screenHeight) {
                height = screenHeight
            }
            width = (height.toFloat() * ratio).toInt()
            Timber.d("New landscape video dimensions are $width:$height")
        }

        exoPlayerFrame.let {
            val layoutParams = it.layoutParams
            layoutParams.width = width
            layoutParams.height = height
            it.layoutParams = layoutParams
            it.requestLayout()
        }
    }

    private fun onPauseButtonSelected() {
        Timber.d("Pause button selected")
        viewModel.pause()
    }

    private fun onPlayButtonSelected() {
        Timber.d("Play button selected")
        viewModel.play()
    }

    private fun onSettingsButtonSelected() {
        Timber.d("Settings button selected")
        if (TrackSelectionDialog.willHaveContent(viewModel.trackSelector)) {
            val trackSelectionDialog = TrackSelectionDialog.createForTrackSelector(viewModel.trackSelector)
            trackSelectionDialog.show(supportFragmentManager, null)
        }
    }

    private fun onInformationButtonSelected() {
        Timber.d("Information button selected")
        val trackInformationDialog = TrackInformationDialog.createForTrackSelector(viewModel.player)
        trackInformationDialog.show(supportFragmentManager, null)
    }

    private fun onChangeAspectRatioSelected() {
        Timber.d("Change aspect ratio button selected")
        val width = selectedVideoAspectRatio?.width ?: 0
        val height = selectedVideoAspectRatio?.height ?: 1
        val currentRatio = (width.toFloat() / height.toFloat()).toString().subStringUntilOrLess(4)

        Timber.d("Current video dimensions are $width:$height, current ratio: $currentRatio:1, selected index $selectedVideoAspectIndex")
        if (selectedVideoAspectIndex == -1) {
            videoAspectRatioList.forEachIndexed { index, value ->
                val ratio = (value.width.toFloat() / value.height.toFloat()).toString().subStringUntilOrLess(4)
                Timber.d("Current ratio: $currentRatio, ratio: $ratio")
                if (currentRatio == ratio) {
                    selectedVideoAspectIndex = index
                }
            }
        }

        MaterialDialog(this).show {
            title(text = "Select the video aspect ratio")
            listItemsSingleChoice(items = videoAspectRatioNameList.toList(), initialSelection = selectedVideoAspectIndex) { _, which, _ ->
                Timber.d("Selected aspect ratio index is $which")
                selectedVideoAspectIndex = which
                viewModel.setVideoAspectRatio(videoAspectRatioList[which])
            }
        }
    }

    private fun onToggleFullscreenSelected() {
        when (resources.configuration.orientation) {
            Configuration.ORIENTATION_PORTRAIT -> {
                forceOrientation = true
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
            Configuration.ORIENTATION_LANDSCAPE -> {
                forceOrientation = false
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        }
    }

    private fun onRewindButtonSelected() {
        Timber.d("Rewind button selected")
        viewModel.seekBackward()
    }

    private fun onForwardButtonSelected() {
        Timber.d("Forward button selected")
        viewModel.seekForward()
    }

    private fun onPlayPreviousChannelButtonSelected() {
        Timber.d("Play previous channel button selected")
        viewModel.playPreviousChannel(applicationContext)
    }

    private fun onPlayNextChannelButtonSelected() {
        Timber.d("Play next channel button selected")
        viewModel.playNextChannel(applicationContext)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        Timber.d("Window focus changed to $hasFocus")
        if (hasFocus) {
            if (Build.VERSION.SDK_INT >= 19) {
                window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        // Set the content to appear under the system bars so that the
                        // content doesn't resize when the system bars hide and show.
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        // Hide the nav bar and status bar
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN)
            }
        }
    }

    override fun onUserLeaveHint() {
        Timber.d("Checking if PIP mode can be entered")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ratio = selectedVideoAspectRatio
            if (ratio != null) {
                Timber.d("Entering PIP mode with ratio ${ratio.width}:${ratio.height}")
                // Set the value already here because the onPause method is called before the onPictureInPictureModeChanged.
                // This would pause the player before we know that the PIP mode has been entered.
                viewModel.pipModeActive = true
                enterPictureInPictureMode(
                        PictureInPictureParams.Builder()
                                .setAspectRatio(android.util.Rational(ratio.width, ratio.height))
                                .build())
            }
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        Timber.d("PIP mode entered $isInPictureInPictureMode")
        playerView.useController = !isInPictureInPictureMode
        viewModel.pipModeActive = isInPictureInPictureMode
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
    }

    override fun finish() {
        super.finish()
        Timber.d("Finishing")
        startActivity(Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
    }
}