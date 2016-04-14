package org.tvheadend.tvhclient.model;

import android.graphics.Bitmap;
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
    public Set<Program> epg = Collections.synchronizedSortedSet(new TreeSet<Program>());
    public Set<Recording> recordings = Collections.synchronizedSortedSet(new TreeSet<Recording>());
    public List<Integer> tags;
    public Bitmap iconBitmap;
    public boolean isTransmitting;
    
    public int compareTo(Channel that) {
        return this.number - that.number;
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
            if ("recording".equals(rec.state)) {
                return true;
            }
        }
        return false;
    }
}
