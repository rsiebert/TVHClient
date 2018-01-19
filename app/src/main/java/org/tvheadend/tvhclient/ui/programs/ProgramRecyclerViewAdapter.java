package org.tvheadend.tvhclient.ui.programs;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.DataStorage;
import org.tvheadend.tvhclient.data.model.Program;
import org.tvheadend.tvhclient.ui.common.RecyclerViewClickCallback;
import org.tvheadend.tvhclient.utils.UIUtils;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ProgramRecyclerViewAdapter extends RecyclerView.Adapter<ProgramRecyclerViewAdapter.RecyclerViewHolder> {

    private List<Program> programList;
    private RecyclerViewClickCallback clickCallback;
    private int htspVersion;
    private SharedPreferences sharedPreferences;
    private Context context;
    private int selectedPosition = 0;

    ProgramRecyclerViewAdapter(Context context, List<Program> programList, RecyclerViewClickCallback clickCallback) {
        this.context = context;
        this.htspVersion = DataStorage.getInstance().getProtocolVersion();
        this.programList = programList;
        this.clickCallback = clickCallback;
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    @Override
    public RecyclerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new RecyclerViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.program_list_adapter, parent, false));
    }

    @Override
    public void onBindViewHolder(RecyclerViewHolder holder, int position) {
        Program program = programList.get(position);

        holder.itemView.setTag(program);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clickCallback.onClick(view, holder.getAdapterPosition());
            }
        });
        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                return clickCallback.onLongClick(view);
            }
        });

        boolean showProgramSubtitle = sharedPreferences.getBoolean("showProgramSubtitlePref", true);
        boolean showGenreColors = sharedPreferences.getBoolean("showGenreColorsProgramsPref", false);

        if (program != null) {
            holder.titleTextView.setText(program.getTitle());

            Drawable drawable = UIUtils.getRecordingState(context, program.getDvrId());
            holder.stateTextView.setVisibility(drawable != null ? View.VISIBLE : View.GONE);
            holder.stateTextView.setImageDrawable(drawable);

            holder.dateTextView.setText(UIUtils.getDate(context, program.getStart()));

            String time = UIUtils.getTime(context, program.getStart()) + " - " + UIUtils.getTime(context, program.getStop());
            holder.timeTextView.setText(time);

            String durationTime = context.getString(R.string.minutes, (int) ((program.getStop() - program.getStart()) / 1000 / 60));
            holder.durationTextView.setText(durationTime);

            String progressText = UIUtils.getProgressText(context, program.getStart(), program.getStop());
            holder.progressTextView.setVisibility(!TextUtils.isEmpty(progressText) ? View.VISIBLE : View.GONE);
            holder.progressTextView.setText(progressText);

            String contentType = UIUtils.getContentTypeText(context, program.getContentType());
            holder.contentTypeTextView.setVisibility(!TextUtils.isEmpty(contentType) ? View.VISIBLE : View.GONE);
            holder.contentTypeTextView.setText(contentType);

            String seriesInfo = UIUtils.getSeriesInfo(context, program);
            holder.seriesInfoTextView.setVisibility(!TextUtils.isEmpty(seriesInfo) ? View.VISIBLE : View.GONE);
            holder.seriesInfoTextView.setText(seriesInfo);

            holder.subtitleTextView.setVisibility(showProgramSubtitle && !TextUtils.isEmpty(program.getSubtitle())? View.VISIBLE : View.GONE);
            holder.subtitleTextView.setText(program.getSubtitle());

            holder.descriptionTextView.setVisibility(!TextUtils.isEmpty(program.getDescription())? View.VISIBLE : View.GONE);
            holder.descriptionTextView.setText(program.getDescription());

            holder.summaryTextView.setVisibility(!TextUtils.isEmpty(program.getSummary())? View.VISIBLE : View.GONE);
            holder.summaryTextView.setText(program.getSummary());

            if (showGenreColors) {
                int color = UIUtils.getGenreColor(context, program.getContentType(), 0);
                holder.genreTextView.setBackgroundColor(color);
                holder.genreTextView.setVisibility(View.VISIBLE);
            } else {
                holder.genreTextView.setVisibility(View.GONE);
            }
        }
    }

    void addItems(List<Program> programList) {
        this.programList = programList;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return programList.size();
    }

    public Program getItem(int position) {
        return programList.get(position);
    }

    static class RecyclerViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.title)
        TextView titleTextView;
        @BindView(R.id.time)
        TextView timeTextView;
        @BindView(R.id.date)
        TextView dateTextView;
        @BindView(R.id.duration)
        TextView durationTextView;
        @BindView(R.id.progress)
        TextView progressTextView;
        @BindView(R.id.summary)
        TextView summaryTextView;
        @BindView(R.id.description)
        TextView descriptionTextView;
        @BindView(R.id.series_info)
        TextView seriesInfoTextView;
        @BindView(R.id.subtitle)
        TextView subtitleTextView;
        @BindView(R.id.content_type)
        TextView contentTypeTextView;
        @BindView(R.id.state)
        ImageView stateTextView;
        @BindView(R.id.genre)
        TextView genreTextView;

        RecyclerViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }
}
