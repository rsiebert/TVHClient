package org.tvheadend.tvhclient.utils;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.tvheadend.tvhclient.data.entity.ServerProfile;
import org.tvheadend.tvhclient.data.entity.ServerStatus;
import org.tvheadend.tvhclient.features.notifications.ProgramNotificationReceiver;
import org.tvheadend.tvhclient.features.notifications.RecordingNotificationReceiver;

import java.util.Date;

import timber.log.Timber;

import static android.content.Context.ALARM_SERVICE;

public class NotificationUtils {

    private NotificationUtils() {
        throw new IllegalAccessError("Utility class");
    }

    /**
     *
     * @param context
     * @param title
     * @param eventId
     * @param channelId
     * @param start
     * @param offset
     * @param profile
     * @param serverStatus
     */
    static void addProgramNotification(@NonNull Context context, @NonNull String title, int eventId, int channelId, long start, int offset, @Nullable ServerProfile profile, @NonNull ServerStatus serverStatus) {

        Intent intent = new Intent(context, ProgramNotificationReceiver.class);
        intent.putExtra("eventTitle", title);
        intent.putExtra("eventId", eventId);
        intent.putExtra("channelId", channelId);
        intent.putExtra("start", start);

        if (MiscUtils.isServerProfileEnabled(profile, serverStatus)) {
            intent.putExtra("configName", profile.getName());
        }

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, eventId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager am = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        if (am != null) {
            Timber.d("Created notification for program " + title + " with id " + eventId);
            am.set(AlarmManager.RTC_WAKEUP, getNotificationTime(start, offset), pendingIntent);
        } else {
            Timber.e("Could not get alarm manager to create notification for program " + title + " with id " + eventId);
        }
    }

    /**
     *
     * @param context
     * @param title
     * @param dvrId
     * @param start
     * @param offset
     */
    public static void addRecordingNotification(@NonNull Context context, @NonNull String title, int dvrId, long start, int offset) {
        Intent intent = new Intent(context, RecordingNotificationReceiver.class);
        intent.putExtra("dvrTitle", title);
        intent.putExtra("dvrId", dvrId);
        intent.putExtra("start", start);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, dvrId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager am = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        if (am != null) {
            Timber.d("Created notification for recording " + title + " with id " + dvrId);
            am.set(AlarmManager.RTC_WAKEUP, getNotificationTime(start, offset), pendingIntent);
        } else {
            Timber.e("Could not get alarm manager to create notification for recording " + title + " with id " + dvrId);
        }
    }

    /**
     *
     * @param startTime
     * @param offset
     * @return
     */
    private static long getNotificationTime(long startTime, int offset) {
        long currentTime = new Date().getTime();
        long notificationTime = (startTime - (offset * 1000 * 60));
        if (notificationTime < currentTime) {
            notificationTime = currentTime;
        }
        Timber.d("Notification time is " + notificationTime + " ms, startTime is " + startTime + " ms, offset is " + offset + " minutes");
        return notificationTime;
    }

    public static void removeRecordingNotification(Context context, long id) {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.cancel((int) id);
        }
    }
}
