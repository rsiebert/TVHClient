package org.tvheadend.tvhclient.data.service;

public interface EpgSyncStatusCallback {

    void onEpgTaskStateChanged(EpgSyncTaskState state);
}
