package org.tvheadend.tvhclient.data.service.worker;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;

import org.tvheadend.tvhclient.data.service.EpgSyncIntentService;

import androidx.work.Worker;
import androidx.work.WorkerParameters;
import timber.log.Timber;

public class EpgDataRemovalWorker extends Worker {

    public EpgDataRemovalWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Timber.d("Waiting 5s before removing outdated epg data from the database");

        // The work here will be done when the worker is first enqueued.
        // Delay the execution to avoid having too much load during
        // startup or any issues during the initial sync.
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Timber.d("Removing outdated epg data from the database");
        EpgSyncIntentService.enqueueWork(getApplicationContext(), new Intent().setAction("deleteEvents"));
        return Result.SUCCESS;
    }
}
