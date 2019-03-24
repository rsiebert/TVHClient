package org.tvheadend.tvhclient.ui.features.information

import android.os.Bundle
import android.view.*
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import kotlinx.android.synthetic.main.status_fragment.*
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.domain.entity.Connection
import org.tvheadend.tvhclient.domain.entity.ServerStatus
import org.tvheadend.tvhclient.ui.base.BaseFragment
import org.tvheadend.tvhclient.ui.common.tasks.WakeOnLanTask
import org.tvheadend.tvhclient.ui.features.channels.ChannelViewModel
import org.tvheadend.tvhclient.ui.features.dvr.recordings.RecordingViewModel
import org.tvheadend.tvhclient.ui.features.dvr.series_recordings.SeriesRecordingViewModel
import org.tvheadend.tvhclient.ui.features.dvr.timer_recordings.TimerRecordingViewModel
import org.tvheadend.tvhclient.ui.features.programs.ProgramViewModel

class StatusFragment : BaseFragment() {

    private lateinit var connection: Connection

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.status_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        forceSingleScreenLayout()

        toolbarInterface.setTitle(getString(R.string.status))
        toolbarInterface.setSubtitle("")

        connection = appRepository.connectionData.activeItem
        val text = "${connection.name} (${connection.hostname})"
        connection_view.text = text

        showRecordings()
        showAdditionalInformation()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.status_options_menu, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.menu_wol)?.isVisible = isUnlocked && connection.isWolEnabled
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
            programs_view.text = resources.getQuantityString(R.plurals.programs, count ?: 0, count)
        })

        val seriesRecordingViewModel = ViewModelProviders.of(this).get(SeriesRecordingViewModel::class.java)
        seriesRecordingViewModel.numberOfRecordings.observe(viewLifecycleOwner, Observer { count ->
            series_recordings_view.text = resources.getQuantityString(R.plurals.series_recordings, count
                    ?: 0, count)
        })

        val timerRecordingViewModel = ViewModelProviders.of(this).get(TimerRecordingViewModel::class.java)
        timerRecordingViewModel.numberOfRecordings.observe(viewLifecycleOwner, Observer { count ->
            timer_recordings_view.text = resources.getQuantityString(R.plurals.timer_recordings, count
                    ?: 0, count)
        })

        val recordingViewModel = ViewModelProviders.of(this).get(RecordingViewModel::class.java)
        recordingViewModel.numberOfCompletedRecordings.observe(viewLifecycleOwner, Observer { count ->
            completed_recordings_view.text = resources.getQuantityString(R.plurals.completed_recordings, count
                    ?: 0, count)
        })
        recordingViewModel.numberOfScheduledRecordings.observe(viewLifecycleOwner, Observer { count ->
            upcoming_recordings_view.text = resources.getQuantityString(R.plurals.upcoming_recordings, count
                    ?: 0, count)
        })
        recordingViewModel.numberOfFailedRecordings.observe(viewLifecycleOwner, Observer { count ->
            failed_recordings_view.text = resources.getQuantityString(R.plurals.failed_recordings, count
                    ?: 0, count)
        })
        recordingViewModel.numberOfRemovedRecordings.observe(viewLifecycleOwner, Observer { count ->
            removed_recordings_view.text = resources.getQuantityString(R.plurals.removed_recordings, count
                    ?: 0, count)
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
                currently_recording_view.text = if (currentRecText.isNotEmpty()) currentRecText.toString() else getString(R.string.nothing)
            }
        })
    }

    private fun showAdditionalInformation() {
        val channelViewModel = ViewModelProviders.of(activity).get(ChannelViewModel::class.java)
        channelViewModel.numberOfChannels.observe(viewLifecycleOwner, Observer { count ->
            val text = "$count " + getString(R.string.available)
            channels_view.text = text
        })
        channelViewModel.serverStatus.observe(viewLifecycleOwner, Observer { serverStatus ->
            if (serverStatus != null) {
                series_recordings_view.visibility = if (serverStatus.htspVersion >= 13) View.VISIBLE else View.GONE
                timer_recordings_view.visibility = if (serverStatus.htspVersion >= 18 && isUnlocked) View.VISIBLE else View.GONE
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

        server_api_version_view.text = version

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
            free_discspace_view.text = freeDiscSpace
            total_discspace_view.text = totalDiscSpace

        } catch (e: Exception) {
            free_discspace_view.setText(R.string.unknown)
            total_discspace_view.setText(R.string.unknown)
        }

    }
}
