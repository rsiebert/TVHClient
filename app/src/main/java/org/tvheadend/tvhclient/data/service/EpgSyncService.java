package org.tvheadend.tvhclient.data.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import org.tvheadend.tvhclient.MainApplication;
import org.tvheadend.tvhclient.data.repository.AppRepository;

import javax.inject.Inject;

import timber.log.Timber;

public class EpgSyncService extends Service {

    @Inject
    protected AppRepository appRepository;
    @Inject
    protected EpgSyncHandler epgSyncHandler;

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Binding to service not allowed");
    }

    @Override
    public void onCreate() {
        MainApplication.getComponent().inject(this);

        if (!epgSyncHandler.init()) {
            Timber.i("No connection available, not starting service");
            stopSelf();
        }
        Timber.i("Connection available, starting service");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Timber.d("Received start command");
        if (!epgSyncHandler.isConnected()) {
            Timber.d("Not connected to server");
            epgSyncHandler.connect();
        } else {
            Timber.d("Connected to server, passing intent to epg sync task");
            epgSyncHandler.handleIntent(intent);
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Timber.d("Stopping service");
        epgSyncHandler.stop();
    }
}
