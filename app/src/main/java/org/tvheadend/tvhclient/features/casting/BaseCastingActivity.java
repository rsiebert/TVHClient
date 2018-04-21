package org.tvheadend.tvhclient.features.casting;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;

import org.tvheadend.tvhclient.data.entity.Connection;
import org.tvheadend.tvhclient.data.entity.ServerStatus;
import org.tvheadend.tvhclient.data.repository.ConfigRepository;
import org.tvheadend.tvhclient.data.repository.ConnectionRepository;
import org.tvheadend.tvhclient.utils.MiscUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import timber.log.Timber;

public abstract class BaseCastingActivity extends AppCompatActivity {

    protected ConfigRepository configRepository;
    protected ServerStatus serverStatus;
    protected Connection connection;
    protected CastSession castSession;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(MiscUtils.getThemeId(this));
        super.onCreate(savedInstanceState);
        MiscUtils.setLanguage(this);

        ConnectionRepository connectionRepository = new ConnectionRepository(this);
        configRepository = new ConfigRepository(this);

        connection = connectionRepository.getActiveConnectionSync();
        serverStatus = configRepository.getServerStatus();

        if (connection == null || serverStatus == null) {
            new MaterialDialog.Builder(BaseCastingActivity.this)
                    .title("Error starting casting")
                    .content("Could not load required connection and server information")
                    .positiveText(getString(android.R.string.ok))
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            finish();
                        }
                    })
                    .show();
        }

        CastContext castContext = CastContext.getSharedInstance(this);
        castSession = castContext.getSessionManager().getCurrentCastSession();
        if (castSession == null) {
            Timber.d("No cast session available");
            finish();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("ticket");
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, intentFilter);
    }

    protected abstract void onHttpTicketReceived(String path, String ticket);

    @Override
    public void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver);
    }

    private BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Timber.d("Received message");

            String path = intent.getStringExtra("path");
            String ticket = intent.getStringExtra("ticket");
            Timber.d("Received http ticket with path: " + path + " and ticket: " + ticket);
            onHttpTicketReceived(path, ticket);
        }
    };

    /**
     * Creates the url with the credentials and the host and
     * port configuration. This one is the same for playing recording and programs
     *
     * @return The url with the protocol and credentials
     */
    protected String getBaseUrl(@NonNull Connection connection) {
        String encodedUsername = "";
        String encodedPassword = "";
        try {
            if (!connection.getUsername().isEmpty()) {
                encodedUsername = URLEncoder.encode(connection.getUsername(), "UTF-8");
            }
            if (!connection.getPassword().isEmpty()) {
                encodedPassword = URLEncoder.encode(connection.getPassword(), "UTF-8");
            }
        } catch (UnsupportedEncodingException e) {
            // Can't happen since encoding is statically specified
        }

        // Only add the credentials to the playback URL if a
        // username and password are set in the current connection
        String url = "http://";
        if (!encodedUsername.isEmpty()) {
            url += encodedUsername;
            if (!encodedPassword.isEmpty()) {
                url += ":" + encodedPassword + "@";
            }
        }

        url += connection.getHostname() + ":" + connection.getStreamingPort();
        return url;
    }
}
