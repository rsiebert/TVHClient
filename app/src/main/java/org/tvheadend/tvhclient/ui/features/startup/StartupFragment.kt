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
import org.tvheadend.tvhclient.data.service.HtspService
import org.tvheadend.tvhclient.ui.common.callbacks.ToolbarInterface
import org.tvheadend.tvhclient.ui.common.visible
import org.tvheadend.tvhclient.ui.features.MainActivity
import org.tvheadend.tvhclient.ui.features.MainViewModel
import org.tvheadend.tvhclient.ui.features.settings.SettingsActivity
import timber.log.Timber

// TODO add nice background image

class StartupFragment : Fragment() {

    private lateinit var mainViewModel: MainViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.startup_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mainViewModel = ViewModelProviders.of(activity!!).get(MainViewModel::class.java)

        setHasOptionsMenu(true)

        if (activity is StartupActivity) {
            (activity as ToolbarInterface).setTitle(getString(R.string.status))
        }

        startup_status.text = savedInstanceState?.getString("stateText", "")
                ?: getString(R.string.initializing)
        startup_status.visible()

        mainViewModel.connectionCountLiveData.observe(viewLifecycleOwner, Observer { count ->
            if (count == 0) {
                Timber.d("No connection available, showing settings button")
                startup_status.text = getString(R.string.no_connection_available)
                add_connection_button.visible()
                add_connection_button.setOnClickListener { showSettingsAddNewConnection() }
            } else {
                if (mainViewModel.connection.id == -1) {
                    Timber.d("No active connection available, showing settings button")
                    startup_status.text = getString(R.string.no_connection_active_advice)
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
        outState.putString("stateText", startup_status.text.toString())
        super.onSaveInstanceState(outState)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        // Do not show the reconnect menu in case no connections are available or none is active
        menu.findItem(R.id.menu_refresh)?.isVisible = (mainViewModel.connection.id > 0)
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
                activity?.let { activity ->
                    MaterialDialog(activity).show {
                        title(R.string.dialog_title_reconnect_to_server)
                        message(R.string.dialog_content_reconnect_to_server)
                        positiveButton(R.string.reconnect) {
                            Timber.d("Reconnect requested, stopping service and updating active connection to require a full sync")
                            activity.stopService(Intent(activity, HtspService::class.java))

                            mainViewModel.setConnectionSyncRequired()
                            // Finally restart the application to show the startup fragment
                            val intent = Intent(activity, SplashActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            activity.startActivity(intent)
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
