package org.tvheadend.tvhclient.ui.features.epg

import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager

internal class CustomHorizontalLayoutManager(context: Context) : LinearLayoutManager(context, HORIZONTAL, false) {

    override fun canScrollHorizontally(): Boolean {
        return false
    }
}
