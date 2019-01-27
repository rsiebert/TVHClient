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

        Intent intent = new Intent();
        intent.setAction("getMoreEvents");
        intent.putExtra("numFollowing", 250);
        EpgSyncIntentService.enqueueWork(getApplicationContext(), intent);
        return Result.success();
    }
}
