package org.tvheadend.tvhclient.ui.features.epg;

import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.domain.entity.EpgChannel;
import org.tvheadend.tvhclient.domain.entity.EpgProgram;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

public class EpgViewPagerViewHolder extends RecyclerView.ViewHolder {

    private final EpgProgramListRecyclerViewAdapter recyclerViewAdapter;
    private final FragmentActivity activity;
    private final long startTime;
    private final long endTime;
    private final EpgViewModel viewModel;
    private final ScheduledExecutorService execService = Executors.newScheduledThreadPool(10);

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

        recyclerView.setLayoutManager(new CustomHorizontalLayoutManager(view.getContext()));
        recyclerView.addItemDecoration(new DividerItemDecoration(view.getContext(), LinearLayoutManager.HORIZONTAL));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setRecycledViewPool(viewPool);
        recyclerViewAdapter = new EpgProgramListRecyclerViewAdapter(pixelsPerMinute, startTime, endTime);
        recyclerView.setAdapter(recyclerViewAdapter);

        viewModel = ViewModelProviders.of(activity).get(EpgViewModel.class);
    }

    void bindData(final EpgChannel epgChannel) {

        recyclerView.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        noProgramsTextView.setVisibility(View.GONE);

        execService.execute(() -> {
            List<EpgProgram> programs = viewModel.getProgramsByChannelAndBetweenTimeSync(epgChannel.getId(), startTime, endTime);
            if (programs.size() > 0) {
                Timber.d("Loaded " + programs.size() + " programs for channel " + epgChannel.getName());
                activity.runOnUiThread(() -> {
                    recyclerViewAdapter.addItems(programs);
                    recyclerView.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.GONE);
                    noProgramsTextView.setVisibility(View.GONE);
                });
            } else {
                Timber.d("Loaded no programs for channel " + epgChannel.getName());
                activity.runOnUiThread(() -> {
                    recyclerView.setVisibility(View.GONE);
                    progressBar.setVisibility(View.GONE);
                    noProgramsTextView.setVisibility(View.VISIBLE);
                });
            }
        });

        viewModel.getRecordingsByChannel(epgChannel.getId()).observe(activity, recordings -> {
            if (recordings != null) {
                recyclerViewAdapter.addRecordings(recordings);
            }
        });
    }
}
