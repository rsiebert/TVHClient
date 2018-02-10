package org.tvheadend.tvhclient.ui.recordings.timer_recordings;

import android.arch.lifecycle.ViewModelProviders;
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
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.entity.Channel;
import org.tvheadend.tvhclient.data.entity.ServerProfile;
import org.tvheadend.tvhclient.data.entity.TimerRecording;
import org.tvheadend.tvhclient.service.EpgSyncService;
import org.tvheadend.tvhclient.ui.common.BackPressedInterface;
import org.tvheadend.tvhclient.ui.recordings.base.BaseRecordingAddEditFragment;
import org.tvheadend.tvhclient.ui.recordings.common.DateTimePickerCallback;
import org.tvheadend.tvhclient.ui.recordings.common.DaysOfWeekSelectionCallback;
import org.tvheadend.tvhclient.ui.recordings.common.RecordingPriorityListCallback;
import org.tvheadend.tvhclient.ui.recordings.common.RecordingProfileListCallback;
import org.tvheadend.tvhclient.utils.ChannelListSelectionCallback;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.OnTextChanged;
import butterknife.Unbinder;

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

    private Unbinder unbinder;
    private String id;
    private TimerRecording recording;
    private int recordingProfileName;

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

        // Get the selected profile from the connection and select it from the recording config list
        ServerProfile profile = profileRepository.getRecordingServerProfile();
        if (profile != null) {
            for (int i = 0; i < recordingProfilesList.length; i++) {
                if (recordingProfilesList[i].equals(profile.getName())) {
                    recordingProfileName = i;
                    break;
                }
            }
        }

        if (savedInstanceState == null) {
            Bundle bundle = getArguments();
            if (bundle != null) {
                id = bundle.getString("id");
            }
        } else {
            id = savedInstanceState.getString("id");
            recordingProfileName = savedInstanceState.getInt("configName");
        }

        // Create the view model that stores the connection model
        TimerRecordingViewModel viewModel = ViewModelProviders.of(activity).get(TimerRecordingViewModel.class);
        recording = viewModel.getRecordingByIdSync(id);
        updateUI();

        toolbarInterface.setTitle(!TextUtils.isEmpty(id) ?
                getString(R.string.edit_recording) :
                getString(R.string.add_recording));
    }

    private void updateUI() {

        isEnabledCheckbox.setVisibility(serverStatus.getHtspVersion() >= 19 ? View.VISIBLE : View.GONE);
        isEnabledCheckbox.setChecked(recording.getEnabled() == 1);
        titleEditText.setText(recording.getTitle());
        nameEditText.setText(recording.getName());

        directoryLabelTextView.setVisibility(serverStatus.getHtspVersion() >= 19 ? View.VISIBLE : View.GONE);
        directoryEditText.setVisibility(serverStatus.getHtspVersion() >= 19 ? View.VISIBLE : View.GONE);
        directoryEditText.setText(recording.getDirectory());

        Channel channel = repository.getChannelByIdSync(recording.getChannelId());
        channelNameTextView.setText(channel != null ? channel.getChannelName() : getString(R.string.all_channels));
        channelNameTextView.setOnClickListener(view -> {
            // Determine if the server supports recording on all channels
            boolean allowRecordingOnAllChannels = serverStatus.getHtspVersion() >= 21;
            handleChannelListSelection(recording.getChannelId(), TimerRecordingAddEditFragment.this, allowRecordingOnAllChannels);
        });

        priorityTextView.setText(priorityList[recording.getPriority()]);
        priorityTextView.setOnClickListener(view -> handlePrioritySelection(priorityList, recording.getPriority(), TimerRecordingAddEditFragment.this));

        if (recordingProfilesList.length == 0) {
            recordingProfileNameTextView.setVisibility(View.GONE);
            recordingProfileLabelTextView.setVisibility(View.GONE);
        } else {
            recordingProfileNameTextView.setVisibility(View.VISIBLE);
            recordingProfileLabelTextView.setVisibility(View.VISIBLE);

            recordingProfileNameTextView.setText(recordingProfileName);
            recordingProfileNameTextView.setOnClickListener(view -> handleRecordingProfileSelection(recordingProfilesList, recordingProfileName, TimerRecordingAddEditFragment.this));
        }

        startTimeTextView.setText(getTimeStringFromTimeInMillis(recording.getStart()));
        startTimeTextView.setOnClickListener(view -> handleTimeSelection(recording.getStart(), TimerRecordingAddEditFragment.this, "startTime"));

        stopTimeTextView.setText(getTimeStringFromTimeInMillis(recording.getStop()));
        stopTimeTextView.setOnClickListener(view -> handleTimeSelection(recording.getStop(), TimerRecordingAddEditFragment.this, "stopTime"));

        daysOfWeekTextView.setText(getSelectedDaysOfWeekText(recording.getDaysOfWeek()));
        daysOfWeekTextView.setOnClickListener(view -> handleDayOfWeekSelection(recording.getDaysOfWeek(), TimerRecordingAddEditFragment.this));
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putString("id", id);
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

    private void save() {
        if (TextUtils.isEmpty(recording.getTitle())) {
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
        intent.putExtra("directory", recording.getDirectory());
        intent.putExtra("title", recording.getTitle());
        intent.putExtra("name", recording.getName());
        intent.putExtra("start", getMinutesFromTimeInMillis(recording.getStart()));
        intent.putExtra("stop", getMinutesFromTimeInMillis(recording.getStop()));
        intent.putExtra("daysOfWeek", recording.getDaysOfWeek());
        intent.putExtra("priority", recording.getPriority());
        intent.putExtra("enabled", recording.getEnabled());

        if (recording.getChannelId() > 0) {
            intent.putExtra("channelId", recording.getChannelId());
        }
        // Add the recording profile if available and enabled
        ServerProfile profile = profileRepository.getRecordingServerProfile();
        if (profile != null && profile.isEnabled()
                && (recordingProfileNameTextView.getText().length() > 0)
                && serverStatus.getHtspVersion() >= 16) {
            // Use the selected profile. If no change was done in the 
            // selection then the default one from the connection setting will be used
            intent.putExtra("configName", recordingProfileNameTextView.getText().toString());
        }
        return intent;
    }

    @Override
    public void onChannelIdSelected(int which) {
        if (which > 0) {
            recording.setChannelId(which);
            Channel channel = repository.getChannelByIdSync(which);
            channelNameTextView.setText(channel.getChannelName());
        } else {
            channelNameTextView.setText(R.string.all_channels);
        }
    }

    @Override
    public void onPrioritySelected(int which) {
        priorityTextView.setText(priorityList[which]);
        recording.setPriority(which);
    }

    @Override
    public void onProfileSelected(int which) {
        recordingProfileNameTextView.setText(recordingProfilesList[which]);
        recordingProfileName = which;
    }

    @Override
    public void onTimeSelected(long milliSeconds, String tag) {
        if (tag.equals("startTime")) {
            recording.setStart(milliSeconds);
            startTimeTextView.setText(getTimeStringFromTimeInMillis(milliSeconds));

        } else if (tag.equals("stopTime")) {
            recording.setStop(milliSeconds);
            stopTimeTextView.setText(getTimeStringFromTimeInMillis(milliSeconds));
        }
    }

    @Override
    public void onDateSelected(long milliSeconds, String tag) {
        // NOP
    }

    @Override
    public void onDaysOfWeekSelected(int selectedDays) {
        recording.setDaysOfWeek(selectedDays);
        daysOfWeekTextView.setText(getSelectedDaysOfWeekText(selectedDays));
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
        recording.setEnabled(isChecked ? 1 : 0);
    }
}
