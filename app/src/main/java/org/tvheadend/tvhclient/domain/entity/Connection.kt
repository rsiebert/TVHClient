package org.tvheadend.tvhclient.domain.entity

import android.util.Patterns
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.regex.Pattern

@Entity(tableName = "connections", indices = [Index(value = ["id"], unique = true)])
data class Connection(

        @PrimaryKey(autoGenerate = true)
        var id: Int = 0,
        var name: String? = "",
        var hostname: String? = "",
        var port: Int = 9982,
        var username: String? = "",
        var password: String? = "",
        @ColumnInfo(name = "active")
        var isActive: Boolean = false,
        @ColumnInfo(name = "streaming_port")
        var streamingPort: Int = 9981,
        @ColumnInfo(name = "wol_enabled")
        var isWolEnabled: Boolean = false,
        @ColumnInfo(name = "wol_hostname")
        var wolMacAddress: String? = "",
        @ColumnInfo(name = "wol_port")
        var wolPort: Int = 9,
        @ColumnInfo(name = "wol_use_broadcast")
        var isWolUseBroadcast: Boolean = false,
        @ColumnInfo(name = "last_update")
        var lastUpdate: Long = 0,
        @ColumnInfo(name = "sync_required")
        var isSyncRequired: Boolean = true
) {

    fun isWolMacAddressValid(macAddress: String): Boolean {
        // Check if the MAC address is valid
        val pattern = Pattern.compile("([0-9a-fA-F]{2}(?::|-|$)){6}")
        val matcher = pattern.matcher(macAddress)
        return matcher.matches()
    }

    fun isNameValid(name: String?): Boolean {
        if (name.isNullOrEmpty()) {
            return false
        }
        // Check if the name contains only valid characters.
        val pattern = Pattern.compile("^[0-9a-zA-Z_\\-.]*$")
        val matcher = pattern.matcher(name)
        return matcher.matches()
    }

    fun isIpAddressValid(address: String?): Boolean {
        // Do not allow an empty address
        if (address.isNullOrEmpty()) {
            return false
        }

        // Check if the name contains only valid characters.
        var pattern = Pattern.compile("^[0-9a-zA-Z_\\-.]*$")
        var matcher = pattern.matcher(address)
        if (!matcher.matches()) {
            return false
        }

        // Check if the address has only numbers and dots in it.
        pattern = Pattern.compile("^[0-9.]*$")
        matcher = pattern.matcher(address)

        // Now validate the IP address
        if (matcher.matches()) {
            pattern = Patterns.IP_ADDRESS
            matcher = pattern.matcher(address)
            return matcher.matches()
        }
        return true
    }

    fun isPortValid(port: Int): Boolean {
        return port in 1..65535
    }
}
