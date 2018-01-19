package org.tvheadend.tvhclient.ui.common;

import android.view.View;

public interface RecyclerViewClickCallback {
    void onClick(View view, int position);
    boolean onLongClick(View view);
}
