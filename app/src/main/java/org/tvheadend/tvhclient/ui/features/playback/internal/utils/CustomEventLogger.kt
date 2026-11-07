package org.tvheadend.tvhclient.ui.features.playback.internal.utils

import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.analytics.AnalyticsListener.EventTime
import androidx.media3.exoplayer.trackselection.MappingTrackSelector
import timber.log.Timber

class CustomEventLogger(private val trackSelector: MappingTrackSelector) : AnalyticsListener {

    override fun onTracksChanged(eventTime: EventTime, tracks: Tracks) {

        if (tracks.isEmpty) {
            Timber.d("No media tracks available")
            return
        }

        Timber.d("Available media tracks:")

        val groups = tracks.groups
        for (groupIndex in groups.indices) {
            val group = groups[groupIndex]
            Timber.d("  Group:$groupIndex, type:${group.type}, track count:${group.length}")

            for (trackIndex in 0 until group.length) {
                val isSelected = getTrackStatusString(group.isTrackSelected(trackIndex))
                val metadata = Format.toLogString(group.getTrackFormat(trackIndex))
                val formatSupport = getFormatSupportString(group.getTrackSupport(trackIndex))
                Timber.d("      Track:$trackIndex, selected=$isSelected, $metadata, supported=$formatSupport")
            }
        }
    }

    private fun getFormatSupportString(formatSupport: Int): String {
        return when (formatSupport) {
            C.FORMAT_HANDLED -> "yes"
            C.FORMAT_EXCEEDS_CAPABILITIES -> "no, exceeds capabilities"
            C.FORMAT_UNSUPPORTED_DRM -> "no, unsupported drm"
            C.FORMAT_UNSUPPORTED_SUBTYPE -> "no, unsupported type"
            C.FORMAT_UNSUPPORTED_TYPE -> "no"
            else -> "unknown"
        }
    }

    private fun getTrackStatusString(enabled: Boolean): String {
        return if (enabled) "yes" else "no"
    }
}
