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

import android.support.annotation.NonNull;
import android.util.Log;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import timber.log.Timber;

public class HtspMessageSerializer implements HtspMessage.Serializer {
    private static final String TAG = HtspMessageSerializer.class.getSimpleName();
    private static final boolean DEBUG = false;

    private static final byte FIELD_MAP = 1;
    private static final byte FIELD_S64 = 2;
    private static final byte FIELD_STR = 3;
    private static final byte FIELD_BIN = 4;
    private static final byte FIELD_LIST = 5;

    public HtspMessageSerializer() {
    }

    @Override
    public HtspMessage read(@NonNull ByteBuffer buffer) {
        if (buffer.limit() < 4) {
            Timber.w("Buffer does not have enough data to read a message length");
            return null;
        }

        byte[] lenBytes = new byte[4];

        lenBytes[0] = buffer.get(0);
        lenBytes[1] = buffer.get(1);
        lenBytes[2] = buffer.get(2);
        lenBytes[3] = buffer.get(3);

        int length = (int) bin2long(lenBytes);
        int fullLength = length + 4;

        if (DEBUG) {
            Log.v(TAG, "Reading message of length " + fullLength + " from buffer");
        }

        if (buffer.capacity() < fullLength) {
            throw new RuntimeException("Message exceeds buffer capacity: " + fullLength);
        }

        // Keep reading until we have the entire message
        if (buffer.limit() < fullLength) {
            if (DEBUG) {
                Log.v(TAG, "Waiting for more data, don't have enough yet. Need: " + fullLength + " bytes / Have: " + buffer.limit() + " bytes");
            }
            return null;
        }

        // Set the buffers limit to ensure we don't read data belonging to the next message...
        buffer.limit(fullLength);

        buffer.position(4);

        HtspMessage message = deserialize(buffer);

        return message;
    }

    @Override
    public void write(@NonNull ByteBuffer buffer, @NonNull HtspMessage message) {
        // Skip forward 4 bytes to make space for the length field
        buffer.position(4);

        // Write the data
        serialize(buffer, message);

        // Figure out how long the data is
        int dataLength = buffer.position() - 4;

        // Drop in the length
        byte[] lengthBytes = long2bin(dataLength);

        for(int i=0; i < lengthBytes.length; i++){
            buffer.put(i, lengthBytes[i]);
        }
    }

    protected static HtspMessage deserialize(ByteBuffer buffer) {
        HtspMessage message = new HtspMessage();

        byte fieldType;
        String key;
        byte keyLength;
        byte[] valueLengthBytes = new byte[4];
        long valueLength;
        byte[] valueBytes;
        Object value = null;

        int listIndex = 0;

        while (buffer.hasRemaining()) {
            fieldType = buffer.get();
            keyLength = buffer.get();
            buffer.get(valueLengthBytes);
            valueLength = bin2long(valueLengthBytes);

            // 50000000 is ~50MB, aka improbably large. Without this guard, we'll get a series of
            // OutOfMemoryError crash reports, which don't group nicely as the values are always
            // different. This makes it hard to understand the extent of the issue or begin tracing
            // the bug (it may even be a TVHeadend bug?)
            if (valueLength > 50000000) {
                Timber.e("Attempted to deserialize an improbably large field (" + valueLength + " bytes)");
                throw new RuntimeException("Attempted to deserialize an improbably large field");
            }

            // Deserialize the Key
            if (keyLength == 0) {
                // Working on a list...
                key = Integer.toString(listIndex++);
            } else {
                // Working on a map..
                byte[] keyBytes = new byte[keyLength];
                buffer.get(keyBytes);
                key = new String(keyBytes);
            }

            // Extract Value bytes
            valueBytes = new byte[(int) valueLength];
            buffer.get(valueBytes);

            // Deserialize the Value
            if (fieldType == FIELD_STR) {
                if (DEBUG) {
                    Log.v(TAG, "Deserializaing a STR with key " + key);
                }
                value = new String(valueBytes);

            } else if (fieldType == FIELD_S64) {
                if (DEBUG) {
                    Log.v(TAG, "Deserializaing a S64 with key " + key + " and valueBytes length " + valueBytes.length);
                }
                value = toBigInteger(valueBytes);

            } else if (fieldType == FIELD_MAP) {
                if (DEBUG) {
                    Log.v(TAG, "Deserializaing a MAP with key " + key);
                }
                value = deserialize(ByteBuffer.wrap(valueBytes));

            } else if (fieldType == FIELD_LIST) {
                if (DEBUG) {
                    Log.v(TAG, "Deserializaing a LIST with key " + key);
                }
                value = new ArrayList<>(deserialize(ByteBuffer.wrap(valueBytes)).values());

            } else if (fieldType == FIELD_BIN) {
                if (DEBUG) {
                    Log.v(TAG, "Deserializaing a BIN with key " + key);
                }
                value = valueBytes;

            } else {
                throw new RuntimeException("Cannot deserialize unknown data type, derp: " + fieldType);
            }

            if (value != null) {
                message.put(key, value);
            }
        }

        return message;
    }

    protected void serialize(ByteBuffer buffer, Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            serialize(buffer, entry.getKey(), entry.getValue());
        }
    }

    protected void serialize(ByteBuffer buffer, Iterable<?> list) {
        for (Object value : list) {
            // Lists are just like maps, but with empty / zero length keys.
            serialize(buffer, "", value);
        }
    }

    @SuppressWarnings("unchecked") // We cast LOTS here...
    protected void serialize(ByteBuffer buffer, String key, Object value) {
        byte[] keyBytes = key.getBytes();
        ByteBuffer valueBytes = ByteBuffer.allocate(65535);

        // 1 byte type
        if (value == null) {
            // Ignore and do nothing
            return;
        } else if (value instanceof String) {
            if (DEBUG) {
                Log.v(TAG, "Serializaing a STR with key " + key + " value " + value);
            }
            buffer.put(FIELD_STR);
            valueBytes.put(((String) value).getBytes());
        } else if (value instanceof BigInteger) {
            if (DEBUG) {
                Log.v(TAG, "Serializaing a S64b with key " + key + " value " + value);
            }
            buffer.put(FIELD_S64);
            valueBytes.put(toByteArray((BigInteger) value));
        } else if (value instanceof Integer) {
            if (DEBUG) {
                Log.v(TAG, "Serializaing a S64i with key " + key + " value " + value);
            }
            buffer.put(FIELD_S64);
            valueBytes.put(toByteArray(BigInteger.valueOf((Integer) value)));
        } else if (value instanceof Long) {
            if (DEBUG) {
                Log.v(TAG, "Serializaing a S64l with key " + key + " value " + value);
            }
            buffer.put(FIELD_S64);
            valueBytes.put(toByteArray(BigInteger.valueOf((Long) value)));
        } else if (value instanceof Map) {
            if (DEBUG) {
                Log.v(TAG, "Serializaing a MAP with key " + key);
            }
            buffer.put(FIELD_MAP);
            serialize(valueBytes, (Map<String, Object>) value);
        } else if (value instanceof byte[]) {
            if (DEBUG) {
                Log.v(TAG, "Serializaing a BIN with key " + key);
            }
            buffer.put(FIELD_BIN);
            valueBytes.put((byte[]) value);
        } else if (value instanceof Iterable) {
            if (DEBUG) {
                Log.v(TAG, "Serializaing a LIST with key " + key);
            }
            buffer.put(FIELD_LIST);
            serialize(valueBytes, (Iterable<?>) value);
        } else {
            throw new RuntimeException("Cannot serialize unknown data type, derp: " + value.getClass().getName());
        }

        // 1 byte key length
        buffer.put((byte) (keyBytes.length & 0xFF));

        // Reset the Value Buffer and grab it's length
        valueBytes.flip();
        int valueLength = valueBytes.limit();

        // 4 bytes value length
        buffer.put(long2bin(valueLength));

        // Key + Value Bytes
        buffer.put(keyBytes);
        buffer.put(valueBytes);
    }

    private static byte[] long2bin(long l) {
        /**
         * return chr(i >> 24 & 0xFF) + chr(i >> 16 & 0xFF) + chr(i >> 8 & 0xFF) + chr(i & 0xFF)
         */
        byte[] result = new byte[4];

        result[0] = (byte) ((l >> 24) & 0xFF);
        result[1] = (byte) ((l >> 16) & 0xFF);
        result[2] = (byte) ((l >> 8) & 0xFF);
        result[3] = (byte) (l & 0xFF);

        return result;
    }

    private static long bin2long(byte[] bytes) {
        /**
         *  return (ord(d[0]) << 24) + (ord(d[1]) << 16) + (ord(d[2]) <<  8) + ord(d[3])
         */
        long result = 0;

        result ^= (bytes[0] & 0xFF) << 24;
        result ^= (bytes[1] & 0xFF) << 16;
        result ^= (bytes[2] & 0xFF) << 8;
        result ^= bytes[3] & 0xFF;

        return result;
    }

    private static byte[] toByteArray(BigInteger big) {
        // Convert to a byte array
        byte[] b = big.toByteArray();

        // Reverse the byte order
        byte b1[] = new byte[b.length];
        for (int i = 0; i < b.length; i++) {
            b1[i] = b[b.length - 1 - i];
        }

        // Negative numbers in HTSP are weird
        if (big.compareTo(BigInteger.ZERO) < 0) {
            byte[] b3 = new byte[8];
            Arrays.fill(b3, (byte) 0xFF);
            System.arraycopy(b1, 0, b3, 0, b1.length - 1);
            return b3;
        }

        return b1;
    }

    private static BigInteger toBigInteger(byte b[]) {
        byte b1[] = new byte[b.length + 1];

        // Reverse the order
        for (int i = 0; i < b.length; i++) {
            b1[i + 1] = b[b.length - 1 - i];
        }

        // Convert to a BigInteger
        return new BigInteger(b1);
    }
}
