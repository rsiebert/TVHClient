package org.tvheadend.tvhclient.ui.features.playback.external

import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.preference.PreferenceManager
import org.tvheadend.tvhclient.MainApplication
import timber.log.Timber
import java.io.File

class PlayRecordingActivity : BasePlaybackActivity() {

    override fun onTicketReceived() {
        val url = viewModel.getPlaybackUrl()
        Timber.d("Playing recording from server with url $url")

        val intent = Intent(Intent.ACTION_VIEW)
        val title = viewModel.recording?.title ?: ""
        intent.putExtra("itemTitle", title)
        intent.putExtra("title",title)

        // Check if the recording exists in the download folder, if not stream it from the server
        val downloadDirectory = PreferenceManager.getDefaultSharedPreferences(this)
                .getString("download_directory", Environment.DIRECTORY_DOWNLOADS)
        val file = File(downloadDirectory, "$title.mkv")

        if (file.exists() && MainApplication.getInstance().isUnlocked) {
            Timber.d("Playing recording from local file ${file.absolutePath}")
            intent.setDataAndType(Uri.parse(file.absolutePath), "video/*")
        } else {
            Timber.d("Playing recording from server with url: $url")
            intent.setDataAndType(Uri.parse(url), "video/*")
        }
        startExternalPlayer(intent)
    }
}
