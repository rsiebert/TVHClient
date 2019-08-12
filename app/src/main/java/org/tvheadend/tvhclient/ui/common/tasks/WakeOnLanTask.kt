package org.tvheadend.tvhclient.ui.common.tasks

import android.content.Context
import android.net.Uri
import android.os.AsyncTask
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.domain.entity.Connection
import org.tvheadend.tvhclient.util.extensions.sendSnackbarMessage
import timber.log.Timber
import java.lang.ref.WeakReference
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.regex.Pattern

class WakeOnLanTask(context: Context, private val connection: Connection) : AsyncTask<String, Void, Int>() {

    private val context: WeakReference<Context> = WeakReference(context)
    private var exception: Exception? = null

    override fun doInBackground(vararg params: String): Int? {
        // Exit if the MAC address is not ok, this should never happen because
        // it is already validated in the settings
        if (!validateMacAddress(connection.wolMacAddress)) {
            return WOL_INVALID_MAC
        }
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

            return if (!connection.isWolUseBroadcast) {
                WOL_SEND
            } else {
                WOL_SEND_BROADCAST
            }
        } catch (e: Exception) {
            this.exception = e
            return WOL_ERROR
        }

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

    /**
     * Depending on the wake on LAN status the toast with the success or error
     * message is shown to the user
     */
    override fun onPostExecute(result: Int?) {
        val ctx = context.get()
        if (ctx != null) {
            val message: String
            when (result) {
                WOL_SEND -> {
                    Timber.d("Successfully sent WOL packet to ${connection.serverUrl}")
                    message = ctx.getString(R.string.wol_send, connection.serverUrl)
                }
                WOL_SEND_BROADCAST -> {
                    Timber.d("Successfully sent WOL packet as a broadcast to ${connection.serverUrl}")
                    message = ctx.getString(R.string.wol_send_broadcast, connection.serverUrl)
                }
                WOL_INVALID_MAC -> {
                    Timber.d("Can't send WOL packet, the MAC-address is not valid")
                    message = ctx.getString(R.string.wol_address_invalid)
                }
                else -> {
                    Timber.d("Error sending WOL packet to ${connection.serverUrl}")
                    message = ctx.getString(R.string.wol_error, connection.serverUrl, exception?.localizedMessage)
                }
            }
            ctx.sendSnackbarMessage(message)
        }
    }

    companion object {

        private const val WOL_SEND = 0
        private const val WOL_SEND_BROADCAST = 1
        private const val WOL_INVALID_MAC = 2
        private const val WOL_ERROR = 3
    }
}