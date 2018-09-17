package org.tvheadend.tvhclient.features.streaming.external;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;

import org.tvheadend.tvhclient.MainApplication;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.entity.Connection;
import org.tvheadend.tvhclient.data.entity.ServerProfile;
import org.tvheadend.tvhclient.data.entity.ServerStatus;
import org.tvheadend.tvhclient.data.repository.AppRepository;
import org.tvheadend.tvhclient.data.service.EpgSyncService;
import org.tvheadend.tvhclient.data.service.EpgSyncStatusCallback;
import org.tvheadend.tvhclient.data.service.EpgSyncTaskState;
import org.tvheadend.tvhclient.features.shared.receivers.ServiceStatusReceiver;
import org.tvheadend.tvhclient.utils.MiscUtils;

import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

public abstract class BasePlaybackActivity extends AppCompatActivity implements EpgSyncStatusCallback {

    @BindView(R.id.progress_bar)
    ProgressBar progressBar;
    @BindView(R.id.status)
    TextView statusTextView;

    Connection connection;
    ServerStatus serverStatus;
    @Inject
    protected AppRepository appRepository;
    @Inject
    protected SharedPreferences sharedPreferences;
    protected String baseUrl;
    protected String serverUrl;
    ServerProfile serverProfile;
    private ServiceStatusReceiver serviceStatusReceiver;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(MiscUtils.getThemeId(this));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.play_activity);
        MiscUtils.setLanguage(this);
        MainApplication.getComponent().inject(this);

        ButterKnife.bind(this);

        serviceStatusReceiver = new ServiceStatusReceiver(this);
        connection = appRepository.getConnectionData().getActiveItem();
        serverStatus = appRepository.getServerStatusData().getActiveItem();
        serverProfile = appRepository.getServerProfileData().getItemById(serverStatus.getPlaybackServerProfileId());
    }

    @Override
    public void onStart() {
        super.onStart();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("ticket");
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, intentFilter);
        LocalBroadcastManager.getInstance(this).registerReceiver(serviceStatusReceiver, new IntentFilter(ServiceStatusReceiver.ACTION));
    }

    @Override
    public void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(serviceStatusReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        init();
    }

    private void init() {
        if (connection == null || serverStatus == null) {
            progressBar.setVisibility(View.GONE);
            statusTextView.setText(getString(R.string.error_starting_playback_no_connection));
        } else if (serverProfile == null) {
            progressBar.setVisibility(View.GONE);
            statusTextView.setText(getString(R.string.error_starting_playback_no_profile));
        } else {
            progressBar.setVisibility(View.VISIBLE);
            statusTextView.setText(getString(R.string.requesting_playback_information));
            getHttpTicket();
        }
    }

    protected abstract void onHttpTicketReceived();

    protected abstract void getHttpTicket();

    private final BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            statusTextView.setText(getString(R.string.received_playback_information));
            String path = intent.getStringExtra("path");
            String ticket = intent.getStringExtra("ticket");

            try {
                String hostAddress = new ConvertHostnameToAddressTask(connection).execute().get();
                if (connection.getStreamingPort() != 80 && connection.getStreamingPort() != 443) {
                    baseUrl = "http://" + hostAddress + ":" + connection.getStreamingPort() + serverStatus.getWebroot();
                } else {
                    baseUrl = "http://" + hostAddress + serverStatus.getWebroot();
                }
                serverUrl = baseUrl + path + "?ticket=" + ticket + "&profile=" + serverProfile.getName();
                onHttpTicketReceived();

            } catch (InterruptedException | ExecutionException e) {
                Timber.d("Could not execute task to get ip address from " + connection.getHostname(), e);
                progressBar.setVisibility(View.GONE);
                statusTextView.setText(getString(R.string.error_starting_playback_no_profile));
            }
        }
    };

    void startExternalPlayer(Intent intent) {

        progressBar.setVisibility(View.GONE);
        statusTextView.setText(getString(R.string.starting_playback));

        // Start playing the video in the UI thread
        this.runOnUiThread(() -> {
            try {
                Timber.d("Starting external player");
                startActivity(intent);
                finish();
            } catch (Throwable t) {
                Timber.d("Can't execute external media player");
                statusTextView.setText(R.string.no_media_player);

                // Show a confirmation dialog before deleting the recording
                new MaterialDialog.Builder(BasePlaybackActivity.this)
                        .title(R.string.no_media_player)
                        .content(R.string.show_play_store)
                        .positiveText(getString(android.R.string.yes))
                        .negativeText(getString(android.R.string.no))
                        .onPositive((dialog, which) -> {
                            try {
                                Timber.d("Opening play store to download external players");
                                Intent installIntent = new Intent(Intent.ACTION_VIEW);
                                installIntent.setData(Uri.parse("market://search?q=free%20video%20player&c=apps"));
                                startActivity(installIntent);
                            } catch (Throwable t2) {
                                Timber.d("Could not startPlayback google play store");
                            } finally {
                                finish();
                            }
                        })
                        .onNegative((dialog, which) -> finish())
                        .show();
            }
        });
    }

    @Override
    public void onEpgTaskStateChanged(EpgSyncTaskState state) {
        Timber.d("Epg task state changed, message is " + state.getMessage());
        switch (state.getState()) {
            case FAILED:
                progressBar.setVisibility(View.GONE);
                statusTextView.setText(state.getMessage());
                stopService(new Intent(this, EpgSyncService.class));
                startService(new Intent(this, EpgSyncService.class));
                break;

            case CONNECTING:
                progressBar.setVisibility(View.GONE);
                statusTextView.setText(state.getMessage());
                break;

            case CONNECTED:
                progressBar.setVisibility(View.GONE);
                init();
                break;
        }
    }
}
