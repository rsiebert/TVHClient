package org.tvheadend.tvhclient.util.worker

import android.content.Context
import android.content.Intent
import androidx.work.Worker
import androidx.work.WorkerParameters
import org.tvheadend.tvhclient.service.ConnectionIntentService
import timber.log.Timber

class EpgDataUpdateWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    companion object {
        const val WORK_NAME = "EpgDataUpdateWorker"
    }

    override fun doWork(): Result {
        Timber.d("Loading more event data from server")

        val intent = Intent()
        intent.action = "getMoreEvents"
        intent.putExtra("numFollowing", 250)
        ConnectionIntentService.enqueueWork(applicationContext, intent)
        return Result.success()
    }
}
