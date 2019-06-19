package org.tvheadend.tvhclient.ui.features.settings

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProviders
import androidx.preference.CheckBoxPreference
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.afollestad.materialdialogs.MaterialDialog
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.domain.entity.Connection
import org.tvheadend.tvhclient.ui.common.callbacks.BackPressedInterface
import org.tvheadend.tvhclient.ui.common.callbacks.ToolbarInterface
import org.tvheadend.tvhclient.util.extensions.sendSnackbarMessage

// TODO use view model to store the connection

abstract class SettingsConnectionBaseFragment : PreferenceFragmentCompat(), BackPressedInterface, Preference.OnPreferenceChangeListener {

    lateinit var toolbarInterface: ToolbarInterface
    lateinit var connection: Connection
    lateinit var settingsViewModel: SettingsViewModel

    private lateinit var namePreference: EditTextPreference
    private lateinit var hostnamePreference: EditTextPreference
    private lateinit var htspPortPreference: EditTextPreference
    private lateinit var streamingPortPreference: EditTextPreference
    private lateinit var usernamePreference: EditTextPreference
    private lateinit var passwordPreference: EditTextPreference
    lateinit var activeEnabledPreference: CheckBoxPreference
    private lateinit var wolMacAddressPreference: EditTextPreference
    private lateinit var wolPortPreference: EditTextPreference
    private lateinit var wolEnabledPreference: CheckBoxPreference
    private lateinit var wolUseBroadcastEnabled: CheckBoxPreference

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (activity is ToolbarInterface) {
            toolbarInterface = activity as ToolbarInterface
        }

        settingsViewModel = ViewModelProviders.of(activity as AppCompatActivity).get(SettingsViewModel::class.java)
        setHasOptionsMenu(true)

        if (savedInstanceState == null) {
            settingsViewModel.connectionHasChanged = false
        }

        // Get the connectivity preferences for later usage
        namePreference = findPreference("name")!!
        hostnamePreference = findPreference("hostname")!!
        htspPortPreference = findPreference("htsp_port")!!
        streamingPortPreference = findPreference("streaming_port")!!
        usernamePreference = findPreference("username")!!
        passwordPreference = findPreference("password")!!
        activeEnabledPreference = findPreference("active_enabled")!!
        wolEnabledPreference = findPreference("wol_enabled")!!
        wolMacAddressPreference = findPreference("wol_mac_address")!!
        wolPortPreference = findPreference("wol_port")!!
        wolUseBroadcastEnabled = findPreference("wol_broadcast_enabled")!!

        namePreference.onPreferenceChangeListener = this
        hostnamePreference.onPreferenceChangeListener = this
        htspPortPreference.onPreferenceChangeListener = this
        streamingPortPreference.onPreferenceChangeListener = this
        usernamePreference.onPreferenceChangeListener = this
        passwordPreference.onPreferenceChangeListener = this
        activeEnabledPreference.onPreferenceChangeListener = this
        wolEnabledPreference.onPreferenceChangeListener = this
        wolMacAddressPreference.onPreferenceChangeListener = this
        wolPortPreference.onPreferenceChangeListener = this
        wolUseBroadcastEnabled.onPreferenceChangeListener = this
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_add_connection, rootKey)
    }

    override fun onResume() {
        super.onResume()
        setPreferenceDefaultValues()
    }

    private fun setPreferenceDefaultValues() {
        val name = connection.name
        namePreference.text = name
        namePreference.summary = if (name.isNullOrEmpty()) getString(R.string.pref_name_sum) else name

        val address = connection.hostname
        hostnamePreference.text = address
        hostnamePreference.summary = if (address.isNullOrEmpty()) getString(R.string.pref_host_sum) else address

        val port = connection.port.toString()
        htspPortPreference.text = port
        htspPortPreference.summary = port

        streamingPortPreference.text = connection.streamingPort.toString()
        streamingPortPreference.summary = getString(R.string.pref_streaming_port_sum, connection.streamingPort)

        val username = connection.username
        usernamePreference.text = username
        usernamePreference.summary = if (username.isNullOrEmpty()) getString(R.string.pref_user_sum) else username

        val password = connection.password
        passwordPreference.text = password
        passwordPreference.summary = if (password.isNullOrEmpty()) getString(R.string.pref_pass_sum) else getString(R.string.pref_pass_set_sum)

        activeEnabledPreference.isChecked = connection.isActive
        wolEnabledPreference.isChecked = connection.isWolEnabled

        if (!connection.isWolEnabled) {
            val macAddress = connection.wolMacAddress
            wolMacAddressPreference.text = macAddress
            wolMacAddressPreference.summary = if (macAddress.isNullOrEmpty()) getString(R.string.pref_wol_address_sum) else macAddress
            wolPortPreference.text = connection.wolPort.toString()
            wolPortPreference.summary = getString(R.string.pref_wol_port_sum, connection.wolPort)
            wolUseBroadcastEnabled.isChecked = connection.isWolUseBroadcast
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.save_cancel_options_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_save -> {
                save()
                true
            }
            R.id.menu_cancel -> {
                cancel()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    protected abstract fun save()

    /**
     * Asks the user to confirm canceling the current activity. If no is
     * chosen the user can continue to add or edit the connection. Otherwise
     * the input will be discarded and the activity will be closed.
     */
    private fun cancel() {
        if (!settingsViewModel.connectionHasChanged) {
            activity?.finish()
        } else {
            // Show confirmation dialog to cancel
            activity?.let { activity ->
                MaterialDialog(activity).show {
                    message(R.string.confirm_discard_connection)
                    positiveButton(R.string.discard) { activity.finish() }
                    negativeButton(R.string.cancel) { dismiss() }
                }
            }
        }
    }

    override fun onBackPressed() {
        cancel()
    }

    override fun onPreferenceChange(preference: Preference, o: Any): Boolean {
        val value = o.toString()
        when (preference.key) {
            "name" -> {
                if (connection.isNameValid(value)) {
                    connection.name = value
                    namePreference.text = value
                    namePreference.summary = if (value.isEmpty()) getString(R.string.pref_name_sum) else value
                } else {
                    namePreference.text = connection.name
                    context?.sendSnackbarMessage(R.string.pref_name_error_invalid)
                }
            }
            "hostname" -> {
                settingsViewModel.connectionHasChanged = true
                if (connection.isIpAddressValid(value)) {
                    connection.hostname = value
                    hostnamePreference.text = value
                    hostnamePreference.summary = if (value.isEmpty()) getString(R.string.pref_host_sum) else value
                } else {
                    hostnamePreference.text = connection.hostname
                    context?.sendSnackbarMessage(R.string.pref_host_error_invalid)
                }
            }
            "htsp_port" -> {
                settingsViewModel.connectionHasChanged = true
                try {
                    val port = Integer.parseInt(value)
                    if (connection.isPortValid(port)) {
                        connection.port = port
                        htspPortPreference.text = port.toString()
                        htspPortPreference.summary = port.toString()
                    } else {
                        htspPortPreference.text = connection.port.toString()
                        context?.sendSnackbarMessage(R.string.pref_port_error_invalid)
                    }
                } catch (nex: NumberFormatException) {
                    // NOP
                }
            }
            "streaming_port" -> {
                settingsViewModel.connectionHasChanged = true
                try {
                    val port = Integer.parseInt(value)
                    if (connection.isPortValid(port)) {
                        connection.streamingPort = port
                        streamingPortPreference.text = value
                        streamingPortPreference.summary = getString(R.string.pref_streaming_port_sum, port)
                    } else {
                        streamingPortPreference.text = connection.streamingPort.toString()
                        context?.sendSnackbarMessage(R.string.pref_port_error_invalid)
                    }
                } catch (e: NumberFormatException) {
                    // NOP
                }
            }
            "username" -> {
                settingsViewModel.connectionHasChanged = true
                connection.username = value
                usernamePreference.text = value
                usernamePreference.summary = if (value.isEmpty()) getString(R.string.pref_user_sum) else value
            }
            "password" -> {
                settingsViewModel.connectionHasChanged = true
                connection.password = value
                passwordPreference.text = value
                passwordPreference.summary = if (value.isEmpty()) getString(R.string.pref_pass_sum) else getString(R.string.pref_pass_set_sum)
            }
            "active_enabled" -> {
                settingsViewModel.connectionHasChanged = true
                // When the connection was set as the new active
                // connection, an initial sync is required
                val isActive = java.lang.Boolean.valueOf(value)
                if (!connection.isActive && isActive) {
                    connection.isSyncRequired = true
                    connection.lastUpdate = 0
                }
                connection.isActive = java.lang.Boolean.valueOf(value)
            }
            "wol_enabled" -> {
                connection.isWolEnabled = java.lang.Boolean.valueOf(value)
            }
            "wol_mac_address" -> {
                if (connection.isWolMacAddressValid(value)) {
                    connection.wolMacAddress = value
                    wolMacAddressPreference.text = value
                    wolMacAddressPreference.summary = if (value.isEmpty()) getString(R.string.pref_wol_address_sum) else value
                } else {
                    wolMacAddressPreference.text = connection.wolMacAddress
                    context?.sendSnackbarMessage(R.string.pref_wol_address_invalid)
                }
            }
            "wol_port" -> {
                try {
                    val port = Integer.parseInt(value)
                    if (connection.isPortValid(port)) {
                        connection.wolPort = port
                        wolPortPreference.text = value
                        wolPortPreference.summary = getString(R.string.pref_wol_port_sum, port)
                    } else {
                        wolPortPreference.text = connection.wolPort.toString()
                        context?.sendSnackbarMessage(R.string.pref_port_error_invalid)
                    }
                } catch (e: NumberFormatException) {
                    // NOP
                }
            }
            "wol_broadcast_enabled" -> connection.isWolUseBroadcast = java.lang.Boolean.valueOf(value)
        }
        return true
    }
}
