package org.tvheadend.htsp

data class HtspConnectionData(
        var username: String? = "",
        var password: String? = "",
        var serverUrl: String? = "",
        var versionName: String = "",
        var versionCode: Int = 0,
        var connectionTimeout: Int = 0,
)

sealed class HtspAuthenticationStateResult {
    data class Idle(val message: String) : HtspAuthenticationStateResult()
    data class InProgress(val message: String) : HtspAuthenticationStateResult()
    data class Authenticated(val message: String) : HtspAuthenticationStateResult()
    // Bad credentials or any other failure
    data class Failed(val message: String) : HtspAuthenticationStateResult()
}

sealed class HtspConnectionStateResult {
    data class Idle(val message: String) : HtspConnectionStateResult()
    data class Closed(val message: String) : HtspConnectionStateResult()
    data class Connecting(val message: String) : HtspConnectionStateResult()
    data class Connected(val message: String) : HtspConnectionStateResult()
    // Failed, interrupted, unresolved address, error connecting to server or exception while opening socket
    data class Failed(val message: String) : HtspConnectionStateResult()
}