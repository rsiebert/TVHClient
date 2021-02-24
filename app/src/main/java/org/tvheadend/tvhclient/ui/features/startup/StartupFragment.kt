package org.tvheadend.tvhclient.ui.features.startup

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.core.view.forEach
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.afollestad.materialdialogs.MaterialDialog
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.databinding.StartupFragmentBinding
import org.tvheadend.tvhclient.ui.base.BaseViewModel
import org.tvheadend.tvhclient.ui.common.interfaces.HideNavigationDrawerInterface
import org.tvheadend.tvhclient.ui.common.interfaces.LayoutControlInterface
import org.tvheadend.tvhclient.ui.common.interfaces.ToolbarInterface
import org.tvheadend.tvhclient.ui.features.settings.SettingsActivity
import org.tvheadend.tvhclient.util.extensions.gone
import org.tvheadend.tvhclient.util.extensions.visible
import timber.log.Timber

class StartupFragment : Fragment(), HideNavigationDrawerInterface {

    private lateinit var binding: StartupFragmentBinding
    private lateinit var startupViewModel: StartupViewModel
    private lateinit var baseViewModel: BaseViewModel
    private var loadingDone = false
    private var connectionCount: Int = 0
    private var isConnectionActive = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = StartupFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        startupViewModel = ViewModelProvider(requireActivity()).get(StartupViewModel::class.java)
        baseViewModel = ViewModelProvider(requireActivity()).get(BaseViewModel::class.java)

        if (activity is LayoutControlInterface) {
            (activity as LayoutControlInterface).forceSingleScreenLayout()
        }

        if (activity is ToolbarInterface) {
            (activity as ToolbarInterface).setTitle(getString(R.string.status))
        }

        setHasOptionsMenu(true)
        loadingDone = false

        Timber.d("Observing connection status")
        startupViewModel.connectionStatus.observe(viewLifecycleOwner,  { status ->
            Timber.d("Received connection status from view model")

            if (status != null) {
                loadingDone = true
                connectionCount = status.first!!
                isConnectionActive = status.second!!
                Timber.d("connection count is $connectionCount and connection is active is $isConnectionActive")
                showStartupStatus()
            } else {
                Timber.d("Connection count and active connection are still loading")
                binding.startupStatus.visible()
                binding.startupStatus.text = getString(R.string.initializing)
                binding.addConnectionButton.gone()
                binding.listConnectionsButton.gone()
            }
        })
    }

    private fun showStartupStatus() {
        if (loadingDone) {
            if (!isConnectionActive && connectionCount == 0) {
                Timber.d("No connection available, showing settings button")
                binding.startupStatus.visible()
                binding.startupStatus.text = getString(R.string.no_connection_available)
                binding.addConnectionButton.visible()
                binding.addConnectionButton.setOnClickListener { showSettingsAddNewConnection() }
                binding.listConnectionsButton.gone()

            } else if (!isConnectionActive && connectionCount > 0) {
                Timber.d("No active connection available, showing settings button")
                binding.startupStatus.visible()
                binding.startupStatus.text = getString(R.string.no_connection_active_advice)
                binding.addConnectionButton.gone()
                binding.listConnectionsButton.visible()
                binding.listConnectionsButton.setOnClickListener { showConnectionListSettings() }

            } else {
                Timber.d("Connection is available and active, showing contents")
                binding.startupStatus.gone()
                binding.addConnectionButton.gone()
                binding.listConnectionsButton.gone()
                baseViewModel.setStartupComplete(true)
            }
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menu.forEach { it.isVisible = false }
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
                            baseViewModel.updateConnectionAndRestartApplication(context)
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
}
