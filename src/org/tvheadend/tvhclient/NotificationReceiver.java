package org.tvheadend.tvhclient;

import org.tvheadend.tvhclient.model.Recording;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class NotificationReceiver extends BroadcastReceiver {

    @SuppressWarnings("unused")
    private final static String TAG = NotificationReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Received notification");

        // Get the recording of interest
        TVHClientApplication app = (TVHClientApplication) context.getApplicationContext();
        long recId = intent.getLongExtra(Constants.BUNDLE_RECORDING_ID, 0);
        Recording rec = app.getRecording(recId);

        if (rec != null) {
            Log.d(TAG, "Showing notification for recording " + rec.title);

            String msg = "";
            if (rec.error == null && rec.state.equals("scheduled")) {
                msg = "Recording started";
            }
            if (rec.error == null && rec.state.equals("completed")) {
                msg = "Recording completed";
            }

            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
                    context).setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(rec.title)
                    .setContentText(msg)
                    .setVibrate(new long[] { 0, 300, 0 })
                    .setAutoCancel(true);

            mBuilder.setAutoCancel(true);
            NotificationManager mNotificationManager = (NotificationManager) context
                    .getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.notify((int) rec.id, mBuilder.build());
        }
    }

}