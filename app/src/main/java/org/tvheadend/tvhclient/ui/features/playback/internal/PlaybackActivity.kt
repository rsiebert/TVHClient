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
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.google.android.exoplayer2.Player
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.exo_player_control_view.*
import kotlinx.android.synthetic.main.exo_player_view.*
import kotlinx.android.synthetic.main.player_overlay_view.*
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.ui.common.onAttach
import org.tvheadend.tvhclient.ui.common.setOptionalDescriptionText
import org.tvheadend.tvhclient.ui.features.MainActivity
import org.tvheadend.tvhclient.ui.features.playback.internal.utils.Rational
import org.tvheadend.tvhclient.ui.features.playback.internal.utils.TrackInformationDialog
import org.tvheadend.tvhclient.ui.features.playback.internal.utils.TrackSelectionDialog
import org.tvheadend.tvhclient.util.extensions.*
import org.tvheadend.tvhclient.util.getIconUrl
import org.tvheadend.tvhclient.util.getThemeId
import timber.log.Timber

// TODO set aspect ratio in SD content to 16:9 as a default
// TODO disable subtitles as a default

class PlaybackActivity : AppCompatActivity() {

    private var timeshiftSupported: Boolean = false
    private lateinit var viewModel: PlayerViewModel

    private val videoAspectRatioNameList = listOf("5:4", "4:3", "16:9", "16:10", "18:9")
    private val videoAspectRatioList = listOf(Rational(5, 4), Rational(4, 3), Rational(16, 9), Rational(16, 10), Rational(18, 9))
    private var selectedVideoAspectRatio: Rational? = null
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
        setContentView(R.layout.player_overlay_view)
        Timber.d("Creating")

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

        status.setText(R.string.connecting_to_server)
        player_rewind?.invisible()
        player_pause?.invisible()
        player_play?.invisible()
        player_forward?.invisible()
        play_next_channel?.invisible()
        play_previous_channel?.invisible()

        player_play?.setOnClickListener { onPlayButtonSelected() }
        player_pause?.setOnClickListener { onPauseButtonSelected() }
        player_rewind?.setOnClickListener { onRewindButtonSelected() }
        player_forward?.setOnClickListener { onForwardButtonSelected() }
        player_settings?.setOnClickListener { onSettingsButtonSelected() }
        player_information?.setOnClickListener { onInformationButtonSelected() }
        player_aspect_ratio?.setOnClickListener { onChangeAspectRatioSelected() }
        player_toggle_fullscreen?.setOnClickListener { onToggleFullscreenSelected() }
        play_next_channel?.setOnClickListener { onPlayNextChannelButtonSelected() }
        play_previous_channel?.setOnClickListener { onPlayPreviousChannelButtonSelected() }

        Timber.d("Getting view model")
        viewModel = ViewModelProvider(this).get(PlayerViewModel::class.java)
        viewModel.player.setVideoSurfaceView(exo_player_surface_view)
        player_view.player = viewModel.player

        Timber.d("Observing authentication status")
        viewModel.isConnected.observe(this,  { isConnected ->
            if (isConnected) {
                Timber.d("Connected to server")
                status.setText(R.string.connected_to_server)
                viewModel.loadMediaSource(applicationContext, intent.extras)
            } else {
                Timber.d("Not connected to server")
                status.setText(R.string.connection_failed)
            }
        })

        Timber.d("Observing video aspect ratio value")
        viewModel.videoAspectRatio.observe(this,  { ratio ->
            Timber.d("Received video aspect ratio value")
            selectedVideoAspectRatio = ratio
            updateVideoAspectRatio(ratio)
        })

        Timber.d("Observing player playback state")
        viewModel.playerState.observe(this,  { state ->
            Timber.d("Received player playback state $state")
            when (state) {
                Player.STATE_IDLE -> {
                    status?.visible()
                    exo_player_surface_view?.gone()
                }
                Player.STATE_BUFFERING -> {
                    status?.visible()
                    exo_player_surface_view?.gone()
                    status?.setText(R.string.player_is_loading_more_data)
                }
                Player.STATE_READY, Player.STATE_ENDED -> {
                    status?.gone()
                    exo_player_surface_view?.visible()
                }
            }
        })

        Timber.d("Observing player is playing state")
        viewModel.playerIsPlaying.observe(this,  { isPlaying ->
            Timber.d("Received player is playing $isPlaying")
            player_play?.visibleOrInvisible(!isPlaying)
            player_pause?.visibleOrInvisible(isPlaying)
            player_forward?.visibleOrInvisible(isPlaying && timeshiftSupported)
            player_rewind?.visibleOrInvisible(isPlaying && timeshiftSupported)
        })

        Timber.d("Observing live TV playing")
        viewModel.liveTvIsPlaying.observe(this,  { isPlaying ->
            Timber.d("Received live TV is playing $isPlaying")
            play_previous_channel?.visibleOrGone(isPlaying)
            play_next_channel?.visibleOrGone(isPlaying)
        })

        Timber.d("Observing playback information")
        viewModel.channelIcon.observe(this,  { icon ->
            Timber.d("Received channel icon $icon")
            Picasso.get()
                    .load(getIconUrl(this, icon))
                    .into(channel_icon, object : Callback {
                        override fun onSuccess() {
                            channel_name?.gone()
                            channel_icon?.visible()
                        }

                        override fun onError(e: Exception) {
                            channel_name?.visible()
                            channel_icon?.gone()
                        }
                    })
        })

        viewModel.channelName.observe(this,  { channelName ->
            Timber.d("Received channel name $channelName")
            channel_name?.text = if (!channelName.isNullOrEmpty()) channelName else getString(R.string.all_channels)
        })
        viewModel.title.observe(this,  { title ->
            Timber.d("Received title $title")
            setOptionalDescriptionText(program_title, title)
        })
        viewModel.subtitle.observe(this,  { subtitle ->
            Timber.d("Received subtitle $subtitle")
            setOptionalDescriptionText(program_subtitle, subtitle)
            program_subtitle?.visibleOrGone(subtitle.isNotEmpty())
        })
        viewModel.nextTitle.observe(this,  { nextTitle ->
            Timber.d("Received next title $nextTitle")
            setOptionalDescriptionText(next_program_title, nextTitle)
            next_program_title?.visibleOrGone(nextTitle.isNotEmpty())
        })
        viewModel.elapsedTime.observe(this,  { elapsedTime ->
            elapsed_time?.text = elapsedTime
        })
        viewModel.remainingTime.observe(this,  { remainingTime ->
            remaining_time?.text = remainingTime
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
                player_toggle_fullscreen?.setImageResource(R.drawable.ic_player_fullscreen)
            }
            Configuration.ORIENTATION_LANDSCAPE -> {
                Timber.d("Player is in landscape mode")
                player_toggle_fullscreen?.setImageResource(R.drawable.ic_player_fullscreen_exit)
            }
        }

    }

    private fun updateVideoAspectRatio(videoAspectRatio: Rational) {
        Timber.d("Updating video dimensions")

        val display = windowManager.defaultDisplay
        val size = Point()
        display.getSize(size)
        val screenWidth = size.x
        val screenHeight = size.y

        var width = videoAspectRatio.numerator
        var height = videoAspectRatio.denominator
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

        exo_player_frame?.let {
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
        val width = selectedVideoAspectRatio?.numerator ?: 0
        val height = selectedVideoAspectRatio?.denominator ?: 1
        val currentRatio = (width.toFloat() / height.toFloat()).toString().subStringUntilOrLess(4)

        Timber.d("Current video dimensions are $width:$height, current ratio: $currentRatio:1, selected index $selectedVideoAspectIndex")
        if (selectedVideoAspectIndex == -1) {
            videoAspectRatioList.forEachIndexed { index, value ->
                val ratio = (value.numerator.toFloat() / value.denominator.toFloat()).toString().subStringUntilOrLess(4)
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
                Timber.d("Entering PIP mode with ratio ${ratio.numerator}:${ratio.denominator}")
                // Set the value already here because the onPause method is called before the onPictureInPictureModeChanged.
                // This would pause the player before we know that the PIP mode has been entered.
                viewModel.pipModeActive = true
                enterPictureInPictureMode(
                        PictureInPictureParams.Builder()
                                .setAspectRatio(android.util.Rational(ratio.numerator, ratio.denominator))
                                .build())
            }
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        Timber.d("PIP mode entered $isInPictureInPictureMode")
        player_view.useController = !isInPictureInPictureMode
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