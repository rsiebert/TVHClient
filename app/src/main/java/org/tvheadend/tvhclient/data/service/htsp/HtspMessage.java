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

import android.os.Handler;
import android.support.annotation.NonNull;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import timber.log.Timber;

/*
 * See: https://tvheadend.org/projects/tvheadend/wiki/Htsp
 * See: https://tvheadend.org/projects/tvheadend/wiki/Htsmsgbinary
 *
 * HTSP Message Field Types:
 *
 * | Name | ID | Description
 * | Map  | 1  | Sub message of type map
 * | S64  | 2  | Signed 64bit integer
 * | Str  | 3  | UTF-8 encoded string
 * | Bin  | 4  | Binary blob
 * | List | 5  | Sub message of type list
 */

public class HtspMessage extends HashMap<String, Object> {

    // Message Handler, will receive incoming messages

    /**
     * A Message Listener will receive incoming messages
     */
    public interface Listener {
        /**
         * Returns the Handler on which to execute the callback.
         *
         * @return Handler, or null.
         */
        Handler getHandler();

        /**
         * Called once for each new message available
         *
         * @param message The message
         */
        void onMessage(@NonNull HtspMessage message);
    }

    // Message Dispatcher, can be used to register interest in incoming messages

    /**
     * A Dispatcher is used to orchestrate the movement of Messages, both messages needing to
     * be sent to the server, and messages received from the server.
     */
    public interface Dispatcher {

        /**
         * Register a new Message Listener
         *
         * @param listener The Listener to add
         */
        void addMessageListener(HtspMessage.Listener listener);

        /**
         * Removes an existing Message Listener
         *
         * @param listener The Listener to remove
         */
        void removeMessageListener(HtspMessage.Listener listener);

        /**
         * Queues a message for sending
         *
         * @param message The message to send
         * @return The messages sequence number
         */
        long sendMessage(@NonNull HtspMessage message) throws HtspNotConnectedException;

        /**
         * Queues a message for sending, blocks for the response
         *
         * @param message The message to send
         * @return The response message
         */
        HtspMessage sendMessage(@NonNull HtspMessage message, int timeout) throws HtspNotConnectedException;
    }

    public interface DispatcherInternal extends Dispatcher {
        /**
         * Called once for each new message available
         *
         * @param message The message
         */
        void onMessage(@NonNull HtspMessage message);

        /**
         * Called to check if there are any pending messages to send
         *
         * @return true if there are pending messages, false otherwise
         */
        boolean hasPendingMessages();

        /**
         * Called to fetch the next message to send. This will typically be FIFO, but higher
         * priority messages may be returned early (e.g. subscriptionStart/subscriptionStop
         * messages).
         *
         * @return The next message to send
         */
        @NonNull
        HtspMessage getMessage();
    }

    /**
     * A HTSP Serializer
     */
    public interface Serializer {
        /**
         * Deserializes data from a buffer into HTSPMessage instances
         *
         * @param buffer The buffer from which to read data
         * @return the first deserialized message in the buffer.
         */
        HtspMessage read(@NonNull ByteBuffer buffer);

        /**
         * Serializes a message onto a buffer
         *
         * @param buffer  The buffer to serialize to
         * @param message The message to serialize
         */
        void write(@NonNull ByteBuffer buffer, @NonNull HtspMessage message);
    }

    public HtspMessage(Map<? extends String, ?> m) {
        super(m);
    }

    public HtspMessage() {
    }

    @Override
    public Object put(@NonNull String key, Object value) {
        if (value == null) {
            // HTSP Messages can't have null values. Remove was probably more appropriate.
            Timber.e("HTSP Messages can't have a null value (field: " + key + ")");
        }

        return super.put(key, value);
    }

    public String getString(String key, String fallback) {
        if (!containsKey(key)) {
            return fallback;
        }

        return getString(key);
    }

    public String getString(String key) {
        Object obj = get(key);
        if (obj == null) {
            return null;
        }
        return obj.toString();
    }

    public int getInteger(String key, int fallback) {
        if (!containsKey(key)) {
            return fallback;
        }

        return getInteger(key);
    }

    public int getInteger(String key) {
        Object obj = get(key);
        if (obj == null) {
            throw new RuntimeException("Attempted to getInteger(" + key + ") on non-existent key");
        }
        if (obj instanceof BigInteger) {
            return ((BigInteger) obj).intValue();
        }

        return (int) obj;
    }

    public long getLong(String key, long fallback) {
        if (!containsKey(key)) {
            return fallback;
        }

        return getLong(key);
    }

    public long getLong(String key) {
        Object obj = get(key);
        if (obj == null) {
            throw new RuntimeException("Attempted to getLong(" + key + ") on non-existent key");
        }

        if (obj instanceof BigInteger) {
            return ((BigInteger) obj).longValue();
        }

        return (long) obj;
    }

    public boolean getBoolean(String key, boolean fallback) {
        if (!containsKey(key)) {
            return fallback;
        }

        return getBoolean(key);
    }

    private boolean getBoolean(String key) {
        return getInteger(key) == 1;
    }

    public ArrayList getArrayList(String key, ArrayList fallback) {
        if (!containsKey(key)) {
            return fallback;
        }

        return getArrayList(key);
    }

    public ArrayList getArrayList(String key) {
        Object obj = get(key);

        //noinspection unchecked
        return (ArrayList<String>) obj;
    }

    public HtspMessage[] getHtspMessageArray(String key, HtspMessage[] fallback) {
        if (!containsKey(key)) {
            return fallback;
        }

        return getHtspMessageArray(key);
    }

    public HtspMessage[] getHtspMessageArray(String key) {
        ArrayList value = getArrayList(key);

        return (HtspMessage[]) value.toArray(new HtspMessage[value.size()]);
    }

    public String[] getStringArray(String key, String[] fallback) {
        if (!containsKey(key)) {
            return fallback;
        }

        return getStringArray(key);
    }

    public String[] getStringArray(String key) {
        ArrayList value = getArrayList(key);

        return (String[]) value.toArray(new String[value.size()]);
    }

    public byte[] getByteArray(String key, byte[] fallback) {
        if (!containsKey(key)) {
            return fallback;
        }

        return getByteArray(key);
    }

    public byte[] getByteArray(String key) {
        Object value = get(key);

        return (byte[]) value;
    }

    public List<Integer> getIntegerList(String key) {
        ArrayList<Integer> list = new ArrayList<>();
        if (!containsKey(key)) {
            return list;
        }
        for (Object obj : (List<?>) get(key)) {
            if (obj instanceof BigInteger) {
                list.add(((BigInteger) obj).intValue());
            }
        }
        return list;
    }
}
