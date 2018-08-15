package org.tvheadend.tvhclient.data.service;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;

import org.tvheadend.tvhclient.data.entity.Connection;
import org.tvheadend.tvhclient.data.repository.AppRepository;
import org.tvheadend.tvhclient.data.service.htsp.SimpleHtspConnection;

import timber.log.Timber;

public class EpgSyncHandler {

    private final Context context;
    private final AppRepository appRepository;
    private HandlerThread handlerThread;
    private Handler handler;
    private Connection connection;
    private SimpleHtspConnection simpleHtspConnection;
    private EpgSyncTask epgSyncTask;

    public EpgSyncHandler(Context context, AppRepository appRepository) {
        this.appRepository = appRepository;
        this.context = context;

        handlerThread = new HandlerThread("EpgSyncService Handler Thread");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
    }

    public boolean init() {
        Timber.d("Init called");
        connection = appRepository.getConnectionData().getActiveItem();
        if (connection == null) {
            Timber.i("No account configured, not starting handler");
            return false;
        }
        return true;
    }

    public boolean isConnected() {
        return (simpleHtspConnection != null
                && simpleHtspConnection.isAuthenticated()
                && simpleHtspConnection.isConnected());
    }

    public void connect() {
        Timber.d("Opening connection to server");
        simpleHtspConnection = new SimpleHtspConnection(context, connection);
        epgSyncTask = new EpgSyncTask(simpleHtspConnection, connection);
        simpleHtspConnection.addMessageListener(epgSyncTask);
        simpleHtspConnection.addConnectionListener(epgSyncTask);
        simpleHtspConnection.addAuthenticationListener(epgSyncTask);
        simpleHtspConnection.start();
    }

    public void handleIntent(Intent intent) {
        if (intent != null && intent.getAction() != null) {
            epgSyncTask.handleIntent(intent);
        }
    }

    public void stop() {
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
