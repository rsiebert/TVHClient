package org.tvheadend.tvhclient.ui.recordings.recordings;

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
import org.tvheadend.tvhclient.data.entity.Channel;
import org.tvheadend.tvhclient.data.entity.Recording;
import org.tvheadend.tvhclient.sync.EpgSyncService;
import org.tvheadend.tvhclient.ui.common.BackPressedInterface;
import org.tvheadend.tvhclient.ui.recordings.base.BaseRecordingAddEditFragment;
import org.tvheadend.tvhclient.ui.recordings.common.DateTimePickerCallback;
import org.tvheadend.tvhclient.ui.recordings.common.RecordingPriorityListCallback;
import org.tvheadend.tvhclient.ui.recordings.common.RecordingProfileListCallback;
import org.tvheadend.tvhclient.utils.callbacks.ChannelListSelectionCallback;

import java.util.Calendar;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

// TODO hide show layout stuff if editing scheduled or recording recordings
// TODO updated getIntentData according to the previous todo
// TODO use contraintlayout
// TODO replace savedinstance bundle with viewmodel
// TODO use default recording from viewmodel when no id is available

public class RecordingAddEditFragment extends BaseRecordingAddEditFragment implements BackPressedInterface, ChannelListSelectionCallback, DateTimePickerCallback, RecordingPriorityListCallback, RecordingProfileListCallback {

    @BindView(R.id.start_time)
    TextView startTimeTextView;
    @BindView(R.id.stop_time)
    TextView stopTimeTextView;
    @BindView(R.id.start_date)
    TextView startDateTextView;
    @BindView(R.id.stop_date)
    TextView stopDateTextView;
    @BindView(R.id.is_enabled)
    CheckBox isEnabledCheckbox;
    @BindView(R.id.start_extra)
    EditText startExtraEditText;
    @BindView(R.id.stop_extra)
    EditText stopExtraEditText;
    @BindView(R.id.title)
    EditText titleEditText;
    @BindView(R.id.title_label)
    TextView titleLabelTextView;
    @BindView(R.id.subtitle)
    EditText subtitleEditText;
    @BindView(R.id.subtitle_label)
    TextView subtitleLabelTextView;
    @BindView(R.id.description)
    EditText descriptionEditText;
    @BindView(R.id.description_label)
    TextView descriptionLabelTextView;
    @BindView(R.id.channel_label)
    TextView channelNameLabelTextView;
    @BindView(R.id.channel)
    TextView channelNameTextView;
    @BindView(R.id.dvr_config_label)
    TextView recordingProfileLabelTextView;
    @BindView(R.id.priority)
    TextView priorityTextView;
    @BindView(R.id.dvr_config)
    TextView recordingProfileNameTextView;

    private String title;
    private String subtitle;
    private String description;
    private long startTime;
    private long stopTime;
    private long startExtra;
    private long stopExtra;
    private boolean isEnabled;
    private int channelId;

    private Unbinder unbinder;
    private int dvrId = 0;
    private boolean isRecording;
    private boolean isScheduled;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.recording_add_edit_fragment, container, false);
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

        // Create the list of available configurations that the user can select from
        recordingProfilesList = new String[dataStorage.getDvrConfigs().size()];
        for (int i = 0; i < dataStorage.getDvrConfigs().size(); i++) {
            recordingProfilesList[i] = dataStorage.getDvrConfigs().get(i).name;
        }

        priorityList = activity.getResources().getStringArray(R.array.dvr_priorities);

        Bundle bundle = getArguments();
        if (bundle != null) {
            dvrId = bundle.getInt("dvrId");
        }

        if (savedInstanceState == null) {
            // Get the values from the recording otherwise use default values
            if (dvrId > 0) {
                Recording recording = repository.getRecordingSync(dvrId);
                if (recording != null) {
                    priority = recording.getPriority();
                    startExtra = recording.getStartExtra();
                    stopExtra = recording.getStopExtra();
                    startTime = recording.getStart();
                    stopTime = recording.getStop();
                    title = recording.getTitle();
                    subtitle = recording.getSubtitle();
                    description = recording.getDescription();
                    isEnabled = recording.getEnabled() > 0;
                    channelId = recording.getChannelId();

                    // If the recording is already being recorded, show only the
                    // title, subtitle, description, stop and stop extra field
                    isRecording = recording.isRecording();
                    // If the recording is scheduled (regular edit mode, hide the channel selection)
                    isScheduled = recording.isScheduled();
                }
                updateUI();
            } else {
                priority = 2;
                startExtra = 0;
                stopExtra = 0;
                Calendar calendar = Calendar.getInstance();
                startTime = calendar.getTimeInMillis();
                // Let the stop time be 30 minutes after the start time
                stopTime = calendar.getTimeInMillis() + (30 * 60 * 1000);
                title = "";
                subtitle = "";
                description = "";
                isEnabled = true;
                channelId = 0;
                updateUI();
            }
        } else {
            // Restore the values before the orientation change
            priority = savedInstanceState.getInt("priority");
            startExtra = savedInstanceState.getLong("startExtra");
            stopExtra = savedInstanceState.getLong("stopExtra");
            startTime = savedInstanceState.getLong("start");
            stopTime = savedInstanceState.getLong("stop");
            title = savedInstanceState.getString("title");
            subtitle = savedInstanceState.getString("subtitle");
            description = savedInstanceState.getString("description");
            channelId = savedInstanceState.getInt("channelId");
            recordingProfileName = savedInstanceState.getInt("configName");
            isEnabled = savedInstanceState.getBoolean("isEnabled");
            updateUI();
        }

        toolbarInterface.setTitle(dvrId > 0 ?
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

        titleLabelTextView.setVisibility(htspVersion >= 21 ? View.VISIBLE : View.GONE);
        titleEditText.setVisibility(htspVersion >= 21 ? View.VISIBLE : View.GONE);
        titleEditText.setText(title);

        subtitleLabelTextView.setVisibility(htspVersion >= 21 ? View.VISIBLE : View.GONE);
        subtitleEditText.setVisibility(htspVersion >= 21 ? View.VISIBLE : View.GONE);
        subtitleEditText.setText(subtitle);

        descriptionLabelTextView.setVisibility(htspVersion >= 21 ? View.VISIBLE : View.GONE);
        descriptionEditText.setVisibility(htspVersion >= 21 ? View.VISIBLE : View.GONE);
        descriptionEditText.setText(description);

        stopTimeTextView.setText(getTimeStringFromTimeInMillis(stopTime));
        stopTimeTextView.setOnClickListener(view -> handleTimeSelection(stopTime, RecordingAddEditFragment.this, "stopTime"));

        stopDateTextView.setText(getDateStringFromTimeInMillis(stopTime));
        stopDateTextView.setOnClickListener(view -> handleDateSelection(stopTime, RecordingAddEditFragment.this, "stopDate"));

        stopExtraEditText.setText(String.valueOf(stopExtra));

        if (isScheduled || isRecording) {
            channelNameLabelTextView.setVisibility(View.GONE);
            channelNameTextView.setVisibility(View.GONE);
        } else {
            Channel channel = repository.getChannelSync(channelId);
            channelNameTextView.setText(channel != null ? channel.getChannelName() : getString(R.string.no_channel));
            channelNameTextView.setOnClickListener(view -> {
                // Determine if the server supports recording on all channels
                boolean allowRecordingOnAllChannels = htspVersion >= 21;
                handleChannelListSelection(channelId, RecordingAddEditFragment.this, allowRecordingOnAllChannels);
            });
        }

        isEnabledCheckbox.setVisibility(htspVersion >= 23 && !isRecording ? View.VISIBLE : View.GONE);
        isEnabledCheckbox.setChecked(isEnabled);

        priorityTextView.setVisibility(!isRecording ? View.VISIBLE : View.GONE);
        priorityTextView.setText(priorityList[priority]);
        priorityTextView.setOnClickListener(view -> handlePrioritySelection(priorityList, priority, RecordingAddEditFragment.this));

        if (recordingProfilesList.length == 0 || isRecording) {
            recordingProfileNameTextView.setVisibility(View.GONE);
            recordingProfileLabelTextView.setVisibility(View.GONE);
        } else {
            recordingProfileNameTextView.setVisibility(View.VISIBLE);
            recordingProfileLabelTextView.setVisibility(View.VISIBLE);

            recordingProfileNameTextView.setText(recordingProfileName);
            recordingProfileNameTextView.setOnClickListener(view -> handleRecordingProfileSelection(recordingProfilesList, recordingProfileName, RecordingAddEditFragment.this));
        }

        startTimeTextView.setVisibility(!isRecording ? View.VISIBLE : View.GONE);
        startTimeTextView.setText(getTimeStringFromTimeInMillis(startTime));
        startTimeTextView.setOnClickListener(view -> handleTimeSelection(startTime, RecordingAddEditFragment.this, "startTime"));

        startDateTextView.setVisibility(!isRecording ? View.VISIBLE : View.GONE);
        startDateTextView.setText(getDateStringFromTimeInMillis(startTime));
        startDateTextView.setOnClickListener(view -> handleDateSelection(startTime, RecordingAddEditFragment.this, "startDate"));

        // Add the additional pre- and post recording values in minutes
        startExtraEditText.setVisibility(!isRecording ? View.VISIBLE : View.GONE);
        startExtraEditText.setText(String.valueOf(startExtra));
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        saveWidgetValuesIntoVariables();
        outState.putInt("priority", priority);
        outState.putLong("start", startTime / 1000);
        outState.putLong("stop", stopTime / 1000);
        outState.putLong("startExtra", startExtra);
        outState.putLong("stopExtra", stopExtra);
        outState.putString("title", title);
        outState.putString("subtitle", subtitle);
        outState.putString("description", description);
        outState.putInt("channelId", channelId);
        outState.putInt("configName", recordingProfileName);
        outState.putBoolean("isEnabled", isEnabled);
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

    /**
     * Retrieves and checks the values from the user input elements and stores
     * them in internal variables. These are used to remember the values during
     * an orientation change or when the recording shall be saved.
     */
    private void saveWidgetValuesIntoVariables() {
        title = titleEditText.getText().toString();
        subtitle = subtitleEditText.getText().toString();
        description = descriptionEditText.getText().toString();
        isEnabled = isEnabledCheckbox.isChecked();
        try {
            startExtra = Long.valueOf(startExtraEditText.getText().toString());
        } catch (NumberFormatException ex) {
            startExtra = 2;
        }
        try {
            stopExtra = Long.valueOf(stopExtraEditText.getText().toString());
        } catch (NumberFormatException ex) {
            stopExtra = 2;
        }
    }

    /**
     * Checks certain given values for plausibility and if everything is fine
     * creates the intent that will be passed to the service to save the newly
     * created recording.
     */
    private void save() {
        saveWidgetValuesIntoVariables();

        if (TextUtils.isEmpty(title) && htspVersion >= 21) {
            if (activity.getCurrentFocus() != null) {
                Snackbar.make(activity.getCurrentFocus(), getString(R.string.error_empty_title), Toast.LENGTH_SHORT).show();
            }
            return;
        }
        if (channelId == 0) {
            if (activity.getCurrentFocus() != null) {
                Snackbar.make(activity.getCurrentFocus(), "Please select a channel", Toast.LENGTH_SHORT).show();
            }
            return;
        }
        if (dvrId > 0) {
            updateRecording();
        } else {
            addRecording();
        }
    }

    private void addRecording() {
        Intent intent = getIntentData();
        intent.setAction("addDvrEntry");
        activity.startService(intent);
        activity.finish();
    }

    private void updateRecording() {
        Intent intent = getIntentData();
        intent.setAction("updateDvrEntry");
        intent.putExtra("id", dvrId);
        activity.startService(intent);
        activity.finish();
    }

    private Intent getIntentData() {
        Intent intent = new Intent(activity, EpgSyncService.class);
        intent.putExtra("title", title);
        intent.putExtra("subtitle", subtitle);
        intent.putExtra("description", description);
        // Pass on seconds not milliseconds
        intent.putExtra("stop", stopTime);
        intent.putExtra("stopExtra", stopExtra);

        if (!isScheduled) {
            intent.putExtra("channelId", channelId);
        }
        if (!isRecording) {
            // Pass on seconds not milliseconds
            intent.putExtra("start", startTime);
            intent.putExtra("startExtra", startExtra);
            intent.putExtra("priority", priority);
            intent.putExtra("enabled", (isEnabled ? 1 : 0));
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

    /**
     * Asks the user to confirm canceling the current activity. If no is
     * chosen the user can continue to add or edit the recording. Otherwise
     * the input will be discarded and the activity will be closed.
     */
    private void cancel() {
        // Show confirmation dialog to cancel
        new MaterialDialog.Builder(activity)
                .content(R.string.cancel_edit_recording)
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
                }).show();
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
    public void onTimeSelected(long milliSeconds, String tag) {
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
        if (tag.equals("startDate")) {
            startTime = milliSeconds;
            startDateTextView.setText(getDateStringFromTimeInMillis(startTime));
        } else if (tag.equals("stopDate")) {
            stopTime = milliSeconds;
            stopDateTextView.setText(getDateStringFromTimeInMillis(stopTime));
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
    public void onBackPressed() {
        cancel();
    }
}
