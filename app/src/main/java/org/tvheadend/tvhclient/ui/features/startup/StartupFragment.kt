package org.tvheadend.tvhclient.ui.features.startup

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.*
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.Unbinder
import org.tvheadend.tvhclient.MainApplication
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.data.repository.AppRepository
import org.tvheadend.tvhclient.data.service.SyncStateReceiver
import org.tvheadend.tvhclient.ui.base.callbacks.ToolbarInterface
import org.tvheadend.tvhclient.ui.features.MainActivity
import org.tvheadend.tvhclient.ui.features.settings.SettingsActivity
import org.tvheadend.tvhclient.util.menu.MenuUtils
import timber.log.Timber
import javax.inject.Inject

// TODO add nice background image

class StartupFragment : Fragment() {

    @Inject
    lateinit var appRepository: AppRepository
    @Inject
    lateinit var sharedPreferences: SharedPreferences

    @BindView(R.id.progress_bar)
    lateinit var progressBar: ProgressBar
    @BindView(R.id.state)
    lateinit var stateTextView: TextView
    @BindView(R.id.details)
    lateinit var detailsTextView: TextView
    @BindView(R.id.add_connection_button)
    lateinit var addConnectionButton: ImageButton
    @BindView(R.id.settings_button)
    lateinit var settingsButton: ImageButton

    private lateinit var unbinder: Unbinder
    private lateinit var stateText: String
    private lateinit var detailsText: String
    private lateinit var state: SyncStateReceiver.State

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.startup_fragment, container, false)
        unbinder = ButterKnife.bind(this, view)
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        unbinder.unbind()
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
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putSerializable("state", state)
        outState.putString("stateText", stateTextView.text.toString())
        outState.putString("detailsText", detailsTextView.text.toString())
        super.onSaveInstanceState(outState)
    }

    private fun handleStartupProcedure() {
        when {
            appRepository.connectionData.getItems().isEmpty() -> {
                Timber.d("No connection available, showing settings button")
                stateText = getString(R.string.no_connection_available)
                progressBar.visibility = View.INVISIBLE
                addConnectionButton.visibility = View.VISIBLE
                addConnectionButton.setOnClickListener { showSettingsAddNewConnection() }
            }
            appRepository.connectionData.activeItemId == -1 -> {
                Timber.d("No active connection available, showing settings button")
                stateText = getString(R.string.no_connection_active_advice)
                progressBar.visibility = View.INVISIBLE
                settingsButton.visibility = View.VISIBLE
                settingsButton.setOnClickListener { showConnectionListSettings() }
            }
            else -> {
                Timber.d("Connection is available and active, showing contents")
                showContentScreen()
            }
        }

        stateTextView.text = stateText
        detailsTextView.text = detailsText
    }

    override fun onResume() {
        super.onResume()
        handleStartupProcedure()
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        // Do not show the reconnect menu in case no connections are available or none is active
        menu.findItem(R.id.menu_refresh).isVisible = (appRepository.connectionData.activeItemId >= 0)
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
