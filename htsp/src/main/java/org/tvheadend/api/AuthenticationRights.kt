package org.tvheadend.api

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * The rights that the server granted to the account that is used for the connection.
 *
 * The server only sends them when the negotiated HTSP version is 26 or newer. Servers that
 * do not send them are treated as if they granted everything, because that is how they
 * behaved before the rights were introduced.
 */
@Parcelize
data class AuthenticationRights(
        val isAdmin: Boolean = false,
        val isStreaming: Boolean = true,
        val isDvr: Boolean = true,
        val isFailedDvr: Boolean = false,
        val isAnonymous: Boolean = false,
        val limitAll: Int = 0,
        val limitDvr: Int = 0,
        val limitStreaming: Int = 0,
        val uiLevel: Int = 0,
        val uiLanguage: String = ""
) : Parcelable
