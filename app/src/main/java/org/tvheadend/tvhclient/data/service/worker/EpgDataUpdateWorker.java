package org.tvheadend.tvhclient.data.service.worker;

import android.content.Intent;
import android.support.annotation.NonNull;

import org.tvheadend.tvhclient.data.service.EpgSyncIntentService;

import androidx.work.Worker;
import timber.log.Timber;

public class EpgDataUpdateWorker extends Worker {
    @NonNull
    @Override
    public Result doWork() {
        Timber.d("Loading more event data from server");

        // The work here will be done when the worker is first enqueued.
        // This is done during startup. Delay the execution for 60
        // seconds to avoid having too much load during startup.
        // TODO This can be removed when a periodic task supports the setInitialDelay call.
        try {
            Thread.sleep(60000);
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
            Thread.sleep(30000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        EpgSyncIntentService.enqueueWork(
                getApplicationContext(), new Intent().setAction("getMoreEvents"));

        return Result.SUCCESS;
    }
}
