package org.tvheadend.tvhclient.ui.features.programs;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.databinding.ProgramDetailsFragmentBinding;
import org.tvheadend.tvhclient.domain.entity.Program;
import org.tvheadend.tvhclient.domain.entity.Recording;
import org.tvheadend.tvhclient.ui.base.BaseFragment;
import org.tvheadend.tvhclient.util.menu.PopupMenuUtil;
import org.tvheadend.tvhclient.util.menu.SearchMenuUtils;
import org.tvheadend.tvhclient.ui.features.dvr.RecordingAddEditActivity;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProviders;
import timber.log.Timber;

public class ProgramDetailsFragment extends BaseFragment {

    private Toolbar nestedToolbar;
    private ScrollView scrollView;
    private TextView statusTextView;

    private int eventId;
    private int channelId;
    private Program program;
    private Recording recording;
    private int programIdToBeEditedWhenBeingRecorded = 0;
    private ProgramDetailsFragmentBinding itemBinding;

    public static ProgramDetailsFragment newInstance(int eventId, int channelId) {
        ProgramDetailsFragment f = new ProgramDetailsFragment();
        Bundle args = new Bundle();
        args.putInt("eventId", eventId);
        args.putInt("channelId", channelId);
        f.setArguments(args);
        return f;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        itemBinding = DataBindingUtil.inflate(inflater, R.layout.program_details_fragment, container, false);
        View view = itemBinding.getRoot();

        nestedToolbar = view.findViewById(R.id.nested_toolbar);
        scrollView = view.findViewById(R.id.scrollview);
        statusTextView = view.findViewById(R.id.status);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        forceSingleScreenLayout();

        if (!isDualPane) {
            toolbarInterface.setTitle(getString(R.string.details));
            toolbarInterface.setSubtitle("");
        }
        if (savedInstanceState != null) {
            eventId = savedInstanceState.getInt("eventId", 0);
            channelId = savedInstanceState.getInt("channelId", 0);
        } else {
            Bundle bundle = getArguments();
            if (bundle != null) {
                eventId = bundle.getInt("eventId", 0);
                channelId = bundle.getInt("channelId", 0);
            }
        }

        ProgramViewModel viewModel = ViewModelProviders.of(activity).get(ProgramViewModel.class);
        program = viewModel.getProgramByIdSync(eventId);
        if (program != null) {
            Timber.d("Loaded details for program " + program.getTitle());
            itemBinding.setProgram(program);
            itemBinding.setHtspVersion(htspVersion);
            itemBinding.setIsProgramArtworkEnabled(isUnlocked && sharedPreferences.getBoolean("program_artwork_enabled", false));
            // The toolbar is hidden as a default to prevent pressing any icons if no recording
            // has been loaded yet. The toolbar is shown here because a recording was loaded
            nestedToolbar.setVisibility(View.VISIBLE);
            activity.invalidateOptionsMenu();
        } else {
            scrollView.setVisibility(View.GONE);
            statusTextView.setText(getString(R.string.error_loading_program_details));
            statusTextView.setVisibility(View.VISIBLE);
        }

        viewModel.getRecordingsByChannelId(channelId).observe(getViewLifecycleOwner(), recordings -> {
            Timber.d("Got recordings");
            if (recordings != null) {
                boolean recordingExists = false;
                for (Recording rec : recordings) {
                    // Show the edit recording screen of the scheduled recording
                    // in case the user has selected the record and edit menu item.
                    // Otherwise remember the recording so that the state can be updated
                    if (rec.getEventId() == programIdToBeEditedWhenBeingRecorded
                            && programIdToBeEditedWhenBeingRecorded > 0) {
                        programIdToBeEditedWhenBeingRecorded = 0;
                        Intent intent = new Intent(activity, RecordingAddEditActivity.class);
                        intent.putExtra("id", rec.getId());
                        intent.putExtra("type", "recording");
                        activity.startActivity(intent);
                        break;

                    } else if (program != null
                            && rec.getEventId() == program.getEventId()) {
                        Timber.d("Found recording for program " + program.getTitle());
                        recording = rec;
                        recordingExists = true;
                        break;
                    }
                }
                // If there is no recording for the program set the
                // recording to null so that the correct state is shown
                if (!recordingExists) {
                    recording = null;
                }
                // Update the state of the recording (if there is one)
                // and also the menu items in the nested toolbar
                program.setRecording(recording);
                itemBinding.setProgram(program);
                activity.invalidateOptionsMenu();
            }
        });
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        // Show or hide search menu items in the main toolbar
        PopupMenuUtil.prepareSearchMenu(menu, program.getTitle(), isNetworkAvailable);
        // Show or hide menus of the nested toolbar
        PopupMenuUtil.prepareMenu(activity, nestedToolbar.getMenu(), program, program.getRecording(), isNetworkAvailable, htspVersion, isUnlocked);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("eventId", eventId);
        outState.putInt("channelId", channelId);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.external_search_options_menu, menu);
        nestedToolbar.inflateMenu(R.menu.program_popup_and_toolbar_menu);
        nestedToolbar.setOnMenuItemClickListener(this::onOptionsItemSelected);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // The program might be null in case the viewmodel
        // has not yet loaded the program for the given id
        if (program == null) {
            return super.onOptionsItemSelected(item);
        }
        if (SearchMenuUtils.onMenuSelected(activity, item.getItemId(), program.getTitle(), program.getChannelId())) {
            return true;
        }
        switch (item.getItemId()) {
            case R.id.menu_record_stop:
                return menuUtils.handleMenuStopRecordingSelection(recording, null);

            case R.id.menu_record_cancel:
                return menuUtils.handleMenuCancelRecordingSelection(recording, null);

            case R.id.menu_record_remove:
                return menuUtils.handleMenuRemoveRecordingSelection(recording, null);

            case R.id.menu_record_once:
                return menuUtils.handleMenuRecordSelection(program.getEventId());

            case R.id.menu_record_once_and_edit:
                programIdToBeEditedWhenBeingRecorded = program.getEventId();
                return menuUtils.handleMenuRecordSelection(program.getEventId());

            case R.id.menu_record_once_custom_profile:
                return menuUtils.handleMenuCustomRecordSelection(program.getEventId(), program.getChannelId());

            case R.id.menu_record_series:
                return menuUtils.handleMenuSeriesRecordSelection(program.getTitle());

            case R.id.menu_play:
                return menuUtils.handleMenuPlayChannel(program.getChannelId());

            case R.id.menu_cast:
                return menuUtils.handleMenuCast("channelId", program.getChannelId());

            case R.id.menu_add_notification:
                return menuUtils.handleMenuAddNotificationSelection(program);

            default:
                return super.onOptionsItemSelected(item);
        }
    }
}

    