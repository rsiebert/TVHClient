package org.tvheadend.tvhclient.ui.features.epg

import android.content.ContextWrapper
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.databinding.EpgProgramItemAdapterBinding
import org.tvheadend.tvhclient.domain.entity.EpgProgram
import org.tvheadend.tvhclient.domain.entity.Recording
import org.tvheadend.tvhclient.ui.common.callbacks.RecyclerViewClickCallback
import org.tvheadend.tvhclient.ui.features.programs.ProgramDetailsFragment
import org.tvheadend.tvhclient.util.isEqualTo
import java.util.*

internal class EpgProgramListRecyclerViewAdapter(private val pixelsPerMinute: Float, private val fragmentStartTime: Long, private val fragmentStopTime: Long) : RecyclerView.Adapter<EpgProgramListRecyclerViewAdapter.EpgProgramListViewHolder>(), RecyclerViewClickCallback {

    private val programList = ArrayList<EpgProgram>()
    private val recordingList = ArrayList<Recording>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EpgProgramListViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val itemBinding = EpgProgramItemAdapterBinding.inflate(layoutInflater, parent, false)

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(parent.context)
        val showProgramSubtitle = sharedPreferences.getBoolean("program_subtitle_enabled", parent.context.resources.getBoolean(R.bool.pref_default_program_subtitle_enabled))
        val showGenreColors = sharedPreferences.getBoolean("genre_colors_for_program_guide_enabled", parent.context.resources.getBoolean(R.bool.pref_default_genre_colors_for_program_guide_enabled))

        return EpgProgramListViewHolder(itemBinding, showProgramSubtitle, showGenreColors, this)
    }

    override fun onBindViewHolder(holder: EpgProgramListViewHolder, position: Int) {
        if (programList.size > position) {
            val program = programList[position]

            val startTime = if (program.start < fragmentStartTime) fragmentStartTime else program.start
            val stopTime = if (program.stop > fragmentStopTime) fragmentStopTime else program.stop
            val layoutWidth = ((stopTime - startTime) / 1000 / 60 * pixelsPerMinute).toInt()

            holder.bind(program, position, layoutWidth)
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
        return R.layout.epg_program_item_adapter
    }

    private fun getItem(position: Int): EpgProgram? {
        return if (programList.size > position && position >= 0) {
            programList[position]
        } else {
            null
        }
    }

    /**
     * Returns the activity from the view context so that
     * stuff like the fragment manager can be accessed
     *
     * @param view The view to retrieve the activity from
     * @return Activity or null if none was found
     */
    private fun getActivity(view: View): AppCompatActivity? {
        var context = view.context
        while (context is ContextWrapper) {
            if (context is AppCompatActivity) {
                return context
            }
            context = context.baseContext
        }
        return null
    }

    override fun onClick(view: View, position: Int) {
        val activity = getActivity(view)
        val program = getItem(position)
        if (program == null || activity == null) {
            return
        }

        val fragment = ProgramDetailsFragment.newInstance(program.eventId, program.channelId)
        val ft = activity.supportFragmentManager.beginTransaction()
        ft.replace(R.id.main, fragment)
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
        ft.addToBackStack(null)
        ft.commit()
    }

    override fun onLongClick(view: View, position: Int): Boolean {
        val activity = getActivity(view)
        val program = getItem(position)
        if (program == null || activity == null) {
            return false
        }

        val fragment = activity.supportFragmentManager.findFragmentById(R.id.main)
        if (fragment is ProgramGuideFragment
                && fragment.isAdded
                && fragment.isResumed) {
            fragment.showPopupMenu(view, program)
        }
        return true
    }

    internal class EpgProgramListViewHolder(private val binding: EpgProgramItemAdapterBinding, private val showProgramSubtitle: Boolean, private val showGenreColors: Boolean, private val clickCallback: RecyclerViewClickCallback) : RecyclerView.ViewHolder(binding.root) {

        fun bind(program: EpgProgram, position: Int, layoutWidth: Int) {
            binding.program = program
            binding.position = position
            binding.layoutWidth = layoutWidth
            binding.showGenreColor = showGenreColors
            binding.showProgramSubtitle = showProgramSubtitle
            binding.callback = clickCallback
            binding.executePendingBindings()
        }
    }
}
