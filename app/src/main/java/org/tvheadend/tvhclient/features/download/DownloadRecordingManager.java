package org.tvheadend.tvhclient.features.download;

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
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Base64;

import org.tvheadend.tvhclient.MainApplication;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.entity.Connection;
import org.tvheadend.tvhclient.data.entity.Recording;
import org.tvheadend.tvhclient.data.entity.ServerStatus;
import org.tvheadend.tvhclient.data.repository.AppRepository;

import javax.inject.Inject;

import timber.log.Timber;

public class DownloadRecordingManager {

    private final Recording recording;
    private final Connection connection;
    private final ServerStatus serverStatus;
    private final DownloadManager downloadManager;
    private final Activity activity;
    @Inject
    protected AppRepository appRepository;
    @Inject
    protected SharedPreferences sharedPreferences;
    private long lastDownloadId;

    public DownloadRecordingManager(Activity activity, int dvrId) {
        MainApplication.getComponent().inject(this);

        this.activity = activity;
        this.recording = appRepository.getRecordingData().getItemById(dvrId);
        this.connection = appRepository.getConnectionData().getActiveItem();
        this.serverStatus = appRepository.getServerStatusData().getActiveItem();
        this.downloadManager = (DownloadManager) this.activity.getSystemService(Service.DOWNLOAD_SERVICE);

        if (recording != null && downloadManager != null) {
            if (isStoragePermissionGranted()) {
                startDownload();
            }
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

        lastDownloadId = downloadManager.enqueue(getDownloadRequest());
        // Check after a certain delay the status of the download and that for
        // example the download has not failed due to insufficient storage space.
        // The download manager does not sent a broadcast if this error occurs.
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                showDownloadStatusMessage();
            }
        }, 1500);
    }

    private DownloadManager.Request getDownloadRequest() {
        // The path that can be specified can only be in the external storage.
        // Therefore /storage/emulated/0 is fixed, only the location within this folder can be changed
        String downloadDirectory = sharedPreferences.getString("download_directory", Environment.DIRECTORY_DOWNLOADS);
        String downloadUrl = "http://" +
                connection.getHostname() + ":" +
                connection.getStreamingPort() +
                serverStatus.getWebroot() + "/dvrfile/" +
                recording.getId();

        // The user and password are required for authentication. They need to be encoded.
        String credentials = "Basic " + Base64.encodeToString((connection.getUsername() + ":" + connection.getPassword()).getBytes(), Base64.NO_WRAP);

        Timber.d("Download recording from url " + downloadUrl + " to " + downloadDirectory);

        return new DownloadManager.Request(Uri.parse(downloadUrl))
                .addRequestHeader("Authorization", credentials)
                .setTitle(activity.getString(R.string.download))
                .setDescription(recording.getTitle())
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(downloadDirectory, recording.getTitle() + ".mkv");
    }

    private void showDownloadStatusMessage() {

        String msg;
        Cursor cursor = downloadManager.query(new DownloadManager.Query().setFilterById(lastDownloadId));
        if (cursor == null) {
            msg = "Download was not found";
        } else {
            cursor.moveToFirst();
            int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
            int reason = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_REASON));

            switch (status) {
                case DownloadManager.STATUS_FAILED:
                    // Check different failure reasons
                    switch (reason) {
                        case DownloadManager.ERROR_FILE_ALREADY_EXISTS:
                            msg = "Recording " + recording.getTitle() + " exists already";
                            break;
                        case DownloadManager.ERROR_INSUFFICIENT_SPACE:
                            msg = activity.getString(R.string.download_error_insufficient_space, recording.getTitle());
                            break;
                        case 401:
                        case 407:
                            msg = activity.getString(R.string.download_error_authentication_required, recording.getTitle());
                            break;
                        default:
                            msg = "Download failed with error code " + reason;
                            break;
                    }
                    break;
                case DownloadManager.STATUS_PAUSED:
                    msg = "Download paused";
                    break;
                case DownloadManager.STATUS_PENDING:
                    msg = "Download pending";
                    break;
                case DownloadManager.STATUS_RUNNING:
                    msg = "Download in progress";
                    break;
                case DownloadManager.STATUS_SUCCESSFUL:
                    msg = "Download complete";
                    break;
                default:
                    msg = "Download is nowhere in sight";
                    break;
            }
        }
        Timber.d("Download status is " + msg);

        if (!TextUtils.isEmpty(msg)) {
            Intent intent = new Intent("message");
            intent.putExtra("message", msg);
            LocalBroadcastManager.getInstance(activity).sendBroadcast(intent);
        }
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
        if (Build.VERSION.SDK_INT >= 23) {
            if (activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                return true;
            } else {
                ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            }
        } else {
            return true;
        }
    }
}
