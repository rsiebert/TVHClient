package org.tvheadend.tvhclient.features.epg;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ProgressBar;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.entity.ChannelSubset;
import org.tvheadend.tvhclient.data.entity.Program;
import org.tvheadend.tvhclient.features.programs.ProgramDetailsActivity;
import org.tvheadend.tvhclient.features.shared.callbacks.RecyclerViewClickCallback;

import butterknife.BindView;
import butterknife.ButterKnife;

public class EpgViewPagerViewHolder extends RecyclerView.ViewHolder implements RecyclerViewClickCallback {

    private final EpgProgramListRecyclerViewAdapter recyclerViewAdapter;
    private final FragmentActivity activity;
    private final long startTime;
    private final long endTime;
    private EpgViewModel viewModel;

    @BindView(R.id.program_list_recycler_view)
    protected RecyclerView recyclerView;
    @BindView(R.id.progress_bar)
    ProgressBar progressBar;

    EpgViewPagerViewHolder(FragmentActivity activity, View view, float pixelsPerMinute, long startTime, long endTime, RecyclerView.RecycledViewPool viewPool) {
        super(view);
        ButterKnife.bind(this, view);

        this.activity = activity;
        this.startTime = startTime;
        this.endTime = endTime;

        recyclerView.setLayoutManager(new CustomHorizontalLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false));
        recyclerView.addItemDecoration(new DividerItemDecoration(activity, LinearLayoutManager.HORIZONTAL));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setRecycledViewPool(viewPool);
        recyclerViewAdapter = new EpgProgramListRecyclerViewAdapter(activity, pixelsPerMinute, startTime, endTime, this);
        recyclerView.setAdapter(recyclerViewAdapter);

        recyclerView.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        viewModel = ViewModelProviders.of(activity).get(EpgViewModel.class);
    }

    @Override
    public void onClick(View view, int position) {
        Program program = recyclerViewAdapter.getItem(position);
        if (program == null) {
            return;
        }
        Intent intent = new Intent(activity, ProgramDetailsActivity.class);
        intent.putExtra("eventId", program.getEventId());
        intent.putExtra("channelId", program.getChannelId());
        activity.startActivity(intent);
    }

    @Override
    public void onLongClick(View view, int position) {
        final Program program = (Program) view.getTag();
        if (program == null) {
            return;
        }
        Fragment fragment = activity.getSupportFragmentManager().findFragmentById(R.id.main);
        if (fragment != null
                && fragment.isAdded()
                && fragment.isResumed()
                && fragment instanceof ProgramGuideFragment) {
            ((ProgramGuideFragment) fragment).showPopupMenu(view, program);
        }
    }

    public void bindData(ChannelSubset channelSubset) {
        viewModel.getProgramsByChannelAndBetweenTime(channelSubset.getId(), startTime, endTime).observe(activity, programs -> {
            if (programs != null) {
                recyclerViewAdapter.addItems(programs);
                recyclerView.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
            }
        });
        viewModel.getRecordingsByChannel(channelSubset.getId()).observe(activity, recordings -> {
            if (recordings != null) {
                recyclerViewAdapter.addRecordings(recordings);
            }
        });
    }
}
