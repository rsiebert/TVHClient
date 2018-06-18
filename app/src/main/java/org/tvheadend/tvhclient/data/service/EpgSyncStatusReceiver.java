package org.tvheadend.tvhclient.data.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.tvheadend.tvhclient.data.service.htsp.HtspConnection;
import org.tvheadend.tvhclient.data.service.htsp.tasks.Authenticator;

public class EpgSyncStatusReceiver extends BroadcastReceiver {

    public final static String ACTION = "service_status";
    public final static String CONNECTION_STATE = "connection_state";
    public final static String AUTH_STATE = "authentication_state";
    public final static String SYNC_STATE = "sync_state";

    private final EpgSyncStatusCallback callback;

    public enum State {
        FAILED,
        DONE
    }

    public EpgSyncStatusReceiver(EpgSyncStatusCallback callback) {
        this.callback = callback;
    }

    /**
     * This receiver handles the data that was given from an intent via the LocalBroadcastManager.
     * The main message is sent via the "state" extra. Any details about the state is given
     * via the "details" extra. When the extra "done" was received the startup of the app
     * is considered done. The pending intents for the background data sync are started.
     * Finally the defined main fragment like the channel list will be shown.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.hasExtra(CONNECTION_STATE)) {
            HtspConnection.State state = (HtspConnection.State) intent.getSerializableExtra("connection_state");
            switch (state) {
                case CLOSED:
                    callback.onEpgSyncMessageChanged("Connection closed", "");
                    break;
                case CONNECTED:
                    callback.onEpgSyncMessageChanged("Connected", "");
                    break;
                case CONNECTING:
                    callback.onEpgSyncMessageChanged("Connecting...", "");
                    break;
                case FAILED:
                    callback.onEpgSyncMessageChanged("Connection failed", "");
                    callback.onEpgSyncStateChanged(State.FAILED);
                    break;
                case FAILED_UNRESOLVED_ADDRESS:
                    callback.onEpgSyncMessageChanged("Connection failed", "Failed to resolve server address");
                    callback.onEpgSyncStateChanged(State.FAILED);
                    break;
                case FAILED_EXCEPTION_OPENING_SOCKET:
                    callback.onEpgSyncMessageChanged("Connection failed", "Error while opening a connection to the server");
                    callback.onEpgSyncStateChanged(State.FAILED);
                    break;
                case FAILED_CONNECTING_TO_SERVER:
                    callback.onEpgSyncMessageChanged("Connection failed", "Failed to connect to server");
                    callback.onEpgSyncStateChanged(State.FAILED);
                    break;
            }
        } else if (intent.hasExtra(AUTH_STATE)) {
            Authenticator.State state = (Authenticator.State) intent.getSerializableExtra("authentication_state");
            switch (state) {
                case AUTHENTICATING:
                    callback.onEpgSyncMessageChanged("Authenticating...", "");
                    break;
                case AUTHENTICATED:
                    callback.onEpgSyncMessageChanged("Authenticated", "");
                    break;
                case FAILED_BAD_CREDENTIALS:
                    callback.onEpgSyncMessageChanged("Authentication failed", "Probably bad username or password");
                    callback.onEpgSyncStateChanged(State.FAILED);
                    break;
            }
        } else if (intent.hasExtra(SYNC_STATE)) {
            EpgSyncTask.State state = (EpgSyncTask.State) intent.getSerializableExtra("sync_state");
            switch (state) {
                case CONNECTED:
                    callback.onEpgSyncMessageChanged("Connected to server.", "");
                    break;
                case SYNCING:
                    callback.onEpgSyncMessageChanged("Loading data from server.", "");
                    break;
                case DONE:
                    callback.onEpgSyncMessageChanged("Loading data from server finished.", "");
                    callback.onEpgSyncStateChanged(State.DONE);
                    break;
                case RECONNECT:
                    callback.onEpgSyncMessageChanged("Reconnecting to server...", "");
                    context.stopService(new Intent(context, EpgSyncService.class));
                    context.startService(new Intent(context, EpgSyncService.class));
                    callback.onEpgSyncStateChanged(State.DONE);
                    break;
            }
        }
    }
}
