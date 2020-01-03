package org.tvheadend.tvhclient.ui.features.playback.internal

interface HtspDataSourceInterface {

    val timeshiftOffsetPts: Long

    val timeshiftStartTime: Long

    val timeshiftStartPts: Long

    fun setSpeed(tvhSpeed: Int)

    fun resume()

    fun pause()

    fun getResponseHeaders(): Map<String, List<String>>?
}
