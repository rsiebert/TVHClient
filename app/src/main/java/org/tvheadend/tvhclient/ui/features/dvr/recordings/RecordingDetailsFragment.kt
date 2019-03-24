package org.tvheadend.tvhclient.ui.features.dvr.recordings

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.databinding.RecordingDetailsFragmentBinding
import org.tvheadend.tvhclient.domain.entity.Recording
import org.tvheadend.tvhclient.ui.base.BaseFragment
import org.tvheadend.tvhclient.ui.common.getCastSession
import org.tvheadend.tvhclient.ui.common.onMenuSelected
import org.tvheadend.tvhclient.ui.common.prepareSearchMenu
import org.tvheadend.tvhclient.ui.features.download.DownloadPermissionGrantedInterface
import org.tvheadend.tvhclient.ui.features.download.DownloadRecordingManager
import org.tvheadend.tvhclient.ui.features.dvr.RecordingAddEditActivity
import org.tvheadend.tvhclient.ui.features.dvr.RecordingRemovedCallback

// TODO put shownId into the viewmodel

class RecordingDetailsFragment : BaseFragment(), RecordingRemovedCallback, DownloadPermissionGrantedInterface {

    private lateinit var nestedToolbar: Toolbar
    private lateinit var scrollView: ScrollView
    private lateinit var statusTextView: TextView

    private var recording: Recording? = null
    var shownDvrId: Int = 0
    private lateinit var itemBinding: RecordingDetailsFragmentBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        itemBinding = DataBindingUtil.inflate(inflater, R.layout.recording_details_fragment, container, false)
        val view = itemBinding.root

        nestedToolbar = view.findViewById(R.id.nested_toolbar)
        scrollView = view.findViewById(R.id.scrollview)
        statusTextView = view.findViewById(R.id.status)
        return view
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

        val viewModel = ViewModelProviders.of(activity).get(RecordingViewModel::class.java)
        viewModel.getRecordingById(shownDvrId)?.observe(viewLifecycleOwner, Observer { rec ->
            if (rec != null) {
                recording = rec
                itemBinding.recording = recording
                itemBinding.htspVersion = htspVersion
                // The toolbar is hidden as a default to prevent pressing any icons if no recording
                // has been loaded yet. The toolbar is shown here because a recording was loaded
                nestedToolbar.visibility = View.VISIBLE
                activity.invalidateOptionsMenu()
            } else {
                scrollView.visibility = View.GONE
                statusTextView.text = getString(R.string.error_loading_recording_details)
                statusTextView.visibility = View.VISIBLE
            }
        })
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        val recording = this.recording ?: return

        prepareSearchMenu(menu, recording.title, isNetworkAvailable)

        val toolbarMenu = nestedToolbar.menu
        if (isNetworkAvailable) {
            if (recording.isCompleted) {
                toolbarMenu.findItem(R.id.menu_record_remove)?.isVisible = true
                toolbarMenu.findItem(R.id.menu_play)?.isVisible = true
                toolbarMenu.findItem(R.id.menu_cast)?.isVisible = getCastSession(activity) != null
                toolbarMenu.findItem(R.id.menu_download)?.isVisible = isUnlocked

            } else if (recording.isScheduled && !recording.isRecording) {
                toolbarMenu.findItem(R.id.menu_record_cancel)?.isVisible = true
                toolbarMenu.findItem(R.id.menu_edit)?.isVisible = isUnlocked

            } else if (recording.isRecording) {
                toolbarMenu.findItem(R.id.menu_record_stop)?.isVisible = true
                toolbarMenu.findItem(R.id.menu_play)?.isVisible = true
                toolbarMenu.findItem(R.id.menu_cast)?.isVisible = getCastSession(activity) != null
                toolbarMenu.findItem(R.id.menu_edit)?.isVisible = isUnlocked

            } else if (recording.isFailed || recording.isFileMissing || recording.isMissed || recording.isAborted) {
                toolbarMenu.findItem(R.id.menu_record_remove)?.isVisible = true
                // Allow playing a failed recording which size is not zero
                if (recording.dataSize > 0) {
                    toolbarMenu.findItem(R.id.menu_play)?.isVisible = true
                    toolbarMenu.findItem(R.id.menu_cast)?.isVisible = getCastSession(activity) != null
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("id", shownDvrId)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.external_search_options_menu, menu)
        nestedToolbar.inflateMenu(R.menu.recording_details_toolbar_menu)
        nestedToolbar.setOnMenuItemClickListener { this.onOptionsItemSelected(it) }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // The recording might be null in case the viewmodel
        // has not yet loaded the recording for the given id
        val recording = this.recording ?: return super.onOptionsItemSelected(item)

        if (onMenuSelected(activity, item.itemId, recording.title)) {
            return true
        }
        when (item.itemId) {
            R.id.menu_play -> return menuUtils.handleMenuPlayRecording(recording.id)

            R.id.menu_cast -> return menuUtils.handleMenuCast("dvrId", recording.id)

            R.id.menu_download -> {
                DownloadRecordingManager(activity, recording.id)
                return true
            }

            R.id.menu_edit -> {
                val editIntent = Intent(activity, RecordingAddEditActivity::class.java)
                editIntent.putExtra("id", recording.id)
                editIntent.putExtra("type", "recording")
                activity.startActivity(editIntent)
                return true
            }

            R.id.menu_record_stop -> return menuUtils.handleMenuStopRecordingSelection(recording, this)

            R.id.menu_record_cancel -> return menuUtils.handleMenuCancelRecordingSelection(recording, this)

            R.id.menu_record_remove -> return menuUtils.handleMenuRemoveRecordingSelection(recording, this)

            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onRecordingRemoved() {
        if (!isDualPane) {
            activity.onBackPressed()
        } else {
            val detailsFragment = activity.supportFragmentManager.findFragmentById(R.id.details)
            if (detailsFragment != null) {
                activity.supportFragmentManager
                        .beginTransaction()
                        .remove(detailsFragment)
                        .commit()
            }
        }
    }

    override fun downloadRecording() {
        recording?.let {
            DownloadRecordingManager(activity, it.id)
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
