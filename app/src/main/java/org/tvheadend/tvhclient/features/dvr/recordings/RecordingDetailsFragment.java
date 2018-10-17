package org.tvheadend.tvhclient.features.dvr.recordings;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
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
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.entity.Recording;
import org.tvheadend.tvhclient.features.download.DownloadPermissionGrantedInterface;
import org.tvheadend.tvhclient.features.dvr.RecordingAddEditActivity;
import org.tvheadend.tvhclient.features.shared.BaseFragment;
import org.tvheadend.tvhclient.features.shared.callbacks.RecordingRemovedCallback;
import org.tvheadend.tvhclient.utils.UIUtils;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class RecordingDetailsFragment extends BaseFragment implements RecordingRemovedCallback, DownloadPermissionGrantedInterface {

    @BindView(R.id.state)
    ImageView stateImageView;
    @BindView(R.id.summary_label)
    TextView summaryLabelTextView;
    @BindView(R.id.summary)
    TextView summaryTextView;
    @BindView(R.id.description_label)
    TextView descLabelTextView;
    @BindView(R.id.description)
    TextView descTextView;
    @BindView(R.id.title_label)
    TextView titleLabelTextView;
    @BindView(R.id.title)
    TextView titleTextView;
    @BindView(R.id.subtitle_label)
    TextView subtitleLabelTextView;
    @BindView(R.id.subtitle)
    TextView subtitleTextView;
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

    @Nullable
    @BindView(R.id.failed_reason)
    TextView failedReasonTextView;
    @BindView(R.id.is_series_recording)
    TextView isSeriesRecordingTextView;
    @BindView(R.id.is_timer_recording)
    TextView isTimerRecordingTextView;
    @BindView(R.id.is_enabled)
    TextView isEnabledTextView;

    @BindView(R.id.episode)
    TextView episodeTextView;
    @BindView(R.id.episode_label)
    TextView episodeLabelTextView;
    @BindView(R.id.comment)
    TextView commentTextView;
    @BindView(R.id.comment_label)
    TextView commentLabelTextView;
    @BindView(R.id.subscription_error)
    TextView subscriptionErrorTextView;
    @BindView(R.id.stream_errors)
    TextView streamErrorsTextView;
    @BindView(R.id.data_errors)
    TextView dataErrorsTextView;
    @BindView(R.id.data_size)
    TextView dataSizeTextView;
    @BindView(R.id.status_label)
    TextView statusLabelTextView;
    @BindView(R.id.nested_toolbar)
    Toolbar nestedToolbar;
    @BindView(R.id.scrollview)
    ScrollView scrollView;
    @BindView(R.id.status)
    TextView statusTextView;

    private Recording recording = null;
    private int id;
    private Unbinder unbinder;

    public static RecordingDetailsFragment newInstance(int dvrId) {
        RecordingDetailsFragment f = new RecordingDetailsFragment();
        Bundle args = new Bundle();
        args.putInt("id", dvrId);
        f.setArguments(args);
        return f;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.details_fragment, container, false);
        ViewStub stub = view.findViewById(R.id.stub);
        stub.setLayoutResource(R.layout.recording_details_fragment_contents);
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
        }

        // Get the recording id after an orientation change has occurred
        // or when the fragment is shown for the first time
        if (savedInstanceState != null) {
            id = savedInstanceState.getInt("id", 0);
        } else {
            Bundle bundle = getArguments();
            if (bundle != null) {
                id = bundle.getInt("id", 0);
            }
        }

        RecordingViewModel viewModel = ViewModelProviders.of(activity).get(RecordingViewModel.class);
        viewModel.getRecordingById(id).observe(this, rec -> {
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

        Drawable drawable = UIUtils.getRecordingState(activity, recording);
        stateImageView.setVisibility(drawable != null ? View.VISIBLE : View.GONE);
        stateImageView.setImageDrawable(drawable);

        dateTextView.setText(UIUtils.getDate(getContext(), recording.getStart()));
        startTimeTextView.setText(UIUtils.getTimeText(getContext(), recording.getStart()));
        stopTimeTextView.setText(UIUtils.getTimeText(getContext(), recording.getStop()));

        String durationTime = getString(R.string.minutes, (int) ((recording.getStop() - recording.getStart()) / 1000 / 60));
        durationTextView.setText(durationTime);

        channelNameTextView.setText(!TextUtils.isEmpty(recording.getChannelName()) ? recording.getChannelName() : getString(R.string.no_channel));

        summaryLabelTextView.setVisibility(!TextUtils.isEmpty(recording.getSummary()) ? View.VISIBLE : View.GONE);
        summaryTextView.setVisibility(!TextUtils.isEmpty(recording.getSummary()) ? View.VISIBLE : View.GONE);
        summaryTextView.setText(recording.getSummary());

        descLabelTextView.setVisibility(!TextUtils.isEmpty(recording.getDescription()) ? View.VISIBLE : View.GONE);
        descTextView.setVisibility(!TextUtils.isEmpty(recording.getDescription()) ? View.VISIBLE : View.GONE);
        descTextView.setText(recording.getDescription());

        titleLabelTextView.setVisibility(!TextUtils.isEmpty(recording.getTitle()) ? View.VISIBLE : View.GONE);
        titleTextView.setVisibility(!TextUtils.isEmpty(recording.getTitle()) ? View.VISIBLE : View.GONE);
        titleTextView.setText(recording.getTitle());

        subtitleLabelTextView.setVisibility(!TextUtils.isEmpty(recording.getSubtitle()) ? View.VISIBLE : View.GONE);
        subtitleTextView.setVisibility(!TextUtils.isEmpty(recording.getSubtitle()) ? View.VISIBLE : View.GONE);
        subtitleTextView.setText(recording.getSubtitle());

        episodeLabelTextView.setVisibility(!TextUtils.isEmpty(recording.getEpisode()) ? View.VISIBLE : View.GONE);
        episodeTextView.setVisibility(!TextUtils.isEmpty(recording.getEpisode()) ? View.VISIBLE : View.GONE);
        episodeTextView.setText(recording.getEpisode());

        commentLabelTextView.setVisibility(!TextUtils.isEmpty(recording.getComment()) ? View.VISIBLE : View.GONE);
        commentTextView.setVisibility(!TextUtils.isEmpty(recording.getComment()) ? View.VISIBLE : View.GONE);
        commentTextView.setText(recording.getComment());

        if (failedReasonTextView != null) {
            String failedReasonText = UIUtils.getRecordingFailedReasonText(activity, recording);
            failedReasonTextView.setVisibility(!TextUtils.isEmpty(failedReasonText) ? View.VISIBLE : View.GONE);
            failedReasonTextView.setText(failedReasonText);
        }

        // Show the information if the recording belongs to a series recording
        // only when no dual pane is active (the controls shall be shown)
        isSeriesRecordingTextView.setVisibility(!TextUtils.isEmpty(recording.getAutorecId()) ? ImageView.VISIBLE : ImageView.GONE);
        isTimerRecordingTextView.setVisibility(!TextUtils.isEmpty(recording.getTimerecId()) ? ImageView.VISIBLE : ImageView.GONE);

        isEnabledTextView.setVisibility((htspVersion >= 23 && recording.getEnabled() == 0) ? View.VISIBLE : View.GONE);
        isEnabledTextView.setText(recording.getEnabled() > 0 ? R.string.recording_enabled : R.string.recording_disabled);

        // Only show the status details in the 
        // completed and failed details screens
        if (!recording.isScheduled()) {

            if (recording.getSubscriptionError() != null && recording.getSubscriptionError().length() > 0) {
                subscriptionErrorTextView.setVisibility(View.VISIBLE);
                subscriptionErrorTextView.setText(getResources().getString(
                        R.string.subscription_error, recording.getSubscriptionError()));
            } else {
                subscriptionErrorTextView.setVisibility(View.GONE);
            }

            streamErrorsTextView.setText(getResources().getString(R.string.stream_errors, recording.getStreamErrors() == null ? "0" : recording.getStreamErrors()));
            dataErrorsTextView.setText(getResources().getString(R.string.data_errors, recording.getDataErrors() == null ? "0" : recording.getDataErrors()));

            if (recording.getDataSize() > 1048576) {
                dataSizeTextView.setText(getResources().getString(R.string.data_size, recording.getDataSize() / 1048576, "MB"));
            } else {
                dataSizeTextView.setText(getResources().getString(R.string.data_size, recording.getDataSize() / 1024, "KB"));
            }

        } else {
            statusLabelTextView.setVisibility(View.GONE);
            subscriptionErrorTextView.setVisibility(View.GONE);
            streamErrorsTextView.setVisibility(View.GONE);
            dataErrorsTextView.setVisibility(View.GONE);
            dataSizeTextView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        if (recording == null) {
            return;
        }

        menuUtils.onPreparePopupSearchMenu(menu, isNetworkAvailable);

        menu = nestedToolbar.getMenu();
        if (isNetworkAvailable) {
            if (recording.isCompleted()) {
                menu.findItem(R.id.menu_record_remove).setVisible(true);
                menu.findItem(R.id.menu_play).setVisible(true);
                CastSession castSession = CastContext.getSharedInstance(activity.getApplicationContext()).getSessionManager().getCurrentCastSession();
                menu.findItem(R.id.menu_cast).setVisible(castSession != null);
                menu.findItem(R.id.menu_download).setVisible(isUnlocked);

            } else if (recording.isScheduled() && !recording.isRecording()) {
                menu.findItem(R.id.menu_record_cancel).setVisible(true);
                menu.findItem(R.id.menu_edit).setVisible(isUnlocked);

            } else if (recording.isRecording()) {
                menu.findItem(R.id.menu_record_stop).setVisible(true);
                menu.findItem(R.id.menu_play).setVisible(true);
                CastSession castSession = CastContext.getSharedInstance(activity.getApplicationContext()).getSessionManager().getCurrentCastSession();
                menu.findItem(R.id.menu_cast).setVisible(castSession != null);
                menu.findItem(R.id.menu_edit).setVisible(isUnlocked);

            } else if (recording.isFailed() || recording.isFileMissing() || recording.isMissed() || recording.isAborted()) {
                menu.findItem(R.id.menu_record_remove).setVisible(true);
                // Allow playing a failed recording which size is not zero
                if (recording.getDataSize() > 0) {
                    menu.findItem(R.id.menu_play).setVisible(true);
                    CastSession castSession = CastContext.getSharedInstance(activity.getApplicationContext()).getSessionManager().getCurrentCastSession();
                    menu.findItem(R.id.menu_cast).setVisible(castSession != null);
                }
            }
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("id", id);
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
        switch (item.getItemId()) {
            case android.R.id.home:
                activity.finish();
                return true;

            case R.id.menu_play:
                return menuUtils.handleMenuPlayRecording(recording.getId());

            case R.id.menu_cast:
                return menuUtils.handleMenuCast("dvrId", recording.getId());

            case R.id.menu_download:
                return menuUtils.handleMenuDownloadSelection(recording.getId());

            case R.id.menu_edit:
                Intent editIntent = new Intent(activity, RecordingAddEditActivity.class);
                editIntent.putExtra("id", recording.getId());
                editIntent.putExtra("type", "recording");
                activity.startActivity(editIntent);
                return true;

            case R.id.menu_record_stop:
                return menuUtils.handleMenuStopRecordingSelection(recording, this);

            case R.id.menu_record_cancel:
                return menuUtils.handleMenuCancelRecordingSelection(recording, this);

            case R.id.menu_record_remove:
                return menuUtils.handleMenuRemoveRecordingSelection(recording, this);

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

    public int getShownDvrId() {
        return id;
    }

    @Override
    public void onRecordingRemoved() {
        if (!isDualPane) {
            activity.finish();
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

    @Override
    public void downloadRecording() {
        menuUtils.handleMenuDownloadSelection(recording.getId());
    }
}
