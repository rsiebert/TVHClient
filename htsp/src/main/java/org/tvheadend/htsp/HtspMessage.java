package org.tvheadend.htsp;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import timber.log.Timber;

public class HtspMessage extends HashMap<String, Object> {

    private static final long serialVersionUID = 1L;

    static final long HTSP_VERSION = 44;
    private static final byte HMF_MAP = 1;
    private static final byte HMF_S64 = 2;
    private static final byte HMF_STR = 3;
    private static final byte HMF_BIN = 4;
    private static final byte HMF_LIST = 5;
    private ByteBuffer buf;

    public void setMethod(String name) {
        put("method", name);
    }

    public String getMethod() {
        return getString("method", "");
    }

    public String getString(String key, String fallback) {
        // A key that is present but holds null is treated the same as an absent one. Previously
        // only an absent key returned the fallback, so a present-but-null value handed a null to
        // callers that had explicitly asked for a default.
        String value = getString(key);
        return value != null ? value : fallback;
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
        return toNumber(key, obj).intValue();
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
        return toNumber(key, obj).longValue();
    }

    /**
     * The numeric value of a decoded field. Integers arriving from the server are BigInteger, while
     * values put in locally are boxed Integer or Long - BigInteger, Integer and Long are all Number,
     * so everything is narrowed through it. Previously only BigInteger was recognised and anything
     * else was cast directly, so asking for a long that had been stored as an Integer - or an int
     * stored as a Long - threw ClassCastException.
     */
    private static Number toNumber(String key, Object value) {
        if (value instanceof Number) {
            return (Number) value;
        }
        // ClassCastException rather than a plainer exception: that is what this used to throw for a
        // non-numeric value, and it is what callers catch today.
        throw new ClassCastException("Attempted to read " + key + " as a number but it is a "
                + value.getClass().getSimpleName());
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

    public List<?> getList(String name) {
        return (List<?>) get(name);
    }

    public List<Integer> getIntegerList(String name) {
        ArrayList<Integer> list = new ArrayList<>();
        if (!containsKey(name)) {
            return list;
        }
        for (Object obj : getList(name)) {
            // Accept any Number, not just BigInteger, so a list put in locally as Integer or Long is
            // not silently dropped.
            if (obj instanceof Number) {
                list.add(((Number) obj).intValue());
            }
        }
        return list;
    }

    public ArrayList<String> getArrayList(String key) {
        Object obj = get(key);
        //noinspection unchecked
        return (ArrayList<String>) obj;
    }

    public byte[] getByteArray(String key) {
        Object value = get(key);

        return (byte[]) value;
    }

    void transmit(SocketChannel ch) throws IOException {
        if (buf == null) {
            byte[] data = serializeBinary(this);
            int len = data.length;
            buf = ByteBuffer.allocateDirect(len + 4);

            buf.put((byte) ((len >> 24) & 0xFF));
            buf.put((byte) ((len >> 16) & 0xFF));
            buf.put((byte) ((len >> 8) & 0xFF));
            buf.put((byte) ((len) & 0xFF));
            buf.put(data);
            buf.flip();
        }

        if (ch.write(buf) < 0) {
            throw new IOException("Server went down");
        }

        if (!buf.hasRemaining()) {
            buf.flip();
        }
    }

    private static byte[] toByteArray(BigInteger big) {
        byte[] b = big.toByteArray();
        byte[] b1 = new byte[b.length];

        for (int i = 0; i < b.length; i++) {
            b1[i] = b[b.length - 1 - i];
        }

        return b1;
    }

    private static BigInteger toBigInteger(byte[] b) {
        byte[] b1 = new byte[b.length + 1];

        for (int i = 0; i < b.length; i++) {
            b1[i + 1] = b[b.length - 1 - i];
        }

        return new BigInteger(b1);
    }

    private static long uIntToLong(byte b1, byte b2, byte b3, byte b4) {
        long i = 0;
        i <<= 8;
        i ^= b1 & 0xFF;
        i <<= 8;
        i ^= b2 & 0xFF;
        i <<= 8;
        i ^= b3 & 0xFF;
        i <<= 8;
        i ^= b4 & 0xFF;
        return i;
    }

    public static HtspMessage parse(ByteBuffer buf) throws IOException {
        long len;

        if (buf.position() < 4) {
            return null;
        }

        len = uIntToLong(buf.get(0), buf.get(1), buf.get(2), buf.get(3));

        if (len + 4 > buf.capacity()) {
            throw new IOException("Message is to long, length " + len + ", capacity " + buf.capacity());
        }

        if (buf.limit() == 4) {
            buf.limit((int) (4 + len));
        }

        // Message not yet fully read
        if (buf.position() < len + 4) {
            return null;
        }

        buf.flip();
        buf.getInt(); // drops 4 bytes
        HtspMessage msg = deserializeBinary(buf);

        buf.limit(4);
        buf.position(0);
        return msg;
    }

    @SuppressWarnings("unchecked")
    private static byte[] serializeBinary(String name, Object value) throws IOException {
        byte[] bName = name.getBytes();
        byte[] bData;
        byte type;

        if (value instanceof String) {
            type = HtspMessage.HMF_STR;
            bData = ((String) value).getBytes();
        } else if (value instanceof BigInteger) {
            type = HtspMessage.HMF_S64;
            bData = toByteArray((BigInteger) value);
        } else if (value instanceof Integer) {
            type = HtspMessage.HMF_S64;
            bData = toByteArray(BigInteger.valueOf((Integer) value));
        } else if (value instanceof Long) {
            type = HtspMessage.HMF_S64;
            bData = toByteArray(BigInteger.valueOf((Long) value));
        } else if (value instanceof byte[]) {
            type = HtspMessage.HMF_BIN;
            bData = (byte[]) value;
        } else if (value instanceof Map) {
            type = HtspMessage.HMF_MAP;

            bData = serializeBinary((Map<String, Object>) value);
        } else if (value instanceof Collection) {
            type = HtspMessage.HMF_LIST;
            bData = serializeBinary((Collection<?>) value);
        } else if (value == null) {
            throw new IOException("HTSP doesn't support null values");
        } else {
            throw new IOException("Unhandled class for " + name + ": " + value
                    + " (" + value.getClass().getSimpleName() + ")");
        }

        byte[] buf = new byte[1 + 1 + 4 + bName.length + bData.length];
        buf[0] = type;
        buf[1] = (byte) (bName.length & 0xFF);
        buf[2] = (byte) ((bData.length >> 24) & 0xFF);
        buf[3] = (byte) ((bData.length >> 16) & 0xFF);
        buf[4] = (byte) ((bData.length >> 8) & 0xFF);
        buf[5] = (byte) ((bData.length) & 0xFF);

        System.arraycopy(bName, 0, buf, 6, bName.length);
        System.arraycopy(bData, 0, buf, 6 + bName.length, bData.length);

        return buf;
    }

    /**
     * Grows the serialization buffer when a list or map does not fit into its current capacity.
     * The previous fixed size of {@code Short.MAX_VALUE} bytes overflowed for replies with many
     * entries (for example a program guide reply with thousands of events).
     */
    private static ByteBuffer growBuffer(ByteBuffer buf, int required) {
        if (buf.remaining() >= required) {
            return buf;
        }
        ByteBuffer bigger = ByteBuffer.allocate(
                Math.max(buf.capacity() * 2, buf.position() + required));
        buf.flip();
        bigger.put(buf);
        return bigger;
    }

    private static byte[] serializeBinary(Collection<?> list) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(Short.MAX_VALUE);

        for (Object value : list) {
            byte[] sub = serializeBinary("", value);
            buf = growBuffer(buf, sub.length);
            buf.put(sub);
        }

        byte[] bBuf = new byte[buf.position()];
        buf.flip();
        buf.get(bBuf);

        return bBuf;
    }

    private static byte[] serializeBinary(Map<String, Object> map) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(Short.MAX_VALUE);

        for (Object key : map.keySet()) {
            //noinspection SuspiciousMethodCalls
            Object value = map.get(key);
            byte[] sub = serializeBinary(key.toString(), value);
            buf = growBuffer(buf, sub.length);
            buf.put(sub);
        }

        byte[] bBuf = new byte[buf.position()];
        buf.flip();
        buf.get(bBuf);

        return bBuf;
    }

    private static HtspMessage deserializeBinary(ByteBuffer buf) throws IOException {
        byte type, namelen;
        long datalen;

        HtspMessage msg = new HtspMessage();
        int cnt = 0;

        while (buf.hasRemaining()) {
            type = buf.get();
            namelen = buf.get();
            datalen = uIntToLong(buf.get(), buf.get(), buf.get(), buf.get());

            if (datalen > Integer.MAX_VALUE) {
                throw new IOException("Would get precision losses, datalen " + datalen + ", max int " + Integer.MAX_VALUE);
            }
            if (buf.limit() < namelen + datalen) {
                // The message is incomplete: more data has to be read from the server first.
                Timber.v("Buffer limit exceeded, limit %d, namelen %d, datalen %d", buf.limit(), namelen, datalen);
                return null;
            }

            // Get the key for the map (the name)
            String name;
            if (namelen == 0) {
                name = Integer.toString(cnt++);
            } else if (namelen > 0) {
                byte[] bName = new byte[namelen];
                buf.get(bName);
                name = new String(bName);
            } else {
                // Only a corrupt message can get here, the name length is unsigned on the wire.
                Timber.w("Received a corrupt message, the name length is negative: %d", namelen);
                return null;
            }

            // Get the actual content
            Object obj;
            byte[] bData = new byte[(int) datalen]; // Should be long?
            buf.get(bData);

            switch (type) {
                case HtspMessage.HMF_STR -> obj = new String(bData);
                case HMF_BIN -> obj = bData;
                case HMF_S64 -> obj = toBigInteger(bData);
                case HMF_MAP -> {
                    ByteBuffer sub = ByteBuffer.allocateDirect((int) datalen);
                    sub.put(bData);
                    sub.flip();
                    obj = deserializeBinary(sub);
                }
                case HMF_LIST -> {
                    ByteBuffer sub = ByteBuffer.allocateDirect((int) datalen);
                    sub.put(bData);
                    sub.flip();
                    obj = new ArrayList<>(deserializeBinary(sub).values());
                }
                default -> throw new IOException("Unknown data type " + type);
            }
            msg.put(name, obj);
        }
        return msg;
    }
}
