package org.tvheadend.tvhclient.ui.features.download

import android.Manifest
import android.app.Activity
import android.app.DownloadManager
import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.text.TextUtils
import android.util.Base64
import androidx.core.app.ActivityCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.material.snackbar.Snackbar
import org.tvheadend.tvhclient.MainApplication
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.data.repository.AppRepository
import org.tvheadend.tvhclient.domain.entity.Connection
import org.tvheadend.tvhclient.domain.entity.Recording
import org.tvheadend.tvhclient.domain.entity.ServerStatus
import org.tvheadend.tvhclient.ui.common.SnackbarMessageReceiver
import timber.log.Timber
import javax.inject.Inject

class DownloadRecordingManager(private val activity: Activity, dvrId: Int) {

    @Inject
    lateinit var appRepository: AppRepository
    @Inject
    lateinit var sharedPreferences: SharedPreferences

    private lateinit var recording: Recording
    private var connection: Connection
    private val serverStatus: ServerStatus
    private val downloadManager: DownloadManager
    private var lastDownloadId: Long = 0

    init {
        Timber.d("Initializing download manager, given recording id is $dvrId")
        MainApplication.getComponent().inject(this)

        connection = appRepository.connectionData.activeItem
        serverStatus = appRepository.serverStatusData.activeItem
        downloadManager = activity.getSystemService(Service.DOWNLOAD_SERVICE) as DownloadManager

        if (dvrId > 0) {
            recording = appRepository.recordingData.getItemById(dvrId)
            Timber.d("Recording is not null")
            if (isStoragePermissionGranted) {
                startDownload()
            }
        }
    }

    private val downloadRequest: DownloadManager.Request
        get() {
            // The path that can be specified can only be in the external storage.
            // Therefore /storage/emulated/0 is fixed, only the location within this folder can be changed
            val downloadDirectory = sharedPreferences.getString("download_directory", Environment.DIRECTORY_DOWNLOADS)
            val downloadUrl = "http://" +
                    connection.hostname + ":" +
                    connection.streamingPort +
                    (if (!TextUtils.isEmpty(serverStatus.webroot)) serverStatus.webroot else "") +
                    "/dvrfile/" +
                    recording.id
            // The user and password are required for authentication. They need to be encoded.
            val credentials = "Basic " + Base64.encodeToString((connection.username + ":" + connection.password).toByteArray(), Base64.NO_WRAP)
            // Use the recording title if present, otherwise use the recording id only
            val recordingTitle = (if (!recording.title.isNullOrEmpty()) recording.title?.replace(" ", "_") else recording.id.toString()) + ".mkv"

            Timber.d("Download recording from url $downloadUrl to $downloadDirectory/$recordingTitle")
            return DownloadManager.Request(Uri.parse(downloadUrl))
                    .addRequestHeader("Authorization", credentials)
                    .setTitle(recording.title)
                    .setDescription(recording.description)
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setDestinationInExternalPublicDir(downloadDirectory, recordingTitle)
        }

    /**
     * Android API version 23 and higher requires that the permission to access
     * the external storage must be requested programmatically (if it has not
     * been granted already). The positive or negative request is checked in the
     * onRequestPermissionsResult(...) method.
     *
     * @return True if permission is granted, otherwise false
     */
    private val isStoragePermissionGranted: Boolean
        get() {
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
    private fun startDownload() {
        Timber.d("Starting download of recording ${recording.title}")

        lastDownloadId = downloadManager.enqueue(downloadRequest)
        Timber.d("Started download with id $lastDownloadId")

        // Check after a certain delay the status of the download and that for
        // example the download has not failed due to insufficient storage space.
        // The download manager does not sent a broadcast if this error occurs.
        Handler().postDelayed({ this.showDownloadStatusMessage() }, 1500)
    }

    private fun showDownloadStatusMessage() {
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
        val intent = Intent(SnackbarMessageReceiver.ACTION)
        intent.putExtra(SnackbarMessageReceiver.CONTENT, msg)
        intent.putExtra(SnackbarMessageReceiver.DURATION, Snackbar.LENGTH_LONG)
        LocalBroadcastManager.getInstance(activity).sendBroadcast(intent)
    }
}
