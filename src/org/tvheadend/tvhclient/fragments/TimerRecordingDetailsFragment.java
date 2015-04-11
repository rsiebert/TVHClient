package org.tvheadend.tvhclient.fragments;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.tvheadend.tvhclient.Constants;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.Utils;
import org.tvheadend.tvhclient.model.TimerRecording;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;

public class TimerRecordingDetailsFragment extends DialogFragment {

    @SuppressWarnings("unused")
    private final static String TAG = TimerRecordingDetailsFragment.class.getSimpleName();

    private Activity activity;
    private boolean showControls = false;
    private TimerRecording trec;

    private TextView isEnabled;
    private TextView time;
    private TextView duration;
    private TextView daysOfWeek;
    private TextView channelName;
    private TextView priority;

    private LinearLayout playerLayout;
    private TextView recordRemove;

    public static TimerRecordingDetailsFragment newInstance(Bundle args) {
        TimerRecordingDetailsFragment f = new TimerRecordingDetailsFragment();
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getDialog() != null) {
            getDialog().requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
            getDialog().getWindow().getAttributes().windowAnimations = R.style.dialog_animation_fade;
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = (Activity) activity;
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
        TVHClientApplication app = (TVHClientApplication) activity.getApplication();
        trec = app.getTimerRecording(recId);

        // Initialize all the widgets from the layout
        View v = inflater.inflate(R.layout.timer_recording_details_layout, container, false);
        channelName = (TextView) v.findViewById(R.id.channel);
        isEnabled = (TextView) v.findViewById(R.id.is_enabled);
        time = (TextView) v.findViewById(R.id.time);
        duration = (TextView) v.findViewById(R.id.duration);
        daysOfWeek = (TextView) v.findViewById(R.id.days_of_week);
        priority = (TextView) v.findViewById(R.id.priority);

        // Initialize the player layout
        playerLayout = (LinearLayout) v.findViewById(R.id.player_layout);
        recordRemove = (TextView) v.findViewById(R.id.menu_record_remove);
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        
        // If the recording is null exit
        if (trec == null) {
            return;
        }

        if (getDialog() != null) {
            getDialog().getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.dialog_fragment_title);
            TextView dialogTitle = (TextView) getDialog().findViewById(android.R.id.title);
            if (dialogTitle != null) {
                dialogTitle.setText(trec.title);
                dialogTitle.setSingleLine(false);
            }
        }
        // Show the player controls
        if (showControls) {
            addPlayerControlListeners();
        }
        showPlayerControls();

        if (isEnabled != null) {
            TVHClientApplication app = (TVHClientApplication) activity.getApplication();
            isEnabled.setVisibility((app.getProtocolVersion() >= 18) ? View.VISIBLE : View.GONE);
            if (trec.enabled) {
                isEnabled.setText(R.string.recording_enabled);
            } else {
                isEnabled.setText(R.string.recording_disabled);
            }
        }
        if (channelName != null && trec.channel != null) {
            channelName.setText(trec.channel.name);
        }

        Utils.setDaysOfWeek(activity, null, daysOfWeek, trec.daysOfWeek);

        if (priority != null) {
            String[] priorityItems = getResources().getStringArray(R.array.dvr_priorities);
            if (trec.priority >= 0 && trec.priority < priorityItems.length) {
                priority.setText(priorityItems[(int) (trec.priority)]);
            }
        }
        if (time != null) {
            SimpleDateFormat formatter = new SimpleDateFormat("HH:mm", Locale.US);
            String start = formatter.format(new Date(trec.start * 60L * 1000L));
            String stop = formatter.format(new Date(trec.stop * 60L * 1000L));
            time.setText(getString(R.string.from_to_time, start, stop));
        }
        if (duration != null) {
            duration.setText(getString(R.string.minutes, (int) (trec.stop - trec.start)));
        }
    }

    /**
     * 
     */
    private void showPlayerControls() {
        playerLayout.setVisibility(showControls ? View.VISIBLE : View.GONE);
        recordRemove.setVisibility(View.VISIBLE);
    }

    /**
     * 
     */
    private void addPlayerControlListeners() {
        recordRemove.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Utils.confirmRemoveRecording(activity, trec);
                if (getDialog() != null) {
                    getDialog().dismiss();
                }
            }
        });
    }
}
