package org.tvheadend.tvhclient.ui.common;

import android.view.View;

public interface RecyclerViewTouchCallback {
    void onClick(View view, int position);
    void onLongClick(View view, int position);
}
