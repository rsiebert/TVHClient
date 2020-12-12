package org.tvheadend.api

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed class ConnectionStateResult : Parcelable {
    @Parcelize
    data class Idle(val message: String = "") : ConnectionStateResult(), Parcelable
    @Parcelize
    data class Closed(val message: String = "") : ConnectionStateResult(), Parcelable
    @Parcelize
    data class Connecting(val message: String = "") : ConnectionStateResult(), Parcelable
    @Parcelize
    data class Connected(val message: String = "") : ConnectionStateResult(), Parcelable
    @Parcelize
    data class Failed(val reason: ConnectionFailureReason) : ConnectionStateResult(), Parcelable
}

sealed class ConnectionFailureReason : Parcelable {
    @Parcelize
    data class Interrupted(val message: String = "") : ConnectionFailureReason(), Parcelable
    @Parcelize
    data class UnresolvedAddress(val message: String = "") : ConnectionFailureReason(), Parcelable
    @Parcelize
    data class ConnectingToServer(val message: String = "") : ConnectionFailureReason(), Parcelable
    @Parcelize
    data class SocketException(val message: String = "") : ConnectionFailureReason(), Parcelable
    @Parcelize
    data class Other(val message: String = "") : ConnectionFailureReason(), Parcelable
}