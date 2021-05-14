package org.tvheadend.tvhclient.ui.features.epg

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.recyclerview.widget.RecyclerView
import org.tvheadend.data.entity.EpgChannel
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.databinding.EpgChannelRecyclerviewAdapterBinding
import org.tvheadend.tvhclient.ui.common.interfaces.RecyclerViewClickInterface
import java.util.*

class EpgChannelListRecyclerViewAdapter(private val viewModel: EpgViewModel, private val clickCallback: RecyclerViewClickInterface, private val lifecycleOwner: LifecycleOwner) : RecyclerView.Adapter<EpgChannelListRecyclerViewAdapter.EpgChannelViewHolder>() {

    private val channelList = ArrayList<EpgChannel>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EpgChannelViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val itemBinding = EpgChannelRecyclerviewAdapterBinding.inflate(layoutInflater, parent, false)
        val viewHolder = EpgChannelViewHolder(itemBinding, viewModel)
        itemBinding.lifecycleOwner = lifecycleOwner
        return viewHolder
    }

    override fun onBindViewHolder(holder: EpgChannelViewHolder, position: Int) {
        val channel = channelList[position]
        holder.bind(channel, position, clickCallback)
    }

    fun addItems(list: List<EpgChannel>) {
        channelList.clear()
        channelList.addAll(list)

        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return channelList.size
    }

    override fun getItemViewType(position: Int): Int {
        return R.layout.epg_channel_recyclerview_adapter
    }

    fun getItem(position: Int): EpgChannel? {
        return if (channelList.size > position && position >= 0) {
            channelList[position]
        } else {
            null
        }
    }

    class EpgChannelViewHolder(private val binding: EpgChannelRecyclerviewAdapterBinding,
                               private val viewModel: EpgViewModel) : RecyclerView.ViewHolder(binding.root) {

        fun bind(channel: EpgChannel, position: Int, clickCallback: RecyclerViewClickInterface) {
            binding.channel = channel
            binding.position = position
            binding.callback = clickCallback
            binding.viewModel = viewModel
            binding.executePendingBindings()
        }
    }
}
