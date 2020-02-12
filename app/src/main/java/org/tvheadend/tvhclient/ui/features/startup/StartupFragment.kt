package org.tvheadend.tvhclient.ui.features.startup

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.afollestad.materialdialogs.MaterialDialog
import kotlinx.android.synthetic.main.startup_fragment.*
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.ui.common.interfaces.ToolbarInterface
import org.tvheadend.tvhclient.ui.features.MainActivity
import org.tvheadend.tvhclient.ui.features.settings.SettingsActivity
import org.tvheadend.tvhclient.util.extensions.gone
import org.tvheadend.tvhclient.util.extensions.visible
import timber.log.Timber

class StartupFragment : Fragment() {

    private lateinit var startupViewModel: StartupViewModel
    private var loadingDone = false
    private var connectionCount: Int = 0
    private var isConnectionActive = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.startup_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        startupViewModel = ViewModelProviders.of(activity!!).get(StartupViewModel::class.java)

        if (activity is ToolbarInterface) {
            (activity as ToolbarInterface).setTitle(getString(R.string.status))
        }

        setHasOptionsMenu(true)
        loadingDone = false

        Timber.d("Observing connection status")
        startupViewModel.connectionStatus.observe(viewLifecycleOwner, Observer { status ->
            Timber.d("Received connection status from view model")

            if (status != null) {
                loadingDone = true
                connectionCount = status.first!!
                isConnectionActive = status.second!!
                Timber.d("connection count is $connectionCount and connection is active is $isConnectionActive")
                showStartupStatus()
            } else {
                Timber.d("Connection count and active connection are still loading")
                startup_status.visible()
                startup_status.text = getString(R.string.initializing)
                add_connection_button.gone()
                list_connections_button.gone()
            }
        })
    }

    private fun showStartupStatus() {
        if (loadingDone) {
            if (!isConnectionActive && connectionCount == 0) {
                Timber.d("No connection available, showing settings button")
                startup_status.visible()
                startup_status.text = getString(R.string.no_connection_available)
                add_connection_button.visible()
                add_connection_button.setOnClickListener { showSettingsAddNewConnection() }
                list_connections_button.gone()

            } else if (!isConnectionActive && connectionCount > 0) {
                Timber.d("No active connection available, showing settings button")
                startup_status.visible()
                startup_status.text = getString(R.string.no_connection_active_advice)
                add_connection_button.gone()
                list_connections_button.visible()
                list_connections_button.setOnClickListener { showConnectionListSettings() }

            } else {
                Timber.d("Connection is available and active, showing contents")
                showContentScreen()
            }
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        // Do not show the reconnect menu in case no connections are available or none is active
        menu.findItem(R.id.menu_settings)?.isVisible = loadingDone
        menu.findItem(R.id.menu_reconnect_to_server)?.isVisible = loadingDone && isConnectionActive
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.startup_options_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_settings -> {
                showMainSettings()
                true
            }
            R.id.menu_reconnect_to_server -> {
                activity?.let { activity ->
                    MaterialDialog(activity).show {
                        title(R.string.reconnect_to_server)
                        message(R.string.restart_and_sync)
                        positiveButton(R.string.reconnect) {
                            Timber.d("Reconnect requested, stopping service and updating active connection to require a full sync")
                            startupViewModel.updateConnectionAndRestartApplication(context)
                        }
                        negativeButton(R.string.cancel)
                    }
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showSettingsAddNewConnection() {
        val intent = Intent(context, SettingsActivity::class.java)
        intent.putExtra("setting_type", "add_connection")
        startActivity(intent)
    }

    private fun showConnectionListSettings() {
        val intent = Intent(context, SettingsActivity::class.java)
        intent.putExtra("setting_type", "list_connections")
        startActivity(intent)
    }

    private fun showMainSettings() {
        val intent = Intent(context, SettingsActivity::class.java)
        startActivity(intent)
    }

    /**
     * Shows the main fragment like the channel list when the startup is complete.
     * Which fragment shall be shown is determined by a preference.
     * The connection to the server will be established by the network status
     * which is monitored in the base activity which the main activity inherits.
     */
    private fun showContentScreen() {
        val intent = Intent(context, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        activity?.startActivity(intent)
        activity?.finish()
    }
}
