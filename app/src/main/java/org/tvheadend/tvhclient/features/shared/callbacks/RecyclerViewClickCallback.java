package org.tvheadend.tvhclient.features.shared.callbacks;

import android.view.View;

public interface RecyclerViewClickCallback {
    void onClick(View view, int position);
    void onLongClick(View view, int position);
}
