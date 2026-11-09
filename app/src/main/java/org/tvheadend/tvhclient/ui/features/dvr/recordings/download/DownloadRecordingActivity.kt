package org.tvheadend.tvhclient.ui.features.dvr.recordings.download

import android.Manifest
import android.app.Activity
import android.app.DownloadManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Base64
import androidx.core.app.ActivityCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import org.tvheadend.data.entity.Recording
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.ui.common.SnackbarMessageReceiver
import org.tvheadend.tvhclient.ui.features.playback.external.BasePlaybackActivity
import timber.log.Timber
import java.io.File


class DownloadRecordingActivity : BasePlaybackActivity() {

    private lateinit var downloadManager: DownloadManager
    private var lastDownloadId: Long = 0

    override suspend fun onTicketReceived() {
        viewModel.recording?.let {
            if (getIsStoragePermissionGranted(this)) {
                Timber.d("Initializing download manager")
                downloadManager = getSystemService(Service.DOWNLOAD_SERVICE) as DownloadManager
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

        Timber.d("State of external storage is ${Environment.getExternalStorageState()}")

        val downloadDirectory: String = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            Timber.d("Android API version is ${Build.VERSION.SDK_INT}, loading download folder from preference")
            val path = PreferenceManager.getDefaultSharedPreferences(this).getString("download_directory", Environment.DIRECTORY_DOWNLOADS)
                ?: Environment.DIRECTORY_DOWNLOADS
            Environment.getExternalStorageDirectory().absolutePath + path
        } else {
            Timber.d("Android API version is ${Build.VERSION.SDK_INT}, using default folder")

            val state = Environment.getExternalStorageState()
            if (Environment.MEDIA_MOUNTED == state) {
                Timber.d("External storage state is mounted")
                val baseDirFile: File? = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                if (baseDirFile == null) {
                    Timber.d("Download directory is null, using path '${filesDir.absolutePath}'")
                    filesDir.absolutePath
                } else {
                    Timber.d("Download directory is not null, path is '${baseDirFile.absolutePath}'")
                    baseDirFile.absolutePath
                }
            } else {
                Timber.d("External storage is not mounted, using path '${filesDir.absolutePath}'")
                filesDir.absolutePath
            }
        }

        Timber.d("Download recording from serverUrl '$downloadUrl' to $downloadDirectory/$recordingTitle")
        val request = DownloadManager.Request(Uri.parse(downloadUrl))
            .addRequestHeader("Authorization", credentials)
            .setTitle(recording.title)
            .setDescription(recording.description)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

        try {
            Timber.d("Adding download directory to the request")
            request.setDestinationInExternalPublicDir(downloadDirectory, recordingTitle)
            Timber.d("Adding download request to the queue")
            lastDownloadId = downloadManager.enqueue(request)
            Timber.d("Started download with id $lastDownloadId")
        } catch (e: SecurityException) {
            Timber.w(e, "Could not start the download, the storage permission is missing")
        } catch (e: IllegalArgumentException) {
            Timber.w(e, "Could not start the download, the download uri is not valid")
        } catch (e: IllegalStateException) {
            Timber.w(e, "Could not set the destination directory to %s", downloadDirectory)
        }

        // Check after a certain delay the status of the download and that for
        // example the download has not failed due to insufficient storage space.
        // The download manager does not sent a broadcast if this error occurs.
        Handler(Looper.getMainLooper()).postDelayed({ this.showDownloadStatusMessage(this, recording) }, 3000)
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
     * Since Android 10 (API 29) the [DownloadManager] writes to the public Downloads folder through
     * scoped storage, so no runtime permission is required. WRITE_EXTERNAL_STORAGE is only requested
     * on the earlier releases that still need it (it is capped at API 28 in the manifest). The
     * positive or negative result is checked in the host activity's onRequestPermissionsResult(...)
     * method.
     *
     * @return True if the download can proceed, otherwise false
     */
    private fun getIsStoragePermissionGranted(activity: Activity): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return true
        }
        Timber.d("Checking if storage permission was granted")
        return if (activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            Timber.d("Storage permission was already granted")
            true
        } else {
            Timber.d("Storage permission is not yet granted, requesting it")
            ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
            false
        }
    }

    private fun showDownloadStatusMessage(context: Context, recording: Recording) {
        Timber.d("Checking download status of id $lastDownloadId, recording ${recording.title}")

        // Initialize the default status message
        var msg = "Could not download recording ${recording.title}"
        var status = DownloadManager.STATUS_FAILED
        var reason = DownloadManager.ERROR_UNKNOWN

        val cursor = downloadManager.query(DownloadManager.Query().setFilterById(lastDownloadId))
        if (cursor != null && cursor.moveToFirst()) {

            if (cursor.columnCount > 0
                && cursor.getColumnIndex(DownloadManager.COLUMN_STATUS) != -1
                && cursor.getColumnIndex(DownloadManager.COLUMN_REASON) != -1
            ) {
                val columnStatus = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                val columnReason = cursor.getColumnIndex(DownloadManager.COLUMN_REASON)
                status = cursor.getInt(columnStatus)
                reason = cursor.getInt(columnReason)
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
        LocalBroadcastManager.getInstance(context.applicationContext).sendBroadcast(intent)
        finish()
    }
}
