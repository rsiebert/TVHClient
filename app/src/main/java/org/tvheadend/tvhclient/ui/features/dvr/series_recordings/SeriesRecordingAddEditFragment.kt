package org.tvheadend.tvhclient.ui.features.dvr.series_recordings

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.*
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.TextView
import androidx.lifecycle.ViewModelProviders
import butterknife.*
import com.afollestad.materialdialogs.MaterialDialog
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.data.service.HtspService
import org.tvheadend.tvhclient.domain.entity.Channel
import org.tvheadend.tvhclient.domain.entity.ServerProfile
import org.tvheadend.tvhclient.ui.base.BaseFragment
import org.tvheadend.tvhclient.ui.base.callbacks.BackPressedInterface
import org.tvheadend.tvhclient.ui.base.utils.SnackbarUtils
import org.tvheadend.tvhclient.ui.features.dvr.DatePickerFragment
import org.tvheadend.tvhclient.ui.features.dvr.RecordingConfigSelectedListener
import org.tvheadend.tvhclient.ui.features.dvr.RecordingUtils
import org.tvheadend.tvhclient.ui.features.dvr.TimePickerFragment
import org.tvheadend.tvhclient.util.MiscUtils
import timber.log.Timber

class SeriesRecordingAddEditFragment : BaseFragment(), BackPressedInterface, RecordingConfigSelectedListener, DatePickerFragment.Listener, TimePickerFragment.Listener {

    @BindView(R.id.is_enabled)
    lateinit var isEnabledCheckbox: CheckBox
    @BindView(R.id.days_of_week)
    lateinit var daysOfWeekTextView: TextView
    @BindView(R.id.minimum_duration)
    lateinit var minDurationEditText: EditText
    @BindView(R.id.maximum_duration)
    lateinit var maxDurationEditText: EditText
    @BindView(R.id.time_enabled)
    lateinit var timeEnabledCheckBox: CheckBox
    @BindView(R.id.start_time_label)
    lateinit var startTimeLabelTextView: TextView
    @BindView(R.id.start_time)
    lateinit var startTimeTextView: TextView
    @BindView(R.id.start_window_time_label)
    lateinit var startWindowTimeLabelTextView: TextView
    @BindView(R.id.start_window_time)
    lateinit var startWindowTimeTextView: TextView
    @BindView(R.id.start_extra)
    lateinit var startExtraTimeTextView: EditText
    @BindView(R.id.stop_extra)
    lateinit var stopExtraTimeTextView: EditText
    @BindView(R.id.duplicate_detection)
    lateinit var duplicateDetectionTextView: TextView
    @BindView(R.id.duplicate_detection_label)
    lateinit var duplicateDetectionLabelTextView: TextView
    @BindView(R.id.directory)
    lateinit var directoryEditText: EditText
    @BindView(R.id.directory_label)
    lateinit var directoryLabelTextView: TextView
    @BindView(R.id.title)
    lateinit var titleEditText: EditText
    @BindView(R.id.name)
    lateinit var nameEditText: EditText
    @BindView(R.id.channel)
    lateinit var channelNameTextView: TextView
    @BindView(R.id.dvr_config_label)
    lateinit var recordingProfileLabelTextView: TextView
    @BindView(R.id.priority)
    lateinit var priorityTextView: TextView
    @BindView(R.id.dvr_config)
    lateinit var recordingProfileNameTextView: TextView

    lateinit var unbinder: Unbinder

    private lateinit var recordingProfilesList: Array<String>
    private lateinit var duplicateDetectionList: Array<String>
    private var channelList: List<Channel>? = null
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
            if (viewModel.recording.isTimeEnabled) {
                Timber.d("Intent Recording start time is " + viewModel.recording.start)
                Timber.d("Intent Recording startWindow time is " + viewModel.recording.startWindow)
                // TODO why do we need to add 15 minutes here?
                intent.putExtra("start", viewModel.recording.start + 15)
                intent.putExtra("startWindow", viewModel.recording.startWindow + 15)
            } else {
                intent.putExtra("start", -1)
                intent.putExtra("startWindow", -1)
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
            if (MiscUtils.isServerProfileEnabled(profile, serverStatus) && recordingProfileNameTextView.text.isNotEmpty()) {
                // Use the selected profile. If no change was done in the
                // selection then the default one from the connection setting will be used
                intent.putExtra("configName", recordingProfileNameTextView.text.toString())
            }
            return intent
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.series_recording_add_edit_fragment, container, false)
        unbinder = ButterKnife.bind(this, view)
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        unbinder.unbind()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel = ViewModelProviders.of(activity).get(SeriesRecordingViewModel::class.java)

        duplicateDetectionList = activity.resources.getStringArray(R.array.duplicate_detection_list)
        recordingProfilesList = appRepository.serverProfileData.recordingProfileNames
        profile = appRepository.serverProfileData.getItemById(serverStatus.recordingServerProfileId)
        viewModel.recordingProfileNameId = RecordingUtils.getSelectedProfileId(profile, recordingProfilesList)
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

        isEnabledCheckbox.visibility = if (htspVersion >= 19) View.VISIBLE else View.GONE
        isEnabledCheckbox.isChecked = viewModel.recording.isEnabled
        titleEditText.setText(viewModel.recording.title)
        nameEditText.setText(viewModel.recording.name)
        directoryLabelTextView.visibility = if (htspVersion >= 19) View.VISIBLE else View.GONE
        directoryEditText.visibility = if (htspVersion >= 19) View.VISIBLE else View.GONE
        directoryEditText.setText(viewModel.recording.directory)

        channelNameTextView.text = if (!TextUtils.isEmpty(viewModel.recording.channelName)) viewModel.recording.channelName else getString(R.string.all_channels)
        channelNameTextView.setOnClickListener {
            // Determine if the server supports recording on all channels
            val allowRecordingOnAllChannels = htspVersion >= 21
            RecordingUtils.handleChannelListSelection(activity, channelList, allowRecordingOnAllChannels, this@SeriesRecordingAddEditFragment)
        }

        priorityTextView.text = RecordingUtils.getPriorityName(activity, viewModel.recording.priority)
        priorityTextView.setOnClickListener {
            RecordingUtils.handlePrioritySelection(activity, viewModel.recording.priority, this@SeriesRecordingAddEditFragment)
        }

        if (recordingProfilesList.isEmpty()) {
            recordingProfileNameTextView.visibility = View.GONE
            recordingProfileLabelTextView.visibility = View.GONE
        } else {
            recordingProfileNameTextView.visibility = View.VISIBLE
            recordingProfileLabelTextView.visibility = View.VISIBLE

            recordingProfileNameTextView.text = recordingProfilesList[viewModel.recordingProfileNameId]
            recordingProfileNameTextView.setOnClickListener {
                RecordingUtils.handleRecordingProfileSelection(activity, recordingProfilesList, viewModel.recordingProfileNameId, this)
            }
        }

        startTimeTextView.text = RecordingUtils.getTimeStringFromTimeInMillis(viewModel.startTimeInMillis)
        startTimeTextView.setOnClickListener {
            RecordingUtils.handleTimeSelection(activity, viewModel.startTimeInMillis, this@SeriesRecordingAddEditFragment, "startTime")
        }

        startWindowTimeTextView.text = RecordingUtils.getTimeStringFromTimeInMillis(viewModel.startWindowTimeInMillis)
        startWindowTimeTextView.setOnClickListener {
            RecordingUtils.handleTimeSelection(activity, viewModel.startWindowTimeInMillis, this@SeriesRecordingAddEditFragment, "startWindowTime")
        }

        startExtraTimeTextView.setText(viewModel.recording.startExtra.toString())
        stopExtraTimeTextView.setText(viewModel.recording.stopExtra.toString())

        daysOfWeekTextView.text = RecordingUtils.getSelectedDaysOfWeekText(activity, viewModel.recording.daysOfWeek)
        daysOfWeekTextView.setOnClickListener {
            RecordingUtils.handleDayOfWeekSelection(activity, viewModel.recording.daysOfWeek, this@SeriesRecordingAddEditFragment)
        }

        minDurationEditText.setText(if (viewModel.recording.minDuration > 0) viewModel.recording.minDuration.toString() else getString(R.string.duration_sum))
        maxDurationEditText.setText(if (viewModel.recording.maxDuration > 0) viewModel.recording.maxDuration.toString() else getString(R.string.duration_sum))

        timeEnabledCheckBox.isChecked = viewModel.recording.isTimeEnabled

        timeEnabledCheckBox.setOnClickListener {
            val checked = timeEnabledCheckBox.isChecked
            viewModel.recording.isTimeEnabled = checked

            startTimeLabelTextView.visibility = if (checked) View.VISIBLE else View.GONE
            startTimeTextView.visibility = if (checked) View.VISIBLE else View.GONE
            startTimeTextView.isEnabled = checked

            startWindowTimeLabelTextView.visibility = if (checked) View.VISIBLE else View.GONE
            startWindowTimeTextView.visibility = if (checked) View.VISIBLE else View.GONE
            startWindowTimeTextView.isEnabled = checked
        }

        duplicateDetectionLabelTextView.visibility = if (htspVersion >= 20) View.VISIBLE else View.GONE
        duplicateDetectionTextView.visibility = if (htspVersion >= 20) View.VISIBLE else View.GONE
        duplicateDetectionTextView.text = duplicateDetectionList[viewModel.recording.dupDetect]

        duplicateDetectionTextView.setOnClickListener {
            handleDuplicateDetectionSelection(duplicateDetectionList, viewModel.recording.dupDetect)
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
     * created recording.
     */
    private fun save() {
        if (TextUtils.isEmpty(viewModel.recording.title)) {
            SnackbarUtils.sendSnackbarMessage(activity, R.string.error_empty_title)
            return
        }

        // The maximum durationTextView must be at least the minimum durationTextView
        if (viewModel.recording.minDuration > 0
                && viewModel.recording.maxDuration > 0
                && viewModel.recording.maxDuration < viewModel.recording.minDuration) {
            viewModel.recording.maxDuration = viewModel.recording.minDuration
        }

        if (!TextUtils.isEmpty(viewModel.recording.id)) {
            updateSeriesRecording()
        } else {
            addSeriesRecording()
        }
    }

    /**
     * Asks the user to confirm canceling the current activity. If no is
     * chosen the user can continue to add or edit the recording. Otherwise
     * the input will be discarded and the activity will be closed.
     */
    private fun cancel() {
        MaterialDialog.Builder(activity)
                .content(R.string.cancel_add_recording)
                .positiveText(getString(R.string.discard))
                .negativeText(getString(R.string.cancel))
                .onPositive { _, _ -> activity.finish() }
                .onNegative { dialog, _ -> dialog.cancel() }
                .show()
    }

    /**
     * Adds a new series recording with the given values. This method is also
     * called when a recording is being edited. It adds a recording with edited
     * values which was previously removed.
     */
    private fun addSeriesRecording() {
        val intent = intentData
        intent.action = "addAutorecEntry"
        activity.startService(intent)
        activity.finish()
    }

    /**
     * Update the series recording with the given values.
     */
    private fun updateSeriesRecording() {
        val intent = intentData
        intent.action = "updateAutorecEntry"
        intent.putExtra("id", viewModel.recording.id)
        activity.startService(intent)
        activity.finish()
    }

    override fun onChannelSelected(channel: Channel) {
        viewModel.recording.channelId = channel.id
        channelNameTextView.text = channel.name
    }

    override fun onPrioritySelected(which: Int) {
        viewModel.recording.priority = which
        priorityTextView.text = RecordingUtils.getPriorityName(activity, viewModel.recording.priority)
    }

    override fun onProfileSelected(which: Int) {
        recordingProfileNameTextView.text = recordingProfilesList[which]
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

        startTimeTextView.text = RecordingUtils.getTimeStringFromTimeInMillis(viewModel.startTimeInMillis)
        startWindowTimeTextView.text = RecordingUtils.getTimeStringFromTimeInMillis(viewModel.startWindowTimeInMillis)
    }

    override fun onDateSelected(milliSeconds: Long, tag: String?) {
        // NOP
    }

    override fun onDaysSelected(selectedDays: Int) {
        viewModel.recording.daysOfWeek = selectedDays
        daysOfWeekTextView.text = RecordingUtils.getSelectedDaysOfWeekText(activity, selectedDays)
    }

    private fun onDuplicateDetectionValueSelected(which: Int) {
        viewModel.recording.dupDetect = which
        duplicateDetectionTextView.text = duplicateDetectionList[which]
    }

    private fun handleDuplicateDetectionSelection(duplicateDetectionList: Array<String>, duplicateDetectionId: Int) {
        MaterialDialog.Builder(activity)
                .title(R.string.select_duplicate_detection)
                .items(*duplicateDetectionList)
                .itemsCallbackSingleChoice(duplicateDetectionId) { _, _, which, _ ->
                    onDuplicateDetectionValueSelected(which)
                    true
                }
                .show()
    }

    override fun onBackPressed() {
        cancel()
    }

    @OnTextChanged(R.id.title)
    internal fun onTitleTextChanged(text: CharSequence, start: Int, count: Int, after: Int) {
        viewModel.recording.title = text.toString()
    }

    @OnTextChanged(R.id.name)
    internal fun onNameTextChanged(text: CharSequence, start: Int, count: Int, after: Int) {
        viewModel.recording.name = text.toString()
    }

    @OnTextChanged(R.id.directory)
    internal fun onDirectoryTextChanged(text: CharSequence, start: Int, count: Int, after: Int) {
        viewModel.recording.directory = text.toString()
    }

    @OnCheckedChanged(R.id.is_enabled)
    internal fun onEnabledCheckboxChanged(buttonView: CompoundButton, isChecked: Boolean) {
        viewModel.recording.isEnabled = isChecked
    }

    @OnTextChanged(R.id.minimum_duration)
    internal fun onMinDurationTextChanged(text: CharSequence, start: Int, count: Int, after: Int) {
        try {
            viewModel.recording.minDuration = Integer.valueOf(text.toString())
        } catch (ex: NumberFormatException) {
            viewModel.recording.minDuration = 0
        }
    }

    @OnTextChanged(R.id.maximum_duration)
    internal fun onMaxDurationTextChanged(text: CharSequence, start: Int, count: Int, after: Int) {
        try {
            viewModel.recording.maxDuration = Integer.valueOf(text.toString())
        } catch (ex: NumberFormatException) {
            viewModel.recording.maxDuration = 0
        }
    }

    @OnTextChanged(R.id.start_extra)
    internal fun onStartExtraTextChanged(text: CharSequence, start: Int, count: Int, after: Int) {
        try {
            viewModel.recording.startExtra = java.lang.Long.valueOf(text.toString())
        } catch (ex: NumberFormatException) {
            viewModel.recording.startExtra = 2
        }
    }

    @OnTextChanged(R.id.stop_extra)
    internal fun onStopExtraTextChanged(text: CharSequence, start: Int, count: Int, after: Int) {
        try {
            viewModel.recording.stopExtra = java.lang.Long.valueOf(text.toString())
        } catch (ex: NumberFormatException) {
            viewModel.recording.stopExtra = 2
        }
    }

    @OnCheckedChanged(R.id.time_enabled)
    internal fun onTimeEnabledCheckboxChanged(buttonView: CompoundButton, isChecked: Boolean) {
        viewModel.recording.isTimeEnabled = isChecked
    }
}
