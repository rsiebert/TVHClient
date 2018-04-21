package org.tvheadend.tvhclient.features.notifications;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.remote.EpgSyncService;

import java.util.Date;

public class RecordingNotificationReceiver extends NotificationBroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (!sharedPreferences.getBoolean("notifications_enabled", true)) {
            return;
        }

        String dvrTitle = intent.getStringExtra("dvrTitle");
        int dvrId = intent.getIntExtra("dvrId", 0);
        long startTime = intent.getLongExtra("start", 0);

        // Create the intent that handles the cancelling of the scheduled recording
        Intent recordIntent = new Intent(context, EpgSyncService.class);
        recordIntent.setAction("cancelDvrEntry");
        recordIntent.putExtra("id", dvrId);
        PendingIntent cancelRecordingPendingIntent = PendingIntent.getService(context, 0, recordIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        // Create the title of the notification.
        // The text below the title will be the recording name
        long currentTime = new Date().getTime();
        String title = "Recording starts in " + ((startTime - currentTime) / 1000 / 60) + " minutes.";

        NotificationCompat.Builder builder = getNotificationBuilder(context);
        builder.setContentTitle(title)
                .setContentText(dvrTitle)
                .addAction(R.attr.ic_menu_record_cancel, context.getString(R.string.record_cancel), cancelRecordingPendingIntent);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(dvrId, builder.build());
    }
}
