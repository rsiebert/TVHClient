package org.tvheadend.tvhclient.data.service.worker;

import android.content.Intent;
import android.support.annotation.NonNull;

import org.tvheadend.tvhclient.data.service.EpgSyncIntentService;

import androidx.work.Worker;
import timber.log.Timber;

public class EpgPingServiceWorker extends Worker {
    @NonNull
    @Override
    public Result doWork() {
        Timber.d("Starting service and calling getStatus");

        Intent intent = new Intent();
        intent.setAction("getStatus");
        EpgSyncIntentService.enqueueWork(getApplicationContext(), intent);

        return Result.SUCCESS;
    }
}
