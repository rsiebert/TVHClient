package org.tvheadend.tvhclient.ui.features.dialogs


import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.tvheadend.tvhclient.databinding.GenreColorListAdapterBinding

class GenreColorListAdapter internal constructor(private val contentInfo: Array<String>) : RecyclerView.Adapter<GenreColorListAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val itemBinding = GenreColorListAdapterBinding.inflate(layoutInflater, parent, false)
        return GenreColorListAdapter.ViewHolder(itemBinding)
    }

    override fun getItemCount(): Int {
        return contentInfo.size
    }

    /**
     * Applies the values to the available layout items
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind((position + 1) * 16, contentInfo[position])
    }

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    class ViewHolder(private val binding: GenreColorListAdapterBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(contentType: Int, contentName: String) {
            binding.contentType = contentType
            binding.contentName = contentName
        }
    }
}
