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
package org.tvheadend.tvhclient.htsp.tasks;

import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;

import org.tvheadend.tvhclient.BuildConfig;
import org.tvheadend.tvhclient.data.entity.Connection;
import org.tvheadend.tvhclient.htsp.HtspConnection;
import org.tvheadend.tvhclient.htsp.HtspMessage;
import org.tvheadend.tvhclient.htsp.HtspNotConnectedException;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Handles Authentication on a HTSP Connection
 *
 * * Waits for State==CONNECTED, adds itself as a Message listener
 * * Sends a Hello request
 * * Receives a Hello response with challenge etc
 * * Sends an Authenticator request
 * * Receives a Authenticator response
 * * Removes itself as a Message listener
 */
public class Authenticator implements HtspMessage.Listener, HtspConnection.Listener {
    private static final String TAG = Authenticator.class.getSimpleName();

    private static final Set<String> HANDLED_METHODS = new HashSet<>(Arrays.asList("hello", "authenticate"));

    /**
     * A listener for Authentication state events
     */
    public interface Listener {
        /**
         * Returns the Handler on which to execute the callback.
         *
         * @return Handler, or null.
         */
        Handler getHandler();

        /**
         * Called whenever the Authentication state changes
         *
         * @param state The new authentication state
         */
        void onAuthenticationStateChange(@NonNull State state);
    }

    public enum State {
        IDLE,
        AUTHENTICATING,
        AUTHENTICATED,
        FAILED
    }

    private final HtspMessage.Dispatcher dispatcher;
    private State mState = State.IDLE;

    private Connection connection;

    public Authenticator(@NonNull HtspMessage.Dispatcher dispatcher, @NonNull Connection connectionInfo) {
        this.dispatcher = dispatcher;
        connection = connectionInfo;
    }

    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();

    public void addAuthenticationListener(Listener listener) {
        if (listeners.contains(listener)) {
            Log.w(TAG, "Attempted to add duplicate authentication listener");
            return;
        }
        listeners.add(listener);
    }

    public void removeAuthenticationListener(Listener listener) {
        if (!listeners.contains(listener)) {
            Log.w(TAG, "Attempted to remove non existing authentication listener");
            return;
        }
        listeners.remove(listener);
    }

    public State getState() {
        return mState;
    }

    private void setState(final State state) {
        for (final Listener listener : listeners) {
            Handler handler = listener.getHandler();
            if (handler == null) {
                listener.onAuthenticationStateChange(state);
            } else {
                handler.post(() -> listener.onAuthenticationStateChange(state));
            }
        }
    }

    // HtspConnection.Listener and HtspMessage.Listener Methods
    @Override
    public Handler getHandler() {
        return null;
    }

    // HtspConnection.Listener Methods
    @Override
    public void setConnection(@NonNull HtspConnection connection) {

    }

    @Override
    public void onConnectionStateChange(@NonNull HtspConnection.State state) {
        // Start Authentication flow once we see CONNECTED
        if (state == HtspConnection.State.CONNECTED) {
            startAuthentication();
        } else if (state == HtspConnection.State.CLOSING) {
            setState(State.IDLE);
        }
    }

    // HtspMessage.Listener Methods
    @Override
    public void onMessage(@NonNull HtspMessage message) {
        final String method = message.getString("method");

        if (HANDLED_METHODS.contains(method)) {
            Log.d(TAG, "Authenticator received message with method: " + method);

            if (method.equals("hello")) {
                handleHelloResponse(message);
            } else if (method.equals("authenticate")) {
                handleAuthenticateResponse(message);
            }
        }
    }

    // Internal Methods
    private void startAuthentication() {
        Log.i(TAG, "Starting Authentication");
        setState(State.AUTHENTICATING);

        dispatcher.addMessageListener(this);

        sendHelloRequest();
    }

    private void sendHelloRequest() {
        Log.i(TAG, "Sending hello request");
        HtspMessage message = new HtspMessage();

        message.put("method", "hello");
        message.put("htspversion", 26);
        message.put("clientname", "TVHClient");
        message.put("clientversion", BuildConfig.VERSION_NAME);

        try {
            dispatcher.sendMessage(message);
        } catch (HtspNotConnectedException e) {
            Log.w(TAG, "Authenticator failed, not connected", e);
            setState(State.FAILED);
        }
    }

    private void handleHelloResponse(HtspMessage responseMessage) {
        Log.i(TAG, "Got hello response");

        if (responseMessage.containsKey("error")) {
            Log.e(TAG, "Received error response to hello request: " + responseMessage.getString("error"));
            setState(State.FAILED);

            // Remove myself as a message listener, I'm all done for now.
            dispatcher.removeMessageListener(this);

            return;
        }

        Log.i(TAG, "Sending authenticate request");
        HtspMessage message = new HtspMessage();

        message.put("method", "authenticate");
        message.put("username", connection.getUsername());
        message.put("digest", calculateDigest(responseMessage.getByteArray("challenge")));

        try {
            dispatcher.sendMessage(message);
        } catch (HtspNotConnectedException e) {
            Log.w(TAG, "Authenticator failed, not connected", e);
            setState(State.FAILED);

            // Remove myself as a message listener, I'm all done for now.
            dispatcher.removeMessageListener(this);
        }
    }

    private void handleAuthenticateResponse(HtspMessage responseMessage) {
        Log.i(TAG, "Got authenticate response");

        // Remove myself as a message listener, I'm all done for now.
        dispatcher.removeMessageListener(this);

        if (responseMessage.containsKey("error")) {
            Log.e(TAG, "Received error response to authenticate request: " + responseMessage.getString("error"));
            setState(State.FAILED);
        } else if (responseMessage.getBoolean("noaccess", false)) {
            Log.w(TAG, "Authenticator failed, likely bad username/password");
            setState(State.FAILED);
        } else {
            Log.i(TAG, "Authenticator successful");
            setState(State.AUTHENTICATED);
        }
    }

    private byte[] calculateDigest(byte[] challenge) {
        MessageDigest md;

        try {
            md = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Your platform doesn't support SHA-1");
        }

        try {
            md.update(connection.getPassword().getBytes("utf8"));
            md.update(challenge);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Your platform doesn't support UTF-8");
        }

        return md.digest();
    }
}
