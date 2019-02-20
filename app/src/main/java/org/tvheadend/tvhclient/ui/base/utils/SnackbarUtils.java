package org.tvheadend.tvhclient.ui.base.utils;

import android.content.Context;
import android.content.Intent;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import timber.log.Timber;

public class SnackbarUtils {

    public static void sendSnackbarMessage(Context context, int resId) {
        sendSnackbarMessage(context, context.getString(resId));
    }

    public static void sendSnackbarMessage(Context context, String msg) {
        Timber.d("Sending broadcast to show snackbar message " + msg);
        Intent intent = new Intent(SnackbarMessageReceiver.ACTION);
        intent.putExtra(SnackbarMessageReceiver.CONTENT, msg);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }
}
