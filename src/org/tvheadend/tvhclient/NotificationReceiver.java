package org.tvheadend.tvhclient;

import org.tvheadend.tvhclient.model.Recording;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class NotificationReceiver extends BroadcastReceiver {

    private final static String TAG = NotificationReceiver.class.getSimpleName();
    private final static String GROUP_KEY = "recordings";

    @Override
    public void onReceive(Context context, Intent intent) {

        // Get the recording of interest
        TVHClientApplication app = (TVHClientApplication) context.getApplicationContext();
        long recId = intent.getLongExtra(Constants.BUNDLE_RECORDING_ID, 0);
        String msg = intent.getStringExtra(Constants.BUNDLE_NOTIFICATION_MSG);
        final Recording rec = app.getRecording(recId);

        if (rec != null) {
            app.log(TAG, "Showing notification for recording " + rec.title);
            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
                    context).setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(rec.title)
                    .setContentText(msg)
                    .setGroup(GROUP_KEY)
                    .setAutoCancel(true);

            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            nm.notify((int) rec.id, mBuilder.build());
        }
    }
}
