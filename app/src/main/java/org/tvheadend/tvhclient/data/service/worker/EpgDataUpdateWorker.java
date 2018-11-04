package org.tvheadend.tvhclient.data.service.worker;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;

import org.tvheadend.tvhclient.data.service.EpgSyncIntentService;

import androidx.work.Worker;
import androidx.work.WorkerParameters;
import timber.log.Timber;

public class EpgDataUpdateWorker extends Worker {

    public EpgDataUpdateWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Timber.d("Loading more event data from server");

        // The work here will be done when the worker is first enqueued.
        // Delay the execution to avoid having too much load during
        // startup or any issues during the initial sync.
        try {
            Thread.sleep(15000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        EpgSyncIntentService.enqueueWork(getApplicationContext(), new Intent().setAction("getMoreEvents"));
        return Result.SUCCESS;
    }
}
