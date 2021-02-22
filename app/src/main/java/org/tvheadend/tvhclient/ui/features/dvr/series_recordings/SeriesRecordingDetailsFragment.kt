package org.tvheadend.tvhclient.ui.features.dvr.series_recordings

import android.os.Bundle
import android.view.*
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import org.tvheadend.data.entity.SeriesRecording
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.databinding.SeriesRecordingDetailsFragmentBinding
import org.tvheadend.tvhclient.ui.base.BaseFragment
import org.tvheadend.tvhclient.ui.common.*
import org.tvheadend.tvhclient.ui.common.interfaces.ClearSearchResultsOrPopBackStackInterface
import org.tvheadend.tvhclient.ui.common.interfaces.RecordingRemovedInterface
import org.tvheadend.tvhclient.util.extensions.gone
import org.tvheadend.tvhclient.util.extensions.visible

class SeriesRecordingDetailsFragment : BaseFragment(), RecordingRemovedInterface, ClearSearchResultsOrPopBackStackInterface {

    private lateinit var seriesRecordingViewModel: SeriesRecordingViewModel
    private var recording: SeriesRecording? = null
    private lateinit var binding: SeriesRecordingDetailsFragmentBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.series_recording_details_fragment, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        seriesRecordingViewModel = ViewModelProvider(requireActivity()).get(SeriesRecordingViewModel::class.java)

        if (!isDualPane) {
            toolbarInterface.setTitle(getString(R.string.details))
            toolbarInterface.setSubtitle("")
        }

        arguments?.let {
            seriesRecordingViewModel.currentIdLiveData.value = it.getString("id", "")
        }

        seriesRecordingViewModel.recordingLiveData.observe(viewLifecycleOwner,  {
            recording = it
            showRecordingDetails()
        })
    }

    private fun showRecordingDetails() {
        recording?.let {
            binding.recording = it
            binding.htspVersion = htspVersion
            binding.isDualPane = isDualPane
            binding.duplicateDetectionText = seriesRecordingViewModel.duplicateDetectionList[it.dupDetect]
            // The toolbar is hidden as a default to prevent pressing any icons if no recording
            // has been loaded yet. The toolbar is shown here because a recording was loaded
            binding.nestedToolbar.visible()
            activity?.invalidateOptionsMenu()
        } ?: run {
            binding.scrollview.gone()
            binding.status.text = getString(R.string.error_loading_recording_details)
            binding.status.visible()
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        val recording = this.recording ?: return
        preparePopupOrToolbarSearchMenu(menu, recording.title, isConnectionToServerAvailable)

        binding.nestedToolbar.menu.findItem(R.id.menu_edit_recording)?.isVisible = true
        binding.nestedToolbar.menu.findItem(R.id.menu_remove_recording)?.isVisible = true
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.external_search_options_menu, menu)
        binding.nestedToolbar.inflateMenu(R.menu.recording_details_toolbar_menu)
        binding.nestedToolbar.setOnMenuItemClickListener { this.onOptionsItemSelected(it) }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val ctx = context ?: return super.onOptionsItemSelected(item)
        val recording = this.recording ?: return super.onOptionsItemSelected(item)

        return when (item.itemId) {
            R.id.menu_edit_recording -> editSelectedSeriesRecording(requireActivity(), recording.id)
            R.id.menu_remove_recording -> showConfirmationToRemoveSelectedSeriesRecording(ctx, recording, this)

            R.id.menu_search_imdb -> return searchTitleOnImdbWebsite(ctx, recording.title)
            R.id.menu_search_fileaffinity -> return searchTitleOnFileAffinityWebsite(ctx, recording.title)
            R.id.menu_search_youtube -> return searchTitleOnYoutube(ctx, recording.title)
            R.id.menu_search_google -> return searchTitleOnGoogle(ctx, recording.title)
            R.id.menu_search_epg -> return searchTitleInTheLocalDatabase(requireActivity(), baseViewModel, recording.title)
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onRecordingRemoved() {
        if (!isDualPane) {
            activity?.onBackPressed()
        } else {
            val detailsFragment = activity?.supportFragmentManager?.findFragmentById(R.id.details)
            if (detailsFragment != null) {
                activity?.supportFragmentManager?.beginTransaction()?.also {
                    it.remove(detailsFragment)
                    it.commit()
                }
            }
        }
    }

    companion object {

        fun newInstance(id: String): SeriesRecordingDetailsFragment {
            val f = SeriesRecordingDetailsFragment()
            val args = Bundle()
            args.putString("id", id)
            f.arguments = args
            return f
        }
    }
}
