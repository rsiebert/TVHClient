package org.tvheadend.tvhclient.features.shared.callbacks;

public interface NetworkStatusReceiverCallback {
    void onNetworkStatusChanged(boolean isAvailable);
}
