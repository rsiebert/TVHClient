package org.tvheadend.tvhclient.utils;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.tvheadend.tvhclient.data.entity.Program;
import org.tvheadend.tvhclient.data.entity.Recording;
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

    public static void addProgramNotification(@NonNull Context context, @NonNull Program program, Integer offset, @Nullable ServerProfile profile, @NonNull ServerStatus serverStatus) {
        Intent intent = new Intent(context, ProgramNotificationReceiver.class);
        intent.putExtra("eventTitle", program.getTitle());
        intent.putExtra("eventId", program.getEventId());
        intent.putExtra("channelId", program.getChannelId());
        intent.putExtra("start", program.getStart());

        if (MiscUtils.isServerProfileEnabled(profile, serverStatus)) {
            intent.putExtra("configName", profile.getName());
        }

        long notificationTime = getNotificationTime(program.getStart(), offset);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, program.getEventId(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager am = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        if (am != null) {
            Timber.d("Created notification for program " + program.getTitle());
            am.set(AlarmManager.RTC_WAKEUP, notificationTime, pendingIntent);
        } else {
            Timber.e("Could not get alarm manager to create notification for program " + program.getTitle());
        }
    }

    public static void addRecordingNotification(@NonNull Context context, @NonNull Recording recording, int offset) {
        Intent intent = new Intent(context, RecordingNotificationReceiver.class);
        intent.putExtra("dvrTitle", recording.getTitle());
        intent.putExtra("dvrId", recording.getId());
        intent.putExtra("start", recording.getStart());

        long notificationTime = getNotificationTime(recording.getStart(), offset);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, recording.getId(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager am = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        if (am != null) {
            Timber.d("Created notification for recording " + recording.getTitle());
            am.set(AlarmManager.RTC_WAKEUP, notificationTime, pendingIntent);
        } else {
            Timber.e("Could not get alarm manager to create notification for recording " + recording.getTitle());
        }
    }

    private static long getNotificationTime(long startTime, int offset) {
        long currentTime = new Date().getTime();
        long notificationTime = (startTime - (offset * 1000 * 60));
        if (notificationTime < currentTime) {
            notificationTime = currentTime;
        }
        return notificationTime;
    }

    public static void removeRecordingNotification(Context context, long id) {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.cancel((int) id);
        }
    }
}
