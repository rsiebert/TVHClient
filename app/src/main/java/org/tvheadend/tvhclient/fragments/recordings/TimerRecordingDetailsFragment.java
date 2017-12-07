package org.tvheadend.tvhclient.fragments.recordings;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.tvheadend.tvhclient.Constants;
import org.tvheadend.tvhclient.DataStorage;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.model.Channel;
import org.tvheadend.tvhclient.model.TimerRecording;
import org.tvheadend.tvhclient.utils.MenuUtils;
import org.tvheadend.tvhclient.utils.Utils;

import java.util.Calendar;

public class TimerRecordingDetailsFragment extends DialogFragment {

    @SuppressWarnings("unused")
    private final static String TAG = TimerRecordingDetailsFragment.class.getSimpleName();

    private AppCompatActivity activity;
    private boolean showControls = false;
    private TimerRecording trec;

    private TextView isEnabled;
    private TextView directoryLabel;
    private TextView directory;
    private TextView time;
    private TextView duration;
    private TextView daysOfWeek;
    private TextView channelName;
    private TextView priority;

    private LinearLayout playerLayout;
    private Button recordRemoveButton;
    private Button recordEditButton;

    private Toolbar toolbar;
    private TextView toolbarTitle;
    private View toolbarShadow;
    private TVHClientApplication app;
    private DataStorage dataStorage;
    private MenuUtils menuUtils;

    public static TimerRecordingDetailsFragment newInstance(Bundle args) {
        TimerRecordingDetailsFragment f = new TimerRecordingDetailsFragment();
        f.setArguments(args);
        return f;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        if (dialog.getWindow() != null) {
            dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        }
        return dialog;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().getAttributes().windowAnimations = R.style.dialog_animation_fade;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        // Initialize all the widgets from the layout
        View v = inflater.inflate(R.layout.timer_recording_details_layout, container, false);
        channelName = v.findViewById(R.id.channel);
        isEnabled = v.findViewById(R.id.is_enabled);
        directoryLabel = v.findViewById(R.id.directory_label);
        directory = v.findViewById(R.id.directory);
        time = v.findViewById(R.id.time);
        duration = v.findViewById(R.id.duration);
        daysOfWeek = v.findViewById(R.id.days_of_week);
        priority = v.findViewById(R.id.priority);
        toolbar = v.findViewById(R.id.toolbar);
        toolbarTitle = v.findViewById(R.id.toolbar_title);
        toolbarShadow = v.findViewById(R.id.toolbar_shadow);

        // Initialize the player layout
        playerLayout = v.findViewById(R.id.player_layout);
        recordRemoveButton = v.findViewById(R.id.menu_record_remove);
        recordEditButton = v.findViewById(R.id.menu_record_edit);

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        activity = (AppCompatActivity) getActivity();
        app = TVHClientApplication.getInstance();
        dataStorage = DataStorage.getInstance();
        menuUtils = new MenuUtils(getActivity());

        String recId = "";
        Bundle bundle = getArguments();
        if (bundle != null) {
            recId = bundle.getString("id");
            showControls = bundle.getBoolean(Constants.BUNDLE_SHOW_CONTROLS, false);
        }

        // Get the recording so we can show its details
        trec = dataStorage.getTimerRecordingFromArray(recId);

        // If the recording is null exit
        if (trec == null) {
            return;
        }

        if (toolbar != null) {
            toolbar.setVisibility(getDialog() != null ? View.VISIBLE : View.GONE);
        }
        if (toolbarShadow != null) {
            toolbarShadow.setVisibility(getDialog() != null ? View.VISIBLE : View.GONE);
        }
        if (getDialog() != null && toolbarTitle != null) {
            if (trec.title != null && trec.title.length() > 0) {
                toolbarTitle.setText(trec.title);
            } else {
                toolbarTitle.setText(trec.name);
            }
        }

        // Show the player controls
        if (showControls) {
            addPlayerControlListeners();
            playerLayout.setVisibility(View.VISIBLE);
            recordRemoveButton.setVisibility(View.VISIBLE);
            recordEditButton.setVisibility(View.VISIBLE);
        }

        isEnabled.setVisibility((dataStorage.getProtocolVersion() >= Constants.MIN_API_VERSION_REC_FIELD_ENABLED) ? View.VISIBLE : View.GONE);
        isEnabled.setText(trec.enabled > 0 ? R.string.recording_enabled : R.string.recording_disabled);

        directoryLabel.setVisibility(dataStorage.getProtocolVersion() >= Constants.MIN_API_VERSION_REC_FIELD_DIRECTORY ? View.VISIBLE : View.GONE);
        directory.setVisibility(dataStorage.getProtocolVersion() >= Constants.MIN_API_VERSION_REC_FIELD_DIRECTORY ? View.VISIBLE : View.GONE);
        directory.setText(trec.directory);

        Channel channel = dataStorage.getChannelFromArray(trec.channel);
        if (channel != null) {
            channelName.setText(channel.channelName);
        } else {
            channelName.setText(R.string.all_channels);
        }

        Utils.setDaysOfWeek(activity, null, daysOfWeek, trec.daysOfWeek);

        String[] priorityItems = getResources().getStringArray(R.array.dvr_priorities);
        if (trec.priority >= 0 && trec.priority < priorityItems.length) {
            priority.setText(priorityItems[trec.priority]);
        }

        // TODO multiple uses, consolidate
        Calendar startTime = Calendar.getInstance();
        startTime.set(Calendar.HOUR_OF_DAY, trec.start / 60);
        startTime.set(Calendar.MINUTE, trec.start % 60);

        Calendar endTime = Calendar.getInstance();
        endTime.set(Calendar.HOUR_OF_DAY, trec.stop / 60);
        endTime.set(Calendar.MINUTE, trec.stop % 60);

        Utils.setTime2(time, startTime.getTimeInMillis(), endTime.getTimeInMillis());
        duration.setText(getString(R.string.minutes, (int) (trec.stop - trec.start)));
    }

    /**
     * 
     */
    private void addPlayerControlListeners() {
        recordRemoveButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                final String name = (trec.name != null && trec.name.length() > 0) ? trec.name : "";
                final String title = trec.title != null ? trec.title : "";
                menuUtils.handleMenuRemoveTimerRecordingSelection(trec.id, (name.length() > 0 ? name : title));
                if (getDialog() != null) {
                    getDialog().dismiss();
                }
            }
        });
        recordEditButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // Create the fragment and show it as a dialog.
                DialogFragment editFragment = TimerRecordingAddFragment.newInstance();
                Bundle bundle = new Bundle();
                bundle.putString("id", trec.id);
                editFragment.setArguments(bundle);
                editFragment.show(activity.getSupportFragmentManager(), "dialog");

                if (getDialog() != null) {
                    getDialog().dismiss();
                }
            }
        });
    }
}
