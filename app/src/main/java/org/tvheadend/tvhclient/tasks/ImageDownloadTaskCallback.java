package org.tvheadend.tvhclient.tasks;

import android.graphics.drawable.Drawable;

public interface ImageDownloadTaskCallback {
    void notify(Drawable image);
}
