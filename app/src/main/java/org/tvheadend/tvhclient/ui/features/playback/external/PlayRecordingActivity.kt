package org.tvheadend.tvhclient.ui.features.playback.external

import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.preference.PreferenceManager
import timber.log.Timber
import java.io.File

class PlayRecordingActivity : BasePlaybackActivity() {

    override fun onTicketReceived() {
        val url = viewModel.getPlaybackUrl()
        val intent = Intent(Intent.ACTION_VIEW)
        val title = viewModel.recording?.title ?: ""
        intent.putExtra("itemTitle", title)
        intent.putExtra("title",title)

        // Check if the recording exists in the download folder, if not stream it from the server
        val downloadDirectory = PreferenceManager.getDefaultSharedPreferences(this)
                .getString("download_directory", Environment.DIRECTORY_DOWNLOADS)
        val file = File(downloadDirectory, "$title.mkv")

        if (file.exists() && viewModel.isUnlocked) {
            Timber.d("Playing recording from local file ${file.absolutePath}")
            intent.setDataAndType(Uri.parse(file.absolutePath), "video/mp4")
        } else {
            Timber.d("Playing recording from server with url: $url")
            intent.setDataAndType(Uri.parse(url), "video/mp4")
        }
        startExternalPlayer(intent)
    }
}
