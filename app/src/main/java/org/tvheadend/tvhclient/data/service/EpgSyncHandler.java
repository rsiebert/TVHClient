package org.tvheadend.tvhclient.data.service;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;

import org.tvheadend.tvhclient.data.entity.Connection;
import org.tvheadend.tvhclient.data.repository.AppRepository;
import org.tvheadend.tvhclient.data.service.htsp.SimpleHtspConnection;
import org.tvheadend.tvhclient.data.service.worker.EpgWorkerHandler;

import timber.log.Timber;

public class EpgSyncHandler {

    private final Context context;
    private final AppRepository appRepository;
    private Connection connection;
    private SimpleHtspConnection simpleHtspConnection;
    private EpgSyncTask epgSyncTask;
    private EpgWorkerHandler epgWorkerHandler;

    public EpgSyncHandler(Context context, AppRepository appRepository) {
        this.context = context;
        this.appRepository = appRepository;

        HandlerThread handlerThread = new HandlerThread("EpgSyncService Handler Thread");
        handlerThread.start();
        Handler handler = new Handler(handlerThread.getLooper());
    }

    boolean init() {
        connection = appRepository.getConnectionData().getActiveItem();
        return connection != null;
    }

    public boolean isConnected() {
        return (simpleHtspConnection != null
                && simpleHtspConnection.isAuthenticated()
                && simpleHtspConnection.isConnected());
    }

    void connect() {
        Timber.d("Opening connection to server");
        simpleHtspConnection = new SimpleHtspConnection(context, connection);

        epgSyncTask = new EpgSyncTask(simpleHtspConnection, connection);
        epgWorkerHandler = new EpgWorkerHandler(context);

        simpleHtspConnection.addMessageListener(epgSyncTask);
        simpleHtspConnection.addConnectionListener(epgWorkerHandler);
        simpleHtspConnection.addAuthenticationListener(epgSyncTask);
        simpleHtspConnection.start();
    }

    void handleIntent(Intent intent) {
        if (intent != null && intent.getAction() != null) {
            Timber.d("Passing intent action " + intent.getAction() + " to epg sync task");
            epgSyncTask.handleIntent(intent);
        } else {
            Timber.d("No intent action given");
        }
    }

    public void stop() {
        Timber.d("Closing connection to server");
        connection = null;

        if (simpleHtspConnection != null) {
            simpleHtspConnection.removeMessageListener(epgSyncTask);
            simpleHtspConnection.removeAuthenticationListener(epgSyncTask);
            simpleHtspConnection.removeConnectionListener(epgWorkerHandler);
            simpleHtspConnection.stop();
        }
        simpleHtspConnection = null;
        epgSyncTask = null;
        epgWorkerHandler = null;

        Timber.d("Connection to server closed");
    }

    public SimpleHtspConnection getConnection() {
        return simpleHtspConnection;
    }
}
