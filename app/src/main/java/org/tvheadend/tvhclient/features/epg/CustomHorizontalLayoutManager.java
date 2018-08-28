package org.tvheadend.tvhclient.features.epg;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;

class CustomHorizontalLayoutManager extends LinearLayoutManager {

    CustomHorizontalLayoutManager(Context context) {
        super(context, HORIZONTAL, false);
    }

    @Override
    public boolean canScrollHorizontally() {
        return false;
    }
}
