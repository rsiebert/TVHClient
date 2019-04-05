package org.tvheadend.tvhclient.ui.features.playback.internal

import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Point
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.afollestad.materialdialogs.MaterialDialog
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerControlView
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.exo_player_control_view.*
import kotlinx.android.synthetic.main.exo_player_view.*
import kotlinx.android.synthetic.main.player_overlay_view.*
import org.tvheadend.tvhclient.MainApplication
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.data.repository.AppRepository
import org.tvheadend.tvhclient.ui.common.*
import org.tvheadend.tvhclient.ui.features.MainActivity
import org.tvheadend.tvhclient.ui.features.playback.internal.utils.Rational
import org.tvheadend.tvhclient.ui.features.playback.internal.utils.TrackSelectionHelper
import org.tvheadend.tvhclient.util.getIconUrl
import org.tvheadend.tvhclient.util.getThemeId
import timber.log.Timber
import java.util.*
import javax.inject.Inject

class PlaybackActivity : AppCompatActivity(), PlayerControlView.VisibilityListener {

    @Inject
    lateinit var sharedPreferences: SharedPreferences
    @Inject
    lateinit var appRepository: AppRepository

    private var timeshiftSupported: Boolean = false
    private lateinit var viewModel: PlayerViewModel

    private val videoAspectRatioNameList = Arrays.asList("5:4 (1.25:1)", "4:3 (1.3:1)", "16:9 (1.7:1)", "16:10 (1.6:1)")
    private val videoAspectRatioList = listOf(Rational(5, 4), Rational(4, 3), Rational(16, 9), Rational(16, 10))
    private var selectedVideoAspectRatio: Rational? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(getThemeId(this))
        super.onCreate(savedInstanceState)
        setContentView(R.layout.player_overlay_view)
        Timber.d("Creating")

        MainApplication.getComponent().inject(this)

        timeshiftSupported = sharedPreferences.getBoolean("timeshift_enabled", resources.getBoolean(R.bool.pref_default_timeshift_enabled))

        status.setText(R.string.connecting_to_server)
        player_rewind?.invisible()
        player_pause?.invisible()
        player_play?.invisible()
        player_forward?.invisible()

        player_play?.setOnClickListener { onPlayButtonSelected() }
        player_pause?.setOnClickListener { onPauseButtonSelected() }
        player_rewind?.setOnClickListener { onRewindButtonSelected() }
        player_forward?.setOnClickListener { onForwardButtonSelected() }
        player_menu?.setOnClickListener { onMenuButtonSelected() }
        player_menu_aspect_ratio?.setOnClickListener { onChangeAspectRatioSelected() }

        Timber.d("Getting view model")
        viewModel = ViewModelProviders.of(this).get(PlayerViewModel::class.java)
        viewModel.player.setVideoSurfaceView(exo_player_surface_view)
        player_view.player = viewModel.player

        Timber.d("Observing authentication status")
        viewModel.isConnected.observe(this, Observer { isConnected ->
            if (isConnected) {
                Timber.d("Connected to server")
                status.setText(R.string.connected_to_server)
                viewModel.loadMediaSource(intent.extras)
            } else {
                Timber.d("Not connected to server")
                status.setText(R.string.connection_failed)
            }
        })

        Timber.d("Observing video aspect ratio value")
        viewModel.videoAspectRatio.observe(this, Observer { ratio ->
            Timber.d("Received video aspect ratio value")
            selectedVideoAspectRatio = ratio
            updateVideoAspectRatio(ratio)
        })

        Timber.d("Observing player playback state")
        viewModel.playerState.observe(this, Observer { state ->
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
        viewModel.playerIsPlaying.observe(this, Observer { isPlaying ->
            Timber.d("Received player is playing $isPlaying")
            player_play?.visibleOrInvisible(!isPlaying)
            player_pause?.visibleOrInvisible(isPlaying)
            player_forward?.visibleOrInvisible(isPlaying && timeshiftSupported)
            player_rewind?.visibleOrInvisible(isPlaying && timeshiftSupported)
        })

        Timber.d("Observing playback information")
        viewModel.channelIcon.observe(this, Observer { icon ->
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

        viewModel.channelName.observe(this, Observer { channelName ->
            Timber.d("Received channel name $channelName")
            channel_name?.text = channelName
        })
        viewModel.title.observe(this, Observer { title ->
            Timber.d("Received title $title")
            program_title?.text = title
        })
        viewModel.subtitle.observe(this, Observer { subtitle ->
            Timber.d("Received subtitle $subtitle")
            program_subtitle?.text = subtitle
            program_subtitle?.visibleOrGone(!subtitle.isEmpty())
        })
        viewModel.nextTitle.observe(this, Observer { nextTitle ->
            Timber.d("Received next title $nextTitle")
            next_program_title?.text = nextTitle
            next_program_title?.visibleOrGone(!nextTitle.isEmpty())
        })
        viewModel.elapsedTime.observe(this, Observer { elapsedTime ->
            Timber.d("Received elapsed time $elapsedTime")
            elapsed_time?.text = elapsedTime
        })
        viewModel.remainingTime.observe(this, Observer { remainingTime ->
            Timber.d("Received remaining time $remainingTime")
            remaining_time?.text = remainingTime
        })
    }

    override fun attachBaseContext(context: Context) {
        super.attachBaseContext(onAttach(context))
    }

    override fun onNewIntent(intent: Intent) {
        Timber.d("New intent")
        setIntent(intent)

        Timber.d("Getting channel id or recording id from bundle")
        viewModel.loadMediaSource(intent.extras)
    }

    override fun onStop() {
        Timber.d("Stopping")
        viewModel.pause()
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        Timber.d("Resuming")
        viewModel.play()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Timber.d("Configuration changed")
        val ratio = selectedVideoAspectRatio
        if (ratio != null) {
            updateVideoAspectRatio(ratio)
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

    fun onPauseButtonSelected() {
        Timber.d("Pause button selected")
        viewModel.pause()
    }

    fun onPlayButtonSelected() {
        Timber.d("Play button selected")
        viewModel.play()
    }

    fun onMenuButtonSelected() {
        Timber.d("Menu button selected")

        val popupMenu = PopupMenu(this, player_menu)
        popupMenu.menuInflater.inflate(R.menu.player_popup_menu, popupMenu.menu)

        val mappedTrackInfo = viewModel.trackSelector.currentMappedTrackInfo
        if (mappedTrackInfo != null) {
            for (i in 0 until mappedTrackInfo.length) {
                val trackGroups = mappedTrackInfo.getTrackGroups(i)
                if (trackGroups.length != 0) {
                    when (viewModel.player.getRendererType(i)) {
                        C.TRACK_TYPE_AUDIO -> {
                            Timber.d("Track renderer type for index $i is audio, showing audio menu")
                            popupMenu.menu.findItem(R.id.menu_audio)?.isVisible = true
                        }
                        C.TRACK_TYPE_VIDEO -> {
                            Timber.d("Track renderer type for index $i is video")
                            //popupMenu.menu.findItem(R.id.menu_video)?.isVisible = true
                        }
                        C.TRACK_TYPE_TEXT -> {
                            Timber.d("Track renderer type for index $i is text, showing subtitle menu")
                            popupMenu.menu.findItem(R.id.menu_subtitle)?.isVisible = true
                        }
                    }
                }
            }

            Timber.d("Adding popup menu listener")
            popupMenu.setOnMenuItemClickListener { item ->
                val trackSelectionHelper = TrackSelectionHelper(viewModel.trackSelector, viewModel.adaptiveTrackSelectionFactory)
                when (item.itemId) {
                    R.id.menu_audio -> {
                        trackSelectionHelper.showSelectionDialog(this, "Audio", mappedTrackInfo, C.TRACK_TYPE_AUDIO)
                        return@setOnMenuItemClickListener true
                    }
                    R.id.menu_subtitle -> {
                        trackSelectionHelper.showSelectionDialog(this, "Subtitles", mappedTrackInfo, C.TRACK_TYPE_TEXT)
                        return@setOnMenuItemClickListener true
                    }
                    else -> {
                        return@setOnMenuItemClickListener false
                    }
                }
            }
            popupMenu.show()
        }
    }

    fun onChangeAspectRatioSelected() {
        Timber.d("Change aspect ratio button selected")
        MaterialDialog.Builder(this)
                .title("Select the video aspect ratio")
                .items(videoAspectRatioNameList)
                .itemsCallbackSingleChoice(-1) { _, _, which, _ ->
                    Timber.d("Selected aspect ratio index is $which")
                    viewModel.setVideoAspectRatio(videoAspectRatioList[which])
                    true
                }
                .show()
    }

    fun onRewindButtonSelected() {
        Timber.d("Rewind button selected")
        viewModel.seekBackward()
    }

    fun onForwardButtonSelected() {
        Timber.d("Forward button selected")
        viewModel.seekForward()
    }

    override fun onVisibilityChange(visibility: Int) {
        Timber.d("Visibility changed to $visibility")
        if (visibility != View.VISIBLE) {
            if (Build.VERSION.SDK_INT >= 19) {
                val decorView = window.decorView
                decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE
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
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
    }

    override fun finish() {
        super.finish()
        Timber.d("Finishing")
        startActivity(Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
    }
}