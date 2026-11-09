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
        // These two belong to the media3-ui library, not to the app, so they are referenced
        // through its R class.
        val rootView = inflater.inflate(androidx.media3.ui.R.layout.exo_track_selection_dialog, container, false)
        val trackSelectionView: TrackSelectionView =
                rootView.findViewById(androidx.media3.ui.R.id.exo_track_selection_view)

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
