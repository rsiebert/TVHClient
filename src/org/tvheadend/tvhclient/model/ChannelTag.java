package org.tvheadend.tvhclient.model;

import java.util.Comparator;
import java.util.Locale;
import android.graphics.Bitmap;

public class ChannelTag {

    public long id;
    public String name;
    public String icon;
    public Bitmap iconBitmap;

    public static Comparator<ChannelTag> ChannelTagNameSorter = new Comparator<ChannelTag>() {
        public int compare(ChannelTag x, ChannelTag y) {
            if (x != null && y != null && x.name != null && y.name != null) {
                return x.name.toLowerCase(Locale.getDefault()).compareTo(
                        y.name.toLowerCase(Locale.getDefault()));
            }
            return 0;
        }
    };

    @Override
    public String toString() {
        return name;
    }
}
