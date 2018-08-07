package org.tvheadend.tvhclient.features.shared.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.tvheadend.tvhclient.features.shared.callbacks.NetworkStatusReceiverCallback;
import org.tvheadend.tvhclient.utils.NetworkUtils;

import timber.log.Timber;

public class NetworkStatusReceiver extends BroadcastReceiver {

    private final NetworkStatusReceiverCallback callback;

    public NetworkStatusReceiver(NetworkStatusReceiverCallback callback) {
        this.callback = callback;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Timber.d("Received network change " + intent.getAction());
        if (NetworkUtils.isNetworkAvailable(context)) {
            callback.onNetworkAvailable();
        } else {
            callback.onNetworkNotAvailable();
        }
    }
}
