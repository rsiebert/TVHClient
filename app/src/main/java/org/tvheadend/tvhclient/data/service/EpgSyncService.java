package org.tvheadend.tvhclient.data.service;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;

import org.tvheadend.tvhclient.MainApplication;
import org.tvheadend.tvhclient.data.entity.Connection;
import org.tvheadend.tvhclient.data.repository.AppRepository;
import org.tvheadend.tvhclient.data.service.htsp.SimpleHtspConnection;

import javax.inject.Inject;

import timber.log.Timber;

public class EpgSyncService extends Service {

    @Inject
    protected AppRepository appRepository;
    private HandlerThread handlerThread;
    private Handler handler;
    private Connection connection;
    private SimpleHtspConnection simpleHtspConnection;
    private EpgSyncTask epgSyncTask;

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Binding to service not allowed");
    }

    @Override
    public void onCreate() {
        Timber.d("Starting service");

        handlerThread = new HandlerThread("EpgSyncService Handler Thread");
        handlerThread.start();
        new Handler(handlerThread.getLooper());

        MainApplication.getComponent().inject(this);

        connection = appRepository.getConnectionData().getActiveItem();
        if (connection == null) {
            Timber.i("No account configured, not starting service");
            stopSelf();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (connection == null) {
            return START_NOT_STICKY;
        }
        if (simpleHtspConnection == null
                || !simpleHtspConnection.isAuthenticated()
                || !simpleHtspConnection.isConnected()) {
            Timber.d("htsp connection is null, opening connection to server");
            openConnection();
        } else {
            Timber.d("htsp connection is not null, passing intent to epg sync task");
            if (epgSyncTask != null
                    && intent != null
                    && intent.getAction() != null) {
                epgSyncTask.getHandler().post(() -> epgSyncTask.handleIntent(intent));
            }
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Timber.d("Stopping service");
        closeConnection();
        if (handlerThread != null) {
            handlerThread.quit();
            handlerThread.interrupt();
            handlerThread = null;
        }
    }

    private void openConnection() {
        Timber.d("Opening connection to server");
        simpleHtspConnection = new SimpleHtspConnection(getApplicationContext(), connection);
        epgSyncTask = new EpgSyncTask(simpleHtspConnection, connection);
        simpleHtspConnection.addMessageListener(epgSyncTask);
        simpleHtspConnection.addConnectionListener(epgSyncTask);
        simpleHtspConnection.addAuthenticationListener(epgSyncTask);
        simpleHtspConnection.start();
    }

    private void closeConnection() {
        Timber.d("Closing connection to server");
        if (epgSyncTask != null) {
            simpleHtspConnection.removeMessageListener(epgSyncTask);
            simpleHtspConnection.removeConnectionListener(epgSyncTask);
            simpleHtspConnection.removeAuthenticationListener(epgSyncTask);
            epgSyncTask = null;
        }
        if (simpleHtspConnection != null) {
            simpleHtspConnection.stop();
        }
        simpleHtspConnection = null;
    }
}
