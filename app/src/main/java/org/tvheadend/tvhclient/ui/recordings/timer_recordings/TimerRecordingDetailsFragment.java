package org.tvheadend.tvhclient.ui.recordings.timer_recordings;

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
import android.widget.TextView;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.DataStorage;
import org.tvheadend.tvhclient.data.model.Channel;
import org.tvheadend.tvhclient.data.model.TimerRecording;
import org.tvheadend.tvhclient.ui.base.ToolbarInterface;
import org.tvheadend.tvhclient.ui.recordings.recordings.RecordingAddEditActivity;
import org.tvheadend.tvhclient.utils.MenuUtils;
import org.tvheadend.tvhclient.utils.UIUtils;

import java.util.Calendar;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

// TODO when a recording is updated refresh

public class TimerRecordingDetailsFragment extends Fragment {

    @BindView(R.id.is_enabled) TextView isEnabledTextView;
    @BindView(R.id.directory_label) TextView directoryLabelTextView;
    @BindView(R.id.directory) TextView directoryTextView;
    @BindView(R.id.time) TextView timeTextView;
    @BindView(R.id.duration) TextView durationTextView;
    @BindView(R.id.days_of_week) TextView daysOfWeekTextView;
    @BindView(R.id.channel) TextView channelNameTextView;
    @BindView(R.id.priority) TextView priorityTextView;

    @Nullable
    @BindView(R.id.nested_toolbar)
    Toolbar nestedToolbar;

    private TimerRecording recording;
    private ToolbarInterface toolbarInterface;
    private MenuUtils menuUtils;
    private String id;
    private Unbinder unbinder;
    private int htspVersion;

    public static TimerRecordingDetailsFragment newInstance(String id) {
        TimerRecordingDetailsFragment f = new TimerRecordingDetailsFragment();
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
        stub.setLayoutResource(R.layout.timer_recording_details_fragment_contents);
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
        recording = DataStorage.getInstance().getTimerRecordingFromArray(id);

        if (nestedToolbar != null) {
            nestedToolbar.inflateMenu(R.menu.recording_details_toolbar_menu);
            nestedToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    return onOptionsItemSelected(menuItem);
                }
            });
        }

        isEnabledTextView.setVisibility((htspVersion >= 19) ? View.VISIBLE : View.GONE);
        isEnabledTextView.setText(recording.enabled > 0 ? R.string.recording_enabled : R.string.recording_disabled);

        directoryLabelTextView.setVisibility(htspVersion >= 19 ? View.VISIBLE : View.GONE);
        directoryTextView.setVisibility(htspVersion >= 19 ? View.VISIBLE : View.GONE);
        directoryTextView.setText(recording.directory);

        Channel channel = DataStorage.getInstance().getChannelFromArray(recording.channel);
        channelNameTextView.setText(channel != null ? channel.channelName : getString(R.string.all_channels));

        daysOfWeekTextView.setText(UIUtils.getDaysOfWeekText(getActivity(), recording.daysOfWeek));

        String[] priorityItems = getResources().getStringArray(R.array.dvr_priorities);
        if (recording.priority >= 0 && recording.priority < priorityItems.length) {
            priorityTextView.setText(priorityItems[recording.priority]);
        }

        Calendar startTime = Calendar.getInstance();
        startTime.set(Calendar.HOUR_OF_DAY, recording.start / 60);
        startTime.set(Calendar.MINUTE, recording.start % 60);
        Calendar endTime = Calendar.getInstance();
        endTime.set(Calendar.HOUR_OF_DAY, recording.stop / 60);
        endTime.set(Calendar.MINUTE, recording.stop % 60);
        String time = UIUtils.getTime(getContext(), startTime.getTimeInMillis()) + " - " + UIUtils.getTime(getContext(), endTime.getTimeInMillis());
        timeTextView.setText(time);

        durationTextView.setText(getString(R.string.minutes, (recording.stop - recording.start)));
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
    public void onSaveInstanceState(Bundle outState) {
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
                intent.putExtra("type", "timer_recording");
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
}
