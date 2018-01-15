package org.tvheadend.tvhclient.ui.recordings.series_recordings;

import android.content.Intent;
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
import android.widget.TextView;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.DataStorage;
import org.tvheadend.tvhclient.data.model.Channel;
import org.tvheadend.tvhclient.data.model.SeriesRecording;
import org.tvheadend.tvhclient.ui.base.ToolbarInterface;
import org.tvheadend.tvhclient.ui.recordings.recordings.RecordingAddEditActivity;
import org.tvheadend.tvhclient.utils.MenuUtils;
import org.tvheadend.tvhclient.utils.UIUtils;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

// TODO recording on all channels shows empty channel logo
// TODO when a recording is updated refresh

public class SeriesRecordingDetailsFragment extends Fragment {

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

    private ToolbarInterface toolbarInterface;
    private SeriesRecording recording;
    private MenuUtils menuUtils;
    private String id;
    private Unbinder unbinder;
    private int htspVersion;

    public static SeriesRecordingDetailsFragment newInstance(String id) {
        SeriesRecordingDetailsFragment f = new SeriesRecordingDetailsFragment();
        Bundle args = new Bundle();
        args.putString("id", id);
        f.setArguments(args);
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
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

        if (getActivity() instanceof ToolbarInterface) {
            toolbarInterface = (ToolbarInterface) getActivity();
            toolbarInterface.setTitle(getString(R.string.details));
        }
        menuUtils = new MenuUtils(getActivity());
        htspVersion = DataStorage.getInstance().getProtocolVersion();
        setHasOptionsMenu(true);

        Bundle bundle = getArguments();
        if (bundle != null) {
            id = bundle.getString("id");
        }
        if (savedInstanceState != null) {
            id = savedInstanceState.getString("id");
        }

        // Get the recording so we can show its details
        recording = DataStorage.getInstance().getSeriesRecordingFromArray(id);

        if (nestedToolbar != null) {
            nestedToolbar.inflateMenu(R.menu.recording_details_toolbar_menu);
            nestedToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    return onOptionsItemSelected(menuItem);
                }
            });
        }

        isEnabledTextView.setVisibility(htspVersion >= 19 ? View.VISIBLE : View.GONE);
        isEnabledTextView.setText(recording.enabled > 0 ? R.string.recording_enabled : R.string.recording_disabled);

        directoryLabelTextView.setVisibility(!TextUtils.isEmpty(recording.directory) && htspVersion >= 19 ? View.VISIBLE : View.GONE);
        directoryTextView.setVisibility(!TextUtils.isEmpty(recording.directory) && htspVersion >= 19 ? View.VISIBLE : View.GONE);
        directoryTextView.setText(recording.directory);

        Channel channel = DataStorage.getInstance().getChannelFromArray(recording.channel);
        channelNameTextView.setText(channel != null ? channel.channelName : getString(R.string.all_channels));

        nameLabelTextView.setVisibility(!TextUtils.isEmpty(recording.name) ? View.VISIBLE : View.GONE);
        nameTextView.setVisibility(!TextUtils.isEmpty(recording.name) ? View.VISIBLE : View.GONE);
        nameTextView.setText(recording.name);

        daysOfWeekTextView.setText(UIUtils.getDaysOfWeekText(getActivity(), recording.daysOfWeek));

        String[] priorityList = getResources().getStringArray(R.array.dvr_priorities);
        if (recording.priority >= 0 && recording.priority < priorityList.length) {
            priorityTextView.setText(priorityList[recording.priority]);
        }
        if (recording.minDuration > 0) {
            // The minimum timeTextView is given in seconds, but we want to show it in minutes
            minDurationTextView.setText(getString(R.string.minutes, (int) (recording.minDuration / 60)));
        }
        if (recording.maxDuration > 0) {
            // The maximum timeTextView is given in seconds, but we want to show it in minutes
            maxDurationTextView.setText(getString(R.string.minutes, (int) (recording.maxDuration / 60)));
        }

        startTimeTextView.setText(UIUtils.getTime(getContext(), recording.start));

        startWindowTimeTextView.setText(UIUtils.getTime(getContext(), recording.startWindow));
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
                getActivity().finish();
                return true;
            case R.id.menu_edit:
                Intent intent = new Intent(getActivity(), RecordingAddEditActivity.class);
                intent.putExtra("type", "series_recording");
                intent.putExtra("id", recording.id);
                getActivity().startActivity(intent);
                return true;
            case R.id.menu_record_remove:
                menuUtils.handleMenuRemoveSeriesRecordingSelection(recording.id, recording.title);
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

    public String getShownId() {
        return id;
    }

    /**
     * Converts the given time in milliseconds to a human readable time value.
     * Adds leading zeros to the hour or minute values in case they are lower
     * then ten.
     *
     * @return time in hh:mm format
     */
    private String getTimeStringFromValue(long time) {
        if (time < 0) {
            return getString(R.string.not_set);
        }
        String minutes = String.valueOf(time % 60);
        if (minutes.length() == 1) {
            minutes = "0" + minutes;
        }
        String hours = String.valueOf(time / 60);
        if (hours.length() == 1) {
            hours = "0" + hours;
        }
        return (hours + ":" + minutes);
    }
}
