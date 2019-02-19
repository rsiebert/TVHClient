package org.tvheadend.tvhclient.ui.features.playback.external;

import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;

import org.tvheadend.tvhclient.MainApplication;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.domain.entity.Connection;
import org.tvheadend.tvhclient.domain.entity.ServerProfile;
import org.tvheadend.tvhclient.domain.entity.ServerStatus;
import org.tvheadend.tvhclient.data.repository.AppRepository;
import org.tvheadend.tvhclient.data.service.HtspService;
import org.tvheadend.tvhclient.util.receivers.SyncStateReceiver;
import org.tvheadend.tvhclient.util.MiscUtils;

import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

public abstract class BasePlaybackActivity extends AppCompatActivity implements SyncStateReceiver.Listener {

    @BindView(R.id.progress_bar)
    ProgressBar progressBar;
    @BindView(R.id.status)
    TextView statusTextView;

    private Connection connection;
    ServerStatus serverStatus;
    @Inject
    protected AppRepository appRepository;
    @Inject
    protected SharedPreferences sharedPreferences;
    String baseUrl;
    String serverUrl;
    ServerProfile serverProfile;
    private SyncStateReceiver syncStateReceiver;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(MiscUtils.getThemeId(this));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.play_activity);
        MiscUtils.setLanguage(this);
        MainApplication.getComponent().inject(this);

        ButterKnife.bind(this);

        syncStateReceiver = new SyncStateReceiver(this);
        connection = appRepository.getConnectionData().getActiveItem();
        serverStatus = appRepository.getServerStatusData().getActiveItem();

        serverProfile = appRepository.getServerProfileData().getItemById(serverStatus.getHttpPlaybackServerProfileId());
    }

    @Override
    public void onStart() {
        super.onStart();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("ticket");
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, intentFilter);
        LocalBroadcastManager.getInstance(this).registerReceiver(syncStateReceiver, new IntentFilter(SyncStateReceiver.ACTION));
    }

    @Override
    public void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(syncStateReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        initialize();
    }

    private void initialize() {
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

    protected abstract boolean requireHostnameToAddressConversion();

    protected abstract void onHttpTicketReceived();

    protected abstract void getHttpTicket();

    private final BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            progressBar.setVisibility(View.GONE);
            statusTextView.setText(getString(R.string.received_playback_information));
            String path = intent.getStringExtra("path");
            String ticket = intent.getStringExtra("ticket");

            // Convert the hostname to the IP address only when required.
            // This is usually required when a channel or recording shall
            // be played on a chromecast
            String hostAddress = connection.getHostname();
            if (requireHostnameToAddressConversion()) {
                Timber.d("Convert hostname " + connection.getHostname() + " to IP address");
                try {
                    hostAddress = new ConvertHostnameToAddressTask(connection).execute().get();
                } catch (InterruptedException | ExecutionException e) {
                    Timber.d("Could not execute task to get ip address from " + connection.getHostname(), e);
                }
            } else {
                Timber.d("Hostname " + connection.getHostname() + " to IP address conversion not required");
            }

            if (connection.getStreamingPort() != 80 && connection.getStreamingPort() != 443) {
                baseUrl = "http://" + hostAddress + ":" + connection.getStreamingPort();
            } else {
                baseUrl = "http://" + hostAddress;
            }
            if (!TextUtils.isEmpty(serverStatus.getWebroot())) {
                baseUrl += serverStatus.getWebroot();
            }
            serverUrl = baseUrl + path + "?ticket=" + ticket + "&profile=" + serverProfile.getName();

            // Copy the created server url to the clip board if the setting is enabled
            if (sharedPreferences.getBoolean("copy_playback_url_to_clipboard_enabled", getResources().getBoolean(R.bool.pref_default_copy_playback_url_to_clipboard_enabled))) {
                Timber.d("Copying playback url " + serverUrl + " to clipboard");
                ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Server url", serverUrl);
                clipboard.setPrimaryClip(clip);
            } else {
                Timber.d("Not copying playback url " + serverUrl + " to clipboard");
            }

            onHttpTicketReceived();
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
    public void onSyncStateChanged(SyncStateReceiver.State state, String message, String details) {
        Timber.d("Epg task state changed, message is " + message);
        switch (state) {
            case FAILED:
                progressBar.setVisibility(View.GONE);
                statusTextView.setText(message);
                stopService(new Intent(this, HtspService.class));
                startService(new Intent(this, HtspService.class));
                break;

            case CONNECTING:
                progressBar.setVisibility(View.GONE);
                statusTextView.setText(message);
                break;

            case CONNECTED:
                progressBar.setVisibility(View.GONE);
                initialize();
                break;
        }
    }
}
