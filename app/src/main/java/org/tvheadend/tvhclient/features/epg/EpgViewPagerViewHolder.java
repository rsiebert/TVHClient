package org.tvheadend.tvhclient.features.epg;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.entity.ChannelSubset;
import org.tvheadend.tvhclient.data.entity.Program;
import org.tvheadend.tvhclient.data.entity.ProgramSubset;
import org.tvheadend.tvhclient.features.programs.ProgramDetailsActivity;
import org.tvheadend.tvhclient.features.shared.callbacks.RecyclerViewClickCallback;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

public class EpgViewPagerViewHolder extends RecyclerView.ViewHolder implements RecyclerViewClickCallback {

    private final EpgProgramListRecyclerViewAdapter recyclerViewAdapter;
    private final FragmentActivity activity;
    private final long startTime;
    private final long endTime;
    private final EpgViewModel viewModel;
    private final Handler handler;

    @BindView(R.id.program_list_recycler_view)
    protected RecyclerView recyclerView;
    @BindView(R.id.progress_bar)
    ProgressBar progressBar;
    @BindView(R.id.no_programs)
    TextView noProgramsTextView;

    EpgViewPagerViewHolder(FragmentActivity activity, View view, float pixelsPerMinute, long startTime, long endTime, RecyclerView.RecycledViewPool viewPool) {
        super(view);
        ButterKnife.bind(this, view);

        this.activity = activity;
        this.startTime = startTime;
        this.endTime = endTime;

        Context context = itemView.getContext();

        recyclerView.setLayoutManager(new CustomHorizontalLayoutManager(context));
        recyclerView.addItemDecoration(new DividerItemDecoration(context, LinearLayoutManager.HORIZONTAL));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setRecycledViewPool(viewPool);
        recyclerViewAdapter = new EpgProgramListRecyclerViewAdapter(pixelsPerMinute, startTime, endTime, this);
        recyclerView.setAdapter(recyclerViewAdapter);

        viewModel = ViewModelProviders.of(activity).get(EpgViewModel.class);

        HandlerThread handlerThread = new HandlerThread("EpgSyncService Handler Thread");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
    }

    @Override
    public void onClick(View view, int position) {
        ProgramSubset program = recyclerViewAdapter.getItem(position);
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
        if (fragment instanceof ProgramGuideFragment
                && fragment.isAdded()
                && fragment.isResumed()) {
            ((ProgramGuideFragment) fragment).showPopupMenu(view, program);
        }
    }

    public void bindData(final ChannelSubset channelSubset) {

        recyclerView.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        noProgramsTextView.setVisibility(View.GONE);

        handler.post(() -> {
            List<ProgramSubset> programs = viewModel.getProgramsByChannelAndBetweenTimeSync(channelSubset.getId(), startTime, endTime);
            if (programs != null && programs.size() > 0) {
                Timber.d("Loaded " + programs.size() + " programs for channel " + channelSubset.getName());
                activity.runOnUiThread(() -> {
                    recyclerViewAdapter.addItems(programs);
                    recyclerView.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.GONE);
                    noProgramsTextView.setVisibility(View.GONE);
                });
            } else {
                Timber.d("Loaded no programs for channel " + channelSubset.getName());
                activity.runOnUiThread(() -> {
                    recyclerView.setVisibility(View.GONE);
                    progressBar.setVisibility(View.GONE);
                    noProgramsTextView.setVisibility(View.VISIBLE);
                });
            }
        });

        viewModel.getRecordingsByChannel(channelSubset.getId()).observe(activity, recordings -> {
            if (recordings != null) {
                recyclerViewAdapter.addRecordings(recordings);
            }
        });
    }
}
