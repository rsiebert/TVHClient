package org.tvheadend.tvhclient.data.tasks;

import android.graphics.drawable.Drawable;

public interface ImageDownloadTaskCallback {
    void notify(Drawable image);
}
