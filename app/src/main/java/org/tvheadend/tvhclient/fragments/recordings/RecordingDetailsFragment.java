package org.tvheadend.tvhclient.fragments.recordings;

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

import org.tvheadend.tvhclient.DataStorage;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.activities.AddEditActivity;
import org.tvheadend.tvhclient.activities.ToolbarInterfaceLight;
import org.tvheadend.tvhclient.model.Channel;
import org.tvheadend.tvhclient.model.Recording;
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

    @BindView(R.id.summary_label) TextView summaryLabel;
    @BindView(R.id.summary) TextView summary;
    @BindView(R.id.description_label) TextView descLabel;
    @BindView(R.id.description) TextView desc;
    @BindView(R.id.title_label) TextView titleLabel;
    @BindView(R.id.title) TextView title;
    @BindView(R.id.subtitle_label) TextView subtitleLabel;
    @BindView(R.id.subtitle) TextView subtitle;
    @BindView(R.id.channel_label) TextView channelLabel;
    @BindView(R.id.channel) TextView channelName;
    @BindView(R.id.date) TextView date;
    @BindView(R.id.time) TextView time;
    @BindView(R.id.duration) TextView duration;

    @Nullable
    @BindView(R.id.failed_reason) TextView failed_reason;
    @BindView(R.id.is_series_recording) TextView is_series_recording;
    @BindView(R.id.is_timer_recording) TextView is_timer_recording;
    @BindView(R.id.is_enabled) TextView isEnabled;

    @BindView(R.id.episode) TextView episode;
    @BindView(R.id.episode_label) TextView episodeLabel;
    @BindView(R.id.comment) TextView comment;
    @BindView(R.id.comment_label) TextView commentLabel;
    @BindView(R.id.subscription_error) TextView subscription_error;
    @BindView(R.id.stream_errors) TextView stream_errors;
    @BindView(R.id.data_errors) TextView data_errors;
    @BindView(R.id.data_size) TextView data_size;
    @BindView(R.id.status_label) TextView statusLabel;

    @Nullable
    @BindView(R.id.nested_toolbar) Toolbar nestedToolbar;

    private ToolbarInterfaceLight toolbarInterface;
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
        View view = inflater.inflate(R.layout.recording_details_fragment, container, false);
        ViewStub stub = view.findViewById(R.id.stub);
        stub.setLayoutResource(R.layout.recording_details_content_fragment);
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
            dvrId = bundle.getInt("dvrId", 0);
        }
        if (savedInstanceState != null) {
            dvrId = savedInstanceState.getInt("dvrId", 0);
        }
        // Get the recording so we can show its details
        recording = DataStorage.getInstance().getRecordingFromArray(dvrId);

        if (nestedToolbar != null) {
            nestedToolbar.inflateMenu(R.menu.recording_toolbar_menu);
            nestedToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    return onOptionsItemSelected(menuItem);
                }
            });
        }

        Utils.setDate(date, recording.start);
        Utils.setTime(time, recording.start, recording.stop);
        Utils.setDuration(duration, recording.start, recording.stop);

        Channel channel = DataStorage.getInstance().getChannelFromArray(recording.channel);
        Utils.setDescription(channelLabel, channelName, ((channel != null) ? channel.channelName : ""));
        Utils.setDescription(summaryLabel, summary, recording.summary);
        Utils.setDescription(descLabel, desc, recording.description);
        Utils.setDescription(titleLabel, title, recording.title);
        Utils.setDescription(subtitleLabel, subtitle, recording.subtitle);
        Utils.setDescription(episodeLabel, episode, recording.episode);
        Utils.setDescription(commentLabel, comment, recording.comment);
        Utils.setFailedReason(failed_reason, recording);

        // Show the information if the recording belongs to a series recording
        // only when no dual pane is active (the controls shall be shown)
        is_series_recording.setVisibility((recording.autorecId != null) ? ImageView.VISIBLE : ImageView.GONE);
        is_timer_recording.setVisibility((recording.timerecId != null) ? ImageView.VISIBLE : ImageView.GONE);

        isEnabled.setVisibility((htspVersion >= 23 && recording.enabled == 0) ? View.VISIBLE : View.GONE);
        isEnabled.setText(recording.enabled > 0 ? R.string.recording_enabled : R.string.recording_disabled);

        // Only show the status details in the 
        // completed and failed details screens
        if (!recording.isScheduled()) {
            if (recording.subscriptionError != null && recording.subscriptionError.length() > 0) {
                subscription_error.setVisibility(View.VISIBLE);
                subscription_error.setText(getResources().getString(
                        R.string.subscription_error, recording.subscriptionError));
            } else {
                subscription_error.setVisibility(View.GONE);
            }

            stream_errors.setText(getResources().getString(R.string.stream_errors, recording.streamErrors == null ? "0" : recording.streamErrors));
            data_errors.setText(getResources().getString(R.string.data_errors, recording.dataErrors == null ? "0" : recording.dataErrors));

            if (recording.dataSize > 1048576) {
                data_size.setText(getResources().getString(R.string.data_size, recording.dataSize / 1048576, "MB"));
            } else {
                data_size.setText(getResources().getString(R.string.data_size, recording.dataSize / 1024, "KB"));
            }
        } else {
            statusLabel.setVisibility(View.GONE);
            subscription_error.setVisibility(View.GONE);
            stream_errors.setVisibility(View.GONE);
            data_errors.setVisibility(View.GONE);
            data_size.setVisibility(View.GONE);
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
            inflater.inflate(R.menu.recording_context_menu, menu);
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
            case R.id.menu_play:
                menuUtils.handleMenuPlaySelection(-1, recording.id);
                return true;
            case R.id.menu_download:
                menuUtils.handleMenuDownloadSelection(recording.id);
                return true;
            case R.id.menu_edit:
                Intent editIntent = new Intent(getActivity(), AddEditActivity.class);
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
