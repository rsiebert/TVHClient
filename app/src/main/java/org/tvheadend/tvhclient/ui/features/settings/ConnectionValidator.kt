package org.tvheadend.tvhclient.ui.features.settings

import org.tvheadend.data.entity.Connection
import timber.log.Timber
import java.net.URI
import java.net.URISyntaxException
import java.util.regex.Pattern

class ConnectionValidator {

    fun isConnectionInputValid(connection: Connection): ValidationResult {
        Timber.d("Validating input before saving")
        var status = isConnectionNameValid(connection.name)
        if (status != ValidationResult.Success()) {
            return status
        }
        status = isConnectionUrlValid(connection.serverUrl)
        if (status != ValidationResult.Success()) {
            return status
        }
        status = isConnectionUrlValid(connection.streamingUrl)
        if (status != ValidationResult.Success()) {
            return status
        }
        if (connection.isWolEnabled) {
            status = isConnectionWolPortValid(connection.wolPort)
            if (status != ValidationResult.Success()) {
                return status
            }
            status = isConnectionWolMacAddressValid(connection.wolMacAddress)
            if (status != ValidationResult.Success()) {
                return status
            }
        }

        return ValidationResult.Success()
    }

    fun isConnectionNameValid(value: String?): ValidationResult {
        if (value.isNullOrEmpty()) {
            return ValidationResult.Failed(ValidationFailureReason.NameEmpty())
        }
        // Check if the name contains only valid characters.
        val pattern = Pattern.compile("^[0-9a-zA-Z_\\-.]*$")
        val matcher = pattern.matcher(value)
        return if (matcher.matches()) {
            ValidationResult.Success()
        } else {
            ValidationResult.Failed(ValidationFailureReason.NameInvalid())
        }
    }

    fun isConnectionUrlValid(value: String?): ValidationResult {
        // Do not allow an empty serverUrl
        if (value.isNullOrEmpty()) {
            return ValidationResult.Failed(ValidationFailureReason.UrlEmpty())
        }
        lateinit var uri: URI
        try {
            uri = URI(value)
        } catch (e: URISyntaxException) {
            return ValidationResult.Failed(ValidationFailureReason.UrlSchemeMissing())
        }
        if (uri.scheme.isNullOrEmpty()) {
            return ValidationResult.Failed(ValidationFailureReason.UrlSchemeMissing())
        }
        if (uri.scheme != "http" && uri.scheme != "https") {
            return ValidationResult.Failed(ValidationFailureReason.UrlSchemeWrong())
        }
        if (uri.host.isNullOrEmpty()) {
            return ValidationResult.Failed(ValidationFailureReason.UrlHostMissing())
        }
        if (uri.port == -1) {
            return ValidationResult.Failed(ValidationFailureReason.UrlPortMissing())
        }
        if (uri.port < 0 || uri.port > 65535) {
            return ValidationResult.Failed(ValidationFailureReason.UrlPortInvalid())
        }
        if (!uri.userInfo.isNullOrEmpty()) {
            return ValidationResult.Failed(ValidationFailureReason.UrlUnneededCredentials())
        }
        return ValidationResult.Success()
    }

    fun isConnectionWolMacAddressValid(value: String?): ValidationResult {
        // Do not allow an empty address
        if (value.isNullOrEmpty()) {
            return ValidationResult.Failed(ValidationFailureReason.MacAddressEmpty())
        }
        // Check if the MAC address is valid
        val pattern = Pattern.compile("([0-9a-fA-F]{2}(?::|-|$)){6}")
        val matcher = pattern.matcher(value)
        return if (matcher.matches()) {
            ValidationResult.Success()
        } else {
            ValidationResult.Failed(ValidationFailureReason.MacAddressInvalid())
        }
    }

    fun isConnectionWolPortValid(port: Int): ValidationResult {
        if (port < 0 || port > 65535) {
            return ValidationResult.Failed(ValidationFailureReason.UrlPortInvalid())
        }
        return ValidationResult.Success()
    }
}

sealed class ValidationResult {
    data class Success(val message: String = "") : ValidationResult()
    data class Failed(val reason: ValidationFailureReason) : ValidationResult()
}

sealed class ValidationFailureReason {
    data class NameEmpty(val message: String = "") : ValidationFailureReason()
    data class NameInvalid(val message: String = "") : ValidationFailureReason()
    data class UrlEmpty(val message: String = "") : ValidationFailureReason()
    data class UrlSchemeMissing(val message: String = "") : ValidationFailureReason()
    data class UrlSchemeWrong(val message: String = "") : ValidationFailureReason()
    data class UrlHostMissing(val message: String = "") : ValidationFailureReason()
    data class UrlPortMissing(val message: String = "") : ValidationFailureReason()
    data class UrlPortInvalid(val message: String = "") : ValidationFailureReason()
    data class UrlUnneededCredentials(val message: String = "") : ValidationFailureReason()
    data class MacAddressEmpty(val message: String = "") : ValidationFailureReason()
    data class MacAddressInvalid(val message: String = "") : ValidationFailureReason()
}
