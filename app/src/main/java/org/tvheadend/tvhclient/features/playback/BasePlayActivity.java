package org.tvheadend.tvhclient.features.playback;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import org.tvheadend.tvhclient.MainApplication;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.entity.Connection;
import org.tvheadend.tvhclient.data.entity.ServerProfile;
import org.tvheadend.tvhclient.data.entity.ServerStatus;
import org.tvheadend.tvhclient.data.repository.AppRepository;
import org.tvheadend.tvhclient.utils.MiscUtils;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

public abstract class BasePlayActivity extends AppCompatActivity {

    @BindView(R.id.progress_bar)
    ProgressBar progressBar;
    @BindView(R.id.status)
    protected TextView statusTextView;

    protected Connection connection;
    protected ServerStatus serverStatus;
    private ServerProfile serverProfile;
    @Inject
    protected AppRepository appRepository;
    @Inject
    protected SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(MiscUtils.getThemeId(this));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.play_activity);
        MiscUtils.setLanguage(this);
        MainApplication.getComponent().inject(this);

        ButterKnife.bind(this);

        connection = appRepository.getConnectionData().getActiveItem();
        serverStatus = appRepository.getServerStatusData().getItemById(connection.getId());
        serverProfile = appRepository.getServerProfileData().getItemById(serverStatus.getPlaybackServerProfileId());
    }

    @Override
    public void onStart() {
        super.onStart();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("ticket");
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, intentFilter);
    }

    @Override
    public void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (connection == null || serverStatus == null) {
            progressBar.setVisibility(View.GONE);
            statusTextView.setText("Error starting playback.\nCould not load required connection and server information");
        } else {
            statusTextView.setText("Requesting playback information from server");
            getHttpTicket();
        }
    }

    protected abstract void onHttpTicketReceived(String path, String ticket);

    protected abstract void getHttpTicket();

    private BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            statusTextView.setText("Received playback information from server");
            String path = intent.getStringExtra("path");
            String ticket = intent.getStringExtra("ticket");
            onHttpTicketReceived(path, ticket);
        }
    };

    protected String getPlayerMimeType() {
        if (serverProfile.getName().contains("matroska")) {
            return "video/x-matroska";
        } else if (serverProfile.getName().contains("webm")) {
            return "video/webm";
        } else if (serverProfile.getName().contains("mpegts")) {
            return "video/mp2t";
        } else if (serverProfile.getName().contains("mpegps")) {
            return "video/mpeg";
        } else {
            return "application/octet-stream";
        }
    }

    protected String getPlayerUrl(String path, String ticket) {
        String url = "http://" + connection.getHostname() + ":" + connection.getStreamingPort() + path + "?ticket=" + ticket;

        // Use the newer server profiles over the old local ones
        ServerProfile serverProfile = appRepository.getServerProfileData().getItemById(serverStatus.getPlaybackServerProfileId());
        url += "&profile=" + serverProfile.getName();

        // Strip the username and password from the url before logging it
        if (url.indexOf('@') != -1) {
            Timber.d("Starting playback with url: http://user:pass" + url.substring(url.indexOf('@')));
        }

        return url;
    }

    protected void startExternalPlayer(Intent intent) {

        progressBar.setVisibility(View.GONE);
        statusTextView.setText("Starting playback");

        // Start playing the video in the UI thread
        this.runOnUiThread(new Runnable() {
            public void run() {
                try {
                    Timber.d("Starting external player");
                    startActivity(intent);
                    finish();
                } catch (Throwable t) {
                    Timber.d("Can't execute external media player");
                    statusTextView.setText(R.string.no_media_player);

                    // Show a confirmation dialog before deleting the recording
                    new MaterialDialog.Builder(BasePlayActivity.this)
                            .title(R.string.no_media_player)
                            .content(R.string.show_play_store)
                            .positiveText(getString(android.R.string.yes))
                            .negativeText(getString(android.R.string.no))
                            .onPositive(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                    try {
                                        Timber.d("Opening play store to download external players");
                                        Intent installIntent = new Intent(Intent.ACTION_VIEW);
                                        installIntent.setData(Uri.parse("market://search?q=free%20video%20player&c=apps"));
                                        startActivity(installIntent);
                                    } catch (Throwable t2) {
                                        Timber.d("Could not open google play store");
                                    } finally {
                                        finish();
                                    }
                                }
                            })
                            .onNegative(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                    finish();
                                }
                            })
                            .show();
                }
            }
        });
    }
}
