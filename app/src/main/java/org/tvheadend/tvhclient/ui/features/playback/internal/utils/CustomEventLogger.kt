package org.tvheadend.tvhclient.ui.features.playback.internal.utils

import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.Format
import com.google.android.exoplayer2.RendererCapabilities
import com.google.android.exoplayer2.analytics.AnalyticsListener
import com.google.android.exoplayer2.analytics.AnalyticsListener.EventTime
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.MappingTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import timber.log.Timber

class CustomEventLogger(private val trackSelector: MappingTrackSelector) : AnalyticsListener {

    override fun onTracksChanged(eventTime: EventTime, ignored: TrackGroupArray, trackSelections: TrackSelectionArray) {

        val mappedTrackInfo = trackSelector.currentMappedTrackInfo
        if (mappedTrackInfo == null) {
            Timber.d("No media tracks available")
            return
        }

        Timber.d("Available media tracks:")

        // Log tracks associated to renderers.
        for (rendererIndex in 0 until mappedTrackInfo.rendererCount) {
            val rendererTrackGroups = mappedTrackInfo.getTrackGroups(rendererIndex)
            val trackSelection = trackSelections[rendererIndex]

            if (rendererTrackGroups.length > 0) {
                Timber.d("  Renderer:$rendererIndex")

                for (groupIndex in 0 until rendererTrackGroups.length) {
                    val trackGroup = rendererTrackGroups[groupIndex]
                    val adaptiveSupport = getAdaptiveSupportString(trackGroup.length, mappedTrackInfo.getAdaptiveSupport(rendererIndex, groupIndex, false))
                    Timber.d("    Group:$groupIndex, adaptive streaming supported:$adaptiveSupport")

                    for (trackIndex in 0 until trackGroup.length) {
                        val isEnabled = getTrackStatusString(trackSelection != null && trackSelection.trackGroup === trackGroup && trackSelection.indexOf(trackIndex) != C.INDEX_UNSET)
                        val metadata = Format.toLogString(trackGroup.getFormat(trackIndex))
                        val formatSupport = getFormatSupportString(mappedTrackInfo.getTrackSupport(rendererIndex, groupIndex, trackIndex))
                        Timber.d("      Track:$trackIndex, selected=$isEnabled, $metadata, supported=$formatSupport")
                    }
                }
                // Log metadata for at most one of the tracks selected for the renderer.
                if (trackSelection != null) {
                    for (selectionIndex in 0 until trackSelection.length()) {
                        val metadata = trackSelection.getFormat(selectionIndex).metadata
                        if (metadata != null) {
                            Timber.d("    Metadata:")
                            for (i in 0 until metadata.length()) {
                                Timber.d("      ${metadata[i]}")
                            }
                            break
                        }
                    }
                }
            }
        }

        // Log tracks not associated with a renderer.
        val unassociatedTrackGroups = mappedTrackInfo.unmappedTrackGroups
        if (unassociatedTrackGroups.length > 0) {
            Timber.d("  Renderer:none")
            for (groupIndex in 0 until unassociatedTrackGroups.length) {
                Timber.d("    Group:$groupIndex")
                val trackGroup = unassociatedTrackGroups[groupIndex]
                for (trackIndex in 0 until trackGroup.length) {
                    val isEnabled = getTrackStatusString(false)
                    val metadata = Format.toLogString(trackGroup.getFormat(trackIndex))
                    val formatSupport = getFormatSupportString(RendererCapabilities.FORMAT_UNSUPPORTED_TYPE)
                    Timber.d("      Track:$trackIndex, selected=$isEnabled, $metadata, supported=$formatSupport")
                }
            }
        }
    }

    private fun getFormatSupportString(formatSupport: Int): String {
        return when (formatSupport) {
            RendererCapabilities.FORMAT_HANDLED -> "yes"
            RendererCapabilities.FORMAT_EXCEEDS_CAPABILITIES -> "no, exceeds capabilities"
            RendererCapabilities.FORMAT_UNSUPPORTED_DRM -> "no, unsupported drm"
            RendererCapabilities.FORMAT_UNSUPPORTED_SUBTYPE -> "no, unsupported type"
            RendererCapabilities.FORMAT_UNSUPPORTED_TYPE -> "no"
            else -> "unknown"
        }
    }

    private fun getAdaptiveSupportString(trackCount: Int, adaptiveSupport: Int): String {
        return if (trackCount < 2) {
            "n/a"
        } else when (adaptiveSupport) {
            RendererCapabilities.ADAPTIVE_SEAMLESS -> "yes"
            RendererCapabilities.ADAPTIVE_NOT_SEAMLESS -> "yes but not seamless"
            RendererCapabilities.ADAPTIVE_NOT_SUPPORTED -> "no"
            else -> "unknown"
        }
    }

    private fun getTrackStatusString(enabled: Boolean): String {
        return if (enabled) "yes" else "no"
    }
}