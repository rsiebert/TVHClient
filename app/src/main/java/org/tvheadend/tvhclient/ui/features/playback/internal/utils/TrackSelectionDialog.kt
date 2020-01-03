package org.tvheadend.tvhclient.ui.features.playback.internal.utils

import android.app.Dialog
import android.content.DialogInterface
import android.content.res.Resources
import android.os.Bundle
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.app.AppCompatDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector.SelectionOverride
import com.google.android.exoplayer2.trackselection.MappingTrackSelector.MappedTrackInfo
import com.google.android.exoplayer2.util.Assertions
import com.google.android.material.tabs.TabLayout
import org.tvheadend.tvhclient.R
import java.util.*

class TrackSelectionDialog : DialogFragment() {

    private val tabFragments: SparseArray<TrackSelectionViewFragment> = SparseArray()
    private val tabTrackTypes: ArrayList<Int> = ArrayList()
    private var titleId = 0
    private lateinit var onClickListener: DialogInterface.OnClickListener
    private lateinit var onDismissListener: DialogInterface.OnDismissListener

    private fun init(mappedTrackInfo: MappedTrackInfo,
                     initialParameters: DefaultTrackSelector.Parameters,
                     onClickListener: DialogInterface.OnClickListener,
                     onDismissListener: DialogInterface.OnDismissListener) {

        this.titleId = R.string.track_selection_title
        this.onClickListener = onClickListener
        this.onDismissListener = onDismissListener

        for (i in 0 until mappedTrackInfo.rendererCount) {
            if (showTabForRenderer(mappedTrackInfo, i)) {
                val trackType = mappedTrackInfo.getRendererType(i)
                val trackGroupArray = mappedTrackInfo.getTrackGroups(i)
                val tabFragment = TrackSelectionViewFragment()
                tabFragment.init(mappedTrackInfo, i,
                        initialParameters.getRendererDisabled(i),
                        initialParameters.getSelectionOverride(i, trackGroupArray))
                tabFragments.put(i, tabFragment)
                tabTrackTypes.add(trackType)
            }
        }
    }

    private fun getIsDisabled(rendererIndex: Int): Boolean {
        val rendererView = tabFragments[rendererIndex]
        return rendererView != null && rendererView.isDisabled
    }

    /**
     * Returns the list of selected track selection overrides for the specified renderer. There will
     * be at most one override for each track group.
     *
     * @param rendererIndex Renderer index.
     * @return The list of track selection overrides for this renderer.
     */
    private fun getOverrides(rendererIndex: Int): List<SelectionOverride> {
        val rendererView = tabFragments[rendererIndex]
        return rendererView?.overrides ?: emptyList()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // We need to own the view to let tab layout work correctly on all API levels. We can't use
        // AlertDialog because it owns the view itself, so we use AppCompatDialog instead, themed using
        // the AlertDialog theme overlay with force-enabled title.
        val dialog = AppCompatDialog(activity, R.style.TrackSelectionDialogThemeOverlay)
        dialog.setTitle(titleId)
        return dialog
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onDismissListener.onDismiss(dialog)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val dialogView = inflater.inflate(R.layout.track_selection_dialog, container, false)
        val tabLayout: TabLayout = dialogView.findViewById(R.id.track_selection_dialog_tab_layout)
        val viewPager: ViewPager = dialogView.findViewById(R.id.track_selection_dialog_view_pager)
        val cancelButton = dialogView.findViewById<Button>(R.id.track_selection_dialog_cancel_button)
        val okButton = dialogView.findViewById<Button>(R.id.track_selection_dialog_ok_button)

        viewPager.adapter = FragmentAdapter(childFragmentManager)
        tabLayout.setupWithViewPager(viewPager)
        tabLayout.visibility = if (tabFragments.size() > 1) View.VISIBLE else View.GONE
        cancelButton.setOnClickListener { dismiss() }
        okButton.setOnClickListener {
            onClickListener.onClick(dialog, DialogInterface.BUTTON_POSITIVE)
            dismiss()
        }
        return dialogView
    }

    private inner class FragmentAdapter internal constructor(fragmentManager: FragmentManager?) : FragmentPagerAdapter(fragmentManager!!, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
        override fun getItem(position: Int): Fragment {
            return tabFragments.valueAt(position)
        }

        override fun getCount(): Int {
            return tabFragments.size()
        }

        override fun getPageTitle(position: Int): CharSequence {
            return getTrackTypeString(resources, tabTrackTypes[position])
        }
    }

    companion object {

        /**
         * Returns whether a track selection dialog will have content to display
         * if initialized with the specified [DefaultTrackSelector] in its current state.
         */
        fun willHaveContent(trackSelector: DefaultTrackSelector): Boolean {
            val mappedTrackInfo = trackSelector.currentMappedTrackInfo
            return mappedTrackInfo != null && willHaveContent(mappedTrackInfo)
        }

        /**
         * Returns whether a track selection dialog will have content to display
         * if initialized with the specified [MappedTrackInfo].
         */
        private fun willHaveContent(mappedTrackInfo: MappedTrackInfo): Boolean {
            for (i in 0 until mappedTrackInfo.rendererCount) {
                if (showTabForRenderer(mappedTrackInfo, i)) {
                    return true
                }
            }
            return false
        }

        /**
         * Creates a dialog for a given [DefaultTrackSelector], whose parameters
         * will be automatically updated when tracks are selected.
         *
         * @param trackSelector     The [DefaultTrackSelector].
         * @param onDismissListener A [DialogInterface.OnDismissListener] to call when the dialog is dismissed.
         */
        fun createForTrackSelector(trackSelector: DefaultTrackSelector, onDismissListener: DialogInterface.OnDismissListener): TrackSelectionDialog {
            val mappedTrackInfo = Assertions.checkNotNull(trackSelector.currentMappedTrackInfo)
            val parameters = trackSelector.parameters
            val trackSelectionDialog = TrackSelectionDialog()

            trackSelectionDialog.init(mappedTrackInfo, parameters, DialogInterface.OnClickListener { _: DialogInterface?, _: Int ->
                val builder = parameters.buildUpon()
                for (i in 0 until mappedTrackInfo.rendererCount) {
                    builder.clearSelectionOverrides(i).setRendererDisabled(i, trackSelectionDialog.getIsDisabled(i))
                    val overrides = trackSelectionDialog.getOverrides(i)
                    if (overrides.isNotEmpty()) {
                        builder.setSelectionOverride(i, mappedTrackInfo.getTrackGroups(i), overrides[0])
                    }
                }
                trackSelector.setParameters(builder)
            }, onDismissListener = onDismissListener)
            return trackSelectionDialog
        }

        private fun showTabForRenderer(mappedTrackInfo: MappedTrackInfo, rendererIndex: Int): Boolean {
            val trackGroupArray = mappedTrackInfo.getTrackGroups(rendererIndex)
            if (trackGroupArray.length == 0) {
                return false
            }
            val trackType = mappedTrackInfo.getRendererType(rendererIndex)
            return isSupportedTrackType(trackType)
        }

        private fun isSupportedTrackType(trackType: Int): Boolean {
            return when (trackType) {
                C.TRACK_TYPE_VIDEO, C.TRACK_TYPE_AUDIO, C.TRACK_TYPE_TEXT -> true
                else -> false
            }
        }

        private fun getTrackTypeString(resources: Resources, trackType: Int): String {
            return when (trackType) {
                C.TRACK_TYPE_VIDEO -> resources.getString(R.string.exo_track_selection_title_video)
                C.TRACK_TYPE_AUDIO -> resources.getString(R.string.exo_track_selection_title_audio)
                C.TRACK_TYPE_TEXT -> resources.getString(R.string.exo_track_selection_title_text)
                else -> throw IllegalArgumentException()
            }
        }
    }

    init {
        // Retain instance across activity re-creation to prevent losing access to init data.
        retainInstance = true
    }
}