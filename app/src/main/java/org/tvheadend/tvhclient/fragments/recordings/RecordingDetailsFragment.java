package org.tvheadend.tvhclient.fragments.recordings;

import android.app.Activity;
import android.app.Dialog;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.tvheadend.tvhclient.Constants;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.Utils;
import org.tvheadend.tvhclient.intent.DownloadIntent;
import org.tvheadend.tvhclient.intent.PlayIntent;
import org.tvheadend.tvhclient.intent.SearchEPGIntent;
import org.tvheadend.tvhclient.intent.SearchIMDbIntent;
import org.tvheadend.tvhclient.interfaces.HTSListener;
import org.tvheadend.tvhclient.model.Recording;

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
    private TextView subtitleLabel;
    private TextView subtitle;
    private TextView channelLabel;
    private TextView channelName;
    private TextView date;
    private TextView time;
    private TextView duration;
    private TextView failed_reason;
    private TextView is_series_recording;
    private TextView is_timer_recording;
    private TextView isEnabled;

    private LinearLayout playerLayout;
    private Button playRecordingButton;
    private Button editRecordingButton;
    private Button removeRecordingButton;
    private Button downloadRecordingButton;

    private Toolbar toolbar;
    private TextView toolbarTitle;
    private View toolbarShadow;
    private TVHClientApplication app;

    private TextView episode;
    private TextView episodeLabel;
    private TextView comment;
    private TextView commentLabel;
    private TextView subscription_error;
    private TextView stream_errors;
    private TextView data_errors;
    private TextView data_size;
    private TextView statusLabel;

    public static RecordingDetailsFragment newInstance(Bundle args) {
        RecordingDetailsFragment f = new RecordingDetailsFragment();
        f.setArguments(args);
        return f;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getDialog() != null) {
            getDialog().getWindow().getAttributes().windowAnimations = R.style.dialog_animation_fade;
            setStyle(DialogFragment.STYLE_NO_TITLE, Utils.getThemeId(activity));
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

        long recId = 0;
        Bundle bundle = getArguments();
        if (bundle != null) {
            recId = bundle.getLong(Constants.BUNDLE_RECORDING_ID, 0);
            showControls = bundle.getBoolean(Constants.BUNDLE_SHOW_CONTROLS, false);
        }

        // Get the recording so we can show its details 
        rec = app.getRecording(recId);

        // Initialize all the widgets from the layout
        View v = inflater.inflate(R.layout.recording_details_layout, container, false);
        summaryLabel = (TextView) v.findViewById(R.id.summary_label);
        summary = (TextView) v.findViewById(R.id.summary);
        descLabel = (TextView) v.findViewById(R.id.description_label);
        desc = (TextView) v.findViewById(R.id.description);
        subtitleLabel = (TextView) v.findViewById(R.id.subtitle_label);
        subtitle = (TextView) v.findViewById(R.id.subtitle);
        channelLabel = (TextView) v.findViewById(R.id.channel_label);
        channelName = (TextView) v.findViewById(R.id.channel);
        date = (TextView) v.findViewById(R.id.date);
        time = (TextView) v.findViewById(R.id.time);
        duration = (TextView) v.findViewById(R.id.duration);
        failed_reason = (TextView) v.findViewById(R.id.failed_reason);
        is_series_recording = (TextView) v.findViewById(R.id.is_series_recording);
        is_timer_recording = (TextView) v.findViewById(R.id.is_timer_recording);
        isEnabled = (TextView) v.findViewById(R.id.is_enabled);
        toolbar = (Toolbar) v.findViewById(R.id.toolbar);
        toolbarTitle = (TextView) v.findViewById(R.id.toolbar_title);
        toolbarShadow = v.findViewById(R.id.toolbar_shadow);

        episode = (TextView) v.findViewById(R.id.episode);
        episodeLabel = (TextView) v.findViewById(R.id.episode_label);
        comment = (TextView) v.findViewById(R.id.comment);
        commentLabel = (TextView) v.findViewById(R.id.comment_label);
        statusLabel = (TextView) v.findViewById(R.id.status_label);
        subscription_error = (TextView) v.findViewById(R.id.subscription_error);
        stream_errors = (TextView) v.findViewById(R.id.stream_errors);
        data_errors = (TextView) v.findViewById(R.id.data_errors);
        data_size = (TextView) v.findViewById(R.id.data_size);

        // Initialize the player layout
        playerLayout = (LinearLayout) v.findViewById(R.id.player_layout);
        playRecordingButton = (Button) v.findViewById(R.id.menu_play);
        editRecordingButton = (Button) v.findViewById(R.id.menu_edit);
        removeRecordingButton = (Button) v.findViewById(R.id.menu_record_remove);
        downloadRecordingButton = (Button) v.findViewById(R.id.menu_download);

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        
        // If the recording is null exit
        if (rec == null) {
            return;
        }

        if (toolbar != null) {
            toolbar.setVisibility(getDialog() != null ? View.VISIBLE : View.GONE);
        }
        if (toolbarShadow != null) {
            toolbarShadow.setVisibility(getDialog() != null ? View.VISIBLE : View.GONE);
        }
        if (getDialog() != null && toolbarTitle != null) {
            toolbarTitle.setText(rec.title);
        }

        // Show the player controls
        if (showControls) {
            addPlayerControlListeners();
            playerLayout.setVisibility(View.VISIBLE);
            showPlayerControls();
        }

        Utils.setDate(date, rec.start);
        Utils.setTime(time, rec.start, rec.stop);
        Utils.setDuration(duration, rec.start, rec.stop);
        Utils.setProgressText(null, rec.start, rec.stop);
        Utils.setDescription(channelLabel, channelName, ((rec.channel != null) ? rec.channel.name : ""));
        Utils.setDescription(summaryLabel, summary, rec.summary);
        Utils.setDescription(descLabel, desc, rec.description);
        Utils.setDescription(subtitleLabel, subtitle, rec.subtitle);
        Utils.setDescription(episodeLabel, episode, rec.episode);
        Utils.setDescription(commentLabel, comment, rec.comment);
        Utils.setFailedReason(failed_reason, rec);

        // Show the information if the recording belongs to a series recording
        // only when no dual pane is active (the controls shall be shown)
        is_series_recording.setVisibility((rec.autorecId != null && showControls) ? ImageView.VISIBLE : ImageView.GONE);
        is_timer_recording.setVisibility((rec.timerecId != null && showControls) ? ImageView.VISIBLE : ImageView.GONE);

        isEnabled.setVisibility((app.getProtocolVersion() >= Constants.MIN_API_VERSION_DVR_FIELD_ENABLED && !rec.enabled) ? View.VISIBLE : View.GONE);
        isEnabled.setText(rec.enabled ? R.string.recording_enabled : R.string.recording_disabled);

        // Only show the status details in the 
        // completed and failed details screens
        if (!rec.isScheduled()) {
            if (rec.subscriptionError != null && rec.subscriptionError.length() > 0) {
                subscription_error.setVisibility(View.VISIBLE);
                subscription_error.setText(getResources().getString(
                        R.string.subscription_error, rec.subscriptionError));
            } else {
                subscription_error.setVisibility(View.GONE);
            }
    
            stream_errors.setText(getResources().getString(R.string.stream_errors, rec.streamErrors));
            data_errors.setText(getResources().getString(R.string.data_errors, rec.dataErrors));
    
            if (rec.dataSize > 1048576) {
                data_size.setText(getResources().getString(R.string.data_size, rec.dataSize / 1048576, "MB"));
            } else {
                data_size.setText(getResources().getString(R.string.data_size, rec.dataSize / 1024, "KB"));
            }
        } else {
            statusLabel.setVisibility(View.GONE);
            subscription_error.setVisibility(View.GONE);
            stream_errors.setVisibility(View.GONE);
            data_errors.setVisibility(View.GONE);
            data_size.setVisibility(View.GONE);
        }

        if (getDialog() != null && Build.VERSION.SDK_INT >= 21) {
            // Inflate a menu to be displayed in the toolbar
            toolbar.inflateMenu(R.menu.search_info_menu);

            // Set an OnMenuItemClickListener to handle menu item clicks
            toolbar.setOnMenuItemClickListener(
                    new Toolbar.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            return onToolbarItemSelected(item);
                        }
                    });
        }
    }

    /**
     *
     * @param item
     * @return
     */
    private boolean onToolbarItemSelected(MenuItem item) {
        if (rec == null) {
            return false;
        }

        switch (item.getItemId()) {
            case R.id.menu_search_imdb:
                startActivity(new SearchIMDbIntent(activity, rec.title));
                return true;

            case R.id.menu_search_epg:
                startActivity(new SearchEPGIntent(activity, rec.title));
                return true;

            default:
                return false;
        }
    }

    /**
     * Shows certain menu items depending on the recording state, the server
     * capabilities and if the application is unlocked
     */
    private void showPlayerControls() {
        // Hide all buttons as a default
        playRecordingButton.setVisibility(View.GONE);
        editRecordingButton.setVisibility(View.GONE);
        removeRecordingButton.setVisibility(View.GONE);
        downloadRecordingButton.setVisibility(View.GONE);

        // TODO use Objects.equals(Object, Object) for comparing strings
        if (rec.isCompleted()) {
            // The recording is available, it can be played and removed
            removeRecordingButton.setVisibility(View.VISIBLE);
            playRecordingButton.setVisibility(View.VISIBLE);
            if (app.isUnlocked()) {
                downloadRecordingButton.setVisibility(View.VISIBLE);
            }

            // The recording is recording it can be played or cancelled
            removeRecordingButton.setText(getString(R.string.stop));
            removeRecordingButton.setVisibility(View.VISIBLE);
            playRecordingButton.setVisibility(View.VISIBLE);
            if (app.isUnlocked()) {
                editRecordingButton.setVisibility(View.VISIBLE);
            }
        } else if (rec.isScheduled()) {
            // The recording is scheduled, it can only be cancelled
            removeRecordingButton.setVisibility(View.VISIBLE);
            if (app.isUnlocked()) {
                editRecordingButton.setVisibility(View.VISIBLE);
            }
        } else if (rec.isMissed() || rec.isFailed() || rec.isAborted() || rec.isRemoved()) {
            // The recording has failed or has been missed, allow removing it
            removeRecordingButton.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 
     */
    private void addPlayerControlListeners() {
        playRecordingButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // Open a new activity that starts playing the program
                startActivity(new PlayIntent(activity, rec));
            }
        });
        editRecordingButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // Open a new activity that starts playing the program
                if (rec != null) {
                    DialogFragment editFragment = RecordingAddFragment.newInstance();
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
        removeRecordingButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (rec != null && rec.isRecording()) {
                    Utils.confirmStopRecording(activity, rec);
                } else if (rec != null && rec.isScheduled()) {
                    Utils.confirmCancelRecording(activity, rec);
                } else {
                    Utils.confirmRemoveRecording(activity, rec);
                }
                if (getDialog() != null) {
                    getDialog().dismiss();
                }
            }
        });
        downloadRecordingButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new DownloadIntent(activity, rec));
                if (getDialog() != null) {
                    getDialog().dismiss();
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        app.addListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
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
