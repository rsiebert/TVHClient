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
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import org.tvheadend.tvhclient.data.entity.Connection;
import org.tvheadend.tvhclient.data.service.htsp.tasks.Authenticator;

import timber.log.Timber;

public class SimpleHtspConnection implements HtspMessage.Dispatcher, HtspConnection.Listener {
    private static final String TAG = SimpleHtspConnection.class.getSimpleName();

    private final HtspMessageDispatcher messageDispatcher;
    private final Authenticator authenticator;
    private final HtspConnection htspConnection;
    private final Context context;
    private Thread connectionThread;

    private boolean enableReconnect = false;
    private int retryCount = 0;
    private int retryDelay = 3;

    public SimpleHtspConnection(Context context, Connection connectionInfo) {
        this.context = context;

        messageDispatcher = new HtspMessageDispatcher();

        HtspDataHandler htspDataHandler = new HtspDataHandler(new HtspMessageSerializer(), messageDispatcher);
        authenticator = new Authenticator(messageDispatcher, connectionInfo);

        htspConnection = new HtspConnection(connectionInfo, htspDataHandler, htspDataHandler);
        htspConnection.addConnectionListener(this);
        htspConnection.addConnectionListener(messageDispatcher);
        htspConnection.addConnectionListener(htspDataHandler);
        htspConnection.addConnectionListener(authenticator);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        retryDelay = Integer.valueOf(sharedPreferences.getString("connection_timeout", "5")) * 1000;
    }

    public void start() {
        start(true);
    }

    private void start(boolean allowRestart) {
        if (connectionThread != null) {
            Timber.w("SimpleHtspConnection already started");
            return;
        }

        if (allowRestart) {
            enableReconnect = true;
        }

        connectionThread = new Thread(htspConnection);
        connectionThread.start();
    }

    private void restart() {
        if (connectionThread != null) {
            stop(false);
        }

        start(false);
    }

    public void stop() {
        stop(true);
    }

    private void stop(boolean preventRestart) {
        if (connectionThread == null) {
            Timber.w("SimpleHtspConnection not started");
            return;
        }

        if (preventRestart) {
            enableReconnect = false;
        }

        htspConnection.closeConnection();
        connectionThread.interrupt();
        try {
            connectionThread.join();
        } catch (InterruptedException e) {
            // Ignore.
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
        Timber.d("isConnected " + htspConnection.isConnected());
        return htspConnection.isConnected();
    }

    public boolean isAuthenticated() {
        Timber.d("isAuthenticated is " + authenticator.isAuthenticated());
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
    public HtspMessage sendMessage(@NonNull HtspMessage message, int timeout) throws HtspNotConnectedException {
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

        // Simple HTSP Connections will take care of reconnecting upon failure for you..
        if (enableReconnect
                && (state == HtspConnection.State.FAILED
                || state == HtspConnection.State.FAILED_CONNECTING_TO_SERVER
                || state == HtspConnection.State.FAILED_EXCEPTION_OPENING_SOCKET
                || state == HtspConnection.State.FAILED_INTERRUPTED
                || state == HtspConnection.State.FAILED_UNRESOLVED_ADDRESS)) {

            // Wait half of the retry delay time so that the user
            // can read the message about the connection status.
            try {
                Thread.sleep(retryDelay);
            } catch (InterruptedException e) {
                // NOP
            }

            if (retryCount < 3) {
                restart();
            } else {
                stop();
            }
            retryCount++;

        } else if (state == HtspConnection.State.CONNECTED) {
            // Reset our retry counter and delay back to zero
            retryCount = 0;
        }
    }
}
