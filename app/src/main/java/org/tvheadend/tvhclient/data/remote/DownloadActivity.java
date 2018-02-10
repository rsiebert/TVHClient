package org.tvheadend.tvhclient.data.remote;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DownloadManager;
import android.app.DownloadManager.Request;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityCompat.OnRequestPermissionsResultCallback;
import android.util.Base64;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.Constants;
import org.tvheadend.tvhclient.data.repository.RecordingRepository;
import org.tvheadend.tvhclient.data.entity.Connection;
import org.tvheadend.tvhclient.data.entity.Recording;
import org.tvheadend.tvhclient.data.local.Logger;
import org.tvheadend.tvhclient.data.repository.ConnectionRepository;
import org.tvheadend.tvhclient.utils.MiscUtils;

public class DownloadActivity extends Activity implements OnRequestPermissionsResultCallback {
    private final static String TAG = DownloadActivity.class.getSimpleName();

    private Connection conn;
    private DownloadManager dm;

    private Recording rec;
    private Logger logger;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(MiscUtils.getThemeId(this));
        super.onCreate(savedInstanceState);
        MiscUtils.setLanguage(this);

        rec = new RecordingRepository(this).getRecordingByIdSync(getIntent().getIntExtra("dvrId", 0));
        conn = new ConnectionRepository(this).getActiveConnectionSync();
        logger = Logger.getInstance();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (rec != null) {
            if (isStoragePermissionGranted()) {
                prepareDownload();
            }
        }
    }

    /**
     * Creates the request for the download of the defined recording via the
     * internal download manager.
     */
    private void prepareDownload() {

        String downloadUrl = "http://" + conn.getHostname() + ":" + conn.getStreamingPort() + "/dvrfile/" + rec.getId();
        String auth = "Basic " + Base64.encodeToString((conn.getUsername() + ":" + conn.getPassword()).getBytes(), Base64.NO_WRAP);

        try {
            Request request = new Request(Uri.parse(downloadUrl));
            request.addRequestHeader("Authorization", auth);
            request.setTitle(getString(R.string.download));
            request.setDescription(rec.getTitle());
            request.setNotificationVisibility(Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

            // The path that can be specified is always in the external storage. Therefore the path
            // like /storage/emulated/0 is fixed, only the location within this folder can be changed
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            final String path = prefs.getString("pref_download_directory", Environment.DIRECTORY_DOWNLOADS);
            request.setDestinationInExternalPublicDir(path, rec.getTitle() + ".mkv");

            logger.log(TAG, "prepareDownload: Saving download from url " + downloadUrl + " to " + path);
            startDownload(request);

        } catch (IllegalStateException e) {
            logger.log(TAG, "prepareDownload: External storage not available, " + e.getLocalizedMessage());
            showErrorDialog(getString(R.string.no_external_storage_available));
        }
    }

    /**
     * Puts the request with the required data in the download queue. When the
     * download has been started it checks after a while if the download has
     * actually started or if it has failed due to insufficient space. This is
     * required because the download manager does not throw this error via a
     * notification.
     *
     * @param request The given download request with all relevant data
     */
    private void startDownload(Request request) {
        dm = (DownloadManager) getSystemService(Service.DOWNLOAD_SERVICE);
        final long id = dm.enqueue(request);

        // Check after a certain delay the status of the download and that for
        // example the download has not failed due to insufficient storage space.
        // The download manager does not sent a broadcast if this error occurs.
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                DownloadManager.Query query = new DownloadManager.Query();
                query.setFilterById(id);
                Cursor c = dm.query(query);
                while (c.moveToNext()) {
                    int status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));
                    int reason = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_REASON));
                    logger.log(TAG, "startDownload: Downloading [" + id + "], status: " + status + ", reason: " + reason);

                    switch (status) {
                    case DownloadManager.STATUS_FAILED:
                        // Check the reason value if it is insufficient storage space
                        if (reason == 1006) {
                            logger.log(TAG, "startDownload: Download failed due to insufficient storage space");
                            showErrorDialog(getString(R.string.download_error_insufficient_space, rec.getTitle()));
                        } else if (reason == 407) {
                            logger.log(TAG, "startDownload: Download failed due to missing / wrong authentication");
                            showErrorDialog(getString(R.string.download_error_authentication_required, rec.getTitle()));
                        } else {
                            finish();
                        }
                        break;

                    case DownloadManager.STATUS_PAUSED:
                        finish();
                        break;

                    case DownloadManager.STATUS_PENDING:
                        finish();
                        break;

                    case DownloadManager.STATUS_RUNNING:
                        finish();
                        break;

                    case DownloadManager.STATUS_SUCCESSFUL:
                        finish();
                        break;

                    default:
                        finish();
                        break;
                    }
                }
            }
        }, 1500);
    }

    /**
     * Called when an activity was closed and this one is active again
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Constants.RESULT_CODE_START_PLAYER) {
            if (resultCode == RESULT_OK) {
                finish();
            }
        }
    }

    /**
     * Displays a dialog with the given message.
     *
     * @param msg The message that shall be shown
     */
    private void showErrorDialog(String msg) {
        new MaterialDialog.Builder(this)
                .content(msg)
                .positiveText("Close")
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        finish();
                    }
                })
                .show();
    }

    @SuppressLint("NewApi")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                permissions[0].equals("android.permission.WRITE_EXTERNAL_STORAGE")) {
            prepareDownload();
        } else {
            finish();
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
    @SuppressLint("NewApi")
    private boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                return true;
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            }
        } else {
            return true;
        }
    }
}
