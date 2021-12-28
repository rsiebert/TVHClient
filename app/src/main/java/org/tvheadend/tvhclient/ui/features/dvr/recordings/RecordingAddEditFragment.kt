package org.tvheadend.tvhclient.ui.features.dvr.recordings

import android.os.Bundle
import android.view.*
import androidx.core.view.forEach
import androidx.lifecycle.ViewModelProvider
import com.afollestad.materialdialogs.MaterialDialog
import org.tvheadend.data.entity.Channel
import org.tvheadend.data.entity.ServerProfile
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.databinding.RecordingAddEditFragmentBinding
import org.tvheadend.tvhclient.ui.base.BaseFragment
import org.tvheadend.tvhclient.ui.common.interfaces.BackPressedInterface
import org.tvheadend.tvhclient.ui.common.interfaces.HideNavigationDrawerInterface
import org.tvheadend.tvhclient.ui.common.interfaces.LayoutControlInterface
import org.tvheadend.tvhclient.ui.features.dvr.*
import org.tvheadend.tvhclient.util.extensions.afterTextChanged
import org.tvheadend.tvhclient.util.extensions.gone
import org.tvheadend.tvhclient.util.extensions.sendSnackbarMessage
import org.tvheadend.tvhclient.util.extensions.visibleOrGone

class RecordingAddEditFragment : BaseFragment(), BackPressedInterface, RecordingConfigSelectedListener, DatePickerFragment.Listener, TimePickerFragment.Listener, HideNavigationDrawerInterface {

    private lateinit var binding: RecordingAddEditFragmentBinding
    private lateinit var recordingViewModel: RecordingViewModel
    private lateinit var recordingProfilesList: Array<String>
    private var profile: ServerProfile? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = RecordingAddEditFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recordingViewModel = ViewModelProvider(requireActivity())[RecordingViewModel::class.java]

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

        binding.titleLabel.visibleOrGone(htspVersion >= 21)
        binding.title.visibleOrGone(htspVersion >= 21)
        binding.title.setText(recordingViewModel.recording.title)

        binding.subtitleLabel.visibleOrGone(htspVersion >= 21)
        binding.subtitle.visibleOrGone(htspVersion >= 21)
        binding.subtitle.setText(recordingViewModel.recording.subtitle)

        binding.summaryLabel.visibleOrGone(htspVersion >= 21)
        binding.summary.visibleOrGone(htspVersion >= 21)
        binding.summary.setText(recordingViewModel.recording.summary)

        binding.descriptionLabel.visibleOrGone(htspVersion >= 21)
        binding.description.visibleOrGone(htspVersion >= 21)
        binding.description.setText(recordingViewModel.recording.description)

        binding.stopTime.text = getTimeStringFromTimeInMillis(recordingViewModel.recording.stop)
        binding.stopTime.setOnClickListener { handleTimeSelection(activity, recordingViewModel.recording.stop, this@RecordingAddEditFragment, "stopTime") }

        binding.stopDate.text = getDateStringFromTimeInMillis(recordingViewModel.recording.stop)
        binding.stopDate.setOnClickListener { handleDateSelection(activity, recordingViewModel.recording.stop, this@RecordingAddEditFragment, "stopDate") }

        binding.stopExtra.setText(recordingViewModel.recording.stopExtra.toString())

        binding.channelNameLabel.visibleOrGone(!recordingViewModel.recording.isRecording)
        binding.channelName.visibleOrGone(!recordingViewModel.recording.isRecording)

        if (!recordingViewModel.recording.isRecording) {
            binding.channelName.text = recordingViewModel.recording.channelName ?: getString(R.string.all_channels)
            binding.channelName.setOnClickListener {
                // Determine if the server supports recording on all channels
                val allowRecordingOnAllChannels = htspVersion >= 21
                handleChannelListSelection(ctx, recordingViewModel.getChannelList(), allowRecordingOnAllChannels, this@RecordingAddEditFragment)
            }
        }

        binding.isEnabled.visibleOrGone(htspVersion >= 23 && !recordingViewModel.recording.isRecording)
        binding.isEnabled.isChecked = recordingViewModel.recording.isEnabled

        binding.priority.visibleOrGone(!recordingViewModel.recording.isRecording)
        binding.priority.text = getPriorityName(ctx, recordingViewModel.recording.priority)
        binding.priority.setOnClickListener { handlePrioritySelection(ctx, recordingViewModel.recording.priority, this@RecordingAddEditFragment) }

        binding.dvrConfig.visibleOrGone(!(recordingProfilesList.isEmpty() || recordingViewModel.recording.isRecording))
        binding.dvrConfigLabel.visibleOrGone(!(recordingProfilesList.isEmpty() || recordingViewModel.recording.isRecording))

        if (recordingProfilesList.isNotEmpty() && !recordingViewModel.recording.isRecording) {
            binding.dvrConfig.text = recordingProfilesList[recordingViewModel.recordingProfileNameId]
            binding.dvrConfig.setOnClickListener { handleRecordingProfileSelection(ctx, recordingProfilesList, recordingViewModel.recordingProfileNameId, this@RecordingAddEditFragment) }
        }

        if (recordingViewModel.recording.isRecording) {
            binding.startTimeLabel.gone()
            binding.startTime.gone()
            binding.startDate.gone()
            binding.startExtraLabel.gone()
            binding.startExtra.gone()
        } else {
            binding.startTime.text = getTimeStringFromTimeInMillis(recordingViewModel.recording.start)
            binding.startTime.setOnClickListener { handleTimeSelection(activity, recordingViewModel.recording.start, this@RecordingAddEditFragment, "startTime") }
            binding.startDate.text = getDateStringFromTimeInMillis(recordingViewModel.recording.start)
            binding.startDate.setOnClickListener { handleDateSelection(activity, recordingViewModel.recording.start, this@RecordingAddEditFragment, "startDate") }
            binding.startExtra.setText(recordingViewModel.recording.startExtra.toString())
        }

        binding.title.afterTextChanged { recordingViewModel.recording.title = it }
        binding.subtitle.afterTextChanged { recordingViewModel.recording.subtitle = it }
        binding.description.afterTextChanged { recordingViewModel.recording.description = it }
        binding.startExtra.afterTextChanged {
            try {
                recordingViewModel.recording.startExtra = java.lang.Long.valueOf(it)
            } catch (ex: NumberFormatException) {
                recordingViewModel.recording.startExtra = 2
            }
        }
        binding.stopExtra.afterTextChanged {
            try {
                recordingViewModel.recording.stopExtra = java.lang.Long.valueOf(it)
            } catch (ex: NumberFormatException) {
                recordingViewModel.recording.stopExtra = 2
            }
        }
        binding.isEnabled.setOnCheckedChangeListener { _, isChecked ->
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
        if (profile != null && htspVersion >= 16 && binding.dvrConfig.text.isNotEmpty()) {
            intent.putExtra("configName", binding.dvrConfig.text.toString())
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
        binding.channelName.text = channel.name
    }

    override fun onTimeSelected(milliSeconds: Long, tag: String?) {
        if (tag == "startTime") {
            recordingViewModel.recording.start = milliSeconds
            binding.startTime.text = getTimeStringFromTimeInMillis(milliSeconds)
        } else if (tag == "stopTime") {
            recordingViewModel.recording.stop = milliSeconds
            binding.stopTime.text = getTimeStringFromTimeInMillis(milliSeconds)
        }
    }

    override fun onDateSelected(milliSeconds: Long, tag: String?) {
        if (tag == "startDate") {
            recordingViewModel.recording.start = milliSeconds
            binding.startDate.text = getDateStringFromTimeInMillis(milliSeconds)
        } else if (tag == "stopDate") {
            recordingViewModel.recording.stop = milliSeconds
            binding.stopDate.text = getDateStringFromTimeInMillis(milliSeconds)
        }
    }

    override fun onPrioritySelected(which: Int) {
        context?.let {
            binding.priority.text = getPriorityName(it, which)
        }
        recordingViewModel.recording.priority = which
    }

    override fun onDaysSelected(selectedDays: Int) {
        // NOP
    }

    override fun onProfileSelected(which: Int) {
        binding.dvrConfig.text = recordingProfilesList[which]
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
