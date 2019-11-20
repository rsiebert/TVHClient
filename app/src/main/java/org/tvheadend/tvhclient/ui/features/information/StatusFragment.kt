package org.tvheadend.tvhclient.ui.features.information

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.*
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import kotlinx.android.synthetic.main.status_fragment.*
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.service.HtspService
import org.tvheadend.tvhclient.data.entity.ServerStatus
import org.tvheadend.tvhclient.ui.base.BaseFragment
import org.tvheadend.tvhclient.ui.common.callbacks.LayoutInterface
import org.tvheadend.tvhclient.ui.common.sendWakeOnLanPacket
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
        statusViewModel = ViewModelProviders.of(activity!!).get(StatusViewModel::class.java)

        if (activity is LayoutInterface) {
            (activity as LayoutInterface).forceSingleScreenLayout()
        }

        toolbarInterface.setTitle(getString(R.string.status))
        toolbarInterface.setSubtitle("")

        showStatus()
        showSubscriptionAndInputStatus()

        loadDataTask = Runnable {
            val activityManager: ActivityManager? = activity?.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
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

        baseViewModel.connectionToServerAvailable.observe(this, Observer { connectionAvailable ->
            Timber.d("Connection to server availability changed to $connectionAvailable")
            if (connectionAvailable) {
                Timber.d("Starting additional information update handler")
                loadDataHandler.post(loadDataTask)
            } else {
                loadDataHandler.removeCallbacks(loadDataTask)
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.status_options_menu, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.menu_send_wake_on_lan_packet)?.isVisible = isUnlocked && connection.isWolEnabled
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val ctx = context ?: return super.onOptionsItemSelected(item)
        return when (item.itemId) {
            R.id.menu_send_wake_on_lan_packet -> sendWakeOnLanPacket(ctx, connection)
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showStatus() {

        val text = "${connection.name} (${connection.serverUrl})"
        connection_view.text = text

        series_recordings_view.visibility = if (htspVersion >= 13) View.VISIBLE else View.GONE
        timer_recordings_view.visibility = if (htspVersion >= 18 && isUnlocked) View.VISIBLE else View.GONE

        statusViewModel.channelCount.observe(viewLifecycleOwner, Observer { count ->
            val channelCountText = "$count ${getString(R.string.available)}"
            channels_view.text = channelCountText
        })
        statusViewModel.programCount.observe(viewLifecycleOwner, Observer { count ->
            programs_view.text = resources.getQuantityString(R.plurals.programs, count ?: 0, count)
        })
        statusViewModel.seriesRecordingCount.observe(viewLifecycleOwner, Observer { count ->
            series_recordings_view.text = resources.getQuantityString(R.plurals.series_recordings, count
                    ?: 0, count)
        })
        statusViewModel.timerRecordingCount.observe(viewLifecycleOwner, Observer { count ->
            timer_recordings_view.text = resources.getQuantityString(R.plurals.timer_recordings, count
                    ?: 0, count)
        })
        statusViewModel.completedRecordingCount.observe(viewLifecycleOwner, Observer { count ->
            completed_recordings_view.text = resources.getQuantityString(R.plurals.completed_recordings, count
                    ?: 0, count)
        })
        statusViewModel.scheduledRecordingCount.observe(viewLifecycleOwner, Observer { count ->
            upcoming_recordings_view.text = resources.getQuantityString(R.plurals.upcoming_recordings, count
                    ?: 0, count)
        })
        statusViewModel.failedRecordingCount.observe(viewLifecycleOwner, Observer { count ->
            failed_recordings_view.text = resources.getQuantityString(R.plurals.failed_recordings, count
                    ?: 0, count)
        })
        statusViewModel.removedRecordingCount.observe(viewLifecycleOwner, Observer { count ->
            removed_recordings_view.text = resources.getQuantityString(R.plurals.removed_recordings, count
                    ?: 0, count)
        })
        statusViewModel.serverStatusLiveData.observe(viewLifecycleOwner, Observer { serverStatus ->
            if (serverStatus != null) {
                showServerInformation(serverStatus)
            }
        })

        // Get the programs that are currently being recorded
        val recordingViewModel = ViewModelProviders.of(this).get(RecordingViewModel::class.java)
        recordingViewModel.scheduledRecordings.observe(viewLifecycleOwner, Observer { recordings ->
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

    private fun showSubscriptionAndInputStatus() {
        statusViewModel.subscriptions.observe(viewLifecycleOwner, Observer { subscriptions ->
            if (subscriptions != null) {
                Timber.d("Received subscription status")
            }
        })
        statusViewModel.inputs.observe(viewLifecycleOwner, Observer { inputs ->
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
