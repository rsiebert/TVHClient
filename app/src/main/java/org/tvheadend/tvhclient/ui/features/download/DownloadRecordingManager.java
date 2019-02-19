package org.tvheadend.tvhclient.ui.features.download;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Base64;

import com.google.android.material.snackbar.Snackbar;

import org.tvheadend.tvhclient.MainApplication;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.domain.entity.Connection;
import org.tvheadend.tvhclient.domain.entity.Recording;
import org.tvheadend.tvhclient.domain.entity.ServerStatus;
import org.tvheadend.tvhclient.data.repository.AppRepository;
import org.tvheadend.tvhclient.ui.base.receivers.SnackbarMessageReceiver;

import javax.inject.Inject;

import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import timber.log.Timber;

public class DownloadRecordingManager {

    @Inject
    protected AppRepository appRepository;
    @Inject
    protected SharedPreferences sharedPreferences;

    private final Recording recording;
    private final Connection connection;
    private final ServerStatus serverStatus;
    private final DownloadManager downloadManager;
    private final Activity activity;
    private long lastDownloadId;

    public DownloadRecordingManager(Activity activity, int dvrId) {
        Timber.d("Initializing download manager, given recording id is " + dvrId);
        MainApplication.getComponent().inject(this);

        this.activity = activity;
        this.recording = appRepository.getRecordingData().getItemById(dvrId);
        this.connection = appRepository.getConnectionData().getActiveItem();
        this.serverStatus = appRepository.getServerStatusData().getActiveItem();
        this.downloadManager = (DownloadManager) this.activity.getSystemService(Service.DOWNLOAD_SERVICE);

        if (recording != null) {
            Timber.d("Recording is not null");
            if (isStoragePermissionGranted()) {
                startDownload();
            }
        } else {
            Timber.d("Recording is null");
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
    private void startDownload() {
        Timber.d("Starting download of recording " + recording.getTitle());

        lastDownloadId = downloadManager.enqueue(getDownloadRequest());
        Timber.d("Started download with id " + lastDownloadId);

        // Check after a certain delay the status of the download and that for
        // example the download has not failed due to insufficient storage space.
        // The download manager does not sent a broadcast if this error occurs.
        new Handler().postDelayed(this::showDownloadStatusMessage, 1500);
    }

    private DownloadManager.Request getDownloadRequest() {
        // The path that can be specified can only be in the external storage.
        // Therefore /storage/emulated/0 is fixed, only the location within this folder can be changed
        String downloadDirectory = sharedPreferences.getString("download_directory", Environment.DIRECTORY_DOWNLOADS);
        String downloadUrl = "http://" +
                connection.getHostname() + ":" +
                connection.getStreamingPort() +
                (!TextUtils.isEmpty(serverStatus.getWebroot()) ? serverStatus.getWebroot() : "") +
                "/dvrfile/" +
                recording.getId();

        // The user and password are required for authentication. They need to be encoded.
        String credentials = "Basic " + Base64.encodeToString((connection.getUsername() + ":" + connection.getPassword()).getBytes(), Base64.NO_WRAP);
        // Use the recording title if present, otherwise use the recording id only
        String recordingTitle = (recording.getTitle() != null ? recording.getTitle().replace(" ", "_") : String.valueOf(recording.getId())) + ".mkv";

        Timber.d("Download recording from url " + downloadUrl + " to " + downloadDirectory + "/" + recordingTitle);
        return new DownloadManager.Request(Uri.parse(downloadUrl))
                .addRequestHeader("Authorization", credentials)
                .setTitle(recording.getTitle())
                .setDescription(recording.getDescription())
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(downloadDirectory, recordingTitle);
    }

    private void showDownloadStatusMessage() {
        Timber.d("Checking download status of recording " + recording.getTitle());

        // Initialize the default status message
        String msg = "Download of recording " + recording.getTitle() + " was not found";

        Cursor cursor = downloadManager.query(new DownloadManager.Query().setFilterById(lastDownloadId));
        if (cursor != null) {
            cursor.moveToFirst();
            int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
            int reason = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_REASON));

            switch (status) {
                case DownloadManager.STATUS_FAILED:
                    switch (reason) {
                        case 401:
                        case 407:
                            msg = activity.getString(R.string.download_error_authentication_required, recording.getTitle());
                            break;
                        case DownloadManager.ERROR_CANNOT_RESUME:
                            msg = "Download failed, cannot resume";
                            break;
                        case DownloadManager.ERROR_DEVICE_NOT_FOUND:
                            msg = "Download failed, device not found";
                            break;
                        case DownloadManager.ERROR_FILE_ALREADY_EXISTS:
                            msg = "Download failed, file exists already";
                            break;
                        case DownloadManager.ERROR_FILE_ERROR:
                            msg = "Download failed, file error";
                            break;
                        case DownloadManager.ERROR_HTTP_DATA_ERROR:
                            msg = "Download failed, HTTP data error";
                            break;
                        case DownloadManager.ERROR_INSUFFICIENT_SPACE:
                            msg = activity.getString(R.string.download_error_insufficient_space, recording.getTitle());
                            break;
                        case DownloadManager.ERROR_TOO_MANY_REDIRECTS:
                            msg = "Download failed, too many redirects";
                            break;
                        case DownloadManager.ERROR_UNHANDLED_HTTP_CODE:
                            msg = "Download failed, unhandled HTTP code";
                            break;
                        case DownloadManager.ERROR_UNKNOWN:
                            msg = "Download failed, unknown error";
                            break;
                    }
                    break;
                case DownloadManager.STATUS_PAUSED:
                    switch (reason) {
                        case DownloadManager.PAUSED_QUEUED_FOR_WIFI:
                            msg = "Download paused, queued for Wifi";
                            break;
                        case DownloadManager.PAUSED_UNKNOWN:
                            msg = "Download paused, unknown reason";
                            break;
                        case DownloadManager.PAUSED_WAITING_FOR_NETWORK:
                            msg = "Download paused, waiting for network";
                            break;
                        case DownloadManager.PAUSED_WAITING_TO_RETRY:
                            msg = "Download paused, waiting for retry";
                            break;
                    }
                    break;
                case DownloadManager.STATUS_PENDING:
                    msg = "Download pending";
                    break;
                case DownloadManager.STATUS_RUNNING:
                    msg = "Download is running";
                    break;
                case DownloadManager.STATUS_SUCCESSFUL:
                    msg = "Downloading " + recording.getTitle();
                    break;
                default:
                    msg = "Download is not available";
                    break;
            }
        }

        Timber.d("Download status of recording " + recording.getTitle() + " is " + msg);
        Intent intent = new Intent(SnackbarMessageReceiver.ACTION);
        intent.putExtra(SnackbarMessageReceiver.CONTENT, msg);
        intent.putExtra(SnackbarMessageReceiver.DURATION, Snackbar.LENGTH_LONG);
        LocalBroadcastManager.getInstance(activity).sendBroadcast(intent);
    }

    /**
     * Android API version 23 and higher requires that the permission to access
     * the external storage must be requested programmatically (if it has not
     * been granted already). The positive or negative request is checked in the
     * onRequestPermissionsResult(...) method.
     *
     * @return True if permission is granted, otherwise false
     */
    private boolean isStoragePermissionGranted() {
        Timber.d("Checking if storage permission was granted");
        if (Build.VERSION.SDK_INT >= 23) {
            if (activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                Timber.d("Storage permissions were granted (API >= 23)");
                return true;
            } else {
                Timber.d("Storage permissions are not yet granted (API >= 23)");
                ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            }
        } else {
            Timber.d("Storage permissions were granted (API < 23)");
            return true;
        }
    }
}
