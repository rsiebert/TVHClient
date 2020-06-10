package org.tvheadend.tvhclient.ui.features.dvr.recordings

import android.os.Bundle
import android.view.*
import androidx.core.view.forEach
import androidx.lifecycle.ViewModelProviders
import com.afollestad.materialdialogs.MaterialDialog
import kotlinx.android.synthetic.main.recording_add_edit_fragment.*
import org.tvheadend.data.entity.Channel
import org.tvheadend.data.entity.ServerProfile
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.ui.base.BaseFragment
import org.tvheadend.tvhclient.ui.common.interfaces.HideNavigationDrawerInterface
import org.tvheadend.tvhclient.ui.common.interfaces.BackPressedInterface
import org.tvheadend.tvhclient.ui.common.interfaces.LayoutControlInterface
import org.tvheadend.tvhclient.ui.features.dvr.*
import org.tvheadend.tvhclient.util.extensions.afterTextChanged
import org.tvheadend.tvhclient.util.extensions.gone
import org.tvheadend.tvhclient.util.extensions.sendSnackbarMessage
import org.tvheadend.tvhclient.util.extensions.visibleOrGone

class RecordingAddEditFragment : BaseFragment(), BackPressedInterface, RecordingConfigSelectedListener, DatePickerFragment.Listener, TimePickerFragment.Listener, HideNavigationDrawerInterface {

    private lateinit var recordingViewModel: RecordingViewModel
    private lateinit var recordingProfilesList: Array<String>
    private var profile: ServerProfile? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.recording_add_edit_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        recordingViewModel = ViewModelProviders.of(requireActivity()).get(RecordingViewModel::class.java)

        if (activity is LayoutControlInterface) {
            (activity as LayoutControlInterface).forceSingleScreenLayout()
        }

        recordingProfilesList = recordingViewModel.getRecordingProfileNames()
        profile = recordingViewModel.getRecordingProfile()
        recordingViewModel.recordingProfileNameId = getSelectedProfileId(profile, recordingProfilesList)

        if (savedInstanceState == null) {
            recordingViewModel.loadRecordingByIdSync(arguments?.getInt("id", 0) ?: 0)
        }

        updateUI()

        toolbarInterface.setSubtitle("")
        toolbarInterface.setTitle(if (id > 0)
            getString(R.string.edit_recording)
        else
            getString(R.string.add_recording))
    }

    private fun updateUI() {
        val ctx = context ?: return

        title_label.visibleOrGone(htspVersion >= 21)
        title.visibleOrGone(htspVersion >= 21)
        title.setText(recordingViewModel.recording.title)

        subtitle_label.visibleOrGone(htspVersion >= 21)
        subtitle.visibleOrGone(htspVersion >= 21)
        subtitle.setText(recordingViewModel.recording.subtitle)

        summary_label.visibleOrGone(htspVersion >= 21)
        summary.visibleOrGone(htspVersion >= 21)
        summary.setText(recordingViewModel.recording.summary)

        description_label.visibleOrGone(htspVersion >= 21)
        description.visibleOrGone(htspVersion >= 21)
        description.setText(recordingViewModel.recording.description)

        stop_time.text = getTimeStringFromTimeInMillis(recordingViewModel.recording.stop)
        stop_time.setOnClickListener { handleTimeSelection(activity, recordingViewModel.recording.stop, this@RecordingAddEditFragment, "stopTime") }

        stop_date.text = getDateStringFromTimeInMillis(recordingViewModel.recording.stop)
        stop_date.setOnClickListener { handleDateSelection(activity, recordingViewModel.recording.stop, this@RecordingAddEditFragment, "stopDate") }

        stop_extra.setText(recordingViewModel.recording.stopExtra.toString())

        channel_name_label.visibleOrGone(!recordingViewModel.recording.isRecording)
        channel_name.visibleOrGone(!recordingViewModel.recording.isRecording)

        if (!recordingViewModel.recording.isRecording) {
            channel_name.text = recordingViewModel.recording.channelName ?: getString(R.string.all_channels)
            channel_name.setOnClickListener {
                // Determine if the server supports recording on all channels
                val allowRecordingOnAllChannels = htspVersion >= 21
                handleChannelListSelection(ctx, recordingViewModel.getChannelList(), allowRecordingOnAllChannels, this@RecordingAddEditFragment)
            }
        }

        is_enabled.visibleOrGone(htspVersion >= 23 && !recordingViewModel.recording.isRecording)
        is_enabled.isChecked = recordingViewModel.recording.isEnabled

        priority.visibleOrGone(!recordingViewModel.recording.isRecording)
        priority.text = getPriorityName(ctx, recordingViewModel.recording.priority)
        priority.setOnClickListener { handlePrioritySelection(ctx, recordingViewModel.recording.priority, this@RecordingAddEditFragment) }

        dvr_config.visibleOrGone(!(recordingProfilesList.isEmpty() || recordingViewModel.recording.isRecording))
        dvr_config_label.visibleOrGone(!(recordingProfilesList.isEmpty() || recordingViewModel.recording.isRecording))

        if (recordingProfilesList.isNotEmpty() && !recordingViewModel.recording.isRecording) {
            dvr_config.text = recordingProfilesList[recordingViewModel.recordingProfileNameId]
            dvr_config.setOnClickListener { handleRecordingProfileSelection(ctx, recordingProfilesList, recordingViewModel.recordingProfileNameId, this@RecordingAddEditFragment) }
        }

        if (recordingViewModel.recording.isRecording) {
            start_time_label.gone()
            start_time.gone()
            start_date.gone()
            start_extra_label.gone()
            start_extra.gone()
        } else {
            start_time.text = getTimeStringFromTimeInMillis(recordingViewModel.recording.start)
            start_time.setOnClickListener { handleTimeSelection(activity, recordingViewModel.recording.start, this@RecordingAddEditFragment, "startTime") }
            start_date.text = getDateStringFromTimeInMillis(recordingViewModel.recording.start)
            start_date.setOnClickListener { handleDateSelection(activity, recordingViewModel.recording.start, this@RecordingAddEditFragment, "startDate") }
            start_extra.setText(recordingViewModel.recording.startExtra.toString())
        }

        title.afterTextChanged { recordingViewModel.recording.title = it }
        subtitle.afterTextChanged { recordingViewModel.recording.subtitle = it }
        description.afterTextChanged { recordingViewModel.recording.description = it }
        start_extra.afterTextChanged {
            try {
                recordingViewModel.recording.startExtra = java.lang.Long.valueOf(it)
            } catch (ex: NumberFormatException) {
                recordingViewModel.recording.startExtra = 2
            }
        }
        stop_extra.afterTextChanged {
            try {
                recordingViewModel.recording.stopExtra = java.lang.Long.valueOf(it)
            } catch (ex: NumberFormatException) {
                recordingViewModel.recording.stopExtra = 2
            }
        }
        is_enabled.setOnCheckedChangeListener { _, isChecked ->
            recordingViewModel.recording.isEnabled = isChecked
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menu.forEach { it.isVisible = false }
        menu.findItem(R.id.menu_save)?.isVisible = true
        menu.findItem(R.id.menu_cancel)?.isVisible = true
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.save_cancel_options_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                cancel()
                true
            }
            R.id.menu_save -> {
                save()
                true
            }
            R.id.menu_cancel -> {
                cancel()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Checks certain given values for plausibility and if everything is fine
     * creates the intent that will be passed to the service to save the newly
     * created recordingViewModel.recording.
     */
    private fun save() {
        if (recordingViewModel.recording.title.isNullOrEmpty() && htspVersion >= 21) {
            context?.sendSnackbarMessage(R.string.error_empty_title)
            return
        }
        if (recordingViewModel.recording.channelId == 0 && htspVersion < 21) {
            context?.sendSnackbarMessage(R.string.error_no_channel_selected)
            return
        }

        if (recordingViewModel.recording.start >= recordingViewModel.recording.stop) {
            context?.sendSnackbarMessage(R.string.error_start_time_past_stop_time)
            return
        }

        val intent = recordingViewModel.getIntentData(requireContext(), recordingViewModel.recording)
        if (profile != null && htspVersion >= 16 && dvr_config.text.isNotEmpty()) {
            intent.putExtra("configName", dvr_config.text.toString())
        }

        if (recordingViewModel.recording.id > 0) {
            intent.action = "updateDvrEntry"
            intent.putExtra("id", recordingViewModel.recording.id)
        } else {
            intent.action = "addDvrEntry"
        }
        activity?.startService(intent)
        activity?.supportFragmentManager?.popBackStack()
    }

    /**
     * Asks the user to confirm canceling the current activity. If no is
     * chosen the user can continue to add or edit the recordingViewModel.recording. Otherwise
     * the input will be discarded and the activity will be closed.
     */
    private fun cancel() {
        // Show confirmation dialog to cancel
        context?.let {
            MaterialDialog(it).show {
                message(R.string.cancel_edit_recording)
                positiveButton(R.string.discard) { activity?.supportFragmentManager?.popBackStack() }
                negativeButton(R.string.cancel) { dismiss() }
            }
        }
    }

    override fun onChannelSelected(channel: Channel) {
        recordingViewModel.recording.channelId = channel.id
        channel_name.text = channel.name
    }

    override fun onTimeSelected(milliSeconds: Long, tag: String?) {
        if (tag == "startTime") {
            recordingViewModel.recording.start = milliSeconds
            start_time.text = getTimeStringFromTimeInMillis(milliSeconds)
        } else if (tag == "stopTime") {
            recordingViewModel.recording.stop = milliSeconds
            stop_time.text = getTimeStringFromTimeInMillis(milliSeconds)
        }
    }

    override fun onDateSelected(milliSeconds: Long, tag: String?) {
        if (tag == "startDate") {
            recordingViewModel.recording.start = milliSeconds
            start_date.text = getDateStringFromTimeInMillis(milliSeconds)
        } else if (tag == "stopDate") {
            recordingViewModel.recording.stop = milliSeconds
            stop_date.text = getDateStringFromTimeInMillis(milliSeconds)
        }
    }

    override fun onPrioritySelected(which: Int) {
        context?.let {
            priority.text = getPriorityName(it, which)
        }
        recordingViewModel.recording.priority = which
    }

    override fun onDaysSelected(selectedDays: Int) {
        // NOP
    }

    override fun onProfileSelected(which: Int) {
        dvr_config.text = recordingProfilesList[which]
        recordingViewModel.recordingProfileNameId = which
    }

    override fun onBackPressed() {
        cancel()
    }

    companion object {
        fun newInstance(id: Int = 0): RecordingAddEditFragment {
            val f = RecordingAddEditFragment()
            if (id > 0) {
                f.arguments = Bundle().also { it.putInt("id", id) }
            }
            return f
        }
    }
}
