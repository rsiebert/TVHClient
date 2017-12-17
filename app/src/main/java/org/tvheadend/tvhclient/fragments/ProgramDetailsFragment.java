package org.tvheadend.tvhclient.fragments;

import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;

import org.tvheadend.tvhclient.DataStorage;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.activities.ToolbarInterfaceLight;
import org.tvheadend.tvhclient.model.Channel;
import org.tvheadend.tvhclient.model.Program;
import org.tvheadend.tvhclient.model.Recording;
import org.tvheadend.tvhclient.tasks.ImageDownloadTask;
import org.tvheadend.tvhclient.tasks.ImageDownloadTaskCallback;
import org.tvheadend.tvhclient.utils.MenuUtils;
import org.tvheadend.tvhclient.utils.Utils;

import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class ProgramDetailsFragment extends Fragment implements ImageDownloadTaskCallback {

    @Nullable
    @BindView(R.id.state) ImageView state;
    @BindView(R.id.title) TextView title;
    @BindView(R.id.title_label) TextView titleLabel;
    @BindView(R.id.summary_label) TextView summaryLabel;
    @BindView(R.id.summary) TextView summary;
    @BindView(R.id.description_label) TextView descLabel;
    @BindView(R.id.description) TextView desc;
    @BindView(R.id.channel_label) TextView channelLabel;
    @BindView(R.id.channel) TextView channelName;
    @BindView(R.id.date) TextView date;
    @BindView(R.id.time) TextView time;
    @BindView(R.id.duration) TextView duration;
    @BindView(R.id.progress) TextView progress;
    @BindView(R.id.content_type_label) TextView contentTypeLabel;
    @BindView(R.id.content_type) TextView contentType;
    @BindView(R.id.series_info_label) TextView seriesInfoLabel;
    @BindView(R.id.series_info) TextView seriesInfo;
    @BindView(R.id.star_rating_label) TextView ratingBarLabel;
    @BindView(R.id.star_rating_text) TextView ratingBarText;
    @BindView(R.id.star_rating) RatingBar ratingBar;
    @Nullable
    @BindView(R.id.nested_toolbar) Toolbar nestedToolbar;
    @Nullable
    @BindView(R.id.image) ImageView imageView;

    private int eventId;
    private boolean isUnlocked;
    private int htspVersion;
    private Program program;
    private MenuUtils menuUtils;
    private Unbinder unbinder;
    private ToolbarInterfaceLight toolbarInterface;

    public static ProgramDetailsFragment newInstance(int eventId) {
        ProgramDetailsFragment f = new ProgramDetailsFragment();
        Bundle args = new Bundle();
        args.putInt("eventId", eventId);
        f.setArguments(args);
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.recording_details_fragment, container, false);
        ViewStub stub = view.findViewById(R.id.stub);
        stub.setLayoutResource(R.layout.program_details_content_fragment);
        stub.inflate();
        unbinder = ButterKnife.bind(this, view);
        return view;
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (getActivity() instanceof ToolbarInterfaceLight) {
            toolbarInterface = (ToolbarInterfaceLight) getActivity();
            toolbarInterface.setTitle("Details");
        }
        menuUtils = new MenuUtils(getActivity());
        isUnlocked = TVHClientApplication.getInstance().isUnlocked();
        htspVersion = DataStorage.getInstance().getProtocolVersion();
        setHasOptionsMenu(true);

        Bundle bundle = getArguments();
        if (bundle != null) {
            eventId = bundle.getInt("eventId", 0);
        }
        if (savedInstanceState != null) {
            eventId = savedInstanceState.getInt("eventId", 0);
        }
        // Get the recording so we can show its details
        program = DataStorage.getInstance().getProgramFromArray(eventId);
        Channel channel = DataStorage.getInstance().getChannelFromArray(program.channelId);

        if (nestedToolbar != null) {
            nestedToolbar.inflateMenu(R.menu.program_toolbar_menu);
            nestedToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    return onOptionsItemSelected(menuItem);
                }
            });
        }

        // Show the program information        
        Utils.setState(getActivity(), state, program);
        Utils.setDate(date, program.start);
        Utils.setTime(time, program.start, program.stop);
        Utils.setDuration(duration, program.start, program.stop);
        Utils.setProgressText(progress, program.start, program.stop);
        Utils.setDescription(descLabel, desc, program.description);
        Utils.setDescription(titleLabel, title, program.title);
        Utils.setDescription(summaryLabel, summary, program.summary);

        Utils.setDescription(channelLabel, channelName, channel.channelName);
        Utils.setDescription(descLabel, desc, program.description);
        Utils.setSeriesInfo(getContext(), seriesInfoLabel, seriesInfo, program);
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
        if (isUnlocked && prefs.getBoolean("pref_show_program_artwork", false)) {
            ImageDownloadTask dt = new ImageDownloadTask(this);
            dt.execute(program.image, String.valueOf(program.eventId));
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

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        if (nestedToolbar != null) {
            menu = nestedToolbar.getMenu();
        }
        // Show the play menu item when the current
        // time is between the program start and end time
        long currentTime = new Date().getTime();
        if (currentTime > program.start && currentTime < program.stop) {
            menu.findItem(R.id.menu_play).setVisible(true);
        }

        Recording recording = DataStorage.getInstance().getRecordingFromArray(program.dvrId);
        if (recording == null || (!recording.isRecording()
                && !recording.isScheduled())) {
            menu.findItem(R.id.menu_record_once).setVisible(true);
            menu.findItem(R.id.menu_record_once_custom_profile).setVisible(isUnlocked);
            menu.findItem(R.id.menu_record_series).setVisible(htspVersion >= 13);

        } else if (recording.isCompleted()) {
            menu.findItem(R.id.menu_record_remove).setVisible(true);
            menu.findItem(R.id.menu_play).setVisible(true);

        } else if (recording.isScheduled() && !recording.isRecording()) {
            menu.findItem(R.id.menu_record_remove).setVisible(true);

        } else if (recording.isRecording()) {
            menu.findItem(R.id.menu_record_stop).setVisible(true);

        } else if (recording.isFailed() || recording.isRemoved() || recording.isMissed() || recording.isAborted()) {
            menu.findItem(R.id.menu_record_remove).setVisible(true);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("eventId", eventId);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        if (nestedToolbar == null) {
            inflater.inflate(R.menu.program_context_menu, menu);
        } else {
            inflater.inflate(R.menu.search_info_menu, menu);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                getActivity().finish();
                return true;
            case R.id.menu_record_remove:
                Recording rec = DataStorage.getInstance().getRecordingFromArray(program.dvrId);
                if (rec != null) {
                    if (rec.isRecording()) {
                        menuUtils.handleMenuStopRecordingSelection(rec.id, rec.title);
                    } else if (rec.isScheduled()) {
                        menuUtils.handleMenuCancelRecordingSelection(rec.id, rec.title);
                    } else {
                        menuUtils.handleMenuRemoveRecordingSelection(rec.id, rec.title);
                    }
                }
                return true;
            case R.id.menu_record_once:
                menuUtils.handleMenuRecordSelection(program.eventId);
                return true;
            case R.id.menu_record_once_custom_profile:
                menuUtils.handleMenuCustomRecordSelection(program.eventId, program.channelId);
                return true;
            case R.id.menu_record_series:
                menuUtils.handleMenuSeriesRecordSelection(program.title);
                return true;
            case R.id.menu_play:
                menuUtils.handleMenuPlaySelection(program.channelId, -1);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public int getShownEventId() {
        return eventId;
    }
}

    