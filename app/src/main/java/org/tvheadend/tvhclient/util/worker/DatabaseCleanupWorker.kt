package org.tvheadend.tvhclient.util.worker

import android.content.Context
import android.content.Intent
import androidx.work.Worker
import androidx.work.WorkerParameters
import org.tvheadend.tvhclient.service.HtspIntentService
import timber.log.Timber

class DatabaseCleanupWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    override fun doWork(): Result {
        Timber.d("Cleaning database by removing duplicate entries")
        HtspIntentService.enqueueWork(applicationContext, Intent().setAction("cleanupDatabase"))
        return Result.success()
    }
}
