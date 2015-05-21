package org.tvheadend.tvhclient.model;

import java.util.Comparator;
import java.util.Locale;
import android.graphics.Bitmap;

public class ChannelTag {

    public long id;
    public String name;
    public String icon;
    public Bitmap iconBitmap;
    
    public static Comparator<ChannelTag> ChannelTagNameSortor = new Comparator<ChannelTag>() {
    	public int compare(ChannelTag x, ChannelTag y) {
    	   return x.name.toLowerCase(Locale.US).compareTo(y.name.toLowerCase(Locale.US));
      }};    
    
    @Override
    public String toString() {
        return name;
    }
}
