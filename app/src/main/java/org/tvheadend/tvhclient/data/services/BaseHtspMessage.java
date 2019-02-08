package org.tvheadend.tvhclient.data.services;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class BaseHtspMessage extends HashMap<String, Object> {

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

    List<Integer> getIntegerList(String name, List<Integer> std) {
        if (!containsKey(name)) {
            return std;
        }
        return getIntegerList(name);
    }

    public String[] getStringArray(String key) {
        ArrayList value = getArrayList(key);
        return (String[]) value.toArray(new String[value.size()]);
    }

    public ArrayList getArrayList(String key) {
        Object obj = get(key);
        //noinspection unchecked
        return (ArrayList<String>) obj;
    }

    public BaseHtspMessage[] getHtspMessageArray(String key) {
        ArrayList value = getArrayList(key);

        return (BaseHtspMessage[]) value.toArray(new BaseHtspMessage[value.size()]);
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


}
