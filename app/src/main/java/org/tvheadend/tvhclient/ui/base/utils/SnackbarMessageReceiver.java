package org.tvheadend.tvhclient.ui.base.utils;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.google.android.material.snackbar.Snackbar;

import java.lang.ref.WeakReference;

/**
 * This receiver handles the data that was given from an intent via the LocalBroadcastManager.
 * Any string in the given extra will be shown as a snackbar.
 */
public class SnackbarMessageReceiver extends BroadcastReceiver {

    public final static String ACTION = "snackbar_message";
    public final static String CONTENT = "content";
    public final static String DURATION = "duration";
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
                int duration = intent.getIntExtra(DURATION, Snackbar.LENGTH_SHORT);
                Snackbar.make(activity.getCurrentFocus(), msg, duration).show();
            }
        }
    }
}
