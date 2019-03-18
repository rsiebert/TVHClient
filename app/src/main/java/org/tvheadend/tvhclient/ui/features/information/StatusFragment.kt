package org.tvheadend.tvhclient.ui.features.information

import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.Unbinder
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.domain.entity.Connection
import org.tvheadend.tvhclient.domain.entity.ServerStatus
import org.tvheadend.tvhclient.ui.base.BaseFragment
import org.tvheadend.tvhclient.ui.features.channels.ChannelViewModel
import org.tvheadend.tvhclient.ui.features.dvr.recordings.RecordingViewModel
import org.tvheadend.tvhclient.ui.features.dvr.series_recordings.SeriesRecordingViewModel
import org.tvheadend.tvhclient.ui.features.dvr.timer_recordings.TimerRecordingViewModel
import org.tvheadend.tvhclient.ui.features.programs.ProgramViewModel
import org.tvheadend.tvhclient.ui.common.tasks.WakeOnLanTask

class StatusFragment : BaseFragment() {

    @BindView(R.id.connection)
    lateinit var connectionTextView: TextView
    @BindView(R.id.channels)
    lateinit var channelsTextView: TextView
    @BindView(R.id.programs)
    lateinit var programsTextView: TextView
    @BindView(R.id.completed_recordings)
    lateinit var completedRecordingsTextView: TextView
    @BindView(R.id.upcoming_recordings)
    lateinit var upcomingRecordingsTextView: TextView
    @BindView(R.id.failed_recordings)
    lateinit var failedRecordingsTextView: TextView
    @BindView(R.id.removed_recordings)
    lateinit var removedRecordingsTextView: TextView
    @BindView(R.id.series_recordings)
    lateinit var seriesRecordingsTextView: TextView
    @BindView(R.id.timer_recordings)
    lateinit var timerRecordingsTextView: TextView
    @BindView(R.id.currently_recording)
    lateinit var currentlyRecordingTextView: TextView
    @BindView(R.id.free_discspace)
    lateinit var freeDiscSpaceTextView: TextView
    @BindView(R.id.total_discspace)
    lateinit var totalDiscSpaceTextView: TextView
    @BindView(R.id.server_api_version)
    lateinit var serverApiVersionTextView: TextView

    private lateinit var unbinder: Unbinder
    private lateinit var connection: Connection

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.status_fragment, container, false)
        unbinder = ButterKnife.bind(this, view)
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        unbinder.unbind()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        forceSingleScreenLayout()

        toolbarInterface.setTitle(getString(R.string.status))
        toolbarInterface.setSubtitle("")

        connection = appRepository.connectionData.activeItem
        val text = "${connection.name} (${connection.hostname})"
        connectionTextView.text = text

        showRecordings()
        showAdditionalInformation()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.status_options_menu, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.menu_wol).isVisible = isUnlocked && connection.isWolEnabled
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                activity.finish()
                true
            }
            R.id.menu_wol -> {
                WakeOnLanTask(activity, connection).execute()
                true
            }
            R.id.menu_refresh -> {
                menuUtils.handleMenuReconnectSelection()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showRecordings() {

        val programViewModel = ViewModelProviders.of(this).get(ProgramViewModel::class.java)
        programViewModel.numberOfPrograms.observe(viewLifecycleOwner, Observer { count ->
            programsTextView.text = resources.getQuantityString(R.plurals.programs, count ?: 0, count)
        })

        val seriesRecordingViewModel = ViewModelProviders.of(this).get(SeriesRecordingViewModel::class.java)
        seriesRecordingViewModel.numberOfRecordings.observe(viewLifecycleOwner, Observer { count ->
            seriesRecordingsTextView.text = resources.getQuantityString(R.plurals.series_recordings, count ?: 0, count)
        })

        val timerRecordingViewModel = ViewModelProviders.of(this).get(TimerRecordingViewModel::class.java)
        timerRecordingViewModel.numberOfRecordings.observe(viewLifecycleOwner, Observer { count ->
            timerRecordingsTextView.text = resources.getQuantityString(R.plurals.timer_recordings, count ?: 0, count) })

        val recordingViewModel = ViewModelProviders.of(this).get(RecordingViewModel::class.java)
        recordingViewModel.numberOfCompletedRecordings.observe(viewLifecycleOwner, Observer { count ->
            completedRecordingsTextView.text = resources.getQuantityString(R.plurals.completed_recordings, count ?: 0, count)
        })
        recordingViewModel.numberOfScheduledRecordings.observe(viewLifecycleOwner, Observer { count ->
            upcomingRecordingsTextView.text = resources.getQuantityString(R.plurals.upcoming_recordings, count ?: 0, count)
        })
        recordingViewModel.numberOfFailedRecordings.observe(viewLifecycleOwner, Observer { count ->
            failedRecordingsTextView.text = resources.getQuantityString(R.plurals.failed_recordings, count ?: 0, count)
        })
        recordingViewModel.numberOfRemovedRecordings.observe(viewLifecycleOwner, Observer { count ->
            removedRecordingsTextView.text = resources.getQuantityString(R.plurals.removed_recordings, count ?: 0, count)
        })

        // Get the programs that are currently being recorded
        recordingViewModel.scheduledRecordings.observe(viewLifecycleOwner, Observer { recordings ->
            if (recordings != null) {
                val currentRecText = StringBuilder()
                for (rec in recordings) {
                    if (rec.isRecording) {
                        currentRecText.append(getString(R.string.currently_recording)).append(": ").append(rec.title)
                        val channel = appRepository.channelData.getItemById(rec.channelId)
                        if (channel != null) {
                            currentRecText.append(" (").append(getString(R.string.channel)).append(" ").append(channel.name).append(")\n")
                        }
                    }
                }
                // Show which programs are being recorded
                currentlyRecordingTextView.text = if (currentRecText.isNotEmpty()) currentRecText.toString() else getString(R.string.nothing)
            }
        })
    }

    private fun showAdditionalInformation() {
        val channelViewModel = ViewModelProviders.of(activity).get(ChannelViewModel::class.java)
        channelViewModel.numberOfChannels.observe(viewLifecycleOwner, Observer { count ->
            val text = "$count " + getString(R.string.available)
            channelsTextView.text = text
        })
        channelViewModel.serverStatus.observe(viewLifecycleOwner, Observer { serverStatus ->
            if (serverStatus != null) {
                seriesRecordingsTextView.visibility = if (serverStatus.htspVersion >= 13) View.VISIBLE else View.GONE
                timerRecordingsTextView.visibility = if (serverStatus.htspVersion >= 18 && isUnlocked) View.VISIBLE else View.GONE
                showServerInformation(serverStatus)
            }
        })
    }

    /**
     * Shows the server api version and the available and total disc
     * space either in MB or GB to avoid showing large numbers.
     * This depends on the size of the value.
     */
    private fun showServerInformation(serverStatus: ServerStatus) {

        val version = (serverStatus.htspVersion.toString()
                + "   (" + getString(R.string.server) + ": "
                + serverStatus.serverName + " "
                + serverStatus.serverVersion + ")")

        serverApiVersionTextView.text = version

        try {
            // Get the disc space values and convert them to megabytes
            val free = serverStatus.freeDiskSpace / 1000000
            val total = serverStatus.totalDiskSpace / 1000000

            val freeDiscSpace: String
            val totalDiscSpace: String

            // Show the free amount of disc space as GB or MB
            freeDiscSpace = if (free > 1000) {
                (free / 1000).toString() + " GB " + getString(R.string.available)
            } else {
                free.toString() + " MB " + getString(R.string.available)
            }
            // Show the total amount of disc space as GB or MB
            totalDiscSpace = if (total > 1000) {
                (total / 1000).toString() + " GB " + getString(R.string.total)
            } else {
                total.toString() + " MB " + getString(R.string.total)
            }
            freeDiscSpaceTextView.text = freeDiscSpace
            totalDiscSpaceTextView.text = totalDiscSpace

        } catch (e: Exception) {
            freeDiscSpaceTextView.setText(R.string.unknown)
            totalDiscSpaceTextView.setText(R.string.unknown)
        }

    }
}
