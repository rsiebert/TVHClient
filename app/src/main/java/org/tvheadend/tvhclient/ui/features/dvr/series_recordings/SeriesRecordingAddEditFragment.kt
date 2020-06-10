package org.tvheadend.tvhclient.ui.features.dvr.series_recordings

import android.os.Bundle
import android.view.*
import androidx.core.view.forEach
import androidx.lifecycle.ViewModelProviders
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import kotlinx.android.synthetic.main.series_recording_add_edit_fragment.*
import org.tvheadend.data.entity.Channel
import org.tvheadend.data.entity.ServerProfile
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.ui.base.BaseFragment
import org.tvheadend.tvhclient.ui.common.interfaces.HideNavigationDrawerInterface
import org.tvheadend.tvhclient.ui.common.interfaces.BackPressedInterface
import org.tvheadend.tvhclient.ui.common.interfaces.LayoutControlInterface
import org.tvheadend.tvhclient.ui.features.dvr.*
import org.tvheadend.tvhclient.util.extensions.afterTextChanged
import org.tvheadend.tvhclient.util.extensions.sendSnackbarMessage
import org.tvheadend.tvhclient.util.extensions.visibleOrGone
import timber.log.Timber

class SeriesRecordingAddEditFragment : BaseFragment(), BackPressedInterface, RecordingConfigSelectedListener, DatePickerFragment.Listener, TimePickerFragment.Listener, HideNavigationDrawerInterface {

    private lateinit var seriesRecordingViewModel: SeriesRecordingViewModel
    private lateinit var recordingProfilesList: Array<String>
    private var profile: ServerProfile? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.series_recording_add_edit_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        seriesRecordingViewModel = ViewModelProviders.of(requireActivity()).get(SeriesRecordingViewModel::class.java)

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

        is_enabled.visibleOrGone(htspVersion >= 19)
        is_enabled.isChecked = seriesRecordingViewModel.recording.isEnabled

        title.setText(seriesRecordingViewModel.recording.title)
        name.setText(seriesRecordingViewModel.recording.name)

        directory_label.visibleOrGone(htspVersion >= 19)
        directory.visibleOrGone(htspVersion >= 19)
        directory.setText(seriesRecordingViewModel.recording.directory)

        channel_name.text = seriesRecordingViewModel.recording.channelName
                ?: getString(R.string.all_channels)
        channel_name.setOnClickListener {
            // Determine if the server supports recording on all channels
            val allowRecordingOnAllChannels = htspVersion >= 21
            handleChannelListSelection(ctx, seriesRecordingViewModel.getChannelList(), allowRecordingOnAllChannels, this@SeriesRecordingAddEditFragment)
        }

        priority.text = getPriorityName(ctx, seriesRecordingViewModel.recording.priority)
        priority.setOnClickListener {
            handlePrioritySelection(ctx, seriesRecordingViewModel.recording.priority, this@SeriesRecordingAddEditFragment)
        }

        dvr_config.visibleOrGone(recordingProfilesList.isNotEmpty())
        dvr_config_label.visibleOrGone(recordingProfilesList.isNotEmpty())

        if (recordingProfilesList.isNotEmpty()) {
            dvr_config.text = recordingProfilesList[seriesRecordingViewModel.recordingProfileNameId]
            dvr_config.setOnClickListener {
                handleRecordingProfileSelection(ctx, recordingProfilesList, seriesRecordingViewModel.recordingProfileNameId, this)
            }
        }

        start_time.text = getTimeStringFromTimeInMillis(seriesRecordingViewModel.startTimeInMillis)
        start_time.setOnClickListener {
            handleTimeSelection(activity, seriesRecordingViewModel.startTimeInMillis, this@SeriesRecordingAddEditFragment, "startTime")
        }

        start_window_time.text = getTimeStringFromTimeInMillis(seriesRecordingViewModel.startWindowTimeInMillis)
        start_window_time.setOnClickListener {
            handleTimeSelection(activity, seriesRecordingViewModel.startWindowTimeInMillis, this@SeriesRecordingAddEditFragment, "startWindowTime")
        }

        start_extra.setText(seriesRecordingViewModel.recording.startExtra.toString())
        stop_extra.setText(seriesRecordingViewModel.recording.stopExtra.toString())

        days_of_week.text = getSelectedDaysOfWeekText(ctx, seriesRecordingViewModel.recording.daysOfWeek)
        days_of_week.setOnClickListener {
            handleDayOfWeekSelection(ctx, seriesRecordingViewModel.recording.daysOfWeek, this@SeriesRecordingAddEditFragment)
        }

        minimum_duration.setText(if (seriesRecordingViewModel.recording.minDuration > 0) (seriesRecordingViewModel.recording.minDuration / 60).toString() else getString(R.string.duration_sum))
        maximum_duration.setText(if (seriesRecordingViewModel.recording.maxDuration > 0) (seriesRecordingViewModel.recording.maxDuration / 60).toString() else getString(R.string.duration_sum))

        time_enabled.isChecked = seriesRecordingViewModel.isTimeEnabled
        handleTimeEnabledClick(time_enabled.isChecked)

        time_enabled.setOnClickListener {
            handleTimeEnabledClick(time_enabled.isChecked)
        }

        duplicate_detection_label.visibleOrGone(htspVersion >= 20)
        duplicate_detection.visibleOrGone(htspVersion >= 20)
        duplicate_detection.text = seriesRecordingViewModel.duplicateDetectionList[seriesRecordingViewModel.recording.dupDetect]

        duplicate_detection.setOnClickListener {
            handleDuplicateDetectionSelection(seriesRecordingViewModel.duplicateDetectionList, seriesRecordingViewModel.recording.dupDetect)
        }

        title.afterTextChanged { seriesRecordingViewModel.recording.title = it }
        name.afterTextChanged { seriesRecordingViewModel.recording.name = it }
        directory.afterTextChanged { seriesRecordingViewModel.recording.directory = it }
        minimum_duration.afterTextChanged {
            try {
                seriesRecordingViewModel.recording.minDuration = Integer.valueOf(it)
            } catch (ex: NumberFormatException) {
                seriesRecordingViewModel.recording.minDuration = 0
            }
        }
        maximum_duration.afterTextChanged {
            try {
                seriesRecordingViewModel.recording.maxDuration = Integer.valueOf(it)
            } catch (ex: NumberFormatException) {
                seriesRecordingViewModel.recording.maxDuration = 0
            }
        }
        start_extra.afterTextChanged {
            try {
                seriesRecordingViewModel.recording.startExtra = java.lang.Long.valueOf(it)
            } catch (ex: NumberFormatException) {
                seriesRecordingViewModel.recording.startExtra = 2
            }
        }
        stop_extra.afterTextChanged {
            try {
                seriesRecordingViewModel.recording.stopExtra = java.lang.Long.valueOf(it)
            } catch (ex: NumberFormatException) {
                seriesRecordingViewModel.recording.stopExtra = 2
            }
        }
        is_enabled.setOnCheckedChangeListener { _, isChecked ->
            seriesRecordingViewModel.recording.isEnabled = isChecked
        }
    }

    private fun handleTimeEnabledClick(checked: Boolean) {
        Timber.d("Setting time enabled ${time_enabled.isChecked}")
        seriesRecordingViewModel.isTimeEnabled = checked

        start_time_label.visibleOrGone(checked)
        start_time.visibleOrGone(checked)
        start_time.isEnabled = checked

        start_window_time_label.visibleOrGone(checked)
        start_window_time.visibleOrGone(checked)
        start_window_time.isEnabled = checked
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
        if (profile != null && htspVersion >= 16 && dvr_config.text.isNotEmpty()) {
            intent.putExtra("configName", dvr_config.text.toString())
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
        channel_name.text = channel.name
    }

    override fun onPrioritySelected(which: Int) {
        seriesRecordingViewModel.recording.priority = which
        context?.let {
            priority.text = getPriorityName(it, seriesRecordingViewModel.recording.priority)
        }
    }

    override fun onProfileSelected(which: Int) {
        dvr_config.text = recordingProfilesList[which]
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

        start_time.text = getTimeStringFromTimeInMillis(seriesRecordingViewModel.startTimeInMillis)
        start_window_time.text = getTimeStringFromTimeInMillis(seriesRecordingViewModel.startWindowTimeInMillis)
    }

    override fun onDateSelected(milliSeconds: Long, tag: String?) {
        // NOP
    }

    override fun onDaysSelected(selectedDays: Int) {
        seriesRecordingViewModel.recording.daysOfWeek = selectedDays
        context?.let {
            days_of_week.text = getSelectedDaysOfWeekText(it, selectedDays)
        }
    }

    private fun onDuplicateDetectionValueSelected(which: Int) {
        seriesRecordingViewModel.recording.dupDetect = which
        duplicate_detection.text = seriesRecordingViewModel.duplicateDetectionList[which]
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
