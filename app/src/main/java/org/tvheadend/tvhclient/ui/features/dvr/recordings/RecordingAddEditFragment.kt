package org.tvheadend.tvhclient.ui.features.dvr.recordings

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.*
import androidx.lifecycle.ViewModelProviders
import com.afollestad.materialdialogs.MaterialDialog
import kotlinx.android.synthetic.main.recording_add_edit_fragment.*
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.data.service.HtspService
import org.tvheadend.tvhclient.domain.entity.Channel
import org.tvheadend.tvhclient.domain.entity.ServerProfile
import org.tvheadend.tvhclient.ui.base.BaseFragment
import org.tvheadend.tvhclient.ui.common.afterTextChanged
import org.tvheadend.tvhclient.ui.common.callbacks.BackPressedInterface
import org.tvheadend.tvhclient.ui.common.gone
import org.tvheadend.tvhclient.ui.common.sendSnackbarMessage
import org.tvheadend.tvhclient.ui.common.visibleOrGone
import org.tvheadend.tvhclient.ui.features.dvr.*
import org.tvheadend.tvhclient.util.isServerProfileEnabled

class RecordingAddEditFragment : BaseFragment(), BackPressedInterface, RecordingConfigSelectedListener, DatePickerFragment.Listener, TimePickerFragment.Listener {

    private lateinit var viewModel: RecordingViewModel
    private lateinit var recordingProfilesList: Array<String>
    private lateinit var channelList: List<Channel>
    private var profile: ServerProfile? = null

    private// Pass on seconds not milliseconds
    // Pass on seconds not milliseconds
    // Add the recording profile if available and enabled
    // Use the selected profile. If no change was done in the
    // selection then the default one from the connection setting will be used
    val intentData: Intent
        get() {
            val intent = Intent(activity, HtspService::class.java)
            intent.putExtra("title", viewModel.recording.title)
            intent.putExtra("subtitle", viewModel.recording.subtitle)
            intent.putExtra("summary", viewModel.recording.summary)
            intent.putExtra("description", viewModel.recording.description)
            intent.putExtra("stop", viewModel.recording.stop / 1000)
            intent.putExtra("stopExtra", viewModel.recording.stopExtra)

            if (!viewModel.recording.isRecording) {
                intent.putExtra("channelId", viewModel.recording.channelId)
                intent.putExtra("start", viewModel.recording.start / 1000)
                intent.putExtra("startExtra", viewModel.recording.startExtra)
                intent.putExtra("priority", viewModel.recording.priority)
                intent.putExtra("enabled", if (viewModel.recording.isEnabled) 1 else 0)
            }
            if (isServerProfileEnabled(profile, serverStatus) && dvr_config.text.isNotEmpty()) {
                intent.putExtra("configName", dvr_config.text.toString())
            }
            return intent
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.recording_add_edit_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel = ViewModelProviders.of(activity).get(RecordingViewModel::class.java)

        recordingProfilesList = appRepository.serverProfileData.recordingProfileNames
        profile = appRepository.serverProfileData.getItemById(serverStatus.recordingServerProfileId)
        viewModel.recordingProfileNameId = getSelectedProfileId(profile, recordingProfilesList)
        channelList = appRepository.channelData.getItems()

        if (savedInstanceState == null) {
            viewModel.loadRecordingByIdSync(arguments?.getInt("id", 0) ?: 0)
        }

        setHasOptionsMenu(true)
        updateUI()

        toolbarInterface.setTitle(if (id > 0)
            getString(R.string.edit_recording)
        else
            getString(R.string.add_recording))
    }

    private fun updateUI() {

        title_label.visibleOrGone(htspVersion >= 21)
        title.visibleOrGone(htspVersion >= 21)
        title.setText(viewModel.recording.title)

        subtitle_label.visibleOrGone(htspVersion >= 21)
        subtitle.visibleOrGone(serverStatus.htspVersion >= 21)
        subtitle.setText(viewModel.recording.subtitle)

        summary_label.visibleOrGone(htspVersion >= 21)
        summary.visibleOrGone(htspVersion >= 21)
        summary.setText(viewModel.recording.summary)

        description_label.visibleOrGone(htspVersion >= 21)
        description.visibleOrGone(htspVersion >= 21)
        description.setText(viewModel.recording.description)

        stop_time.text = getTimeStringFromTimeInMillis(viewModel.recording.stop)
        stop_time.setOnClickListener { handleTimeSelection(activity, viewModel.recording.stop, this@RecordingAddEditFragment, "stopTime") }

        stop_date.text = getDateStringFromTimeInMillis(viewModel.recording.stop)
        stop_date.setOnClickListener { handleDateSelection(activity, viewModel.recording.stop, this@RecordingAddEditFragment, "stopDate") }

        stop_extra.setText(viewModel.recording.stopExtra.toString())

        channel_name_label.visibleOrGone(!viewModel.recording.isRecording)
        channel_name.visibleOrGone(!viewModel.recording.isRecording)

        if (!viewModel.recording.isRecording) {
            channel_name.text = if (!TextUtils.isEmpty(viewModel.recording.channelName)) viewModel.recording.channelName else getString(R.string.all_channels)
            channel_name.setOnClickListener {
                // Determine if the server supports recording on all channels
                val allowRecordingOnAllChannels = serverStatus.htspVersion >= 21
                handleChannelListSelection(activity, channelList, allowRecordingOnAllChannels, this@RecordingAddEditFragment)
            }
        }

        is_enabled.visibleOrGone(htspVersion >= 23 && !viewModel.recording.isRecording)
        is_enabled.isChecked = viewModel.recording.isEnabled

        priority.visibleOrGone(!viewModel.recording.isRecording)
        priority.text = getPriorityName(activity, viewModel.recording.priority)
        priority.setOnClickListener { handlePrioritySelection(activity, viewModel.recording.priority, this@RecordingAddEditFragment) }

        dvr_config.visibleOrGone(!(recordingProfilesList.isEmpty() || viewModel.recording.isRecording))
        dvr_config_label.visibleOrGone(!(recordingProfilesList.isEmpty() || viewModel.recording.isRecording))

        if (!recordingProfilesList.isEmpty() && !viewModel.recording.isRecording) {
            dvr_config.text = recordingProfilesList[viewModel.recordingProfileNameId]
            dvr_config.setOnClickListener { handleRecordingProfileSelection(activity, recordingProfilesList, viewModel.recordingProfileNameId, this@RecordingAddEditFragment) }
        }

        if (viewModel.recording.isRecording) {
            start_time_label.gone()
            start_time.gone()
            start_date.gone()
            start_extra_label.gone()
            start_extra.gone()
        } else {
            start_time.text = getTimeStringFromTimeInMillis(viewModel.recording.start)
            start_time.setOnClickListener { handleTimeSelection(activity, viewModel.recording.start, this@RecordingAddEditFragment, "startTime") }
            start_date.text = getDateStringFromTimeInMillis(viewModel.recording.start)
            start_date.setOnClickListener { handleDateSelection(activity, viewModel.recording.start, this@RecordingAddEditFragment, "startDate") }
            start_extra.setText(viewModel.recording.startExtra.toString())
        }

        title.afterTextChanged { viewModel.recording.title = it }
        subtitle.afterTextChanged { viewModel.recording.subtitle = it }
        description.afterTextChanged { viewModel.recording.description = it }
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
     * created viewModel.recording.
     */
    private fun save() {
        if (TextUtils.isEmpty(viewModel.recording.title) && serverStatus.htspVersion >= 21) {
            context?.sendSnackbarMessage(R.string.error_empty_title)
            return
        }
        if (viewModel.recording.channelId == 0 && serverStatus.htspVersion < 21) {
            context?.sendSnackbarMessage(R.string.error_no_channel_selected)
            return
        }

        if (viewModel.recording.start >= viewModel.recording.stop) {
            context?.sendSnackbarMessage(R.string.error_start_time_past_stop_time)
            return
        }

        if (id > 0) {
            updateRecording()
        } else {
            addRecording()
        }
    }

    private fun addRecording() {
        val intent = intentData
        intent.action = "addDvrEntry"
        activity.startService(intent)
        activity.finish()
    }

    private fun updateRecording() {
        val intent = intentData
        intent.action = "updateDvrEntry"
        intent.putExtra("id", id)
        activity.startService(intent)
        activity.finish()
    }

    /**
     * Asks the user to confirm canceling the current activity. If no is
     * chosen the user can continue to add or edit the viewModel.recording. Otherwise
     * the input will be discarded and the activity will be closed.
     */
    private fun cancel() {
        // Show confirmation dialog to cancel
        MaterialDialog.Builder(activity)
                .content(R.string.cancel_edit_recording)
                .positiveText(getString(R.string.discard))
                .negativeText(getString(R.string.cancel))
                .onPositive { _, _ -> activity.finish() }
                .onNegative { dialog, _ -> dialog.cancel() }
                .show()
    }

    override fun onChannelSelected(channel: Channel) {
        viewModel.recording.channelId = channel.id
        channel_name.text = channel.name
    }

    override fun onTimeSelected(milliSeconds: Long, tag: String?) {
        if (tag == "startTime") {
            viewModel.recording.start = milliSeconds
            start_time.text = getTimeStringFromTimeInMillis(milliSeconds)
        } else if (tag == "stopTime") {
            viewModel.recording.stop = milliSeconds
            stop_time.text = getTimeStringFromTimeInMillis(milliSeconds)
        }
    }

    override fun onDateSelected(milliSeconds: Long, tag: String?) {
        if (tag == "startDate") {
            viewModel.recording.start = milliSeconds
            start_date.text = getDateStringFromTimeInMillis(milliSeconds)
        } else if (tag == "stopDate") {
            viewModel.recording.stop = milliSeconds
            stop_date.text = getDateStringFromTimeInMillis(milliSeconds)
        }
    }

    override fun onPrioritySelected(which: Int) {
        priority.text = getPriorityName(activity, which)
        viewModel.recording.priority = which
    }

    override fun onDaysSelected(selectedDays: Int) {
        // NOP
    }

    override fun onProfileSelected(which: Int) {
        dvr_config.text = recordingProfilesList[which]
        viewModel.recordingProfileNameId = which
    }

    override fun onBackPressed() {
        cancel()
    }
}
