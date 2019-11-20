package org.tvheadend.tvhclient.ui.features.dvr.timer_recordings

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.databinding.TimerRecordingListAdapterBinding
import org.tvheadend.data.entity.TimerRecording
import org.tvheadend.tvhclient.ui.common.interfaces.RecyclerViewClickInterface
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

class TimerRecordingRecyclerViewAdapter internal constructor(private val isDualPane: Boolean, private val clickCallback: RecyclerViewClickInterface, private val htspVersion: Int) : RecyclerView.Adapter<TimerRecordingRecyclerViewAdapter.TimerRecordingViewHolder>(), Filterable {

    private val recordingList = ArrayList<TimerRecording>()
    private var recordingListFiltered: MutableList<TimerRecording> = ArrayList()
    private var selectedPosition = 0

    val items: List<TimerRecording>
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
            override fun performFiltering(charSequence: CharSequence): FilterResults {
                val charString = charSequence.toString()
                val filteredList: MutableList<TimerRecording> = ArrayList()
                if (charString.isNotEmpty()) {
                    for (recording in CopyOnWriteArrayList(recordingList)) {
                        val title = recording.title ?: ""
                        val name = recording.name ?: ""
                        when {
                            title.toLowerCase(Locale.getDefault()).contains(charString.toLowerCase(Locale.getDefault())) -> filteredList.add(recording)
                            name.toLowerCase(Locale.getDefault()).contains(charString.toLowerCase(Locale.getDefault())) -> filteredList.add(recording)
                        }
                    }
                } else {
                    filteredList.addAll(recordingList)
                }

                val filterResults = FilterResults()
                filterResults.values = filteredList
                return filterResults
            }

            override fun publishResults(charSequence: CharSequence, filterResults: FilterResults) {
                recordingListFiltered.clear()
                @Suppress("UNCHECKED_CAST")
                recordingListFiltered.addAll(filterResults.values as ArrayList<TimerRecording>)
                notifyDataSetChanged()
            }
        }
    }

    class TimerRecordingViewHolder(private val binding: TimerRecordingListAdapterBinding, private val isDualPane: Boolean) : RecyclerView.ViewHolder(binding.root) {

        fun bind(recording: TimerRecording, position: Int, isSelected: Boolean, htspVersion: Int, clickCallback: RecyclerViewClickInterface) {
            binding.recording = recording
            binding.position = position
            binding.htspVersion = htspVersion
            binding.isSelected = isSelected
            binding.isDualPane = isDualPane
            binding.callback = clickCallback
            binding.executePendingBindings()
        }
    }
}
