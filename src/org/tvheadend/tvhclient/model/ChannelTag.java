package org.tvheadend.tvhclient.model;

import android.graphics.Bitmap;

public class ChannelTag {

    public long id;
    public String name;
    public String icon;
    public Bitmap iconBitmap;
    
    @Override
    public String toString() {
        return name;
    }
}
