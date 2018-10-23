/*
 * Copyright (c) 2017 Kiall Mac Innes <kiall@macinnes.ie>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.tvheadend.tvhclient.data.service.htsp;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.entity.Connection;
import org.tvheadend.tvhclient.data.service.htsp.tasks.Authenticator;
import org.tvheadend.tvhclient.features.shared.receivers.ServiceStatusReceiver;

import timber.log.Timber;

public class SimpleHtspConnection implements HtspMessage.Dispatcher, HtspConnection.Listener {

    private final HtspMessageDispatcher messageDispatcher;
    private final Authenticator authenticator;
    private final HtspConnection htspConnection;
    private final Context context;
    private Thread connectionThread;

    public SimpleHtspConnection(Context context, Connection connection) {
        Timber.d("Creating simple HTSP connection");

        this.context = context;
        messageDispatcher = new HtspMessageDispatcher();

        HtspDataHandler htspDataHandler = new HtspDataHandler(new HtspMessageSerializer(), messageDispatcher);
        authenticator = new Authenticator(messageDispatcher, connection);

        htspConnection = new HtspConnection(connection, htspDataHandler, htspDataHandler);
        htspConnection.addConnectionListener(this);
        htspConnection.addConnectionListener(messageDispatcher);
        htspConnection.addConnectionListener(htspDataHandler);
        htspConnection.addConnectionListener(authenticator);
    }

    public void start() {
        Timber.d("Starting simple HTSP connection");

        if (connectionThread != null) {
            Timber.w("Simple HTSP connection already started");
            return;
        }

        connectionThread = new Thread(htspConnection);
        connectionThread.start();
    }

    public void stop() {
        Timber.d("Stopping simple HTSP connection");

        if (connectionThread == null) {
            Timber.w("Simple HTSP connection not started");
            return;
        }

        Timber.d("Closing HTSP connection thread");
        htspConnection.closeConnection();
        if (connectionThread != null) {
            Timber.d("Interrupting thread");
            connectionThread.interrupt();
        } else {
            Timber.e("Could not call interrupt, HTSP connection thread is null");
        }
        try {
            Timber.d("Waiting for the thread to die");
            if (connectionThread != null) {
                connectionThread.join();
            } else {
                Timber.e("Could not call join, HTSP connection thread is null");
            }
        } catch (InterruptedException e) {
            Timber.e("Thread got interrupted while waiting to die");
        }
        connectionThread = null;
    }

    public HtspMessageDispatcher getMessageDispatcher() {
        return messageDispatcher;
    }

    public boolean isClosed() {
        return htspConnection.isClosed();
    }

    public boolean isClosedOrClosing() {
        return htspConnection.isClosedOrClosing();
    }

    public boolean isConnected() {
        return htspConnection.isConnected();
    }

    public boolean isAuthenticated() {
        return authenticator.isAuthenticated();
    }

    public void addConnectionListener(HtspConnection.Listener listener) {
        htspConnection.addConnectionListener(listener);
    }

    public void removeConnectionListener(HtspConnection.Listener listener) {
        htspConnection.removeConnectionListener(listener);
    }

    public void addAuthenticationListener(Authenticator.Listener listener) {
        authenticator.addAuthenticationListener(listener);
    }

    public void removeAuthenticationListener(Authenticator.Listener listener) {
        authenticator.removeAuthenticationListener(listener);
    }

    @Override
    public void addMessageListener(HtspMessage.Listener listener) {
        messageDispatcher.addMessageListener(listener);
    }

    @Override
    public void removeMessageListener(HtspMessage.Listener listener) {
        messageDispatcher.removeMessageListener(listener);
    }

    @Override
    public long sendMessage(@NonNull HtspMessage message) throws HtspNotConnectedException {
        return messageDispatcher.sendMessage(message);
    }

    @Override
    public HtspMessage sendMessage(@NonNull HtspMessage message, int timeout) throws
            HtspNotConnectedException {
        return messageDispatcher.sendMessage(message, timeout);
    }

    @Override
    public Handler getHandler() {
        return null;
    }

    @Override
    public void setConnection(@NonNull HtspConnection connection) {
    }

    @Override
    public void onConnectionStateChange(@NonNull HtspConnection.State state) {
        Timber.d("Simple HTSP connection state changed, state is " + state);

        switch (state) {
            case FAILED:
                sendEpgSyncStatusMessage(ServiceStatusReceiver.State.FAILED,
                        context.getString(R.string.connection_failed),
                        null);
                break;
            case FAILED_CONNECTING_TO_SERVER:
                sendEpgSyncStatusMessage(ServiceStatusReceiver.State.FAILED,
                        context.getString(R.string.connection_failed),
                        context.getString(R.string.failed_connecting_to_server));
                break;
            case FAILED_EXCEPTION_OPENING_SOCKET:
                sendEpgSyncStatusMessage(ServiceStatusReceiver.State.FAILED,
                        context.getString(R.string.connection_failed),
                        context.getString(R.string.failed_opening_socket));
                break;
            case FAILED_INTERRUPTED:
                sendEpgSyncStatusMessage(ServiceStatusReceiver.State.FAILED,
                        context.getString(R.string.connection_failed),
                        context.getString(R.string.failed_during_connection_attempt));
                break;
            case FAILED_UNRESOLVED_ADDRESS:
                sendEpgSyncStatusMessage(ServiceStatusReceiver.State.FAILED,
                        context.getString(R.string.connection_failed),
                        context.getString(R.string.failed_to_resolve_address));
                break;
            case CONNECTING:
                sendEpgSyncStatusMessage(ServiceStatusReceiver.State.CONNECTING,
                        context.getString(R.string.connecting_to_server), "");
                break;
            case CLOSED:
                sendEpgSyncStatusMessage(ServiceStatusReceiver.State.CLOSED,
                        context.getString(R.string.connection_closed), "");
                break;
        }

        // Stop the connection if a failure occurred
/*
        if (state == HtspConnection.State.FAILED
                || state == HtspConnection.State.FAILED_CONNECTING_TO_SERVER
                || state == HtspConnection.State.FAILED_EXCEPTION_OPENING_SOCKET
                || state == HtspConnection.State.FAILED_INTERRUPTED
                || state == HtspConnection.State.FAILED_UNRESOLVED_ADDRESS) {
            stop();
        }
*/
    }

    private void sendEpgSyncStatusMessage(ServiceStatusReceiver.State state, String msg, String details) {
        Intent intent = new Intent(ServiceStatusReceiver.ACTION);
        intent.putExtra(ServiceStatusReceiver.STATE, state);
        if (!TextUtils.isEmpty(msg)) {
            intent.putExtra(ServiceStatusReceiver.MESSAGE, msg);
        }
        if (!TextUtils.isEmpty(details)) {
            intent.putExtra(ServiceStatusReceiver.DETAILS, details);
        }
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }
}
