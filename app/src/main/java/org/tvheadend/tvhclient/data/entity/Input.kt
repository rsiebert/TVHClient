package org.tvheadend.tvhclient.data.entity

data class Input(

        var uuid: String = "",
        var input: String = "",
        var username: String = "",
        var stream: String = "",
        var numberOfSubscriptions: Int = 0,
        var weight: Int = 0,
        var signalStrength: Int = 0,    // Shown as percentage from a range between 0 to 65535)
        var bitErrorRate: Int = 0,
        var uncorrectedBlocks: Int = 0,
        var signalNoiseRatio: Int = 0,  // Shown as percentage from a range between 0 to 65535)
        var bandWidth: Int = 0,         // bits/second
        var continuityErrors: Int = 0,
        var transportErrors: Int = 0,
        var connectionId: Int = 0
)
