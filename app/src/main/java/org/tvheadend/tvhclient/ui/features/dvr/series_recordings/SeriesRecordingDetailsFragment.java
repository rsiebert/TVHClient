package org.tvheadend.tvhclient.ui.features.dvr.series_recordings;

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
import org.tvheadend.tvhclient.databinding.SeriesRecordingDetailsFragmentBinding;
import org.tvheadend.tvhclient.domain.entity.SeriesRecording;
import org.tvheadend.tvhclient.ui.base.BaseFragment;
import org.tvheadend.tvhclient.util.menu.PopupMenuUtil;
import org.tvheadend.tvhclient.util.menu.SearchMenuUtils;
import org.tvheadend.tvhclient.ui.features.dvr.RecordingAddEditActivity;
import org.tvheadend.tvhclient.ui.features.dvr.RecordingRemovedCallback;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

public class SeriesRecordingDetailsFragment extends BaseFragment implements RecordingRemovedCallback {

    private Toolbar nestedToolbar;
    private ScrollView scrollView;
    private TextView statusTextView;

    private SeriesRecording recording;
    private String id;
    private SeriesRecordingDetailsFragmentBinding itemBinding;

    public static SeriesRecordingDetailsFragment newInstance(String id) {
        SeriesRecordingDetailsFragment f = new SeriesRecordingDetailsFragment();
        Bundle args = new Bundle();
        args.putString("id", id);
        f.setArguments(args);
        return f;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        itemBinding = DataBindingUtil.inflate(inflater, R.layout.series_recording_details_fragment, container, false);
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
            id = savedInstanceState.getString("id");
        } else {
            Bundle bundle = getArguments();
            if (bundle != null) {
                id = bundle.getString("id");
            }
        }

        SeriesRecordingViewModel viewModel = ViewModelProviders.of(activity).get(SeriesRecordingViewModel.class);
        viewModel.getRecordingById(id).observe(getViewLifecycleOwner(), rec -> {
            if (rec != null) {
                recording = rec;
                itemBinding.setRecording(recording);
                itemBinding.setHtspVersion(htspVersion);
                itemBinding.setGmtOffset(serverStatus.getGmtoffset());
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

        if (nestedToolbar.getMenu() == null) {
            return;
        }
        menu = nestedToolbar.getMenu();
        menu.findItem(R.id.menu_edit).setVisible(true);
        menu.findItem(R.id.menu_record_remove).setVisible(true);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("id", id);
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
            case R.id.menu_edit:
                Intent intent = new Intent(activity, RecordingAddEditActivity.class);
                intent.putExtra("type", "series_recording");
                intent.putExtra("id", recording.getId());
                activity.startActivity(intent);
                return true;

            case R.id.menu_record_remove:
                return menuUtils.handleMenuRemoveSeriesRecordingSelection(recording, this);

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    String getShownId() {
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
}
