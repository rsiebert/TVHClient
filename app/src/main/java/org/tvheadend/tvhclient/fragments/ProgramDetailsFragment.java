package org.tvheadend.tvhclient.fragments;

import android.app.Activity;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;

import org.tvheadend.tvhclient.Constants;
import org.tvheadend.tvhclient.DataStorage;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.interfaces.HTSListener;
import org.tvheadend.tvhclient.model.Channel;
import org.tvheadend.tvhclient.model.Program;
import org.tvheadend.tvhclient.model.Recording;
import org.tvheadend.tvhclient.tasks.ImageDownloadTask;
import org.tvheadend.tvhclient.tasks.ImageDownloadTaskCallback;
import org.tvheadend.tvhclient.utils.MenuUtils;
import org.tvheadend.tvhclient.utils.Utils;

import java.util.Date;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;

public class ProgramDetailsFragment extends DialogFragment implements HTSListener, ImageDownloadTaskCallback {

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
    private Button playButton;
    private Button recordOnceButton;
    private Button recordSeriesButton;
    private Button recordRemoveButton;

    private Toolbar toolbar;
    private TextView toolbarTitle;
    private View toolbarShadow;
    private TVHClientApplication app;
    private ImageView imageView;
    private DataStorage dataStorage;
    private MenuUtils menuUtils;

    public static ProgramDetailsFragment newInstance(Bundle args) {
        ProgramDetailsFragment f = new ProgramDetailsFragment();
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
        final View v = inflater.inflate(R.layout.program_details_layout, container, false);
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
        toolbarTitle = (TextView) v.findViewById(R.id.toolbar_title);
        toolbarShadow = v.findViewById(R.id.toolbar_shadow);
        imageView = (ImageView) v.findViewById(R.id.image);
        
        // Initialize the player layout
        playerLayout = (LinearLayout) v.findViewById(R.id.player_layout);
        playButton = (Button) v.findViewById(R.id.menu_play);
        recordOnceButton = (Button) v.findViewById(R.id.menu_record_once);
        recordSeriesButton = (Button) v.findViewById(R.id.menu_record_series);
        recordRemoveButton = (Button) v.findViewById(R.id.menu_record_remove);

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        activity = getActivity();
        app = TVHClientApplication.getInstance();
        dataStorage = DataStorage.getInstance();
        menuUtils = new MenuUtils(getActivity());

        if (toolbarTitle != null) {
            toolbarTitle.setVisibility(getDialog() != null ? View.VISIBLE : View.GONE);
        }
        if (toolbarShadow != null) {
            toolbarShadow.setVisibility(getDialog() != null ? View.VISIBLE : View.GONE);
        }

        long channelId = 0;
        long programId = 0;

        Bundle bundle = getArguments();
        if (bundle != null) {
            channelId = bundle.getLong("channelId", 0);
            programId = bundle.getLong("eventId", 0);
            showControls = bundle.getBoolean(Constants.BUNDLE_SHOW_CONTROLS, false);
        }

        // Get the channel of the program
        channel = dataStorage.getChannel(channelId);
        if (channel != null) {
            // Find the program with the given id within this channel so we can
            // show the program details
            CopyOnWriteArrayList<Program> epg = new CopyOnWriteArrayList<>(channel.epg);
            Iterator<Program> it = epg.iterator();
            Program p;
            while (it.hasNext()) {
                p = it.next();
                if (p.id == programId) {
                    program = p;
                    break;
                }
            }
        }

        // If the channel or program is null exit
        if (channel == null || program == null) {
            return;
        }

        if (getDialog() != null && toolbarTitle != null) {
            toolbarTitle.setText(program.title);
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

        // Show the program image if one exists
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        if (app.isUnlocked() && prefs.getBoolean("pref_show_program_artwork", false)) {
            ImageDownloadTask dt = new ImageDownloadTask(this);
            dt.execute(program.image, String.valueOf(program.id));
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

    private boolean onToolbarItemSelected(MenuItem item) {
        if (program == null) {
            return false;
        }

        switch (item.getItemId()) {
            case R.id.menu_search_imdb:
                menuUtils.handleMenuSearchWebSelection(program.title);
                return true;

            case R.id.menu_search_epg:
                menuUtils.handleMenuSearchEpgSelection(program.title);
                return true;

            default:
                return false;
        }
    }

    /**
     * 
     */
    private void showPlayerControls() {
        if (program == null || activity == null) {
            return;
        }

        playButton.setVisibility(View.VISIBLE);
        recordOnceButton.setVisibility(View.VISIBLE);
        recordRemoveButton.setVisibility(View.VISIBLE);

        if (dataStorage.getProtocolVersion() >= Constants.MIN_API_VERSION_SERIES_RECORDINGS) {
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
            recordRemoveButton.setVisibility(View.GONE);
        } else if (program.isRecording()) {
            // Show the cancel menu
            recordOnceButton.setVisibility(View.GONE);
            recordSeriesButton.setVisibility(View.GONE);
            recordRemoveButton.setText(R.string.stop);
        } else if (program.isScheduled()) {
            // Show the cancel and play menu
            recordOnceButton.setVisibility(View.GONE);
            recordSeriesButton.setVisibility(View.GONE);
        } else {
            // Show the delete menu
            recordRemoveButton.setVisibility(View.GONE);
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
                    menuUtils.handleMenuPlaySelection(program.channel.id, -1);
                }
            });
        }
        if (recordOnceButton != null) {
            recordOnceButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    menuUtils.handleMenuRecordSelection(program.id);
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
                    menuUtils.handleMenuSeriesRecordSelection(program.title);
                    if (getDialog() != null) {
                        getDialog().dismiss();
                    }
                }
            });
        }
        if (recordRemoveButton != null) {
            recordRemoveButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Recording rec = program.recording;
                    if (rec != null) {
                        if (rec.isRecording()) {
                            menuUtils.handleMenuStopRecordingSelection(rec.id, rec.title);
                        } else if (rec.isScheduled()) {
                            menuUtils.handleMenuCancelRecordingSelection(rec.id, rec.title);
                        } else {
                            menuUtils.handleMenuRemoveRecordingSelection(rec.id, rec.title);
                        }
                    }
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
        if (action.equals("eventUpdate")
                || action.equals("dvrEntryAdd")
                || action.equals("dvrEntryDelete")
                || action.equals("dvrEntryUpdate")) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    showPlayerControls();
                }
            });
        }
    }

    @Override
    public void notify(Drawable image) {
        if (imageView != null && image != null) {
            imageView.setVisibility(View.VISIBLE);
            imageView.setImageDrawable(image);

            // Get the dimensions of the image so the
            // width / height ratio can be determined
            final float w = image.getIntrinsicWidth();
            final float h = image.getIntrinsicHeight();

            if (h > 0) {
                // Scale the image view so it fits the width of the dialog or fragment root view
                final float scale = h / w;
                final float vw = imageView.getRootView().getWidth() - 128;
                final float vh = vw * scale;
                final LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams((int)vw, (int)vh);
                layoutParams.gravity = Gravity.CENTER;
                imageView.setLayoutParams(layoutParams);
            }
        }
    }
}

    