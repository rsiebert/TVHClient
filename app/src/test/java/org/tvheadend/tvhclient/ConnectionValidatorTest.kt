package org.tvheadend.tvhclient

import android.os.Build
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.tvheadend.tvhclient.ui.features.settings.ConnectionValidator

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class ConnectionValidatorTest {

    private val validator = ConnectionValidator()

    @Test
    fun connectionNameIsValid() {
        assertEquals(ConnectionValidator.ValidationStatus.SUCCESS.toString(),
                validator.isConnectionNameValid("ServerName").toString())
    }

    @Test
    fun connectionNameIsEmpty() {
        assertEquals(ConnectionValidator.ValidationStatus.ERROR_EMPTY_NAME.toString(),
                validator.isConnectionNameValid("").toString())
    }

    @Test
    fun connectionNameIsInvalid() {
        assertEquals(ConnectionValidator.ValidationStatus.ERROR_INVALID_NAME.toString(),
                validator.isConnectionNameValid("Server/#:Name").toString())
    }

    @Test
    fun connectionUrlIsValid() {
        assertEquals(ConnectionValidator.ValidationStatus.SUCCESS.toString(),
                validator.isConnectionUrlValid("http://myserver:8080").toString())
    }

    @Test
    fun connectionUrlIsEmpty() {
        assertEquals(ConnectionValidator.ValidationStatus.ERROR_EMPTY_URL.toString(),
                validator.isConnectionUrlValid("").toString())
    }

    @Test
    fun connectionUrlSchemeMissing() {
        assertEquals(ConnectionValidator.ValidationStatus.ERROR_MISSING_URL_SCHEME.toString(),
                validator.isConnectionUrlValid("://myserver:8080").toString())
    }

    @Test
    fun connectionUrlSchemeInvalid() {
        assertEquals(ConnectionValidator.ValidationStatus.ERROR_WRONG_URL_SCHEME.toString(),
                validator.isConnectionUrlValid("htpp://myserver:8080").toString())
    }

    @Test
    fun connectionUrlHostInvalid() {
        assertEquals(ConnectionValidator.ValidationStatus.ERROR_MISSING_URL_HOST.toString(),
                validator.isConnectionUrlValid("http://:8080").toString())
    }

    @Test
    fun connectionUrlPortInvalid() {
        assertEquals(ConnectionValidator.ValidationStatus.ERROR_MISSING_URL_PORT.toString(),
                validator.isConnectionUrlValid("http://myserver").toString())
    }

    @Test
    fun connectionMacAddressIsValid() {
        assertEquals(ConnectionValidator.ValidationStatus.SUCCESS.toString(),
                validator.isConnectionWolMacAddressValid("10-62-E5-EC-A5-6A").toString())
    }

    @Test
    fun connectionMacAddressIsInvalid() {
        assertEquals(ConnectionValidator.ValidationStatus.ERROR_INVALID_MAC_ADDRESS.toString(),
                validator.isConnectionWolMacAddressValid("http://myserver:8080").toString())
    }
}