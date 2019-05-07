package org.tvheadend.tvhclient.ui.features.dvr.series_recordings

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import kotlinx.android.synthetic.main.details_fragment_header.*
import kotlinx.android.synthetic.main.series_recording_details_fragment.*
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.databinding.SeriesRecordingDetailsFragmentBinding
import org.tvheadend.tvhclient.domain.entity.SeriesRecording
import org.tvheadend.tvhclient.ui.base.BaseFragment
import org.tvheadend.tvhclient.ui.common.gone
import org.tvheadend.tvhclient.ui.common.onMenuSelected
import org.tvheadend.tvhclient.ui.common.prepareSearchMenu
import org.tvheadend.tvhclient.ui.common.visible
import org.tvheadend.tvhclient.ui.features.dvr.RecordingAddEditActivity
import org.tvheadend.tvhclient.ui.features.dvr.RecordingRemovedCallback

// TODO put shownId into the viewmodel

class SeriesRecordingDetailsFragment : BaseFragment(), RecordingRemovedCallback {

    private var recording: SeriesRecording? = null
    var shownId: String = ""
    private lateinit var itemBinding: SeriesRecordingDetailsFragmentBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        itemBinding = DataBindingUtil.inflate(inflater, R.layout.series_recording_details_fragment, container, false)
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
        shownId = savedInstanceState?.getString("id", "") ?: (arguments?.getString("id", "") ?: "")

        val viewModel = ViewModelProviders.of(activity!!).get(SeriesRecordingViewModel::class.java)
        viewModel.getRecordingById(shownId).observe(viewLifecycleOwner, Observer { rec ->
            if (rec != null) {
                recording = rec
                itemBinding.recording = recording
                itemBinding.htspVersion = serverStatus.htspVersion
                itemBinding.isDualPane = isDualPane
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
        val recording = this.recording ?: return
        prepareSearchMenu(menu, recording.title, isNetworkAvailable)

        nested_toolbar.menu.findItem(R.id.menu_edit)?.isVisible = true
        nested_toolbar.menu.findItem(R.id.menu_record_remove)?.isVisible = true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("id", shownId)
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
        return when (item.itemId) {
            R.id.menu_edit -> {
                val intent = Intent(activity, RecordingAddEditActivity::class.java)
                intent.putExtra("type", "series_recording")
                intent.putExtra("id", recording.id)
                activity?.startActivity(intent)
                true
            }
            R.id.menu_record_remove ->
                menuUtils.handleMenuRemoveSeriesRecordingSelection(recording, this)
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
