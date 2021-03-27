package org.tvheadend.tvhclient.ui.features.playback.external

import android.content.Intent
import android.net.Uri
import timber.log.Timber

class PlayChannelActivity : BasePlaybackActivity() {

    override fun onTicketReceived() {
        val url = viewModel.getPlaybackUrl()
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(Uri.parse(url), "video/mp4")

        if (!viewModel.channel?.name.isNullOrEmpty()) {
            intent.putExtra("itemTitle", viewModel.channel?.name)
            intent.putExtra("title", viewModel.channel?.name)
        }
        Timber.d("Playing channel from server with url $url")
        startExternalPlayer(intent)
    }
}
