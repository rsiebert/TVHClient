package org.tvheadend.tvhclient.cast

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.cast.CastPlayer
import androidx.media3.common.DeviceInfo
import androidx.media3.common.Player
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.ui.features.playback.external.ExpandedControlsActivity

/**
 * Hosts the Media3 [CastPlayer] and exposes it as a [MediaSession] so the casting activities can
 * drive remote playback through a `MediaController` and the expanded controls can display/control it.
 *
 * Only the cast path uses this service. The app's internal HTSP playback lives in `PlaybackActivity`
 * and is completely independent of it.
 *
 * The [CastPlayer] is built with its default local ExoPlayer, which the player owns and releases.
 */
class CastPlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var castPlayer: CastPlayer? = null

    /**
     * Publishes cast session availability to [CastSessionState]. `onDeviceInfoChanged` replaces the
     * deprecated `CastPlayer.setSessionAvailabilityListener`: the device type is remote while a Cast
     * session is active and local otherwise.
     */
    private val deviceInfoListener = object : Player.Listener {
        override fun onDeviceInfoChanged(deviceInfo: DeviceInfo) {
            CastSessionState.setCastAvailable(deviceInfo.playbackType == DeviceInfo.PLAYBACK_TYPE_REMOTE)
        }
    }

    override fun onCreate() {
        super.onCreate()

        // Keep the session alive in the background so a cast is not torn down when the app leaves the
        // foreground. Tap on the notification returns to the expanded controls.
        setMediaNotificationProvider(
                DefaultMediaNotificationProvider.Builder(this)
                        .setChannelName(R.string.notification_channel_casting)
                        .build()
                        .also { it.setSmallIcon(R.drawable.ic_notification) }
        )

        val player = CastPlayer.Builder(this).build()
        castPlayer = player
        player.addListener(deviceInfoListener)

        val sessionActivity = PendingIntent.getActivity(
                this,
                0,
                Intent(this, ExpandedControlsActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, player)
                .setSessionActivity(sessionActivity)
                .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        CastSessionState.setCastAvailable(false)
        castPlayer?.removeListener(deviceInfoListener)
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        castPlayer = null
        super.onDestroy()
    }
}
