package org.tvheadend.tvhclient.features.dvr.series_recordings;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.ScrollView;
import android.widget.TextView;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.entity.SeriesRecording;
import org.tvheadend.tvhclient.features.dvr.RecordingAddEditActivity;
import org.tvheadend.tvhclient.features.dvr.RecordingUtils;
import org.tvheadend.tvhclient.features.shared.BaseFragment;
import org.tvheadend.tvhclient.features.dvr.RecordingRemovedCallback;
import org.tvheadend.tvhclient.utils.UIUtils;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class SeriesRecordingDetailsFragment extends BaseFragment implements RecordingRemovedCallback {

    @BindView(R.id.disabled)
    TextView isDisabledTextView;
    @BindView(R.id.directory_label)
    TextView directoryLabelTextView;
    @BindView(R.id.directory)
    TextView directoryTextView;
    @BindView(R.id.minimum_duration)
    TextView minDurationTextView;
    @BindView(R.id.maximum_duration)
    TextView maxDurationTextView;
    @BindView(R.id.start_after_time)
    TextView startTimeTextView;
    @BindView(R.id.start_before_time)
    TextView startWindowTimeTextView;
    @BindView(R.id.days_of_week)
    TextView daysOfWeekTextView;
    @BindView(R.id.channel)
    TextView channelNameTextView;
    @BindView(R.id.name_label)
    TextView nameLabelTextView;
    @BindView(R.id.name)
    TextView nameTextView;
    @BindView(R.id.priority)
    TextView priorityTextView;
    @BindView(R.id.nested_toolbar)
    Toolbar nestedToolbar;
    @BindView(R.id.scrollview)
    ScrollView scrollView;
    @BindView(R.id.status)
    TextView statusTextView;

    private SeriesRecording recording;
    private String id;
    private Unbinder unbinder;

    public static SeriesRecordingDetailsFragment newInstance(String id) {
        SeriesRecordingDetailsFragment f = new SeriesRecordingDetailsFragment();
        Bundle args = new Bundle();
        args.putString("id", id);
        f.setArguments(args);
        return f;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.details_fragment, container, false);
        ViewStub stub = view.findViewById(R.id.stub);
        stub.setLayoutResource(R.layout.series_recording_details_fragment_contents);
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

        if (!isDualPane) {
            toolbarInterface.setTitle(getString(R.string.details));
            toolbarInterface.setSubtitle("");
        }

        // Get the recording id after an orientation change has occurred
        // or when the fragment is shown for the first time
        if (savedInstanceState != null) {
            id = savedInstanceState.getString("id");
        } else {
            Bundle bundle = getArguments();
            if (bundle != null) {
                id = bundle.getString("id");
            }
        }

        SeriesRecordingViewModel viewModel = ViewModelProviders.of(activity).get(SeriesRecordingViewModel.class);
        viewModel.getRecordingById(id).observe(getViewLifecycleOwner(), rec -> {
            if (rec != null) {
                recording = rec;
                updateUI();
                activity.invalidateOptionsMenu();
            } else {
                scrollView.setVisibility(View.GONE);
                statusTextView.setText(getString(R.string.error_loading_recording_details));
                statusTextView.setVisibility(View.VISIBLE);

            }
        });
    }

    private void updateUI() {
        // The toolbar is hidden as a default to prevent pressing any icons if no recording
        // has been loaded yet. The toolbar is shown here because a recording was loaded
        nestedToolbar.setVisibility(View.VISIBLE);

        isDisabledTextView.setVisibility(htspVersion >= 19 && recording.isEnabled() ? View.VISIBLE : View.GONE);
        isDisabledTextView.setText(recording.isEnabled() ? R.string.recording_enabled : R.string.recording_disabled);

        directoryLabelTextView.setVisibility(!TextUtils.isEmpty(recording.getDirectory()) && htspVersion >= 19 ? View.VISIBLE : View.GONE);
        directoryTextView.setVisibility(!TextUtils.isEmpty(recording.getDirectory()) && htspVersion >= 19 ? View.VISIBLE : View.GONE);
        directoryTextView.setText(recording.getDirectory());

        channelNameTextView.setText(!TextUtils.isEmpty(recording.getChannelName()) ? recording.getChannelName() : getString(R.string.all_channels));

        if (TextUtils.isEmpty(recording.getName()) && TextUtils.isEmpty(recording.getTitle())) {
            nameLabelTextView.setVisibility(View.GONE);
            nameTextView.setVisibility(View.GONE);
        } else {
            nameLabelTextView.setVisibility(View.VISIBLE);
            nameTextView.setVisibility(View.VISIBLE);

            nameTextView.setText(!TextUtils.isEmpty(recording.getName()) ? recording.getName() : recording.getTitle());
        }

        daysOfWeekTextView.setText(UIUtils.getDaysOfWeekText(activity, recording.getDaysOfWeek()));

        priorityTextView.setText(RecordingUtils.getPriorityName(activity, recording.getPriority()));

        if (recording.getMinDuration() > 0) {
            // The minimum timeTextView is given in seconds, but we want to show it in minutes
            minDurationTextView.setText(getString(R.string.minutes, (int) (recording.getMinDuration() / 60)));
        }
        if (recording.getMaxDuration() > 0) {
            // The maximum timeTextView is given in seconds, but we want to show it in minutes
            maxDurationTextView.setText(getString(R.string.minutes, (int) (recording.getMaxDuration() / 60)));
        }

        int gmtOffset = serverStatus.getGmtoffset();
        startTimeTextView.setText(UIUtils.getTimeText(getContext(), recording.getStart() - gmtOffset));
        startWindowTimeTextView.setText(UIUtils.getTimeText(getContext(), recording.getStartWindow() - gmtOffset));
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        menuUtils.onPreparePopupSearchMenu(menu, isNetworkAvailable);
        if (nestedToolbar.getMenu() == null) {
            return;
        }
        menu = nestedToolbar.getMenu();
        menu.findItem(R.id.menu_edit).setVisible(true);
        menu.findItem(R.id.menu_record_remove).setVisible(true);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("id", id);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.external_search_options_menu, menu);
        nestedToolbar.inflateMenu(R.menu.recording_details_toolbar_menu);
        nestedToolbar.setOnMenuItemClickListener(this::onOptionsItemSelected);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // The recording might be null in case the viewmodel
        // has not yet loaded the recording for the given id
        if (recording == null) {
            return super.onOptionsItemSelected(item);
        }

        switch (item.getItemId()) {
            case R.id.menu_edit:
                Intent intent = new Intent(activity, RecordingAddEditActivity.class);
                intent.putExtra("type", "series_recording");
                intent.putExtra("id", recording.getId());
                activity.startActivity(intent);
                return true;

            case R.id.menu_record_remove:
                return menuUtils.handleMenuRemoveSeriesRecordingSelection(recording, this);

            case R.id.menu_search_imdb:
                return menuUtils.handleMenuSearchImdbWebsite(recording.getTitle());

            case R.id.menu_search_fileaffinity:
                return menuUtils.handleMenuSearchFileAffinityWebsite(recording.getTitle());

            case R.id.menu_search_youtube:
                return menuUtils.handleMenuSearchYoutube(recording.getTitle());

            case R.id.menu_search_google:
                return menuUtils.handleMenuSearchGoogle(recording.getTitle());

            case R.id.menu_search_epg:
                return menuUtils.handleMenuSearchEpgSelection(recording.getTitle());

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    String getShownId() {
        return id;
    }

    @Override
    public void onRecordingRemoved() {
        if (!isDualPane) {
            activity.onBackPressed();
        } else {
            Fragment detailsFragment = activity.getSupportFragmentManager().findFragmentById(R.id.details);
            if (detailsFragment != null) {
                activity.getSupportFragmentManager()
                        .beginTransaction()
                        .remove(detailsFragment)
                        .commit();
            }
        }
    }
}
