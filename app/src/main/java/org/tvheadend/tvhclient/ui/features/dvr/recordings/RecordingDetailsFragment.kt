package org.tvheadend.tvhclient.ui.features.dvr.recordings

import android.os.Bundle
import android.view.*
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import kotlinx.android.synthetic.main.details_fragment_header.*
import kotlinx.android.synthetic.main.recording_details_fragment.*
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.databinding.RecordingDetailsFragmentBinding
import org.tvheadend.tvhclient.data.entity.Recording
import org.tvheadend.tvhclient.ui.base.BaseFragment
import org.tvheadend.tvhclient.ui.common.*
import org.tvheadend.tvhclient.ui.features.download.DownloadPermissionGrantedInterface
import org.tvheadend.tvhclient.ui.features.download.DownloadRecordingManager
import org.tvheadend.tvhclient.ui.features.dvr.RecordingRemovedCallback
import org.tvheadend.tvhclient.util.extensions.gone
import org.tvheadend.tvhclient.util.extensions.visible
import timber.log.Timber

// TODO Observe the id and load the recording as livedata

class RecordingDetailsFragment : BaseFragment(), RecordingRemovedCallback, DownloadPermissionGrantedInterface {

    private lateinit var recordingViewModel: RecordingViewModel
    private var recording: Recording? = null
    private lateinit var itemBinding: RecordingDetailsFragmentBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        itemBinding = DataBindingUtil.inflate(inflater, R.layout.recording_details_fragment, container, false)
        return itemBinding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        recordingViewModel = ViewModelProviders.of(activity!!).get(RecordingViewModel::class.java)

        if (!isDualPane) {
            toolbarInterface.setTitle(getString(R.string.details))
            toolbarInterface.setSubtitle("")
        }

        // Get the recording id after an orientation change has occurred
        // or when the fragment is shown for the first time
        arguments?.let {
            recordingViewModel.currentId.value = it.getInt("id", 0)
        }

        Timber.d("Observing recording")
        recordingViewModel.recordingLiveData.observe(viewLifecycleOwner, Observer {
            Timber.d("View model returned a recording")
            recording = it
            showRecordingDetails()
        })
    }

    private fun showRecordingDetails() {
        if (recording != null) {
            itemBinding.recording = recording
            itemBinding.htspVersion = htspVersion
            // The toolbar is hidden as a default to prevent pressing any icons if no recording
            // has been loaded yet. The toolbar is shown here because a recording was loaded
            nested_toolbar.visible()
            activity?.invalidateOptionsMenu()
        } else {
            scrollview.gone()
            status.text = getString(R.string.error_loading_recording_details)
            status.visible()
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        val ctx = context ?: return
        val recording = recording ?: return
        preparePopupOrToolbarMiscMenu(ctx, nested_toolbar.menu, null, isConnectionToServerAvailable, isUnlocked)
        preparePopupOrToolbarRecordingMenu(ctx, nested_toolbar.menu, recording, isConnectionToServerAvailable, htspVersion, isUnlocked)
        preparePopupOrToolbarSearchMenu(menu, recording.title, isConnectionToServerAvailable)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.external_search_options_menu, menu)
        nested_toolbar.inflateMenu(R.menu.recording_details_toolbar_menu)
        nested_toolbar.setOnMenuItemClickListener { this.onOptionsItemSelected(it) }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val ctx = context ?: return super.onOptionsItemSelected(item)
        val recording = this.recording ?: return super.onOptionsItemSelected(item)

        when (item.itemId) {
            R.id.menu_stop_recording -> return showConfirmationToStopSelectedRecording(ctx, recording, this)
            R.id.menu_cancel_recording -> return showConfirmationToCancelSelectedRecording(ctx, recording, this)
            R.id.menu_remove_recording -> return showConfirmationToRemoveSelectedRecording(ctx, recording, this)
            R.id.menu_edit_recording -> return editSelectedRecording(ctx, recording.id)
            R.id.menu_play -> return playSelectedRecording(ctx, recording.id, isUnlocked)
            R.id.menu_cast -> return castSelectedRecording(ctx, recording.id)

            R.id.menu_search_imdb -> return searchTitleOnImdbWebsite(ctx, recording.title)
            R.id.menu_search_fileaffinity -> return searchTitleOnFileAffinityWebsite(ctx, recording.title)
            R.id.menu_search_youtube -> return searchTitleOnYoutube(ctx, recording.title)
            R.id.menu_search_google -> return searchTitleOnGoogle(ctx, recording.title)
            R.id.menu_search_epg -> return searchTitleInTheLocalDatabase(activity!!, baseViewModel, recording.title)

            R.id.menu_download_recording -> {
                DownloadRecordingManager(activity, connection, recording)
                return true
            }
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
        DownloadRecordingManager(activity, connection, recording)
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
