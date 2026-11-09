package org.tvheadend.tvhclient.ui.features.information

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.tvheadend.tvhclient.databinding.ChangelogEntryItemBinding
import org.tvheadend.tvhclient.databinding.ChangelogVersionItemBinding

/**
 * Renders the changelog rows, that is the version headings and the individual change items.
 */
internal class ChangeLogRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val rows = mutableListOf<ChangeLogRow>()

    override fun getItemCount(): Int = rows.size

    override fun getItemViewType(position: Int): Int = when (rows[position]) {
        is ChangeLogRow.Header -> TYPE_HEADER
        is ChangeLogRow.Item -> TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) {
            HeaderViewHolder(ChangelogVersionItemBinding.inflate(inflater, parent, false))
        } else {
            ItemViewHolder(ChangelogEntryItemBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = rows[position]) {
            is ChangeLogRow.Header -> (holder as HeaderViewHolder).bind(row)
            is ChangeLogRow.Item -> (holder as ItemViewHolder).bind(row)
        }
    }

    internal fun submitRows(newRows: List<ChangeLogRow>) {
        rows.clear()
        rows.addAll(newRows)
        notifyDataSetChanged()
    }

    internal class HeaderViewHolder(private val binding: ChangelogVersionItemBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(row: ChangeLogRow.Header) {
            binding.versionText.text = row.version
            binding.dateText.text = row.date
        }
    }

    internal class ItemViewHolder(private val binding: ChangelogEntryItemBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(row: ChangeLogRow.Item) {
            binding.entryText.text = row.text
        }
    }

    private companion object {
        const val TYPE_HEADER = 0
        const val TYPE_ITEM = 1
    }
}
