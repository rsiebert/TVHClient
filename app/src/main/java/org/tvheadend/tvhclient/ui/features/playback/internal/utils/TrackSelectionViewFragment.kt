package org.tvheadend.tvhclient.ui.features.playback.internal.utils

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.media3.common.TrackGroup
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.RendererCapabilities
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector.SelectionOverride
import androidx.media3.exoplayer.trackselection.MappingTrackSelector.MappedTrackInfo
import androidx.media3.ui.TrackSelectionView
import androidx.media3.ui.TrackSelectionView.TrackSelectionListener
import org.tvheadend.tvhclient.R

class TrackSelectionViewFragment : Fragment(), TrackSelectionListener {

    private var trackGroups: List<Tracks.Group> = emptyList()
    private var allowAdaptiveSelections = false
    private var allowMultipleOverrides = false

    var isDisabled = false
    var overrides: Map<TrackGroup, TrackSelectionOverride> = emptyMap()

    init {
        // Retain instance across activity re-creation to prevent losing access to init data.
        retainInstance = true
    }

    // The support values come straight from MappedTrackInfo#getTrackSupport, which is declared
    // @C.FormatSupport, so they are valid; the array is built at runtime, which the inspection on
    // the annotated Tracks.Group parameter cannot see.
    @Suppress("WrongConstant")
    fun init(mappedTrackInfo: MappedTrackInfo,
             rendererIndex: Int,
             initialIsDisabled: Boolean,
             initialOverride: SelectionOverride?,
             allowAdaptiveSelections: Boolean = true,
             allowMultipleOverrides: Boolean = false) {

        this.isDisabled = initialIsDisabled
        this.allowAdaptiveSelections = allowAdaptiveSelections
        this.allowMultipleOverrides = allowMultipleOverrides

        val trackGroupArray = mappedTrackInfo.getTrackGroups(rendererIndex)

        // Media3's TrackSelectionView operates on Tracks.Group instances, so translate the mapped
        // track information for this renderer into that representation.
        val groups = ArrayList<Tracks.Group>(trackGroupArray.length)
        for (groupIndex in 0 until trackGroupArray.length) {
            val trackGroup = trackGroupArray[groupIndex]
            val trackSupport = IntArray(trackGroup.length) { mappedTrackInfo.getTrackSupport(rendererIndex, groupIndex, it) }
            val trackSelected = BooleanArray(trackGroup.length) { false }
            val adaptiveSupported = mappedTrackInfo.getAdaptiveSupport(rendererIndex, groupIndex, false) != RendererCapabilities.ADAPTIVE_NOT_SUPPORTED
            groups.add(Tracks.Group(trackGroup, adaptiveSupported, trackSupport, trackSelected))
        }
        this.trackGroups = groups

        this.overrides = if (initialOverride != null && initialOverride.groupIndex < trackGroupArray.length) {
            val trackGroup = trackGroupArray[initialOverride.groupIndex]
            mapOf(trackGroup to TrackSelectionOverride(trackGroup, initialOverride.tracks.toList()))
        } else {
            emptyMap()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // The app owns this layout; media3-ui's own copy and its id are private resources.
        val rootView = inflater.inflate(R.layout.track_selection_view, container, false)
        val trackSelectionView: TrackSelectionView = rootView.findViewById(R.id.track_selection_view)

        trackSelectionView.setShowDisableOption(true)
        trackSelectionView.setAllowMultipleOverrides(allowMultipleOverrides)
        trackSelectionView.setAllowAdaptiveSelections(allowAdaptiveSelections)
        trackSelectionView.init(trackGroups, isDisabled, overrides, null, this)
        return rootView
    }

    override fun onTrackSelectionChanged(isDisabled: Boolean, overrides: Map<TrackGroup, TrackSelectionOverride>) {
        this.isDisabled = isDisabled
        this.overrides = overrides
    }
}
