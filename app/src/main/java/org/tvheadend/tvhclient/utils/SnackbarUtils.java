package org.tvheadend.tvhclient.utils;

import android.content.Context;
import android.content.Intent;

import org.tvheadend.tvhclient.features.shared.receivers.SnackbarMessageReceiver;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class SnackbarUtils {

    public static void sendSnackbarMessage(Context context, int resId) {
        sendSnackbarMessage(context, context.getString(resId));
    }

    public static void sendSnackbarMessage(Context context, String msg) {
        Intent intent = new Intent(SnackbarMessageReceiver.ACTION);
        intent.putExtra(SnackbarMessageReceiver.CONTENT, msg);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }
}
