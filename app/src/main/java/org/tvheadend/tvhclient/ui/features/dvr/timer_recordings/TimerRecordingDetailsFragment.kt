package org.tvheadend.tvhclient.ui.features.dvr.timer_recordings

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
import org.tvheadend.tvhclient.databinding.TimerRecordingDetailsFragmentBinding
import org.tvheadend.tvhclient.domain.entity.TimerRecording
import org.tvheadend.tvhclient.ui.base.BaseFragment
import org.tvheadend.tvhclient.ui.common.onMenuSelected
import org.tvheadend.tvhclient.ui.common.prepareSearchMenu
import org.tvheadend.tvhclient.ui.features.dvr.RecordingAddEditActivity
import org.tvheadend.tvhclient.ui.features.dvr.RecordingRemovedCallback

class TimerRecordingDetailsFragment : BaseFragment(), RecordingRemovedCallback {

    private lateinit var nestedToolbar: Toolbar
    private lateinit var scrollView: ScrollView
    private lateinit var statusTextView: TextView

    private var recording: TimerRecording? = null
    var shownId: String = ""
    private lateinit var itemBinding: TimerRecordingDetailsFragmentBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        itemBinding = DataBindingUtil.inflate(inflater, R.layout.timer_recording_details_fragment, container, false)
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
        shownId = if (savedInstanceState != null) {
            savedInstanceState.getString("id") ?: ""
        } else {
            arguments?.getString("id") ?: ""
        }

        val viewModel = ViewModelProviders.of(activity).get(TimerRecordingViewModel::class.java)
        viewModel.getRecordingById(shownId).observe(viewLifecycleOwner, Observer { rec ->
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

        nestedToolbar.menu?.findItem(R.id.menu_edit)?.isVisible = true
        nestedToolbar.menu?.findItem(R.id.menu_record_remove)?.isVisible = true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("id", shownId)
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
        return when (item.itemId) {
            R.id.menu_edit -> {
                val intent = Intent(activity, RecordingAddEditActivity::class.java)
                intent.putExtra("type", "timer_recording")
                intent.putExtra("id", recording.id)
                activity.startActivity(intent)
                true
            }

            R.id.menu_record_remove -> menuUtils.handleMenuRemoveTimerRecordingSelection(recording, this)

            else -> super.onOptionsItemSelected(item)
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

    companion object {

        fun newInstance(id: String): TimerRecordingDetailsFragment {
            val f = TimerRecordingDetailsFragment()
            val args = Bundle()
            args.putString("id", id)
            f.arguments = args
            return f
        }
    }
}
