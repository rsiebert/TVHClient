package org.tvheadend.tvhclient.ui.features.dvr.recordings

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.databinding.RecordingListAdapterBinding
import org.tvheadend.tvhclient.domain.entity.Recording
import org.tvheadend.tvhclient.ui.common.callbacks.RecyclerViewClickCallback
import org.tvheadend.tvhclient.ui.features.dvr.recordings.RecordingListDiffCallback.Companion.PAYLOAD_DATA_SIZE
import timber.log.Timber
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

class RecordingRecyclerViewAdapter internal constructor(private val isDualPane: Boolean, private val clickCallback: RecyclerViewClickCallback, private val htspVersion: Int) : RecyclerView.Adapter<RecordingRecyclerViewAdapter.RecordingViewHolder>(), Filterable {
    private val recordingList = ArrayList<Recording>()
    private var recordingListFiltered: MutableList<Recording> = ArrayList()
    private var selectedPosition = 0

    val items: List<Recording>
        get() = recordingListFiltered

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordingViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val itemBinding = RecordingListAdapterBinding.inflate(layoutInflater, parent, false)
        return RecordingViewHolder(itemBinding, isDualPane)
    }

    override fun onBindViewHolder(holder: RecordingViewHolder, position: Int) {
        // NOP
    }

    override fun onBindViewHolder(holder: RecordingViewHolder, position: Int, payloads: List<Any>) {
        val recording = recordingListFiltered[position]

        if (payloads.isEmpty()) {
            Timber.d("Recording '${recording.title}' has changed, doing a full update")
            holder.bind(recording, position, selectedPosition == position, htspVersion, clickCallback)
        } else {
            for (payload in payloads) {
                if (payload == PAYLOAD_DATA_SIZE) {
                    // Update only the data size and errors
                    Timber.d("Recording '${recording.title}' has changed, doing a partial update")
                    holder.bind(recording, position, selectedPosition == position, htspVersion, clickCallback)
                }
            }
        }
    }

    internal fun addItems(newItems: List<Recording>) {
        val oldItems = ArrayList(recordingListFiltered)
        val diffResult = DiffUtil.calculateDiff(RecordingListDiffCallback(oldItems, newItems))

        recordingList.clear()
        recordingListFiltered.clear()
        recordingList.addAll(newItems)
        recordingListFiltered.addAll(newItems)
        diffResult.dispatchUpdatesTo(this)

        if (selectedPosition > recordingListFiltered.size) {
            selectedPosition = 0
        }
    }

    override fun getItemCount(): Int {
        return recordingListFiltered.size
    }

    override fun getItemViewType(position: Int): Int {
        return R.layout.recording_list_adapter
    }

    fun setPosition(pos: Int) {
        notifyItemChanged(selectedPosition)
        selectedPosition = pos
        notifyItemChanged(pos)
    }

    fun getItem(position: Int): Recording? {
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
                    val filteredList = ArrayList<Recording>()
                    // Iterate over the available channels. Use a copy on write
                    // array in case the channel list changes during filtering.
                    for (recording in CopyOnWriteArrayList(recordingList)) {
                        // name match condition. this might differ depending on your requirement
                        // here we are looking for a channel name match
                        val title = recording.title ?: ""
                        val subtitle = recording.subtitle ?: ""
                        if (title.toLowerCase().contains(charString.toLowerCase())) {
                            filteredList.add(recording)
                        } else if (subtitle.toLowerCase().contains(charString.toLowerCase())) {
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
                recordingListFiltered = filterResults.values as ArrayList<Recording>
                notifyDataSetChanged()
            }
        }
    }

    class RecordingViewHolder(private val binding: RecordingListAdapterBinding, private val isDualPane: Boolean) : RecyclerView.ViewHolder(binding.getRoot()) {

        fun bind(recording: Recording, position: Int, isSelected: Boolean, htspVersion: Int, clickCallback: RecyclerViewClickCallback) {
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
