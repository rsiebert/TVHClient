package org.tvheadend.tvhclient.ui.features.programs

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
import org.tvheadend.tvhclient.databinding.ProgramDetailsFragmentBinding
import org.tvheadend.tvhclient.domain.entity.Program
import org.tvheadend.tvhclient.domain.entity.Recording
import org.tvheadend.tvhclient.ui.base.BaseFragment
import org.tvheadend.tvhclient.ui.common.onMenuSelected
import org.tvheadend.tvhclient.ui.common.prepareMenu
import org.tvheadend.tvhclient.ui.common.prepareSearchMenu
import org.tvheadend.tvhclient.ui.features.dvr.RecordingAddEditActivity
import timber.log.Timber

class ProgramDetailsFragment : BaseFragment() {

    private lateinit var nestedToolbar: Toolbar
    private lateinit var scrollView: ScrollView
    lateinit var statusTextView: TextView

    private var eventId: Int = 0
    private var channelId: Int = 0
    private var program: Program? = null
    private var recording: Recording? = null
    private var programIdToBeEditedWhenBeingRecorded = 0
    lateinit var itemBinding: ProgramDetailsFragmentBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        itemBinding = DataBindingUtil.inflate<ProgramDetailsFragmentBinding>(inflater, R.layout.program_details_fragment, container, false)
        val view = itemBinding.getRoot()

        nestedToolbar = view.findViewById(R.id.nested_toolbar)
        scrollView = view.findViewById(R.id.scrollview)
        statusTextView = view.findViewById(R.id.status)
        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        forceSingleScreenLayout()

        if (!isDualPane) {
            toolbarInterface.setTitle(getString(R.string.details))
            toolbarInterface.setSubtitle("")
        }
        if (savedInstanceState != null) {
            eventId = savedInstanceState.getInt("eventId", 0)
            channelId = savedInstanceState.getInt("channelId", 0)
        } else {
            val bundle = arguments
            if (bundle != null) {
                eventId = bundle.getInt("eventId", 0)
                channelId = bundle.getInt("channelId", 0)
            }
        }

        val viewModel = ViewModelProviders.of(activity).get(ProgramViewModel::class.java)
        program = viewModel.getProgramByIdSync(eventId)
        if (program != null) {
            program?.let {
                Timber.d("Loaded details for program ${it.title}")
                itemBinding.setProgram(it)
                itemBinding.setHtspVersion(htspVersion)
                itemBinding.setIsProgramArtworkEnabled(isUnlocked && sharedPreferences.getBoolean("program_artwork_enabled", false))
                // The toolbar is hidden as a default to prevent pressing any icons if no recording
                // has been loaded yet. The toolbar is shown here because a recording was loaded
                nestedToolbar.visibility = View.VISIBLE
                activity.invalidateOptionsMenu()
            }
        } else {
            scrollView.visibility = View.GONE
            statusTextView.text = getString(R.string.error_loading_program_details)
            statusTextView.visibility = View.VISIBLE
        }

        viewModel.getRecordingsByChannelId(channelId).observe(viewLifecycleOwner, Observer { recordings ->
            Timber.d("Got recordings")
            if (recordings != null) {
                var recordingExists = false
                for (rec in recordings) {
                    // Show the edit recording screen of the scheduled recording
                    // in case the user has selected the record and edit menu item.
                    // Otherwise remember the recording so that the state can be updated
                    if (rec.eventId == programIdToBeEditedWhenBeingRecorded && programIdToBeEditedWhenBeingRecorded > 0) {
                        programIdToBeEditedWhenBeingRecorded = 0
                        val intent = Intent(activity, RecordingAddEditActivity::class.java)
                        intent.putExtra("id", rec.id)
                        intent.putExtra("type", "recording")
                        activity.startActivity(intent)
                        break

                    } else if (program != null && rec.eventId == program?.eventId) {
                        Timber.d("Found recording for program " + program?.title)
                        recording = rec
                        recordingExists = true
                        break
                    }
                }
                // If there is no recording for the program set the
                // recording to null so that the correct state is shown
                if (!recordingExists) {
                    recording = null
                }
                // Update the state of the recording (if there is one)
                // and also the menu items in the nested toolbar
                program?.recording = recording
                itemBinding.setProgram(program)
                activity.invalidateOptionsMenu()
            }
        })
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        // Show or hide search menu items in the main toolbar
        prepareSearchMenu(menu, program?.title, isNetworkAvailable)
        // Show or hide menus of the nested toolbar
        prepareMenu(activity, nestedToolbar.menu, program, program?.recording, isNetworkAvailable, htspVersion, isUnlocked)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("eventId", eventId)
        outState.putInt("channelId", channelId)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.external_search_options_menu, menu)
        nestedToolbar.inflateMenu(R.menu.program_popup_and_toolbar_menu)
        nestedToolbar.setOnMenuItemClickListener { this.onOptionsItemSelected(it) }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // The program might be null in case the view model
        // has not yet loaded the program for the given id
        val program = this.program ?: return super.onOptionsItemSelected(item)

        if (onMenuSelected(activity, item.itemId, program.title, program.channelId)) {
            return true
        }
        when (item.itemId) {
            R.id.menu_record_stop -> return menuUtils.handleMenuStopRecordingSelection(recording, null)

            R.id.menu_record_cancel -> return menuUtils.handleMenuCancelRecordingSelection(recording, null)

            R.id.menu_record_remove -> return menuUtils.handleMenuRemoveRecordingSelection(recording, null)

            R.id.menu_record_once -> return menuUtils.handleMenuRecordSelection(program.eventId)

            R.id.menu_record_once_and_edit -> {
                programIdToBeEditedWhenBeingRecorded = program.eventId
                return menuUtils.handleMenuRecordSelection(program.eventId)
            }

            R.id.menu_record_once_custom_profile -> return menuUtils.handleMenuCustomRecordSelection(program.eventId, program.channelId)

            R.id.menu_record_series -> return menuUtils.handleMenuSeriesRecordSelection(program.title)

            R.id.menu_play -> return menuUtils.handleMenuPlayChannel(program.channelId)

            R.id.menu_cast -> return menuUtils.handleMenuCast("channelId", program.channelId)

            R.id.menu_add_notification -> return menuUtils.handleMenuAddNotificationSelection(program)

            else -> return super.onOptionsItemSelected(item)
        }
    }

    companion object {

        fun newInstance(eventId: Int, channelId: Int): ProgramDetailsFragment {
            val f = ProgramDetailsFragment()
            val args = Bundle()
            args.putInt("eventId", eventId)
            args.putInt("channelId", channelId)
            f.arguments = args
            return f
        }
    }
}

    