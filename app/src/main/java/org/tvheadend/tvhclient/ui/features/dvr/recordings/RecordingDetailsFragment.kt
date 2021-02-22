package org.tvheadend.tvhclient.ui.features.dvr.recordings

import android.os.Bundle
import android.view.*
import androidx.lifecycle.ViewModelProvider
import org.tvheadend.data.entity.Recording
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.databinding.RecordingDetailsFragmentBinding
import org.tvheadend.tvhclient.ui.base.BaseFragment
import org.tvheadend.tvhclient.ui.common.*
import org.tvheadend.tvhclient.ui.common.interfaces.ClearSearchResultsOrPopBackStackInterface
import org.tvheadend.tvhclient.ui.common.interfaces.RecordingRemovedInterface
import org.tvheadend.tvhclient.ui.features.dvr.recordings.download.DownloadPermissionGrantedInterface
import org.tvheadend.tvhclient.util.extensions.gone
import org.tvheadend.tvhclient.util.extensions.visible
import timber.log.Timber

class RecordingDetailsFragment : BaseFragment(), RecordingRemovedInterface, DownloadPermissionGrantedInterface, ClearSearchResultsOrPopBackStackInterface {

    private lateinit var recordingViewModel: RecordingViewModel
    private var recording: Recording? = null
    private lateinit var binding: RecordingDetailsFragmentBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = RecordingDetailsFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recordingViewModel = ViewModelProvider(requireActivity()).get(RecordingViewModel::class.java)

        if (!isDualPane) {
            toolbarInterface.setTitle(getString(R.string.details))
            toolbarInterface.setSubtitle("")
        }

        // Get the recording id after an orientation change has occurred
        // or when the fragment is shown for the first time
        arguments?.let {
            recordingViewModel.currentIdLiveData.value = it.getInt("id", 0)
        }

        Timber.d("Observing recording")
        recordingViewModel.recordingLiveData.observe(viewLifecycleOwner,  {
            Timber.d("View model returned a recording")
            recording = it
            showRecordingDetails()
        })
    }

    private fun showRecordingDetails() {
        if (recording != null) {
            binding.recording = recording
            binding.htspVersion = htspVersion
            // The toolbar is hidden as a default to prevent pressing any icons if no recording
            // has been loaded yet. The toolbar is shown here because a recording was loaded
            binding.nestedToolbar.visible()
            activity?.invalidateOptionsMenu()
        } else {
            binding.scrollview.gone()
            binding.status.text = getString(R.string.error_loading_recording_details)
            binding.status.visible()
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        val ctx = context ?: return
        val recording = recording ?: return
        preparePopupOrToolbarMiscMenu(ctx, binding.nestedToolbar.menu, null, isConnectionToServerAvailable, isUnlocked)
        preparePopupOrToolbarRecordingMenu(ctx, binding.nestedToolbar.menu, recording, isConnectionToServerAvailable, htspVersion, isUnlocked)
        preparePopupOrToolbarSearchMenu(menu, recording.title, isConnectionToServerAvailable)
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

        when (item.itemId) {
            R.id.menu_stop_recording -> return showConfirmationToStopSelectedRecording(ctx, recording, this)
            R.id.menu_cancel_recording -> return showConfirmationToCancelSelectedRecording(ctx, recording, this)
            R.id.menu_remove_recording -> return showConfirmationToRemoveSelectedRecording(ctx, recording, this)
            R.id.menu_edit_recording -> return editSelectedRecording(requireActivity(), recording.id)
            R.id.menu_play -> return playSelectedRecording(ctx, recording.id, isUnlocked)
            R.id.menu_cast -> return castSelectedRecording(ctx, recording.id)

            R.id.menu_search_imdb -> return searchTitleOnImdbWebsite(ctx, recording.title)
            R.id.menu_search_fileaffinity -> return searchTitleOnFileAffinityWebsite(ctx, recording.title)
            R.id.menu_search_youtube -> return searchTitleOnYoutube(ctx, recording.title)
            R.id.menu_search_google -> return searchTitleOnGoogle(ctx, recording.title)
            R.id.menu_search_epg -> return searchTitleInTheLocalDatabase(requireActivity(), baseViewModel, recording.title)

            R.id.menu_download_recording -> return downloadSelectedRecording(ctx, recording.id)
            else -> return super.onOptionsItemSelected(item)
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

    override fun downloadRecording() {
        //DownloadRecordingManager(activity, connection, recording)
        val id = recording?.id
        if (id != null) {
            downloadSelectedRecording(requireContext(), id)
        }
    }

    companion object {

        fun newInstance(dvrId: Int): RecordingDetailsFragment {
            val f = RecordingDetailsFragment()
            val args = Bundle()
            args.putInt("id", dvrId)
            f.arguments = args
            return f
        }
    }
}
