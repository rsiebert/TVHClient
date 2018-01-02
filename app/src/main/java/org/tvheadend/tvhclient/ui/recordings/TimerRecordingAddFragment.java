package org.tvheadend.tvhclient.ui.recordings;

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
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.service.HTSService;
import org.tvheadend.tvhclient.service.HTSListener;
import org.tvheadend.tvhclient.data.model.Channel;
import org.tvheadend.tvhclient.data.model.TimerRecording;
import org.tvheadend.tvhclient.utils.callbacks.ChannelListSelectionCallback;
import org.tvheadend.tvhclient.utils.callbacks.DateTimePickerCallback;
import org.tvheadend.tvhclient.utils.callbacks.DaysOfWeekSelectionCallback;
import org.tvheadend.tvhclient.utils.callbacks.RecordingPriorityListCallback;
import org.tvheadend.tvhclient.utils.callbacks.RecordingProfileListCallback;

import java.util.Calendar;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

// TODO convert handleStartTimeSelection and handleStopTimeSelection to use calendar

public class TimerRecordingAddFragment extends BaseRecordingAddEditFragment implements HTSListener, ChannelListSelectionCallback, RecordingPriorityListCallback, RecordingProfileListCallback, DateTimePickerCallback, DaysOfWeekSelectionCallback {

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

    private Calendar startTime = Calendar.getInstance();
    private Calendar stopTime = Calendar.getInstance();
    private String directory;
    private String title;
    private String name;
    private boolean isEnabled;
    private int channelId;

    private Unbinder unbinder;
    private String id;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_add_edit_timer_recording, container, false);
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

        // Get the values from the recording otherwise use default values
        Channel channel;
        if (!TextUtils.isEmpty(id)) {
            TimerRecording recording = dataStorage.getTimerRecordingFromArray(id);
            priority = recording.priority;
            startTime.setTimeInMillis(recording.start * 60 * 1000);
            stopTime.setTimeInMillis(recording.stop * 60 * 1000);
            daysOfWeek = recording.daysOfWeek;
            directory = recording.directory;
            title = recording.title;
            name = recording.name;
            isEnabled = (recording.enabled > 0);
            channelId = recording.channel;
            channel = dataStorage.getChannelFromArray(channelId);
        } else {
            Calendar calendar = Calendar.getInstance();
            priority = 2;
            startTime.setTimeInMillis(calendar.getTimeInMillis());
            stopTime.setTimeInMillis(calendar.getTimeInMillis() + 30 * 60 * 1000);
            daysOfWeek = 127;
            directory = "";
            title = "";
            name = "";
            isEnabled = true;
            channelId = 0;
            channel = null;
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

        // Restore the values before the orientation change
        if (savedInstanceState != null) {
            isEnabled = savedInstanceState.getBoolean("isEnabledTextView");
            title = savedInstanceState.getString("title");
            name = savedInstanceState.getString("name");
            channelId = savedInstanceState.getInt("channelId");
            startTime.setTimeInMillis(savedInstanceState.getLong("startTime"));
            stopTime.setTimeInMillis(savedInstanceState.getLong("stopTime"));
            daysOfWeek = savedInstanceState.getInt("daysOfWeekTextView");
            priority = savedInstanceState.getInt("priorityTextView");
            directory = savedInstanceState.getString("directoryTextView");
            recordingProfileName = savedInstanceState.getInt("configName");
        }

        isEnabledCheckbox.setVisibility(htspVersion >= 19 ? View.VISIBLE : View.GONE);
        isEnabledCheckbox.setChecked(isEnabled);
        titleEditText.setText(title);
        nameEditText.setText(name);
        directoryLabelTextView.setVisibility(htspVersion >= 19 ? View.VISIBLE : View.GONE);
        directoryEditText.setVisibility(htspVersion >= 19 ? View.VISIBLE : View.GONE);
        directoryEditText.setText(directory);

        channelNameTextView.setText(channel != null ? channel.channelName : getString(R.string.all_channels));
        channelNameTextView.setOnClickListener(view -> {
            // Determine if the server supports recording on all channels
            boolean allowRecordingOnAllChannels = htspVersion >= 21;
            recordingUtils.handleChannelListSelection(channelId, TimerRecordingAddFragment.this, allowRecordingOnAllChannels);
        });

        priorityTextView.setText(priorityList[priority]);
        priorityTextView.setOnClickListener(view -> recordingUtils.handlePrioritySelection(priorityList, priority, TimerRecordingAddFragment.this));

        if (TextUtils.isEmpty(id) || recordingProfilesList.length == 0) {
            recordingProfileNameTextView.setVisibility(View.GONE);
            recordingProfileLabelTextView.setVisibility(View.GONE);
        } else {
            recordingProfileNameTextView.setVisibility(View.VISIBLE);
            recordingProfileLabelTextView.setVisibility(View.VISIBLE);
        }

        recordingProfileNameTextView.setText(recordingProfilesList[recordingProfileName]);
        recordingProfileNameTextView.setOnClickListener(view -> recordingUtils.handleRecordingProfileSelection(recordingProfilesList, recordingProfileName, TimerRecordingAddFragment.this));

        startTimeTextView.setText(getTimeStringFromDate(startTime));
        startTimeTextView.setOnClickListener(view -> recordingUtils.handleTimeSelection(startTime, TimerRecordingAddFragment.this, "startTime"));

        stopTimeTextView.setText(getTimeStringFromDate(stopTime));
        stopTimeTextView.setOnClickListener(view -> recordingUtils.handleTimeSelection(stopTime, TimerRecordingAddFragment.this, "stopTime"));

        daysOfWeekTextView.setText(getSelectedDaysOfWeek());
        daysOfWeekTextView.setOnClickListener(view -> recordingUtils.handleDayOfWeekSelection(daysOfWeek, TimerRecordingAddFragment.this));
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        saveWidgetValuesIntoVariables();
        outState.putBoolean("isEnabledTextView", isEnabled);
        outState.putString("title", title);
        outState.putString("name", name);
        outState.putInt("channelId", channelId);
        outState.putLong("startTime", startTime.getTimeInMillis() / 60 / 1000);
        outState.putLong("stopTime", stopTime.getTimeInMillis() / 60 / 1000);
        outState.putInt("daysOfWeekTextView", daysOfWeek);
        outState.putInt("priorityTextView", priority);
        outState.putString("directoryTextView", directory);
        outState.putInt("configName", recordingProfileName);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.options_menu_save_cancel, menu);
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

    private void saveWidgetValuesIntoVariables() {
        directory = directoryEditText.getText().toString();
        title = titleEditText.getText().toString();
        name = nameEditText.getText().toString();
        isEnabled = isEnabledCheckbox.isChecked();
    }

    private void save() {
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
                        getActivity().finish();
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
        if (action.equals("timerecEntryDelete")) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    TimerRecording timerRecording = (TimerRecording) obj;
                    if (timerRecording.id.equals(id)) {
                        addTimerRecording();
                    }
                    activity.finish();
                }
            });
        }
    }

    /**
     * Adds a new timer recording with the given values. This method is also
     * called when a recording is being edited. It adds a recording with edited
     * values which was previously removed.
     */
    private void addTimerRecording() {
        Intent intent = getIntentData();
        intent.setAction("addTimerecEntry");
        activity.startService(intent);
    }

    /**
     * Updates the timer recording with the given values.
     * If the API version supports it, use the native service call method
     * otherwise the old recording is removed and a new one with the
     * edited values is added afterwards
     */
    private void updateTimerRecording() {
        if (htspVersion >= 25) {
            // If the API version supports it, use the native service call method
            Intent intent = getIntentData();
            intent.setAction("updateTimerecEntry");
            intent.putExtra("id", id);
            activity.startService(intent);
            activity.finish();
        } else {
            Intent intent = new Intent(activity, HTSService.class);
            intent.setAction("deleteTimerecEntry");
            intent.putExtra("id", id);
            activity.startService(intent);
        }
    }

    /**
     * Returns an intent with the recording data
     */
    private Intent getIntentData() {
        Intent intent = new Intent(activity, HTSService.class);
        intent.putExtra("directory", directory);
        intent.putExtra("title", title);
        intent.putExtra("name", name);
        // Pass on minutes not milliseconds
        intent.putExtra("start", (int)(startTime.getTimeInMillis() / 60 / 1000));
        intent.putExtra("stop", (int)(stopTime.getTimeInMillis() / 60 / 1000));
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
            Channel channel = dataStorage.getChannelFromArray(which);
            channelNameTextView.setText(channel.channelName);
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
    public void onTimeSelected(int hour, int minute, String tag) {
        if (tag.equals("startTime")) {
            startTime.set(Calendar.HOUR_OF_DAY, hour);
            startTime.set(Calendar.MINUTE, minute);
            startTimeTextView.setText(getTimeStringFromDate(startTime));
        } else if (tag.equals("stopTime")) {
            stopTime.set(Calendar.HOUR_OF_DAY, hour);
            stopTime.set(Calendar.MINUTE, minute);
            stopTimeTextView.setText(getTimeStringFromDate(stopTime));
        }
    }

    @Override
    public void onDateSelected(int year, int month, int day, String tag) {
        // NOP
    }

    @Override
    public void onDaysOfWeekSelected(int selectedDays) {
        daysOfWeek = selectedDays;
        daysOfWeekTextView.setText(getSelectedDaysOfWeek());
    }
}
