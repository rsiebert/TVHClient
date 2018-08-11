package org.tvheadend.tvhclient.features.epg;

import android.content.Intent;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.entity.Program;
import org.tvheadend.tvhclient.data.entity.Recording;
import org.tvheadend.tvhclient.features.programs.ProgramDetailsActivity;
import org.tvheadend.tvhclient.features.shared.callbacks.RecyclerViewClickCallback;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class EpgViewPagerViewHolder extends RecyclerView.ViewHolder implements RecyclerViewClickCallback {

    private final EpgProgramListRecyclerViewAdapter programListRecyclerViewAdapter;
    private final FragmentActivity activity;
    @BindView(R.id.program_list_recycler_view)
    protected RecyclerView programListRecyclerView;

    EpgViewPagerViewHolder(FragmentActivity activity, View view, float pixelsPerMinute, long fragmentStartTime, long fragmentStopTime, RecyclerView.RecycledViewPool viewPool) {
        super(view);
        ButterKnife.bind(this, view);

        this.activity = activity;
        programListRecyclerView.setLayoutManager(new CustomHorizontalLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false));
        programListRecyclerView.addItemDecoration(new DividerItemDecoration(activity, LinearLayoutManager.HORIZONTAL));
        programListRecyclerView.setItemAnimator(new DefaultItemAnimator());
        programListRecyclerViewAdapter = new EpgProgramListRecyclerViewAdapter(activity, pixelsPerMinute, fragmentStartTime, fragmentStopTime, this);
        programListRecyclerView.setAdapter(programListRecyclerViewAdapter);
        programListRecyclerView.setRecycledViewPool(viewPool);
    }

    public void bindData(List<Program> programs, List<Recording> recordings) {
        programListRecyclerViewAdapter.addItems(programs);
        programListRecyclerViewAdapter.addRecordings(recordings);
    }

    @Override
    public void onClick(View view, int position) {
        Program program = programListRecyclerViewAdapter.getItem(position);
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
}
