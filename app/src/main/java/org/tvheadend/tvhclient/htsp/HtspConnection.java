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
package org.tvheadend.tvhclient.htsp;

import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;

import org.tvheadend.tvhclient.data.entity.Connection;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.UnresolvedAddressException;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Main HTSP Connection class
 */
public class HtspConnection implements Runnable {
    private static final String TAG = HtspConnection.class.getSimpleName();

    /**
     * A listener for Connection state events
     */
    public interface Listener {
        /**
         * Returns the Handler on which to execute the callback.
         *
         * @return Handler, or null.
         */
        Handler getHandler();

        /**
         * Called when this Listener is registered on a HtspConnection, allowing the listener
         * to have a connection rederence.
         *
         * @param connection The HtspConnection this Listener has been added to.
         */
        void setConnection(@NonNull HtspConnection connection);

        /**
         * Called whenever the HtspConnection state changes
         *
         * @param state The new connection state
         */
        void onConnectionStateChange(@NonNull State state);
    }

    /**
     * A Connection Reader, unsurprisingly, reads data off the HtspConnection.
     */
    public interface Reader {
        /**
         * Is called as data becomes available available to read from the SocketChannel
         *
         * @param socketChannel The SocketChannel from which to read
         * @return true on a successful read, false otherwise
         */
        boolean read(@NonNull SocketChannel socketChannel);
    }

    /**
     * A Connection Writer, unsurprisingly, writes data to the HtspConnection.
     */
    public interface Writer {

        /**
         * Called by the Connection to determine if we have data awaiting writing.
         *
         * @return true if there is data to write, false otherwise.
         */
        boolean hasPendingData();

        /**
         * Is called when we have A) indicated we have data to write (via hasPendingData), and
         * the connection is in a state suitable for writing to.
         *
         * @param socketChannel The SocketChannel to write to
         * @return true if the data was written successfully, false otherwise.
         */
        boolean write(@NonNull SocketChannel socketChannel);
    }

    public enum State {
        CLOSED,
        CONNECTING,
        CONNECTED,
        CLOSING,
        FAILED,
        FAILED_INTERRUPTED,
        FAILED_UNRESOLVED_ADDRESS,
        FAILED_CONNECTING_TO_SERVER,
        FAILED_EXCEPTION_OPENING_SOCKET
    }

    private Connection connection;
    private final Reader reader;
    private final Writer writer;

    private boolean running = false;
    private final Lock lock = new ReentrantLock();
    private State state = State.CLOSED;

    private final Set<Listener> mListeners = new CopyOnWriteArraySet<>();
    private SocketChannel mSocketChannel;
    private Selector mSelector;

    public HtspConnection(Connection connection, Reader reader, Writer writer) {
        this.connection = connection;
        this.reader = reader;
        this.writer = writer;
    }

    // Runnable Methods
    @Override
    public void run() {
        // Do the initial connection
        try {
            running = openConnection();
        } catch (Exception e) {
            Log.e(TAG, "Unhandled exception while opening HTSP connection, shutting down", e);
            if (!isClosedOrClosingOrFailed()) {
                closeConnection(State.FAILED);
            }
            return;
        }

        // Main Loop
        while (running) {
            if (mSelector == null) {
                break;
            }

            try {
                mSelector.select();
            } catch (IOException e) {
                Log.e(TAG, "Failed to select from socket channel", e);
                closeConnection(State.FAILED);
                break;
            }

            if (mSelector == null || !mSelector.isOpen()) {
                break;
            }

            Set<SelectionKey> keys = mSelector.selectedKeys();
            Iterator<SelectionKey> i = keys.iterator();

            try {
                while (i.hasNext()) {
                    SelectionKey selectionKey = i.next();
                    i.remove();

                    if (!selectionKey.isValid()) {
                        break;
                    }

                    if (selectionKey.isValid() && selectionKey.isConnectable()) {
                        processConnectableSelectionKey(selectionKey);
                    }

                    if (selectionKey.isValid() && selectionKey.isReadable()) {
                        processReadableSelectionKey(selectionKey);
                    }

                    if (selectionKey.isValid() && selectionKey.isWritable()) {
                        processWritableSelectionKey(selectionKey);
                    }

                    if (isClosedOrClosingOrFailed()) {
                        break;
                    }
                }

                if (isClosedOrClosingOrFailed()) {
                    break;
                }

                if (mSocketChannel != null && mSocketChannel.isConnected() && writer.hasPendingData()) {
                    mSocketChannel.register(mSelector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                } else if (mSocketChannel != null && mSocketChannel.isConnected()) {
                    mSocketChannel.register(mSelector, SelectionKey.OP_READ);
                }
            } catch (Exception e) {
                Log.e(TAG, "Something failed - shutting down", e);
                closeConnection(State.FAILED);
                break;
            }
        }

        lock.lock();
        try {
            if (!isClosedOrClosingOrFailed()) {
                Log.e(TAG, "HTSP Connection thread wrapping up without already being closed");
                closeConnection(State.FAILED);
                return;
            }

            if (getState() == State.CLOSED) {
                Log.i(TAG, "HTSP Connection thread wrapped up cleanly");
            } else if (getState() == State.FAILED) {
                Log.e(TAG, "HTSP Connection thread wrapped up upon failure");
            } else {
                Log.e(TAG, "HTSP Connection thread wrapped up in an unexpected state: " + getState());
            }
        } finally {
            lock.unlock();
        }
    }

    // Internal Methods
    private void processConnectableSelectionKey(SelectionKey selectionKey) throws IOException {
        if (HtspConstants.DEBUG)
            Log.v(TAG, "processConnectableSelectionKey()");

        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();

        if (HtspConstants.DEBUG)
            Log.v(TAG, "Finishing SocketChannel Connection");

        try {
            socketChannel.finishConnect();
        } catch (ConnectException e) {
            Log.e(TAG, "Failed to connect to HTSP server address:", e);
            closeConnection(State.FAILED_CONNECTING_TO_SERVER);
            return;
        }

        Log.i(TAG, "HTSP Connected");
        setState(State.CONNECTED);
    }

    private void processReadableSelectionKey(SelectionKey selectionKey) throws IOException {
        if (HtspConstants.DEBUG)
            Log.v(TAG, "processReadableSelectionKey()");

        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();

        if (!isClosedOrClosing()) {
            if (!reader.read(socketChannel)) {
                Log.e(TAG, "Failed to process readable selection key");
                closeConnection(State.FAILED);
            }
        }
    }

    private void processWritableSelectionKey(SelectionKey selectionKey) throws IOException {
        if (HtspConstants.DEBUG)
            Log.v(TAG, "processWritableSelectionKey()");

        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();

        if (!isClosedOrClosing()) {
            if (!writer.write(socketChannel)) {
                Log.e(TAG, "Failed to process writable selection key");
                closeConnection(State.FAILED);
            }
        }
    }

    public void addConnectionListener(Listener listener) {
        if (mListeners.contains(listener)) {
            Log.w(TAG, "Attempted to add duplicate connection listener");
            return;
        }
        listener.setConnection(this);
        mListeners.add(listener);
    }

    public void removeConnectionListener(Listener listener) {
        if (!mListeners.contains(listener)) {
            Log.w(TAG, "Attempted to remove non existing connection listener");
            return;
        }
        mListeners.remove(listener);
    }

    public void setWritePending() {
        if (HtspConstants.DEBUG)
            Log.d(TAG, "Notified of available data to write");

        lock.lock();
        try {
            if (isClosedOrClosingOrFailed()) {
                Log.w(TAG, "Attempting to write while closed, closing or failed - discarding");
                return;
            }

            if (mSocketChannel != null && mSocketChannel.isConnected() && !mSocketChannel.isConnectionPending()) {
                try {
                    if (HtspConstants.DEBUG)
                        Log.d(TAG, "Registering OP_READ | OP_WRITE on SocketChannel");
                    mSocketChannel.register(mSelector, SelectionKey.OP_WRITE);
                    mSelector.wakeup();
                } catch (ClosedChannelException e) {
                    Log.e(TAG, "Failed to register selector, channel closed", e);
                    closeConnection(State.FAILED);
                    return;
                }
            }
        } finally {
            lock.unlock();
        }
    }

    public boolean isConnected() {
        return getState() == State.CONNECTED;
    }

    public boolean isClosed() {
        return getState() == State.CLOSED;
    }

    public boolean isClosing() {
        return getState() == State.CLOSING;
    }

    public boolean isFailed() {
        return getState() == State.FAILED;
    }

    public boolean isClosedOrFailed() {
        return isClosed() || isFailed();
    }

    public boolean isClosedOrClosing() {
        return isClosed() || isClosing();
    }

    public boolean isClosedOrClosingOrFailed() {
        return isClosed() || isClosing() || isFailed();
    }

    public State getState() {
        return state;
    }

    private void setState(final State state) {
        lock.lock();
        try {
            this.state = state;
        } finally {
            lock.unlock();
        }

        for (final Listener listener : mListeners) {
            Handler handler = listener.getHandler();
            if (handler == null) {
                listener.onConnectionStateChange(state);
            } else {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        listener.onConnectionStateChange(state);
                    }
                });
            }
        }
    }

    private boolean openConnection() throws HtspException {
        Log.i(TAG, "Opening HTSP Connection");

        lock.lock();
        try {
            if (!isClosedOrFailed()) {
                throw new HtspException("Attempting to connect while already connected");
            }

            setState(State.CONNECTING);

            try {
                mSocketChannel = SocketChannel.open();
                mSocketChannel.configureBlocking(false);
                mSocketChannel.connect(new InetSocketAddress(connection.getHostname(), connection.getPort()));
                mSelector = Selector.open();
            } catch (ClosedByInterruptException e) {
                Log.e(TAG, "Failed to open HTSP connection, interrupted");
                closeConnection(State.FAILED_INTERRUPTED);
                return false;
            } catch (UnresolvedAddressException e) {
                Log.e(TAG, "Failed to resolve HTSP server address:", e);
                closeConnection(State.FAILED_UNRESOLVED_ADDRESS);
                return false;
            } catch (IOException e) {
                Log.e(TAG, "Caught IOException while opening SocketChannel:", e);
                closeConnection(State.FAILED_EXCEPTION_OPENING_SOCKET);
                return false;
            }
        } finally {
            lock.unlock();
        }

        try {
            if (HtspConstants.DEBUG)
                Log.d(TAG, "Registering OP_CONNECT | OP_READ on SocketChannel");
            int operations = SelectionKey.OP_CONNECT | SelectionKey.OP_READ;
            mSocketChannel.register(mSelector, operations);
        } catch (ClosedChannelException e) {
            Log.e(TAG, "Failed to register selector, channel closed:", e);
            closeConnection(State.FAILED);
            return false;
        }

        return true;
    }

    public void closeConnection() {
        closeConnection(State.CLOSED);
    }

    private void closeConnection(State finalState) {
        if (isClosedOrClosingOrFailed()) {
            Log.w(TAG, "Attempting to close while already closed, closing or failed");
            return;
        }

        Log.i(TAG, "Closing HTSP Connection");

        running = false;

        lock.lock();
        try {
            setState(State.CLOSING);

            if (mSocketChannel != null) {
                try {
                    if (HtspConstants.DEBUG)
                        Log.d(TAG, "Calling SocketChannel close");
                    mSocketChannel.socket().close();
                    mSocketChannel.close();
                } catch (IOException e) {
                    Log.w(TAG, "Failed to close socket channel:", e);
                } finally {
                    mSocketChannel = null;
                }
            }

            if (mSelector != null) {
                try {
                    if (HtspConstants.DEBUG)
                        Log.d(TAG, "Calling Selector close");
                    mSelector.close();
                } catch (IOException e) {
                    Log.w(TAG, "Failed to close socket channel:", e);
                } finally {
                    mSelector = null;
                }
            }

            setState(finalState);
        } finally {
            lock.unlock();
        }
    }
}
