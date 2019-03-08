package org.tvheadend.tvhclient.ui.base.callbacks

import android.view.View

interface RecyclerViewClickCallback {
    fun onClick(view: View, position: Int)
    fun onLongClick(view: View, position: Int): Boolean
}
