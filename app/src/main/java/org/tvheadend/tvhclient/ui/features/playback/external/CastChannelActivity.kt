package org.tvheadend.tvhclient.ui.features.playback.external

import android.content.Intent
import android.net.Uri
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadOptions
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import com.google.android.gms.common.images.WebImage
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.util.extensions.getCastSession
import org.tvheadend.tvhclient.util.extensions.gone
import timber.log.Timber

class CastChannelActivity : BasePlaybackActivity() {

    override fun onTicketReceived() {

        val castSession = this.getCastSession()
        if (castSession == null) {
            binding.progressBar.gone()
            binding.status.text = getString(R.string.no_cast_session)
            return
        }

        val channel = viewModel.channel ?: return
        val programTitle : String = channel.programTitle ?: ""
        val programSubtitle : String = channel.programSubtitle ?: ""
        val movieMetadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE)
        movieMetadata.putString(MediaMetadata.KEY_TITLE, programTitle)
        movieMetadata.putString(MediaMetadata.KEY_SUBTITLE, programSubtitle)

        val icon = channel.icon
        if (!icon.isNullOrEmpty()) {
            val iconUrl: String? = if (icon.startsWith("http")) {
                channel.icon
            } else {
                viewModel.getServerUrl() + "/" + channel.icon
            }
            Timber.d("Channel icon url: $iconUrl")
            movieMetadata.addImage(WebImage(Uri.parse(iconUrl)))   // small cast icon
            movieMetadata.addImage(WebImage(Uri.parse(iconUrl)))   // large background icon
        }

        val castingProfileId = viewModel.getServerStatus().castingServerProfileId
        if (castingProfileId == 0) {
            binding.progressBar.gone()
            binding.status.text = getString(R.string.error_starting_playback_no_profile)
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
            binding.progressBar.gone()
            binding.status.setText(R.string.cast_error_no_media_client_available)
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
