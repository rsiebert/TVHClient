package org.tvheadend.tvhclient.data.service.worker;

import android.content.Intent;
import android.support.annotation.NonNull;

import org.tvheadend.tvhclient.data.service.EpgSyncService;

import androidx.work.Worker;
import timber.log.Timber;

public class EpgPingServiceWorker extends Worker {
    @NonNull
    @Override
    public WorkerResult doWork() {
        Timber.d("Starting service and calling getStatus");

        Intent intent = new Intent(getApplicationContext(), EpgSyncService.class);
        intent.setAction("getStatus");
        getApplicationContext().startService(intent);

        return WorkerResult.SUCCESS;
    }
}
