package org.tvheadend.tvhclient.ui.recordings.series_recordings;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;
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
import android.widget.TextView;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.DataStorage;
import org.tvheadend.tvhclient.data.entity.Channel;
import org.tvheadend.tvhclient.data.entity.SeriesRecording;
import org.tvheadend.tvhclient.ui.base.BaseFragment;
import org.tvheadend.tvhclient.ui.recordings.common.RecordingAddEditActivity;
import org.tvheadend.tvhclient.utils.UIUtils;
import org.tvheadend.tvhclient.utils.callbacks.RecordingRemovedCallback;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

// TODO recording on all channels shows empty channel logo
// TODO use contraintlayout
// TODO split start stop time in the layout

public class SeriesRecordingDetailsFragment extends BaseFragment implements RecordingRemovedCallback {

    @BindView(R.id.is_enabled) TextView isEnabledTextView;
    @BindView(R.id.directory_label) TextView directoryLabelTextView;
    @BindView(R.id.directory) TextView directoryTextView;
    @BindView(R.id.minimum_duration) TextView minDurationTextView;
    @BindView(R.id.maximum_duration) TextView maxDurationTextView;
    @BindView(R.id.start_after_time) TextView startTimeTextView;
    @BindView(R.id.start_before_time) TextView startWindowTimeTextView;
    @BindView(R.id.days_of_week) TextView daysOfWeekTextView;
    @BindView(R.id.channel) TextView channelNameTextView;
    @BindView(R.id.name_label) TextView nameLabelTextView;
    @BindView(R.id.name) TextView nameTextView;
    @BindView(R.id.priority) TextView priorityTextView;

    @Nullable
    @BindView(R.id.nested_toolbar)
    Toolbar nestedToolbar;

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
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.recording_details_fragment, container, false);
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

        toolbarInterface.setTitle(getString(R.string.details));

        Bundle bundle = getArguments();
        if (bundle != null) {
            id = bundle.getString("id");
        }
        if (savedInstanceState != null) {
            id = savedInstanceState.getString("id");
        }

        SeriesRecordingViewModel viewModel = ViewModelProviders.of(activity).get(SeriesRecordingViewModel.class);
        viewModel.getRecording(id).observe(this, rec -> {
            recording = rec;
            updateUI();
        });

        if (nestedToolbar != null) {
            nestedToolbar.inflateMenu(R.menu.recording_details_toolbar_menu);
            nestedToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    return onOptionsItemSelected(menuItem);
                }
            });
        }
    }

    private void updateUI() {

        isEnabledTextView.setVisibility(htspVersion >= 19 ? View.VISIBLE : View.GONE);
        isEnabledTextView.setText(recording.getEnabled() > 0 ? R.string.recording_enabled : R.string.recording_disabled);

        directoryLabelTextView.setVisibility(!TextUtils.isEmpty(recording.getDirectory()) && htspVersion >= 19 ? View.VISIBLE : View.GONE);
        directoryTextView.setVisibility(!TextUtils.isEmpty(recording.getDirectory()) && htspVersion >= 19 ? View.VISIBLE : View.GONE);
        directoryTextView.setText(recording.getDirectory());

        Channel channel = DataStorage.getInstance().getChannelFromArray(recording.getChannelId());
        channelNameTextView.setText(channel != null ? channel.getChannelName() : getString(R.string.all_channels));

        nameLabelTextView.setVisibility(!TextUtils.isEmpty(recording.getName()) ? View.VISIBLE : View.GONE);
        nameTextView.setVisibility(!TextUtils.isEmpty(recording.getName()) ? View.VISIBLE : View.GONE);
        nameTextView.setText(recording.getName());

        daysOfWeekTextView.setText(UIUtils.getDaysOfWeekText(activity, recording.getDaysOfWeek()));

        String[] priorityList = getResources().getStringArray(R.array.dvr_priorities);
        if (recording.getPriority() >= 0 && recording.getPriority() < priorityList.length) {
            priorityTextView.setText(priorityList[recording.getPriority()]);
        }
        if (recording.getMinDuration() > 0) {
            // The minimum timeTextView is given in seconds, but we want to show it in minutes
            minDurationTextView.setText(getString(R.string.minutes, (int) (recording.getMinDuration() / 60)));
        }
        if (recording.getMaxDuration() > 0) {
            // The maximum timeTextView is given in seconds, but we want to show it in minutes
            maxDurationTextView.setText(getString(R.string.minutes, (int) (recording.getMaxDuration() / 60)));
        }

        startTimeTextView.setText(UIUtils.getTime(getContext(), recording.getStart()));

        startWindowTimeTextView.setText(UIUtils.getTime(getContext(), recording.getStartWindow()));
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        if (nestedToolbar != null) {
            menu = nestedToolbar.getMenu();
        }
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
        if (nestedToolbar == null) {
            inflater.inflate(R.menu.recordings_popup_menu, menu);
        } else {
            inflater.inflate(R.menu.external_search_options_menu, menu);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                activity.finish();
                return true;
            case R.id.menu_edit:
                Intent intent = new Intent(activity, RecordingAddEditActivity.class);
                intent.putExtra("type", "series_recording");
                intent.putExtra("id", recording.getId());
                activity.startActivity(intent);
                activity.finish();
                return true;
            case R.id.menu_record_remove:
                menuUtils.handleMenuRemoveSeriesRecordingSelection(recording.getId(), recording.getTitle(), this);
                return true;
            case R.id.menu_search_imdb:
                menuUtils.handleMenuSearchWebSelection(recording.getTitle());
                return true;
            case R.id.menu_search_epg:
                menuUtils.handleMenuSearchEpgSelection(recording.getTitle());
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public String getShownId() {
        return id;
    }

    @Override
    public void onRecordingRemoved() {
        activity.finish();
    }
}
