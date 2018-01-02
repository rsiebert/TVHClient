package org.tvheadend.tvhclient.ui.recordings;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.ImageView;
import android.widget.TextView;

import org.tvheadend.tvhclient.data.DataStorage;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.ui.base.ToolbarInterface;
import org.tvheadend.tvhclient.data.model.Channel;
import org.tvheadend.tvhclient.data.model.Recording;
import org.tvheadend.tvhclient.utils.MenuUtils;
import org.tvheadend.tvhclient.utils.Utils;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

// TODO improve layout, more info, title, subtitle,... without header label
// TODO update icons (same color...)

public class RecordingDetailsFragment extends Fragment {

    @SuppressWarnings("unused")
    private final static String TAG = RecordingDetailsFragment.class.getSimpleName();

    private Recording recording;

    @BindView(R.id.summary_label) TextView summaryLabelTextView;
    @BindView(R.id.summary) TextView summaryTextView;
    @BindView(R.id.description_label) TextView descLabelTextView;
    @BindView(R.id.description) TextView descTextView;
    @BindView(R.id.title_label) TextView titleLabelTextView;
    @BindView(R.id.title) TextView titleTextView;
    @BindView(R.id.subtitle_label) TextView subtitleLabelTextView;
    @BindView(R.id.subtitle) TextView subtitleTextView;
    @BindView(R.id.channel_label) TextView channelLabelTextView;
    @BindView(R.id.channel) TextView channelNameTextView;
    @BindView(R.id.date) TextView dateTextView;
    @BindView(R.id.time) TextView timeTextView;
    @BindView(R.id.duration) TextView durationTextView;

    @Nullable
    @BindView(R.id.failed_reason) TextView failedReasonTextView;
    @BindView(R.id.is_series_recording) TextView isSeriesRecordingTextView;
    @BindView(R.id.is_timer_recording) TextView isTimerRecordingTextView;
    @BindView(R.id.is_enabled) TextView isEnabledTextView;

    @BindView(R.id.episode) TextView episodeTextView;
    @BindView(R.id.episode_label) TextView episodeLabelTextView;
    @BindView(R.id.comment) TextView commentTextView;
    @BindView(R.id.comment_label) TextView commentLabelTextView;
    @BindView(R.id.subscription_error) TextView subscriptionErrorTextView;
    @BindView(R.id.stream_errors) TextView streamErrorsTextView;
    @BindView(R.id.data_errors) TextView dataErrorsTextView;
    @BindView(R.id.data_size) TextView dataSizeTextView;
    @BindView(R.id.status_label) TextView statusLabelTextView;

    @Nullable
    @BindView(R.id.nested_toolbar) Toolbar nestedToolbar;

    private ToolbarInterface toolbarInterface;
    private MenuUtils menuUtils;
    private int dvrId;
    private boolean isUnlocked;
    private int htspVersion;
    private Unbinder unbinder;

    public static RecordingDetailsFragment newInstance(int dvrId) {
        RecordingDetailsFragment f = new RecordingDetailsFragment();
        Bundle args = new Bundle();
        args.putInt("dvrId", dvrId);
        f.setArguments(args);
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_recording_details, container, false);
        ViewStub stub = view.findViewById(R.id.stub);
        stub.setLayoutResource(R.layout.viewstub_recording_details_contents);
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

        if (getActivity() instanceof ToolbarInterface) {
            toolbarInterface = (ToolbarInterface) getActivity();
            toolbarInterface.setTitle(getString(R.string.details));
        }
        menuUtils = new MenuUtils(getActivity());
        isUnlocked = TVHClientApplication.getInstance().isUnlocked();
        htspVersion = DataStorage.getInstance().getProtocolVersion();
        setHasOptionsMenu(true);

        Bundle bundle = getArguments();
        if (bundle != null) {
            dvrId = bundle.getInt("dvrId", 0);
        }
        if (savedInstanceState != null) {
            dvrId = savedInstanceState.getInt("dvrId", 0);
        }
        // Get the recording so we can show its details
        recording = DataStorage.getInstance().getRecordingFromArray(dvrId);

        if (nestedToolbar != null) {
            nestedToolbar.inflateMenu(R.menu.toolbar_menu_recording_details);
            nestedToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    return onOptionsItemSelected(menuItem);
                }
            });
        }

        Utils.setDate(dateTextView, recording.start);
        Utils.setTime(timeTextView, recording.start, recording.stop);
        Utils.setDuration(durationTextView, recording.start, recording.stop);

        Channel channel = DataStorage.getInstance().getChannelFromArray(recording.channel);
        Utils.setDescription(channelLabelTextView, channelNameTextView, ((channel != null) ? channel.channelName : ""));
        Utils.setDescription(summaryLabelTextView, summaryTextView, recording.summary);
        Utils.setDescription(descLabelTextView, descTextView, recording.description);
        Utils.setDescription(titleLabelTextView, titleTextView, recording.title);
        Utils.setDescription(subtitleLabelTextView, subtitleTextView, recording.subtitle);
        Utils.setDescription(episodeLabelTextView, episodeTextView, recording.episode);
        Utils.setDescription(commentLabelTextView, commentTextView, recording.comment);
        Utils.setFailedReason(failedReasonTextView, recording);

        // Show the information if the recording belongs to a series recording
        // only when no dual pane is active (the controls shall be shown)
        isSeriesRecordingTextView.setVisibility((recording.autorecId != null) ? ImageView.VISIBLE : ImageView.GONE);
        isTimerRecordingTextView.setVisibility((recording.timerecId != null) ? ImageView.VISIBLE : ImageView.GONE);

        isEnabledTextView.setVisibility((htspVersion >= 23 && recording.enabled == 0) ? View.VISIBLE : View.GONE);
        isEnabledTextView.setText(recording.enabled > 0 ? R.string.recording_enabled : R.string.recording_disabled);

        // Only show the status details in the 
        // completed and failed details screens
        if (!recording.isScheduled()) {
            if (recording.subscriptionError != null && recording.subscriptionError.length() > 0) {
                subscriptionErrorTextView.setVisibility(View.VISIBLE);
                subscriptionErrorTextView.setText(getResources().getString(
                        R.string.subscription_error, recording.subscriptionError));
            } else {
                subscriptionErrorTextView.setVisibility(View.GONE);
            }

            streamErrorsTextView.setText(getResources().getString(R.string.stream_errors, recording.streamErrors == null ? "0" : recording.streamErrors));
            dataErrorsTextView.setText(getResources().getString(R.string.data_errors, recording.dataErrors == null ? "0" : recording.dataErrors));

            if (recording.dataSize > 1048576) {
                dataSizeTextView.setText(getResources().getString(R.string.data_size, recording.dataSize / 1048576, "MB"));
            } else {
                dataSizeTextView.setText(getResources().getString(R.string.data_size, recording.dataSize / 1024, "KB"));
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
        if (nestedToolbar != null) {
            menu = nestedToolbar.getMenu();
        }
        if (recording.isCompleted()) {
            menu.findItem(R.id.menu_record_remove).setVisible(true);
            menu.findItem(R.id.menu_play).setVisible(true);
            menu.findItem(R.id.menu_download).setVisible(isUnlocked);

        } else if (recording.isScheduled() && !recording.isRecording()) {
            menu.findItem(R.id.menu_record_remove).setVisible(true);
            menu.findItem(R.id.menu_edit).setVisible(isUnlocked);

        } else if (recording.isRecording()) {
            menu.findItem(R.id.menu_record_stop).setVisible(true);
            menu.findItem(R.id.menu_play).setVisible(true);
            menu.findItem(R.id.menu_edit).setVisible(isUnlocked);

        } else if (recording.isFailed() || recording.isRemoved() || recording.isMissed() || recording.isAborted()) {
            menu.findItem(R.id.menu_record_remove).setVisible(true);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("dvrId", dvrId);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        if (nestedToolbar == null) {
            inflater.inflate(R.menu.popup_menu_recordings, menu);
        } else {
            inflater.inflate(R.menu.options_menu_external_search, menu);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                getActivity().finish();
                return true;
            case R.id.menu_play:
                menuUtils.handleMenuPlaySelection(-1, recording.id);
                return true;
            case R.id.menu_download:
                menuUtils.handleMenuDownloadSelection(recording.id);
                return true;
            case R.id.menu_edit:
                Intent editIntent = new Intent(getActivity(), RecordingAddEditActivity.class);
                editIntent.putExtra("dvrId", recording.id);
                editIntent.putExtra("type", "recording");
                getActivity().startActivity(editIntent);
                return true;
            case R.id.menu_record_stop:
                menuUtils.handleMenuStopRecordingSelection(recording.id, recording.title);
                return true;
            case R.id.menu_record_remove:
                if (recording.isScheduled()) {
                    menuUtils.handleMenuCancelRecordingSelection(recording.id, recording.title);
                } else {
                    menuUtils.handleMenuRemoveRecordingSelection(recording.id, recording.title);
                }
                return true;
            case R.id.menu_search_imdb:
                menuUtils.handleMenuSearchWebSelection(recording.title);
                return true;
            case R.id.menu_search_epg:
                menuUtils.handleMenuSearchEpgSelection(recording.title);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public int getShownDvrId() {
        return dvrId;
    }
}
