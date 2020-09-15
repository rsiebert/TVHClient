package org.tvheadend.tvhclient.ui.features.dvr.recordings.download

import android.Manifest
import android.app.Activity
import android.app.DownloadManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.util.Base64
import androidx.core.app.ActivityCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import com.downloader.OnDownloadListener
import com.downloader.PRDownloader
import com.google.android.material.snackbar.Snackbar
import org.tvheadend.data.entity.Recording
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.ui.common.SnackbarMessageReceiver
import org.tvheadend.tvhclient.ui.features.playback.external.BasePlaybackActivity
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL


class DownloadRecordingActivity : BasePlaybackActivity() {

    private lateinit var downloadManager: DownloadManager
    private var lastDownloadId: Long = 0

    override fun onTicketReceived() {
        viewModel.recording?.let {
            if (getIsStoragePermissionGranted(this)) {
                Timber.d("Initializing download manager")
                downloadManager = getSystemService(Service.DOWNLOAD_SERVICE) as DownloadManager

                PRDownloader.initialize(this.applicationContext)

                val url = viewModel.getPlaybackUrl()
                Timber.d("Downloading recording from server with url $url")
                startDownload(url, it)
            }
        }
    }

    private fun startDownload(downloadUrl: String, recording: Recording) {
        Timber.d("Preparing download of recording ${recording.title}")

        // The user and password are required for authentication. They need to be encoded.
        val credentials = "Basic " + Base64.encodeToString((viewModel.connection.username + ":" + viewModel.connection.password).toByteArray(), Base64.NO_WRAP)
        // Use the recording title if present, otherwise use the recording id only
        val recordingTitle = getRecordingTitle(recording)

        val downloadDirectory: String = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            Timber.d("Android API version is ${Build.VERSION.SDK_INT}, loading download folder from preference")
            val path = PreferenceManager.getDefaultSharedPreferences(this).getString("download_directory", Environment.DIRECTORY_DOWNLOADS)
                    ?: Environment.DIRECTORY_DOWNLOADS
            @Suppress("DEPRECATION")
            Environment.getExternalStorageDirectory().absolutePath + path
        } else {
            Timber.d("Android API version is ${Build.VERSION.SDK_INT}, using default folder")
            getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!.absolutePath
        }

        Timber.d("Download recording from serverUrl '$downloadUrl' to $downloadDirectory/$recordingTitle")
        val request = DownloadManager.Request(Uri.parse(downloadUrl))
                .addRequestHeader("Authorization", credentials)
                .setTitle(recording.title)
                .setDescription(recording.description)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(downloadDirectory, recordingTitle)

        try {
            lastDownloadId = downloadManager.enqueue(request)
            Timber.d("Started download with id $lastDownloadId")
        } catch (e: SecurityException) {
            Timber.d(e, "Could not start download, security exception")
        } catch (e: IllegalArgumentException) {
            Timber.d(e, "Could not start download, download uri is not valid")
        }
        // Check after a certain delay the status of the download and that for
        // example the download has not failed due to insufficient storage space.
        // The download manager does not sent a broadcast if this error occurs.
        Handler().postDelayed({ this.showDownloadStatusMessage(this, recording) }, 1500)
    }

    private fun getRecordingTitle(recording: Recording): String {
        var title = ""
        title += if (!recording.title.isNullOrEmpty()) recording.title else recording.id.toString()
        title += if (!recording.subtitle.isNullOrEmpty()) "_" + recording.subtitle else ""
        title += if (!recording.episode.isNullOrEmpty()) "_" + recording.episode else ""
        // Replace blanks with minus and remove other characters that shall could mess
        // things up. E.g. a backslash generates a new directory
        title = title.replace(Regex("""[ /\\:*"?<>|]"""), "-")
        title += ".mkv"
        return title
    }

    /**
     * Android API version 23 and higher requires that the permission to access
     * the external storage must be requested programmatically (if it has not
     * been granted already). The positive or negative request is checked in the
     * onRequestPermissionsResult(...) method.
     *
     * @return True if permission is granted, otherwise false
     */
    private fun getIsStoragePermissionGranted(activity: Activity): Boolean {
        Timber.d("Checking if storage permission was granted")
        return if (Build.VERSION.SDK_INT >= 23) {
            if (activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                Timber.d("Storage permissions were granted (API >= 23)")
                true
            } else {
                Timber.d("Storage permissions are not yet granted (API >= 23)")
                ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
                false
            }
        } else {
            Timber.d("Storage permissions were granted (API < 23)")
            true
        }
    }

    private fun showDownloadStatusMessage(context: Context, recording: Recording) {
        Timber.d("Checking download status of id $lastDownloadId, recording ${recording.title}")

        // Initialize the default status message
        var msg = "Could not download recording ${recording.title}"
        var status = DownloadManager.STATUS_FAILED
        var reason = DownloadManager.ERROR_UNKNOWN

        val cursor = downloadManager.query(DownloadManager.Query().setFilterById(lastDownloadId))
        if (cursor != null) {
            cursor.moveToFirst()

            if (cursor.columnCount > 0
                    && cursor.getColumnIndex(DownloadManager.COLUMN_STATUS) != -1
                    && cursor.getColumnIndex(DownloadManager.COLUMN_REASON) != -1) {
                status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
                reason = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_REASON))
            }

            when (status) {
                DownloadManager.STATUS_FAILED -> when (reason) {
                    404 -> msg = "Recording was not found"
                    401, 407 -> msg = context.getString(R.string.download_error_authentication_required, recording.title)
                    DownloadManager.ERROR_CANNOT_RESUME -> msg = "Download failed, cannot resume"
                    DownloadManager.ERROR_DEVICE_NOT_FOUND -> msg = "Download failed, device not found"
                    DownloadManager.ERROR_FILE_ALREADY_EXISTS -> msg = "Download failed, file exists already"
                    DownloadManager.ERROR_FILE_ERROR -> msg = "Download failed, file error"
                    DownloadManager.ERROR_HTTP_DATA_ERROR -> msg = "Download failed, HTTP data error"
                    DownloadManager.ERROR_INSUFFICIENT_SPACE -> msg = context.getString(R.string.download_error_insufficient_space, recording.title)
                    DownloadManager.ERROR_TOO_MANY_REDIRECTS -> msg = "Download failed, too many redirects"
                    DownloadManager.ERROR_UNHANDLED_HTTP_CODE -> msg = "Download failed, unhandled HTTP code"
                }
                DownloadManager.STATUS_PAUSED -> when (reason) {
                    DownloadManager.PAUSED_QUEUED_FOR_WIFI -> msg = "Download paused, queued for Wifi"
                    DownloadManager.PAUSED_UNKNOWN -> msg = "Download paused, unknown reason"
                    DownloadManager.PAUSED_WAITING_FOR_NETWORK -> msg = "Download paused, waiting for network"
                    DownloadManager.PAUSED_WAITING_TO_RETRY -> msg = "Download paused, waiting for retry"
                }
                DownloadManager.STATUS_PENDING -> msg = "Download pending"
                DownloadManager.STATUS_RUNNING -> msg = "Download is running"
                DownloadManager.STATUS_SUCCESSFUL -> msg = "Downloading " + recording.title
            }
        }
        cursor.close()

        Timber.d("Download status of recording ${recording.title} is $msg")
        val intent = Intent(SnackbarMessageReceiver.SNACKBAR_ACTION)
        intent.putExtra(SnackbarMessageReceiver.SNACKBAR_CONTENT, msg)
        intent.putExtra(SnackbarMessageReceiver.SNACKBAR_DURATION, Snackbar.LENGTH_LONG)
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
        finish()
    }
}
