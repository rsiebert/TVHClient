package org.tvheadend.tvhclient.fragments;

import java.util.Date;

import org.tvheadend.tvhclient.Constants;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.Utils;
import org.tvheadend.tvhclient.intent.PlayIntent;
import org.tvheadend.tvhclient.interfaces.HTSListener;
import org.tvheadend.tvhclient.model.Channel;
import org.tvheadend.tvhclient.model.Program;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;

import com.gc.materialdesign.views.ButtonFlat;

public class ProgramDetailsFragment extends DialogFragment implements HTSListener {

    @SuppressWarnings("unused")
    private final static String TAG = ProgramDetailsFragment.class.getSimpleName();

    private Activity activity;
    private boolean showControls = false;
    private Program program;
    private Channel channel;

    private ImageView state;
    private TextView summaryLabel;
    private TextView summary;
    private TextView descLabel;
    private TextView desc;
    private TextView channelLabel;
    private TextView channelName;
    private TextView date;
    private TextView time;
    private TextView duration;
    private TextView progress;
    private TextView contentTypeLabel;
    private TextView contentType;
    private TextView seriesInfoLabel;
    private TextView seriesInfo;
    private TextView ratingBarLabel;
    private TextView ratingBarText;
    private RatingBar ratingBar;

    private LinearLayout playerLayout;
    private ButtonFlat playButton;
    private ButtonFlat recordOnceButton;
    private ButtonFlat recordSeriesButton;
    private ButtonFlat recordCancelButton;

    private Toolbar toolbar;
    private View toolbarShadow;
    private TVHClientApplication app;

    public static ProgramDetailsFragment newInstance(Bundle args) {
        ProgramDetailsFragment f = new ProgramDetailsFragment();
        f.setArguments(args);
        return f;
    }

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
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = (Activity) activity;
        app = (TVHClientApplication) activity.getApplication();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        long channelId = 0;
        long programId = 0;

        Bundle bundle = getArguments();
        if (bundle != null) {
            channelId = bundle.getLong(Constants.BUNDLE_CHANNEL_ID, 0);
            programId = bundle.getLong(Constants.BUNDLE_PROGRAM_ID, 0);
            showControls = bundle.getBoolean(Constants.BUNDLE_SHOW_CONTROLS, false);
        }
        
        // Get the channel of the program
        channel = app.getChannel(channelId);
        if (channel != null) {
            // Find the program with the given id within this channel so we can
            // show the program details
            for (Program p : channel.epg) {
                if (p != null && p.id == programId) {
                    program = p;
                    break;
                }
            }
        }

        // Initialize all the widgets from the layout
        View v = inflater.inflate(R.layout.program_details_layout, container, false);
        state = (ImageView) v.findViewById(R.id.state);
        summaryLabel = (TextView) v.findViewById(R.id.summary_label);
        summary = (TextView) v.findViewById(R.id.summary);
        descLabel = (TextView) v.findViewById(R.id.description_label);
        desc = (TextView) v.findViewById(R.id.description);
        channelLabel = (TextView) v.findViewById(R.id.channel_label);
        channelName = (TextView) v.findViewById(R.id.channel);
        date = (TextView) v.findViewById(R.id.date);
        time = (TextView) v.findViewById(R.id.time);
        duration = (TextView) v.findViewById(R.id.duration);
        progress = (TextView) v.findViewById(R.id.progress);
        contentTypeLabel = (TextView) v.findViewById(R.id.content_type_label);
        contentType = (TextView) v.findViewById(R.id.content_type);
        seriesInfoLabel = (TextView) v.findViewById(R.id.series_info_label);
        seriesInfo = (TextView) v.findViewById(R.id.series_info);
        ratingBarLabel = (TextView) v.findViewById(R.id.star_rating_label);
        ratingBarText = (TextView) v.findViewById(R.id.star_rating_text);
        ratingBar = (RatingBar) v.findViewById(R.id.star_rating);
        toolbar = (Toolbar) v.findViewById(R.id.toolbar);
        toolbarShadow = v.findViewById(R.id.toolbar_shadow);
        
        // Initialize the player layout
        playerLayout = (LinearLayout) v.findViewById(R.id.player_layout);
        playButton = (ButtonFlat) v.findViewById(R.id.menu_play);
        recordOnceButton = (ButtonFlat) v.findViewById(R.id.menu_record_once);
        recordSeriesButton = (ButtonFlat) v.findViewById(R.id.menu_record_series);
        recordCancelButton = (ButtonFlat) v.findViewById(R.id.menu_record_cancel);

        int bgColor = (Utils.getThemeId(activity) == R.style.CustomTheme_Light) ? getResources()
                .getColor(R.color.button_text_color_light) : getResources()
                .getColor(R.color.button_text_color_dark);
        playButton.setBackgroundColor(bgColor);
        recordOnceButton.setBackgroundColor(bgColor);
        recordSeriesButton.setBackgroundColor(bgColor);
        recordCancelButton.setBackgroundColor(bgColor);
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // If the channel or program is null exit
        if (channel == null || program == null) {
            return;
        }

        toolbar.setVisibility(getDialog() != null ? View.VISIBLE : View.GONE);
        toolbarShadow.setVisibility(getDialog() != null ? View.VISIBLE : View.GONE);
        if (getDialog() != null) {
            toolbar.setTitle(program.title);
        }

        // Show the player controls
        playerLayout.setVisibility(showControls ? View.VISIBLE : View.GONE);
        if (showControls) {
            addPlayerControlListeners();
            showPlayerControls();
        }

        // Show the program information        
        Utils.setState(activity, state, program);
        Utils.setDate(date, program.start);
        Utils.setTime(time, program.start, program.stop);
        Utils.setDuration(duration, program.start, program.stop);
        Utils.setProgressText(progress, program.start, program.stop);
        Utils.setDescription(descLabel, desc, program.description);
        Utils.setDescription(summaryLabel, summary, program.summary);
        Utils.setDescription(channelLabel, channelName, channel.name);
        Utils.setDescription(descLabel, desc, program.description);
        Utils.setSeriesInfo(seriesInfoLabel, seriesInfo, program.seriesInfo);
        Utils.setContentType(contentTypeLabel, contentType, program.contentType);
        
        // Show the rating information as starts
        if (program.starRating < 0) {
            ratingBarLabel.setVisibility(View.GONE);
            ratingBarText.setVisibility(View.GONE);
            ratingBar.setVisibility(View.GONE);
        } else {
            ratingBar.setRating((float)program.starRating / 10.0f);
            String value = "(" + program.starRating + "/" + 100 + ")";
            ratingBarText.setText(value);
        }
    }

    /**
     * 
     */
    private void showPlayerControls() {
        if (program == null) {
            return;
        }

        playButton.setVisibility(View.VISIBLE);
        recordOnceButton.setVisibility(View.VISIBLE);
        recordCancelButton.setVisibility(View.VISIBLE);

        if (app.getProtocolVersion() >= Constants.MIN_API_VERSION_SERIES_RECORDINGS) {
            recordSeriesButton.setVisibility(View.VISIBLE);
        } else {
            recordSeriesButton.setVisibility(View.GONE);
        }

        // Show the play menu item when the current 
        // time is between the program start and end time
        long currentTime = new Date().getTime();
        if (program.start != null && program.stop != null
                && currentTime > program.start.getTime()
                && currentTime < program.stop.getTime()) {
            playButton.setVisibility(View.VISIBLE);
        } else {
            playButton.setVisibility(View.GONE);
        }

        if (program.recording == null) {
            // Show the record menu
            recordCancelButton.setVisibility(View.GONE);
        } else if (program.isRecording()) {
            // Show the cancel menu
            recordOnceButton.setVisibility(View.GONE);
            recordSeriesButton.setVisibility(View.GONE);
        } else if (program.isScheduled()) {
            // Show the cancel and play menu
            recordOnceButton.setVisibility(View.GONE);
            recordSeriesButton.setVisibility(View.GONE);
        } else {
            // Show the delete menu
            recordOnceButton.setVisibility(View.GONE);
            recordSeriesButton.setVisibility(View.GONE);
            recordCancelButton.setVisibility(View.GONE);
        }
    }

    /**
     * 
     */
    private void addPlayerControlListeners() {
        if (playButton != null) {
            playButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new PlayIntent(activity, program));
                }
            });
        }
        if (recordOnceButton != null) {
            recordOnceButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Utils.recordProgram(activity, program, false);
                    if (getDialog() != null) {
                        getDialog().dismiss();
                    }
                }
            });
        }
        if (recordSeriesButton != null) {
            recordSeriesButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Utils.recordProgram(activity, program, true);
                    if (getDialog() != null) {
                        getDialog().dismiss();
                    }
                }
            });
        }
        if (recordCancelButton != null) {
            recordCancelButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Utils.confirmCancelRecording(activity, program.recording);
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
        app.addListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        app.removeListener(this);
    }

    @Override
    public void onMessage(String action, Object obj) {
        // An existing program has been updated, this is valid for all menu options. 
        if (action.equals(Constants.ACTION_PROGRAM_UPDATE)
                || action.equals(Constants.ACTION_DVR_ADD)
                || action.equals(Constants.ACTION_DVR_DELETE)
                || action.equals(Constants.ACTION_DVR_UPDATE)) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    showPlayerControls();
                }
            });
        }
    }
}

    