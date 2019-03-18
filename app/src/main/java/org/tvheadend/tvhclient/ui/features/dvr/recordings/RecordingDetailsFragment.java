package org.tvheadend.tvhclient.ui.features.dvr.recordings;

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
import org.tvheadend.tvhclient.databinding.RecordingDetailsFragmentBinding;
import org.tvheadend.tvhclient.domain.entity.Recording;
import org.tvheadend.tvhclient.ui.base.BaseFragment;
import org.tvheadend.tvhclient.ui.common.CastUtils;
import org.tvheadend.tvhclient.ui.common.PopupMenuUtil;
import org.tvheadend.tvhclient.ui.common.SearchMenuUtils;
import org.tvheadend.tvhclient.ui.features.download.DownloadPermissionGrantedInterface;
import org.tvheadend.tvhclient.ui.features.dvr.RecordingAddEditActivity;
import org.tvheadend.tvhclient.ui.features.dvr.RecordingRemovedCallback;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

public class RecordingDetailsFragment extends BaseFragment implements RecordingRemovedCallback, DownloadPermissionGrantedInterface {

    private Toolbar nestedToolbar;
    private ScrollView scrollView;
    private TextView statusTextView;

    private Recording recording = null;
    private int id;
    private RecordingDetailsFragmentBinding itemBinding;

    public static RecordingDetailsFragment newInstance(int dvrId) {
        RecordingDetailsFragment f = new RecordingDetailsFragment();
        Bundle args = new Bundle();
        args.putInt("id", dvrId);
        f.setArguments(args);
        return f;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        itemBinding = DataBindingUtil.inflate(inflater, R.layout.recording_details_fragment, container, false);
        View view = itemBinding.getRoot();

        nestedToolbar = view.findViewById(R.id.nested_toolbar);
        scrollView = view.findViewById(R.id.scrollview);
        statusTextView = view.findViewById(R.id.status);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (!isDualPane) {
            toolbarInterface.setTitle(getString(R.string.details));
            toolbarInterface.setSubtitle("");
        }

        // Get the recording id after an orientation change has occurred
        // or when the fragment is shown for the first time
        if (savedInstanceState != null) {
            id = savedInstanceState.getInt("id", 0);
        } else {
            Bundle bundle = getArguments();
            if (bundle != null) {
                id = bundle.getInt("id", 0);
            }
        }

        RecordingViewModel viewModel = ViewModelProviders.of(activity).get(RecordingViewModel.class);
        viewModel.getRecordingById(id).observe(getViewLifecycleOwner(), rec -> {
            if (rec != null) {
                recording = rec;
                itemBinding.setRecording(recording);
                itemBinding.setHtspVersion(htspVersion);
                // The toolbar is hidden as a default to prevent pressing any icons if no recording
                // has been loaded yet. The toolbar is shown here because a recording was loaded
                nestedToolbar.setVisibility(View.VISIBLE);
                activity.invalidateOptionsMenu();
            } else {
                scrollView.setVisibility(View.GONE);
                statusTextView.setText(getString(R.string.error_loading_recording_details));
                statusTextView.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        if (recording == null) {
            return;
        }

        PopupMenuUtil.prepareSearchMenu(menu, recording.getTitle(), isNetworkAvailable);

        menu = nestedToolbar.getMenu();
        if (isNetworkAvailable) {
            if (recording.isCompleted()) {
                menu.findItem(R.id.menu_record_remove).setVisible(true);
                menu.findItem(R.id.menu_play).setVisible(true);
                menu.findItem(R.id.menu_cast).setVisible(CastUtils.getCastSession(activity) != null);
                menu.findItem(R.id.menu_download).setVisible(isUnlocked);

            } else if (recording.isScheduled() && !recording.isRecording()) {
                menu.findItem(R.id.menu_record_cancel).setVisible(true);
                menu.findItem(R.id.menu_edit).setVisible(isUnlocked);

            } else if (recording.isRecording()) {
                menu.findItem(R.id.menu_record_stop).setVisible(true);
                menu.findItem(R.id.menu_play).setVisible(true);
                menu.findItem(R.id.menu_cast).setVisible(CastUtils.getCastSession(activity) != null);
                menu.findItem(R.id.menu_edit).setVisible(isUnlocked);

            } else if (recording.isFailed() || recording.isFileMissing() || recording.isMissed() || recording.isAborted()) {
                menu.findItem(R.id.menu_record_remove).setVisible(true);
                // Allow playing a failed recording which size is not zero
                if (recording.getDataSize() > 0) {
                    menu.findItem(R.id.menu_play).setVisible(true);
                    menu.findItem(R.id.menu_cast).setVisible(CastUtils.getCastSession(activity) != null);
                }
            }
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("id", id);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.external_search_options_menu, menu);
        nestedToolbar.inflateMenu(R.menu.recording_details_toolbar_menu);
        nestedToolbar.setOnMenuItemClickListener(this::onOptionsItemSelected);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // The recording might be null in case the viewmodel
        // has not yet loaded the recording for the given id
        if (recording == null) {
            return super.onOptionsItemSelected(item);
        }
        if (SearchMenuUtils.onMenuSelected(activity, item.getItemId(), recording.getTitle())) {
            return true;
        }
        switch (item.getItemId()) {
            case R.id.menu_play:
                return menuUtils.handleMenuPlayRecording(recording.getId());

            case R.id.menu_cast:
                return menuUtils.handleMenuCast("dvrId", recording.getId());

            case R.id.menu_download:
                return menuUtils.handleMenuDownloadSelection(recording.getId());

            case R.id.menu_edit:
                Intent editIntent = new Intent(activity, RecordingAddEditActivity.class);
                editIntent.putExtra("id", recording.getId());
                editIntent.putExtra("type", "recording");
                activity.startActivity(editIntent);
                return true;

            case R.id.menu_record_stop:
                return menuUtils.handleMenuStopRecordingSelection(recording, this);

            case R.id.menu_record_cancel:
                return menuUtils.handleMenuCancelRecordingSelection(recording, this);

            case R.id.menu_record_remove:
                return menuUtils.handleMenuRemoveRecordingSelection(recording, this);

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    int getShownDvrId() {
        return id;
    }

    @Override
    public void onRecordingRemoved() {
        if (!isDualPane) {
            activity.onBackPressed();
        } else {
            Fragment detailsFragment = activity.getSupportFragmentManager().findFragmentById(R.id.details);
            if (detailsFragment != null) {
                activity.getSupportFragmentManager()
                        .beginTransaction()
                        .remove(detailsFragment)
                        .commit();
            }
        }
    }

    @Override
    public void downloadRecording() {
        menuUtils.handleMenuDownloadSelection(recording.getId());
    }
}
