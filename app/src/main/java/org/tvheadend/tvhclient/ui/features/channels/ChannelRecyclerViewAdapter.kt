package org.tvheadend.tvhclient.ui.features.channels

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.databinding.ChannelListAdapterBinding
import org.tvheadend.tvhclient.domain.entity.Channel
import org.tvheadend.tvhclient.domain.entity.Recording
import org.tvheadend.tvhclient.ui.common.callbacks.RecyclerViewClickCallback
import org.tvheadend.tvhclient.util.isEqualTo
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

class ChannelRecyclerViewAdapter internal constructor(private val viewModel: ChannelViewModel, private val clickCallback: RecyclerViewClickCallback) : RecyclerView.Adapter<ChannelRecyclerViewAdapter.ChannelViewHolder>(), Filterable {

    private val recordingList = ArrayList<Recording>()
    private val channelList = ArrayList<Channel>()
    private var channelListFiltered: MutableList<Channel> = ArrayList()
    private var selectedPosition = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val itemBinding = ChannelListAdapterBinding.inflate(layoutInflater, parent, false)
        val viewHolder = ChannelViewHolder(itemBinding, viewModel)
        itemBinding.lifecycleOwner = viewHolder
        return viewHolder
    }

    override fun onBindViewHolder(holder: ChannelViewHolder, position: Int) {
        if (channelListFiltered.size > position) {
            val channel = channelListFiltered[position]
            holder.bind(channel, position, selectedPosition == position, clickCallback)
        }
    }

    override fun onBindViewHolder(holder: ChannelViewHolder, position: Int, payloads: List<Any>) {
        onBindViewHolder(holder, position)
    }

    internal fun addItems(newItems: MutableList<Channel>) {
        updateRecordingState(newItems, recordingList)

        val oldItems = ArrayList(channelListFiltered)
        val diffResult = DiffUtil.calculateDiff(ChannelListDiffCallback(oldItems, newItems))

        channelList.clear()
        channelListFiltered.clear()
        channelList.addAll(newItems)
        channelListFiltered.addAll(newItems)
        diffResult.dispatchUpdatesTo(this)

        if (selectedPosition > channelListFiltered.size) {
            selectedPosition = 0
        }
    }

    override fun getItemCount(): Int {
        return channelListFiltered.size
    }

    override fun getItemViewType(position: Int): Int {
        return R.layout.channel_list_adapter
    }

    fun setPosition(pos: Int) {
        notifyItemChanged(selectedPosition)
        selectedPosition = pos
        notifyItemChanged(pos)
    }

    fun getItem(position: Int): Channel? {
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
                    val filteredList = ArrayList<Channel>()
                    // Iterate over the available channels. Use a copy on write
                    // array in case the channel list changes during filtering.
                    for (channel in CopyOnWriteArrayList(channelList)) {
                        // name match condition. this might differ depending on your requirement
                        // here we are looking for a channel name match
                        val name = channel.name ?: ""
                        val programTitle = channel.programTitle ?: ""
                        val programSubtitle = channel.programSubtitle ?: ""
                        val nextProgramTitle = channel.nextProgramTitle ?: ""
                        when {
                            name.toLowerCase().contains(charString.toLowerCase()) -> filteredList.add(channel)
                            programTitle.toLowerCase().contains(charString.toLowerCase()) -> filteredList.add(channel)
                            programSubtitle.toLowerCase().contains(charString.toLowerCase()) -> filteredList.add(channel)
                            nextProgramTitle.toLowerCase().contains(charString.toLowerCase()) -> filteredList.add(channel)
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
                channelListFiltered = filterResults.values as ArrayList<Channel>
                notifyDataSetChanged()
            }
        }
    }

    /**
     * Whenever a recording changes in the database the list of available recordings are
     * saved in this recycler view. The previous list is cleared to avoid showing outdated
     * recording states. Each recording is checked if it belongs to the
     * currently shown program. If yes then its state is updated.
     *
     * @param list List of recordings
     */
    internal fun addRecordings(list: List<Recording>) {
        recordingList.clear()
        recordingList.addAll(list)
        updateRecordingState(channelListFiltered, recordingList)
    }

    private fun updateRecordingState(channels: MutableList<Channel>, recordings: List<Recording>) {
        for (i in channels.indices) {
            val channel = channels[i]
            var recordingExists = false

            for (recording in recordings) {
                if (channel.programId > 0 && channel.programId == recording.eventId) {
                    val oldRecording = channel.recording
                    channel.recording = recording

                    // Do a full update only when a new recording was added or the recording
                    // state has changed which results in a different recording state icon
                    // Otherwise do not update the UI
                    if (oldRecording == null
                            || !oldRecording.error.isEqualTo(recording.error)
                            || !oldRecording.state.isEqualTo(recording.state)) {
                        notifyItemChanged(i)
                    }
                    recordingExists = true
                    break
                }
            }
            if (!recordingExists && channel.recording != null) {
                channel.recording = null
                notifyItemChanged(i)
            }
            channels[i] = channel
        }
    }

    override fun onViewAttachedToWindow(holder: ChannelViewHolder) {
        super.onViewAttachedToWindow(holder)
        holder.markAttach()
    }

    override fun onViewDetachedFromWindow(holder: ChannelViewHolder) {
        super.onViewDetachedFromWindow(holder)
        holder.markDetach()
    }

    class ChannelViewHolder(private val binding: ChannelListAdapterBinding,
                            private val viewModel: ChannelViewModel) : RecyclerView.ViewHolder(binding.root), LifecycleOwner {

        private val lifecycleRegistry = LifecycleRegistry(this)

        init {
            lifecycleRegistry.markState(Lifecycle.State.INITIALIZED)
        }

        fun markAttach() {
            lifecycleRegistry.markState(Lifecycle.State.STARTED)
        }

        fun markDetach() {
            lifecycleRegistry.markState(Lifecycle.State.DESTROYED)
        }

        override fun getLifecycle(): Lifecycle {
            return lifecycleRegistry
        }

        fun bind(channel: Channel, position: Int, isSelected: Boolean, clickCallback: RecyclerViewClickCallback) {
            binding.channel = channel
            binding.position = position
            binding.isSelected = isSelected
            binding.viewModel = viewModel
            binding.callback = clickCallback
            binding.executePendingBindings()
        }
    }
}
