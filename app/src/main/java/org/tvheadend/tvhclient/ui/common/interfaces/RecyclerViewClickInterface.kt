package org.tvheadend.tvhclient.ui.common.interfaces

import android.view.View

interface RecyclerViewClickInterface {
    fun onClick(view: View, position: Int)
    fun onLongClick(view: View, position: Int): Boolean
}
