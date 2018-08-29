package org.tvheadend.tvhclient.data.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import org.tvheadend.tvhclient.MainApplication;
import org.tvheadend.tvhclient.data.repository.AppRepository;
import org.tvheadend.tvhclient.data.service.worker.EpgDataRemovalWorker;
import org.tvheadend.tvhclient.data.service.worker.EpgDataUpdateWorker;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import timber.log.Timber;

public class EpgSyncService extends Service {

    @Inject
    protected AppRepository appRepository;
    @Inject
    protected EpgSyncHandler epgSyncHandler;
    private final String REQUEST_TAG = "tvhclient_worker";

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Binding to service not allowed");
    }

    @Override
    public void onCreate() {
        Timber.d("start");
        MainApplication.getComponent().inject(this);

        if (!epgSyncHandler.init()) {
            stopSelf();
        }

        startBackgroundWorkers();
        Timber.d("end");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Timber.d("start");
        if (!epgSyncHandler.isConnected()) {
            Timber.d("Not connected to server");
            epgSyncHandler.connect();
        } else {
            Timber.d("Connected to server, passing intent to epg sync task");
            epgSyncHandler.handleIntent(intent);
        }
        Timber.d("end");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Timber.d("start");
        epgSyncHandler.stop();
        WorkManager.getInstance().cancelAllWorkByTag(REQUEST_TAG);
        Timber.d("end");
    }

    private void startBackgroundWorkers() {
        Timber.d("Starting background workers");

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest updateWorkRequest =
                new PeriodicWorkRequest.Builder(EpgDataUpdateWorker.class, 2, TimeUnit.HOURS)
                        .setConstraints(constraints)
                        .addTag(REQUEST_TAG)
                        .build();

        PeriodicWorkRequest removalWorkRequest =
                new PeriodicWorkRequest.Builder(EpgDataRemovalWorker.class, 4, TimeUnit.HOURS)
                        .setConstraints(constraints)
                        .addTag(REQUEST_TAG)
                        .build();

        WorkManager.getInstance().cancelAllWorkByTag(REQUEST_TAG);
        WorkManager.getInstance().enqueue(updateWorkRequest);
        WorkManager.getInstance().enqueue(removalWorkRequest);
    }
}
