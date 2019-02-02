package org.tvheadend.tvhclient.features.programs;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
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
import android.widget.ScrollView;
import android.widget.TextView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.entity.Program;
import org.tvheadend.tvhclient.data.entity.Recording;
import org.tvheadend.tvhclient.features.dvr.RecordingAddEditActivity;
import org.tvheadend.tvhclient.features.shared.BaseFragment;
import org.tvheadend.tvhclient.utils.UIUtils;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProviders;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import timber.log.Timber;

public class ProgramDetailsFragment extends BaseFragment {

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
    @BindView(R.id.nested_toolbar)
    Toolbar nestedToolbar;
    @BindView(R.id.image)
    ImageView imageView;
    @BindView(R.id.scrollview)
    ScrollView scrollView;
    @BindView(R.id.status)
    TextView statusTextView;

    private int eventId;
    private int channelId;
    private Unbinder unbinder;
    private Program program;
    private Recording recording;
    private int programIdToBeEditedWhenBeingRecorded = 0;

    public static ProgramDetailsFragment newInstance(int eventId, int channelId) {
        ProgramDetailsFragment f = new ProgramDetailsFragment();
        Bundle args = new Bundle();
        args.putInt("eventId", eventId);
        args.putInt("channelId", channelId);
        f.setArguments(args);
        return f;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
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
        forceSingleScreenLayout();

        if (!isDualPane) {
            toolbarInterface.setTitle(getString(R.string.details));
            toolbarInterface.setSubtitle("");
        }
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

        ProgramViewModel viewModel = ViewModelProviders.of(activity).get(ProgramViewModel.class);
        program = viewModel.getProgramByIdSync(eventId);
        if (program != null) {
            Timber.d("Loaded details for program " + program.getTitle());
            updateUI();
            activity.invalidateOptionsMenu();
        } else {
            scrollView.setVisibility(View.GONE);
            statusTextView.setText(getString(R.string.error_loading_program_details));
            statusTextView.setVisibility(View.VISIBLE);
        }

        viewModel.getRecordingsByChannelId(channelId).observe(getViewLifecycleOwner(), recordings -> {
            Timber.d("Got recordings");
            if (recordings != null) {
                boolean recordingExists = false;
                for (Recording rec : recordings) {
                    // Show the edit recording screen of the scheduled recording
                    // in case the user has selected the record and edit menu item.
                    // Otherwise remember the recording so that the state can be updated
                    if (rec.getEventId() == programIdToBeEditedWhenBeingRecorded
                            && programIdToBeEditedWhenBeingRecorded > 0) {
                        programIdToBeEditedWhenBeingRecorded = 0;
                        Intent intent = new Intent(activity, RecordingAddEditActivity.class);
                        intent.putExtra("id", rec.getId());
                        intent.putExtra("type", "recording");
                        activity.startActivity(intent);
                        break;

                    } else if (program != null
                            && rec.getEventId() == program.getEventId()) {
                        Timber.d("Found recording for program " + program.getTitle());
                        recording = rec;
                        recordingExists = true;
                        break;
                    }
                }
                // If there is no recording for the program set the
                // recording to null so that the correct state is shown
                if (!recordingExists) {
                    recording = null;
                }
                // Update the state of the recording (if there is one)
                // and also the menu items in the nested toolbar
                updateRecordingState();
                activity.invalidateOptionsMenu();
            }
        });
    }

    private void updateRecordingState() {
        Drawable drawable = UIUtils.getRecordingState(activity, recording);
        stateImageView.setVisibility(drawable != null ? View.VISIBLE : View.GONE);
        stateImageView.setImageDrawable(drawable);
    }

    private void updateUI() {
        // The toolbar is hidden as a default to prevent pressing any icons if no recording
        // has been loaded yet. The toolbar is shown here because a recording was loaded
        nestedToolbar.setVisibility(View.VISIBLE);

        updateRecordingState();

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

        String ct = UIUtils.getContentTypeText(activity, program.getContentType());
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
        // Show or hide menus of the main toolbar
        menuUtils.onPreparePopupSearchMenu(menu, isNetworkAvailable);
        // Show or hide menus of the nested toolbar
        menu = nestedToolbar.getMenu();
        menuUtils.onPreparePopupMenu(menu,
                (program != null ? program.getStart() : 0),
                (program != null ? program.getStop() : 0),
                recording, isNetworkAvailable);
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
        nestedToolbar.inflateMenu(R.menu.program_popup_and_toolbar_menu);
        nestedToolbar.setOnMenuItemClickListener(this::onOptionsItemSelected);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // The program might be null in case the viewmodel
        // has not yet loaded the program for the given id
        if (program == null) {
            return super.onOptionsItemSelected(item);
        }

        switch (item.getItemId()) {
            case android.R.id.home:
                activity.finish();
                return true;

            case R.id.menu_record_stop:
                return menuUtils.handleMenuStopRecordingSelection(recording, null);

            case R.id.menu_record_cancel:
                return menuUtils.handleMenuCancelRecordingSelection(recording, null);

            case R.id.menu_record_remove:
                return menuUtils.handleMenuRemoveRecordingSelection(recording, null);

            case R.id.menu_record_once:
                return menuUtils.handleMenuRecordSelection(program.getEventId());

            case R.id.menu_record_once_and_edit:
                programIdToBeEditedWhenBeingRecorded = program.getEventId();
                return menuUtils.handleMenuRecordSelection(program.getEventId());

            case R.id.menu_record_once_custom_profile:
                return menuUtils.handleMenuCustomRecordSelection(program.getEventId(), program.getChannelId());

            case R.id.menu_record_series:
                return menuUtils.handleMenuSeriesRecordSelection(program.getTitle());

            case R.id.menu_play:
                return menuUtils.handleMenuPlayChannel(program.getChannelId());

            case R.id.menu_cast:
                return menuUtils.handleMenuCast("channelId", program.getChannelId());

            case R.id.menu_add_notification:
                return menuUtils.handleMenuAddNotificationSelection(program);

            case R.id.menu_search_imdb:
                return menuUtils.handleMenuSearchImdbWebsite(program.getTitle());

            case R.id.menu_search_fileaffinity:
                return menuUtils.handleMenuSearchFileAffinityWebsite(program.getTitle());

            case R.id.menu_search_youtube:
                return menuUtils.handleMenuSearchYoutube(program.getTitle());

            case R.id.menu_search_google:
                return menuUtils.handleMenuSearchGoogle(program.getTitle());

            case R.id.menu_search_epg:
                return menuUtils.handleMenuSearchEpgSelection(program.getTitle(), program.getChannelId());

            default:
                return super.onOptionsItemSelected(item);
        }
    }
}

    