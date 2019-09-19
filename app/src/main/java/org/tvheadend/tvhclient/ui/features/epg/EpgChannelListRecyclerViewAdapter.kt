package org.tvheadend.tvhclient.ui.features.epg

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.recyclerview.widget.RecyclerView
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.databinding.EpgChannelAdapterItemBinding
import org.tvheadend.tvhclient.domain.entity.EpgChannel
import org.tvheadend.tvhclient.ui.common.callbacks.RecyclerViewClickCallback
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

class EpgChannelListRecyclerViewAdapter(private val viewModel: EpgViewModel, private val clickCallback: RecyclerViewClickCallback) : RecyclerView.Adapter<EpgChannelListRecyclerViewAdapter.EpgChannelViewHolder>(), Filterable {

    private val channelList = ArrayList<EpgChannel>()
    private var channelListFiltered: MutableList<EpgChannel> = ArrayList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EpgChannelViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val itemBinding = EpgChannelAdapterItemBinding.inflate(layoutInflater, parent, false)
        val viewHolder = EpgChannelViewHolder(itemBinding, viewModel)
        itemBinding.lifecycleOwner = viewHolder
        return viewHolder
    }

    override fun onBindViewHolder(holder: EpgChannelViewHolder, position: Int) {
        val channel = channelListFiltered[position]
        holder.bind(channel, position, clickCallback)
    }

    fun addItems(list: List<EpgChannel>) {
        channelList.clear()
        channelListFiltered.clear()
        channelList.addAll(list)
        channelListFiltered.addAll(list)

        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return channelListFiltered.size
    }

    override fun getItemViewType(position: Int): Int {
        return R.layout.epg_channel_adapter_item
    }

    fun getItem(position: Int): EpgChannel? {
        return if (channelListFiltered.size > position && position >= 0) {
            channelListFiltered[position]
        } else {
            null
        }
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(charSequence: CharSequence): FilterResults {
                val charString = charSequence.toString()
                if (charString.isEmpty()) {
                    channelListFiltered = channelList
                } else {
                    val filteredList = ArrayList<EpgChannel>()
                    // Iterate over the available channels. Use a copy on write
                    // array in case the channel list changes during filtering.
                    for (channel in CopyOnWriteArrayList(channelList)) {
                        // name match condition. this might differ depending on your requirement
                        // here we are looking for a channel name match
                        val name = channel.name ?: ""
                        if (name.toLowerCase().contains(charString.toLowerCase())) {
                            filteredList.add(channel)
                        }
                    }
                    channelListFiltered = filteredList
                }

                val filterResults = FilterResults()
                filterResults.values = channelListFiltered
                return filterResults
            }

            override fun publishResults(charSequence: CharSequence, filterResults: FilterResults) {
                @Suppress("UNCHECKED_CAST")
                channelListFiltered = filterResults.values as ArrayList<EpgChannel>
                notifyDataSetChanged()
            }
        }
    }

    override fun onViewAttachedToWindow(holder: EpgChannelViewHolder) {
        super.onViewAttachedToWindow(holder)
        holder.markAttach()
    }

    override fun onViewDetachedFromWindow(holder: EpgChannelViewHolder) {
        super.onViewDetachedFromWindow(holder)
        holder.markDetach()
    }

    class EpgChannelViewHolder(private val binding: EpgChannelAdapterItemBinding, private val viewModel: EpgViewModel) : RecyclerView.ViewHolder(binding.root), LifecycleOwner {

        private val lifecycleRegistry = LifecycleRegistry(this)

        init {
            lifecycleRegistry.currentState = Lifecycle.State.INITIALIZED
        }

        fun markAttach() {
            lifecycleRegistry.currentState = Lifecycle.State.STARTED
        }

        fun markDetach() {
            lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        }

        override fun getLifecycle(): Lifecycle {
            return lifecycleRegistry
        }

        fun bind(channel: EpgChannel, position: Int, clickCallback: RecyclerViewClickCallback) {
            binding.channel = channel
            binding.position = position
            binding.callback = clickCallback
            binding.viewModel = viewModel
            binding.executePendingBindings()
        }
    }
}
