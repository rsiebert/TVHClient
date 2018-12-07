package org.tvheadend.tvhclient.data.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import org.tvheadend.tvhclient.MainApplication;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.entity.Connection;
import org.tvheadend.tvhclient.data.repository.AppRepository;
import org.tvheadend.tvhclient.data.service.htsp.SimpleHtspConnection;
import org.tvheadend.tvhclient.features.shared.receivers.ServiceStatusReceiver;

import javax.inject.Inject;

import timber.log.Timber;

public class EpgSyncService extends Service {

    @Inject
    protected AppRepository appRepository;
    private SimpleHtspConnection simpleHtspConnection;
    private Connection connection;
    private EpgSyncTask epgSyncTask;

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Binding to service not allowed");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Timber.d("Starting service");

        MainApplication.getComponent().inject(this);
        connection = appRepository.getConnectionData().getActiveItem();
        simpleHtspConnection = new SimpleHtspConnection(connection);

        epgSyncTask = new EpgSyncTask(simpleHtspConnection, connection);

        simpleHtspConnection.addMessageListener(epgSyncTask);
        simpleHtspConnection.addConnectionListener(epgSyncTask);
        simpleHtspConnection.addAuthenticationListener(epgSyncTask);
        simpleHtspConnection.start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Timber.d("Received command for service");

        if (simpleHtspConnection != null
                && simpleHtspConnection.isConnected()
                && simpleHtspConnection.isAuthenticated()) {
            Timber.d("Connected to server, passing command to epg sync task");
            epgSyncTask.handleIntent(intent);

        } else if (simpleHtspConnection != null
                && !simpleHtspConnection.isIdle()) {
            Timber.d("Server connection has been closed");
            epgSyncTask.sendEpgSyncStatusMessage(ServiceStatusReceiver.State.CLOSED,
                    getString(R.string.connection_closed), "");
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Timber.d("Stopping service");
        if (simpleHtspConnection != null) {
            simpleHtspConnection.removeMessageListener(epgSyncTask);
            simpleHtspConnection.removeConnectionListener(epgSyncTask);
            simpleHtspConnection.removeAuthenticationListener(epgSyncTask);
            simpleHtspConnection.stop();
        }
        simpleHtspConnection = null;
        connection = null;
    }
}
