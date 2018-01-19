package org.tvheadend.tvhclient.ui.recordings.series_recordings;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.data.model.Channel;
import org.tvheadend.tvhclient.data.model.Connection;
import org.tvheadend.tvhclient.data.model.Profile;
import org.tvheadend.tvhclient.data.model.SeriesRecording;
import org.tvheadend.tvhclient.service.HTSListener;
import org.tvheadend.tvhclient.service.HTSService;
import org.tvheadend.tvhclient.ui.common.BackPressedInterface;
import org.tvheadend.tvhclient.ui.recordings.base.BaseRecordingAddEditFragment;
import org.tvheadend.tvhclient.ui.recordings.common.DateTimePickerCallback;
import org.tvheadend.tvhclient.ui.recordings.common.DaysOfWeekSelectionCallback;
import org.tvheadend.tvhclient.ui.recordings.common.RecordingPriorityListCallback;
import org.tvheadend.tvhclient.ui.recordings.common.RecordingProfileListCallback;
import org.tvheadend.tvhclient.utils.callbacks.ChannelListSelectionCallback;

import java.util.Calendar;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class SeriesRecordingAddEditFragment extends BaseRecordingAddEditFragment implements BackPressedInterface, HTSListener, ChannelListSelectionCallback, RecordingPriorityListCallback, RecordingProfileListCallback, DateTimePickerCallback, DaysOfWeekSelectionCallback {

    @BindView(R.id.is_enabled)
    CheckBox isEnabledCheckbox;
    @BindView(R.id.days_of_week)
    TextView daysOfWeekTextView;
    @BindView(R.id.minimum_duration)
    EditText minDurationEditText;
    @BindView(R.id.maximum_duration)
    EditText maxDurationEditText;
    @BindView(R.id.start_time)
    TextView startTimeTextView;
    @BindView(R.id.time_enabled)
    CheckBox timeEnabledCheckBox;
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

    private int minDuration;
    private int maxDuration;
    private long startTime;
    private long startWindowTime;
    private boolean timeEnabled;
    private long startExtraTime;
    private long stopExtraTime;
    private int duplicateDetectionId;
    private String directory;
    private String title;
    private String name;
    private boolean isEnabled;
    private int channelId;

    private String[] duplicateDetectionList;
    private Unbinder unbinder;
    private String id;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
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

        Bundle bundle = getArguments();
        if (bundle != null) {
            id = bundle.getString("id");
        }

        if (savedInstanceState == null) {
            // Get the values from the recording otherwise use default values
            if (!TextUtils.isEmpty(id)) {
                SeriesRecordingViewModel viewModel = ViewModelProviders.of(this).get(SeriesRecordingViewModel.class);
                viewModel.getRecording(id).observe(this, recording -> {
                    if (recording != null) {
                        priority = recording.getPriority();
                        minDuration = (recording.getMinDuration() / 60);
                        maxDuration = (recording.getMaxDuration() / 60);
                        timeEnabled = (recording.getStart() >= 0 || recording.getStartWindow() >= 0);
                        startTime = recording.getStart() * 1000;
                        startWindowTime = recording.getStartWindow();
                        startExtraTime = recording.getStartExtra();
                        stopExtraTime = recording.getStopExtra();
                        duplicateDetectionId = recording.getDupDetect();
                        daysOfWeek = recording.getDaysOfWeek();
                        directory = recording.getDirectory();
                        title = recording.getTitle();
                        name = recording.getName();
                        isEnabled = recording.getEnabled() > 0;
                        channelId = recording.getChannelId();
                    }
                    viewModel.getRecording(id).removeObservers(this);
                    updateUI();
                });
            } else {
                priority = 2;
                minDuration = 0;
                maxDuration = 0;
                timeEnabled = true;
                Calendar calendar = Calendar.getInstance();
                startTime = calendar.getTimeInMillis();
                // Add another 30 minutes
                startWindowTime = calendar.getTimeInMillis() + (30 * 60 * 1000);
                startExtraTime = 2;
                stopExtraTime = 2;
                duplicateDetectionId = 0;
                daysOfWeek = 127;
                directory = "";
                title = "";
                name = "";
                isEnabled = true;
                channelId = 0;
                updateUI();
            }
        } else {
            // Restore the values before the orientation change
            priority = savedInstanceState.getInt("priority");
            minDuration = savedInstanceState.getInt("minDuration");
            maxDuration = savedInstanceState.getInt("maxDuration");
            timeEnabled = savedInstanceState.getBoolean("timeEnabled");
            startTime = savedInstanceState.getLong("startTime");
            startWindowTime = savedInstanceState.getLong("startWindowTime");
            startExtraTime = savedInstanceState.getLong("startExtraTime");
            stopExtraTime = savedInstanceState.getLong("stopExtraTime");
            duplicateDetectionId = savedInstanceState.getInt("duplicateDetectionId");
            daysOfWeek = savedInstanceState.getInt("daysOfWeek");
            directory = savedInstanceState.getString("directory");
            title = savedInstanceState.getString("title");
            name = savedInstanceState.getString("name");
            isEnabled = savedInstanceState.getBoolean("enabled");
            channelId = savedInstanceState.getInt("channelId");
            recordingProfileName = savedInstanceState.getInt("configName");
            updateUI();
        }

        toolbarInterface.setTitle(!TextUtils.isEmpty(id) ?
                getString(R.string.edit_recording) :
                getString(R.string.add_recording));

        // Get the selected profile from the connection
        // and select it from the recording config list
        recordingProfileName = 0;
        final Connection conn = databaseHelper.getSelectedConnection();
        final Profile p = databaseHelper.getProfile(conn.recording_profile_id);
        if (p != null) {
            for (int i = 0; i < recordingProfilesList.length; i++) {
                if (recordingProfilesList[i].equals(p.name)) {
                    recordingProfileName = i;
                    break;
                }
            }
        }
    }

    private void updateUI() {

        isEnabledCheckbox.setVisibility(htspVersion >= 19 ? View.VISIBLE : View.GONE);
        isEnabledCheckbox.setChecked(isEnabled);
        titleEditText.setText(title);
        nameEditText.setText(name);
        directoryLabelTextView.setVisibility(htspVersion >= 19 ? View.VISIBLE : View.GONE);
        directoryEditText.setVisibility(htspVersion >= 19 ? View.VISIBLE : View.GONE);
        directoryEditText.setText(directory);

        Channel channel = dataStorage.getChannelFromArray(channelId);
        channelNameTextView.setText(channel != null ? channel.getChannelName() : getString(R.string.all_channels));
        channelNameTextView.setOnClickListener(view -> {
            // Determine if the server supports recording on all channels
            boolean allowRecordingOnAllChannels = htspVersion >= 21;
            handleChannelListSelection(channelId, SeriesRecordingAddEditFragment.this, allowRecordingOnAllChannels);
        });

        priorityTextView.setText(priorityList[priority]);
        priorityTextView.setOnClickListener(view -> handlePrioritySelection(priorityList, priority, SeriesRecordingAddEditFragment.this));

        if (recordingProfilesList.length == 0) {
            recordingProfileNameTextView.setVisibility(View.GONE);
            recordingProfileLabelTextView.setVisibility(View.GONE);
        } else {
            recordingProfileNameTextView.setVisibility(View.VISIBLE);
            recordingProfileLabelTextView.setVisibility(View.VISIBLE);

            recordingProfileNameTextView.setText(recordingProfileName);
            recordingProfileNameTextView.setOnClickListener(view -> handleRecordingProfileSelection(recordingProfilesList, recordingProfileName, this));
        }

        startTimeTextView.setText(getTimeStringFromTimeInMillis(startTime));
        startTimeTextView.setOnClickListener(view -> handleTimeSelection(startTime, SeriesRecordingAddEditFragment.this, "startTime"));

        startWindowTimeTextView.setText(getTimeStringFromTimeInMillis(startWindowTime));
        startWindowTimeTextView.setOnClickListener(view -> handleTimeSelection(startWindowTime, SeriesRecordingAddEditFragment.this, "startWindowTime"));

        startExtraTimeTextView.setText(String.valueOf(startExtraTime));
        stopExtraTimeTextView.setText(String.valueOf(stopExtraTime));

        daysOfWeekTextView.setText(getSelectedDaysOfWeek());
        daysOfWeekTextView.setOnClickListener(view -> handleDayOfWeekSelection(daysOfWeek, SeriesRecordingAddEditFragment.this));

        minDurationEditText.setText(minDuration > 0 ? String.valueOf(minDuration) : getString(R.string.duration_sum));
        maxDurationEditText.setText(maxDuration > 0 ? String.valueOf(maxDuration) : getString(R.string.duration_sum));

        timeEnabledCheckBox.setChecked(timeEnabled);
        timeEnabledCheckBox.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean checked = timeEnabledCheckBox.isChecked();
                startTimeTextView.setEnabled(checked);
                startTimeTextView.setText(checked ? getTimeStringFromTimeInMillis(startTime) : "-");
                startWindowTimeTextView.setEnabled(checked);
                startWindowTimeTextView.setText(checked ? getTimeStringFromTimeInMillis(startWindowTime) : "-");
            }
        });

        duplicateDetectionLabelTextView.setVisibility(htspVersion >= 20 ? View.VISIBLE : View.GONE);
        duplicateDetectionTextView.setVisibility(htspVersion >= 20 ? View.VISIBLE : View.GONE);
        duplicateDetectionTextView.setText(duplicateDetectionList[duplicateDetectionId]);
        duplicateDetectionTextView.setOnClickListener(view -> handleDuplicateDetectionSelection(duplicateDetectionList, duplicateDetectionId));
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        saveWidgetValuesIntoVariables();
        outState.putInt("priority", priority);
        outState.putInt("minDuration", minDuration);
        outState.putInt("maxDuration", maxDuration);
        outState.putLong("startTime", startTime);
        outState.putLong("startWindowTime", startWindowTime);
        outState.putBoolean("timeEnabled", timeEnabled);
        outState.putLong("startExtraTime", startExtraTime);
        outState.putLong("stopExtraTime", stopExtraTime);
        outState.putInt("duplicateDetectionId", duplicateDetectionId);
        outState.putInt("daysOfWeek", daysOfWeek);
        outState.putString("directory", directory);
        outState.putString("title", title);
        outState.putString("name", name);
        outState.putBoolean("isEnabled", isEnabled);
        outState.putInt("channelId", channelId);
        outState.putInt("configName", recordingProfileName);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.save_cancel_options_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
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

    @Override
    public void onResume() {
        super.onResume();
        TVHClientApplication.getInstance().addListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        TVHClientApplication.getInstance().removeListener(this);
    }

    /**
     * Retrieves and checks the values from the user input elements and stores
     * them in internal variables. These are used to remember the values during
     * an orientation change or when the recording shall be saved.
     */
    private void saveWidgetValuesIntoVariables() {
        try {
            minDuration = Integer.valueOf(minDurationEditText.getText().toString());
        } catch (NumberFormatException ex) {
            minDuration = 0;
        }
        try {
            maxDuration = Integer.valueOf(maxDurationEditText.getText().toString());
        } catch (NumberFormatException ex) {
            maxDuration = 0;
        }

        timeEnabled = timeEnabledCheckBox.isChecked();

        try {
            startExtraTime = Long.valueOf(startExtraTimeTextView.getText().toString());
        } catch (NumberFormatException ex) {
            startExtraTime = 0;
        }
        try {
            stopExtraTime = Long.valueOf(stopExtraTimeTextView.getText().toString());
        } catch (NumberFormatException ex) {
            stopExtraTime = 0;
        }

        directory = directoryEditText.getText().toString();
        title = titleEditText.getText().toString();
        name = nameEditText.getText().toString();
        isEnabled = isEnabledCheckbox.isChecked();
    }

    /**
     * Checks certain given values for plausibility and if everything is fine
     * creates the intent that will be passed to the service to save the newly
     * created recording.
     */
    private void save() {
        saveWidgetValuesIntoVariables();

        if (TextUtils.isEmpty(title)) {
            if (activity.getCurrentFocus() != null) {
                Snackbar.make(activity.getCurrentFocus(), getString(R.string.error_empty_title), Toast.LENGTH_SHORT).show();
            }
            return;
        }

        // The maximum durationTextView must be at least the minimum durationTextView
        if (minDuration > 0 && maxDuration > 0 && maxDuration < minDuration) {
            maxDuration = minDuration;
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
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        activity.finish();
                    }
                })
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        dialog.cancel();
                    }
                })
                .show();
    }

    @Override
    public void onMessage(String action, Object obj) {
        if (action.equals("autorecEntryDelete")) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    SeriesRecording seriesRecording = (SeriesRecording) obj;
                    if (seriesRecording.getId().equals(id)) {
                        addSeriesRecording();
                    }
                }
            });
        }
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
        if (htspVersion >= 25) {
            // If the API version supports it, use the native service call method
            Intent intent = getIntentData();
            intent.setAction("updateAutorecEntry");
            intent.putExtra("id", id);
            activity.startService(intent);
            activity.finish();
        } else {
            // TODO move the update logic into the service
            Intent intent = new Intent(activity, HTSService.class);
            intent.setAction("deleteAutorecEntry");
            intent.putExtra("id", id);
            activity.startService(intent);
        }
    }

    /**
     * Returns an intent with the recording data
     */
    private Intent getIntentData() {
        Intent intent = new Intent(activity, HTSService.class);
        intent.putExtra("title", title);
        intent.putExtra("name", name);
        intent.putExtra("directory", directory);
        intent.putExtra("minDuration", minDuration * 60);
        intent.putExtra("maxDuration", maxDuration * 60);

        // Assume no start time is specified if 0:00 is selected
        if (timeEnabled) {
            // Pass on minutes not milliseconds
            intent.putExtra("start", startTime);
            intent.putExtra("startWindow", startWindowTime);
        }
        intent.putExtra("startExtra", startExtraTime);
        intent.putExtra("stopExtra", stopExtraTime);
        intent.putExtra("dupDetect", duplicateDetectionId);
        intent.putExtra("daysOfWeek", daysOfWeek);
        intent.putExtra("priority", priority);
        intent.putExtra("enabled", (isEnabled ? 1 : 0));

        if (channelId > 0) {
            intent.putExtra("channelId", channelId);
        }
        // Add the recording profile if available and enabled
        if (profile != null && profile.enabled
                && (recordingProfileNameTextView.getText().length() > 0)
                && htspVersion >= 16
                && isUnlocked) {
            // Use the selected profile. If no change was done in the
            // selection then the default one from the connection setting will be used
            intent.putExtra("configName", recordingProfileNameTextView.getText().toString());
        }
        return intent;
    }

    @Override
    public void onChannelIdSelected(int which) {
        if (which > 0) {
            channelId = which;
            Channel channel = dataStorage.getChannelFromArray(which);
            channelNameTextView.setText(channel.getChannelName());
        } else {
            channelNameTextView.setText(R.string.all_channels);
        }
    }

    @Override
    public void onPrioritySelected(int which) {
        priorityTextView.setText(priorityList[which]);
        priority = which;
    }

    @Override
    public void onProfileSelected(int which) {
        recordingProfileNameTextView.setText(recordingProfilesList[which]);
        recordingProfileName = which;
    }

    @Override
    public void onTimeSelected(long milliSeconds, String tag) {
        if (tag.equals("startTime")) {
            startTime = milliSeconds;
            startTimeTextView.setText(getTimeStringFromTimeInMillis(startTime));

        } else if (tag.equals("startWindowTime")) {
            startWindowTime = milliSeconds;
            startWindowTimeTextView.setText(getTimeStringFromTimeInMillis(startWindowTime));
        }
    }

    @Override
    public void onDateSelected(long milliSeconds, String tag) {
        // NOP
    }

    @Override
    public void onDaysOfWeekSelected(int selectedDays) {
        daysOfWeek = selectedDays;
        daysOfWeekTextView.setText(getSelectedDaysOfWeek());
    }

    public void onDuplicateDetectionValueSelected(int which) {
        duplicateDetectionId = which;
        duplicateDetectionTextView.setText(duplicateDetectionList[which]);
    }

    public void handleDuplicateDetectionSelection(String[] duplicateDetectionList, int duplicateDetectionId) {
        new MaterialDialog.Builder(activity)
                .title(R.string.select_duplicate_detection)
                .items(duplicateDetectionList)
                .itemsCallbackSingleChoice(duplicateDetectionId, new MaterialDialog.ListCallbackSingleChoice() {
                    @Override
                    public boolean onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                        onDuplicateDetectionValueSelected(which);
                        return true;
                    }
                })
                .show();
    }

    @Override
    public void onBackPressed() {
        cancel();
    }
}
