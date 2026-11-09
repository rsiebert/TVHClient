package org.tvheadend.tvhclient.util.logging

/**
 * Removes values that must not end up in the log file the user can share with support.
 *
 * The file log is written only in a release build and only when the user enables the debug mode
 * setting, but it can still leave the device. The server address reveals the host and port of a
 * private server, and a playback ticket grants access to a stream until it expires.
 *
 * The console tree of a debug build is left untouched so the values stay useful during development.
 */
object LogRedaction {

    private const val REDACTED = "<redacted>"

    /** Matches a complete http(s) address, stopping at the characters that usually follow it. */
    private val addressPattern = Regex("""(?i)\bhttps?://[^\s,;)"]+""")

    /** Matches a ticket in the `ticket is <value>`, `ticket=<value>` and `ticket: <value>` forms. */
    private val ticketPattern = Regex("""(?i)\bticket\s*(?:=|:|is)\s*[^\s,;)"]+""")

    fun apply(message: String): String {
        val withoutAddresses = addressPattern.replace(message) { match ->
            val value = match.value
            val schemeEnd = value.indexOf("://")
            if (schemeEnd < 0) {
                REDACTED
            } else {
                // Keep the scheme so the log still reads naturally, hide the host and port.
                value.substring(0, schemeEnd + 3) + REDACTED
            }
        }
        return ticketPattern.replace(withoutAddresses) { "ticket=$REDACTED" }
    }
}
