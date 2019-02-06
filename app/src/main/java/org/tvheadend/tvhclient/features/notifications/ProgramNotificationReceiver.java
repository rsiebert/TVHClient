package org.tvheadend.tvhclient.features.notifications;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.features.programs.ProgramDetailsActivity;
import org.tvheadend.tvhclient.utils.MiscUtils;

import java.util.Date;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class ProgramNotificationReceiver extends NotificationBroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (!sharedPreferences.getBoolean("notifications_enabled", true)) {
            return;
        }

        String eventTitle = intent.getStringExtra("eventTitle");
        int eventId = intent.getIntExtra("eventId", 0);
        int channelId = intent.getIntExtra("channelId", 0);
        long startTime = intent.getLongExtra("start", 0);
        String configName = intent.getStringExtra("configName");

        // Create the intent that will handle showing the program details
        Intent detailsIntent = new Intent(context, ProgramDetailsActivity.class);
        detailsIntent.putExtra("eventId", eventId);
        detailsIntent.putExtra("channelId", channelId);
        detailsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent detailsPendingIntent = PendingIntent.getActivity(context, 0, detailsIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        // Create the intent that handles the scheduling of the program
        Intent recordIntent = new Intent(context, MiscUtils.getSelectedService(context));
        recordIntent.setAction("addDvrEntry");
        recordIntent.putExtra("eventId", eventId);
        if (!TextUtils.isEmpty(configName)) {
            recordIntent.putExtra("configName", configName);
        }
        PendingIntent recordPendingIntent = PendingIntent.getService(context, 0, recordIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        // Create the title of the notification.
        // The text below the title will be the program name
        long currentTime = new Date().getTime();
        String title = startTime < currentTime ?
                "Program has already started." :
                "Program starts in " + ((startTime - currentTime) / 1000 / 60) + " minutes.";

        NotificationCompat.Builder builder = getNotificationBuilder(context);
        builder.setContentTitle(title)
                .setContentText(eventTitle)
                .setContentIntent(detailsPendingIntent)
                .addAction(R.attr.ic_menu_record_once, context.getString(R.string.record_once), recordPendingIntent);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(eventId, builder.build());
    }
}
