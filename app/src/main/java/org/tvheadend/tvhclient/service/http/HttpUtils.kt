package org.tvheadend.tvhclient.service.http

import org.json.JSONObject
import org.tvheadend.data.entity.Channel
import org.tvheadend.data.entity.ChannelTag
import timber.log.Timber

fun convertUuidToInt(uuid: String): Int {
    Timber.d("Converting uuid $uuid to integer")
    val id = java.nio.ByteBuffer.wrap(uuid.encodeToByteArray()).asIntBuffer().get()
    Timber.d("Converted uuid $uuid to $id")
    return id
}

fun convertMessageToChannelModel(channel: Channel, msg: JSONObject): Channel {

    if (msg.has("uuid")) {
        channel.id = convertUuidToInt(msg.getString("uuid"))
    }
    if (msg.has("number")) {
        val channelNumber = msg.getInt("number")
        channel.number = channelNumber
        channel.displayNumber = "$channelNumber.0"
    }
    if (msg.has("name")) {
        channel.name = msg.getString("name")
    }
    if (msg.has("tags")) {
        val array = msg.getJSONArray("tags")
        val tags = mutableListOf<Int>()
        for (i in 0 until array.length()) {
            val tagId = convertUuidToInt(array[i] as String)
            Timber.d("Found tag uuid $tagId")
            tags.add(tagId)
        }
        if (tags.size > 0) {
            Timber.d("Adding ${tags.size} tags to the channel")
            channel.tags = tags
        }
    }
    return channel
}

fun convertMessageToChannelTagModel(tag: ChannelTag, msg: JSONObject, channels: List<Channel>): ChannelTag {
    if (msg.has("uuid")) {
        tag.tagId = convertUuidToInt(msg.getString("uuid"))
    }
    if (msg.has("name")) {
        tag.tagName = msg.getString("name")
    }
    if (msg.has("index")) {
        if (msg.getInt("index") > 0) {
            tag.tagIndex = msg.getInt("index")
        }
    }
    if (msg.has("icon")) {
        if (!msg.getString("icon").isNullOrEmpty()) {
            tag.tagIcon = msg.getString("icon")
        }
    }
    if (msg.has("titled_icon")) {
        if (msg.getInt("titled_icon") > 0) {
            tag.tagTitledIcon = msg.getInt("titled_icon")
        }
    }

    var channelCount = 0
    for (channel in channels) {
        Timber.d("Checking if tag ${tag.tagName} is used in channel ${channel.name}")
        if (channel.tags?.contains(tag.tagId) == true) {
            Timber.d("Tag ${tag.tagName} is used in channel ${channel.name}")
            channelCount++
            break
        }
    }
    tag.channelCount = channelCount

    return tag
}
