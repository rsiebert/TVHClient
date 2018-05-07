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

import org.tvheadend.tvhclient.MainApplication;
import org.tvheadend.tvhclient.data.entity.Connection;
import org.tvheadend.tvhclient.data.entity.ServerStatus;
import org.tvheadend.tvhclient.data.repository.AppRepository;
import org.tvheadend.tvhclient.utils.MiscUtils;

import timber.log.Timber;

public abstract class BaseCastingActivity extends AppCompatActivity {

    protected AppRepository appRepository;
    protected ServerStatus serverStatus;
    protected Connection connection;
    protected CastSession castSession;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(MiscUtils.getThemeId(this));
        super.onCreate(savedInstanceState);
        MiscUtils.setLanguage(this);
        MainApplication.getComponent().inject(this);

        connection = appRepository.getConnectionData().getActiveItem();
        serverStatus = appRepository.getServerStatusData().getItemById(connection.getId());

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
     * Creates the url with the host, port and webroot configuration.
     * This one is the same for casting recordings or programs
     *
     * @return The url with the protocol and credentials
     */
    protected String getBaseUrl() {
        return connection.getHostname() + ":" + connection.getStreamingPort() + serverStatus.getWebroot();
    }
}
