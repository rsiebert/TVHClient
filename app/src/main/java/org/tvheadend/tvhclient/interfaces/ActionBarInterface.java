package org.tvheadend.tvhclient.interfaces;

import android.graphics.Bitmap;

public interface ActionBarInterface {

    void setActionBarTitle(final String title);

    void setActionBarSubtitle(final String subtitle);

    void setActionBarIcon(final Bitmap bitmap);
    
    void setActionBarIcon(final int resource);
}
