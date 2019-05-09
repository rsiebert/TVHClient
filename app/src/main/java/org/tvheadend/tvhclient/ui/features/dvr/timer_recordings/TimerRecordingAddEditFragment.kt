package org.tvheadend.tvhclient.ui.features.dvr.timer_recordings

import android.content.Intent
import android.os.Bundle
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

// TODO 2 way use databinding and viewmodel

class TimerRecordingAddEditFragment : BaseFragment(), BackPressedInterface, RecordingConfigSelectedListener, DatePickerFragment.Listener, TimePickerFragment.Listener {

    private lateinit var timerRecordingViewModel: TimerRecordingViewModel
    private lateinit var recordingProfilesList: Array<String>

    private var profile: ServerProfile? = null

    /**
     * Returns an intent with the recording data
     */
    private val intentData: Intent
        get() {
            // TODO get this from the viewmodel
            val intent = Intent(context, HtspService::class.java)
            intent.putExtra("directory", timerRecordingViewModel.recording.directory)
            intent.putExtra("title", timerRecordingViewModel.recording.title)
            intent.putExtra("name", timerRecordingViewModel.recording.name)

            // Assume no start time is specified if 0:00 is selected
            if (timerRecordingViewModel.isTimeEnabled) {
                intent.putExtra("start", timerRecordingViewModel.recording.start)
                intent.putExtra("stop", timerRecordingViewModel.recording.stop)
            } else {
                intent.putExtra("start", -1)
                intent.putExtra("stop", -1)
            }
            intent.putExtra("daysOfWeek", timerRecordingViewModel.recording.daysOfWeek)
            intent.putExtra("priority", timerRecordingViewModel.recording.priority)
            intent.putExtra("enabled", if (timerRecordingViewModel.recording.isEnabled) 1 else 0)

            if (timerRecordingViewModel.recording.channelId > 0) {
                intent.putExtra("channelId", timerRecordingViewModel.recording.channelId)
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
        return inflater.inflate(R.layout.timer_recording_add_edit_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        timerRecordingViewModel = ViewModelProviders.of(activity!!).get(TimerRecordingViewModel::class.java)

        recordingProfilesList = timerRecordingViewModel.getRecordingProfileNames()
        profile = timerRecordingViewModel.getRecordingProfile()
        timerRecordingViewModel.recordingProfileNameId = getSelectedProfileId(profile, recordingProfilesList)

        if (savedInstanceState == null) {
            timerRecordingViewModel.loadRecordingByIdSync(arguments?.getString("id", "") ?: "")
        }

        setHasOptionsMenu(true)
        updateUI()

        toolbarInterface.setTitle(if (timerRecordingViewModel.recording.id.isNotEmpty())
            getString(R.string.edit_recording)
        else
            getString(R.string.add_recording))
    }

    private fun updateUI() {
        val ctx = context ?: return

        is_enabled.visibleOrGone(htspVersion >= 19)
        is_enabled.isChecked = timerRecordingViewModel.recording.isEnabled

        title.setText(timerRecordingViewModel.recording.title)
        name.setText(timerRecordingViewModel.recording.name)

        directory_label.visibleOrGone(htspVersion >= 19)
        directory.visibleOrGone(htspVersion >= 19)
        directory.setText(timerRecordingViewModel.recording.directory)

        channel_name.text = timerRecordingViewModel.recording.channelName ?: getString(R.string.all_channels)
        channel_name.setOnClickListener {
            // Determine if the server supports recording on all channels
            val allowRecordingOnAllChannels = htspVersion >= 21
            handleChannelListSelection(ctx, timerRecordingViewModel.getChannelList(), allowRecordingOnAllChannels, this@TimerRecordingAddEditFragment)
        }

        priority.text = getPriorityName(ctx, timerRecordingViewModel.recording.priority)
        priority.setOnClickListener {
            handlePrioritySelection(ctx, timerRecordingViewModel.recording.priority, this@TimerRecordingAddEditFragment)
        }

        dvr_config.visibleOrGone(recordingProfilesList.isNotEmpty())
        dvr_config_label.visibleOrGone(recordingProfilesList.isNotEmpty())

        if (recordingProfilesList.isNotEmpty()) {
            dvr_config.text = recordingProfilesList[timerRecordingViewModel.recordingProfileNameId]
            dvr_config.setOnClickListener {
                handleRecordingProfileSelection(ctx, recordingProfilesList, timerRecordingViewModel.recordingProfileNameId, this@TimerRecordingAddEditFragment)
            }
        }

        start_time.text = getTimeStringFromTimeInMillis(timerRecordingViewModel.startTimeInMillis)
        start_time.setOnClickListener {
            handleTimeSelection(activity, timerRecordingViewModel.startTimeInMillis, this@TimerRecordingAddEditFragment, "startTime")
        }

        stop_time.text = getTimeStringFromTimeInMillis(timerRecordingViewModel.stopTimeInMillis)
        stop_time.setOnClickListener {
            handleTimeSelection(activity, timerRecordingViewModel.stopTimeInMillis, this@TimerRecordingAddEditFragment, "stopTime")
        }

        days_of_week.text = getSelectedDaysOfWeekText(ctx, timerRecordingViewModel.recording.daysOfWeek)
        days_of_week.setOnClickListener {
            handleDayOfWeekSelection(ctx, timerRecordingViewModel.recording.daysOfWeek, this@TimerRecordingAddEditFragment)
        }

        time_enabled.isChecked = timerRecordingViewModel.isTimeEnabled
        handleTimeEnabledClick(time_enabled.isChecked)

        time_enabled.setOnClickListener {
            handleTimeEnabledClick(time_enabled.isChecked)
        }

        title.afterTextChanged { timerRecordingViewModel.recording.title = it }
        name.afterTextChanged { timerRecordingViewModel.recording.name = it }
        directory.afterTextChanged { timerRecordingViewModel.recording.directory = it }
        is_enabled.setOnCheckedChangeListener { _, isChecked ->
            timerRecordingViewModel.recording.isEnabled = isChecked
        }
    }

    private fun handleTimeEnabledClick(checked: Boolean) {
        Timber.d("Setting time enabled ${time_enabled.isChecked}")
        timerRecordingViewModel.isTimeEnabled = checked

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
        if (timerRecordingViewModel.recording.title.isNullOrEmpty()) {
            context?.sendSnackbarMessage(R.string.error_empty_title)
            return
        }

        val intent = intentData
        if (timerRecordingViewModel.recording.id.isNotEmpty()) {
            intent.action = "updateTimerecEntry"
            intent.putExtra("id", timerRecordingViewModel.recording.id)
        } else {
            intent.action = "addTimerecEntry"
        }
        activity?.startService(intent)
        activity?.finish()
    }

    private fun cancel() {
        // Show confirmation dialog to cancel
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
        timerRecordingViewModel.recording.channelId = channel.id
        channel_name.text = channel.name
    }

    override fun onPrioritySelected(which: Int) {
        timerRecordingViewModel.recording.priority = which
        context?.let {
            priority.text = getPriorityName(it, timerRecordingViewModel.recording.priority)
        }
    }

    override fun onProfileSelected(which: Int) {
        dvr_config.text = recordingProfilesList[which]
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

        start_time.text = getTimeStringFromTimeInMillis(timerRecordingViewModel.startTimeInMillis)
        stop_time.text = getTimeStringFromTimeInMillis(timerRecordingViewModel.stopTimeInMillis)
    }

    override fun onDateSelected(milliSeconds: Long, tag: String?) {
        // NOP
    }

    override fun onDaysSelected(selectedDays: Int) {
        timerRecordingViewModel.recording.daysOfWeek = selectedDays
        context?.let {
            days_of_week.text = getSelectedDaysOfWeekText(it, selectedDays)
        }
    }

    override fun onBackPressed() {
        cancel()
    }
}
