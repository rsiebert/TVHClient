package org.tvheadend.tvhclient.features.shared.receivers;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.design.widget.Snackbar;

import java.lang.ref.WeakReference;

/**
 * This receiver handles the data that was given from an intent via the LocalBroadcastManager.
 * Any string in the given extra will be shown as a snackbar.
 */
public class SnackbarMessageReceiver extends BroadcastReceiver {

    public final static String ACTION = "message";
    public final static String CONTENT = "content";
    private final WeakReference<Activity> activity;

    public SnackbarMessageReceiver(Activity activity) {
        this.activity = new WeakReference<>(activity);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.hasExtra(CONTENT)) {
            Activity activity = this.activity.get();
            if (activity != null && activity.getCurrentFocus() != null) {
                String msg = intent.getStringExtra(CONTENT);
                Snackbar.make(activity.getCurrentFocus(), msg, Snackbar.LENGTH_SHORT).show();
            }
        }
    }
}
