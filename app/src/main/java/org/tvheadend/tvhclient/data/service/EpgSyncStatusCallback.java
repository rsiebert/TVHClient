package org.tvheadend.tvhclient.data.service;

public interface EpgSyncStatusCallback {

    void onEpgSyncMessageChanged(String msg, String details);

    void onEpgSyncStateChanged(EpgSyncStatusReceiver.State state);
}
