package org.tvheadend.tvhclient.features.shared.callbacks;

public interface NetworkStatusReceiverCallback {
    void onNetworkAvailable();
    void onNetworkNotAvailable();
}
