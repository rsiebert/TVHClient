package org.tvheadend.tvhclient.ui.features.dvr.series_recordings

import android.os.Bundle
import android.view.*
import androidx.core.view.forEach
import androidx.lifecycle.ViewModelProvider
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import org.tvheadend.data.entity.Channel
import org.tvheadend.data.entity.ServerProfile
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.databinding.SeriesRecordingAddEditFragmentBinding
import org.tvheadend.tvhclient.ui.base.BaseFragment
import org.tvheadend.tvhclient.ui.common.interfaces.BackPressedInterface
import org.tvheadend.tvhclient.ui.common.interfaces.HideNavigationDrawerInterface
import org.tvheadend.tvhclient.ui.common.interfaces.LayoutControlInterface
import org.tvheadend.tvhclient.ui.features.dvr.*
import org.tvheadend.tvhclient.util.extensions.afterTextChanged
import org.tvheadend.tvhclient.util.extensions.sendSnackbarMessage
import org.tvheadend.tvhclient.util.extensions.visibleOrGone
import timber.log.Timber

class SeriesRecordingAddEditFragment : BaseFragment(), BackPressedInterface, RecordingConfigSelectedListener, DatePickerFragment.Listener, TimePickerFragment.Listener, HideNavigationDrawerInterface {

    private lateinit var binding: SeriesRecordingAddEditFragmentBinding
    private lateinit var seriesRecordingViewModel: SeriesRecordingViewModel
    private lateinit var recordingProfilesList: Array<String>
    private var profile: ServerProfile? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = SeriesRecordingAddEditFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        seriesRecordingViewModel = ViewModelProvider(requireActivity()).get(SeriesRecordingViewModel::class.java)

        if (activity is LayoutControlInterface) {
            (activity as LayoutControlInterface).forceSingleScreenLayout()
        }

        recordingProfilesList = seriesRecordingViewModel.getRecordingProfileNames()
        profile = seriesRecordingViewModel.getRecordingProfile()
        seriesRecordingViewModel.recordingProfileNameId = getSelectedProfileId(profile, recordingProfilesList)

        if (savedInstanceState == null) {
            seriesRecordingViewModel.loadRecordingByIdSync(arguments?.getString("id", "") ?: "")
        }

        updateUI()

        toolbarInterface.setSubtitle("")
        toolbarInterface.setTitle(if (seriesRecordingViewModel.recording.id.isNotEmpty())
            getString(R.string.edit_recording)
        else
            getString(R.string.add_recording))
    }

    private fun updateUI() {
        val ctx = context ?: return

        binding.isEnabled.visibleOrGone(htspVersion >= 19)
        binding.isEnabled.isChecked = seriesRecordingViewModel.recording.isEnabled

        binding.title.setText(seriesRecordingViewModel.recording.title)
        binding.name.setText(seriesRecordingViewModel.recording.name)

        binding.directoryLabel.visibleOrGone(htspVersion >= 19)
        binding.directory.visibleOrGone(htspVersion >= 19)
        binding.directory.setText(seriesRecordingViewModel.recording.directory)

        binding.channelName.text = seriesRecordingViewModel.recording.channelName
                ?: getString(R.string.all_channels)
        binding.channelName.setOnClickListener {
            // Determine if the server supports recording on all channels
            val allowRecordingOnAllChannels = htspVersion >= 21
            handleChannelListSelection(ctx, seriesRecordingViewModel.getChannelList(), allowRecordingOnAllChannels, this@SeriesRecordingAddEditFragment)
        }

        binding.priority.text = getPriorityName(ctx, seriesRecordingViewModel.recording.priority)
        binding.priority.setOnClickListener {
            handlePrioritySelection(ctx, seriesRecordingViewModel.recording.priority, this@SeriesRecordingAddEditFragment)
        }

        binding.dvrConfig.visibleOrGone(recordingProfilesList.isNotEmpty())
        binding.dvrConfigLabel.visibleOrGone(recordingProfilesList.isNotEmpty())

        if (recordingProfilesList.isNotEmpty()) {
            binding.dvrConfig.text = recordingProfilesList[seriesRecordingViewModel.recordingProfileNameId]
            binding.dvrConfig.setOnClickListener {
                handleRecordingProfileSelection(ctx, recordingProfilesList, seriesRecordingViewModel.recordingProfileNameId, this)
            }
        }

        binding.startTime.text = getTimeStringFromTimeInMillis(seriesRecordingViewModel.startTimeInMillis)
        binding.startTime.setOnClickListener {
            handleTimeSelection(activity, seriesRecordingViewModel.startTimeInMillis, this@SeriesRecordingAddEditFragment, "startTime")
        }

        binding.startWindowTime.text = getTimeStringFromTimeInMillis(seriesRecordingViewModel.startWindowTimeInMillis)
        binding.startWindowTime.setOnClickListener {
            handleTimeSelection(activity, seriesRecordingViewModel.startWindowTimeInMillis, this@SeriesRecordingAddEditFragment, "startWindowTime")
        }

        binding.startExtra.setText(seriesRecordingViewModel.recording.startExtra.toString())
        binding.stopExtra.setText(seriesRecordingViewModel.recording.stopExtra.toString())

        binding.daysOfWeek.text = getSelectedDaysOfWeekText(ctx, seriesRecordingViewModel.recording.daysOfWeek)
        binding.daysOfWeek.setOnClickListener {
            handleDayOfWeekSelection(ctx, seriesRecordingViewModel.recording.daysOfWeek, this@SeriesRecordingAddEditFragment)
        }

        binding.minimumDuration.setText(if (seriesRecordingViewModel.recording.minDuration > 0) (seriesRecordingViewModel.recording.minDuration / 60).toString() else getString(R.string.duration_sum))
        binding.maximumDuration.setText(if (seriesRecordingViewModel.recording.maxDuration > 0) (seriesRecordingViewModel.recording.maxDuration / 60).toString() else getString(R.string.duration_sum))

        binding.timeEnabled.isChecked = seriesRecordingViewModel.isTimeEnabled
        handleTimeEnabledClick(binding.timeEnabled.isChecked)

        binding.timeEnabled.setOnClickListener {
            handleTimeEnabledClick(binding.timeEnabled.isChecked)
        }

        binding.duplicateDetectionLabel.visibleOrGone(htspVersion >= 20)
        binding.duplicateDetection.visibleOrGone(htspVersion >= 20)
        binding.duplicateDetection.text = seriesRecordingViewModel.duplicateDetectionList[seriesRecordingViewModel.recording.dupDetect]

        binding.duplicateDetection.setOnClickListener {
            handleDuplicateDetectionSelection(seriesRecordingViewModel.duplicateDetectionList, seriesRecordingViewModel.recording.dupDetect)
        }

        binding.title.afterTextChanged { seriesRecordingViewModel.recording.title = it }
        binding.name.afterTextChanged { seriesRecordingViewModel.recording.name = it }
        binding.directory.afterTextChanged { seriesRecordingViewModel.recording.directory = it }
        binding.minimumDuration.afterTextChanged {
            try {
                seriesRecordingViewModel.recording.minDuration = Integer.valueOf(it)
            } catch (ex: NumberFormatException) {
                seriesRecordingViewModel.recording.minDuration = 0
            }
        }
        binding.maximumDuration.afterTextChanged {
            try {
                seriesRecordingViewModel.recording.maxDuration = Integer.valueOf(it)
            } catch (ex: NumberFormatException) {
                seriesRecordingViewModel.recording.maxDuration = 0
            }
        }
        binding.startExtra.afterTextChanged {
            try {
                seriesRecordingViewModel.recording.startExtra = java.lang.Long.valueOf(it)
            } catch (ex: NumberFormatException) {
                seriesRecordingViewModel.recording.startExtra = 2
            }
        }
        binding.stopExtra.afterTextChanged {
            try {
                seriesRecordingViewModel.recording.stopExtra = java.lang.Long.valueOf(it)
            } catch (ex: NumberFormatException) {
                seriesRecordingViewModel.recording.stopExtra = 2
            }
        }
        binding.isEnabled.setOnCheckedChangeListener { _, isChecked ->
            seriesRecordingViewModel.recording.isEnabled = isChecked
        }
    }

    private fun handleTimeEnabledClick(checked: Boolean) {
        Timber.d("Setting time enabled ${binding.timeEnabled.isChecked}")
        seriesRecordingViewModel.isTimeEnabled = checked

        binding.startTimeLabel.visibleOrGone(checked)
        binding.startTime.visibleOrGone(checked)
        binding.startTime.isEnabled = checked

        binding.startWindowTimeLabel.visibleOrGone(checked)
        binding.startWindowTime.visibleOrGone(checked)
        binding.startWindowTime.isEnabled = checked
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
     * created recording.
     */
    private fun save() {
        if (seriesRecordingViewModel.recording.title.isNullOrEmpty()) {
            context?.sendSnackbarMessage(R.string.error_empty_title)
            return
        }

        // The maximum durationTextView must be at least the minimum durationTextView
        if (seriesRecordingViewModel.recording.minDuration > 0
                && seriesRecordingViewModel.recording.maxDuration > 0
                && seriesRecordingViewModel.recording.maxDuration < seriesRecordingViewModel.recording.minDuration) {
            seriesRecordingViewModel.recording.maxDuration = seriesRecordingViewModel.recording.minDuration
        }

        val intent = seriesRecordingViewModel.getIntentData(requireContext(), seriesRecordingViewModel.recording)

        // Add the recording profile if available and enabled
        if (profile != null && htspVersion >= 16 && binding.dvrConfig.text.isNotEmpty()) {
            intent.putExtra("configName", binding.dvrConfig.text.toString())
        }

        // Update the recording in case the id is not empty, otherwise add a new one.
        // When adding a new recording, the id is an empty string as a default.
        if (seriesRecordingViewModel.recording.id.isNotEmpty()) {
            intent.action = "updateAutorecEntry"
            intent.putExtra("id", seriesRecordingViewModel.recording.id)
        } else {
            intent.action = "addAutorecEntry"
        }
        activity?.startService(intent)
        activity?.supportFragmentManager?.popBackStack()
    }

    /**
     * Asks the user to confirm canceling the current activity. If no is
     * chosen the user can continue to add or edit the recording. Otherwise
     * the input will be discarded and the activity will be closed.
     */
    private fun cancel() {
        context?.let {
            MaterialDialog(it).show {
                message(R.string.cancel_add_recording)
                positiveButton(R.string.discard) { activity?.supportFragmentManager?.popBackStack() }
                negativeButton(R.string.cancel) { dismiss() }
            }
        }
    }

    override fun onChannelSelected(channel: Channel) {
        seriesRecordingViewModel.recording.channelId = channel.id
        binding.channelName.text = channel.name
    }

    override fun onPrioritySelected(which: Int) {
        seriesRecordingViewModel.recording.priority = which
        context?.let {
            binding.priority.text = getPriorityName(it, seriesRecordingViewModel.recording.priority)
        }
    }

    override fun onProfileSelected(which: Int) {
        binding.dvrConfig.text = recordingProfilesList[which]
        seriesRecordingViewModel.recordingProfileNameId = which
    }

    override fun onTimeSelected(milliSeconds: Long, tag: String?) {
        if (tag == "startTime") {
            seriesRecordingViewModel.startTimeInMillis = milliSeconds
            // If the start time is after the start window time, update the start window time with the start value
            if (milliSeconds > seriesRecordingViewModel.startWindowTimeInMillis) {
                seriesRecordingViewModel.startWindowTimeInMillis = milliSeconds
            }
        } else if (tag == "startWindowTime") {
            seriesRecordingViewModel.startWindowTimeInMillis = milliSeconds
            // If the start window time is before the start time, update the start time with the start window value
            if (milliSeconds < seriesRecordingViewModel.startTimeInMillis) {
                seriesRecordingViewModel.startTimeInMillis = milliSeconds
            }
        }

        binding.startTime.text = getTimeStringFromTimeInMillis(seriesRecordingViewModel.startTimeInMillis)
        binding.startWindowTime.text = getTimeStringFromTimeInMillis(seriesRecordingViewModel.startWindowTimeInMillis)
    }

    override fun onDateSelected(milliSeconds: Long, tag: String?) {
        // NOP
    }

    override fun onDaysSelected(selectedDays: Int) {
        seriesRecordingViewModel.recording.daysOfWeek = selectedDays
        context?.let {
            binding.daysOfWeek.text = getSelectedDaysOfWeekText(it, selectedDays)
        }
    }

    private fun onDuplicateDetectionValueSelected(which: Int) {
        seriesRecordingViewModel.recording.dupDetect = which
        binding.duplicateDetection.text = seriesRecordingViewModel.duplicateDetectionList[which]
    }

    private fun handleDuplicateDetectionSelection(duplicateDetectionList: Array<String>, duplicateDetectionId: Int) {
        context?.let {
            MaterialDialog(it).show {
                title(R.string.select_duplicate_detection)
                listItemsSingleChoice(items = duplicateDetectionList.toList(), initialSelection = duplicateDetectionId) { _, index, _ ->
                    onDuplicateDetectionValueSelected(index)
                }
            }
        }
    }

    override fun onBackPressed() {
        cancel()
    }

    companion object {
        fun newInstance(id: String = ""): SeriesRecordingAddEditFragment {
            val f = SeriesRecordingAddEditFragment()
            if (id.isNotEmpty()) {
                f.arguments = Bundle().also { it.putString("id", id) }
            }
            return f
        }
    }
}
