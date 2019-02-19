package org.tvheadend.tvhclient.util.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.tvheadend.tvhclient.MainApplication;

import java.lang.ref.WeakReference;

public class SyncStateReceiver extends BroadcastReceiver {

    public final static String ACTION = "service_status";
    public final static String STATE = "state";
    public final static String MESSAGE = "message";
    public final static String DETAILS = "details";

    private final WeakReference<Listener> callback;

    public enum State {
        CLOSED,
        CONNECTING,
        CONNECTED,
        SYNC_STARTED,
        SYNC_IN_PROGRESS,
        SYNC_DONE,
        IDLE,
        FAILED
    }

    public SyncStateReceiver(Listener callback) {
        this.callback = new WeakReference<>(callback);
    }

    /**
     * This receiver handles the data that was given from an intent via the LocalBroadcastManager.
     * The main message is sent via the "message" extra. Any details about the state is given
     * via the "details" extra.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        if (callback.get() != null && MainApplication.isActivityVisible()) {
            callback.get().onSyncStateChanged(
                    (State) intent.getSerializableExtra(STATE),
                    intent.getStringExtra(MESSAGE),
                    intent.getStringExtra(DETAILS));
        }
    }

    public interface Listener {
        /**
         * Interface method that is called then a local broadcast was received
         * with the connection and synchronization state including any text messages
         *
         * @param state   The current connection and synchronization state
         * @param message Main text message describing the state
         * @param details Additional text message to describe any details
         */
        void onSyncStateChanged(SyncStateReceiver.State state, String message, String details);
    }
}
