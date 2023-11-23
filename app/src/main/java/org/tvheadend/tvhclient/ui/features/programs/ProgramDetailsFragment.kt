package org.tvheadend.tvhclient.ui.features.programs

import android.os.Bundle
import android.view.*
import androidx.lifecycle.ViewModelProvider
import org.tvheadend.data.entity.Program
import org.tvheadend.data.entity.Recording
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.databinding.ProgramDetailsFragmentBinding
import org.tvheadend.tvhclient.ui.base.BaseFragment
import org.tvheadend.tvhclient.ui.common.*
import org.tvheadend.tvhclient.ui.common.interfaces.ClearSearchResultsOrPopBackStackInterface
import org.tvheadend.tvhclient.ui.common.interfaces.LayoutControlInterface
import org.tvheadend.tvhclient.util.extensions.gone
import org.tvheadend.tvhclient.util.extensions.visible
import timber.log.Timber

class ProgramDetailsFragment : BaseFragment(), ClearSearchResultsOrPopBackStackInterface {

    private lateinit var programViewModel: ProgramViewModel
    private var program: Program? = null
    private var recording: Recording? = null
    private var programIdToBeEditedWhenBeingRecorded = 0
    private lateinit var binding: ProgramDetailsFragmentBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        //binding = DataBindingUtil.inflate(inflater, R.layout.program_details_fragment, container, false)
        binding = ProgramDetailsFragmentBinding.inflate(inflater, container, false)
        binding.layout.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                binding.layout.viewTreeObserver.removeOnPreDrawListener(this)
                binding.viewWidth = binding.layout.measuredWidth
                Timber.d("Width of program details layout is %s", binding.layout.measuredWidth)
                return true
            }
        })
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        programViewModel = ViewModelProvider(requireActivity())[ProgramViewModel::class.java]

        if (activity is LayoutControlInterface) {
            (activity as LayoutControlInterface).forceSingleScreenLayout()
        }

        toolbarInterface.setTitle(getString(R.string.details))
        toolbarInterface.setSubtitle("")

        // In case the fragment was called from the program activity, program list or any adapter
        arguments?.let {
            programViewModel.eventIdLiveData.value = it.getInt("eventId", 0)
            programViewModel.channelIdLiveData.value = it.getInt("channelId", 0)
        }

        Timber.d("Observing program")
        programViewModel.program.observe(viewLifecycleOwner) {
            Timber.d("View model returned a program")
            program = it
            showProgramDetails()
        }

        Timber.d("Observing recordings")
        programViewModel.recordings.observe(viewLifecycleOwner) { recordings ->
            if (recordings != null) {
                Timber.d("View model returned ${recordings.size} recordings")
                showRecordingStatusOfProgram(recordings)
            }
        }
    }

    private fun showProgramDetails() {
        if (program != null) {
            binding.program = program
            binding.viewModel = programViewModel
            // The toolbar is hidden as a default to prevent pressing any icons if no recording
            // has been loaded yet. The toolbar is shown here because a recording was loaded
            binding.nestedToolbar.visible()
            activity?.invalidateOptionsMenu()
        } else {
            binding.scrollview.gone()
            binding.status.text = getString(R.string.error_loading_program_details)
            binding.status.visible()
        }
    }

    private fun showRecordingStatusOfProgram(recordings: List<Recording>) {
        var recordingExists = false
        for (rec in recordings) {
            // Show the edit recording screen of the scheduled recording
            // in case the user has selected the record and edit menu item.
            // Otherwise remember the recording so that the state can be updated
            if (rec.eventId == programIdToBeEditedWhenBeingRecorded && programIdToBeEditedWhenBeingRecorded > 0) {
                programIdToBeEditedWhenBeingRecorded = 0
                editSelectedRecording(requireActivity(), rec.id)
                break

            } else {
                val p = program
                if (p != null && rec.eventId == p.eventId) {
                    Timber.d("Found recording for program ${p.title}")
                    recording = rec
                    recordingExists = true
                    break
                }
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
        binding.program = program
        activity?.invalidateOptionsMenu()
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        val ctx = context ?: return

        // Hide the play button in the main toolbar, its already available in the nested toolbar
        menu.findItem(R.id.menu_play)?.isVisible = false

        preparePopupOrToolbarSearchMenu(menu, program?.title, isConnectionToServerAvailable)
        preparePopupOrToolbarRecordingMenu(ctx, binding.nestedToolbar.menu, program?.recording, isConnectionToServerAvailable, htspVersion, isUnlocked)
        preparePopupOrToolbarMiscMenu(ctx, binding.nestedToolbar.menu, program, isConnectionToServerAvailable, isUnlocked)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.external_search_options_menu, menu)
        binding.nestedToolbar.inflateMenu(R.menu.program_popup_and_toolbar_menu)
        binding.nestedToolbar.setOnMenuItemClickListener { this.onOptionsItemSelected(it) }
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
            R.id.menu_record_program_with_custom_profile -> return recordSelectedProgramWithCustomProfile(ctx, program.eventId, program.channelId, programViewModel.getRecordingProfileNames(), programViewModel.getRecordingProfile())
            R.id.menu_record_program_as_series_recording -> return recordSelectedProgramAsSeriesRecording(ctx, program.title, program.channelId, programViewModel.getRecordingProfile(), htspVersion)
            R.id.menu_play -> return playSelectedChannel(ctx, program.channelId, isUnlocked)
            R.id.menu_cast -> return castSelectedChannel(ctx, program.channelId)

            R.id.menu_search_imdb -> return searchTitleOnImdbWebsite(ctx, program.title)
            R.id.menu_search_fileaffinity -> return searchTitleOnFileAffinityWebsite(ctx, program.title)
            R.id.menu_search_youtube -> return searchTitleOnYoutube(ctx, program.title)
            R.id.menu_search_google -> return searchTitleOnGoogle(ctx, program.title)
            R.id.menu_search_epg -> return searchTitleInTheLocalDatabase(requireActivity(), baseViewModel, program.title, program.channelId)

            R.id.menu_add_notification -> return addNotificationProgramIsAboutToStart(ctx, program, programViewModel.getRecordingProfile())
            else -> return false
        }
    }

    companion object {

        fun newInstance(eventId: Int = 0, channelId: Int = 0): ProgramDetailsFragment {
            val f = ProgramDetailsFragment()
            val args = Bundle()
            args.putInt("eventId", eventId)
            args.putInt("channelId", channelId)
            f.arguments = args
            return f
        }
    }
}

    