package org.tvheadend.tvhclient.features.shared.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.tvheadend.tvhclient.data.service.EpgSyncStatusCallback;
import org.tvheadend.tvhclient.data.service.EpgSyncTaskState;

public class ServiceStatusReceiver extends BroadcastReceiver {

    public final static String ACTION = "service_status";
    public final static String STATE = "state";
    public final static String MESSAGE = "message";
    public final static String DETAILS = "details";

    private final EpgSyncStatusCallback callback;

    public enum State {
        CLOSED,
        CONNECTING,
        CONNECTED,
        SYNC_STARTED,
        SYNC_IN_PROGRESS,
        SYNC_DONE,
        FAILED
    }

    public ServiceStatusReceiver(EpgSyncStatusCallback callback) {
        this.callback = callback;
    }

    /**
     * This receiver handles the data that was given from an intent via the LocalBroadcastManager.
     * The main message is sent via the "message" extra. Any details about the state is given
     * via the "details" extra.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        callback.onEpgTaskStateChanged(
                new EpgSyncTaskState.EpgSyncTaskStateBuilder()
                        .state((State) intent.getSerializableExtra(STATE))
                        .message(intent.getStringExtra(MESSAGE))
                        .details(intent.getStringExtra(DETAILS))
                        .build());
    }
}
