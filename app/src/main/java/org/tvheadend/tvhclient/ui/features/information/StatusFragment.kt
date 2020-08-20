package org.tvheadend.tvhclient.ui.features.information

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import kotlinx.android.synthetic.main.status_fragment.*
import org.tvheadend.data.entity.ServerStatus
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.service.HtspService
import org.tvheadend.tvhclient.ui.base.BaseFragment
import org.tvheadend.tvhclient.ui.common.interfaces.LayoutControlInterface
import org.tvheadend.tvhclient.ui.features.dvr.recordings.RecordingViewModel
import timber.log.Timber

class StatusFragment : BaseFragment() {

    private lateinit var statusViewModel: StatusViewModel
    private lateinit var loadDataTask: Runnable
    private val loadDataHandler = Handler()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.status_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        statusViewModel = ViewModelProvider(requireActivity()).get(StatusViewModel::class.java)

        if (activity is LayoutControlInterface) {
            (activity as LayoutControlInterface).forceSingleScreenLayout()
        }

        toolbarInterface.setTitle(getString(R.string.status))
        toolbarInterface.setSubtitle("")

        showStatus()
        showSubscriptionAndInputStatus()

        loadDataTask = Runnable {
            val service = activity?.getSystemService(Context.ACTIVITY_SERVICE)
            service?.let {
                val activityManager = service as ActivityManager?
                val runningAppProcessInfo = activityManager?.runningAppProcesses?.get(0)
                if (runningAppProcessInfo != null
                        && runningAppProcessInfo.importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {

                    Timber.d("Application is in the foreground, starting service to get updated subscriptions and input information")
                    val intent = Intent(activity, HtspService::class.java)
                    intent.action = "getSubscriptions"
                    activity?.startService(intent)
                    intent.action = "getInputs"
                    activity?.startService(intent)
                }
                Timber.d("Restarting additional information update handler in 60s")
                loadDataHandler.postDelayed(loadDataTask, 60000)
            }
        }

        baseViewModel.connectionToServerAvailableLiveData.observe(viewLifecycleOwner,  { connectionAvailable ->
            Timber.d("Connection to server availability changed to $connectionAvailable")
            if (connectionAvailable) {
                Timber.d("Starting additional information update handler")
                loadDataHandler.post(loadDataTask)
            } else {
                loadDataHandler.removeCallbacks(loadDataTask)
            }
        })
    }

    private fun showStatus() {

        val text = "${connection.name} (${connection.serverUrl})"
        connection_view.text = text

        series_recordings_view.visibility = if (htspVersion >= 13) View.VISIBLE else View.GONE
        timer_recordings_view.visibility = if (htspVersion >= 18 && isUnlocked) View.VISIBLE else View.GONE

        statusViewModel.channelCount.observe(viewLifecycleOwner,  { count ->
            val channelCountText = "$count ${getString(R.string.available)}"
            channels_view.text = channelCountText
        })
        statusViewModel.programCount.observe(viewLifecycleOwner,  { count ->
            programs_view.text = resources.getQuantityString(R.plurals.programs, count ?: 0, count)
        })
        statusViewModel.seriesRecordingCount.observe(viewLifecycleOwner,  { count ->
            series_recordings_view.text = resources.getQuantityString(R.plurals.series_recordings, count
                    ?: 0, count)
        })
        statusViewModel.timerRecordingCount.observe(viewLifecycleOwner,  { count ->
            timer_recordings_view.text = resources.getQuantityString(R.plurals.timer_recordings, count
                    ?: 0, count)
        })
        statusViewModel.completedRecordingCount.observe(viewLifecycleOwner,  { count ->
            completed_recordings_view.text = resources.getQuantityString(R.plurals.completed_recordings, count
                    ?: 0, count)
        })
        statusViewModel.scheduledRecordingCount.observe(viewLifecycleOwner,  { count ->
            upcoming_recordings_view.text = resources.getQuantityString(R.plurals.upcoming_recordings, count
                    ?: 0, count)
        })
        statusViewModel.failedRecordingCount.observe(viewLifecycleOwner,  { count ->
            failed_recordings_view.text = resources.getQuantityString(R.plurals.failed_recordings, count
                    ?: 0, count)
        })
        statusViewModel.removedRecordingCount.observe(viewLifecycleOwner,  { count ->
            removed_recordings_view.text = resources.getQuantityString(R.plurals.removed_recordings, count
                    ?: 0, count)
        })
        statusViewModel.serverStatusLiveData.observe(viewLifecycleOwner,  { serverStatus ->
            if (serverStatus != null) {
                showServerInformation(serverStatus)
            }
        })

        // Get the programs that are currently being recorded
        val recordingViewModel = ViewModelProvider(this).get(RecordingViewModel::class.java)
        recordingViewModel.scheduledRecordings.observe(viewLifecycleOwner,  { recordings ->
            if (recordings != null) {
                val currentRecText = StringBuilder()
                for (rec in recordings) {
                    if (rec.isRecording) {
                        currentRecText.append(getString(R.string.currently_recording)).append(": ").append(rec.title)
                        val channel = statusViewModel.getChannelById(rec.channelId)
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
            val free = serverStatus.freeDiskSpace / 1024 / 1024
            val total = serverStatus.totalDiskSpace / 1024 / 1024

            val freeDiscSpace: String
            val totalDiscSpace: String

            // Show the free amount of disc space as GB or MB
            freeDiscSpace = if (free > 1024) {
                (free / 1024).toString() + " GB " + getString(R.string.available)
            } else {
                free.toString() + " MB " + getString(R.string.available)
            }
            // Show the total amount of disc space as GB or MB
            totalDiscSpace = if (total > 1024) {
                (total / 1024).toString() + " GB " + getString(R.string.total)
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

    private fun showSubscriptionAndInputStatus() {
        statusViewModel.subscriptions.observe(viewLifecycleOwner,  { subscriptions ->
            if (subscriptions != null) {
                Timber.d("Received subscription status")
            }
        })
        statusViewModel.inputs.observe(viewLifecycleOwner,  { inputs ->
            if (inputs != null) {
                Timber.d("Received input status")
            }
        })
    }

    override fun onPause() {
        super.onPause()
        loadDataHandler.removeCallbacks(loadDataTask)
    }
}
