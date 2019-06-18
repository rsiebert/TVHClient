package org.tvheadend.tvhclient.ui.features.programs

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import kotlinx.android.synthetic.main.details_fragment_header.*
import kotlinx.android.synthetic.main.program_details_fragment.*
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.databinding.ProgramDetailsFragmentBinding
import org.tvheadend.tvhclient.domain.entity.Program
import org.tvheadend.tvhclient.domain.entity.Recording
import org.tvheadend.tvhclient.ui.base.BaseFragment
import org.tvheadend.tvhclient.ui.common.*
import org.tvheadend.tvhclient.util.extensions.gone
import org.tvheadend.tvhclient.util.extensions.visible
import org.tvheadend.tvhclient.ui.features.dvr.RecordingAddEditActivity
import org.tvheadend.tvhclient.ui.features.notification.addNotificationForProgram
import timber.log.Timber

// TODO put program into the viewmodel
// TODO put event and channel Id into the viewmodel

class ProgramDetailsFragment : BaseFragment() {

    private lateinit var programViewModel: ProgramViewModel
    private var eventId: Int = 0
    private var channelId: Int = 0
    private var program: Program? = null
    private var recording: Recording? = null
    private var programIdToBeEditedWhenBeingRecorded = 0
    lateinit var itemBinding: ProgramDetailsFragmentBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        itemBinding = DataBindingUtil.inflate(inflater, R.layout.program_details_fragment, container, false)
        return itemBinding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        programViewModel = ViewModelProviders.of(activity!!).get(ProgramViewModel::class.java)

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

        program = programViewModel.getProgramByIdSync(eventId)
        if (program != null) {
            program?.let {
                Timber.d("Loaded details for program ${it.title}")
                itemBinding.program = it
                itemBinding.viewModel = programViewModel
                // The toolbar is hidden as a default to prevent pressing any icons if no recording
                // has been loaded yet. The toolbar is shown here because a recording was loaded
                nested_toolbar.visible()
                activity?.invalidateOptionsMenu()
            }
        } else {
            scrollview.gone()
            status.text = getString(R.string.error_loading_program_details)
            status.visible()
        }

        programViewModel.getRecordingsByChannelId(channelId).observe(viewLifecycleOwner, Observer { recordings ->
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
                        activity?.startActivity(intent)
                        break

                    } else if (program != null && rec.eventId == program?.eventId) {
                        Timber.d("Found recording for program ${program?.title}")
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
                itemBinding.program = program
                activity?.invalidateOptionsMenu()
            }
        })
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        val ctx = context ?: return
        preparePopupOrToolbarSearchMenu(menu, program?.title, isNetworkAvailable)
        preparePopupOrToolbarRecordingMenu(ctx, nested_toolbar.menu, program?.recording, isNetworkAvailable, htspVersion, isUnlocked)
        preparePopupOrToolbarMiscMenu(ctx, nested_toolbar.menu, program, isNetworkAvailable, isUnlocked)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("eventId", eventId)
        outState.putInt("channelId", channelId)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.external_search_options_menu, menu)
        nested_toolbar.inflateMenu(R.menu.program_popup_and_toolbar_menu)
        nested_toolbar.setOnMenuItemClickListener { this.onOptionsItemSelected(it) }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // The program might be null in case the view model
        // has not yet loaded the program for the given id
        val program = this.program ?: return super.onOptionsItemSelected(item)
        val ctx = context ?: return super.onOptionsItemSelected(item)
        when (item.itemId) {
            R.id.menu_stop_recording -> return showConfirmationToStopSelectedRecording(ctx, recording, null)
            R.id.menu_cancel_recording -> return showConfirmationToCancelSelectedRecording(ctx, recording, null)
            R.id.menu_remove_recording -> return showConfirmationToRemoveSelectedRecording(ctx, recording, null)
            R.id.menu_record_program -> return recordSelectedProgram(ctx, program.eventId, programViewModel.getRecordingProfile(), htspVersion)
            R.id.menu_record_program_and_edit -> {
                programIdToBeEditedWhenBeingRecorded = program.eventId
                return recordSelectedProgram(ctx, program.eventId, programViewModel.getRecordingProfile(), htspVersion)
            }
            R.id.menu_record_program_with_custom_profile -> return recordSelectedProgramWithCustomProfile(ctx, program.eventId, channelId, programViewModel.getRecordingProfileNames(), programViewModel.getRecordingProfile())
            R.id.menu_record_program_as_series_recording -> return recordSelectedProgramAsSeriesRecording(ctx, program.title, programViewModel.getRecordingProfile(), htspVersion)
            R.id.menu_play -> return playSelectedChannel(ctx, channelId)
            R.id.menu_cast -> return castSelectedChannel(ctx, channelId)

            R.id.menu_search_imdb -> return searchTitleOnImdbWebsite(ctx, program.title)
            R.id.menu_search_fileaffinity -> return searchTitleOnFileAffinityWebsite(ctx, program.title)
            R.id.menu_search_youtube -> return searchTitleOnYoutube(ctx, program.title)
            R.id.menu_search_google -> return searchTitleOnGoogle(ctx, program.title)
            R.id.menu_search_epg -> return searchTitleInTheLocalDatabase(ctx, program.title, program.channelId)

            R.id.menu_add_notification -> return addNotificationForProgram(ctx, program, programViewModel.getRecordingProfile())
            else -> return false
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

    