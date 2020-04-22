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
import org.tvheadend.tvhclient.ui.features.programs.ProgramDetailsActivity
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

class ProgramNotificationWorker(val context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    companion object {
        const val WORK_NAME = "ProgramNotificationWorker"
    }

    override fun doWork(): Result {

        val eventTitle = inputData.getString("eventTitle")
        val eventId = inputData.getInt("eventId", 0)
        val channelId = inputData.getInt("channelId", 0)
        val startTime = inputData.getLong("start", 0)
        val configName = inputData.getString("configName")
        Timber.d("Received notification broadcast for program $eventTitle")

        // Create the intent that will handle showing the program details
        val detailsIntent = Intent(context, ProgramDetailsActivity::class.java)
        detailsIntent.putExtra("eventId", eventId)
        detailsIntent.putExtra("channelId", channelId)
        detailsIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val detailsPendingIntent = PendingIntent.getActivity(context, 0, detailsIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        // Create the intent that handles the scheduling of the program
        val recordIntent = Intent(context, HtspService::class.java)
        recordIntent.action = "addDvrEntry"
        recordIntent.putExtra("eventId", eventId)
        if (!configName.isNullOrEmpty()) {
            recordIntent.putExtra("configName", configName)
        }
        val recordPendingIntent = PendingIntent.getService(context, 0, recordIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        // Create the title of the notification.
        // The text below the title will be the program name
        val currentTime = Date().time
        val title = if (startTime < currentTime)
            context.resources.getString(R.string.program_has_already_started)
        else {
            val sdf = SimpleDateFormat("HH:mm", Locale.US)
            val minutes = ((startTime - currentTime) / 1000 / 60).toInt()
            context.resources.getQuantityString(R.plurals.program_starts_soon, minutes, sdf.format(startTime), minutes)
        }

        val builder = getNotificationBuilder(context)
        builder.setContentTitle(title)
                .setContentText(eventTitle)
                .setContentIntent(detailsPendingIntent)
                .addAction(R.attr.ic_menu_record_once, context.getString(R.string.record_once), recordPendingIntent)

        NotificationManagerCompat.from(context).notify(eventId, builder.build())

        return Result.success()
    }
}
