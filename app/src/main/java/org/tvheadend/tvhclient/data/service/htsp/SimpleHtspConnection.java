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

import androidx.annotation.NonNull;

import org.tvheadend.tvhclient.data.entity.Connection;
import org.tvheadend.tvhclient.data.service.htsp.tasks.Authenticator;

import timber.log.Timber;

public class SimpleHtspConnection implements HtspMessage.Dispatcher {

    private final HtspMessageDispatcher messageDispatcher;
    private final Authenticator authenticator;
    private final HtspConnection htspConnection;
    private final HtspDataHandler htspDataHandler;
    private Thread connectionThread;

    public SimpleHtspConnection(Connection connection) {
        Timber.d("Creating simple HTSP connection");

        messageDispatcher = new HtspMessageDispatcher();

        htspDataHandler = new HtspDataHandler(new HtspMessageSerializer(), messageDispatcher);
        authenticator = new Authenticator(messageDispatcher, connection);

        htspConnection = new HtspConnection(connection, htspDataHandler, htspDataHandler);
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

        Timber.d("Removing listeners from HTSP connection");
        htspConnection.removeConnectionListener(messageDispatcher);
        htspConnection.removeConnectionListener(htspDataHandler);
        htspConnection.removeConnectionListener(authenticator);

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
    }

    public boolean isIdle() {
        return htspConnection.isIdle();
    }

    public boolean isClosed() {
        return htspConnection.isClosed();
    }

    public boolean isConnected() {
        return htspConnection.isConnected();
    }

    public boolean isConnecting() {
        return htspConnection.isConnecting();
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
    public HtspMessage sendMessage(@NonNull HtspMessage message, int timeout) throws HtspNotConnectedException {
        return messageDispatcher.sendMessage(message, timeout);
    }
}
