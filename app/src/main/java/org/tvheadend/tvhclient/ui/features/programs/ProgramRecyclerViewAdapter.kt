package org.tvheadend.tvhclient.ui.features.programs

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import org.tvheadend.data.entity.ProgramInterface
import org.tvheadend.data.entity.Recording
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.databinding.ProgramListAdapterBinding
import org.tvheadend.tvhclient.ui.common.interfaces.RecyclerViewClickInterface
import org.tvheadend.tvhclient.util.extensions.isEqualTo
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

class ProgramRecyclerViewAdapter internal constructor(private val viewModel: ProgramViewModel, private val clickCallback: RecyclerViewClickInterface, private val onLastProgramVisibleListener: LastProgramVisibleListener, private val lifecycleOwner: LifecycleOwner) : RecyclerView.Adapter<ProgramRecyclerViewAdapter.ProgramViewHolder>(), Filterable {

    private val programList = ArrayList<ProgramInterface>()
    private var programListFiltered: MutableList<ProgramInterface> = ArrayList()
    private val recordingList = ArrayList<Recording>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProgramViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val itemBinding = ProgramListAdapterBinding.inflate(layoutInflater, parent, false)
        val viewHolder = ProgramViewHolder(itemBinding, viewModel)
        itemBinding.lifecycleOwner = lifecycleOwner
        return viewHolder
    }

    override fun onBindViewHolder(holder: ProgramViewHolder, position: Int) {
        if (programListFiltered.size > position) {
            val program = programListFiltered[position]
            holder.bind(program, position, clickCallback)
            if (position == programList.size - 1) {
                onLastProgramVisibleListener.onLastProgramVisible(position)
            }
        }
    }

    override fun onBindViewHolder(holder: ProgramViewHolder, position: Int, payloads: List<Any>) {
        onBindViewHolder(holder, position)
    }

    internal fun addItems(newItems: MutableList<ProgramInterface>) {
        updateRecordingState(newItems, recordingList)

        val oldItems = ArrayList(programListFiltered)
        val diffResult = DiffUtil.calculateDiff(ProgramListDiffCallback(oldItems, newItems))

        programList.clear()
        programListFiltered.clear()
        programList.addAll(newItems)
        programListFiltered.addAll(newItems)
        diffResult.dispatchUpdatesTo(this)
    }

    override fun getItemCount(): Int {
        return programListFiltered.size
    }

    override fun getItemViewType(position: Int): Int {
        return R.layout.program_list_adapter
    }

    fun getItem(position: Int): ProgramInterface? {
        return if (programListFiltered.size > position && position >= 0) {
            programListFiltered[position]
        } else {
            null
        }
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(charSequence: CharSequence): FilterResults {
                val charString = charSequence.toString()
                val filteredList: MutableList<ProgramInterface> = ArrayList()
                if (charString.isNotEmpty()) {
                    for (program in CopyOnWriteArrayList(programList)) {
                        val title = program.title ?: ""
                        when {
                            title.lowercase().contains(charString.lowercase()) -> filteredList.add(program)
                        }
                    }
                } else {
                    filteredList.addAll(programList)
                }

                val filterResults = FilterResults()
                filterResults.values = filteredList
                return filterResults
            }

            override fun publishResults(charSequence: CharSequence, filterResults: FilterResults) {
                programListFiltered.clear()
                @Suppress("UNCHECKED_CAST")
                programListFiltered.addAll(filterResults.values as ArrayList<ProgramInterface>)
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
        updateRecordingState(programListFiltered, recordingList)
    }

    private fun updateRecordingState(programs: MutableList<ProgramInterface>, recordings: List<Recording>) {
        for (i in programs.indices) {
            val program = programs[i]
            var recordingExists = false

            for (recording in recordings) {
                if (program.eventId > 0 && program.eventId == recording.eventId) {
                    val oldRecording = program.recording
                    program.recording = recording

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
            if (!recordingExists && program.recording != null) {
                program.recording = null
                notifyItemChanged(i)
            }
            programs[i] = program
        }
    }

    class ProgramViewHolder(private val binding: ProgramListAdapterBinding, private val viewModel: ProgramViewModel) : RecyclerView.ViewHolder(binding.root) {

        fun bind(program: ProgramInterface, position: Int, clickCallback: RecyclerViewClickInterface) {
            binding.program = program
            binding.position = position
            binding.viewModel = viewModel
            binding.callback = clickCallback
            binding.executePendingBindings()
        }
    }
}
