package org.tvheadend.tvhclient.ui.misc;

import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.data.DataRepository;
import org.tvheadend.tvhclient.data.DataStorage;
import org.tvheadend.tvhclient.data.entity.Channel;
import org.tvheadend.tvhclient.data.entity.Connection;
import org.tvheadend.tvhclient.data.entity.Recording;
import org.tvheadend.tvhclient.data.model.DiscSpace;
import org.tvheadend.tvhclient.data.repository.ConnectionDataRepository;
import org.tvheadend.tvhclient.data.tasks.WakeOnLanTask;
import org.tvheadend.tvhclient.data.tasks.WakeOnLanTaskCallback;
import org.tvheadend.tvhclient.ui.base.ToolbarInterface;
import org.tvheadend.tvhclient.ui.channels.ChannelViewModel;
import org.tvheadend.tvhclient.ui.recordings.recordings.RecordingViewModel;
import org.tvheadend.tvhclient.ui.recordings.series_recordings.SeriesRecordingViewModel;
import org.tvheadend.tvhclient.ui.recordings.timer_recordings.TimerRecordingViewModel;
import org.tvheadend.tvhclient.utils.MenuUtils;

import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

// TODO use combined model like the one for the navigation drawer

public class StatusFragment extends Fragment implements WakeOnLanTaskCallback {

    private AppCompatActivity activity;

    // This information is always available
    @BindView(R.id.connection)
    TextView connectionTextView;
    @BindView(R.id.channels_label)
    TextView channelsLabelTextView;
    @BindView(R.id.channels)
    TextView channelsTextView;
    @BindView(R.id.recording_label)
    TextView recordingsLabelTextView;
    @BindView(R.id.completed_recordings)
    TextView completedRecordingsTextView;
    @BindView(R.id.upcoming_recordings)
    TextView upcomingRecordingsTextView;
    @BindView(R.id.failed_recordings)
    TextView failedRecordingsTextView;
    @BindView(R.id.removed_recordings)
    TextView removedRecordingsTextView;
    @BindView(R.id.series_recordings)
    TextView seriesRecordingsTextView;
    @BindView(R.id.timer_recordings)
    TextView timerRecordingsTextView;
    @BindView(R.id.currently_recording_label)
    TextView currentlyRecordingLabelTextView;
    @BindView(R.id.currently_recording)
    TextView currentlyRecordingTextView;
    @BindView(R.id.discspace_label)
    TextView discSpaceLabelTextView;
    @BindView(R.id.free_discspace)
    TextView freeDiscSpaceTextView;
    @BindView(R.id.total_discspace)
    TextView totalDiscSpaceTextView;
    @BindView(R.id.server_api_version_label)
    TextView serverApiVersionLabelTextView;
    @BindView(R.id.server_api_version)
    TextView serverApiVersionTextView;

    private Unbinder unbinder;
    private int htspVersion;
    private boolean isUnlocked;
    private MenuUtils menuUtils;
    private Connection connection;
    private ConnectionDataRepository connectionRepository;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.status_fragment, container, false);
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

        activity = (AppCompatActivity) getActivity();
        if (activity instanceof ToolbarInterface) {
            ToolbarInterface toolbarInterface = (ToolbarInterface) activity;
            toolbarInterface.setTitle(getString(R.string.status));
            toolbarInterface.setSubtitle(null);
        }

        connectionRepository = new ConnectionDataRepository(activity);
        connection = connectionRepository.getActiveConnectionSync();
        DataRepository repository = new DataRepository(activity);
        menuUtils = new MenuUtils(activity);
        htspVersion = repository.getHtspVersion();
        isUnlocked = TVHClientApplication.getInstance().isUnlocked();

        setHasOptionsMenu(true);

        SeriesRecordingViewModel seriesRecordingViewModel = ViewModelProviders.of(activity).get(SeriesRecordingViewModel.class);
        seriesRecordingViewModel.getRecordings().observe(this, recordings -> {
            if (recordings != null) {
                seriesRecordingsTextView.setText(getResources().getQuantityString(
                        R.plurals.series_recordings, recordings.size(), recordings.size()));
            }
        });

        TimerRecordingViewModel timerRecordingViewModel = ViewModelProviders.of(activity).get(TimerRecordingViewModel.class);
        timerRecordingViewModel.getRecordings().observe(this, recordings -> {
            if (recordings != null) {
                timerRecordingsTextView.setText(getResources().getQuantityString(
                        R.plurals.timer_recordings, recordings.size(), recordings.size()));
            }
        });

        RecordingViewModel recordingViewModel = ViewModelProviders.of(activity).get(RecordingViewModel.class);
        recordingViewModel.getCompletedRecordings().observe(this, recordings -> {
            if (recordings != null) {
                completedRecordingsTextView.setText(getResources().getQuantityString(
                        R.plurals.completed_recordings, recordings.size(), recordings.size()));
            }
        });
        recordingViewModel.getScheduledRecordings().observe(this, recordings -> {
            if (recordings != null) {
                upcomingRecordingsTextView.setText(getResources().getQuantityString(
                        R.plurals.upcoming_recordings, recordings.size(), recordings.size()));
            }
        });
        recordingViewModel.getFailedRecordings().observe(this, recordings -> {
            if (recordings != null) {
                failedRecordingsTextView.setText(getResources().getQuantityString(
                        R.plurals.failed_recordings, recordings.size(), recordings.size()));
            }
        });
        recordingViewModel.getRemovedRecordings().observe(this, recordings -> {
            if (recordings != null) {
                removedRecordingsTextView.setText(getResources().getQuantityString(
                        R.plurals.removed_recordings, recordings.size(), recordings.size()));
            }
        });

        ChannelViewModel channelViewModel = ViewModelProviders.of(activity).get(ChannelViewModel.class);
        channelViewModel.getChannelsByTime().observe(this, channels -> {
            if (channels != null) {
                // Show the number of available channelTextView
                final String text = channels.size() + " " + getString(R.string.available);
                channelsTextView.setText(text);
            }
        });
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.status_options_menu, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (isUnlocked && connection != null && !TextUtils.isEmpty(connection.getWolMacAddress())) {
            menu.findItem(R.id.menu_wol).setVisible(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                activity.finish();
                return true;
            case R.id.menu_wol:
                if (connection != null) {
                    WakeOnLanTask task = new WakeOnLanTask(activity, this, connection);
                    task.execute();
                }
                return true;
            case R.id.menu_refresh:
                menuUtils.handleMenuReconnectSelection();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // The connection is ok and not loading anymore, show all data
        seriesRecordingsTextView.setVisibility((htspVersion >= 13) ? View.VISIBLE : View.GONE);
        timerRecordingsTextView.setVisibility((htspVersion >= 18 && isUnlocked) ? View.VISIBLE : View.GONE);

        showConnectionDetails();
        showRecordingStatus();
        showDiscSpace();
        showServerStatus();
    }

    /**
     * Shows the name and address of a connection, otherwise shows an
     * information that no connection is selected or available.
     */
    private void showConnectionDetails() {
        if (connection == null) {
            if (connectionRepository.getAllConnectionsSync() == null) {
                connectionTextView.setText(R.string.no_connection_available_advice);
            } else {
                connectionTextView.setText(R.string.no_connection_active_advice);
            }
        } else {
            String text = connection.getName() + " (" + connection.getHostname() + ")";
            connectionTextView.setText(text);
        }
    }

    /**
     * Shows the available and total disc space either in MB or GB to avoid
     * showing large numbers. This depends on the size of the value.
     */
    private void showDiscSpace() {
        DiscSpace discSpace = DataStorage.getInstance().getDiscSpace();
        if (discSpace == null) {
            freeDiscSpaceTextView.setText(R.string.unknown);
            totalDiscSpaceTextView.setText(R.string.unknown);
            return;
        }

        try {
            // Get the disc space values and convert them to megabytes
            long free = Long.valueOf(discSpace.freediskspace) / 1000000;
            long total = Long.valueOf(discSpace.totaldiskspace) / 1000000;

            String freeDiscSpace;
            String totalDiscSpace;

            // Show the free amount of disc space as GB or MB
            if (free > 1000) {
                freeDiscSpace = (free / 1000) + " GB " + getString(R.string.available);
            } else {
                freeDiscSpace = free + " MB " + getString(R.string.available);
            }
            // Show the total amount of disc space as GB or MB
            if (total > 1000) {
                totalDiscSpace = (total / 1000) + " GB " + getString(R.string.total);
            } else {
                totalDiscSpace = total + " MB " + getString(R.string.total);
            }
            freeDiscSpaceTextView.setText(freeDiscSpace);
            totalDiscSpaceTextView.setText(totalDiscSpace);

        } catch (Exception e) {
            freeDiscSpaceTextView.setText(R.string.unknown);
            totalDiscSpaceTextView.setText(R.string.unknown);
        }
    }

    /**
     * Shows the program that is currently being recorded and the summary about
     * the available, scheduled and failed recordings.
     */
    private void showRecordingStatus() {

        // Get the programs that are currently being recorded
        StringBuilder currentRecText = new StringBuilder();
        Map<Integer, Recording> map = DataStorage.getInstance().getRecordingsFromArray();
        for (Recording rec : map.values()) {
            if (rec.isRecording()) {
                currentRecText.append(getString(R.string.currently_recording)).append(": ").append(rec.getTitle());
                Channel channel = DataStorage.getInstance().getChannelFromArray(rec.getChannelId());
                if (channel != null) {
                    currentRecText.append(" (").append(getString(R.string.channel)).append(" ").append(channel.getChannelName()).append(")\n");
                }
            }
        }

        // Show which programs are being recorded
        currentlyRecordingTextView.setText(currentRecText.length() > 0 ? currentRecText.toString()
                : getString(R.string.nothing));
    }

    private void showServerStatus() {
        String version = String.valueOf(htspVersion)
                + "   (" + getString(R.string.server) + ": "
                + DataStorage.getInstance().getServerName() + " "
                + DataStorage.getInstance().getServerVersion() + ")";
        currentlyRecordingTextView.setText(version);
    }

    @Override
    public void notify(String message) {
        if (getView() != null) {
            Snackbar.make(getView(), message, Snackbar.LENGTH_LONG).show();
        }
    }
}
