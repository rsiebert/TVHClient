package org.tvheadend.tvhclient.data.local;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.data.Constants;
import org.tvheadend.tvhclient.data.DataStorage;
import org.tvheadend.tvhclient.data.model.Recording;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class NotificationHandler {

    private final static String TAG = Logger.class.getSimpleName();

    private static NotificationHandler mInstance;
    private final Logger logger;
    private final TVHClientApplication app;
    private final DataStorage dataStorage;

    public NotificationHandler() {
        logger = Logger.getInstance();
        app = TVHClientApplication.getInstance();
        dataStorage = DataStorage.getInstance();
    }

    public static synchronized NotificationHandler getInstance() {
        if (mInstance == null)
            mInstance = new NotificationHandler();
        return mInstance;
    }

    /**
     * Calls the actual method to add a notification for the giving recording id
     * and the required time that the notification shall be shown earlier.
     *
     * @param id    The id of the recording
     */
    public void addNotification(int id) {
        Context context = TVHClientApplication.getInstance();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        long offset = 0;
        try {
            offset = Integer.valueOf(prefs.getString("pref_show_notification_offset", "0"));
        } catch(NumberFormatException ex) {
            // NOP
        }
        addNotification(id, offset);
    }

    /**
     * Adds the notification of the given recording id and offset time with the
     * notification message. Two notifications will be created, one that the
     * recording has started and another that it has ended regardless of the
     * recording state.
     *
     * @param id        Id of the recording for which the notification shall be shown
     * @param offset    Time in minutes that the notification shall be shown earlier
     */
    private void addNotification(final int id, final long offset) {
        Logger logger = Logger.getInstance();
        logger.log(TAG, "addNotification() called with: id = [" + id + "], offset = [" + offset + "]");

        final Recording rec = dataStorage.getRecordingFromArray(id);
        if (dataStorage.isLoading() || rec == null) {
            return;
        }

        // The start time when the notification shall be shown
        String msg = app.getString(R.string.recording_started);
        long time = rec.getStart();
        if (time > (new Date()).getTime()) {
            logger.log(TAG, "addNotification: Recording added");
            if (offset > 0) {
                // Subtract the offset from the time when the notification shall be shown.
                time -= (offset * 60000);
                msg = app.getString(R.string.recording_starts_in, offset);
            }

            // Create the intent for the start and stop notifications
            createNotification(rec.getId(), time, msg);
            createNotification(rec.getId() * 100, rec.getStop(), app.getString(R.string.recording_completed));
        } else {
            logger.log(TAG, "addNotification: Recording not added, start time is in the past");
        }
    }

    /**
     * Creates the required intent for the notification and passed
     * it on to the alarm manager to the notification can be shown later
     *
     * @param id    The id of the recording
     * @param time  Time when the notification shall be shown
     * @param msg   Message that will be displayed
     */
    private void createNotification(int id, long time, String msg) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy, HH.mm", Locale.US);
        logger.log(TAG, "createNotification() called with: id = [" + id + "], time = [" + sdf.format(time) + "], msg = [" + msg + "]");

        Intent intent = new Intent(app.getApplicationContext(), NotificationReceiver.class);
        Bundle bundle = new Bundle();
        bundle.putInt("dvrId", id);
        bundle.putString(Constants.BUNDLE_NOTIFICATION_MSG, msg);
        intent.putExtras(bundle);

        PendingIntent pi = PendingIntent.getBroadcast(app.getApplicationContext(), id, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager am = (AlarmManager) app.getSystemService(Service.ALARM_SERVICE);
        am.set(AlarmManager.RTC_WAKEUP, time, pi);
    }

    /**
     * Cancels any pending start and stop notifications with the given id
     *
     * @param id    The id of the recording
     */
    public void cancelNotification(int id) {
        logger.log(TAG, "cancelNotification() called with: id = [" + id + "]");
        NotificationManager nm = (NotificationManager) app.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(id);
        nm.cancel(id * 100);
    }

    /**
     * Adds notifications for all recordings that are scheduled.
     *
     * @param offset Time in minutes that the notification shall be shown earlier
     */
    public void addNotifications(final long offset) {
        Map<Integer, Recording> map = dataStorage.getRecordingsFromArray();
        for (Recording rec : map.values()) {
            if (rec.isScheduled()) {
                addNotification(rec.getId(), offset);
            }
        }
    }

    /**
     * Cancels all pending notifications related to recordings
     */
    public void cancelNotifications() {
        Map<Integer, Recording> map = dataStorage.getRecordingsFromArray();
        for (Recording rec : map.values()) {
            cancelNotification(rec.getId());
        }
    }
}
