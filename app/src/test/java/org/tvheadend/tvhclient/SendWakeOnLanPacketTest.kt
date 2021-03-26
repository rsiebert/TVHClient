package org.tvheadend.tvhclient

import org.junit.Assert
import org.junit.Test
import org.tvheadend.data.entity.Connection
import org.tvheadend.tvhclient.ui.common.SendWakeOnLanPacket
import org.tvheadend.tvhclient.ui.common.WakeOnLanTask

class SendWakeOnLanPacketTest {

    private val connection = Connection()
    private val sendWakeOnLanPacket = SendWakeOnLanPacket(connection)

    @Test
    fun sendingWolPacketError_InvalidMacAddress() {
        connection.wolMacAddress = "10-62-E5-EC-A5-6A+"
        connection.isWolUseBroadcast = false

        val status = sendWakeOnLanPacket.prepareAndSend()
        Assert.assertEquals(WakeOnLanTask.WOL_INVALID_MAC, status)
    }

    @Test
    fun sendingWolPacketError_Exception() {
        connection.wolMacAddress = "10-62-E5-EC-A5-6A"
        connection.serverUrl = "invalid server url"
        connection.isWolUseBroadcast = false

        val status = sendWakeOnLanPacket.prepareAndSend()
        Assert.assertEquals(WakeOnLanTask.WOL_ERROR, status)
    }

    @Test
    fun sendingWolPacketSuccess() {
        connection.wolMacAddress = "10-62-E5-EC-A5-6A"
        connection.serverUrl = "http://www.google.de"
        connection.isWolUseBroadcast = false

        val status = sendWakeOnLanPacket.prepareAndSend()
        Assert.assertEquals(WakeOnLanTask.WOL_SEND, status)
    }

    @Test
    fun sendingWolPacketAsBroadcastSuccess() {
        connection.wolMacAddress = "10-62-E5-EC-A5-6A"
        connection.serverUrl = "http://www.google.de"
        connection.isWolUseBroadcast = true

        val status = sendWakeOnLanPacket.prepareAndSend()
        Assert.assertEquals(WakeOnLanTask.WOL_SEND_BROADCAST, status)
    }
}