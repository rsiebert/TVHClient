package org.tvheadend.tvhclient.data.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.tvheadend.tvhclient.data.service.htsp.HtspConnection;
import org.tvheadend.tvhclient.data.service.htsp.tasks.Authenticator;

// TODO use another object with msg, details and state and pass this in one method around

public class EpgSyncStatusReceiver extends BroadcastReceiver {

    public final static String ACTION = "service_status";
    public final static String CONNECTION_STATE = "connection_state";
    public final static String AUTH_STATE = "authentication_state";
    public final static String SYNC_STATE = "sync_state";

    private final EpgSyncStatusCallback callback;

    public enum State {
        IDLE,
        START,
        CONNECTED,
        LOADING,
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
            HtspConnection.State state = (HtspConnection.State) intent.getSerializableExtra(CONNECTION_STATE);
            switch (state) {
                case CLOSED:
                    callback.onEpgTaskStateChanged(
                            new EpgSyncTaskState.EpgSyncTaskStateBuilder()
                                    .message("Connection closed")
                                    .build());
                    break;
                case CONNECTED:
                    callback.onEpgTaskStateChanged(
                            new EpgSyncTaskState.EpgSyncTaskStateBuilder()
                                    .message("Connected")
                                    .build());
                    break;
                case CONNECTING:
                    callback.onEpgTaskStateChanged(
                            new EpgSyncTaskState.EpgSyncTaskStateBuilder()
                                    .state(State.START)
                                    .message("Connecting")
                                    .build());
                    break;
                case FAILED:
                    callback.onEpgTaskStateChanged(
                            new EpgSyncTaskState.EpgSyncTaskStateBuilder()
                                    .state(State.FAILED)
                                    .message("Connection failed")
                                    .details("Failed to connect to server")
                                    .build());
                    break;
                case FAILED_UNRESOLVED_ADDRESS:
                    callback.onEpgTaskStateChanged(
                            new EpgSyncTaskState.EpgSyncTaskStateBuilder()
                                    .state(State.FAILED)
                                    .message("Connection failed")
                                    .details("Failed to resolve server address")
                                    .build());
                    break;
                case FAILED_EXCEPTION_OPENING_SOCKET:
                    callback.onEpgTaskStateChanged(
                            new EpgSyncTaskState.EpgSyncTaskStateBuilder()
                                    .state(State.FAILED)
                                    .message("Connection failed")
                                    .details("Failed to open socket to the server")
                                    .build());
                    break;
                case FAILED_CONNECTING_TO_SERVER:
                    callback.onEpgTaskStateChanged(
                            new EpgSyncTaskState.EpgSyncTaskStateBuilder()
                                    .state(State.FAILED)
                                    .message("Connection failed")
                                    .details("Failed to connect to server")
                                    .build());
                    break;
            }
        } else if (intent.hasExtra(AUTH_STATE)) {
            Authenticator.State state = (Authenticator.State) intent.getSerializableExtra(AUTH_STATE);
            switch (state) {
                case AUTHENTICATING:
                    callback.onEpgTaskStateChanged(
                            new EpgSyncTaskState.EpgSyncTaskStateBuilder()
                                    .message("Authenticating")
                                    .build());
                    break;
                case AUTHENTICATED:
                    callback.onEpgTaskStateChanged(
                            new EpgSyncTaskState.EpgSyncTaskStateBuilder()
                                    .message("Authenticated")
                                    .build());
                    break;
                case FAILED_BAD_CREDENTIALS:
                    callback.onEpgTaskStateChanged(
                            new EpgSyncTaskState.EpgSyncTaskStateBuilder()
                                    .state(State.FAILED)
                                    .message("Authentication failed")
                                    .details("Probably bad username or password")
                                    .build());
                    break;
            }
        } else if (intent.hasExtra(SYNC_STATE)) {
            EpgSyncTask.State state = (EpgSyncTask.State) intent.getSerializableExtra(SYNC_STATE);
            switch (state) {
                case CONNECTED:
                    callback.onEpgTaskStateChanged(
                            new EpgSyncTaskState.EpgSyncTaskStateBuilder()
                                    .state(State.CONNECTED)
                                    .message("Connected to server")
                                    .build());
                    break;
                case SYNCING_STARTED:
                    callback.onEpgTaskStateChanged(
                            new EpgSyncTaskState.EpgSyncTaskStateBuilder()
                                    .state(State.LOADING)
                                    .message("Loading data from server")
                                    .build());
                    break;
                case SYNCING_DONE:
                    callback.onEpgTaskStateChanged(
                            new EpgSyncTaskState.EpgSyncTaskStateBuilder()
                                    .state(State.DONE)
                                    .message("Loading data from server finished")
                                    .build());
                    break;
                case NOT_CONNECTED:
                    callback.onEpgTaskStateChanged(
                            new EpgSyncTaskState.EpgSyncTaskStateBuilder()
                                    .state(State.FAILED)
                                    .message("No connection to server")
                                    .build());
                    break;
            }
        }
    }
}
