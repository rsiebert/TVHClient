package org.tvheadend.htsp

data class HtspConnectionData(
        var username: String? = "",
        var password: String? = "",
        var serverUrl: String? = "",
        var versionName: String = "",
        var versionCode: Int = 0,
        var connectionTimeout: Int = 0,
)
