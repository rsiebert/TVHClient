package org.tvheadend.tvhclient.ui.features.playback.external

import android.content.Intent
import android.net.Uri
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadOptions
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import com.google.android.gms.common.images.WebImage
import kotlinx.android.synthetic.main.play_activity.*
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.ui.common.getCastSession
import org.tvheadend.tvhclient.util.extensions.gone
import timber.log.Timber

class CastChannelActivity : BasePlaybackActivity() {

    override fun onTicketReceived() {

        val castSession = getCastSession(this)
        if (castSession == null) {
            progress_bar.gone()
            status.text = getString(R.string.no_cast_session)
            return
        }

        val channel = viewModel.channel ?: return
        val movieMetadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE)
        movieMetadata.putString(MediaMetadata.KEY_TITLE, channel.programTitle)
        movieMetadata.putString(MediaMetadata.KEY_SUBTITLE, channel.programSubtitle)

        if (!channel.icon.isNullOrEmpty()) {
            val iconUrl: String? = if (channel.icon?.startsWith("http") == true) {
                channel.icon
            } else {
                viewModel.getServerUrl() + "/" + channel.icon
            }
            Timber.d("Channel icon url: $iconUrl")
            movieMetadata.addImage(WebImage(Uri.parse(iconUrl)))   // small cast icon
            movieMetadata.addImage(WebImage(Uri.parse(iconUrl)))   // large background icon
        }

        val castingProfileId = viewModel.serverStatus.castingServerProfileId
        if (castingProfileId == 0) {
            progress_bar.gone()
            status.text = getString(R.string.error_starting_playback_no_profile)
            return
        }

        val url = viewModel.getPlaybackUrl(true, castingProfileId)
        Timber.d("Cast channel from server with url $url")
        val mediaInfo = MediaInfo.Builder(url)
                .setStreamType(MediaInfo.STREAM_TYPE_LIVE)
                .setContentType("video/webm")
                .setMetadata(movieMetadata)
                .setStreamDuration(0)
                .build()

        val remoteMediaClient = castSession.remoteMediaClient
        if (remoteMediaClient == null) {
            progress_bar.gone()
            status.setText(R.string.cast_error_no_media_client_available)
            return
        }

        remoteMediaClient.registerCallback(CastRemoteMediaClientCallback(remoteMediaClient))
        remoteMediaClient.registerCallback(object : RemoteMediaClient.Callback() {
            override fun onStatusUpdated() {
                Timber.d("Status updated")
                val intent = Intent(this@CastChannelActivity, ExpandedControlsActivity::class.java)
                startActivity(intent)
                remoteMediaClient.unregisterCallback(this)
                finish()
            }

            override fun onSendingRemoteMediaRequest() {
                Timber.d("Sending remote media request")
            }
        })

        val mediaLoadOptions = MediaLoadOptions.Builder()
                .setAutoplay(true)
                .setPlayPosition(0)
                .build()
        remoteMediaClient.load(mediaInfo, mediaLoadOptions)
    }
}
