package org.tvheadend.tvhclient.fragments.recordings;

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

import org.tvheadend.tvhclient.Constants;
import org.tvheadend.tvhclient.DataStorage;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.activities.ToolbarInterfaceLight;
import org.tvheadend.tvhclient.model.Channel;
import org.tvheadend.tvhclient.model.SeriesRecording;
import org.tvheadend.tvhclient.utils.MenuUtils;
import org.tvheadend.tvhclient.utils.Utils;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class SeriesRecordingDetailsFragment extends Fragment {

    @BindView(R.id.is_enabled) TextView isEnabled;
    @BindView(R.id.directory_label) TextView directoryLabel;
    @BindView(R.id.directory) TextView directory;
    @BindView(R.id.minimum_duration) TextView minDuration;
    @BindView(R.id.maximum_duration) TextView maxDuration;
    @BindView(R.id.start_after_time) TextView startTime;
    @BindView(R.id.start_before_time) TextView startWindowTime;
    @BindView(R.id.days_of_week) TextView daysOfWeek;
    @BindView(R.id.channel) TextView channelName;
    @BindView(R.id.name_label) TextView nameLabel;
    @BindView(R.id.name) TextView name;
    @BindView(R.id.priority) TextView priority;

    @Nullable
    @BindView(R.id.nested_toolbar)
    Toolbar nestedToolbar;

    private ToolbarInterfaceLight toolbarInterface;
    private SeriesRecording recording;
    private MenuUtils menuUtils;
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.recording_details_fragment, container, false);
        ViewStub stub = view.findViewById(R.id.stub);
        stub.setLayoutResource(R.layout.series_recording_details_content_fragment);
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

        if (getActivity() instanceof ToolbarInterfaceLight) {
            toolbarInterface = (ToolbarInterfaceLight) getActivity();
            toolbarInterface.setTitle("Details");
        }
        menuUtils = new MenuUtils(getActivity());
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
            nestedToolbar.inflateMenu(R.menu.recording_toolbar_menu);
            nestedToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    return onOptionsItemSelected(menuItem);
                }
            });
        }

        isEnabled.setVisibility(DataStorage.getInstance().getProtocolVersion() >= Constants.MIN_API_VERSION_REC_FIELD_ENABLED ? View.VISIBLE : View.GONE);
        isEnabled.setText(recording.enabled > 0 ? R.string.recording_enabled : R.string.recording_disabled);

        directoryLabel.setVisibility(DataStorage.getInstance().getProtocolVersion() >= Constants.MIN_API_VERSION_REC_FIELD_DIRECTORY ? View.VISIBLE : View.GONE);
        directory.setVisibility(DataStorage.getInstance().getProtocolVersion() >= Constants.MIN_API_VERSION_REC_FIELD_DIRECTORY ? View.VISIBLE : View.GONE);
        directory.setText(recording.directory);

        Channel channel = DataStorage.getInstance().getChannelFromArray(recording.channel);
        channelName.setText(channel != null ? channel.channelName : getString(R.string.all_channels));

        Utils.setDescription(nameLabel, name, recording.name);
        Utils.setDaysOfWeek(getActivity(), null, daysOfWeek, recording.daysOfWeek);

        String[] priorityItems = getResources().getStringArray(R.array.dvr_priorities);
        if (recording.priority >= 0 && recording.priority < priorityItems.length) {
            priority.setText(priorityItems[recording.priority]);
        }
        if (recording.minDuration > 0) {
            // The minimum time is given in seconds, but we want to show it in minutes
            minDuration.setText(getString(R.string.minutes, (int) (recording.minDuration / 60)));
        }
        if (recording.maxDuration > 0) {
            // The maximum time is given in seconds, but we want to show it in minutes
            maxDuration.setText(getString(R.string.minutes, (int) (recording.maxDuration / 60)));
        }
        startTime.setText(Utils.getTimeStringFromValue(getActivity(), recording.start));
        startWindowTime.setText(Utils.getTimeStringFromValue(getActivity(), recording.startWindow));
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        //
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
            case R.id.menu_edit:
                /*Intent editIntent = new Intent(getActivity(), SeriesRecordingAddActivity.class);
                editIntent.putExtra("id", recording.id);
                getActivity().startActivity(editIntent);*/
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
