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
        Timber.d("Removing outdated epg data from the database");

        // The work here will be done when the worker is first enqueued.
        // This is done during startup. Delay the execution for 90
        // seconds to avoid having too much load during startup.
        try {
            Thread.sleep(90000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Call the service with no action to check if a connection with
        // the server exists. If not then a reconnect will be performed.
        // Then call the service again with the desired action.
        EpgSyncIntentService.enqueueWork(
                getApplicationContext(), new Intent());

        // Wait 30 seconds
        try {
            Thread.sleep(60000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        EpgSyncIntentService.enqueueWork(
                getApplicationContext(), new Intent().setAction("deleteEvents"));

        return Result.SUCCESS;
    }
}
