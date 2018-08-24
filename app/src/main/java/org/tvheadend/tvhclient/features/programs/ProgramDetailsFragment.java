package org.tvheadend.tvhclient.features.programs;

import android.arch.lifecycle.ViewModelProviders;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.entity.Program;
import org.tvheadend.tvhclient.data.entity.Recording;
import org.tvheadend.tvhclient.features.shared.BaseFragment;
import org.tvheadend.tvhclient.utils.UIUtils;
import org.tvheadend.tvhclient.features.shared.callbacks.RecordingRemovedCallback;

import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import timber.log.Timber;

// TODO update icons (same color, record with profile must differ from regular record...)

public class ProgramDetailsFragment extends BaseFragment implements RecordingRemovedCallback {

    @Nullable
    @BindView(R.id.state)
    ImageView stateImageView;
    @BindView(R.id.title)
    TextView titleTextView;
    @BindView(R.id.title_label)
    TextView titleLabelTextView;
    @BindView(R.id.summary_label)
    TextView summaryLabelTextView;
    @BindView(R.id.summary)
    TextView summaryTextView;
    @BindView(R.id.description_label)
    TextView descriptionLabelTextView;
    @BindView(R.id.description)
    TextView descriptionTextView;
    @BindView(R.id.channel_label)
    TextView channelLabelTextView;
    @BindView(R.id.channel)
    TextView channelNameTextView;
    @BindView(R.id.date)
    TextView dateTextView;
    @BindView(R.id.start_time)
    TextView startTimeTextView;
    @BindView(R.id.stop_time)
    TextView stopTimeTextView;
    @BindView(R.id.duration)
    TextView durationTextView;
    @BindView(R.id.progress)
    TextView progressTextView;
    @BindView(R.id.content_type_label)
    TextView contentTypeLabelTextView;
    @BindView(R.id.content_type)
    TextView contentTypeTextView;
    @BindView(R.id.series_info_label)
    TextView seriesInfoLabelTextView;
    @BindView(R.id.series_info)
    TextView seriesInfoTextView;
    @BindView(R.id.star_rating_label)
    TextView ratingBarLabelTextView;
    @BindView(R.id.star_rating_text)
    TextView ratingBarTextView;
    @BindView(R.id.star_rating)
    RatingBar ratingBar;
    @Nullable
    @BindView(R.id.nested_toolbar)
    Toolbar nestedToolbar;
    @BindView(R.id.image)
    ImageView imageView;

    private int eventId;
    private int channelId;
    private Unbinder unbinder;
    private Program program;
    private Recording recording;

    public static ProgramDetailsFragment newInstance(int eventId) {
        ProgramDetailsFragment f = new ProgramDetailsFragment();
        Bundle args = new Bundle();
        args.putInt("eventId", eventId);
        f.setArguments(args);
        return f;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.details_fragment, container, false);
        ViewStub stub = view.findViewById(R.id.stub);
        stub.setLayoutResource(R.layout.program_details_fragment_contents);
        stub.inflate();
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

        toolbarInterface.setTitle(getString(R.string.details));

        if (savedInstanceState != null) {
            eventId = savedInstanceState.getInt("eventId", 0);
            channelId = savedInstanceState.getInt("channelId", 0);
        } else {
            Bundle bundle = getArguments();
            if (bundle != null) {
                eventId = bundle.getInt("eventId", 0);
                channelId = bundle.getInt("channelId", 0);
            }
        }

        if (nestedToolbar != null) {
            nestedToolbar.inflateMenu(R.menu.program_details_toolbar_menu);
            nestedToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    return onOptionsItemSelected(menuItem);
                }
            });
        }

        ProgramViewModel viewModel = ViewModelProviders.of(activity).get(ProgramViewModel.class);
        program = viewModel.getProgramByIdSync(eventId);
        recording = viewModel.getRecordingsById(program.getDvrId());
        updateUI();
        activity.invalidateOptionsMenu();

        viewModel.getRecordingsByChannelId(channelId).observe(this, recordings -> {
            if (recordings != null) {
                for (Recording rec : recordings) {
                    if (rec.getEventId() == program.getEventId()) {
                        program.setRecording(rec);
                        break;
                    }
                }
            }
        });
    }

    private void updateUI() {

        // Show the program information
        if (stateImageView != null) {
            Drawable drawable = UIUtils.getRecordingState(activity, program.getRecording());
            stateImageView.setVisibility(drawable != null ? View.VISIBLE : View.GONE);
            stateImageView.setImageDrawable(drawable);
        }

        startTimeTextView.setText(UIUtils.getTimeText(getContext(), program.getStart()));
        stopTimeTextView.setText(UIUtils.getTimeText(getContext(), program.getStop()));
        dateTextView.setText(UIUtils.getDate(getContext(), program.getStart()));

        String durationTime = getString(R.string.minutes, (int) ((program.getStop() - program.getStart()) / 1000 / 60));
        durationTextView.setText(durationTime);

        String progressText = UIUtils.getProgressText(getContext(), program.getStart(), program.getStop());
        progressTextView.setVisibility(!TextUtils.isEmpty(progressText) ? View.VISIBLE : View.GONE);
        progressTextView.setText(progressText);

        titleLabelTextView.setVisibility(!TextUtils.isEmpty(program.getTitle()) ? View.VISIBLE : View.GONE);
        titleTextView.setVisibility(!TextUtils.isEmpty(program.getTitle()) ? View.VISIBLE : View.GONE);
        titleTextView.setText(program.getTitle());

        summaryLabelTextView.setVisibility(!TextUtils.isEmpty(program.getSummary()) ? View.VISIBLE : View.GONE);
        summaryTextView.setVisibility(!TextUtils.isEmpty(program.getSummary()) ? View.VISIBLE : View.GONE);
        summaryTextView.setText(program.getSummary());

        descriptionLabelTextView.setVisibility(!TextUtils.isEmpty(program.getDescription()) ? View.VISIBLE : View.GONE);
        descriptionTextView.setVisibility(!TextUtils.isEmpty(program.getDescription()) ? View.VISIBLE : View.GONE);
        descriptionTextView.setText(program.getDescription());

        channelLabelTextView.setVisibility(!TextUtils.isEmpty(program.getChannelName()) ? View.VISIBLE : View.GONE);
        channelNameTextView.setVisibility(!TextUtils.isEmpty(program.getChannelName()) ? View.VISIBLE : View.GONE);
        channelNameTextView.setText(program.getChannelName());

        String seriesInfoText = UIUtils.getSeriesInfo(activity, program);
        if (TextUtils.isEmpty(seriesInfoText)) {
            seriesInfoLabelTextView.setVisibility(View.GONE);
            seriesInfoTextView.setVisibility(View.GONE);
        } else {
            seriesInfoTextView.setText(seriesInfoText);
        }

        String ct = UIUtils.getContentTypeText(getContext(), program.getContentType());
        if (TextUtils.isEmpty(ct)) {
            contentTypeLabelTextView.setVisibility(View.GONE);
            contentTypeTextView.setVisibility(View.GONE);
        } else {
            contentTypeTextView.setText(ct);
        }

        // Show the rating information as starts
        if (program.getStarRating() < 0) {
            ratingBarLabelTextView.setVisibility(View.GONE);
            ratingBarTextView.setVisibility(View.GONE);
            ratingBar.setVisibility(View.GONE);
        } else {
            ratingBar.setRating((float) program.getStarRating() / 10.0f);
            String value = " (" + program.getStarRating() + "/" + 10 + ")";
            ratingBarTextView.setText(value);
        }

        // Show the program image if one exists
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        Timber.d("Showing program image " + program.getImage());
        if (isUnlocked && prefs.getBoolean("program_artwork_enabled", false)) {
            Picasso.get()
                    .load(UIUtils.getIconUrl(activity, program.getImage()))
                    .into(imageView, new Callback() {
                        @Override
                        public void onSuccess() {
                            imageView.setVisibility(View.VISIBLE);
                        }

                        @Override
                        public void onError(Exception e) {

                        }
                    });
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        if (program == null) {
            return;
        }

        menuUtils.onPreparePopupSearchMenu(menu, isNetworkAvailable);
        if (!isDualPane) {
            menu.findItem(R.id.menu_search).setVisible(false);
        }

        menu = nestedToolbar.getMenu();
        // Show the play menu item when the current
        // time is between the program start and end time
        long currentTime = new Date().getTime();
        if (currentTime > program.getStart() && currentTime < program.getStop()) {
            menu.findItem(R.id.menu_play).setVisible(true);
        }

        if (recording == null || (!recording.isRecording() && !recording.isScheduled())) {
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
        outState.putInt("channelId", channelId);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.external_search_options_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                activity.finish();
                return true;

            case R.id.menu_record_remove:
                if (recording != null) {
                    if (recording.isScheduled()) {
                        menuUtils.handleMenuCancelRecordingSelection(recording.getId(), recording.getTitle(), this);
                    } else {
                        menuUtils.handleMenuRemoveRecordingSelection(recording.getId(), recording.getTitle(), this);
                    }
                }
                return true;

            case R.id.menu_record_stop:
                if (recording != null && recording.isRecording()) {
                    menuUtils.handleMenuStopRecordingSelection(recording.getId(), recording.getTitle());
                }
                return true;

            case R.id.menu_record_once:
                menuUtils.handleMenuRecordSelection(program.getEventId());
                return true;

            case R.id.menu_record_once_custom_profile:
                menuUtils.handleMenuCustomRecordSelection(program.getEventId(), program.getChannelId());
                return true;

            case R.id.menu_record_series:
                menuUtils.handleMenuSeriesRecordSelection(program.getTitle());
                return true;

            case R.id.menu_play:
                menuUtils.handleMenuPlayChannel(program.getChannelId());
                return true;

            case R.id.menu_search_imdb:
                menuUtils.handleMenuSearchImdbWebsite(program.getTitle());
                return true;

            case R.id.menu_search_fileaffinity:
                menuUtils.handleMenuSearchFileAffinityWebsite(program.getTitle());
                return true;

            case R.id.menu_search_epg:
                menuUtils.handleMenuSearchEpgSelection(program.getTitle(), program.getChannelId());
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onRecordingRemoved() {
        activity.finish();
    }
}

    