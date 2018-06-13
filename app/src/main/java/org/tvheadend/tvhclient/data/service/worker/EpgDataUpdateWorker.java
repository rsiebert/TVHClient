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
        Timber.d("doWork: Getting more epg data");

        Intent intent = new Intent(getApplicationContext(), EpgSyncService.class);
        intent.setAction("getMoreEvents");
        getApplicationContext().startService(intent);

        return WorkerResult.SUCCESS;
    }
}
