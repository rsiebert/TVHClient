package org.tvheadend.tvhclient.ui.features.dvr.recordings

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import kotlinx.android.synthetic.main.details_fragment_header.*
import kotlinx.android.synthetic.main.recording_details_fragment.*
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.databinding.RecordingDetailsFragmentBinding
import org.tvheadend.tvhclient.domain.entity.Recording
import org.tvheadend.tvhclient.ui.base.BaseFragment
import org.tvheadend.tvhclient.ui.common.*
import org.tvheadend.tvhclient.ui.features.download.DownloadPermissionGrantedInterface
import org.tvheadend.tvhclient.ui.features.download.DownloadRecordingManager
import org.tvheadend.tvhclient.ui.features.dvr.RecordingAddEditActivity
import org.tvheadend.tvhclient.ui.features.dvr.RecordingRemovedCallback

// TODO put shownId into the viewmodel

class RecordingDetailsFragment : BaseFragment(), RecordingRemovedCallback, DownloadPermissionGrantedInterface {

    private var recording: Recording? = null
    var shownDvrId: Int = 0
    private lateinit var itemBinding: RecordingDetailsFragmentBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        itemBinding = DataBindingUtil.inflate(inflater, R.layout.recording_details_fragment, container, false)
        return itemBinding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (!isDualPane) {
            toolbarInterface.setTitle(getString(R.string.details))
            toolbarInterface.setSubtitle("")
        }

        // Get the recording id after an orientation change has occurred
        // or when the fragment is shown for the first time
        shownDvrId = savedInstanceState?.getInt("id", 0) ?: (arguments?.getInt("id", 0) ?: 0)

        viewModel.getRecordingById(shownDvrId)?.observe(viewLifecycleOwner, Observer { rec ->
            if (rec != null) {
                recording = rec
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
        })
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        val ctx = context ?: return
        val recording = recording ?: return
        // Show or hide search menu items in the main toolbar
        prepareSearchMenu(menu, recording.title, isNetworkAvailable)
        // Show or hide menus of the nested toolbar
        prepareMenu(ctx, nested_toolbar.menu, null, recording, isNetworkAvailable, htspVersion, isUnlocked)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("id", shownDvrId)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.external_search_options_menu, menu)
        nested_toolbar.inflateMenu(R.menu.recording_details_toolbar_menu)
        nested_toolbar.setOnMenuItemClickListener { this.onOptionsItemSelected(it) }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val ctx = context ?: return super.onOptionsItemSelected(item)

        // The recording might be null in case the viewmodel
        // has not yet loaded the recording for the given id
        val recording = this.recording ?: return super.onOptionsItemSelected(item)

        if (onMenuSelected(ctx, item.itemId, recording.title)) {
            return true
        }
        when (item.itemId) {
            R.id.menu_play ->
                return menuUtils.handleMenuPlayRecording(recording.id)
            R.id.menu_cast ->
                return menuUtils.handleMenuCast("dvrId", recording.id)
            R.id.menu_download -> {
                activity?.let {
                    DownloadRecordingManager(it, connection, serverStatus, recording)
                }
                return true
            }
            R.id.menu_edit -> {
                val editIntent = Intent(activity, RecordingAddEditActivity::class.java)
                editIntent.putExtra("id", recording.id)
                editIntent.putExtra("type", "recording")
                activity?.startActivity(editIntent)
                return true
            }
            R.id.menu_record_stop ->
                return menuUtils.handleMenuStopRecordingSelection(recording, this)
            R.id.menu_record_cancel ->
                return menuUtils.handleMenuCancelRecordingSelection(recording, this)
            R.id.menu_record_remove ->
                return menuUtils.handleMenuRemoveRecordingSelection(recording, this)
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
        activity?.let {
            recording?.let { rec ->
                DownloadRecordingManager(it, connection, serverStatus, rec)
            }
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
