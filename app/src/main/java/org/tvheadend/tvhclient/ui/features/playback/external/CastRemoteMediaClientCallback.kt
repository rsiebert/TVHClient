package org.tvheadend.tvhclient.ui.features.playback.external

import com.google.android.gms.cast.framework.media.RemoteMediaClient

import timber.log.Timber

internal class CastRemoteMediaClientCallback(private val remoteMediaClient: RemoteMediaClient) : RemoteMediaClient.Callback() {

    override fun onStatusUpdated() {
        Timber.d("onStatusUpdated")
        remoteMediaClient.unregisterCallback(this)
    }

    override fun onMetadataUpdated() {
        Timber.d("onMetadataUpdated")
    }

    override fun onQueueStatusUpdated() {
        Timber.d("onQueueStatusUpdated")
    }

    override fun onPreloadStatusUpdated() {
        Timber.d("onPreloadStatusUpdated")
    }

    override fun onSendingRemoteMediaRequest() {
        Timber.d("onSendingRemoteMediaRequest")
    }

    override fun onAdBreakStatusUpdated() {
        Timber.d("onAdBreakStatusUpdated")
    }
}
