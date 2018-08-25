package org.tvheadend.tvhclient.features.shared.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.tvheadend.tvhclient.features.shared.callbacks.NetworkStatusReceiverCallback;
import org.tvheadend.tvhclient.utils.NetworkUtils;

public class NetworkStatusReceiver extends BroadcastReceiver {

    private final NetworkStatusReceiverCallback callback;

    public NetworkStatusReceiver(NetworkStatusReceiverCallback callback) {
        this.callback = callback;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        callback.onNetworkStatusChanged(NetworkUtils.isNetworkAvailable(context));
    }
}
