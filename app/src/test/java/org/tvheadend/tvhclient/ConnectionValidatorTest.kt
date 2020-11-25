package org.tvheadend.tvhclient

import android.os.Build
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.tvheadend.tvhclient.ui.features.settings.ConnectionValidator

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class ConnectionValidatorTest {

    private val context = RuntimeEnvironment.systemContext;
    private val validator = ConnectionValidator()

    @Test
    fun connectionNameIsValid() {
        assertEquals(ConnectionValidator.ValidationState.Success().toString(),
                validator.isConnectionNameValid("ServerName").toString())
    }

    @Test
    fun connectionNameIsEmpty() {
        assertNotEquals(ConnectionValidator.ValidationState.Success().toString(),
                validator.isConnectionNameValid("").toString())
    }

    @Test
    fun connectionNameIsInvalid() {
        assertNotEquals(ConnectionValidator.ValidationState.Success().toString(),
                validator.isConnectionNameValid("Server/#:Name").toString())
    }

    @Test
    fun connectionUrlIsValid() {
        assertEquals(ConnectionValidator.ValidationState.Success().toString(),
                validator.isConnectionUrlValid("http://myserver:8080").toString())
    }

    @Test
    fun connectionUrlIsEmpty() {
        assertNotEquals(ConnectionValidator.ValidationState.Success().toString(),
                validator.isConnectionUrlValid("").toString())
    }

    @Test
    fun connectionUrlSchemeMissing() {
        assertNotEquals(ConnectionValidator.ValidationState.Success().toString(),
                validator.isConnectionUrlValid("://myserver:8080").toString())
    }

    @Test
    fun connectionUrlSchemeInvalid() {
        assertNotEquals(ConnectionValidator.ValidationState.Success().toString(),
                validator.isConnectionUrlValid("htpp://myserver:8080").toString())
    }

    @Test
    fun connectionUrlHostInvalid() {
        assertNotEquals(ConnectionValidator.ValidationState.Success().toString(),
                validator.isConnectionUrlValid("http://:8080").toString())
    }

    @Test
    fun connectionUrlPortInvalid() {
        assertNotEquals(ConnectionValidator.ValidationState.Success().toString(),
                validator.isConnectionUrlValid("http://myserver").toString())
    }

    @Test
    fun connectionMacAddressIsValid() {
        assertEquals(ConnectionValidator.ValidationState.Success().toString(),
                validator.isConnectionWolMacAddressValid("10-62-E5-EC-A5-6A").toString())
    }

    @Test
    fun connectionMacAddressIsInvalid() {
        assertNotEquals(ConnectionValidator.ValidationState.Success().toString(),
                validator.isConnectionWolMacAddressValid("http://myserver:8080").toString())
    }
}