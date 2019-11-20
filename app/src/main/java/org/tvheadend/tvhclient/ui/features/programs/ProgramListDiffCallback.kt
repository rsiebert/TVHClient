package org.tvheadend.tvhclient.ui.features.programs

import androidx.recyclerview.widget.DiffUtil
import org.tvheadend.tvhclient.data.entity.ProgramInterface

internal class ProgramListDiffCallback(private val oldList: List<ProgramInterface>, private val newList: List<ProgramInterface>) : DiffUtil.Callback() {

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
