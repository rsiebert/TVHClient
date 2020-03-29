package org.tvheadend.tvhclient.ui.features.settings

import android.net.Uri
import org.tvheadend.data.entity.Connection
import timber.log.Timber
import java.util.regex.Pattern

class ConnectionValidator {

    enum class ValidationStatus {
        SUCCESS,
        ERROR_EMPTY_URL,
        ERROR_MISSING_URL_SCHEME,
        ERROR_WRONG_URL_SCHEME,
        ERROR_MISSING_URL_HOST,
        ERROR_MISSING_URL_PORT,
        ERROR_INVALID_PORT_RANGE,
        ERROR_UNNEEDED_CREDENTIALS,
        ERROR_EMPTY_NAME,
        ERROR_INVALID_NAME,
        ERROR_EMPTY_MAC_ADDRESS,
        ERROR_INVALID_MAC_ADDRESS
    }


    fun isConnectionInputValid(connection: Connection): ValidationStatus {
        Timber.d("Validating input before saving")
        var status = isConnectionNameValid(connection.name)
        if (status != ValidationStatus.SUCCESS) {
            return status
        }
        status = isConnectionUrlValid(connection.serverUrl)
        if (status != ValidationStatus.SUCCESS) {
            return status
        }
        status = isConnectionUrlValid(connection.streamingUrl)
        if (status != ValidationStatus.SUCCESS) {
            return status
        }
        if (connection.isWolEnabled) {
            status = isConnectionWolPortValid(connection.wolPort)
            if (status != ValidationStatus.SUCCESS) {
                return status
            }
            status = isConnectionWolMacAddressValid(connection.wolMacAddress)
            if (status != ValidationStatus.SUCCESS) {
                return status
            }
        }

        return ValidationStatus.SUCCESS
    }

    fun isConnectionNameValid(value: String?): ValidationStatus {
        if (value.isNullOrEmpty()) {
            return ValidationStatus.ERROR_EMPTY_NAME
        }
        // Check if the name contains only valid characters.
        val pattern = Pattern.compile("^[0-9a-zA-Z_\\-.]*$")
        val matcher = pattern.matcher(value)
        return if (matcher.matches()) {
            ValidationStatus.SUCCESS
        } else {
            ValidationStatus.ERROR_INVALID_NAME
        }
    }

    fun isConnectionUrlValid(value: String?): ValidationStatus {
        // Do not allow an empty serverUrl
        if (value.isNullOrEmpty()) {
            return ValidationStatus.ERROR_EMPTY_URL
        }
        val uri = Uri.parse(value)
        if (uri.scheme.isNullOrEmpty()) {
            return ValidationStatus.ERROR_MISSING_URL_SCHEME
        }
        if (uri.scheme != "http" && uri.scheme != "https") {
            return ValidationStatus.ERROR_WRONG_URL_SCHEME
        }
        if (uri.host.isNullOrEmpty()) {
            return ValidationStatus.ERROR_MISSING_URL_HOST
        }
        if (uri.port == -1) {
            return ValidationStatus.ERROR_MISSING_URL_PORT
        }
        if (uri.port < 0 || uri.port > 65535) {
            return ValidationStatus.ERROR_INVALID_PORT_RANGE
        }
        if (!uri.userInfo.isNullOrEmpty()) {
            return ValidationStatus.ERROR_UNNEEDED_CREDENTIALS
        }
        return ValidationStatus.SUCCESS
    }

    fun isConnectionWolMacAddressValid(value: String?): ValidationStatus {
        // Do not allow an empty address
        if (value.isNullOrEmpty()) {
            return ValidationStatus.ERROR_EMPTY_MAC_ADDRESS
        }
        // Check if the MAC address is valid
        val pattern = Pattern.compile("([0-9a-fA-F]{2}(?::|-|$)){6}")
        val matcher = pattern.matcher(value)
        return if (matcher.matches()) {
            ValidationStatus.SUCCESS
        } else {
            ValidationStatus.ERROR_INVALID_MAC_ADDRESS
        }
    }

    fun isConnectionWolPortValid(port: Int): ValidationStatus {
        if (port < 0 || port > 65535) {
            return ValidationStatus.ERROR_INVALID_PORT_RANGE
        }
        return ValidationStatus.SUCCESS
    }
}