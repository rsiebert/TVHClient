package org.tvheadend.data.entity

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Pure JVM mapping tests for [ConnectionEntity]. No Robolectric needed: [ConnectionEntity.from]
 * and [ConnectionEntity.toConnection] are plain in-memory conversions.
 */
class ConnectionEntityTest {

    @Test
    fun roundTripsEveryField() {
        val connection = Connection(
                id = 3,
                name = "Home",
                hostname = "192.168.1.10",
                port = 9982,
                username = "user",
                password = "secret",
                isActive = true,
                streamingPort = 9981,
                isWolEnabled = true,
                wolMacAddress = "AA:BB:CC:DD:EE:FF",
                wolPort = 9,
                isWolUseBroadcast = true,
                lastUpdate = 123L,
                isSyncRequired = false,
                serverUrl = "http://192.168.1.10:9981",
                streamingUrl = "http://192.168.1.10:9981/stream")

        val entity = ConnectionEntity.from(connection)

        assertThat(entity.toConnection()).isEqualTo(connection)
    }

    @Test
    fun roundTripsNullFields() {
        val connection = Connection(
                id = 1,
                name = null,
                hostname = null,
                username = null,
                password = null,
                wolMacAddress = null,
                serverUrl = null,
                streamingUrl = null)

        val restored = ConnectionEntity.from(connection).toConnection()

        assertThat(restored).isEqualTo(connection)
        assertThat(restored.name).isNull()
        assertThat(restored.serverUrl).isNull()
    }
}
