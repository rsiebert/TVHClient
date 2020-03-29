package org.tvheadend.tvhclient.ui.features.playback.internal.utils

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector.SelectionOverride
import com.google.android.exoplayer2.trackselection.MappingTrackSelector.MappedTrackInfo
import com.google.android.exoplayer2.ui.TrackSelectionView
import com.google.android.exoplayer2.ui.TrackSelectionView.TrackSelectionListener
import org.tvheadend.tvhclient.R

class TrackSelectionViewFragment : Fragment(), TrackSelectionListener {

    private lateinit var mappedTrackInfo: MappedTrackInfo
    private var rendererIndex = 0
    private var allowAdaptiveSelections = false
    private var allowMultipleOverrides = false

    var isDisabled = false
    var overrides: List<SelectionOverride> = emptyList()

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

        this.mappedTrackInfo = mappedTrackInfo
        this.rendererIndex = rendererIndex
        this.isDisabled = initialIsDisabled
        this.overrides = initialOverride?.let { listOf(it) } ?: emptyList()
        this.allowAdaptiveSelections = allowAdaptiveSelections
        this.allowMultipleOverrides = allowMultipleOverrides
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.exo_track_selection_dialog, container, false)
        val trackSelectionView: TrackSelectionView = rootView.findViewById(R.id.exo_track_selection_view)

        trackSelectionView.setShowDisableOption(true)
        trackSelectionView.setAllowMultipleOverrides(allowMultipleOverrides)
        trackSelectionView.setAllowAdaptiveSelections(allowAdaptiveSelections)
        trackSelectionView.init(mappedTrackInfo, rendererIndex, isDisabled, overrides, this)
        return rootView
    }

    override fun onTrackSelectionChanged(isDisabled: Boolean, overrides: List<SelectionOverride>) {
        this.isDisabled = isDisabled
        this.overrides = overrides
    }
}