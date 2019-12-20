package org.tvheadend.tvhclient.ui.features.dvr.series_recordings

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import org.tvheadend.data.entity.SeriesRecording
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.databinding.SeriesRecordingListAdapterBinding
import org.tvheadend.tvhclient.ui.common.interfaces.RecyclerViewClickInterface
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

class SeriesRecordingRecyclerViewAdapter internal constructor(private val isDualPane: Boolean, private val clickCallback: RecyclerViewClickInterface, private val htspVersion: Int) : RecyclerView.Adapter<SeriesRecordingRecyclerViewAdapter.SeriesRecordingViewHolder>(), Filterable {

    private val recordingList = ArrayList<SeriesRecording>()
    private var recordingListFiltered: MutableList<SeriesRecording> = ArrayList()
    private var selectedPosition = 0

    val items: List<SeriesRecording>
        get() = recordingListFiltered

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SeriesRecordingViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val itemBinding = SeriesRecordingListAdapterBinding.inflate(layoutInflater, parent, false)
        return SeriesRecordingViewHolder(itemBinding, isDualPane)
    }

    override fun onBindViewHolder(holder: SeriesRecordingViewHolder, position: Int) {
        if (recordingListFiltered.size > position) {
            val recording = recordingListFiltered[position]
            holder.bind(recording, position, selectedPosition == position, htspVersion, clickCallback)
        }
    }

    override fun onBindViewHolder(holder: SeriesRecordingViewHolder, position: Int, payloads: List<Any>) {
        onBindViewHolder(holder, position)
    }

    internal fun addItems(newItems: List<SeriesRecording>) {
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
        return R.layout.series_recording_list_adapter
    }

    fun setPosition(pos: Int) {
        notifyItemChanged(selectedPosition)
        selectedPosition = pos
        notifyItemChanged(pos)
    }

    fun getItem(position: Int): SeriesRecording? {
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
                val filteredList: MutableList<SeriesRecording> = ArrayList()
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
                recordingListFiltered.addAll(filterResults.values as ArrayList<SeriesRecording>)
                notifyDataSetChanged()
            }
        }
    }

    class SeriesRecordingViewHolder(private val binding: SeriesRecordingListAdapterBinding, private val isDualPane: Boolean) : RecyclerView.ViewHolder(binding.root) {

        fun bind(recording: SeriesRecording, position: Int, isSelected: Boolean, htspVersion: Int, clickCallback: RecyclerViewClickInterface) {
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
