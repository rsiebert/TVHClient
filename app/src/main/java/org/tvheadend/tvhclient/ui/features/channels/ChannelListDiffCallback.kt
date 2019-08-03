package org.tvheadend.tvhclient.ui.features.channels

import androidx.recyclerview.widget.DiffUtil
import org.tvheadend.tvhclient.domain.entity.Channel

internal class ChannelListDiffCallback(private val oldList: List<Channel>, private val newList: List<Channel>) : DiffUtil.Callback() {

    override fun getOldListSize(): Int {
        return oldList.size
    }

    override fun getNewListSize(): Int {
        return newList.size
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return newList[newItemPosition].id == oldList[oldItemPosition].id
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return newList[newItemPosition] == oldList[oldItemPosition]
    }
}
