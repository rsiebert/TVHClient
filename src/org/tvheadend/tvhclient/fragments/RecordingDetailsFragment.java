package org.tvheadend.tvhclient.fragments;

import org.tvheadend.tvhclient.Constants;
import org.tvheadend.tvhclient.ExternalPlaybackActivity;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.Utils;
import org.tvheadend.tvhclient.interfaces.HTSListener;
import org.tvheadend.tvhclient.model.Recording;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

@SuppressWarnings("deprecation")
public class RecordingDetailsFragment extends DialogFragment implements HTSListener {

    @SuppressWarnings("unused")
    private final static String TAG = RecordingDetailsFragment.class.getSimpleName();

    private ActionBarActivity activity;
    private boolean showControls = false;
    private Recording rec;

    private TextView summaryLabel;
    private TextView summary;
    private TextView descLabel;
    private TextView desc;
    private TextView channelLabel;
    private TextView channelName;
    private TextView date;
    private TextView time;
    private TextView duration;
    private TextView failed_reason;
    private TextView is_series_recording;
    private TextView is_timer_recording;

    private LinearLayout playerLayout;
    private TextView playRecordingButton;
    private TextView editRecordingButton;
    private TextView cancelRecordingButton;
    private TextView removeRecordingButton;

    public static RecordingDetailsFragment newInstance(Bundle args) {
        RecordingDetailsFragment f = new RecordingDetailsFragment();
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
        this.activity = (ActionBarActivity) activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        long recId = 0;
        Bundle bundle = getArguments();
        if (bundle != null) {
            recId = bundle.getLong(Constants.BUNDLE_RECORDING_ID, 0);
            showControls = bundle.getBoolean(Constants.BUNDLE_SHOW_CONTROLS, false);
        }

        // Get the recording so we can show its details 
        TVHClientApplication app = (TVHClientApplication) activity.getApplication();
        rec = app.getRecording(recId);

        // Initialize all the widgets from the layout
        View v = inflater.inflate(R.layout.recording_details_layout, container, false);
        summaryLabel = (TextView) v.findViewById(R.id.summary_label);
        summary = (TextView) v.findViewById(R.id.summary);
        descLabel = (TextView) v.findViewById(R.id.description_label);
        desc = (TextView) v.findViewById(R.id.description);
        channelLabel = (TextView) v.findViewById(R.id.channel_label);
        channelName = (TextView) v.findViewById(R.id.channel);
        date = (TextView) v.findViewById(R.id.date);
        time = (TextView) v.findViewById(R.id.time);
        duration = (TextView) v.findViewById(R.id.duration);
        failed_reason = (TextView) v.findViewById(R.id.failed_reason);
        is_series_recording = (TextView) v.findViewById(R.id.is_series_recording);
        is_timer_recording = (TextView) v.findViewById(R.id.is_timer_recording);
        
        // Initialize the player layout
        playerLayout = (LinearLayout) v.findViewById(R.id.player_layout);
        playRecordingButton = (TextView) v.findViewById(R.id.menu_play);
        editRecordingButton = (TextView) v.findViewById(R.id.menu_edit);
        cancelRecordingButton = (TextView) v.findViewById(R.id.menu_record_cancel);
        removeRecordingButton = (TextView) v.findViewById(R.id.menu_record_remove);

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        
        // If the recording is null exit
        if (rec == null) {
            return;
        }

        if (getDialog() != null) {
            getDialog().getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.dialog_fragment_title);
            TextView dialogTitle = (TextView) getDialog().findViewById(android.R.id.title);
            if (dialogTitle != null) {
                dialogTitle.setText(rec.title);
                dialogTitle.setSingleLine(false);
            }
        }

        // Show the player controls
        if (showControls) {
            addPlayerControlListeners();
        }
        showPlayerControls();

        Utils.setDate(date, rec.start);
        Utils.setTime(time, rec.start, rec.stop);
        Utils.setDuration(duration, rec.start, rec.stop);
        Utils.setProgressText(null, rec.start, rec.stop);
        Utils.setDescription(channelLabel, channelName, ((rec.channel != null) ? rec.channel.name : ""));
        Utils.setDescription(summaryLabel, summary, rec.summary);
        Utils.setDescription(descLabel, desc, rec.description);
        Utils.setFailedReason(failed_reason, rec);

        // Show the information if the recording belongs to a series recording
        // only when no dual pane is active (the controls shall be shown)
        if (is_series_recording != null) {
            if (rec.autorecId != null && showControls) {
                is_series_recording.setVisibility(ImageView.VISIBLE);
            } else {
                is_series_recording.setVisibility(ImageView.GONE);
            }
        }
        if (is_timer_recording != null) {
            if (rec.timerecId != null && showControls) {
                is_timer_recording.setVisibility(ImageView.VISIBLE);
            } else {
                is_timer_recording.setVisibility(ImageView.GONE);
            }
        }
    }

    /**
     * 
     */
    private void showPlayerControls() {
        if (playerLayout != null) {
            playerLayout.setVisibility(showControls ? View.VISIBLE : View.GONE);
        }
        if (playRecordingButton != null) {
            playRecordingButton.setVisibility(View.GONE);
        }
        if (editRecordingButton != null) {
            editRecordingButton.setVisibility(View.GONE);
        }
        if (cancelRecordingButton != null) {
            cancelRecordingButton.setVisibility(View.GONE);
        }
        if (removeRecordingButton != null) {
            removeRecordingButton.setVisibility(View.GONE);
        }

        // Show the play menu items
        if (rec.error == null && rec.state.equals("completed")) {
            // The recording is available, it can be played and removed
            if (removeRecordingButton != null) {
                removeRecordingButton.setVisibility(View.VISIBLE);
            }
            if (playRecordingButton != null) {
                playRecordingButton.setVisibility(View.VISIBLE);
            }
        } else if (rec.isRecording()) {
            // The recording is recording it can be played or cancelled
            if (cancelRecordingButton != null) {
                cancelRecordingButton.setVisibility(View.VISIBLE);
            }
            if (playRecordingButton != null) {
                playRecordingButton.setVisibility(View.VISIBLE);
            }
            TVHClientApplication app = (TVHClientApplication) activity.getApplication();
            if (editRecordingButton != null && app.isUnlocked()) {
                editRecordingButton.setVisibility(View.VISIBLE);
            }
        } else if (rec.isScheduled()) {
            // The recording is scheduled, it can only be cancelled
            if (cancelRecordingButton != null) {
                cancelRecordingButton.setVisibility(View.VISIBLE);
            }
            TVHClientApplication app = (TVHClientApplication) activity.getApplication();
            if (editRecordingButton != null && app.isUnlocked()) {
                editRecordingButton.setVisibility(View.VISIBLE);
            }
        } else if (rec.error != null || rec.state.equals("missed")) {
            // The recording has failed or has been missed, allow removal
            if (removeRecordingButton != null) {
                removeRecordingButton.setVisibility(View.VISIBLE);
            }
        }
    }

    /**
     * 
     */
    private void addPlayerControlListeners() {
        if (playRecordingButton != null) {
            playRecordingButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Open a new activity that starts playing the program
                    if (rec != null) {
                        Intent intent = new Intent(activity, ExternalPlaybackActivity.class);
                        intent.putExtra(Constants.BUNDLE_RECORDING_ID, rec.id);
                        startActivity(intent);
                    }
                }
            });
        }
        if (editRecordingButton != null) {
            editRecordingButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Open a new activity that starts playing the program
                    if (rec != null) {
                        DialogFragment editFragment = RecordingEditFragment.newInstance(null);
                        Bundle bundle = new Bundle();
                        bundle.putLong(Constants.BUNDLE_RECORDING_ID, rec.id);
                        editFragment.setArguments(bundle);
                        editFragment.show(activity.getSupportFragmentManager(), "dialog");
                    }
                    if (getDialog() != null) {
                        getDialog().dismiss();
                    }
                }
            });
        }
        if (cancelRecordingButton != null) {
            cancelRecordingButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Utils.confirmCancelRecording(activity, rec);
                    if (getDialog() != null) {
                        getDialog().dismiss();
                    }
                }
            });
        }
        if (removeRecordingButton != null) {
            removeRecordingButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Utils.confirmRemoveRecording(activity, rec);
                    if (getDialog() != null) {
                        getDialog().dismiss();
                    }
                }
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        TVHClientApplication app = (TVHClientApplication) activity.getApplication();
        app.addListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        TVHClientApplication app = (TVHClientApplication) activity.getApplication();
        app.removeListener(this);
    }

    /**
     * This method is part of the HTSListener interface. Whenever the HTSService
     * sends a new message the correct action will then be executed here.
     */
    @Override
    public void onMessage(String action, Object obj) {
        // An existing recording has been updated, this is valid for all menu options
        if (action.equals(Constants.ACTION_PROGRAM_UPDATE)
                || action.equals(Constants.ACTION_DVR_ADD)
                || action.equals(Constants.ACTION_DVR_DELETE)
                || action.equals(Constants.ACTION_DVR_UPDATE)) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    if (showControls) {
                        showPlayerControls();
                    }
                }
            });
        }
    }
}
