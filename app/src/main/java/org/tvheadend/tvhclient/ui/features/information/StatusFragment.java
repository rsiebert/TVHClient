package org.tvheadend.tvhclient.ui.features.information;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.domain.entity.Channel;
import org.tvheadend.tvhclient.domain.entity.Connection;
import org.tvheadend.tvhclient.domain.entity.Recording;
import org.tvheadend.tvhclient.domain.entity.ServerStatus;
import org.tvheadend.tvhclient.ui.features.channels.ChannelViewModel;
import org.tvheadend.tvhclient.ui.features.dvr.recordings.RecordingViewModel;
import org.tvheadend.tvhclient.ui.features.dvr.series_recordings.SeriesRecordingViewModel;
import org.tvheadend.tvhclient.ui.features.dvr.timer_recordings.TimerRecordingViewModel;
import org.tvheadend.tvhclient.ui.features.programs.ProgramViewModel;
import org.tvheadend.tvhclient.ui.base.BaseFragment;
import org.tvheadend.tvhclient.util.tasks.WakeOnLanTask;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProviders;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class StatusFragment extends BaseFragment {

    @BindView(R.id.connection)
    TextView connectionTextView;
    @BindView(R.id.channels)
    TextView channelsTextView;
    @BindView(R.id.programs)
    TextView programsTextView;
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
    @BindView(R.id.currently_recording)
    TextView currentlyRecordingTextView;
    @BindView(R.id.free_discspace)
    TextView freeDiscSpaceTextView;
    @BindView(R.id.total_discspace)
    TextView totalDiscSpaceTextView;
    @BindView(R.id.server_api_version)
    TextView serverApiVersionTextView;

    private Unbinder unbinder;
    private Connection connection;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
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
        forceSingleScreenLayout();

        toolbarInterface.setTitle(getString(R.string.status));
        toolbarInterface.setSubtitle("");

        connection = appRepository.getConnectionData().getActiveItem();
        String text = connection.getName() + " (" + connection.getHostname() + ")";
        connectionTextView.setText(text);

        showRecordings();
        showAdditionalInformation();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.status_options_menu, menu);
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (isUnlocked && connection != null && connection.isWolEnabled()) {
            menu.findItem(R.id.menu_wol).setVisible(true);
        } else {
            menu.findItem(R.id.menu_wol).setVisible(false);
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
                    new WakeOnLanTask(activity, connection).execute();
                }
                return true;
            case R.id.menu_refresh:
                menuUtils.handleMenuReconnectSelection();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showRecordings() {

        ProgramViewModel programViewModel = ViewModelProviders.of(this).get(ProgramViewModel.class);
        programViewModel.getNumberOfPrograms().observe(getViewLifecycleOwner(), count -> programsTextView.setText(getResources().getQuantityString(R.plurals.programs, count != null ? count : 0, count)));

        SeriesRecordingViewModel seriesRecordingViewModel = ViewModelProviders.of(this).get(SeriesRecordingViewModel.class);
        seriesRecordingViewModel.getNumberOfRecordings().observe(getViewLifecycleOwner(), count -> seriesRecordingsTextView.setText(getResources().getQuantityString(R.plurals.series_recordings, count != null ? count : 0, count)));

        TimerRecordingViewModel timerRecordingViewModel = ViewModelProviders.of(this).get(TimerRecordingViewModel.class);
        timerRecordingViewModel.getNumberOfRecordings().observe(getViewLifecycleOwner(), count -> timerRecordingsTextView.setText(getResources().getQuantityString(R.plurals.timer_recordings, count != null ? count : 0, count)));

        RecordingViewModel recordingViewModel = ViewModelProviders.of(this).get(RecordingViewModel.class);
        recordingViewModel.getNumberOfCompletedRecordings().observe(getViewLifecycleOwner(), count -> completedRecordingsTextView.setText(getResources().getQuantityString(R.plurals.completed_recordings, count != null ? count : 0, count)));
        recordingViewModel.getNumberOfScheduledRecordings().observe(getViewLifecycleOwner(), count -> upcomingRecordingsTextView.setText(getResources().getQuantityString(R.plurals.upcoming_recordings, count != null ? count : 0, count)));
        recordingViewModel.getNumberOfFailedRecordings().observe(getViewLifecycleOwner(), count -> failedRecordingsTextView.setText(getResources().getQuantityString(R.plurals.failed_recordings, count != null ? count : 0, count)));
        recordingViewModel.getNumberOfRemovedRecordings().observe(getViewLifecycleOwner(), count -> removedRecordingsTextView.setText(getResources().getQuantityString(R.plurals.removed_recordings, count != null ? count : 0, count)));

        // Get the programs that are currently being recorded
        recordingViewModel.getScheduledRecordings().observe(getViewLifecycleOwner(), recordings -> {
            if (recordings != null) {
                StringBuilder currentRecText = new StringBuilder();
                for (Recording rec : recordings) {
                    if (rec.isRecording()) {
                        currentRecText.append(getString(R.string.currently_recording)).append(": ").append(rec.getTitle());
                        Channel channel = appRepository.getChannelData().getItemById(rec.getChannelId());
                        if (channel != null) {
                            currentRecText.append(" (").append(getString(R.string.channel)).append(" ").append(channel.getName()).append(")\n");
                        }
                    }
                }
                // Show which programs are being recorded
                currentlyRecordingTextView.setText(currentRecText.length() > 0 ? currentRecText.toString() : getString(R.string.nothing));
            }
        });
    }

    private void showAdditionalInformation() {
        ChannelViewModel channelViewModel = ViewModelProviders.of(activity).get(ChannelViewModel.class);
        channelViewModel.getNumberOfChannels().observe(getViewLifecycleOwner(), count -> {
            final String text = count + " " + getString(R.string.available);
            channelsTextView.setText(text);
        });
        channelViewModel.getServerStatus().observe(getViewLifecycleOwner(), serverStatus -> {
            if (serverStatus != null) {
                seriesRecordingsTextView.setVisibility((serverStatus.getHtspVersion() >= 13) ? View.VISIBLE : View.GONE);
                timerRecordingsTextView.setVisibility((serverStatus.getHtspVersion() >= 18 && isUnlocked) ? View.VISIBLE : View.GONE);
                showServerInformation(serverStatus);
            }
        });
    }

    /**
     * Shows the server api version and the available and total disc
     * space either in MB or GB to avoid showing large numbers.
     * This depends on the size of the value.
     */
    private void showServerInformation(ServerStatus serverStatus) {

        String version = String.valueOf(serverStatus.getHtspVersion())
                + "   (" + getString(R.string.server) + ": "
                + serverStatus.getServerName() + " "
                + serverStatus.getServerVersion() + ")";

        serverApiVersionTextView.setText(version);

        try {
            // Get the disc space values and convert them to megabytes
            long free = serverStatus.getFreeDiskSpace() / 1000000;
            long total = serverStatus.getTotalDiskSpace() / 1000000;

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
}
