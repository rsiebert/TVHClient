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
package org.tvheadend.tvhclient.data.remote.htsp;

import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.LongSparseArray;

import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;

import timber.log.Timber;

/**
 * HtspMessageDispatchers handle taking a message in, and figuring out who needs a copy of it. It's
 * here we'll implement filtering of messages (e.g. Muxpkt's should only go to 1 place, the
 * subscriber of that particular subscription ID etc).
 * <p>
 * Subinterfaces of HtspMessage.Listener will be used to provide for some of this more advanced
 * dispatching functionality later on, as it becomes required.
 */
public class HtspMessageDispatcher implements HtspMessage.DispatcherInternal, HtspMessage.Listener, HtspConnection.Listener {
    private static final String TAG = HtspMessageDispatcher.class.getSimpleName();
    private static final AtomicInteger sSequence = new AtomicInteger();

    private final Set<HtspMessage.Listener> listeners = new CopyOnWriteArraySet<>();
    private final Queue<HtspMessage> queue = new ConcurrentLinkedQueue<>();

    private static final LongSparseArray<String> messageResponseMethodsBySequence = new LongSparseArray<>();

    private HtspConnection htspConnection;

    private final LongSparseArray<Object> sequenceLocks = new LongSparseArray<>();
    private final LongSparseArray<HtspMessage> sequenceResponses = new LongSparseArray<>();

    HtspMessageDispatcher() {
    }

    // HtspMessage.DispatcherInternal Methods
    @Override
    public void addMessageListener(HtspMessage.Listener listener) {
        if (listeners.contains(listener)) {
            Timber.w("Attempted to add duplicate message listener");
            return;
        }
        listeners.add(listener);
    }

    @Override
    public void removeMessageListener(HtspMessage.Listener listener) {
        if (!listeners.contains(listener)) {
            Timber.w("Attempted to remove non existing message listener");
            return;
        }
        listeners.remove(listener);
    }

    @Override
    public long sendMessage(@NonNull HtspMessage message) throws HtspNotConnectedException {
        if (!htspConnection.isConnected()) {
            throw new HtspNotConnectedException("Failed to send message, HTSP Connection not connected");
        }

        // If necessary, inject a sequence number
        if (!message.containsKey("seq")) {
            message.put("seq", (long) sSequence.getAndIncrement());
        }

        // Record the Sequence Number and Method
        if (message.containsKey("method")) {
            messageResponseMethodsBySequence.append(message.getLong("seq"), message.getString("method"));
        }

        queue.add(message);

        if (htspConnection != null) {
            htspConnection.setWritePending();
        }

        return message.getLong("seq");
    }

    @Override
    public HtspMessage sendMessage(@NonNull HtspMessage message, int timeout) throws HtspNotConnectedException {
        if (!htspConnection.isConnected()) {
            throw new HtspNotConnectedException("Failed to send message, HTSP Connection not connected");
        }

        long seq;

        // If necessary, inject a sequence number
        if (!message.containsKey("seq")) {
            seq = (long) sSequence.getAndIncrement();
            message.put("seq", seq);
        } else {
            seq = message.getLong("seq");
        }

        Object lock = new Object();
        try {
            sequenceLocks.put(seq, lock);
            sendMessage(message);

            synchronized (lock) {
                try {
                    lock.wait(timeout);
                } catch (InterruptedException e) {
                    Timber.d("Exception during waiting", e);
                    return null;
                }
            }
            return sequenceResponses.get(seq);

        } finally {
            sequenceLocks.remove(seq);
            sequenceResponses.remove(seq);
        }
    }

    @Override
    public void onMessage(@NonNull final HtspMessage message) {
        if (message.containsKey("seq")) {
            long seq = message.getLong("seq");

            // Reply messages don't include a method, only the sequence supplied in the request, so
            // if we have this sequence in our lookup table, go ahead and add the method into the
            // message.
            if (messageResponseMethodsBySequence.indexOfKey(seq) >= 0) {
                if (!message.containsKey("method")) {
                    String m = messageResponseMethodsBySequence.get(seq);
                    message.put("method", m);
                }

                // Clear the sequence from our lookup table, it's no longer needed.
                messageResponseMethodsBySequence.remove(seq);
            }

            // If we have a SequenceLock for this seq, the message is part of a blocking request/
            // reply, so stash it in place of lock, notify the lock and don't pass the message onto
            // the other listeners.
            if (sequenceLocks.indexOfKey(seq) >= 0) {
                Object lock = sequenceLocks.get(seq);
                sequenceResponses.put(seq, message);
                synchronized (lock) {
                    lock.notify();
                }
                sequenceLocks.remove(seq);
                return;
            }
        }

        for (final HtspMessage.Listener listener : listeners) {
            Handler handler = listener.getHandler();

            if (handler == null) {
                listener.onMessage(message);
            } else {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        listener.onMessage(message);
                    }
                });
            }
        }
    }

    @Override
    public boolean hasPendingMessages() {
        return queue.size() > 0;
    }

    @NonNull
    @Override
    public HtspMessage getMessage() {
        return queue.remove();
    }

    // HtspConnection.Listener Methods
    @Override
    public Handler getHandler() {
        return null;
    }

    @Override
    public void setConnection(@NonNull HtspConnection connection) {
        htspConnection = connection;
    }

    @Override
    public void onConnectionStateChange(@NonNull HtspConnection.State state) {
        // Clear queued messages etc out as we close the connection
        if (state == HtspConnection.State.CLOSED) {
            Timber.d("Clearing out message queue as HTSP connection is closing");
            queue.clear();
        }
    }
}
