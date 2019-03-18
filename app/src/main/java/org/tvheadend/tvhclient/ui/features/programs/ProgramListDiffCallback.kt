package org.tvheadend.tvhclient.ui.features.programs

import androidx.recyclerview.widget.DiffUtil
import org.tvheadend.tvhclient.domain.entity.Program

internal class ProgramListDiffCallback(private val oldList: List<Program>, private val newList: List<Program>) : DiffUtil.Callback() {

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

    override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
        // One can return particular field for changed item.
        return super.getChangePayload(oldItemPosition, newItemPosition)
    }
}
