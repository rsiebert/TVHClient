package org.tvheadend.tvhclient.data.service.worker;

import android.os.Handler;
import android.support.annotation.NonNull;

import org.tvheadend.tvhclient.data.service.htsp.HtspConnection;

import java.util.concurrent.TimeUnit;

import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import timber.log.Timber;

public class EpgWorkerHandler implements HtspConnection.Listener {

    private final String REQUEST_TAG = "tvhclient_worker";

    @Override
    public Handler getHandler() {
        return null;
    }

    @Override
    public void setConnection(@NonNull HtspConnection connection) {

    }

    @Override
    public void onConnectionStateChange(@NonNull HtspConnection.State state) {
        Timber.d("Connection state changed to " + state);
        switch (state) {
            case CONNECTED:
                startBackgroundWorkers();
                break;
        }
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

        Timber.d("Finished starting background workers");
    }

    public void stop() {
        Timber.d("Stopping all background worker");
        WorkManager.getInstance().cancelAllWorkByTag(REQUEST_TAG);
    }
}
