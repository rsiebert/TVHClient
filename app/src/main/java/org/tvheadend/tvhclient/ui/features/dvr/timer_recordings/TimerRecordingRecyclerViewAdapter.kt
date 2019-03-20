package org.tvheadend.tvhclient.ui.features.dvr.timer_recordings

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable

import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.domain.entity.TimerRecording
import org.tvheadend.tvhclient.databinding.TimerRecordingListAdapterBinding
import org.tvheadend.tvhclient.ui.common.callbacks.RecyclerViewClickCallback

import java.util.ArrayList
import java.util.concurrent.CopyOnWriteArrayList
import androidx.recyclerview.widget.RecyclerView

class TimerRecordingRecyclerViewAdapter internal constructor(private val isDualPane: Boolean, private val clickCallback: RecyclerViewClickCallback, private val htspVersion: Int) : RecyclerView.Adapter<TimerRecordingRecyclerViewAdapter.TimerRecordingViewHolder>(), Filterable {
    private val recordingList = ArrayList<TimerRecording>()
    private var recordingListFiltered: MutableList<TimerRecording> = ArrayList()
    private var selectedPosition = 0

    val items: List<TimerRecording>?
        get() = recordingListFiltered

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimerRecordingViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val itemBinding = TimerRecordingListAdapterBinding.inflate(layoutInflater, parent, false)
        return TimerRecordingViewHolder(itemBinding, isDualPane)
    }

    override fun onBindViewHolder(holder: TimerRecordingViewHolder, position: Int) {
        if (recordingListFiltered.size > position) {
            val recording = recordingListFiltered[position]
            holder.bind(recording, position, selectedPosition == position, htspVersion, clickCallback)
        }
    }

    override fun onBindViewHolder(holder: TimerRecordingViewHolder, position: Int, payloads: List<Any>) {
        onBindViewHolder(holder, position)
    }

    internal fun addItems(newItems: List<TimerRecording>) {
        recordingList.clear()
        recordingListFiltered.clear()
        recordingList.addAll(newItems)
        recordingListFiltered.addAll(newItems)

        if (selectedPosition > recordingListFiltered.size) {
            selectedPosition = 0
        }
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return recordingListFiltered.size
    }

    override fun getItemViewType(position: Int): Int {
        return R.layout.timer_recording_list_adapter
    }

    fun setPosition(pos: Int) {
        notifyItemChanged(selectedPosition)
        selectedPosition = pos
        notifyItemChanged(pos)
    }

    fun getItem(position: Int): TimerRecording? {
        return if (recordingListFiltered.size > position && position >= 0) {
            recordingListFiltered[position]
        } else {
            null
        }
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(charSequence: CharSequence): Filter.FilterResults {
                val charString = charSequence.toString()
                if (charString.isEmpty()) {
                    recordingListFiltered = recordingList
                } else {
                    val filteredList = ArrayList<TimerRecording>()
                    // Iterate over the available channels. Use a copy on write
                    // array in case the channel list changes during filtering.
                    for (recording in CopyOnWriteArrayList(recordingList)) {
                        // name match condition. this might differ depending on your requirement
                        // here we are looking for a channel name match
                        val title = recording.title ?: ""
                        val name = recording.name ?: ""
                        if (title.toLowerCase().contains(charString.toLowerCase())) {
                            filteredList.add(recording)
                        } else if (name.toLowerCase().contains(charString.toLowerCase())) {
                            filteredList.add(recording)
                        }
                    }
                    recordingListFiltered = filteredList
                }

                val filterResults = Filter.FilterResults()
                filterResults.values = recordingListFiltered
                return filterResults
            }

            override fun publishResults(charSequence: CharSequence, filterResults: Filter.FilterResults) {
                recordingListFiltered = filterResults.values as ArrayList<TimerRecording>
                notifyDataSetChanged()
            }
        }
    }

    class TimerRecordingViewHolder(private val binding: TimerRecordingListAdapterBinding, private val isDualPane: Boolean) : RecyclerView.ViewHolder(binding.getRoot()) {

        fun bind(recording: TimerRecording, position: Int, isSelected: Boolean, htspVersion: Int, clickCallback: RecyclerViewClickCallback) {
            binding.setRecording(recording)
            binding.setPosition(position)
            binding.setHtspVersion(htspVersion)
            binding.setIsSelected(isSelected)
            binding.setIsDualPane(isDualPane)
            binding.setCallback(clickCallback)
            binding.executePendingBindings()
        }
    }
}
