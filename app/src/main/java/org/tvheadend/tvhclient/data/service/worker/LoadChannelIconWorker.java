package org.tvheadend.tvhclient.data.service.worker;

import android.content.Context;
import android.content.Intent;
import androidx.annotation.NonNull;

import org.tvheadend.tvhclient.data.service.EpgSyncIntentService;

import androidx.work.Worker;
import androidx.work.WorkerParameters;
import timber.log.Timber;

public class LoadChannelIconWorker extends Worker {

    public LoadChannelIconWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Timber.d("Loading channel icons from server");
        EpgSyncIntentService.enqueueWork(getApplicationContext(), new Intent().setAction("loadChannelIcons"));
        return Result.success();
    }
}
