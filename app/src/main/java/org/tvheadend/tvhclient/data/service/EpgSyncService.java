package org.tvheadend.tvhclient.data.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import org.tvheadend.tvhclient.MainApplication;
import org.tvheadend.tvhclient.data.entity.Connection;
import org.tvheadend.tvhclient.data.repository.AppRepository;
import org.tvheadend.tvhclient.data.service.htsp.SimpleHtspConnection;

import javax.inject.Inject;

import androidx.annotation.NonNull;
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
    }

    @Override
    public int onStartCommand(@NonNull Intent intent, int flags, int startId) {
        Timber.d("Received command for service");

        final String action = intent.getAction();
        if (action == null || action.isEmpty()) {
            return START_NOT_STICKY;
        }

        switch (action) {
            case "connect":
                Timber.d("Connection to server requested, stopping previous connection");
                startHtspConnection();
                break;
            case "reconnect":
                if (simpleHtspConnection == null || !simpleHtspConnection.isConnected()) {
                    startHtspConnection();
                }
                break;
            default:
                epgSyncTask.handleIntent(intent);
        }
        return START_STICKY;
    }

    private void startHtspConnection() {
        stopHtspConnection();

        connection = appRepository.getConnectionData().getActiveItem();
        simpleHtspConnection = new SimpleHtspConnection(connection);
        epgSyncTask = new EpgSyncTask(simpleHtspConnection, connection);

        simpleHtspConnection.addMessageListener(epgSyncTask);
        simpleHtspConnection.addConnectionListener(epgSyncTask);
        simpleHtspConnection.addAuthenticationListener(epgSyncTask);
        simpleHtspConnection.start();
    }

    private void stopHtspConnection() {
        if (simpleHtspConnection != null) {
            simpleHtspConnection.removeMessageListener(epgSyncTask);
            simpleHtspConnection.removeConnectionListener(epgSyncTask);
            simpleHtspConnection.removeAuthenticationListener(epgSyncTask);
            simpleHtspConnection.stop();
            simpleHtspConnection = null;
        }
        epgSyncTask = null;
        connection = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Timber.d("Stopping service");
        stopHtspConnection();
    }
}
