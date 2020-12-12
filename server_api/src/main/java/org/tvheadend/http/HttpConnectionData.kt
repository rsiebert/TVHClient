package org.tvheadend.http

data class HttpConnectionData(
        var username: String? = "",
        var password: String? = "",
        var url: String? = "",
        var versionName: String = "",
        var versionCode: Int = 0,
        var connectionTimeout: Int = 0,
)
