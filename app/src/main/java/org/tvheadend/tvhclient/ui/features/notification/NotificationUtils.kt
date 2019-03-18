@file:JvmName("NotificationUtils")

package org.tvheadend.tvhclient.ui.features.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.preference.PreferenceManager
import android.text.TextUtils
import androidx.core.app.NotificationCompat
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.domain.entity.ProgramInterface
import org.tvheadend.tvhclient.domain.entity.Recording
import org.tvheadend.tvhclient.domain.entity.ServerProfile
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Calculated the time that the notification shall be shown from
 * the given start time and the defined offset in the preferences
 *
 * @param context   Context to access android specific resources
 * @param startTime The time in milliseconds that the program or recording starts
 * @return The time in milliseconds when the notification shall be shown
 */
fun getNotificationTime(context: Context, startTime: Long): Long {

    val offset = Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(context).getString("notification_lead_time", context.resources.getString(R.string.pref_default_notification_lead_time))!!)
    val notificationTime = startTime - offset * 1000 * 60
    val currentTime = System.currentTimeMillis()

    Timber.d("Notification time is $notificationTime ms, startTime is $startTime ms, offset is $offset minutes")
    return notificationTime - currentTime
}

/**
 * Returns the notification builder required by the notification manager to show a notification
 * In case android oreo and higher are used then the required notification channel is created
 *
 * @param context Context to access android specific resources
 * @return The notification builder
 */
fun getNotificationBuilder(context: Context): NotificationCompat.Builder {
    val notificationChannelId = context.getString(R.string.app_name)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channelName = context.getString(R.string.app_name)
        // Get a new or existing channel and register the channel with the system
        val channel = NotificationChannel(notificationChannelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }

    val builder = NotificationCompat.Builder(context, notificationChannelId)
    builder.setSmallIcon(R.drawable.ic_notification).setAutoCancel(true)
    return builder
}

/**
 * Creates a worker that will create a notification with the recording details at
 * the given notification time. The recording must be scheduled and its start time
 * must be in the future and its title must not be empty so that it can be shown
 * in the notification.
 *
 * @param context   Context to access android specific resources
 * @param recording The recording for which the notification shall be created
 */
fun addNotification(context: Context, recording: Recording) {

    val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    if (preferences.getBoolean("notifications_enabled", context.resources.getBoolean(R.bool.pref_default_notifications_enabled))
            && recording.isScheduled
            && recording.start > System.currentTimeMillis()
            && !TextUtils.isEmpty(recording.title)) {

        val data = Data.Builder()
                .putString("dvrTitle", recording.title)
                .putInt("dvrId", recording.eventId)
                .putLong("start", recording.start)
                .build()

        val workRequest = OneTimeWorkRequest.Builder(RecordingNotificationWorker::class.java)
                .setInitialDelay(getNotificationTime(context, recording.start), TimeUnit.MILLISECONDS)
                .setInputData(data)
                .build()
        val uniqueWorkName = "RecordingNotification_" + recording.id.toString()
        WorkManager.getInstance().enqueueUniqueWork(uniqueWorkName, ExistingWorkPolicy.REPLACE, workRequest)
    }
}

/**
 * Removes a notification with the given id if it exists
 *
 * @param context Context to access android specific resources
 * @param id      The id of the program or recording
 */
fun removeNotificationById(context: Context, id: Int) {
    if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("notifications_enabled", context.resources.getBoolean(R.bool.pref_default_notifications_enabled))) {
        Timber.d("Removing notification for id $id")

        val uniqueWorkName = "RecordingNotification_$id"
        WorkManager.getInstance().cancelUniqueWork(uniqueWorkName)
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(id)
    }
}

/**
 * Creates a worker that will create a notification with the program details at
 * the given notification time. In case the a profile is selected it is also
 * added so that the user can record the program with the given profile
 *
 * @param context Context to access android specific resources
 * @param program The program for which the notification shall be created
 * @param profile The selected recording profile
 */
fun addNotification(context: Context, program: ProgramInterface, profile: ServerProfile?) {

    val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    if (preferences.getBoolean("notifications_enabled", context.resources.getBoolean(R.bool.pref_default_notifications_enabled))) {
        val data = Data.Builder()
                .putString("eventTitle", program.title)
                .putInt("eventId", program.eventId)
                .putInt("channelId", program.channelId)
                .putLong("start", program.start)
                .putString("configName", if (profile != null) profile.name else "")
                .build()

        val workRequest = OneTimeWorkRequest.Builder(ProgramNotificationWorker::class.java)
                .setInitialDelay(getNotificationTime(context, program.start), TimeUnit.MILLISECONDS)
                .setInputData(data)
                .build()
        val uniqueWorkName = "ProgramNotification_" + program.eventId.toString()
        WorkManager.getInstance().enqueueUniqueWork(uniqueWorkName, ExistingWorkPolicy.REPLACE, workRequest)
    }
}

