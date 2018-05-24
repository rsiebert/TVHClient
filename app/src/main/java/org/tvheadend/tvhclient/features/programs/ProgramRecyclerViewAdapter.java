package org.tvheadend.tvhclient.features.programs;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.entity.Program;
import org.tvheadend.tvhclient.data.entity.Recording;
import org.tvheadend.tvhclient.features.shared.UIUtils;
import org.tvheadend.tvhclient.features.shared.callbacks.RecyclerViewClickCallback;
import org.tvheadend.tvhclient.features.shared.listener.BottomReachedListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

public class ProgramRecyclerViewAdapter extends RecyclerView.Adapter<ProgramRecyclerViewAdapter.RecyclerViewHolder> implements Filterable {

    private final BottomReachedListener onBottomReachedListener;
    private final RecyclerViewClickCallback clickCallback;
    private List<Program> programList = new ArrayList<>();
    private List<Program> programListFiltered = new ArrayList<>();
    private SharedPreferences sharedPreferences;
    private Context context;

    ProgramRecyclerViewAdapter(Context context, RecyclerViewClickCallback clickCallback, BottomReachedListener onBottomReachedListener) {
        this.context = context;
        this.clickCallback = clickCallback;
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        this.onBottomReachedListener = onBottomReachedListener;
    }

    @NonNull
    @Override
    public RecyclerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new RecyclerViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.program_list_adapter, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerViewHolder holder, int position) {
        Program program = programListFiltered.get(position);
        holder.itemView.setTag(program);

        if (position == programList.size() - 1) {
            onBottomReachedListener.onBottomReached(position);
        }

        boolean showProgramSubtitle = sharedPreferences.getBoolean("program_subtitle_enabled", true);
        boolean showGenreColors = sharedPreferences.getBoolean("genre_colors_for_programs_enabled", false);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clickCallback.onClick(view, holder.getAdapterPosition());
            }
        });
        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                clickCallback.onLongClick(view, holder.getAdapterPosition());
                return true;
            }
        });

        if (program != null) {
            holder.titleTextView.setText(program.getTitle());

            Drawable drawable = UIUtils.getRecordingState(context, program.getRecording());
            holder.stateTextView.setVisibility(drawable != null ? View.VISIBLE : View.GONE);
            holder.stateTextView.setImageDrawable(drawable);

            holder.dateTextView.setText(UIUtils.getDate(context, program.getStart()));

            String time = UIUtils.getTimeText(context, program.getStart()) + " - " + UIUtils.getTimeText(context, program.getStop());
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

            holder.subtitleTextView.setVisibility(showProgramSubtitle && !TextUtils.isEmpty(program.getSubtitle()) ? View.VISIBLE : View.GONE);
            holder.subtitleTextView.setText(program.getSubtitle());

            holder.descriptionTextView.setVisibility(!TextUtils.isEmpty(program.getDescription()) ? View.VISIBLE : View.GONE);
            holder.descriptionTextView.setText(program.getDescription());

            holder.summaryTextView.setVisibility(!TextUtils.isEmpty(program.getSummary()) ? View.VISIBLE : View.GONE);
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

    void addItems(List<Program> list) {
        programList.clear();
        programListFiltered.clear();

        if (list != null) {
            programList = list;
            programListFiltered = list;
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return programListFiltered != null ? programListFiltered.size() : 0;
    }

    public Program getItem(int position) {
        if (programListFiltered.size() > position && position >= 0) {
            return programListFiltered.get(position);
        } else {
            return null;
        }
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {
                String charString = charSequence.toString();
                if (charString.isEmpty()) {
                    programListFiltered = programList;
                } else {
                    List<Program> filteredList = new ArrayList<>();
                    // Iterate over the available program. Use a copy on write
                    // array in case the program list changes during filtering.
                    for (Program program : new CopyOnWriteArrayList<>(programList)) {
                        // name match condition. this might differ depending on your requirement
                        // here we are looking for a channel name match
                        if (program.getTitle().toLowerCase().contains(charString.toLowerCase())) {
                            filteredList.add(program);
                        }
                    }
                    programListFiltered = filteredList;
                }

                FilterResults filterResults = new FilterResults();
                filterResults.values = programListFiltered;
                return filterResults;
            }

            @SuppressWarnings("unchecked")
            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                Timber.d("Done filtering");
                programListFiltered = (ArrayList<Program>) filterResults.values;
                notifyDataSetChanged();
            }
        };
    }

    /**
     * Whenever a recording changes in the database the list of available recordings are
     * saved in this recycler view. The previous list is cleared to avoid showing outdated
     * recording states. Each recording is checked if it belongs to the
     * currently shown program. If yes then its state is updated.
     *
     * @param recordings List of recordings
     */
    void addRecordings(List<Recording> recordings) {
        for (Recording recording : recordings) {
            for (int i = 0; i < programList.size(); i++) {
                Program program = programList.get(i);
                if (recording.getEventId() == program.getEventId()) {
                    program.setRecording(recording);
                    notifyItemChanged(i);
                    break;
                }
            }
        }
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
