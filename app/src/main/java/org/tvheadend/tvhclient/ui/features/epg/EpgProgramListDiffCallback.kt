package org.tvheadend.tvhclient.ui.features.epg

import androidx.recyclerview.widget.DiffUtil
import org.tvheadend.data.entity.EpgProgram

internal class EpgProgramListDiffCallback(private val oldList: List<EpgProgram>, private val newList: List<EpgProgram>) : DiffUtil.Callback() {

    override fun getOldListSize(): Int {
        return oldList.size
    }

    override fun getNewListSize(): Int {
        return newList.size
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return newList[newItemPosition].eventId == oldList[oldItemPosition].eventId
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return newList[newItemPosition] == oldList[oldItemPosition]
    }
}
