package org.tvheadend.tvhclient.ui.features.settings

import android.os.Bundle
import android.util.Patterns
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
import timber.log.Timber
import java.util.regex.Pattern

abstract class SettingsConnectionBaseFragment : PreferenceFragmentCompat(), BackPressedInterface, Preference.OnPreferenceChangeListener {

    lateinit var toolbarInterface: ToolbarInterface
    lateinit var settingsViewModel: SettingsViewModel

    private lateinit var namePreference: EditTextPreference
    private lateinit var hostnamePreference: EditTextPreference
    private lateinit var htspPortPreference: EditTextPreference
    private lateinit var streamingPortPreference: EditTextPreference
    private lateinit var usernamePreference: EditTextPreference
    private lateinit var passwordPreference: EditTextPreference
    private lateinit var activeEnabledPreference: CheckBoxPreference
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
            Timber.d("Saved instance is null, setting connection changed to false")
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
        val name = settingsViewModel.connection.name
        namePreference.text = name
        namePreference.summary = if (name.isNullOrEmpty()) getString(R.string.pref_name_sum) else name

        val address = settingsViewModel.connection.hostname
        hostnamePreference.text = address
        hostnamePreference.summary = if (address.isNullOrEmpty()) getString(R.string.pref_host_sum) else address

        val port = settingsViewModel.connection.port.toString()
        htspPortPreference.text = port
        htspPortPreference.summary = port

        val streamingPort = settingsViewModel.connection.streamingPort
        streamingPortPreference.text = streamingPort.toString()
        streamingPortPreference.summary = getString(R.string.pref_streaming_port_sum, streamingPort)

        val username = settingsViewModel.connection.username
        usernamePreference.text = username
        usernamePreference.summary = if (username.isNullOrEmpty()) getString(R.string.pref_user_sum) else username

        val password = settingsViewModel.connection.password
        passwordPreference.text = password
        passwordPreference.summary = if (password.isNullOrEmpty()) getString(R.string.pref_pass_sum) else getString(R.string.pref_pass_set_sum)

        activeEnabledPreference.isChecked = settingsViewModel.connection.isActive
        wolEnabledPreference.isChecked = settingsViewModel.connection.isWolEnabled

        if (!settingsViewModel.connection.isWolEnabled) {
            val wolPort = settingsViewModel.connection.wolPort
            val wolMacAddress = settingsViewModel.connection.wolMacAddress

            wolMacAddressPreference.text = wolMacAddress
            wolMacAddressPreference.summary = if (wolMacAddress.isNullOrEmpty()) getString(R.string.pref_wol_address_sum) else wolMacAddress
            wolPortPreference.text = wolPort.toString()
            wolPortPreference.summary = getString(R.string.pref_wol_port_sum, wolPort)
            wolUseBroadcastEnabled.isChecked = settingsViewModel.connection.isWolUseBroadcast
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

    fun isConnectionInputValid(connection: Connection): Boolean {
        Timber.d("Validating input before saving")
        return if (!isConnectionNameValid(connection.name)) {
            context?.sendSnackbarMessage(R.string.pref_name_error_invalid)
            false
        } else if (!isConnectionIpAddressValid(connection.hostname)) {
            context?.sendSnackbarMessage(R.string.pref_host_error_invalid)
            false
        } else if (!isConnectionPortValid(connection.port)) {
            context?.sendSnackbarMessage(R.string.pref_port_error_invalid)
            false
        } else if (!isConnectionPortValid(connection.streamingPort)) {
            context?.sendSnackbarMessage(R.string.pref_port_error_invalid)
            false
        } else if (connection.isWolEnabled && !isConnectionWolPortValid(connection.wolPort)) {
            context?.sendSnackbarMessage(R.string.pref_port_error_invalid)
            false
        } else if (connection.isWolEnabled && !isConnectionWolMacAddressValid(connection.wolMacAddress)) {
            context?.sendSnackbarMessage(R.string.pref_wol_address_invalid)
            false
        } else {
            true
        }
    }

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
            "name" -> preferenceNameChanged(value)
            "hostname" -> preferenceHostnameChanged(value)
            "htsp_port" -> preferenceHtspPortChanged(value)
            "streaming_port" -> preferenceStreamingPortChanged(value)
            "username" -> preferenceUsernameChanged(value)
            "password" -> preferencePasswordChanged(value)
            "active_enabled" -> preferenceEnabledChanged(value)
            "wol_enabled" -> preferenceWolEnabledChanged(value)
            "wol_mac_address" -> preferenceWolMacAddressChanged(value)
            "wol_port" -> preferenceWolPortChanged(value)
            "wol_broadcast_enabled" -> preferenceWolBroadcastChanged(value)
        }
        return true
    }

    private fun preferenceNameChanged(value: String) {
        if (isConnectionNameValid(value)) {
            settingsViewModel.connection.name = value
            namePreference.text = value
            namePreference.summary = if (value.isEmpty()) getString(R.string.pref_name_sum) else value
        } else {
            namePreference.text = settingsViewModel.connection.name
            context?.sendSnackbarMessage(R.string.pref_name_error_invalid)
        }
    }

    private fun preferenceHostnameChanged(value: String) {
        settingsViewModel.connectionHasChanged = true
        if (isConnectionIpAddressValid(value)) {
            settingsViewModel.connection.hostname = value
            hostnamePreference.text = value
            hostnamePreference.summary = if (value.isEmpty()) getString(R.string.pref_host_sum) else value
        } else {
            hostnamePreference.text = settingsViewModel.connection.hostname
            context?.sendSnackbarMessage(R.string.pref_host_error_invalid)
        }
    }

    private fun preferenceHtspPortChanged(value: String) {
        settingsViewModel.connectionHasChanged = true
        try {
            val port = Integer.parseInt(value)
            if (isConnectionPortValid(port)) {
                settingsViewModel.connection.port = port
                htspPortPreference.text = port.toString()
                htspPortPreference.summary = port.toString()
            } else {
                htspPortPreference.text = settingsViewModel.connection.port.toString()
                context?.sendSnackbarMessage(R.string.pref_port_error_invalid)
            }
        } catch (nex: NumberFormatException) {
            // NOP
        }
    }

    private fun preferenceStreamingPortChanged(value: String) {
        settingsViewModel.connectionHasChanged = true
        try {
            val port = Integer.parseInt(value)
            if (isConnectionPortValid(port)) {
                settingsViewModel.connection.streamingPort = port
                streamingPortPreference.text = value
                streamingPortPreference.summary = getString(R.string.pref_streaming_port_sum, port)
            } else {
                streamingPortPreference.text = settingsViewModel.connection.streamingPort.toString()
                context?.sendSnackbarMessage(R.string.pref_port_error_invalid)
            }
        } catch (e: NumberFormatException) {
            // NOP
        }
    }

    private fun preferenceUsernameChanged(value: String) {
        settingsViewModel.connectionHasChanged = true
        settingsViewModel.connection.username = value
        usernamePreference.text = value
        usernamePreference.summary = if (value.isEmpty()) getString(R.string.pref_user_sum) else value
    }

    private fun preferencePasswordChanged(value: String) {
        settingsViewModel.connectionHasChanged = true
        settingsViewModel.connection.password = value
        passwordPreference.text = value
        passwordPreference.summary = if (value.isEmpty()) getString(R.string.pref_pass_sum) else getString(R.string.pref_pass_set_sum)
    }

    private fun preferenceEnabledChanged(value: String) {
        settingsViewModel.connectionHasChanged = true
        // When the connection was set as the new active
        // connection, an initial sync is required
        val isActive = java.lang.Boolean.valueOf(value)
        if (!settingsViewModel.connection.isActive && isActive) {
            settingsViewModel.connection.isSyncRequired = true
            settingsViewModel.connection.lastUpdate = 0
        }
        settingsViewModel.connection.isActive = java.lang.Boolean.valueOf(value)
    }

    private fun preferenceWolEnabledChanged(value: String) {
        settingsViewModel.connection.isWolEnabled = java.lang.Boolean.valueOf(value)
    }

    private fun preferenceWolMacAddressChanged(value: String) {
        if (isConnectionWolMacAddressValid(value)) {
            settingsViewModel.connection.wolMacAddress = value
            wolMacAddressPreference.text = value
            wolMacAddressPreference.summary = if (value.isEmpty()) getString(R.string.pref_wol_address_sum) else value
        } else {
            wolMacAddressPreference.text = settingsViewModel.connection.wolMacAddress
            context?.sendSnackbarMessage(R.string.pref_wol_address_invalid)
        }
    }

    private fun preferenceWolPortChanged(value: String) {
        try {
            val port = Integer.parseInt(value)
            if (isConnectionWolPortValid(port)) {
                settingsViewModel.connection.wolPort = port
                wolPortPreference.text = value
                wolPortPreference.summary = getString(R.string.pref_wol_port_sum, port)
            } else {
                wolPortPreference.text = settingsViewModel.connection.wolPort.toString()
                context?.sendSnackbarMessage(R.string.pref_port_error_invalid)
            }
        } catch (e: NumberFormatException) {
            // NOP
        }
    }

    private fun preferenceWolBroadcastChanged(value: String) {
        settingsViewModel.connection.isWolUseBroadcast = java.lang.Boolean.valueOf(value)
    }

    private fun isConnectionNameValid(value: String?): Boolean {
        if (value.isNullOrEmpty()) {
            return false
        }
        // Check if the name contains only valid characters.
        val pattern = Pattern.compile("^[0-9a-zA-Z_\\-.]*$")
        val matcher = pattern.matcher(value)
        return matcher.matches()
    }

    private fun isConnectionIpAddressValid(value: String?): Boolean {
        // Do not allow an empty address
        if (value.isNullOrEmpty()) {
            return false
        }
        // Check if the name contains only valid characters.
        var pattern = Pattern.compile("^[0-9a-zA-Z_\\-.]*$")
        var matcher = pattern.matcher(value)
        if (!matcher.matches()) {
            return false
        }
        // Check if the address has only numbers and dots in it.
        pattern = Pattern.compile("^[0-9.]*$")
        matcher = pattern.matcher(value)

        // Now validate the IP address
        if (matcher.matches()) {
            pattern = Patterns.IP_ADDRESS
            matcher = pattern.matcher(value)
            return matcher.matches()
        }
        return true
    }

    private fun isConnectionWolMacAddressValid(value: String?): Boolean {
        // Do not allow an empty address
        if (value.isNullOrEmpty()) {
            return false
        }
        // Check if the MAC address is valid
        val pattern = Pattern.compile("([0-9a-fA-F]{2}(?::|-|$)){6}")
        val matcher = pattern.matcher(value)
        return matcher.matches()
    }

    private fun isConnectionPortValid(port: Int): Boolean {
        return port in 1..65535
    }

    private fun isConnectionWolPortValid(port: Int): Boolean {
        return port in 1..65535
    }
}
