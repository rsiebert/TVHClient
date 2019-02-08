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

import org.tvheadend.tvhclient.data.services.BaseHtspMessage;

import java.nio.ByteBuffer;

import androidx.annotation.NonNull;

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

public class HtspMessage extends BaseHtspMessage {

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
}
