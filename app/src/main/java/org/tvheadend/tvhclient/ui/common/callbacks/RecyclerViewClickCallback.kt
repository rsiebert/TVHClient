package org.tvheadend.tvhclient.ui.common.callbacks

import android.view.View

interface RecyclerViewClickCallback {
    fun onClick(view: View, position: Int)
    fun onLongClick(view: View, position: Int): Boolean
}
