package org.tvheadend.tvhclient.ui.programs;

import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
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

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.data.DataStorage;
import org.tvheadend.tvhclient.data.model.Channel;
import org.tvheadend.tvhclient.data.model.Program;
import org.tvheadend.tvhclient.data.model.Recording;
import org.tvheadend.tvhclient.data.tasks.ImageDownloadTask;
import org.tvheadend.tvhclient.data.tasks.ImageDownloadTaskCallback;
import org.tvheadend.tvhclient.ui.base.ToolbarInterface;
import org.tvheadend.tvhclient.utils.MenuUtils;
import org.tvheadend.tvhclient.utils.UIUtils;

import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

// TODO improve layout, more info, title, subtitle,... without header label
// TODO update icons (same color, record with profile must differ from regular record...)

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
    private ToolbarInterface toolbarInterface;

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
        stub.setLayoutResource(R.layout.program_details_fragment_contents);
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
        if (getActivity() instanceof ToolbarInterface) {
            toolbarInterface = (ToolbarInterface) getActivity();
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
            nestedToolbar.inflateMenu(R.menu.program_details_toolbar_menu);
            nestedToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    return onOptionsItemSelected(menuItem);
                }
            });
        }

        // Show the program information
        if (state != null) {
            Drawable drawable = UIUtils.getRecordingState(getActivity(), program.dvrId);
            state.setVisibility(drawable != null ? View.VISIBLE : View.GONE);
            state.setImageDrawable(drawable);
        }

        String timeStr = UIUtils.getTime(getContext(), program.start) + " - " + UIUtils.getTime(getContext(), program.stop);
        time.setText(timeStr);
        date.setText(UIUtils.getDate(getContext(), program.start));

        String durationTime = getString(R.string.minutes, (int) ((program.stop - program.start) / 1000 / 60));
        duration.setText(durationTime);

        String progressText = UIUtils.getProgressText(getContext(), program.start, program.stop);
        progress.setVisibility(!TextUtils.isEmpty(progressText) ? View.VISIBLE : View.GONE);
        progress.setText(progressText);

        titleLabel.setVisibility(!TextUtils.isEmpty(program.title) ? View.VISIBLE : View.GONE);
        title.setVisibility(!TextUtils.isEmpty(program.title) ? View.VISIBLE : View.GONE);
        title.setText(program.title);

        summaryLabel.setVisibility(!TextUtils.isEmpty(program.summary) ? View.VISIBLE : View.GONE);
        summary.setVisibility(!TextUtils.isEmpty(program.summary) ? View.VISIBLE : View.GONE);
        summary.setText(program.summary);

        descLabel.setVisibility(!TextUtils.isEmpty(program.description) ? View.VISIBLE : View.GONE);
        desc.setVisibility(!TextUtils.isEmpty(program.description) ? View.VISIBLE : View.GONE);
        desc.setText(program.description);

        channelLabel.setVisibility(!TextUtils.isEmpty(channel.channelName) ? View.VISIBLE : View.GONE);
        channelName.setVisibility(!TextUtils.isEmpty(channel.channelName) ? View.VISIBLE : View.GONE);
        channelName.setText(channel.channelName);

        String seriesInfoText = UIUtils.getSeriesInfo(getContext(), program);
        if (TextUtils.isEmpty(seriesInfoText)) {
            seriesInfoLabel.setVisibility(View.GONE);
            seriesInfo.setVisibility(View.GONE);
        } else {
            seriesInfo.setText(seriesInfoText);
        }

        String ct = UIUtils.getContentTypeText(getContext(), program.contentType);
        if (TextUtils.isEmpty(ct)) {
            contentTypeLabel.setVisibility(View.GONE);
            contentType.setVisibility(View.GONE);
        } else {
            contentType.setText(ct);
        }
        
        // Show the rating information as starts
        if (program.starRating < 0) {
            ratingBarLabel.setVisibility(View.GONE);
            ratingBarText.setVisibility(View.GONE);
            ratingBar.setVisibility(View.GONE);
        } else {
            ratingBar.setRating((float)program.starRating / 10.0f);
            String value = " (" + program.starRating + "/" + 10 + ")";
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
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("eventId", eventId);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        if (nestedToolbar == null) {
            inflater.inflate(R.menu.program_details_options_menu, menu);
        } else {
            inflater.inflate(R.menu.external_search_options_menu, menu);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Recording recording;
        switch (item.getItemId()) {
            case android.R.id.home:
                getActivity().finish();
                return true;
            case R.id.menu_record_remove:
                recording = DataStorage.getInstance().getRecordingFromArray(program.dvrId);
                if (recording != null) {
                    if (recording.isScheduled()) {
                        menuUtils.handleMenuCancelRecordingSelection(recording.id, recording.title);
                    } else {
                        menuUtils.handleMenuRemoveRecordingSelection(recording.id, recording.title);
                    }
                }
                return true;
            case R.id.menu_record_stop:
                recording = DataStorage.getInstance().getRecordingFromArray(program.dvrId);
                if (recording != null && recording.isRecording()) {
                    menuUtils.handleMenuStopRecordingSelection(recording.id, recording.title);
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
            case R.id.menu_search_imdb:
                menuUtils.handleMenuSearchWebSelection(program.title);
                return true;
            case R.id.menu_search_epg:
                menuUtils.handleMenuSearchEpgSelection(program.title, program.channelId);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public int getShownEventId() {
        return eventId;
    }
}

    