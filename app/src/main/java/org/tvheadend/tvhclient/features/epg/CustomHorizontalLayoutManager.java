package org.tvheadend.tvhclient.features.epg;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;

public class CustomHorizontalLayoutManager extends LinearLayoutManager {

    CustomHorizontalLayoutManager(Context context, int orientation, boolean reverseLayout) {
        super(context, orientation, reverseLayout);
    }

    @Override
    public boolean canScrollHorizontally() {
        return false;
    }
}
