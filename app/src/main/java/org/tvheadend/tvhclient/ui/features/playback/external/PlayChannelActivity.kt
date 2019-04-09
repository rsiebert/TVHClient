package org.tvheadend.tvhclient.ui.features.playback.external

import android.content.Intent
import android.net.Uri
import timber.log.Timber

class PlayChannelActivity : BasePlaybackActivity() {

    override fun onTicketReceived() {
        val url = viewModel.getPlaybackUrl()
        Timber.d("Playing channel from server with url $url")

        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(Uri.parse(url), "video/*")

        if (!viewModel.channel?.name.isNullOrEmpty()) {
            intent.putExtra("itemTitle", viewModel.channel?.name)
            intent.putExtra("title", viewModel.channel?.name)
        }
        startExternalPlayer(intent)
    }
}
