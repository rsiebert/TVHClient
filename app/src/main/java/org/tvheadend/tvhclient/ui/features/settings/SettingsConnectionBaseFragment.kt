package org.tvheadend.tvhclient.ui.features.settings

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.lifecycle.ViewModelProvider
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import com.afollestad.materialdialogs.MaterialDialog
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.ui.common.interfaces.BackPressedInterface
import org.tvheadend.tvhclient.ui.common.interfaces.ToolbarInterface
import org.tvheadend.tvhclient.util.extensions.sendSnackbarMessage

abstract class SettingsConnectionBaseFragment : PreferenceFragmentCompat(), BackPressedInterface, Preference.OnPreferenceChangeListener {

    lateinit var toolbarInterface: ToolbarInterface
    lateinit var settingsViewModel: SettingsViewModel
    val connectionValidator = ConnectionValidator()

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

        settingsViewModel = ViewModelProvider(activity as SettingsActivity).get(SettingsViewModel::class.java)
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
        val name = settingsViewModel.connectionToEdit.name
        namePreference.text = name
        namePreference.summary = if (name.isNullOrEmpty()) getString(R.string.pref_name_sum) else name

        val serverUrl = settingsViewModel.connectionToEdit.serverUrl
        serverUrlPreference.text = serverUrl
        serverUrlPreference.summary = if (serverUrl.isNullOrEmpty()) getString(R.string.pref_server_url_sum) else serverUrl

        val streamingUrl = settingsViewModel.connectionToEdit.streamingUrl
        streamingUrlPreference.text = streamingUrl
        streamingUrlPreference.summary = if (streamingUrl.isNullOrEmpty()) getString(R.string.pref_streaming_url_sum) else streamingUrl

        val username = settingsViewModel.connectionToEdit.username
        usernamePreference.text = username
        usernamePreference.summary = if (username.isNullOrEmpty() || username == "*") getString(R.string.pref_user_sum) else username

        val password = settingsViewModel.connectionToEdit.password
        passwordPreference.text = password
        passwordPreference.summary = if (password.isNullOrEmpty() || password == "*") getString(R.string.pref_pass_sum) else getString(R.string.pref_pass_set_sum)

        activeEnabledPreference.isChecked = settingsViewModel.connectionToEdit.isActive
        wolEnabledPreference.isChecked = settingsViewModel.connectionToEdit.isWolEnabled

        if (!settingsViewModel.connectionToEdit.isWolEnabled) {
            val wolPort = settingsViewModel.connectionToEdit.wolPort
            val wolMacAddress = settingsViewModel.connectionToEdit.wolMacAddress

            wolMacAddressPreference.text = wolMacAddress
            wolMacAddressPreference.summary = if (wolMacAddress.isNullOrEmpty()) getString(R.string.pref_wol_address_sum) else wolMacAddress
            wolPortPreference.text = wolPort.toString()
            wolPortPreference.summary = getString(R.string.pref_wol_port_sum, wolPort)
            wolUseBroadcastEnabled.isChecked = settingsViewModel.connectionToEdit.isWolUseBroadcast
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

    fun getErrorDescription(status: ConnectionValidator.ErrorReasons): String {
        return when (status) {
            ConnectionValidator.ErrorReasons.ERROR_EMPTY_NAME -> context?.resources?.getString(R.string.pref_name_error_invalid) ?: ""
            ConnectionValidator.ErrorReasons.ERROR_INVALID_NAME -> context?.resources?.getString(R.string.pref_name_error_invalid) ?: ""
            ConnectionValidator.ErrorReasons.ERROR_CONNECTION_URL_EMPTY -> "The connection url must not be empty"
            ConnectionValidator.ErrorReasons.ERROR_CONNECTION_URL_MISSING_SCHEME -> "The connection url must contain with http:// or https://"
            ConnectionValidator.ErrorReasons.ERROR_CONNECTION_URL_WRONG_SCHEME -> "The connection url must start with http:// or https://"
            ConnectionValidator.ErrorReasons.ERROR_CONNECTION_URL_MISSING_HOST -> "The connection url is missing a hostname"
            ConnectionValidator.ErrorReasons.ERROR_CONNECTION_URL_MISSING_PORT -> "The connection url is missing a port number"
            ConnectionValidator.ErrorReasons.ERROR_PLAYBACK_URL_EMPTY -> "The playback url must not be empty"
            ConnectionValidator.ErrorReasons.ERROR_PLAYBACK_URL_MISSING_SCHEME ->"The playback url must contain with http:// or https://"
            ConnectionValidator.ErrorReasons.ERROR_PLAYBACK_URL_WRONG_SCHEME -> "The playback url must start with http:// or https://"
            ConnectionValidator.ErrorReasons.ERROR_PLAYBACK_URL_MISSING_HOST -> "The playback url is missing a hostname"
            ConnectionValidator.ErrorReasons.ERROR_PLAYBACK_URL_MISSING_PORT -> "The playback url is missing a port number"
            ConnectionValidator.ErrorReasons.ERROR_INVALID_PORT_RANGE -> context?.resources?.getString(R.string.pref_port_error_invalid) ?: ""
            ConnectionValidator.ErrorReasons.ERROR_UNNEEDED_CREDENTIALS -> "The url must not contain a username or password"
            ConnectionValidator.ErrorReasons.ERROR_EMPTY_MAC_ADDRESS,
            ConnectionValidator.ErrorReasons.ERROR_INVALID_MAC_ADDRESS -> context?.resources?.getString(R.string.pref_wol_address_invalid) ?: ""
        }
    }

    override fun onPreferenceChange(preference: Preference, o: Any): Boolean {
        val value = o.toString()
        when (preference.key) {
            "name" -> preferenceNameChanged(value)
            "server_url" -> preferenceConnectionUrlChanged(value)
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
        when (val state = connectionValidator.isConnectionNameValid(value)) {
            is ConnectionValidator.ValidationState.Success -> {
                settingsViewModel.connectionToEdit.name = value
                namePreference.text = value
                namePreference.summary = if (value.isEmpty()) getString(R.string.pref_name_sum) else value
            }
            is ConnectionValidator.ValidationState.Error -> {
                namePreference.text = settingsViewModel.connectionToEdit.name
                context?.sendSnackbarMessage(getErrorDescription(state.reason))
            }
        }
    }

    private fun preferenceConnectionUrlChanged(value: String) {
        when (val state = connectionValidator.isConnectionUrlValid(value)){
            is ConnectionValidator.ValidationState.Success -> {
                settingsViewModel.connectionToEdit.serverUrl = value
                serverUrlPreference.text = value
                serverUrlPreference.summary = if (value.isEmpty()) getString(R.string.pref_server_url_sum) else value
            }
            is ConnectionValidator.ValidationState.Error -> {
                serverUrlPreference.text = settingsViewModel.connectionToEdit.serverUrl
                context?.sendSnackbarMessage(getErrorDescription(state.reason))
            }
        }
    }

    private fun preferenceStreamingUrlChanged(value: String) {
        when (val state = connectionValidator.isPlaybackUrlValid(value)){
            is ConnectionValidator.ValidationState.Success -> {
                settingsViewModel.connectionToEdit.streamingUrl = value
                streamingUrlPreference.text = value
                streamingUrlPreference.summary = if (value.isEmpty()) getString(R.string.pref_streaming_url_sum) else value
            }
            is ConnectionValidator.ValidationState.Error -> {
                streamingUrlPreference.text = settingsViewModel.connectionToEdit.streamingUrl
                context?.sendSnackbarMessage(getErrorDescription(state.reason))
            }
        }
    }

    private fun preferenceUsernameChanged(value: String) {
        settingsViewModel.connectionToEdit.username = value
        usernamePreference.text = value
        usernamePreference.summary = if (value.isEmpty()) getString(R.string.pref_user_sum) else value
    }

    private fun preferencePasswordChanged(value: String) {
        settingsViewModel.connectionToEdit.password = value
        passwordPreference.text = value
        passwordPreference.summary = if (value.isEmpty()) getString(R.string.pref_pass_sum) else getString(R.string.pref_pass_set_sum)
    }

    private fun preferenceEnabledChanged(value: String) {
        // When the connection was set as the new active
        // connection, an initial sync is required
        val isActive = java.lang.Boolean.valueOf(value)
        if (!settingsViewModel.connectionToEdit.isActive && isActive) {
            settingsViewModel.connectionToEdit.isSyncRequired = true
            settingsViewModel.connectionToEdit.lastUpdate = 0
        }
        settingsViewModel.connectionToEdit.isActive = java.lang.Boolean.valueOf(value)
    }

    private fun preferenceWolEnabledChanged(value: String) {
        settingsViewModel.connectionToEdit.isWolEnabled = java.lang.Boolean.valueOf(value)
    }

    private fun preferenceWolMacAddressChanged(value: String) {
        when (val state = connectionValidator.isConnectionWolMacAddressValid(value)) {
            is ConnectionValidator.ValidationState.Success -> {
                settingsViewModel.connectionToEdit.wolMacAddress = value
                wolMacAddressPreference.text = value
                wolMacAddressPreference.summary = if (value.isEmpty()) getString(R.string.pref_wol_address_sum) else value
            }
            is ConnectionValidator.ValidationState.Error -> {
                wolMacAddressPreference.text = settingsViewModel.connectionToEdit.wolMacAddress
                context?.sendSnackbarMessage(getErrorDescription(state.reason))
            }
        }
    }

    private fun preferenceWolPortChanged(value: String) {
        try {
            val port = Integer.parseInt(value)
            when (val state = connectionValidator.isConnectionWolPortValid(port)) {
                is ConnectionValidator.ValidationState.Success -> {
                    settingsViewModel.connectionToEdit.wolPort = port
                    wolPortPreference.text = value
                    wolPortPreference.summary = getString(R.string.pref_wol_port_sum, port)
                }
                is ConnectionValidator.ValidationState.Error -> {
                    wolPortPreference.text = settingsViewModel.connectionToEdit.wolPort.toString()
                    context?.sendSnackbarMessage(getErrorDescription(state.reason))
                }
            }
        } catch (e: NumberFormatException) {
            // NOP
        }
    }

    private fun preferenceWolBroadcastChanged(value: String) {
        settingsViewModel.connectionToEdit.isWolUseBroadcast = java.lang.Boolean.valueOf(value)
    }
}
