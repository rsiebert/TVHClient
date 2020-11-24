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

    fun getErrorDescription(status: ConnectionValidator.ValidationStatus): String {
        return when (status) {
            ConnectionValidator.ValidationStatus.ERROR_EMPTY_NAME -> context?.resources?.getString(R.string.pref_name_error_invalid) ?: ""
            ConnectionValidator.ValidationStatus.ERROR_INVALID_NAME -> context?.resources?.getString(R.string.pref_name_error_invalid) ?: ""
            ConnectionValidator.ValidationStatus.ERROR_EMPTY_URL -> "The url must not be empty"
            ConnectionValidator.ValidationStatus.ERROR_WRONG_URL_SCHEME -> "The url must start with http:// or https://"
            ConnectionValidator.ValidationStatus.ERROR_MISSING_URL_SCHEME -> "The url must contain http:// or https://"
            ConnectionValidator.ValidationStatus.ERROR_MISSING_URL_HOST -> "The url is missing a hostname"
            ConnectionValidator.ValidationStatus.ERROR_MISSING_URL_PORT -> "The url is missing a port number"
            ConnectionValidator.ValidationStatus.ERROR_INVALID_PORT_RANGE -> context?.resources?.getString(R.string.pref_port_error_invalid) ?: ""
            ConnectionValidator.ValidationStatus.ERROR_UNNEEDED_CREDENTIALS -> "The url must not contain a username or password"
            ConnectionValidator.ValidationStatus.ERROR_EMPTY_MAC_ADDRESS,
            ConnectionValidator.ValidationStatus.ERROR_INVALID_MAC_ADDRESS -> context?.resources?.getString(R.string.pref_wol_address_invalid) ?: ""
            ConnectionValidator.ValidationStatus.SUCCESS -> ""
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
        val status = connectionValidator.isConnectionNameValid(value)
        if (status == ConnectionValidator.ValidationStatus.SUCCESS) {
            settingsViewModel.connectionToEdit.name = value
            namePreference.text = value
            namePreference.summary = if (value.isEmpty()) getString(R.string.pref_name_sum) else value
        } else {
            namePreference.text = settingsViewModel.connectionToEdit.name
            context?.sendSnackbarMessage(getErrorDescription(status))
        }
    }

    private fun preferenceUrlChanged(value: String) {
        val status = connectionValidator.isConnectionUrlValid(value)
        if (status == ConnectionValidator.ValidationStatus.SUCCESS) {
            settingsViewModel.connectionToEdit.serverUrl = value
            serverUrlPreference.text = value
            serverUrlPreference.summary = if (value.isEmpty()) getString(R.string.pref_server_url_sum) else value
        } else {
            serverUrlPreference.text = settingsViewModel.connectionToEdit.serverUrl
            context?.sendSnackbarMessage(getErrorDescription(status))
        }
    }

    private fun preferenceStreamingUrlChanged(value: String) {
        val status = connectionValidator.isConnectionUrlValid(value)
        if (status == ConnectionValidator.ValidationStatus.SUCCESS) {
            settingsViewModel.connectionToEdit.streamingUrl = value
            streamingUrlPreference.text = value
            streamingUrlPreference.summary = if (value.isEmpty()) getString(R.string.pref_streaming_url_sum) else value
        } else {
            streamingUrlPreference.text = settingsViewModel.connectionToEdit.streamingUrl
            context?.sendSnackbarMessage(getErrorDescription(status))
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
        val status = connectionValidator.isConnectionWolMacAddressValid(value)
        if (status == ConnectionValidator.ValidationStatus.SUCCESS) {
            settingsViewModel.connectionToEdit.wolMacAddress = value
            wolMacAddressPreference.text = value
            wolMacAddressPreference.summary = if (value.isEmpty()) getString(R.string.pref_wol_address_sum) else value
        } else {
            wolMacAddressPreference.text = settingsViewModel.connectionToEdit.wolMacAddress
            context?.sendSnackbarMessage(getErrorDescription(status))
        }
    }

    private fun preferenceWolPortChanged(value: String) {
        try {
            val port = Integer.parseInt(value)
            val status = connectionValidator.isConnectionWolPortValid(port)
            if (status == ConnectionValidator.ValidationStatus.SUCCESS) {
                settingsViewModel.connectionToEdit.wolPort = port
                wolPortPreference.text = value
                wolPortPreference.summary = getString(R.string.pref_wol_port_sum, port)
            } else {
                wolPortPreference.text = settingsViewModel.connectionToEdit.wolPort.toString()
                context?.sendSnackbarMessage(getErrorDescription(status))
            }
        } catch (e: NumberFormatException) {
            // NOP
        }
    }

    private fun preferenceWolBroadcastChanged(value: String) {
        settingsViewModel.connectionToEdit.isWolUseBroadcast = java.lang.Boolean.valueOf(value)
    }
}
