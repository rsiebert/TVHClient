package org.tvheadend.tvhclient.ui.features.dvr.series_recordings;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.domain.entity.Channel;
import org.tvheadend.tvhclient.domain.entity.SeriesRecording;
import org.tvheadend.tvhclient.domain.entity.ServerProfile;
import org.tvheadend.tvhclient.data.service.HtspService;
import org.tvheadend.tvhclient.ui.features.dvr.DatePickerFragment;
import org.tvheadend.tvhclient.ui.features.dvr.RecordingConfigSelectedListener;
import org.tvheadend.tvhclient.ui.features.dvr.RecordingUtils;
import org.tvheadend.tvhclient.ui.features.dvr.TimePickerFragment;
import org.tvheadend.tvhclient.ui.base.BaseFragment;
import org.tvheadend.tvhclient.ui.base.callbacks.BackPressedInterface;
import org.tvheadend.tvhclient.util.MiscUtils;
import org.tvheadend.tvhclient.ui.base.utils.SnackbarUtils;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProviders;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.OnTextChanged;
import butterknife.Unbinder;

public class SeriesRecordingAddEditFragment extends BaseFragment implements BackPressedInterface, RecordingConfigSelectedListener, DatePickerFragment.Listener, TimePickerFragment.Listener {

    @BindView(R.id.is_enabled)
    CheckBox isEnabledCheckbox;
    @BindView(R.id.days_of_week)
    TextView daysOfWeekTextView;
    @BindView(R.id.minimum_duration)
    EditText minDurationEditText;
    @BindView(R.id.maximum_duration)
    EditText maxDurationEditText;
    @BindView(R.id.time_enabled)
    CheckBox timeEnabledCheckBox;
    @BindView(R.id.start_time_label)
    TextView startTimeLabelTextView;
    @BindView(R.id.start_time)
    TextView startTimeTextView;
    @BindView(R.id.start_window_time_label)
    TextView startWindowTimeLabelTextView;
    @BindView(R.id.start_window_time)
    TextView startWindowTimeTextView;
    @BindView(R.id.start_extra)
    EditText startExtraTimeTextView;
    @BindView(R.id.stop_extra)
    EditText stopExtraTimeTextView;
    @BindView(R.id.duplicate_detection)
    TextView duplicateDetectionTextView;
    @BindView(R.id.duplicate_detection_label)
    TextView duplicateDetectionLabelTextView;
    @BindView(R.id.directory)
    EditText directoryEditText;
    @BindView(R.id.directory_label)
    TextView directoryLabelTextView;
    @BindView(R.id.title)
    EditText titleEditText;
    @BindView(R.id.name)
    EditText nameEditText;
    @BindView(R.id.channel)
    TextView channelNameTextView;
    @BindView(R.id.dvr_config_label)
    TextView recordingProfileLabelTextView;
    @BindView(R.id.priority)
    TextView priorityTextView;
    @BindView(R.id.dvr_config)
    TextView recordingProfileNameTextView;

    private String[] recordingProfilesList;
    private List<Channel> channelList;
    private ServerProfile profile;
    private int recordingProfileNameId;

    private String[] duplicateDetectionList;
    private Unbinder unbinder;
    private String id;
    private SeriesRecording recording;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.series_recording_add_edit_fragment, container, false);
        unbinder = ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        duplicateDetectionList = activity.getResources().getStringArray(R.array.duplicate_detection_list);
        recordingProfilesList = appRepository.getServerProfileData().getRecordingProfileNames();
        profile = appRepository.getServerProfileData().getItemById(serverStatus.getRecordingServerProfileId());
        recordingProfileNameId = RecordingUtils.getSelectedProfileId(profile, recordingProfilesList);
        channelList = appRepository.getChannelData().getItems();

        if (savedInstanceState != null) {
            id = savedInstanceState.getString("id", "");
            recordingProfileNameId = savedInstanceState.getInt("configName");
        } else {
            Bundle bundle = getArguments();
            if (bundle != null) {
                id = bundle.getString("id", "");
            }
        }

        setHasOptionsMenu(true);

        // Get the recoding from the view model without observing it.
        // This allows storing any changes and preserving them during orientation changes.
        SeriesRecordingViewModel viewModel = ViewModelProviders.of(activity).get(SeriesRecordingViewModel.class);
        recording = viewModel.getRecordingByIdSync(id);
        updateUI();

        toolbarInterface.setTitle(!TextUtils.isEmpty(id) ?
                getString(R.string.edit_recording) :
                getString(R.string.add_recording));
    }

    private void updateUI() {
        int gmtOffset = serverStatus.getGmtoffset();

        isEnabledCheckbox.setVisibility(htspVersion >= 19 ? View.VISIBLE : View.GONE);
        isEnabledCheckbox.setChecked(recording.isEnabled());
        titleEditText.setText(recording.getTitle());
        nameEditText.setText(recording.getName());
        directoryLabelTextView.setVisibility(htspVersion >= 19 ? View.VISIBLE : View.GONE);
        directoryEditText.setVisibility(htspVersion >= 19 ? View.VISIBLE : View.GONE);
        directoryEditText.setText(recording.getDirectory());

        channelNameTextView.setText(!TextUtils.isEmpty(recording.getChannelName()) ? recording.getChannelName() : getString(R.string.all_channels));
        channelNameTextView.setOnClickListener(view -> {
            // Determine if the server supports recording on all channels
            boolean allowRecordingOnAllChannels = htspVersion >= 21;
            RecordingUtils.handleChannelListSelection(activity, channelList, allowRecordingOnAllChannels, SeriesRecordingAddEditFragment.this);
        });

        priorityTextView.setText(RecordingUtils.getPriorityName(activity, recording.getPriority()));
        priorityTextView.setOnClickListener(view -> RecordingUtils.handlePrioritySelection(activity, recording.getPriority(), SeriesRecordingAddEditFragment.this));

        if (recordingProfilesList.length == 0) {
            recordingProfileNameTextView.setVisibility(View.GONE);
            recordingProfileLabelTextView.setVisibility(View.GONE);
        } else {
            recordingProfileNameTextView.setVisibility(View.VISIBLE);
            recordingProfileLabelTextView.setVisibility(View.VISIBLE);

            recordingProfileNameTextView.setText(recordingProfilesList[recordingProfileNameId]);
            recordingProfileNameTextView.setOnClickListener(view -> RecordingUtils.handleRecordingProfileSelection(activity, recordingProfilesList, recordingProfileNameId, this));
        }

        startTimeTextView.setText(RecordingUtils.getTimeStringFromTimeInMillis(recording.getStart() - gmtOffset));
        startTimeTextView.setOnClickListener(view -> RecordingUtils.handleTimeSelection(activity, recording.getStart() - gmtOffset, SeriesRecordingAddEditFragment.this, "startTime"));

        startWindowTimeTextView.setText(RecordingUtils.getTimeStringFromTimeInMillis(recording.getStartWindow() - gmtOffset));
        startWindowTimeTextView.setOnClickListener(view -> RecordingUtils.handleTimeSelection(activity, recording.getStartWindow() - gmtOffset, SeriesRecordingAddEditFragment.this, "startWindowTime"));

        startExtraTimeTextView.setText(String.valueOf(recording.getStartExtra()));
        stopExtraTimeTextView.setText(String.valueOf(recording.getStopExtra()));

        daysOfWeekTextView.setText(RecordingUtils.getSelectedDaysOfWeekText(activity, recording.getDaysOfWeek()));
        daysOfWeekTextView.setOnClickListener(view -> RecordingUtils.handleDayOfWeekSelection(activity, recording.getDaysOfWeek(), SeriesRecordingAddEditFragment.this));

        minDurationEditText.setText(recording.getMinDuration() > 0 ? String.valueOf(recording.getMinDuration()) : getString(R.string.duration_sum));
        maxDurationEditText.setText(recording.getMaxDuration() > 0 ? String.valueOf(recording.getMaxDuration()) : getString(R.string.duration_sum));

        timeEnabledCheckBox.setChecked(recording.isTimeEnabled());
        timeEnabledCheckBox.setOnClickListener(view -> {
            boolean checked = timeEnabledCheckBox.isChecked();
            startTimeTextView.setEnabled(checked);
            startTimeTextView.setVisibility(checked ? View.VISIBLE : View.GONE);
            startTimeLabelTextView.setVisibility(checked ? View.VISIBLE : View.GONE);
            startTimeTextView.setText(checked ? RecordingUtils.getTimeStringFromTimeInMillis(recording.getStart()) : "-");
            startWindowTimeTextView.setEnabled(checked);
            startWindowTimeTextView.setVisibility(checked ? View.VISIBLE : View.GONE);
            startWindowTimeLabelTextView.setVisibility(checked ? View.VISIBLE : View.GONE);
            startWindowTimeTextView.setText(checked ? RecordingUtils.getTimeStringFromTimeInMillis(recording.getStartWindow()) : "-");
        });

        duplicateDetectionLabelTextView.setVisibility(htspVersion >= 20 ? View.VISIBLE : View.GONE);
        duplicateDetectionTextView.setVisibility(htspVersion >= 20 ? View.VISIBLE : View.GONE);
        duplicateDetectionTextView.setText(duplicateDetectionList[recording.getDupDetect()]);
        duplicateDetectionTextView.setOnClickListener(view -> handleDuplicateDetectionSelection(duplicateDetectionList, recording.getDupDetect()));
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putString("id", id);
        outState.putInt("configName", recordingProfileNameId);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.save_cancel_options_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                cancel();
                return true;
            case R.id.menu_save:
                save();
                return true;
            case R.id.menu_cancel:
                cancel();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Checks certain given values for plausibility and if everything is fine
     * creates the intent that will be passed to the service to save the newly
     * created recording.
     */
    private void save() {
        if (TextUtils.isEmpty(recording.getTitle())) {
            SnackbarUtils.sendSnackbarMessage(activity, R.string.error_empty_title);
            return;
        }

        // The maximum durationTextView must be at least the minimum durationTextView
        if (recording.getMinDuration() > 0
                && recording.getMaxDuration() > 0
                && recording.getMaxDuration() < recording.getMinDuration()) {
            recording.setMaxDuration(recording.getMinDuration());
        }

        if (!TextUtils.isEmpty(id)) {
            updateSeriesRecording();
        } else {
            addSeriesRecording();
        }
    }

    /**
     * Asks the user to confirm canceling the current activity. If no is
     * chosen the user can continue to add or edit the recording. Otherwise
     * the input will be discarded and the activity will be closed.
     */
    private void cancel() {
        new MaterialDialog.Builder(activity)
                .content(R.string.cancel_add_recording)
                .positiveText(getString(R.string.discard))
                .negativeText(getString(R.string.cancel))
                .onPositive((dialog, which) -> activity.finish())
                .onNegative((dialog, which) -> dialog.cancel())
                .show();
    }

    /**
     * Adds a new series recording with the given values. This method is also
     * called when a recording is being edited. It adds a recording with edited
     * values which was previously removed.
     */
    private void addSeriesRecording() {
        Intent intent = getIntentData();
        intent.setAction("addAutorecEntry");
        activity.startService(intent);
        activity.finish();
    }

    /**
     * Update the series recording with the given values.
     */
    private void updateSeriesRecording() {
        Intent intent = getIntentData();
        intent.setAction("updateAutorecEntry");
        intent.putExtra("id", id);
        activity.startService(intent);
        activity.finish();
    }

    /**
     * Returns an intent with the recording data
     */
    private Intent getIntentData() {
        Intent intent = new Intent(activity, HtspService.class);
        intent.putExtra("title", recording.getTitle());
        intent.putExtra("name", recording.getName());
        intent.putExtra("directory", recording.getDirectory());
        intent.putExtra("minDuration", recording.getMinDuration() * 60);
        intent.putExtra("maxDuration", recording.getMaxDuration() * 60);

        // Assume no start time is specified if 0:00 is selected
        if (recording.isTimeEnabled()) {
            // Pass on minutes not milliseconds
            intent.putExtra("start", RecordingUtils.getMinutesFromTimeInMillis(recording.getStart()));
            intent.putExtra("startWindow", RecordingUtils.getMinutesFromTimeInMillis(recording.getStartWindow()));
        }
        intent.putExtra("startExtra", recording.getStartExtra());
        intent.putExtra("stopExtra", recording.getStopExtra());
        intent.putExtra("dupDetect", recording.getDupDetect());
        intent.putExtra("daysOfWeek", recording.getDaysOfWeek());
        intent.putExtra("priority", recording.getPriority());
        intent.putExtra("enabled", recording.isEnabled() ? 1 : 0);

        if (recording.getChannelId() > 0) {
            intent.putExtra("channelId", recording.getChannelId());
        }

        // Add the recording profile if available and enabled
        if (MiscUtils.isServerProfileEnabled(profile, serverStatus)
                && (recordingProfileNameTextView.getText().length() > 0)) {
            // Use the selected profile. If no change was done in the
            // selection then the default one from the connection setting will be used
            intent.putExtra("configName", recordingProfileNameTextView.getText().toString());
        }
        return intent;
    }

    @Override
    public void onChannelSelected(@NonNull Channel channel) {
        recording.setChannelId(channel.getId());
        channelNameTextView.setText(channel.getName());
    }

    @Override
    public void onPrioritySelected(int which) {
        recording.setPriority(which);
        priorityTextView.setText(RecordingUtils.getPriorityName(activity, recording.getPriority()));
    }

    @Override
    public void onProfileSelected(int which) {
        recordingProfileNameTextView.setText(recordingProfilesList[which]);
        recordingProfileNameId = which;
    }

    @Override
    public void onTimeSelected(long milliSeconds, String tag) {
        if (tag.equals("startTime")) {
            recording.setStart(milliSeconds);
            startTimeTextView.setText(RecordingUtils.getTimeStringFromTimeInMillis(milliSeconds));

            // If the start time is after the start window time,
            // update the start window time with the start value
            if (milliSeconds > recording.getStartWindow()) {
                recording.setStartWindow(milliSeconds);
                startWindowTimeTextView.setText(RecordingUtils.getTimeStringFromTimeInMillis(milliSeconds));
            }

        } else if (tag.equals("startWindowTime")) {
            recording.setStartWindow(milliSeconds);
            startWindowTimeTextView.setText(RecordingUtils.getTimeStringFromTimeInMillis(milliSeconds));

            // If the start window time is before the start time,
            // update the start time with the start window value
            if (milliSeconds < recording.getStart()) {
                recording.setStart(milliSeconds);
                startTimeTextView.setText(RecordingUtils.getTimeStringFromTimeInMillis(milliSeconds));
            }
        }
    }

    @Override
    public void onDateSelected(long milliSeconds, String tag) {
        // NOP
    }

    @Override
    public void onDaysSelected(int selectedDays) {
        recording.setDaysOfWeek(selectedDays);
        daysOfWeekTextView.setText(RecordingUtils.getSelectedDaysOfWeekText(activity, selectedDays));
    }

    private void onDuplicateDetectionValueSelected(int which) {
        recording.setDupDetect(which);
        duplicateDetectionTextView.setText(duplicateDetectionList[which]);
    }

    private void handleDuplicateDetectionSelection(String[] duplicateDetectionList, int duplicateDetectionId) {
        new MaterialDialog.Builder(activity)
                .title(R.string.select_duplicate_detection)
                .items(duplicateDetectionList)
                .itemsCallbackSingleChoice(duplicateDetectionId, (dialog, view, which, text) -> {
                    onDuplicateDetectionValueSelected(which);
                    return true;
                })
                .show();
    }

    @Override
    public void onBackPressed() {
        cancel();
    }

    @OnTextChanged(R.id.title)
    void onTitleTextChanged(CharSequence text, int start, int count, int after) {
        recording.setTitle(text.toString());
    }

    @OnTextChanged(R.id.name)
    void onNameTextChanged(CharSequence text, int start, int count, int after) {
        recording.setName(text.toString());
    }

    @OnTextChanged(R.id.directory)
    void onDirectoryTextChanged(CharSequence text, int start, int count, int after) {
        recording.setDirectory(text.toString());
    }

    @OnCheckedChanged(R.id.is_enabled)
    void onEnabledCheckboxChanged(CompoundButton buttonView, boolean isChecked) {
        recording.setEnabled(isChecked);
    }

    @OnTextChanged(R.id.minimum_duration)
    void onMinDurationTextChanged(CharSequence text, int start, int count, int after) {
        try {
            recording.setMinDuration(Integer.valueOf(text.toString()));
        } catch (NumberFormatException ex) {
            recording.setMinDuration(0);
        }
    }

    @OnTextChanged(R.id.maximum_duration)
    void onMaxDurationTextChanged(CharSequence text, int start, int count, int after) {
        try {
            recording.setMaxDuration(Integer.valueOf(text.toString()));
        } catch (NumberFormatException ex) {
            recording.setMaxDuration(0);
        }
    }

    @OnTextChanged(R.id.start_extra)
    void onStartExtraTextChanged(CharSequence text, int start, int count, int after) {
        try {
            recording.setStartExtra(Long.valueOf(text.toString()));
        } catch (NumberFormatException ex) {
            recording.setStartExtra(2);
        }
    }

    @OnTextChanged(R.id.stop_extra)
    void onStopExtraTextChanged(CharSequence text, int start, int count, int after) {
        try {
            recording.setStopExtra(Long.valueOf(text.toString()));
        } catch (NumberFormatException ex) {
            recording.setStopExtra(2);
        }
    }

    @OnCheckedChanged(R.id.time_enabled)
    void onTimeEnabledCheckboxChanged(CompoundButton buttonView, boolean isChecked) {
        recording.setTimeEnabled(isChecked);
    }
}
