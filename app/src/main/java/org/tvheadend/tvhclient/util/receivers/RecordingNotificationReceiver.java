package org.tvheadend.tvhclient.util.receivers;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.service.HtspService;
import org.tvheadend.tvhclient.util.NotificationUtils;

import java.util.Date;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import timber.log.Timber;

public class RecordingNotificationReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        String dvrTitle = intent.getStringExtra("dvrTitle");
        int dvrId = intent.getIntExtra("dvrId", 0);
        long startTime = intent.getLongExtra("start", 0);
        Timber.d("Received notification broadcast for recording " + dvrTitle);

        // Create the intent that handles the cancelling of the scheduled recording
        Intent recordIntent = new Intent(context, HtspService.class);
        recordIntent.setAction("cancelDvrEntry");
        recordIntent.putExtra("id", dvrId);
        PendingIntent cancelRecordingPendingIntent = PendingIntent.getService(context, 0, recordIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        // Create the title of the notification.
        // The text below the title will be the recording name
        long currentTime = new Date().getTime();
        String title = "Recording starts in " + ((startTime - currentTime) / 1000 / 60) + " minutes.";

        NotificationCompat.Builder builder = NotificationUtils.getNotificationBuilder(context);
        builder.setContentTitle(title)
                .setContentText(dvrTitle)
                .addAction(R.attr.ic_menu_record_cancel, context.getString(R.string.record_cancel), cancelRecordingPendingIntent);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(dvrId, builder.build());
    }
}
