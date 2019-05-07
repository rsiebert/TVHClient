package org.tvheadend.tvhclient.ui.features.startup

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import kotlinx.android.synthetic.main.startup_fragment.*
import org.tvheadend.tvhclient.MainApplication
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.data.service.SyncStateReceiver
import org.tvheadend.tvhclient.ui.base.BaseActivity
import org.tvheadend.tvhclient.ui.common.MenuUtils
import org.tvheadend.tvhclient.ui.common.callbacks.ToolbarInterface
import org.tvheadend.tvhclient.ui.common.invisible
import org.tvheadend.tvhclient.ui.common.visible
import org.tvheadend.tvhclient.ui.features.MainActivity
import org.tvheadend.tvhclient.ui.features.MainViewModel
import org.tvheadend.tvhclient.ui.features.settings.SettingsActivity
import timber.log.Timber

// TODO add nice background image

class StartupFragment : Fragment() {

    protected lateinit var mainViewModel: MainViewModel
    private lateinit var stateText: String
    private lateinit var detailsText: String
    private lateinit var state: SyncStateReceiver.State

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.startup_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        MainApplication.getComponent().inject(this)
        setHasOptionsMenu(true)

        if (activity is StartupActivity) {
            (activity as ToolbarInterface).setTitle(getString(R.string.status))
        }

        if (savedInstanceState != null) {
            state = savedInstanceState.getSerializable("state") as SyncStateReceiver.State
            stateText = savedInstanceState.getString("stateText", "")
            detailsText = savedInstanceState.getString("detailsText", "")
        } else {
            state = SyncStateReceiver.State.IDLE
            stateText = getString(R.string.initializing)
            detailsText = ""
        }

        mainViewModel = ViewModelProviders.of(activity as BaseActivity).get(MainViewModel::class.java)
        mainViewModel.connectionCount.observe(viewLifecycleOwner, Observer { count ->
            if (count == 0) {
                Timber.d("No connection available, showing settings button")
                stateText = getString(R.string.no_connection_available)
                progress_bar.invisible()
                add_connection_button.visible()
                add_connection_button.setOnClickListener { showSettingsAddNewConnection() }
            } else {
                if (mainViewModel.activeConnection.id == -1) {
                    Timber.d("No active connection available, showing settings button")
                    stateText = getString(R.string.no_connection_active_advice)
                    progress_bar.invisible()
                    settings_button.visible()
                    settings_button.setOnClickListener { showConnectionListSettings() }
                } else {
                    Timber.d("Connection is available and active, showing contents")
                    showContentScreen()
                }
            }
        })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putSerializable("state", state)
        outState.putString("stateText", state_view.text.toString())
        outState.putString("detailsText", details_view.text.toString())
        super.onSaveInstanceState(outState)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        // Do not show the reconnect menu in case no connections are available or none is active
        menu.findItem(R.id.menu_refresh)?.isVisible = (mainViewModel.activeConnection.id > 0)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.startup_options_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_settings -> {
                showConnectionListSettings()
                true
            }
            R.id.menu_refresh -> {
                val currentActivity = activity
                if (currentActivity != null) {
                    MenuUtils(currentActivity).handleMenuReconnectSelection()
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
