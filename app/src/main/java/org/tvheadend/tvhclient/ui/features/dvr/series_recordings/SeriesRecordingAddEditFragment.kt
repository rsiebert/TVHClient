package org.tvheadend.tvhclient.ui.features.dvr.series_recordings

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.lifecycle.ViewModelProviders
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import kotlinx.android.synthetic.main.series_recording_add_edit_fragment.*
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.data.service.HtspService
import org.tvheadend.tvhclient.domain.entity.Channel
import org.tvheadend.tvhclient.domain.entity.ServerProfile
import org.tvheadend.tvhclient.ui.base.BaseFragment
import org.tvheadend.tvhclient.ui.common.afterTextChanged
import org.tvheadend.tvhclient.ui.common.callbacks.BackPressedInterface
import org.tvheadend.tvhclient.ui.common.sendSnackbarMessage
import org.tvheadend.tvhclient.ui.common.visibleOrGone
import org.tvheadend.tvhclient.ui.features.dvr.*
import org.tvheadend.tvhclient.util.isServerProfileEnabled
import timber.log.Timber

// TODO 2 way use databinding and viewmodel

class SeriesRecordingAddEditFragment : BaseFragment(), BackPressedInterface, RecordingConfigSelectedListener, DatePickerFragment.Listener, TimePickerFragment.Listener {

    private lateinit var seriesRecordingViewModel: SeriesRecordingViewModel
    private lateinit var recordingProfilesList: Array<String>
    private lateinit var duplicateDetectionList: Array<String>
    private var profile: ServerProfile? = null

    /**
     * Returns an intent with the recording data
     */
    private val intentData: Intent
        get() {
            // TODO get this from the viewmodel
            val intent = Intent(context, HtspService::class.java)
            intent.putExtra("title", seriesRecordingViewModel.recording.title)
            intent.putExtra("name", seriesRecordingViewModel.recording.name)
            intent.putExtra("directory", seriesRecordingViewModel.recording.directory)
            intent.putExtra("minDuration", seriesRecordingViewModel.recording.minDuration * 60)
            intent.putExtra("maxDuration", seriesRecordingViewModel.recording.maxDuration * 60)

            // Assume no start time is specified if 0:00 is selected
            if (seriesRecordingViewModel.isTimeEnabled) {
                Timber.d("Intent Recording start time is ${seriesRecordingViewModel.recording.start}")
                Timber.d("Intent Recording startWindow time is ${seriesRecordingViewModel.recording.startWindow}")
                intent.putExtra("start", seriesRecordingViewModel.recording.start)
                intent.putExtra("startWindow", seriesRecordingViewModel.recording.startWindow)
            } else {
                intent.putExtra("start", (-1).toLong())
                intent.putExtra("startWindow", (-1).toLong())
            }
            intent.putExtra("startExtra", seriesRecordingViewModel.recording.startExtra)
            intent.putExtra("stopExtra", seriesRecordingViewModel.recording.stopExtra)
            intent.putExtra("dupDetect", seriesRecordingViewModel.recording.dupDetect)
            intent.putExtra("daysOfWeek", seriesRecordingViewModel.recording.daysOfWeek)
            intent.putExtra("priority", seriesRecordingViewModel.recording.priority)
            intent.putExtra("enabled", if (seriesRecordingViewModel.recording.isEnabled) 1 else 0)

            if (seriesRecordingViewModel.recording.channelId > 0) {
                intent.putExtra("channelId", seriesRecordingViewModel.recording.channelId)
            }

            // Add the recording profile if available and enabled
            if (isServerProfileEnabled(profile, htspVersion) && dvr_config.text.isNotEmpty()) {
                // Use the selected profile. If no change was done in the
                // selection then the default one from the connection setting will be used
                intent.putExtra("configName", dvr_config.text.toString())
            }
            return intent
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.series_recording_add_edit_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        seriesRecordingViewModel = ViewModelProviders.of(activity!!).get(SeriesRecordingViewModel::class.java)

        duplicateDetectionList = resources.getStringArray(R.array.duplicate_detection_list)
        recordingProfilesList = seriesRecordingViewModel.getRecordingProfileNames()
        profile = seriesRecordingViewModel.getRecordingProfile()
        seriesRecordingViewModel.recordingProfileNameId = getSelectedProfileId(profile, recordingProfilesList)

        if (savedInstanceState == null) {
            seriesRecordingViewModel.loadRecordingByIdSync(arguments?.getString("id", "") ?: "")
        }

        setHasOptionsMenu(true)
        updateUI()

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
        duplicate_detection.text = duplicateDetectionList[seriesRecordingViewModel.recording.dupDetect]

        duplicate_detection.setOnClickListener {
            handleDuplicateDetectionSelection(duplicateDetectionList, seriesRecordingViewModel.recording.dupDetect)
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

        val intent = intentData
        if (seriesRecordingViewModel.recording.id.isNotEmpty()) {
            intent.action = "updateAutorecEntry"
            intent.putExtra("id", seriesRecordingViewModel.recording.id)
        } else {
            intent.action = "addAutorecEntry"
        }
        activity?.startService(intent)
        activity?.finish()
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
                positiveButton(R.string.discard) { activity?.finish() }
                negativeButton(R.string.cancel) { cancel() }
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
        duplicate_detection.text = duplicateDetectionList[which]
    }

    private fun handleDuplicateDetectionSelection(duplicateDetectionList: Array<String>, duplicateDetectionId: Int) {
        context?.let {
            MaterialDialog(it).show() {
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
}
