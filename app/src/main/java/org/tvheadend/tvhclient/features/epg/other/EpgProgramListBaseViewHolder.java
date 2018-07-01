package org.tvheadend.tvhclient.features.epg.other;

import android.content.Context;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.entity.Channel;
import org.tvheadend.tvhclient.data.entity.Program;
import org.tvheadend.tvhclient.features.shared.callbacks.RecyclerViewClickCallback;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

public class EpgProgramListBaseViewHolder extends RecyclerView.ViewHolder {

    @BindView(R.id.program_list_recycler_view)
    protected RecyclerView programItemRecyclerView;
    private EpgHorizontalProgramListRecyclerViewAdapter horizontalProgramListRecyclerViewAdapter;

    EpgProgramListBaseViewHolder(View view, RecyclerViewClickCallback clickCallback) {
        super(view);
        ButterKnife.bind(this, view);

        Context context = itemView.getContext();
        horizontalProgramListRecyclerViewAdapter = new EpgHorizontalProgramListRecyclerViewAdapter(context, clickCallback);
        programItemRecyclerView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
        programItemRecyclerView.addItemDecoration(new DividerItemDecoration(context, LinearLayoutManager.VERTICAL));
        programItemRecyclerView.setItemAnimator(new DefaultItemAnimator());
        programItemRecyclerView.setAdapter(horizontalProgramListRecyclerViewAdapter);
        programItemRecyclerView.setNestedScrollingEnabled(false);
        /*
        programItemRecyclerView.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
                Timber.d("programItemRecyclerView onInterceptTouchEvent");
                rv.getParent().requestDisallowInterceptTouchEvent(true);
                return false;
            }

            @Override
            public void onTouchEvent(RecyclerView rv, MotionEvent e) {
                Timber.d("programItemRecyclerView onTouchEvent");
            }

            @Override
            public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
                Timber.d("programItemRecyclerView onRequestDisallowInterceptTouchEvent");
            }
        });
        */
        //programItemRecyclerView.setNestedScrollingEnabled(false);
    }

    public void bindData(Context context, Channel channel, List<Program> programs, int columnCount, RecyclerViewClickCallback clickCallback) {
        Timber.d("Creating horizontal recycler view adapter for " + (programs != null ? programs.size() : "null") + " programs on channel " + (channel != null ? channel.getName() : "null"));

        horizontalProgramListRecyclerViewAdapter.addItems(programs);
/*
        GridLayoutManager gridLayoutManager = new GridLayoutManager(context, columnCount);
        gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (programs != null && programs.size() > position) {
                    Program program = programs.get(position);
                    if (program != null) {
                        int duration = (int) ((program.getStop() - program.getStart()) / 1000 / 60);
                        Timber.d("Setting span count in channel " + program.getChannelName() + " for program " + program.getTitle() + " to " + duration + " (minutes)");
                        return duration;
                    }
                }
                Timber.d("No program found, span count is 0");
                return 0;
            }
        });
*/

    }
}
