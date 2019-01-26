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

        boolean fullSyncRequired = getInputData().getBoolean("fullSyncRequired", false);
        boolean minimalSyncRequired = getInputData().getBoolean("minimalSyncRequired", false);

        Intent intent = new Intent();
        intent.setAction("getMoreEvents");

        if (fullSyncRequired) {
            Timber.d("Full sync is required, trying to load 250 events per channel");
            intent.putExtra("numFollowing", 250);
            intent.putExtra("fullSyncRequired", true);

        } else if (minimalSyncRequired) {
            Timber.d("Minimal sync is required, trying to load only 50 events per channel");
            intent.putExtra("numFollowing", 50);

        } else {
            Timber.d("No sync is required, not loading any events");
            return Result.success();
        }

        EpgSyncIntentService.enqueueWork(getApplicationContext(), intent);
        return Result.success();
    }
}
