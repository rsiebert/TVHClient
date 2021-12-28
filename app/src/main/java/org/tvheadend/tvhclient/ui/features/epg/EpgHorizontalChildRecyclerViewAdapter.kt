package org.tvheadend.tvhclient.ui.features.epg

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import org.tvheadend.data.entity.EpgProgram
import org.tvheadend.data.entity.Recording
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.databinding.EpgHorizontalChildRecyclerviewAdapterBinding
import org.tvheadend.tvhclient.util.extensions.isEqualTo
import java.util.*

internal class EpgHorizontalChildRecyclerViewAdapter(private val viewModel: EpgViewModel, private val fragmentId: Int, private val lifecycleOwner: LifecycleOwner) : RecyclerView.Adapter<EpgHorizontalChildRecyclerViewAdapter.EpgProgramListViewHolder>() {

    private val programList = ArrayList<EpgProgram>()
    private val recordingList = ArrayList<Recording>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EpgProgramListViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val itemBinding = EpgHorizontalChildRecyclerviewAdapterBinding.inflate(layoutInflater, parent, false)
        val viewHolder = EpgProgramListViewHolder(itemBinding, viewModel)
        itemBinding.lifecycleOwner = lifecycleOwner
        return viewHolder
    }

    override fun onBindViewHolder(holder: EpgProgramListViewHolder, position: Int) {
        if (programList.size > position) {
            val program = programList[position]

            val startTime = if (program.start < viewModel.getStartTime(fragmentId)) viewModel.getStartTime(fragmentId) else program.start
            val stopTime = if (program.stop > viewModel.getEndTime(fragmentId)) viewModel.getEndTime(fragmentId) else program.stop
            val layoutWidth = ((stopTime - startTime) / 1000 / 60 * viewModel.pixelsPerMinute).toInt()

            holder.bind(program, layoutWidth)
        }
    }

    override fun onBindViewHolder(holder: EpgProgramListViewHolder, position: Int, payloads: List<Any>) {
        onBindViewHolder(holder, position)
    }

    fun addItems(newItems: MutableList<EpgProgram>) {
        updateRecordingState(newItems, recordingList)

        val oldItems = ArrayList(programList)
        val diffResult = DiffUtil.calculateDiff(EpgProgramListDiffCallback(oldItems, newItems))

        programList.clear()
        programList.addAll(newItems)
        diffResult.dispatchUpdatesTo(this)
    }

    fun addRecordings(list: List<Recording>) {
        recordingList.clear()
        recordingList.addAll(list)
        updateRecordingState(programList, recordingList)
    }

    private fun updateRecordingState(programs: MutableList<EpgProgram>, recordings: List<Recording>) {
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

    override fun getItemCount(): Int {
        return programList.size
    }

    override fun getItemViewType(position: Int): Int {
        return R.layout.epg_horizontal_child_recyclerview_adapter
    }

    internal class EpgProgramListViewHolder(private val binding: EpgHorizontalChildRecyclerviewAdapterBinding,
                                            private val viewModel: EpgViewModel) : RecyclerView.ViewHolder(binding.root) {
        fun bind(program: EpgProgram, layoutWidth: Int) {
            binding.program = program
            binding.layoutWidth = layoutWidth
            binding.viewModel = viewModel
            binding.executePendingBindings()
        }
    }
}
