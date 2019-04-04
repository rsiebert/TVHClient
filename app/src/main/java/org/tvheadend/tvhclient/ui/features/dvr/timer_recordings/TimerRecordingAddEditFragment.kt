package org.tvheadend.tvhclient.ui.features.dvr.timer_recordings

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.*
import androidx.lifecycle.ViewModelProviders
import com.afollestad.materialdialogs.MaterialDialog
import kotlinx.android.synthetic.main.timer_recording_add_edit_fragment.*
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

class TimerRecordingAddEditFragment : BaseFragment(), BackPressedInterface, RecordingConfigSelectedListener, DatePickerFragment.Listener, TimePickerFragment.Listener {

    private lateinit var recordingProfilesList: Array<String>
    private lateinit var channelList: List<Channel>
    private var profile: ServerProfile? = null

    lateinit var viewModel: TimerRecordingViewModel

    /**
     * Returns an intent with the recording data
     */
    private val intentData: Intent
        get() {
            val intent = Intent(context, HtspService::class.java)
            intent.putExtra("directory", viewModel.recording.directory)
            intent.putExtra("title", viewModel.recording.title)
            intent.putExtra("name", viewModel.recording.name)

            // Assume no start time is specified if 0:00 is selected
            if (viewModel.isTimeEnabled) {
                intent.putExtra("start", viewModel.recording.start)
                intent.putExtra("stop", viewModel.recording.stop)
            } else {
                intent.putExtra("start", -1)
                intent.putExtra("stop", -1)
            }
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
        return inflater.inflate(R.layout.timer_recording_add_edit_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel = ViewModelProviders.of(activity).get(TimerRecordingViewModel::class.java)

        recordingProfilesList = appRepository.serverProfileData.recordingProfileNames
        profile = appRepository.serverProfileData.getItemById(serverStatus.recordingServerProfileId)
        viewModel.recordingProfileNameId = getSelectedProfileId(profile, recordingProfilesList)
        channelList = appRepository.channelData.getItems()

        if (savedInstanceState == null) {
            viewModel.loadRecordingByIdSync(arguments?.getString("id", "") ?: "")
        }

        setHasOptionsMenu(true)
        updateUI()

        toolbarInterface.setTitle(if (!TextUtils.isEmpty(viewModel.recording.id))
            getString(R.string.edit_recording)
        else
            getString(R.string.add_recording))
    }

    private fun updateUI() {

        is_enabled.visibleOrGone(htspVersion >= 19)
        is_enabled.isChecked = viewModel.recording.isEnabled

        title.setText(viewModel.recording.title)
        name.setText(viewModel.recording.name)

        directory_label.visibleOrGone(htspVersion >= 19)
        directory.visibleOrGone(htspVersion >= 19)
        directory.setText(viewModel.recording.directory)

        channel_name.text = if (!TextUtils.isEmpty(viewModel.recording.channelName)) viewModel.recording.channelName else getString(R.string.all_channels)
        channel_name.setOnClickListener {
            // Determine if the server supports recording on all channels
            val allowRecordingOnAllChannels = htspVersion >= 21
            handleChannelListSelection(activity, channelList, allowRecordingOnAllChannels, this@TimerRecordingAddEditFragment)
        }

        priority.text = getPriorityName(activity, viewModel.recording.priority)
        priority.setOnClickListener {
            handlePrioritySelection(activity, viewModel.recording.priority, this@TimerRecordingAddEditFragment)
        }

        dvr_config.visibleOrGone(!recordingProfilesList.isEmpty())
        dvr_config_label.visibleOrGone(!recordingProfilesList.isEmpty())

        if (!recordingProfilesList.isEmpty()) {
            dvr_config.text = recordingProfilesList[viewModel.recordingProfileNameId]
            dvr_config.setOnClickListener {
                handleRecordingProfileSelection(activity, recordingProfilesList, viewModel.recordingProfileNameId, this@TimerRecordingAddEditFragment)
            }
        }

        start_time.text = getTimeStringFromTimeInMillis(viewModel.startTimeInMillis)
        start_time.setOnClickListener {
            handleTimeSelection(activity, viewModel.startTimeInMillis, this@TimerRecordingAddEditFragment, "startTime")
        }

        stop_time.text = getTimeStringFromTimeInMillis(viewModel.stopTimeInMillis)
        stop_time.setOnClickListener {
            handleTimeSelection(activity, viewModel.stopTimeInMillis, this@TimerRecordingAddEditFragment, "stopTime")
        }

        days_of_week.text = getSelectedDaysOfWeekText(activity, viewModel.recording.daysOfWeek)
        days_of_week.setOnClickListener {
            handleDayOfWeekSelection(activity, viewModel.recording.daysOfWeek, this@TimerRecordingAddEditFragment)
        }

        time_enabled.isChecked = viewModel.isTimeEnabled
        handleTimeEnabledClick(time_enabled.isChecked)

        time_enabled.setOnClickListener {
            handleTimeEnabledClick(time_enabled.isChecked)
        }

        title.afterTextChanged { viewModel.recording.title = it }
        name.afterTextChanged { viewModel.recording.name = it }
        directory.afterTextChanged { viewModel.recording.directory = it }
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

        stop_time_label.visibleOrGone(checked)
        stop_time.visibleOrGone(checked)
        stop_time.isEnabled = checked
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
        if (TextUtils.isEmpty(viewModel.recording.title)) {
            context?.sendSnackbarMessage(R.string.error_empty_title)
            return
        }
        if (!TextUtils.isEmpty(viewModel.recording.id)) {
            updateTimerRecording()
        } else {
            addTimerRecording()
        }
    }

    private fun cancel() {
        // Show confirmation dialog to cancel
        MaterialDialog.Builder(activity)
                .content(R.string.cancel_add_recording)
                .positiveText(getString(R.string.discard))
                .negativeText(getString(R.string.cancel))
                .onPositive { _, _ -> activity.finish() }
                .onNegative { dialog, _ -> dialog.cancel() }
                .show()
    }

    /**
     * Adds a new timer recording with the given values. This method is also
     * called when a recording is being edited. It adds a recording with edited
     * values which was previously removed.
     */
    private fun addTimerRecording() {
        val intent = intentData
        intent.action = "addTimerecEntry"
        activity.startService(intent)
        activity.finish()
    }

    /**
     * Updates the timer recording with the given values.
     * If the API version supports it, use the native service call method
     * otherwise the old recording is removed and a new one with the
     * edited values is added afterwards. This is done in the service
     */
    private fun updateTimerRecording() {
        val intent = intentData
        intent.action = "updateTimerecEntry"
        intent.putExtra("id", viewModel.recording.id)
        activity.startService(intent)
        activity.finish()
    }

    override fun onChannelSelected(channel: Channel) {
        viewModel.recording.channelId = channel.id
        channel_name.text = channel.name
    }

    override fun onPrioritySelected(which: Int) {
        viewModel.recording.priority = which
        priority.text = getPriorityName(activity, viewModel.recording.priority)
    }

    override fun onProfileSelected(which: Int) {
        dvr_config.text = recordingProfilesList[which]
        viewModel.recordingProfileNameId = which
    }

    override fun onTimeSelected(milliSeconds: Long, tag: String?) {
        if (tag == "startTime") {
            viewModel.startTimeInMillis = milliSeconds
            // If the start time is after the stop time, update the stop time with the start value
            if (milliSeconds > viewModel.stopTimeInMillis) {
                viewModel.stopTimeInMillis = milliSeconds
            }
        } else if (tag == "stopTime") {
            viewModel.stopTimeInMillis = milliSeconds
            // If the stop time is before the start time, update the start time with the stop value
            if (milliSeconds < viewModel.recording.startTimeInMillis) {
                viewModel.startTimeInMillis = milliSeconds
            }
        }

        start_time.text = getTimeStringFromTimeInMillis(viewModel.startTimeInMillis)
        stop_time.text = getTimeStringFromTimeInMillis(viewModel.stopTimeInMillis)
    }

    override fun onDateSelected(milliSeconds: Long, tag: String?) {
        // NOP
    }

    override fun onDaysSelected(selectedDays: Int) {
        viewModel.recording.daysOfWeek = selectedDays
        days_of_week.text = getSelectedDaysOfWeekText(activity, selectedDays)
    }

    override fun onBackPressed() {
        cancel()
    }
}
