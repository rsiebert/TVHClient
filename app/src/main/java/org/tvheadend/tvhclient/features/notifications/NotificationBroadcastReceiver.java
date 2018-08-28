package org.tvheadend.tvhclient.features.notifications;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.os.Build;
import android.support.v4.app.NotificationCompat;

import org.tvheadend.tvhclient.R;

public abstract class NotificationBroadcastReceiver extends BroadcastReceiver {

    NotificationCompat.Builder getNotificationBuilder(Context context) {
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
}
