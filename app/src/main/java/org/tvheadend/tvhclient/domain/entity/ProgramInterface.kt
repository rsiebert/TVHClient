package org.tvheadend.tvhclient.domain.entity

interface ProgramInterface {
    var eventId: Int
    var title: String?
    var channelId: Int
    var start: Long
    var stop: Long
}