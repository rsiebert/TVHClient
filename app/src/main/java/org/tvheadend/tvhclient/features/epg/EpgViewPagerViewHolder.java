package org.tvheadend.tvhclient.features.epg;

import android.content.Context;
import android.preference.PreferenceManager;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.entity.ChannelSubset;
import org.tvheadend.tvhclient.data.entity.Program;
import org.tvheadend.tvhclient.utils.Constants;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

public class EpgViewPagerViewHolder extends RecyclerView.ViewHolder {

    private final GridLayoutManager gridLayoutManager;
    private final EpgProgramListRecyclerViewAdapter programListRecyclerViewAdapter;
    @BindView(R.id.program_list_recycler_view)
    protected RecyclerView programListRecyclerView;
    private Context context;

    EpgViewPagerViewHolder(View view) {
        super(view);
        ButterKnife.bind(this, view);

        context = view.getContext();

        int hoursToShow = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(context).getString("hours_of_epg_data_per_screen", Constants.EPG_DEFAULT_HOURS_VISIBLE));

        gridLayoutManager = new GridLayoutManager(context, hoursToShow * 60);

        programListRecyclerViewAdapter = new EpgProgramListRecyclerViewAdapter(context, null);
        programListRecyclerView.setLayoutManager(gridLayoutManager);
        programListRecyclerView.addItemDecoration(new DividerItemDecoration(context, LinearLayoutManager.VERTICAL));
        programListRecyclerView.setItemAnimator(new DefaultItemAnimator());
        programListRecyclerView.setAdapter(programListRecyclerViewAdapter);
    }

    public void bindData(Context context, ChannelSubset channel, List<Program> programs) {
        Timber.d("Creating horizontal recycler view adapter for " + (programs != null ? programs.size() : "null") + " programs on channel " + (channel != null ? channel.getName() : "null"));

        programListRecyclerViewAdapter.addItems(programs);
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
    }
}
