package org.tvheadend.tvhclient.util.worker

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.service.HtspService
import org.tvheadend.tvhclient.ui.common.getNotificationBuilder
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

class RecordingNotificationWorker(val context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    companion object {
        const val WORK_NAME = "RecordingNotificationWorker"
    }

    override fun doWork(): Result {

        val dvrTitle = inputData.getString("dvrTitle")
        val dvrId = inputData.getInt("dvrId", 0)
        val startTime = inputData.getLong("start", 0)
        Timber.d("Received notification broadcast for recording $dvrTitle")

        // Create the intent that handles the cancelling of the scheduled recording
        val recordIntent = Intent(context, HtspService::class.java)
        recordIntent.action = "cancelDvrEntry"
        recordIntent.putExtra("id", dvrId)
        val cancelRecordingPendingIntent = PendingIntent.getService(context, 0, recordIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        // Create the title of the notification.
        // The text below the title will be the recording name
        val sdf = SimpleDateFormat("HH:mm", Locale.US)
        val title = "Recording starts at ${sdf.format(startTime)} in ${(startTime - Date().time) / 1000 / 60} minutes."

        val builder = getNotificationBuilder(context)
        builder.setContentTitle(title)
                .setContentText(dvrTitle)
                .addAction(R.attr.ic_menu_record_cancel, context.getString(R.string.record_cancel), cancelRecordingPendingIntent)

        NotificationManagerCompat.from(context).notify(dvrId, builder.build())

        return Result.success()
    }
}
