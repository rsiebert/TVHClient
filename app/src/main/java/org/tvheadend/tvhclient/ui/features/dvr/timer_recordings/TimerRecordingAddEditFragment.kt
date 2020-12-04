package org.tvheadend.tvhclient.ui.features.dvr.timer_recordings

import android.os.Bundle
import android.view.*
import androidx.core.view.forEach
import androidx.lifecycle.ViewModelProvider
import com.afollestad.materialdialogs.MaterialDialog
import org.tvheadend.data.entity.Channel
import org.tvheadend.data.entity.ServerProfile
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.databinding.TimerRecordingAddEditFragmentBinding
import org.tvheadend.tvhclient.ui.base.BaseFragment
import org.tvheadend.tvhclient.ui.common.interfaces.BackPressedInterface
import org.tvheadend.tvhclient.ui.common.interfaces.HideNavigationDrawerInterface
import org.tvheadend.tvhclient.ui.common.interfaces.LayoutControlInterface
import org.tvheadend.tvhclient.ui.features.dvr.*
import org.tvheadend.tvhclient.util.extensions.afterTextChanged
import org.tvheadend.tvhclient.util.extensions.sendSnackbarMessage
import org.tvheadend.tvhclient.util.extensions.visibleOrGone
import timber.log.Timber

class TimerRecordingAddEditFragment : BaseFragment(), BackPressedInterface, RecordingConfigSelectedListener, DatePickerFragment.Listener, TimePickerFragment.Listener, HideNavigationDrawerInterface {

    private lateinit var binding: TimerRecordingAddEditFragmentBinding
    private lateinit var timerRecordingViewModel: TimerRecordingViewModel
    private lateinit var recordingProfilesList: Array<String>

    private var profile: ServerProfile? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = TimerRecordingAddEditFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        timerRecordingViewModel = ViewModelProvider(requireActivity()).get(TimerRecordingViewModel::class.java)

        if (activity is LayoutControlInterface) {
           (activity as LayoutControlInterface).forceSingleScreenLayout()
        }

        recordingProfilesList = timerRecordingViewModel.getRecordingProfileNames()
        profile = timerRecordingViewModel.getRecordingProfile()
        timerRecordingViewModel.recordingProfileNameId = getSelectedProfileId(profile, recordingProfilesList)

        if (savedInstanceState == null) {
            timerRecordingViewModel.loadRecordingByIdSync(arguments?.getString("id", "") ?: "")
        }

        updateUI()

        toolbarInterface.setSubtitle("")
        toolbarInterface.setTitle(if (timerRecordingViewModel.recording.id.isNotEmpty())
            getString(R.string.edit_recording)
        else
            getString(R.string.add_recording))
    }

    private fun updateUI() {
        val ctx = context ?: return

        binding.isEnabled.visibleOrGone(htspVersion >= 19)
        binding.isEnabled.isChecked = timerRecordingViewModel.recording.isEnabled

        binding.title.setText(timerRecordingViewModel.recording.title)
        binding.name.setText(timerRecordingViewModel.recording.name)

        binding.directoryLabel.visibleOrGone(htspVersion >= 19)
        binding.directory.visibleOrGone(htspVersion >= 19)
        binding.directory.setText(timerRecordingViewModel.recording.directory)

        binding.channelName.text = timerRecordingViewModel.recording.channelName ?: getString(R.string.all_channels)
        binding.channelName.setOnClickListener {
            // Determine if the server supports recording on all channels
            val allowRecordingOnAllChannels = htspVersion >= 21
            handleChannelListSelection(ctx, timerRecordingViewModel.getChannelList(), allowRecordingOnAllChannels, this@TimerRecordingAddEditFragment)
        }

        binding.priority.text = getPriorityName(ctx, timerRecordingViewModel.recording.priority)
        binding.priority.setOnClickListener {
            handlePrioritySelection(ctx, timerRecordingViewModel.recording.priority, this@TimerRecordingAddEditFragment)
        }

        binding.dvrConfig.visibleOrGone(recordingProfilesList.isNotEmpty())
        binding.dvrConfigLabel.visibleOrGone(recordingProfilesList.isNotEmpty())

        if (recordingProfilesList.isNotEmpty()) {
            binding.dvrConfig.text = recordingProfilesList[timerRecordingViewModel.recordingProfileNameId]
            binding.dvrConfig.setOnClickListener {
                handleRecordingProfileSelection(ctx, recordingProfilesList, timerRecordingViewModel.recordingProfileNameId, this@TimerRecordingAddEditFragment)
            }
        }

        binding.startTime.text = getTimeStringFromTimeInMillis(timerRecordingViewModel.startTimeInMillis)
        binding.startTime.setOnClickListener {
            handleTimeSelection(activity, timerRecordingViewModel.startTimeInMillis, this@TimerRecordingAddEditFragment, "startTime")
        }

        binding.stopTime.text = getTimeStringFromTimeInMillis(timerRecordingViewModel.stopTimeInMillis)
        binding.stopTime.setOnClickListener {
            handleTimeSelection(activity, timerRecordingViewModel.stopTimeInMillis, this@TimerRecordingAddEditFragment, "stopTime")
        }

        binding.daysOfWeek.text = getSelectedDaysOfWeekText(ctx, timerRecordingViewModel.recording.daysOfWeek)
        binding.daysOfWeek.setOnClickListener {
            handleDayOfWeekSelection(ctx, timerRecordingViewModel.recording.daysOfWeek, this@TimerRecordingAddEditFragment)
        }

        binding.timeEnabled.isChecked = timerRecordingViewModel.isTimeEnabled
        handleTimeEnabledClick(binding.timeEnabled.isChecked)

        binding.timeEnabled.setOnClickListener {
            handleTimeEnabledClick(binding.timeEnabled.isChecked)
        }

        binding.title.afterTextChanged { timerRecordingViewModel.recording.title = it }
        binding.name.afterTextChanged { timerRecordingViewModel.recording.name = it }
        binding.directory.afterTextChanged { timerRecordingViewModel.recording.directory = it }
        binding.isEnabled.setOnCheckedChangeListener { _, isChecked ->
            timerRecordingViewModel.recording.isEnabled = isChecked
        }
    }

    private fun handleTimeEnabledClick(checked: Boolean) {
        Timber.d("Setting time enabled ${binding.timeEnabled.isChecked}")
        timerRecordingViewModel.isTimeEnabled = checked

        binding.startTimeLabel.visibleOrGone(checked)
        binding.startTime.visibleOrGone(checked)
        binding.startTime.isEnabled = checked

        binding.stopTimeLabel.visibleOrGone(checked)
        binding.stopTime.visibleOrGone(checked)
        binding.stopTime.isEnabled = checked
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

    private fun save() {
        if (timerRecordingViewModel.recording.title.isNullOrEmpty()) {
            context?.sendSnackbarMessage(R.string.error_empty_title)
            return
        }

        val intent = timerRecordingViewModel.getIntentData(requireContext(), timerRecordingViewModel.recording)

        // Add the recording profile if available and enabled
        if (profile != null && htspVersion >= 16 && binding.dvrConfig.text.isNotEmpty()) {
            intent.putExtra("configName", binding.dvrConfig.text.toString())
        }

        // Update the recording in case the id is not empty, otherwise add a new one.
        // When adding a new recording, the id is an empty string as a default.
        if (timerRecordingViewModel.recording.id.isNotEmpty()) {
            intent.action = "updateTimerecEntry"
            intent.putExtra("id", timerRecordingViewModel.recording.id)
        } else {
            intent.action = "addTimerecEntry"
        }
        activity?.startService(intent)
        //activity?.finish()
        activity?.supportFragmentManager?.popBackStack()
    }

    private fun cancel() {
        Timber.d("cancel")
        // Show confirmation dialog to cancel
        context?.let {
            MaterialDialog(it).show {
                message(R.string.cancel_add_recording)
                positiveButton(R.string.discard) {
                    //activity?.finish()
                    Timber.d("discarding popping back stack")
                    activity?.supportFragmentManager?.popBackStack()
                }
                negativeButton(R.string.cancel) {
                    Timber.d("dismissing dialog")
                    dismiss()
                }
            }
        }
    }

    override fun onChannelSelected(channel: Channel) {
        timerRecordingViewModel.recording.channelId = channel.id
        binding.channelName.text = channel.name
    }

    override fun onPrioritySelected(which: Int) {
        timerRecordingViewModel.recording.priority = which
        context?.let {
            binding.priority.text = getPriorityName(it, timerRecordingViewModel.recording.priority)
        }
    }

    override fun onProfileSelected(which: Int) {
        binding.dvrConfig.text = recordingProfilesList[which]
        timerRecordingViewModel.recordingProfileNameId = which
    }

    override fun onTimeSelected(milliSeconds: Long, tag: String?) {
        if (tag == "startTime") {
            timerRecordingViewModel.startTimeInMillis = milliSeconds
            // If the start time is after the stop time, update the stop time with the start value
            if (milliSeconds > timerRecordingViewModel.stopTimeInMillis) {
                timerRecordingViewModel.stopTimeInMillis = milliSeconds
            }
        } else if (tag == "stopTime") {
            timerRecordingViewModel.stopTimeInMillis = milliSeconds
            // If the stop time is before the start time, update the start time with the stop value
            if (milliSeconds < timerRecordingViewModel.recording.startTimeInMillis) {
                timerRecordingViewModel.startTimeInMillis = milliSeconds
            }
        }

        binding.startTime.text = getTimeStringFromTimeInMillis(timerRecordingViewModel.startTimeInMillis)
        binding.stopTime.text = getTimeStringFromTimeInMillis(timerRecordingViewModel.stopTimeInMillis)
    }

    override fun onDateSelected(milliSeconds: Long, tag: String?) {
        // NOP
    }

    override fun onDaysSelected(selectedDays: Int) {
        timerRecordingViewModel.recording.daysOfWeek = selectedDays
        context?.let {
            binding.daysOfWeek.text = getSelectedDaysOfWeekText(it, selectedDays)
        }
    }

    override fun onBackPressed() {
        Timber.d("onBackPressed")
        cancel()
    }

    companion object {
        fun newInstance(id: String = ""): TimerRecordingAddEditFragment {
            val f = TimerRecordingAddEditFragment()
            if (id.isNotEmpty()) {
                f.arguments = Bundle().also { it.putString("id", id) }
            }
            return f
        }
    }
}
