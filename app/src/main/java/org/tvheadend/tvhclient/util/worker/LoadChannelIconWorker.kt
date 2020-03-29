package org.tvheadend.tvhclient.util.worker

import android.content.Context
import android.content.Intent
import androidx.work.Worker
import androidx.work.WorkerParameters
import org.tvheadend.tvhclient.service.HtspIntentService
import timber.log.Timber

class LoadChannelIconWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    companion object {
        const val WORK_NAME = "LoadChannelIconWorker"
    }

    override fun doWork(): Result {
        Timber.d("Loading channel icons from server")
        HtspIntentService.enqueueWork(applicationContext, Intent().setAction("loadChannelIcons"))
        return Result.success()
    }
}
