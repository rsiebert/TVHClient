package org.tvheadend.tvhclient.model;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class Channel implements Comparable<Channel> {

    public long id;
    public String uuid;
    public String name;
    public String icon;
    public int number;
    public int numberMinor;
    public final Set<Program> epg = Collections.synchronizedSortedSet(new TreeSet<Program>());
    public final Set<Recording> recordings = Collections.synchronizedSortedSet(new TreeSet<Recording>());
    public List<Integer> tags;
    public Bitmap iconBitmap;
    public boolean isTransmitting;
    
    public int compareTo(@NonNull Channel that) {
        final int res = this.number - that.number;
        if (res > 0) {
            return 1;
        } else if (res < 0) {
            return -1;
        } else {
            return 0;
        }
    }

    public boolean hasTag(long id) {
        if (id == 0) {
            return true;
        }

        for (Integer i : tags) {
            if (i == id) {
                return true;
            }
        }
        return false;
    }

    public boolean isRecording() {
        for (Recording rec : recordings) {
            if (rec.isRecording()) {
                return true;
            }
        }
        return false;
    }
}
