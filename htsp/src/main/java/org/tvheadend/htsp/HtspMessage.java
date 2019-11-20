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

public class HtspMessage extends HashMap<String, Object> {

    private static final long serialVersionUID = 1L;

    static final long HTSP_VERSION = 32;
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

    public List<?> getList(String name) {
        return (List<?>) get(name);
    }

    public List<Integer> getIntegerList(String name) {
        ArrayList<Integer> list = new ArrayList<>();
        if (!containsKey(name)) {
            return list;
        }
        for (Object obj : getList(name)) {
            if (obj instanceof BigInteger) {
                list.add(((BigInteger) obj).intValue());
            }
        }
        return list;
    }

    public ArrayList getArrayList(String key) {
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

    private static byte[] serializeBinary(Collection<?> list) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(Short.MAX_VALUE);

        for (Object value : list) {
            byte[] sub = serializeBinary("", value);
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
                throw new IOException("Buffer limit exceeded, limit " + buf.limit() + ", namelen " + namelen + ", datalen " + datalen);
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
                throw new IOException("Buffer position is negative, namelen " + namelen);
            }

            // Get the actual content
            Object obj;
            byte[] bData = new byte[(int) datalen]; // Should be long?
            buf.get(bData);

            switch (type) {
                case HtspMessage.HMF_STR: {
                    obj = new String(bData);
                    break;
                }
                case HMF_BIN: {
                    obj = bData;
                    break;
                }
                case HMF_S64: {
                    obj = toBigInteger(bData);
                    break;
                }
                case HMF_MAP: {
                    ByteBuffer sub = ByteBuffer.allocateDirect((int) datalen);
                    sub.put(bData);
                    sub.flip();
                    obj = deserializeBinary(sub);
                    break;
                }
                case HMF_LIST: {
                    ByteBuffer sub = ByteBuffer.allocateDirect((int) datalen);
                    sub.put(bData);
                    sub.flip();
                    obj = new ArrayList<>(deserializeBinary(sub).values());
                    break;
                }
                default:
                    throw new IOException("Unknown data type " + type);
            }
            msg.put(name, obj);
        }
        return msg;
    }
}
