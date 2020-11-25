package org.tvheadend.tvhclient.ui.features.settings

import android.net.Uri
import org.tvheadend.data.entity.Connection
import timber.log.Timber
import java.util.regex.Pattern

class ConnectionValidator {

    sealed class ValidationState {
        data class Success(val msg: String = "") : ValidationState()
        data class Error(val reason: ErrorReasons) : ValidationState()
    }

    enum class ErrorReasons {
        ERROR_CONNECTION_URL_EMPTY,
        ERROR_CONNECTION_URL_MISSING_SCHEME,
        ERROR_CONNECTION_URL_WRONG_SCHEME,
        ERROR_CONNECTION_URL_MISSING_HOST,
        ERROR_CONNECTION_URL_MISSING_PORT,
        ERROR_PLAYBACK_URL_EMPTY,
        ERROR_PLAYBACK_URL_MISSING_SCHEME,
        ERROR_PLAYBACK_URL_WRONG_SCHEME,
        ERROR_PLAYBACK_URL_MISSING_HOST,
        ERROR_PLAYBACK_URL_MISSING_PORT,
        ERROR_INVALID_PORT_RANGE,
        ERROR_UNNEEDED_CREDENTIALS,
        ERROR_EMPTY_NAME,
        ERROR_INVALID_NAME,
        ERROR_EMPTY_MAC_ADDRESS,
        ERROR_INVALID_MAC_ADDRESS
    }

    fun isConnectionInputValid(connection: Connection): ValidationState {
        Timber.d("Validating input before saving")

        var state = isConnectionNameValid(connection.name)
        if (state != ValidationState.Success()) {
            return state
        }
        state = isConnectionUrlValid(connection.serverUrl)
        if (state != ValidationState.Success()) {
            return state
        }
        state = isPlaybackUrlValid(connection.streamingUrl)
        if (state != ValidationState.Success()) {
            return state
        }
        if (connection.isWolEnabled) {
            state = isConnectionWolPortValid(connection.wolPort)
            if (state != ValidationState.Success()) {
                return state
            }
            state = isConnectionWolMacAddressValid(connection.wolMacAddress)
            if (state != ValidationState.Success()) {
                return state
            }
        }

        return ValidationState.Success()
    }

    fun isConnectionNameValid(value: String?): ValidationState {
        if (value.isNullOrEmpty()) {
            return ValidationState.Error(ErrorReasons.ERROR_INVALID_NAME)
        }
        // Check if the name contains only valid characters.
        val pattern = Pattern.compile("^[0-9a-zA-Z_\\-.]*$")
        val matcher = pattern.matcher(value)
        return if (matcher.matches()) {
            ValidationState.Success()
        } else {
            ValidationState.Error(ErrorReasons.ERROR_INVALID_NAME)
        }
    }

    fun isConnectionUrlValid(value: String?): ValidationState {
        // Do not allow an empty serverUrl
        if (value.isNullOrEmpty()) {
            return ValidationState.Error(ErrorReasons.ERROR_CONNECTION_URL_EMPTY)
        }
        val uri = Uri.parse(value)
        if (uri.scheme.isNullOrEmpty()) {
            return ValidationState.Error(ErrorReasons.ERROR_CONNECTION_URL_MISSING_SCHEME)
        }
        if (uri.scheme != "http" && uri.scheme != "https") {
            return ValidationState.Error(ErrorReasons.ERROR_CONNECTION_URL_WRONG_SCHEME)
        }
        if (uri.host.isNullOrEmpty()) {
            return ValidationState.Error(ErrorReasons.ERROR_CONNECTION_URL_MISSING_HOST)
        }
        if (uri.port == -1) {
            return ValidationState.Error(ErrorReasons.ERROR_CONNECTION_URL_MISSING_PORT)
        }
        if (uri.port < 0 || uri.port > 65535) {
            return ValidationState.Error(ErrorReasons.ERROR_INVALID_PORT_RANGE)
        }
        if (!uri.userInfo.isNullOrEmpty()) {
            return ValidationState.Error(ErrorReasons.ERROR_UNNEEDED_CREDENTIALS)
        }
        return ValidationState.Success()
    }

    fun isPlaybackUrlValid(value: String?): ValidationState {
        // Do not allow an empty serverUrl
        if (value.isNullOrEmpty()) {
            return ValidationState.Error(ErrorReasons.ERROR_PLAYBACK_URL_EMPTY)
        }
        val uri = Uri.parse(value)
        if (uri.scheme.isNullOrEmpty()) {
            return ValidationState.Error(ErrorReasons.ERROR_PLAYBACK_URL_MISSING_SCHEME)
        }
        if (uri.scheme != "http" && uri.scheme != "https") {
            return ValidationState.Error(ErrorReasons.ERROR_PLAYBACK_URL_WRONG_SCHEME)
        }
        if (uri.host.isNullOrEmpty()) {
            return ValidationState.Error(ErrorReasons.ERROR_PLAYBACK_URL_MISSING_HOST)
        }
        if (uri.port == -1) {
            return ValidationState.Error(ErrorReasons.ERROR_PLAYBACK_URL_MISSING_PORT)
        }
        if (uri.port < 0 || uri.port > 65535) {
            return ValidationState.Error(ErrorReasons.ERROR_INVALID_PORT_RANGE)
        }
        if (!uri.userInfo.isNullOrEmpty()) {
            return ValidationState.Error(ErrorReasons.ERROR_UNNEEDED_CREDENTIALS)
        }
        return ValidationState.Success()
    }

    fun isConnectionWolMacAddressValid(value: String?): ValidationState {
        // Do not allow an empty address
        if (value.isNullOrEmpty()) {
            return ValidationState.Error(ErrorReasons.ERROR_INVALID_MAC_ADDRESS)
        }
        // Check if the MAC address is valid
        val pattern = Pattern.compile("([0-9a-fA-F]{2}(?::|-|$)){6}")
        val matcher = pattern.matcher(value)
        return if (matcher.matches()) {
            ValidationState.Success()
        } else {
            return ValidationState.Error(ErrorReasons.ERROR_INVALID_MAC_ADDRESS)
        }
    }

    fun isConnectionWolPortValid(port: Int): ValidationState {
        if (port < 0 || port > 65535) {
            return ValidationState.Error(ErrorReasons.ERROR_INVALID_PORT_RANGE)
        }
        return ValidationState.Success()
    }
}