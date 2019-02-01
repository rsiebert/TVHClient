package org.tvheadend.tvhclient.features.epg;

import android.content.Context;
import androidx.recyclerview.widget.LinearLayoutManager;

class CustomHorizontalLayoutManager extends LinearLayoutManager {

    CustomHorizontalLayoutManager(Context context) {
        super(context, HORIZONTAL, false);
    }

    @Override
    public boolean canScrollHorizontally() {
        return false;
    }
}
