package org.tvheadend.tvhclient.ui.common

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LifecycleCoroutineScope
import org.tvheadend.data.entity.Connection
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.util.extensions.executeAsyncTask
import org.tvheadend.tvhclient.util.extensions.sendSnackbarMessage
import timber.log.Timber
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.regex.Pattern

class WakeOnLanTask(lifecycleScope: LifecycleCoroutineScope, context: Context, private val connection: Connection) {

    private var exception: Exception? = null

    init {
        lifecycleScope.executeAsyncTask(onPreExecute = {
            // ... runs in Main Thread
            Timber.d("onPreExecute")
        }, doInBackground = {
            Timber.d("doInBackground")
            var status: Int
            // ... runs in Worker(Background) Thread
            // Exit if the MAC address is not ok, this should never happen because
            // it is already validated in the settings
            if (!validateMacAddress(connection.wolMacAddress)) {
                status = WOL_INVALID_MAC
            } else {
                // Get the MAC address parts from the string
                val macBytes = getMacBytes(connection.wolMacAddress ?: "")

                // Assemble the byte array that the WOL consists of
                val bytes = ByteArray(6 + 16 * macBytes.size)
                for (i in 0..5) {
                    bytes[i] = 0xff.toByte()
                }
                // Copy the elements from macBytes to i
                var i = 6
                while (i < bytes.size) {
                    System.arraycopy(macBytes, 0, bytes, i, macBytes.size)
                    i += macBytes.size
                }

                try {
                    val uri = Uri.parse(connection.serverUrl)
                    val address: InetAddress
                    if (!connection.isWolUseBroadcast) {
                        address = InetAddress.getByName(uri.host)
                        Timber.d("Sending WOL packet to $address")
                    } else {
                        // Replace the last number by 255 to send the packet as a broadcast
                        val ipAddress = InetAddress.getByName(uri.host).address
                        ipAddress[3] = 255.toByte()
                        address = InetAddress.getByAddress(ipAddress)
                        Timber.d("Sending WOL packet as broadcast to $address")
                    }
                    val packet = DatagramPacket(bytes, bytes.size, address, connection.wolPort)
                    val socket = DatagramSocket()
                    socket.send(packet)
                    socket.close()

                    status = if (!connection.isWolUseBroadcast) {
                        WOL_SEND
                    } else {
                        WOL_SEND_BROADCAST
                    }
                } catch (e: Exception) {
                    this.exception = e
                    status = WOL_ERROR
                }
            }
            status // send data to "onPostExecute"
        }, onPostExecute = {
            // runs in Main Thread
            Timber.d("onPostExecute")
            // ... here "it" is the data returned from "doInBackground"
            val message: String
            when (it) {
                WOL_SEND -> {
                    Timber.d("Successfully sent WOL packet to ${connection.wolMacAddress}:${connection.port}")
                    message = context.getString(R.string.wol_send, "${connection.wolMacAddress}:${connection.port}")
                }
                WOL_SEND_BROADCAST -> {
                    Timber.d("Successfully sent WOL packet as a broadcast to ${connection.wolMacAddress}")
                    message = context.getString(R.string.wol_send_broadcast, connection.wolMacAddress)
                }
                WOL_INVALID_MAC -> {
                    Timber.d("Can't send WOL packet, the MAC-address is not valid")
                    message = context.getString(R.string.wol_address_invalid)
                }
                else -> {
                    Timber.d("Error sending WOL packet to ${connection.wolMacAddress}:${connection.port}")
                    message = context.getString(R.string.wol_error, "${connection.wolMacAddress}:${connection.port}", exception?.localizedMessage)
                }
            }
            context.sendSnackbarMessage(message)
        })
    }

    /**
     * Checks if the given MAC address is correct.
     *
     * @param macAddress The MAC address that shall be validated
     * @return True if the MAC address is correct, false otherwise
     */
    private fun validateMacAddress(macAddress: String?): Boolean {
        if (macAddress == null) {
            return false
        }
        // Check if the MAC address is valid
        val pattern = Pattern.compile("([0-9a-fA-F]{2}(?::|-|$)){6}")
        val matcher = pattern.matcher(macAddress)
        return matcher.matches()
    }

    /**
     * Splits the given MAC address into it's parts and saves it in the bytes
     * array
     *
     * @param macAddress The MAC address that shall be split
     * @return The byte array that holds the MAC address parts
     */
    private fun getMacBytes(macAddress: String): ByteArray {
        val macBytes = ByteArray(6)

        // Parse the MAC address elements into the array.
        val hex = macAddress.split("([:\\-])".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        for (i in 0..5) {
            macBytes[i] = Integer.parseInt(hex[i], 16).toByte()
        }
        return macBytes
    }

    companion object {

        private const val WOL_SEND = 0
        private const val WOL_SEND_BROADCAST = 1
        private const val WOL_INVALID_MAC = 2
        private const val WOL_ERROR = 3
    }
}