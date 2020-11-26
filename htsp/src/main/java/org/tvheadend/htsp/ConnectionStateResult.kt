package org.tvheadend.htsp

sealed class ConnectionStateResult {
    data class Idle(val message: String = "") : ConnectionStateResult()
    data class Closed(val message: String = "") : ConnectionStateResult()
    data class Connecting(val message: String = "") : ConnectionStateResult()
    data class Connected(val message: String = "") : ConnectionStateResult()
    data class Failed(val reason: ConnectionFailureReason) : ConnectionStateResult()
}

sealed class ConnectionFailureReason {
    data class Interrupted(val message: String = "") : ConnectionFailureReason()
    data class UnresolvedAddress(val message: String = "") : ConnectionFailureReason()
    data class ConnectingToServer(val message: String = "") : ConnectionFailureReason()
    data class SocketException(val message: String = "") : ConnectionFailureReason()
    data class Other(val message: String = "") : ConnectionFailureReason()
}