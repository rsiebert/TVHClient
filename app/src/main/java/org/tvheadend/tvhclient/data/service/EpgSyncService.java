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

import androidx.work.WorkManager;
import timber.log.Timber;

public class EpgSyncService extends Service {

    @Inject
    protected AppRepository appRepository;
    @Inject
    protected EpgSyncHandler epgSyncHandler;

    private final String REQUEST_TAG = "tvhclient_worker";
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
        MainApplication.getComponent().inject(this);

        if (!epgSyncHandler.init()) {
            stopSelf();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
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
        WorkManager.getInstance().cancelAllWorkByTag(REQUEST_TAG);
    }
}
