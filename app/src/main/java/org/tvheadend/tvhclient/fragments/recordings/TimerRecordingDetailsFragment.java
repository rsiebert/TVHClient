package org.tvheadend.tvhclient.fragments.recordings;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.ActionBarActivity;
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
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.Utils;
import org.tvheadend.tvhclient.model.TimerRecording;

import java.util.Calendar;
import java.util.Date;

@SuppressWarnings("deprecation")
public class TimerRecordingDetailsFragment extends DialogFragment {

    @SuppressWarnings("unused")
    private final static String TAG = TimerRecordingDetailsFragment.class.getSimpleName();

    private ActionBarActivity activity;
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
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = (ActionBarActivity) activity;
        app = (TVHClientApplication) activity.getApplication();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        String recId = "";
        Bundle bundle = getArguments();
        if (bundle != null) {
            recId = bundle.getString(Constants.BUNDLE_TIMER_RECORDING_ID);
            showControls = bundle.getBoolean(Constants.BUNDLE_SHOW_CONTROLS, false);
        }

        // Get the recording so we can show its details 
        trec = app.getTimerRecording(recId);

        // Initialize all the widgets from the layout
        View v = inflater.inflate(R.layout.timer_recording_details_layout, container, false);
        channelName = (TextView) v.findViewById(R.id.channel);
        isEnabled = (TextView) v.findViewById(R.id.is_enabled);
        directoryLabel = (TextView) v.findViewById(R.id.directory_label);
        directory = (TextView) v.findViewById(R.id.directory);
        time = (TextView) v.findViewById(R.id.time);
        duration = (TextView) v.findViewById(R.id.duration);
        daysOfWeek = (TextView) v.findViewById(R.id.days_of_week);
        priority = (TextView) v.findViewById(R.id.priority);
        toolbar = (Toolbar) v.findViewById(R.id.toolbar);
        toolbarTitle = (TextView) v.findViewById(R.id.toolbar_title);
        toolbarShadow = v.findViewById(R.id.toolbar_shadow);

        // Initialize the player layout
        playerLayout = (LinearLayout) v.findViewById(R.id.player_layout);
        recordRemoveButton = (Button) v.findViewById(R.id.menu_record_remove);
        recordEditButton = (Button) v.findViewById(R.id.menu_record_edit);

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        
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

        isEnabled.setVisibility((app.getProtocolVersion() >= Constants.MIN_API_VERSION_REC_FIELD_ENABLED) ? View.VISIBLE : View.GONE);
        isEnabled.setText(trec.enabled ? R.string.recording_enabled : R.string.recording_disabled);

        directoryLabel.setVisibility(app.getProtocolVersion() >= Constants.MIN_API_VERSION_REC_FIELD_DIRECTORY ? View.VISIBLE : View.GONE);
        directory.setVisibility(app.getProtocolVersion() >= Constants.MIN_API_VERSION_REC_FIELD_DIRECTORY ? View.VISIBLE : View.GONE);
        directory.setText(trec.directory);

        if (trec.channel != null) {
            channelName.setText(trec.channel.name);
        } else {
            channelName.setText(R.string.all_channels);
        }

        Utils.setDaysOfWeek(activity, null, daysOfWeek, trec.daysOfWeek);

        String[] priorityItems = getResources().getStringArray(R.array.dvr_priorities);
        if (trec.priority >= 0 && trec.priority < priorityItems.length) {
            priority.setText(priorityItems[(int) (trec.priority)]);
        }

        // TODO multiple uses, consolidate
        Calendar startTime = Calendar.getInstance();
        startTime.set(Calendar.HOUR_OF_DAY, (int) (trec.start / 60));
        startTime.set(Calendar.MINUTE, (int) (trec.start % 60));

        Calendar endTime = Calendar.getInstance();
        endTime.set(Calendar.HOUR_OF_DAY, (int) (trec.stop / 60));
        endTime.set(Calendar.MINUTE, (int) (trec.stop % 60));

        Utils.setTime(time, new Date(startTime.getTimeInMillis()), new Date(endTime.getTimeInMillis()));
        duration.setText(getString(R.string.minutes, (int) (trec.stop - trec.start)));
    }

    /**
     * 
     */
    private void addPlayerControlListeners() {
        recordRemoveButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Utils.confirmRemoveRecording(activity, trec);
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
                bundle.putString(Constants.BUNDLE_TIMER_RECORDING_ID, trec.id);
                editFragment.setArguments(bundle);
                editFragment.show(activity.getSupportFragmentManager(), "dialog");

                if (getDialog() != null) {
                    getDialog().dismiss();
                }
            }
        });
    }
}
