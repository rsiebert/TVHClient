package org.tvheadend.tvhclient.ui.features.playback.internal

import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Point
import android.os.Build
import android.os.Bundle
import android.view.SurfaceView
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.afollestad.materialdialogs.MaterialDialog
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.DefaultTimeBar
import com.google.android.exoplayer2.ui.PlayerControlView
import com.google.android.exoplayer2.ui.PlayerView
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import org.tvheadend.tvhclient.MainApplication
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.data.repository.AppRepository
import org.tvheadend.tvhclient.ui.features.MainActivity
import org.tvheadend.tvhclient.ui.features.playback.internal.utils.TrackSelectionHelper
import org.tvheadend.tvhclient.ui.features.playback.internal.utils.Rational
import org.tvheadend.tvhclient.util.getIconUrl
import org.tvheadend.tvhclient.util.getThemeId
import org.tvheadend.tvhclient.ui.common.onAttach
import timber.log.Timber
import java.util.*
import javax.inject.Inject

class PlaybackActivity : AppCompatActivity(), PlayerControlView.VisibilityListener {

    @Inject
    lateinit var sharedPreferences: SharedPreferences
    @Inject
    lateinit var appRepository: AppRepository

    @BindView(R.id.exo_player_frame)
    lateinit var playerMainFrame: FrameLayout
    @BindView(R.id.exo_player_surface_view)
    lateinit var playerSurfaceView: SurfaceView
    @BindView(R.id.status)
    lateinit var statusTextView: TextView
    @BindView(R.id.player_view)
    lateinit var playerView: PlayerView
    @BindView(R.id.channel_icon)
    lateinit var iconImageView: ImageView
    @BindView(R.id.channel_name)
    lateinit var iconTextView: TextView
    @BindView(R.id.program_title)
    lateinit var titleTextView: TextView
    @BindView(R.id.program_subtitle)
    lateinit var subtitleTextView: TextView
    @BindView(R.id.next_program_title)
    lateinit var nextTitleTextView: TextView
    @BindView(R.id.progress)
    lateinit var progressBar: DefaultTimeBar
    @BindView(R.id.elapsed_time)
    lateinit var elapsedTimeTextView: TextView
    @BindView(R.id.remaining_time)
    lateinit var remainingTimeTextView: TextView

    @BindView(R.id.player_rewind)
    lateinit var rewindImageView: ImageButton
    @BindView(R.id.player_pause)
    lateinit var pauseImageView: ImageButton
    @BindView(R.id.player_play)
    lateinit var playImageView: ImageButton
    @BindView(R.id.player_forward)
    lateinit var forwardImageView: ImageButton

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
        ButterKnife.bind(this)

        timeshiftSupported = sharedPreferences.getBoolean("timeshift_enabled", resources.getBoolean(R.bool.pref_default_timeshift_enabled))

        statusTextView.setText(R.string.connecting_to_server)
        rewindImageView.visibility = View.INVISIBLE
        pauseImageView.visibility = View.INVISIBLE
        playImageView.visibility = View.INVISIBLE
        forwardImageView.visibility = View.INVISIBLE

        Timber.d("Getting view model")
        viewModel = ViewModelProviders.of(this).get(PlayerViewModel::class.java)
        viewModel.player.setVideoSurfaceView(playerSurfaceView)
        playerView.player = viewModel.player

        Timber.d("Observing authentication status")
        viewModel.isConnected.observe(this, Observer { isConnected ->
            if (isConnected) {
                Timber.d("Connected to server")
                statusTextView.setText(R.string.connected_to_server)
                viewModel.loadMediaSource(intent.extras)
            } else {
                Timber.d("Not connected to server")
                statusTextView.setText(R.string.connection_failed)
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
                    statusTextView.visibility = View.VISIBLE
                    playerSurfaceView.visibility = View.GONE
                }
                Player.STATE_BUFFERING -> {
                    statusTextView.visibility = View.VISIBLE
                    playerSurfaceView.visibility = View.GONE
                    statusTextView.setText(R.string.player_is_loading_more_data)
                }
                Player.STATE_READY, Player.STATE_ENDED -> {
                    statusTextView.visibility = View.GONE
                    playerSurfaceView.visibility = View.VISIBLE
                }
            }
        })

        Timber.d("Observing player is playing state")
        viewModel.playerIsPlaying.observe(this, Observer { isPlaying ->
            Timber.d("Received player is playing $isPlaying")
            playImageView.visibility = if (isPlaying) View.INVISIBLE else View.VISIBLE
            pauseImageView.visibility = if (isPlaying) View.VISIBLE else View.INVISIBLE
            forwardImageView.visibility = if (isPlaying && timeshiftSupported) View.VISIBLE else View.INVISIBLE
            rewindImageView.visibility = if (isPlaying && timeshiftSupported) View.VISIBLE else View.INVISIBLE
        })

        Timber.d("Observing playback information")
        viewModel.channelIcon.observe(this, Observer { icon ->
            Timber.d("Received channel icon $icon")
            Picasso.get()
                    .load(getIconUrl(this, icon))
                    .into(iconImageView, object : Callback {
                        override fun onSuccess() {
                            iconTextView.visibility = View.GONE
                            iconImageView.visibility = View.VISIBLE
                        }

                        override fun onError(e: Exception) {
                            iconTextView.visibility = View.VISIBLE
                            iconImageView.visibility = View.GONE
                        }
                    })
        })

        viewModel.channelName.observe(this, Observer { channelName ->
            Timber.d("Received channel name $channelName")
            iconTextView.text = channelName
        })
        viewModel.title.observe(this, Observer { title ->
            Timber.d("Received title $title")
            titleTextView.text = title
        })
        viewModel.subtitle.observe(this, Observer { subtitle ->
            Timber.d("Received subtitle $subtitle")
            subtitleTextView.text = subtitle
            subtitleTextView.visibility = if (subtitle.isEmpty()) View.GONE else View.VISIBLE
        })
        viewModel.nextTitle.observe(this, Observer { nextTitle ->
            Timber.d("Received next title $nextTitle")
            nextTitleTextView.text = nextTitle
            nextTitleTextView.visibility = if (nextTitle.isEmpty()) View.GONE else View.VISIBLE
        })
        viewModel.elapsedTime.observe(this, Observer { elapsedTime ->
            Timber.d("Received elapsed time $elapsedTime")
            elapsedTimeTextView.text = elapsedTime
        })
        viewModel.remainingTime.observe(this, Observer { remainingTime ->
            Timber.d("Received remaining time $remainingTime")
            remainingTimeTextView.text = remainingTime
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

        val layoutParams = playerMainFrame.layoutParams
        layoutParams.width = width
        layoutParams.height = height
        playerMainFrame.layoutParams = layoutParams
        playerMainFrame.requestLayout()
    }

    @OnClick(R.id.player_pause)
    fun onPauseButtonSelected() {
        Timber.d("Pause button selected")
        viewModel.pause()
    }

    @OnClick(R.id.player_play)
    fun onPlayButtonSelected() {
        Timber.d("Play button selected")
        viewModel.play()
    }

    @OnClick(R.id.player_menu)
    fun onMenuButtonSelected(view: View) {
        Timber.d("Menu button selected")

        val popupMenu = PopupMenu(this, view)
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

    @OnClick(R.id.player_menu_aspect_ratio)
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

    @OnClick(R.id.player_rewind)
    fun onRewindButtonSelected() {
        Timber.d("Rewind button selected")
        viewModel.seekBackward()
    }

    @OnClick(R.id.player_forward)
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
        playerView.useController = !isInPictureInPictureMode
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
    }

    override fun finish() {
        super.finish()
        Timber.d("Finishing")
        startActivity(Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
    }
}