package org.tvheadend.tvhclient.ui.features.playback.external

import android.net.Uri
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.cast.CastMediaItems
import org.tvheadend.tvhclient.cast.CastSessionState
import org.tvheadend.tvhclient.ui.features.playback.PlaybackLogic
import org.tvheadend.tvhclient.util.extensions.gone
import timber.log.Timber

class CastRecordingActivity : BasePlaybackActivity() {

    override suspend fun onTicketReceived() {

        if (!CastSessionState.isCastAvailable) {
            binding.progressBar.gone()
            binding.status.text = getString(R.string.no_cast_session)
            return
        }

        val recording = viewModel.recording ?: return
        val recordingTitle: String = recording.title ?: ""
        val recordingSubtitle: String = recording.subtitle ?: ""

        val artworkUri: Uri? = recording.channelIcon?.takeIf { it.isNotEmpty() }?.let { icon ->
            val iconUrl = if (icon.startsWith("http")) icon else viewModel.getServerUrl() + "/" + icon
            Timber.d("Recording channel icon url: $iconUrl")
            Uri.parse(iconUrl)
        }

        val castingProfileId = viewModel.getServerStatus().castingServerProfileId
        if (castingProfileId == 0) {
            binding.progressBar.gone()
            binding.status.text = getString(R.string.error_starting_playback_no_profile)
            return
        }

        val url = viewModel.getPlaybackUrl(true, castingProfileId)
        Timber.d("Casting recording from server with url $url")

        val mediaItem = CastMediaItems.recording(
                url = url,
                mimeType = PlaybackLogic.castingContentType(viewModel.getCastingProfile()?.name),
                title = recordingTitle,
                subtitle = recordingSubtitle,
                artworkUri = artworkUri
        )

        startCasting(mediaItem)
    }
}
