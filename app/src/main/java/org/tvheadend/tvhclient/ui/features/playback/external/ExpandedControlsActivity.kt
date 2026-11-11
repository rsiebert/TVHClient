package org.tvheadend.tvhclient.ui.features.playback.external

import android.content.ComponentName
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.media3.cast.MediaRouteButtonViewProvider
import androidx.media3.common.DeviceInfo
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.media3.ui.PlayerView
import com.google.common.util.concurrent.ListenableFuture
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.cast.CastPlaybackService
import org.tvheadend.tvhclient.util.getThemeId
import timber.log.Timber

/**
 * Expanded remote controls for the active Cast session, shown on top of the media notification and
 * as the target of the per-item cast flow.
 *
 * A Media3 [PlayerView] is bound to a [MediaController] connected to [CastPlaybackService], which
 * replaces the legacy `ExpandedControllerActivity`. The surface is re-bound on `onDeviceInfoChanged`
 * so the video does not go black when the output switches between local and remote.
 */
class ExpandedControlsActivity : AppCompatActivity() {

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private lateinit var playerView: PlayerView

    private val deviceInfoListener = object : Player.Listener {
        override fun onDeviceInfoChanged(deviceInfo: DeviceInfo) {
            // Reset to null first to bypass PlayerView.setPlayer()'s instance-equality check and
            // force it to re-attach the surface to the (possibly new) controller.
            playerView.player = null
            playerView.player = controller
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(getThemeId(this))
        super.onCreate(savedInstanceState)
        setContentView(R.layout.cast_controls_activity)
        playerView = findViewById(R.id.cast_player_view)
        playerView.setMediaRouteButtonViewProvider(MediaRouteButtonViewProvider())
    }

    override fun onStart() {
        super.onStart()
        val sessionToken = SessionToken(this, ComponentName(this, CastPlaybackService::class.java))
        val future = MediaController.Builder(this, sessionToken).buildAsync()
        controllerFuture = future
        future.addListener({
            if (isFinishing || isDestroyed) {
                MediaController.releaseFuture(future)
                return@addListener
            }
            try {
                val mediaController = future.get()
                controller = mediaController
                mediaController.addListener(deviceInfoListener)
                playerView.player = mediaController
            } catch (e: Exception) {
                Timber.e(e, "Could not connect to the cast playback service")
            }
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onStop() {
        super.onStop()
        controller?.removeListener(deviceInfoListener)
        controller = null
        playerView.player = null
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
    }
}
