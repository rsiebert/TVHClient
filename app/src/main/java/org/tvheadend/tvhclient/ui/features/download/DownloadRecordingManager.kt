package org.tvheadend.tvhclient.ui.features.download

import android.Manifest
import android.app.Activity
import android.app.DownloadManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.util.Base64
import androidx.core.app.ActivityCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.domain.entity.Connection
import org.tvheadend.tvhclient.domain.entity.Recording
import org.tvheadend.tvhclient.ui.common.SnackbarMessageReceiver.Companion.SNACKBAR_ACTION
import org.tvheadend.tvhclient.ui.common.SnackbarMessageReceiver.Companion.SNACKBAR_CONTENT
import org.tvheadend.tvhclient.ui.common.SnackbarMessageReceiver.Companion.SNACKBAR_DURATION
import timber.log.Timber

class DownloadRecordingManager(private val activity: Activity?, private val connection: Connection, recording: Recording?) {

    private lateinit var downloadManager: DownloadManager
    private var lastDownloadId: Long = 0

    init {
        if (activity != null) {
            Timber.d("Initializing download manager, given recording id is ${recording?.id}")
            downloadManager = activity.getSystemService(Service.DOWNLOAD_SERVICE) as DownloadManager
            if (recording != null && getIsStoragePermissionGranted(activity)) {
                startDownload(activity, recording)
            }
        }
    }

    private fun getDownloadRequest(recording: Recording): DownloadManager.Request {

        // The path that can be specified can only be in the external storage.
        // Therefore /storage/emulated/0 is fixed, only the location within this folder can be changed
        val downloadDirectory = PreferenceManager.getDefaultSharedPreferences(activity)
                .getString("download_directory", Environment.DIRECTORY_DOWNLOADS)
        val downloadUrl = "${connection.streamingUrl}/dvrfile/${recording.id}"
        // The user and password are required for authentication. They need to be encoded.
        val credentials = "Basic " + Base64.encodeToString((connection.username + ":" + connection.password).toByteArray(), Base64.NO_WRAP)
        // Use the recording title if present, otherwise use the recording id only
        val recordingTitle = (if (!recording.title.isNullOrEmpty()) recording.title?.replace(" ", "_") else recording.id.toString()) + ".mkv"

        Timber.d("Download recording from serverUrl '$downloadUrl' to $downloadDirectory/$recordingTitle")
        val request = DownloadManager.Request(Uri.parse(downloadUrl))
                .addRequestHeader("Authorization", credentials)
                .setTitle(recording.title)
                .setDescription(recording.description)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

        try {
            request.setDestinationInExternalPublicDir(downloadDirectory, recordingTitle)
        } catch (e: IllegalStateException) {
            Timber.d(e, "Could not set download destination directory to '$downloadDirectory', falling back to default ${Environment.DIRECTORY_DOWNLOADS}")
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, recordingTitle)
        }
        return request
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

    /**
     * Creates the request for the download and puts the request with the
     * required data in the download queue. When the
     * download has been started it checks after a while if the download has
     * actually started or if it has failed due to insufficient space. This is
     * required because the download manager does not throw this error via a
     * notification.
     */
    private fun startDownload(activity: Activity, recording: Recording) {
        Timber.d("Starting download of recording ${recording.title}")

        lastDownloadId = downloadManager.enqueue(getDownloadRequest(recording))
        Timber.d("Started download with id $lastDownloadId")

        // Check after a certain delay the status of the download and that for
        // example the download has not failed due to insufficient storage space.
        // The download manager does not sent a broadcast if this error occurs.
        Handler().postDelayed({ this.showDownloadStatusMessage(activity, recording) }, 1500)
    }

    private fun showDownloadStatusMessage(activity: Activity, recording: Recording) {
        Timber.d("Checking download status of recording ${recording.title}")

        // Initialize the default status message
        var msg = "Download of recording " + recording.title + " was not found"

        val cursor = downloadManager.query(DownloadManager.Query().setFilterById(lastDownloadId))
        if (cursor != null) {
            cursor.moveToFirst()
            val status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
            val reason = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_REASON))

            when (status) {
                DownloadManager.STATUS_FAILED -> when (reason) {
                    401, 407 -> msg = activity.getString(R.string.download_error_authentication_required, recording.title)
                    DownloadManager.ERROR_CANNOT_RESUME -> msg = "Download failed, cannot resume"
                    DownloadManager.ERROR_DEVICE_NOT_FOUND -> msg = "Download failed, device not found"
                    DownloadManager.ERROR_FILE_ALREADY_EXISTS -> msg = "Download failed, file exists already"
                    DownloadManager.ERROR_FILE_ERROR -> msg = "Download failed, file error"
                    DownloadManager.ERROR_HTTP_DATA_ERROR -> msg = "Download failed, HTTP data error"
                    DownloadManager.ERROR_INSUFFICIENT_SPACE -> msg = activity.getString(R.string.download_error_insufficient_space, recording.title)
                    DownloadManager.ERROR_TOO_MANY_REDIRECTS -> msg = "Download failed, too many redirects"
                    DownloadManager.ERROR_UNHANDLED_HTTP_CODE -> msg = "Download failed, unhandled HTTP code"
                    DownloadManager.ERROR_UNKNOWN -> msg = "Download failed, unknown error"
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
                else -> msg = "Download is not available"
            }
        }

        Timber.d("Download status of recording ${recording.title} is $msg")
        val intent = Intent(SNACKBAR_ACTION)
        intent.putExtra(SNACKBAR_CONTENT, msg)
        intent.putExtra(SNACKBAR_DURATION, Snackbar.LENGTH_LONG)
        LocalBroadcastManager.getInstance(activity).sendBroadcast(intent)
    }
}
