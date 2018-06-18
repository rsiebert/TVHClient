package org.tvheadend.tvhclient.features.shared.callbacks;

public interface NetworkStatusCallback {
    void onNetworkAvailable();
    void onNetworkNotAvailable();
}
