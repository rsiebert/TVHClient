package org.tvheadend.tvhclient.ui.features.notification;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.domain.entity.ProgramInterface;
import org.tvheadend.tvhclient.domain.entity.Recording;
import org.tvheadend.tvhclient.domain.entity.ServerProfile;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import timber.log.Timber;

public class NotificationUtils {

    /**
     * Calculated the time that the notification shall be shown from
     * the given start time and the defined offset in the preferences
     *
     * @param context   Context to access android specific resources
     * @param startTime The time in milliseconds that the program or recording starts
     * @return The time in milliseconds when the notification shall be shown
     */
    private static long getNotificationTime(@NonNull Context context, long startTime) {
        //noinspection ConstantConditions
        int offset = Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(context).getString("notification_lead_time", context.getResources().getString(R.string.pref_default_notification_lead_time)));
        long notificationTime = (startTime - (offset * 1000 * 60));
        long currentTime = System.currentTimeMillis();

        Timber.d("Notification time is " + notificationTime + " ms, startTime is " + startTime + " ms, offset is " + offset + " minutes");
        return notificationTime - currentTime;
    }

    /**
     * Returns the notification builder required by the notification manager to show a notification
     * In case android oreo and higher are used then the required notification channel is created
     *
     * @param context Context to access android specific resources
     * @return The notification builder
     */
    public static NotificationCompat.Builder getNotificationBuilder(Context context) {
        String notificationChannelId = context.getString(R.string.app_name);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelName = context.getString(R.string.app_name);
            // Get a new or existing channel and register the channel with the system
            NotificationChannel channel = new NotificationChannel(notificationChannelId, channelName, NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.createNotificationChannel(channel);
            }
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, notificationChannelId);
        builder.setSmallIcon(R.drawable.ic_notification).setAutoCancel(true);
        return builder;
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
    public static void addNotification(@NonNull Context context,
                                       @NonNull Recording recording) {

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (preferences.getBoolean("notifications_enabled", context.getResources().getBoolean(R.bool.pref_default_notifications_enabled))
                && recording.isScheduled()
                && recording.getStart() > System.currentTimeMillis()
                && !TextUtils.isEmpty(recording.getTitle())) {

            Data data = new Data.Builder()
                    .putString("dvrTitle", recording.getTitle())
                    .putInt("dvrId", recording.getEventId())
                    .putLong("start", recording.getStart())
                    .build();

            OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(RecordingNotificationWorker.class)
                    .setInitialDelay(getNotificationTime(context, recording.getStart()), TimeUnit.MILLISECONDS)
                    .setInputData(data)
                    .build();
            String uniqueWorkName = "RecordingNotification_" + String.valueOf(recording.getId());
            WorkManager.getInstance().enqueueUniqueWork(uniqueWorkName, ExistingWorkPolicy.REPLACE, workRequest);
        }
    }

    /**
     * Removes a notification with the given id if it exists
     *
     * @param context Context to access android specific resources
     * @param id      The id of the program or recording
     */
    public static void removeNotificationById(@NonNull Context context, int id) {
        if (PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean("notifications_enabled", context.getResources().getBoolean(R.bool.pref_default_notifications_enabled))) {
            Timber.d("Removing notification for id " + id);

            String uniqueWorkName = "RecordingNotification_" + String.valueOf(id);
            WorkManager.getInstance().cancelUniqueWork(uniqueWorkName);
            ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(id);
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
    public static void addNotification(@NonNull Context context,
                                       @NonNull ProgramInterface program,
                                       @Nullable ServerProfile profile) {

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (preferences.getBoolean("notifications_enabled", context.getResources().getBoolean(R.bool.pref_default_notifications_enabled))) {
            Data data = new Data.Builder()
                    .putString("eventTitle", program.getTitle())
                    .putInt("eventId", program.getEventId())
                    .putInt("channelId", program.getChannelId())
                    .putLong("start", program.getStart())
                    .putString("configName", profile != null ? profile.getName() : "")
                    .build();

            OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(ProgramNotificationWorker.class)
                    .setInitialDelay(getNotificationTime(context, program.getStart()), TimeUnit.MILLISECONDS)
                    .setInputData(data)
                    .build();
            String uniqueWorkName = "ProgramNotification_" + String.valueOf(program.getEventId());
            WorkManager.getInstance().enqueueUniqueWork(uniqueWorkName, ExistingWorkPolicy.REPLACE, workRequest);
        }
    }
}
