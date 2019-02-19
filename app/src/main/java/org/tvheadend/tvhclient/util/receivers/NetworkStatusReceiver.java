package org.tvheadend.tvhclient.util.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.tvheadend.tvhclient.MainApplication;
import org.tvheadend.tvhclient.ui.base.callbacks.NetworkStatusListener;
import org.tvheadend.tvhclient.util.NetworkUtils;

import java.lang.ref.WeakReference;

public class NetworkStatusReceiver extends BroadcastReceiver {

    private final WeakReference<NetworkStatusListener> callback;

    public NetworkStatusReceiver(NetworkStatusListener callback) {
        this.callback = new WeakReference<>(callback);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (callback.get() != null && MainApplication.isActivityVisible()) {
            callback.get().onNetworkStatusChanged(NetworkUtils.isConnectionAvailable(context));
        }
    }
}
