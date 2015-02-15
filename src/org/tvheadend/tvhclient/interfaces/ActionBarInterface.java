package org.tvheadend.tvhclient.interfaces;

import android.graphics.Bitmap;

public interface ActionBarInterface {

    void setActionBarTitle(final String title, final String tag);

    void setActionBarSubtitle(final String subtitle, final String tag);

    void setActionBarIcon(final Bitmap bitmap, final String tag);
    
    void setActionBarIcon(final int resource, final String tag);
}
