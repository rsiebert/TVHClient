package org.tvheadend.tvhclient.ui.features.dvr.series_recordings

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.lifecycle.ViewModelProviders
import com.afollestad.materialdialogs.MaterialDialog
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

class SeriesRecordingAddEditFragment : BaseFragment(), BackPressedInterface, RecordingConfigSelectedListener, DatePickerFragment.Listener, TimePickerFragment.Listener {

    private lateinit var recordingProfilesList: Array<String>
    private lateinit var duplicateDetectionList: Array<String>
    private lateinit var channelList: List<Channel>
    private var profile: ServerProfile? = null

    lateinit var viewModel: SeriesRecordingViewModel

    /**
     * Returns an intent with the recording data
     */
    private val intentData: Intent
        get() {
            val intent = Intent(context, HtspService::class.java)
            intent.putExtra("title", viewModel.recording.title)
            intent.putExtra("name", viewModel.recording.name)
            intent.putExtra("directory", viewModel.recording.directory)
            intent.putExtra("minDuration", viewModel.recording.minDuration * 60)
            intent.putExtra("maxDuration", viewModel.recording.maxDuration * 60)

            // Assume no start time is specified if 0:00 is selected
            if (viewModel.isTimeEnabled) {
                Timber.d("Intent Recording start time is ${viewModel.recording.start}")
                Timber.d("Intent Recording startWindow time is ${viewModel.recording.startWindow}")
                intent.putExtra("start", viewModel.recording.start)
                intent.putExtra("startWindow", viewModel.recording.startWindow)
            } else {
                intent.putExtra("start", (-1).toLong())
                intent.putExtra("startWindow", (-1).toLong())
            }
            intent.putExtra("startExtra", viewModel.recording.startExtra)
            intent.putExtra("stopExtra", viewModel.recording.stopExtra)
            intent.putExtra("dupDetect", viewModel.recording.dupDetect)
            intent.putExtra("daysOfWeek", viewModel.recording.daysOfWeek)
            intent.putExtra("priority", viewModel.recording.priority)
            intent.putExtra("enabled", if (viewModel.recording.isEnabled) 1 else 0)

            if (viewModel.recording.channelId > 0) {
                intent.putExtra("channelId", viewModel.recording.channelId)
            }

            // Add the recording profile if available and enabled
            if (isServerProfileEnabled(profile, serverStatus) && dvr_config.text.isNotEmpty()) {
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

        viewModel = ViewModelProviders.of(activity!!).get(SeriesRecordingViewModel::class.java)

        duplicateDetectionList = resources.getStringArray(R.array.duplicate_detection_list)
        recordingProfilesList = appRepository.serverProfileData.recordingProfileNames
        profile = appRepository.serverProfileData.getItemById(serverStatus.recordingServerProfileId)
        viewModel.recordingProfileNameId = getSelectedProfileId(profile, recordingProfilesList)

        val defaultChannelSortOrder = resources.getString(R.string.pref_default_channel_sort_order)
        val channelSortOrder = Integer.valueOf(sharedPreferences.getString("channel_sort_order", defaultChannelSortOrder) ?: defaultChannelSortOrder)
        channelList = appRepository.channelData.getChannels(channelSortOrder)

        if (savedInstanceState == null) {
            viewModel.loadRecordingByIdSync(arguments?.getString("id", "") ?: "")
        }

        setHasOptionsMenu(true)
        updateUI()

        toolbarInterface.setTitle(if (viewModel.recording.id.isNotEmpty())
            getString(R.string.edit_recording)
        else
            getString(R.string.add_recording))
    }

    private fun updateUI() {
        val ctx = context ?: return

        is_enabled.visibleOrGone(htspVersion >= 19)
        is_enabled.isChecked = viewModel.recording.isEnabled

        title.setText(viewModel.recording.title)
        name.setText(viewModel.recording.name)

        directory_label.visibleOrGone(htspVersion >= 19)
        directory.visibleOrGone(htspVersion >= 19)
        directory.setText(viewModel.recording.directory)

        channel_name.text = viewModel.recording.channelName ?: getString(R.string.all_channels)
        channel_name.setOnClickListener {
            // Determine if the server supports recording on all channels
            val allowRecordingOnAllChannels = htspVersion >= 21
            handleChannelListSelection(ctx, channelList, allowRecordingOnAllChannels, this@SeriesRecordingAddEditFragment)
        }

        priority.text = getPriorityName(ctx, viewModel.recording.priority)
        priority.setOnClickListener {
            handlePrioritySelection(ctx, viewModel.recording.priority, this@SeriesRecordingAddEditFragment)
        }

        dvr_config.visibleOrGone(recordingProfilesList.isNotEmpty())
        dvr_config_label.visibleOrGone(recordingProfilesList.isNotEmpty())

        if (recordingProfilesList.isNotEmpty()) {
            dvr_config.text = recordingProfilesList[viewModel.recordingProfileNameId]
            dvr_config.setOnClickListener {
                handleRecordingProfileSelection(ctx, recordingProfilesList, viewModel.recordingProfileNameId, this)
            }
        }

        start_time.text = getTimeStringFromTimeInMillis(viewModel.startTimeInMillis)
        start_time.setOnClickListener {
            handleTimeSelection(activity, viewModel.startTimeInMillis, this@SeriesRecordingAddEditFragment, "startTime")
        }

        start_window_time.text = getTimeStringFromTimeInMillis(viewModel.startWindowTimeInMillis)
        start_window_time.setOnClickListener {
            handleTimeSelection(activity, viewModel.startWindowTimeInMillis, this@SeriesRecordingAddEditFragment, "startWindowTime")
        }

        start_extra.setText(viewModel.recording.startExtra.toString())
        stop_extra.setText(viewModel.recording.stopExtra.toString())

        days_of_week.text = getSelectedDaysOfWeekText(ctx, viewModel.recording.daysOfWeek)
        days_of_week.setOnClickListener {
            handleDayOfWeekSelection(ctx, viewModel.recording.daysOfWeek, this@SeriesRecordingAddEditFragment)
        }

        minimum_duration.setText(if (viewModel.recording.minDuration > 0) (viewModel.recording.minDuration/60).toString() else getString(R.string.duration_sum))
        maximum_duration.setText(if (viewModel.recording.maxDuration > 0) (viewModel.recording.maxDuration/60).toString() else getString(R.string.duration_sum))

        time_enabled.isChecked = viewModel.isTimeEnabled
        handleTimeEnabledClick(time_enabled.isChecked)

        time_enabled.setOnClickListener {
            handleTimeEnabledClick(time_enabled.isChecked)
        }

        duplicate_detection_label.visibleOrGone(htspVersion >= 20)
        duplicate_detection.visibleOrGone(htspVersion >= 20)
        duplicate_detection.text = duplicateDetectionList[viewModel.recording.dupDetect]

        duplicate_detection.setOnClickListener {
            handleDuplicateDetectionSelection(duplicateDetectionList, viewModel.recording.dupDetect)
        }

        title.afterTextChanged { viewModel.recording.title = it }
        name.afterTextChanged { viewModel.recording.name = it }
        directory.afterTextChanged { viewModel.recording.directory = it }
        minimum_duration.afterTextChanged {
            try {
                viewModel.recording.minDuration = Integer.valueOf(it)
            } catch (ex: NumberFormatException) {
                viewModel.recording.minDuration = 0
            }
        }
        maximum_duration.afterTextChanged {
            try {
                viewModel.recording.maxDuration = Integer.valueOf(it)
            } catch (ex: NumberFormatException) {
                viewModel.recording.maxDuration = 0
            }
        }
        start_extra.afterTextChanged {
            try {
                viewModel.recording.startExtra = java.lang.Long.valueOf(it)
            } catch (ex: NumberFormatException) {
                viewModel.recording.startExtra = 2
            }
        }
        stop_extra.afterTextChanged {
            try {
                viewModel.recording.stopExtra = java.lang.Long.valueOf(it)
            } catch (ex: NumberFormatException) {
                viewModel.recording.stopExtra = 2
            }
        }
        is_enabled.setOnCheckedChangeListener { _, isChecked ->
            viewModel.recording.isEnabled = isChecked
        }
    }

    private fun handleTimeEnabledClick(checked: Boolean) {
        Timber.d("Setting time enabled ${time_enabled.isChecked}")
        viewModel.isTimeEnabled = checked

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
        if (viewModel.recording.title.isNullOrEmpty()) {
            context?.sendSnackbarMessage(R.string.error_empty_title)
            return
        }

        // The maximum durationTextView must be at least the minimum durationTextView
        if (viewModel.recording.minDuration > 0
                && viewModel.recording.maxDuration > 0
                && viewModel.recording.maxDuration < viewModel.recording.minDuration) {
            viewModel.recording.maxDuration = viewModel.recording.minDuration
        }

        val intent = intentData
        if (viewModel.recording.id.isNotEmpty()) {
            intent.action = "updateAutorecEntry"
            intent.putExtra("id", viewModel.recording.id)
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
            MaterialDialog.Builder(it)
                    .content(R.string.cancel_add_recording)
                    .positiveText(getString(R.string.discard))
                    .negativeText(getString(R.string.cancel))
                    .onPositive { _, _ -> activity?.finish() }
                    .onNegative { dialog, _ -> dialog.cancel() }
                    .show()
        }
    }

    override fun onChannelSelected(channel: Channel) {
        viewModel.recording.channelId = channel.id
        channel_name.text = channel.name
    }

    override fun onPrioritySelected(which: Int) {
        viewModel.recording.priority = which
        context?.let {
            priority.text = getPriorityName(it, viewModel.recording.priority)
        }
    }

    override fun onProfileSelected(which: Int) {
        dvr_config.text = recordingProfilesList[which]
        viewModel.recordingProfileNameId = which
    }

    override fun onTimeSelected(milliSeconds: Long, tag: String?) {
        if (tag == "startTime") {
            viewModel.startTimeInMillis = milliSeconds
            // If the start time is after the start window time, update the start window time with the start value
            if (milliSeconds > viewModel.startWindowTimeInMillis) {
                viewModel.startWindowTimeInMillis = milliSeconds
            }
        } else if (tag == "startWindowTime") {
            viewModel.startWindowTimeInMillis = milliSeconds
            // If the start window time is before the start time, update the start time with the start window value
            if (milliSeconds < viewModel.startTimeInMillis) {
                viewModel.startTimeInMillis = milliSeconds
            }
        }

        start_time.text = getTimeStringFromTimeInMillis(viewModel.startTimeInMillis)
        start_window_time.text = getTimeStringFromTimeInMillis(viewModel.startWindowTimeInMillis)
    }

    override fun onDateSelected(milliSeconds: Long, tag: String?) {
        // NOP
    }

    override fun onDaysSelected(selectedDays: Int) {
        viewModel.recording.daysOfWeek = selectedDays
        context?.let {
            days_of_week.text = getSelectedDaysOfWeekText(it, selectedDays)
        }
    }

    private fun onDuplicateDetectionValueSelected(which: Int) {
        viewModel.recording.dupDetect = which
        duplicate_detection.text = duplicateDetectionList[which]
    }

    private fun handleDuplicateDetectionSelection(duplicateDetectionList: Array<String>, duplicateDetectionId: Int) {
        context?.let {
            MaterialDialog.Builder(it)
                    .title(R.string.select_duplicate_detection)
                    .items(*duplicateDetectionList)
                    .itemsCallbackSingleChoice(duplicateDetectionId) { _, _, which, _ ->
                        onDuplicateDetectionValueSelected(which)
                        true
                    }
                    .show()
        }
    }

    override fun onBackPressed() {
        cancel()
    }
}
