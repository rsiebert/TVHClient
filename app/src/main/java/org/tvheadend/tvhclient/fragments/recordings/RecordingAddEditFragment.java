package org.tvheadend.tvhclient.fragments.recordings;

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
import org.tvheadend.tvhclient.htsp.HTSService;
import org.tvheadend.tvhclient.model.Channel;
import org.tvheadend.tvhclient.model.Recording;
import org.tvheadend.tvhclient.utils.MenuChannelSelectionCallback;
import org.tvheadend.tvhclient.utils.RecordingDateTimeCallback;
import org.tvheadend.tvhclient.utils.RecordingPriorityCallback;
import org.tvheadend.tvhclient.utils.RecordingProfileCallback;

import java.util.Calendar;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

// TODO hide show layout stuff if editing scheduled or recording recordings
// TODO updated getIntentData according to the previous todo
// TODO preselect the first channel in the list
// TODO orientation change when editing messed up the timeTextView

public class RecordingAddEditFragment extends BaseRecordingAddEditFragment implements MenuChannelSelectionCallback, RecordingDateTimeCallback, RecordingPriorityCallback, RecordingProfileCallback {

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
    private Calendar startTime = Calendar.getInstance();
    private Calendar stopTime = Calendar.getInstance();
    private long startExtra;
    private long stopExtra;
    private boolean isEnabled;
    private int channelId;

    private Unbinder unbinder;
    private int dvrId = 0;
    private boolean isRecording;
    private boolean isScheduled;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.recording_add_layout, container, false);
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

        // Get the values from the recording otherwise use default values
        Channel channel;
        if (dvrId > 0) {
            Recording recording = dataStorage.getRecordingFromArray(dvrId);
            priority = recording.priority;
            startExtra = recording.startExtra;
            stopExtra = recording.stopExtra;
            startTime.setTimeInMillis(recording.start);
            stopTime.setTimeInMillis(recording.stop);
            title = recording.title;
            subtitle = recording.subtitle;
            description = recording.description;
            isEnabled = recording.enabled > 0;
            channelId = recording.channel;
            channel = dataStorage.getChannelFromArray(channelId);
            // If the recording is already being recorded, show only the
            // title, subtitle, description, stop and stop extra field
            isRecording = recording.isRecording();
            // If the recording is scheduled (regular edit mode, hide the channel selection)
            isScheduled = recording.isScheduled();
        } else {
            priority = 2;
            startExtra = 0;
            stopExtra = 0;
            // The start timeTextView is already set during init
            // From this timeTextView add 30 minutes to the stop timeTextView
            stopTime.setTimeInMillis(startTime.getTimeInMillis() + 30 * 60 * 1000);
            title = "";
            subtitle = "";
            description = "";
            isEnabled = true;
            channelId = 0;
            channel = null;
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

        // Restore the values before the orientation change
        if (savedInstanceState != null) {
            priority = savedInstanceState.getInt("priorityTextView");
            startExtra = savedInstanceState.getLong("startExtra");
            stopExtra = savedInstanceState.getLong("stopExtra");
            startTime.setTimeInMillis(savedInstanceState.getLong("startTime"));
            stopTime.setTimeInMillis(savedInstanceState.getLong("stopTime"));
            title = savedInstanceState.getString("title");
            subtitle = savedInstanceState.getString("subtitle");
            description = savedInstanceState.getString("description");
            channelId = savedInstanceState.getInt("channelId");
            recordingProfileName = savedInstanceState.getInt("configName");
            isEnabled = savedInstanceState.getBoolean("isEnabledTextView");
        }

        titleLabelTextView.setVisibility(htspVersion >= 21 ? View.VISIBLE : View.GONE);
        titleEditText.setVisibility(htspVersion >= 21 ? View.VISIBLE : View.GONE);
        titleEditText.setText(title);
        subtitleLabelTextView.setVisibility(htspVersion >= 21 ? View.VISIBLE : View.GONE);
        subtitleEditText.setVisibility(htspVersion >= 21 ? View.VISIBLE : View.GONE);
        subtitleEditText.setText(subtitle);
        descriptionLabelTextView.setVisibility(htspVersion >= 21 ? View.VISIBLE : View.GONE);
        descriptionEditText.setVisibility(htspVersion >= 21 ? View.VISIBLE : View.GONE);
        descriptionEditText.setText(description);

        stopTimeTextView.setText(getTimeStringFromDate(stopTime));
        stopTimeTextView.setOnClickListener(view -> recordingUtils.handleTimeSelection(stopTime, RecordingAddEditFragment.this, "stopTime"));

        stopDateTextView.setText(getDateStringFromDate(stopTime));
        stopDateTextView.setOnClickListener(view -> recordingUtils.handleDateSelection(stopTime, RecordingAddEditFragment.this, "stopDate"));

        stopExtraEditText.setText(String.valueOf(stopExtra));

        if (isScheduled || isRecording) {
            channelNameLabelTextView.setVisibility(View.GONE);
            channelNameTextView.setVisibility(View.GONE);
        } else {
            channelNameTextView.setText(channel != null ? channel.channelName : getString(R.string.no_channel));
            channelNameTextView.setOnClickListener(view -> {
                // Determine if the server supports recording on all channels
                boolean allowRecordingOnAllChannels = htspVersion >= 21;
                menuUtils.handleMenuChannelSelection(channelId, RecordingAddEditFragment.this, allowRecordingOnAllChannels);
            });
        }

        isEnabledCheckbox.setVisibility(htspVersion >= 23 && !isRecording ? View.VISIBLE : View.GONE);
        isEnabledCheckbox.setChecked(isEnabled);

        priorityTextView.setVisibility(!isRecording ? View.VISIBLE : View.GONE);
        priorityTextView.setText(priorityList[priority]);
        priorityTextView.setOnClickListener(view -> recordingUtils.handlePrioritySelection(priorityList, priority, RecordingAddEditFragment.this));

        recordingProfileNameTextView.setVisibility(!isRecording ? View.VISIBLE : View.GONE);
        recordingProfileNameTextView.setText(recordingProfilesList[recordingProfileName]);
        recordingProfileNameTextView.setOnClickListener(view -> recordingUtils.handleRecordingProfileSelection(recordingProfilesList, recordingProfileName, RecordingAddEditFragment.this));

        startTimeTextView.setVisibility(!isRecording ? View.VISIBLE : View.GONE);
        startTimeTextView.setText(getTimeStringFromDate(startTime));
        startTimeTextView.setOnClickListener(view -> recordingUtils.handleTimeSelection(startTime, RecordingAddEditFragment.this, "startTime"));

        startDateTextView.setVisibility(!isRecording ? View.VISIBLE : View.GONE);
        startDateTextView.setText(getDateStringFromDate(startTime));
        startDateTextView.setOnClickListener(view -> recordingUtils.handleDateSelection(startTime, RecordingAddEditFragment.this, "startDate"));

        // Add the additional pre- and post recording values in minutes
        startExtraEditText.setVisibility(!isRecording ? View.VISIBLE : View.GONE);
        startExtraEditText.setText(String.valueOf(startExtra));
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        saveWidgetValuesIntoVariables();
        outState.putInt("priorityTextView", priority);
        outState.putLong("startTime", startTime.getTimeInMillis() / 60 / 1000);
        outState.putLong("stopTime", stopTime.getTimeInMillis() / 60 / 1000);
        outState.putLong("startExtra", startExtra);
        outState.putLong("stopExtra", stopExtra);
        outState.putString("title", title);
        outState.putString("subtitle", subtitle);
        outState.putString("description", description);
        outState.putInt("channelId", channelId);
        outState.putInt("configName", recordingProfileName);
        outState.putBoolean("isEnabledTextView", isEnabled);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.save_cancel_menu, menu);
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
    }

    private void updateRecording() {
        Intent intent = getIntentData();
        intent.setAction("updateDvrEntry");
        intent.putExtra("id", dvrId);
        activity.startService(intent);
    }

    private Intent getIntentData() {
        Intent intent = new Intent(activity, HTSService.class);
        intent.putExtra("title", title);
        intent.putExtra("subtitle", subtitle);
        intent.putExtra("description", description);
        // Pass on seconds not milliseconds
        intent.putExtra("stopTime", (int)(stopTime.getTimeInMillis() / 1000));
        intent.putExtra("stopExtra", stopExtra);

        if (!isScheduled) {
            intent.putExtra("channelId", channelId);
        }
        if (!isRecording) {
            // Pass on seconds not milliseconds
            intent.putExtra("startTime", (int)(startTime.getTimeInMillis() / 1000));
            intent.putExtra("startExtra", startExtra);
            intent.putExtra("priorityTextView", priority);
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
    public void menuChannelSelected(int which) {
        if (which > 0) {
            channelId = which;
            Channel channel = dataStorage.getChannelFromArray(which);
            channelNameTextView.setText(channel.channelName);
        } else {
            channelNameTextView.setText(R.string.all_channels);
        }
    }

    @Override
    public void timeSelected(int hour, int minute, String tag) {
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
    public void dateSelected(int year, int month, int day, String tag) {
        if (tag.equals("startDate")) {
            startTime.set(Calendar.DAY_OF_MONTH, day);
            startTime.set(Calendar.MONTH, month);
            startTime.set(Calendar.YEAR, year);
            startDateTextView.setText(getDateStringFromDate(startTime));
        } else if (tag.equals("stopDate")) {
            stopTime.set(Calendar.DAY_OF_MONTH, day);
            stopTime.set(Calendar.MONTH, month);
            stopTime.set(Calendar.YEAR, year);
            stopTimeTextView.setText(getDateStringFromDate(stopTime));
        }
    }

    @Override
    public void prioritySelected(int which) {
        priorityTextView.setText(priorityList[which]);
        priority = which;
    }

    @Override
    public void profileSelected(int which) {
        recordingProfileNameTextView.setText(recordingProfilesList[which]);
        recordingProfileName = which;
    }
}
