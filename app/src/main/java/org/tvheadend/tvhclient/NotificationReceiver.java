package org.tvheadend.tvhclient;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import org.tvheadend.tvhclient.model.Recording;

public class NotificationReceiver extends BroadcastReceiver {

    private final static String GROUP_KEY = "recordings";

    @Override
    public void onReceive(Context context, Intent intent) {

        // Get the recording of interest
        int recId = intent.getIntExtra("dvrId", 0);
        String msg = intent.getStringExtra(Constants.BUNDLE_NOTIFICATION_MSG);
        final Recording rec = DataStorage.getInstance().getRecordingFromArray(recId);

        if (rec != null) {
            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
                    context).setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(rec.title)
                    .setContentText(msg)
                    .setGroup(GROUP_KEY)
                    .setAutoCancel(true);

            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            nm.notify(rec.id, mBuilder.build());
        }
    }
}
