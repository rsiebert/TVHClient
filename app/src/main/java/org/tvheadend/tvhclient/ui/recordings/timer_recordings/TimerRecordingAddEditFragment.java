package org.tvheadend.tvhclient.ui.recordings.timer_recordings;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.entity.Channel;
import org.tvheadend.tvhclient.data.entity.TimerRecording;
import org.tvheadend.tvhclient.sync.EpgSyncService;
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

// TODO replace savedinstance bundle with viewmodel
// TODO use default recording from viewmodel when no id is available
// TODO use getter and setting in the model from here and the service

public class TimerRecordingAddEditFragment extends BaseRecordingAddEditFragment implements BackPressedInterface, ChannelListSelectionCallback, RecordingPriorityListCallback, RecordingProfileListCallback, DateTimePickerCallback, DaysOfWeekSelectionCallback {
    private String TAG = getClass().getSimpleName();

    @BindView(R.id.is_enabled)
    CheckBox isEnabledCheckbox;
    @BindView(R.id.priority)
    TextView priorityTextView;
    @BindView(R.id.days_of_week)
    TextView daysOfWeekTextView;
    @BindView(R.id.start_time)
    TextView startTimeTextView;
    @BindView(R.id.stop_time)
    TextView stopTimeTextView;
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
    @BindView(R.id.dvr_config)
    TextView recordingProfileNameTextView;
    @BindView(R.id.dvr_config_label)
    TextView recordingProfileLabelTextView;

    private long startTime;
    private long stopTime;
    private String directory;
    private String title;
    private String name;
    private boolean isEnabled;
    private int channelId;

    private Unbinder unbinder;
    private String id;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.timer_recording_add_edit_fragment, container, false);
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

        Bundle bundle = getArguments();
        if (bundle != null) {
            id = bundle.getString("id");
        }

        if (savedInstanceState == null) {
            // Get the values from the recording otherwise use default values
            if (!TextUtils.isEmpty(id)) {
                TimerRecording recording = repository.getTimerRecordingSync(id);
                if (recording != null) {
                    priority = recording.getPriority();
                    startTime = recording.getStart();
                    stopTime = recording.getStop();
                    daysOfWeek = recording.getDaysOfWeek();
                    directory = recording.getDirectory();
                    title = recording.getTitle();
                    name = recording.getName();
                    isEnabled = (recording.getEnabled() > 0);
                    channelId = recording.getChannelId();
                }
                updateUI();
            } else {
                priority = 2;
                Calendar calendar = Calendar.getInstance();
                startTime = calendar.getTimeInMillis();
                // Add another 30 minutes
                stopTime = calendar.getTimeInMillis() + (30 * 60 * 1000);
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
            isEnabled = savedInstanceState.getBoolean("isEnabled");
            title = savedInstanceState.getString("title");
            name = savedInstanceState.getString("name");
            channelId = savedInstanceState.getInt("channelId");
            startTime = savedInstanceState.getLong("startTime");
            stopTime = savedInstanceState.getLong("stopTime");
            daysOfWeek = savedInstanceState.getInt("daysOfWee");
            priority = savedInstanceState.getInt("priority");
            directory = savedInstanceState.getString("directory");
            recordingProfileName = savedInstanceState.getInt("configName");
            updateUI();
        }

        toolbarInterface.setTitle(!TextUtils.isEmpty(id) ?
                getString(R.string.edit_recording) :
                getString(R.string.add_recording));

        // Get the selected profile from the connection
        // and select it from the recording config list
        recordingProfileName = 0;
        if (profile != null) {
            for (int i = 0; i < recordingProfilesList.length; i++) {
                if (recordingProfilesList[i].equals(profile.name)) {
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

        Channel channel = repository.getChannelSync(channelId);
        channelNameTextView.setText(channel != null ? channel.getChannelName() : getString(R.string.all_channels));
        channelNameTextView.setOnClickListener(view -> {
            // Determine if the server supports recording on all channels
            boolean allowRecordingOnAllChannels = htspVersion >= 21;
            handleChannelListSelection(channelId, TimerRecordingAddEditFragment.this, allowRecordingOnAllChannels);
        });

        priorityTextView.setText(priorityList[priority]);
        priorityTextView.setOnClickListener(view -> handlePrioritySelection(priorityList, priority, TimerRecordingAddEditFragment.this));

        if (recordingProfilesList.length == 0) {
            recordingProfileNameTextView.setVisibility(View.GONE);
            recordingProfileLabelTextView.setVisibility(View.GONE);
        } else {
            recordingProfileNameTextView.setVisibility(View.VISIBLE);
            recordingProfileLabelTextView.setVisibility(View.VISIBLE);

            recordingProfileNameTextView.setText(recordingProfileName);
            recordingProfileNameTextView.setOnClickListener(view -> handleRecordingProfileSelection(recordingProfilesList, recordingProfileName, TimerRecordingAddEditFragment.this));
        }

        startTimeTextView.setText(getTimeStringFromTimeInMillis(startTime));
        startTimeTextView.setOnClickListener(view -> handleTimeSelection(startTime, TimerRecordingAddEditFragment.this, "startTime"));

        stopTimeTextView.setText(getTimeStringFromTimeInMillis(stopTime));
        stopTimeTextView.setOnClickListener(view -> handleTimeSelection(stopTime, TimerRecordingAddEditFragment.this, "stopTime"));

        daysOfWeekTextView.setText(getSelectedDaysOfWeek());
        daysOfWeekTextView.setOnClickListener(view -> handleDayOfWeekSelection(daysOfWeek, TimerRecordingAddEditFragment.this));
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        saveWidgetValuesIntoVariables();
        outState.putBoolean("isEnabled", isEnabled);
        outState.putString("title", title);
        outState.putString("name", name);
        outState.putInt("channelId", channelId);
        outState.putLong("startTime", startTime);
        outState.putLong("stopTime", stopTime);
        outState.putInt("daysOfWeek", daysOfWeek);
        outState.putInt("priority", priority);
        outState.putString("directory", directory);
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

    private void saveWidgetValuesIntoVariables() {
        directory = directoryEditText.getText().toString();
        title = titleEditText.getText().toString();
        name = nameEditText.getText().toString();
        isEnabled = isEnabledCheckbox.isChecked();
    }

    private void save() {
        Log.d(TAG, "save() called");
        saveWidgetValuesIntoVariables();

        if (TextUtils.isEmpty(title)) {
            if (activity.getCurrentFocus() != null) {
                Snackbar.make(activity.getCurrentFocus(), getString(R.string.error_empty_title), Toast.LENGTH_SHORT).show();
            }
            return;
        }
        if (!TextUtils.isEmpty(id)) {
            updateTimerRecording();
        } else {
            addTimerRecording();
        }
    }

    private void cancel() {
        // Show confirmation dialog to cancel
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

    /**
     * Adds a new timer recording with the given values. This method is also
     * called when a recording is being edited. It adds a recording with edited
     * values which was previously removed.
     */
    private void addTimerRecording() {
        Log.d(TAG, "addTimerRecording() called");
        Intent intent = getIntentData();
        intent.setAction("addTimerecEntry");
        activity.startService(intent);
        activity.finish();
    }

    /**
     * Updates the timer recording with the given values.
     * If the API version supports it, use the native service call method
     * otherwise the old recording is removed and a new one with the
     * edited values is added afterwards. This is done in the service
     */
    private void updateTimerRecording() {
        Log.d(TAG, "updateTimerRecording() called");
        Intent intent = getIntentData();
        intent.setAction("updateTimerecEntry");
        intent.putExtra("id", id);
        activity.startService(intent);
        activity.finish();
    }

    /**
     * Returns an intent with the recording data
     */
    private Intent getIntentData() {
        Intent intent = new Intent(activity, EpgSyncService.class);
        intent.putExtra("directory", directory);
        intent.putExtra("title", title);
        intent.putExtra("name", name);
        intent.putExtra("start", getMinutesFromTimeInMillis(startTime));
        intent.putExtra("stop", getMinutesFromTimeInMillis(stopTime));
        intent.putExtra("daysOfWeek", daysOfWeek);
        intent.putExtra("priority", priority);
        intent.putExtra("enabled", (isEnabled ? 1 : 0));

        if (channelId > 0) {
            intent.putExtra("channelId", channelId);
        }
        // Add the recording profile if available and enabled
        if (profile != null && profile.enabled
                && (recordingProfileNameTextView.getText().length() > 0)
                && htspVersion >= 16) {
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
            Channel channel = repository.getChannelSync(channelId);
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
        Log.d("Y", "onTimeSelected() called with: milliSeconds = [" + milliSeconds + "], tag = [" + tag + "]");
        if (tag.equals("startTime")) {
            startTime = milliSeconds;
            startTimeTextView.setText(getTimeStringFromTimeInMillis(startTime));

        } else if (tag.equals("stopTime")) {
            stopTime = milliSeconds;
            stopTimeTextView.setText(getTimeStringFromTimeInMillis(stopTime));
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

    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed() called");
        cancel();
    }
}
