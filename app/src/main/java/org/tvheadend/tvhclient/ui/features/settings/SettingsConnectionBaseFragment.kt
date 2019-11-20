package org.tvheadend.tvhclient.ui.features.settings

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.lifecycle.ViewModelProviders
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import com.afollestad.materialdialogs.MaterialDialog
import com.google.android.material.snackbar.Snackbar
import org.tvheadend.tvhclient.R
import org.tvheadend.data.entity.Connection
import org.tvheadend.tvhclient.ui.common.interfaces.BackPressedInterface
import org.tvheadend.tvhclient.ui.common.interfaces.ToolbarInterface
import org.tvheadend.tvhclient.util.extensions.sendSnackbarMessage
import timber.log.Timber
import java.util.regex.Pattern

abstract class SettingsConnectionBaseFragment : PreferenceFragmentCompat(), BackPressedInterface, Preference.OnPreferenceChangeListener {

    lateinit var toolbarInterface: ToolbarInterface
    lateinit var settingsViewModel: SettingsViewModel

    private lateinit var namePreference: EditTextPreference
    private lateinit var serverUrlPreference: EditTextPreference
    private lateinit var streamingUrlPreference: EditTextPreference
    private lateinit var usernamePreference: EditTextPreference
    private lateinit var passwordPreference: EditTextPreference
    protected lateinit var activeEnabledPreference: SwitchPreference
    private lateinit var wolMacAddressPreference: EditTextPreference
    private lateinit var wolPortPreference: EditTextPreference
    private lateinit var wolEnabledPreference: SwitchPreference
    private lateinit var wolUseBroadcastEnabled: SwitchPreference

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (activity is ToolbarInterface) {
            toolbarInterface = activity as ToolbarInterface
        }

        settingsViewModel = ViewModelProviders.of(activity as SettingsActivity).get(SettingsViewModel::class.java)
        setHasOptionsMenu(true)

        // Get the connectivity preferences for later usage
        namePreference = findPreference("name")!!
        serverUrlPreference = findPreference("server_url")!!
        streamingUrlPreference = findPreference("streaming_url")!!
        usernamePreference = findPreference("username")!!
        passwordPreference = findPreference("password")!!
        activeEnabledPreference = findPreference("active_enabled")!!
        wolEnabledPreference = findPreference("wol_enabled")!!
        wolMacAddressPreference = findPreference("wol_mac_address")!!
        wolPortPreference = findPreference("wol_port")!!
        wolUseBroadcastEnabled = findPreference("wol_broadcast_enabled")!!

        namePreference.onPreferenceChangeListener = this
        serverUrlPreference.onPreferenceChangeListener = this
        streamingUrlPreference.onPreferenceChangeListener = this
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

        val serverUrl = settingsViewModel.connection.serverUrl
        serverUrlPreference.text = serverUrl
        serverUrlPreference.summary = if (serverUrl.isNullOrEmpty()) getString(R.string.pref_server_url_sum) else serverUrl

        val streamingUrl = settingsViewModel.connection.streamingUrl
        streamingUrlPreference.text = streamingUrl
        streamingUrlPreference.summary = if (streamingUrl.isNullOrEmpty()) getString(R.string.pref_streaming_url_sum) else streamingUrl

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
        } else if (!isConnectionUrlValid(context, connection.serverUrl)) {
            false
        } else if (!isConnectionUrlValid(context, connection.streamingUrl)) {
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
        activity?.let { activity ->
            MaterialDialog(activity).show {
                message(R.string.confirm_discard_connection)
                positiveButton(R.string.discard) {
                    if (activity is RemoveFragmentFromBackstackInterface) {
                        activity.removeFragmentFromBackstack()
                    }
                }
                negativeButton(R.string.cancel) {
                    dismiss()
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
            "server_url" -> preferenceUrlChanged(value)
            "streaming_url" -> preferenceStreamingUrlChanged(value)
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

    private fun preferenceUrlChanged(value: String) {
        if (isConnectionUrlValid(context, value)) {
            settingsViewModel.connection.serverUrl = value
            serverUrlPreference.text = value
            serverUrlPreference.summary = if (value.isEmpty()) getString(R.string.pref_server_url_sum) else value
        } else {
            serverUrlPreference.text = settingsViewModel.connection.serverUrl
        }
    }

    private fun preferenceStreamingUrlChanged(value: String) {
        if (isConnectionUrlValid(context, value)) {
            settingsViewModel.connection.streamingUrl = value
            streamingUrlPreference.text = value
            streamingUrlPreference.summary = if (value.isEmpty()) getString(R.string.pref_streaming_url_sum) else value
        } else {
            streamingUrlPreference.text = settingsViewModel.connection.streamingUrl
        }
    }

    private fun preferenceUsernameChanged(value: String) {
        settingsViewModel.connection.username = value
        usernamePreference.text = value
        usernamePreference.summary = if (value.isEmpty()) getString(R.string.pref_user_sum) else value
    }

    private fun preferencePasswordChanged(value: String) {
        settingsViewModel.connection.password = value
        passwordPreference.text = value
        passwordPreference.summary = if (value.isEmpty()) getString(R.string.pref_pass_sum) else getString(R.string.pref_pass_set_sum)
    }

    private fun preferenceEnabledChanged(value: String) {
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

    private fun isConnectionUrlValid(context: Context? = null, value: String?): Boolean {
        // Do not allow an empty serverUrl
        if (value.isNullOrEmpty()) {
            context?.sendSnackbarMessage("The url must not be empty")
            return false
        }

        val uri = Uri.parse(value)
        if (uri.host.isNullOrEmpty()) {
            context?.sendSnackbarMessage("The url is missing a hostname", Snackbar.LENGTH_LONG)
            return false
        }

        if (value.contains(":") && uri.port == -1) {
            context?.sendSnackbarMessage("The url must contain a port", Snackbar.LENGTH_LONG)
            return false
        }

        if (!uri.userInfo.isNullOrEmpty()) {
            context?.sendSnackbarMessage("The url must not contain a username or password", Snackbar.LENGTH_LONG)
            return false
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

    private fun isConnectionWolPortValid(port: Int): Boolean {
        return port in 1..65535
    }
}
