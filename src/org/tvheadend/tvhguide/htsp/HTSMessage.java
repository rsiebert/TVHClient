/*
 *  Copyright (C) 2011 John Törnblom
 *
 * This file is part of TVHGuide.
 *
 * TVHGuide is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * TVHGuide is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with TVHGuide.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.tvheadend.tvhguide.htsp;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author john-tornblom
 */
public class HTSMessage extends HashMap<String, Object> {

    public static final long HTSP_VERSION = 8;
    private static final byte HMF_MAP = 1;
    private static final byte HMF_S64 = 2;
    private static final byte HMF_STR = 3;
    private static final byte HMF_BIN = 4;
    private static final byte HMF_LIST = 5;
    private ByteBuffer buf;

    public void putField(String name, Object value) {
        if (value != null) {
            put(name, value);
        }
    }

    public void setMethod(String name) {
        put("method", name);
    }

    public String getMethod() {
        return getString("method", "");
    }

    public boolean containsField(String name) {
        return containsKey(name);
    }

    public BigInteger getBigInteger(String name) {
        return (BigInteger) get(name);
    }

    public long getLong(String name) {
        return getBigInteger(name).longValue();
    }

    public long getLong(String name, long std) {
        if (!containsField(name)) {
            return std;
        }
        return getLong(name);
    }

    public int getInt(String name) {
        return getBigInteger(name).intValue();
    }

    public int getInt(String name, int std) {
        if (!containsField(name)) {
            return std;
        }
        return getInt(name);
    }

    public String getString(String name, String std) {
        if (!containsField(name)) {
            return std;
        }
        return getString(name);
    }

    public String getString(String name) {
        Object obj = get(name);
        if (obj == null) {
            return null;
        }
        return obj.toString();
    }

    public List<Long> getLongList(String name) {
        ArrayList<Long> list = new ArrayList<Long>();

        if (!containsField(name)) {
            return list;
        }

        for (Object obj : (List) get(name)) {
            if (obj instanceof BigInteger) {
                list.add(((BigInteger) obj).longValue());
            }
        }

        return list;
    }

    List<Long> getLongList(String name, List<Long> std) {
        if (!containsField(name)) {
            return std;
        }

        return getLongList(name);
    }

    public List<Integer> getIntList(String name) {
        ArrayList<Integer> list = new ArrayList<Integer>();

        if (!containsField(name)) {
            return list;
        }

        for (Object obj : (List) get(name)) {
            if (obj instanceof BigInteger) {
                list.add(((BigInteger) obj).intValue());
            }
        }

        return list;
    }

    List<Integer> getIntList(String name, List<Integer> std) {
        if (!containsField(name)) {
            return std;
        }

        return getIntList(name);
    }

    public List getList(String name) {
        return (List) get(name);
    }

    public byte[] getByteArray(String name) {
        return (byte[]) get(name);
    }

    public Date getDate(String name) {
        return new Date(getLong(name) * 1000);
    }

    public boolean transmit(SocketChannel ch) throws IOException {
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

        if (buf.hasRemaining()) {
            return false;
        } else {
            buf.flip();
            return true;
        }
    }

    public static String getHexString(byte[] b) throws Exception {
        String result = "";
        for (int i = 0; i < b.length; i++) {
            result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
        }
        return result;
    }

    private static byte[] toByteArray(BigInteger big) {
        byte[] b = big.toByteArray();
        byte b1[] = new byte[b.length];

        for (int i = 0; i < b.length; i++) {
            b1[i] = b[b.length - 1 - i];
        }

        return b1;
    }

    private static BigInteger toBigInteger(byte b[]) {
        byte b1[] = new byte[b.length + 1];

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

    public static HTSMessage parse(ByteBuffer buf) throws IOException {
        long len;

        if (buf.position() < 4) {
            return null;
        }

        len = uIntToLong(buf.get(0), buf.get(1), buf.get(2), buf.get(3));

        if (len + 4 > buf.capacity()) {
            buf.clear();
            throw new IOException("Mesage is to long");
        }

        if (buf.limit() == 4) {
            buf.limit((int) (4 + len));
        }

        //Message not yet fully read
        if (buf.position() < len + 4) {
            return null;
        }

        buf.flip();
        buf.getInt(); //drops 4 bytes
        HTSMessage msg = deserializeBinary(buf);

        buf.limit(4);
        buf.position(0);
        return msg;
    }

    private static byte[] serializeBinary(String name, Object value) throws IOException {
        byte[] bName = name.getBytes();
        byte[] bData = new byte[0];
        byte type;

        if (value instanceof String) {
            type = HTSMessage.HMF_STR;
            bData = ((String) value).getBytes();
        } else if (value instanceof BigInteger) {
            type = HTSMessage.HMF_S64;
            bData = toByteArray((BigInteger) value);
        } else if (value instanceof Integer) {
            type = HTSMessage.HMF_S64;
            bData = toByteArray(BigInteger.valueOf((Integer) value));
        } else if (value instanceof Long) {
            type = HTSMessage.HMF_S64;
            bData = toByteArray(BigInteger.valueOf((Long) value));
        } else if (value instanceof byte[]) {
            type = HTSMessage.HMF_BIN;
            bData = (byte[]) value;
        } else if (value instanceof Map) {
            type = HTSMessage.HMF_MAP;
            bData = serializeBinary((Map) value);
        } else if (value instanceof Collection) {
            type = HTSMessage.HMF_LIST;
            bData = serializeBinary((Collection) value);
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

    private static byte[] serializeBinary(Collection list) throws IOException {
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

    private static byte[] serializeBinary(Map map) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(Short.MAX_VALUE);

        for (Object key : map.keySet()) {
            Object value = map.get(key);
            byte[] sub = serializeBinary(key.toString(), value);
            buf.put(sub);
        }

        byte[] bBuf = new byte[buf.position()];
        buf.flip();
        buf.get(bBuf);

        return bBuf;
    }

    private static HTSMessage deserializeBinary(ByteBuffer buf) throws IOException {
        byte type, namelen;
        long datalen;

        HTSMessage msg = new HTSMessage();
        int cnt = 0;

        while (buf.hasRemaining()) {
            type = buf.get();
            namelen = buf.get();
            datalen = uIntToLong(buf.get(), buf.get(), buf.get(), buf.get());

            if (datalen > Integer.MAX_VALUE) {
                throw new IOException("Would get precision losses ;(");
            }
            if (buf.limit() < namelen + datalen) {
                throw new IOException("Buffer limit exceeded");
            }

            //Get the key for the map (the name)
            String name = null;
            if (namelen == 0) {
                name = Integer.toString(cnt++);
            } else {
                byte[] bName = new byte[namelen];
                buf.get(bName);
                name = new String(bName);
            }

            //Get the actual content
            Object obj = null;
            byte[] bData = new byte[(int) datalen]; //Should be long?
            buf.get(bData);

            switch (type) {
                case HTSMessage.HMF_STR: {
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
                    obj = new ArrayList<Object>(deserializeBinary(sub).values());
                    break;
                }
                default:
                    throw new IOException("Unknown data type");
            }
            msg.putField(name, obj);
        }
        return msg;
    }
}
