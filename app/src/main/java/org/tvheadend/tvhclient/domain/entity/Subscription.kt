package org.tvheadend.tvhclient.domain.entity

data class Subscription(

        var id: Int = 0,
        var hostname: String = "",
        var username: String = "",
        var title: String = "",
        var client: String = "",
        var channel: String = "",
        var service: String = "",
        var profile: String = "",
        var state: String = "",
        var errors: Int = 0,
        var dataIn: Int = 0,
        var dataOut: Int = 0,
        var start: Int = 0,
        var connectionId: Int = 0
)