package org.tvheadend.tvhclient

import org.junit.Assert.assertEquals
import org.junit.Test
import org.tvheadend.tvhclient.ui.features.settings.ConnectionValidator
import org.tvheadend.tvhclient.ui.features.settings.ValidationFailureReason
import org.tvheadend.tvhclient.ui.features.settings.ValidationResult

class ConnectionValidatorTest {

    private val validator = ConnectionValidator()

    @Test
    fun connectionNameIsValid() {
        assertEquals(ValidationResult.Success().toString(),
                validator.isConnectionNameValid("ServerName").toString())
    }

    @Test
    fun connectionNameIsEmpty() {
        assertEquals(ValidationResult.Failed(ValidationFailureReason.NameEmpty()).toString(),
                validator.isConnectionNameValid("").toString())
    }

    @Test
    fun connectionNameIsInvalid() {
        assertEquals(ValidationResult.Failed(ValidationFailureReason.NameInvalid()).toString(),
                validator.isConnectionNameValid("Server/#:Name").toString())
    }

    @Test
    fun connectionUrlIsValid() {
        assertEquals(ValidationResult.Success().toString(),
                validator.isConnectionUrlValid("http://myserver:8080").toString())
    }

    @Test
    fun connectionUrlIsEmpty() {
        assertEquals(ValidationResult.Failed(ValidationFailureReason.UrlEmpty()).toString(),
                validator.isConnectionUrlValid("").toString())
    }

    @Test
    fun connectionUrlSchemeMissing() {
        assertEquals(ValidationResult.Failed(ValidationFailureReason.UrlSchemeMissing()).toString(),
                validator.isConnectionUrlValid("://myserver:8080").toString())
    }

    @Test
    fun connectionUrlSchemeInvalid() {
        assertEquals(ValidationResult.Failed(ValidationFailureReason.UrlSchemeWrong()).toString(),
                validator.isConnectionUrlValid("htpp://myserver:8080").toString())
    }

    @Test
    fun connectionUrlHostInvalid() {
        assertEquals(ValidationResult.Failed(ValidationFailureReason.UrlHostMissing()).toString(),
                validator.isConnectionUrlValid("http://:8080").toString())
    }

    @Test
    fun connectionUrlPortInvalid() {
        assertEquals(ValidationResult.Failed(ValidationFailureReason.UrlPortMissing()).toString(),
                validator.isConnectionUrlValid("http://myserver").toString())
    }

    @Test
    fun connectionMacAddressIsValid() {
        assertEquals(ValidationResult.Success().toString(),
                validator.isConnectionWolMacAddressValid("10-62-E5-EC-A5-6A").toString())
    }

    @Test
    fun connectionMacAddressIsInvalid() {
        assertEquals(ValidationResult.Failed(ValidationFailureReason.MacAddressInvalid()).toString(),
                validator.isConnectionWolMacAddressValid("http://myserver:8080").toString())
    }
}