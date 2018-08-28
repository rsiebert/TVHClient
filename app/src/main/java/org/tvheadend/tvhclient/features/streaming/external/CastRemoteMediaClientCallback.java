package org.tvheadend.tvhclient.features.streaming.external;

import com.google.android.gms.cast.framework.media.RemoteMediaClient;

import timber.log.Timber;

class CastRemoteMediaClientCallback extends RemoteMediaClient.Callback {

    private final RemoteMediaClient remoteMediaClient;

    CastRemoteMediaClientCallback(RemoteMediaClient remoteMediaClient) {
        this.remoteMediaClient = remoteMediaClient;
    }

    @Override
    public void onStatusUpdated() {
        Timber.d("onStatusUpdated");
        remoteMediaClient.unregisterCallback(this);
    }

    @Override
    public void onMetadataUpdated() {
        Timber.d("onMetadataUpdated");
    }

    @Override
    public void onQueueStatusUpdated() {
        Timber.d("onQueueStatusUpdated");
    }

    @Override
    public void onPreloadStatusUpdated() {
        Timber.d("onPreloadStatusUpdated");
    }

    @Override
    public void onSendingRemoteMediaRequest() {
        Timber.d("onSendingRemoteMediaRequest");
    }

    @Override
    public void onAdBreakStatusUpdated() {
        Timber.d("onAdBreakStatusUpdated");
    }
}
