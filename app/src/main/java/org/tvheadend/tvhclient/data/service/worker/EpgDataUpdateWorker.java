package org.tvheadend.tvhclient.data.service.worker;

import android.content.Intent;
import android.support.annotation.NonNull;

import org.tvheadend.tvhclient.data.service.EpgSyncService;

import androidx.work.Worker;
import timber.log.Timber;

public class EpgDataUpdateWorker extends Worker {
    @NonNull
    @Override
    public WorkerResult doWork() {
        Timber.d("Loading more event data from server");

        // The work here will be done when the worker is first enqueued.
        // This is done during startup. Delay the execution for 30
        // seconds to avoid having too much load during startup.
        // TODO This can be removed when a periodic task supports the setInitialDelay call.
        try {
            Thread.sleep(30000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Intent intent = new Intent(getApplicationContext(), EpgSyncService.class);
        intent.setAction("getMoreEvents");
        getApplicationContext().startService(intent);

        return WorkerResult.SUCCESS;
    }
}
