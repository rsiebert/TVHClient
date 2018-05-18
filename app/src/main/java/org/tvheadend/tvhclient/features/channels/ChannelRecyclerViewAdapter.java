package org.tvheadend.tvhclient.features.channels;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.entity.Channel;
import org.tvheadend.tvhclient.data.entity.Recording;
import org.tvheadend.tvhclient.features.shared.UIUtils;
import org.tvheadend.tvhclient.features.shared.callbacks.ChannelClickCallback;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ChannelRecyclerViewAdapter extends RecyclerView.Adapter<ChannelRecyclerViewAdapter.RecyclerViewHolder> implements Filterable {

    private final ChannelClickCallback channelClickCallback;
    private List<Recording> recordingList = new ArrayList<>();
    private List<Channel> channelList = new ArrayList<>();
    private List<Channel> channelListFiltered = new ArrayList<>();
    private SharedPreferences sharedPreferences;
    private Context context;
    private int selectedPosition = 0;

    ChannelRecyclerViewAdapter(Context context, ChannelClickCallback channelClickCallback) {
        this.context = context;
        this.channelClickCallback = channelClickCallback;
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    @NonNull
    @Override
    public RecyclerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new RecyclerViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.channel_list_adapter, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerViewHolder holder, int position) {
        Channel channel = channelListFiltered.get(position);
        holder.itemView.setTag(channel);

        boolean showChannelName = sharedPreferences.getBoolean("channel_name_enabled", true);
        boolean showProgressbar = sharedPreferences.getBoolean("program_progressbar_enabled", true);
        boolean showSubtitle = sharedPreferences.getBoolean("program_subtitle_enabled", true);
        boolean showNextProgramTitle = sharedPreferences.getBoolean("next_program_title_enabled", true);
        boolean showChannelIcons = sharedPreferences.getBoolean("channel_icons_enabled", true);
        boolean showGenreColors = sharedPreferences.getBoolean("genre_colors_for_channels_enabled", false);
        boolean playUponChannelClick = sharedPreferences.getBoolean("channel_icon_starts_playback_enabled", true);

        // Sets the correct indication when the dual pane mode is active
        // If the item is selected the the arrow will be shown, otherwise
        // only a vertical separation line is displayed.
        if (holder.dualPaneListItemSelection != null) {
            boolean lightTheme = sharedPreferences.getBoolean("light_theme_enabled", true);
            if (selectedPosition == position) {
                final int icon = (lightTheme) ? R.drawable.dual_pane_selector_active_light : R.drawable.dual_pane_selector_active_dark;
                holder.dualPaneListItemSelection.setBackgroundResource(icon);
            } else {
                final int icon = R.drawable.dual_pane_selector_inactive;
                holder.dualPaneListItemSelection.setBackgroundResource(icon);
            }
        }

        // Set the initial values
        holder.progressbar.setProgress(0);
        holder.progressbar.setVisibility(showProgressbar ? View.VISIBLE : View.GONE);

        holder.channelTextView.setText(channel.getName());
        holder.channelTextView.setVisibility(showChannelName ? View.VISIBLE : View.GONE);

        // Show the channel icons. Otherwise show the channel name only
        UIUtils.loadIcon(context, showChannelIcons, channel.getIcon(), channel.getName(), holder.iconImageView, holder.iconTextView);

        if (playUponChannelClick) {
            holder.iconImageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    channelClickCallback.onChannelClick(channel.getId());
                }
            });
            holder.iconTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    channelClickCallback.onChannelClick(channel.getId());
                }
            });
        }

        if (channel.getProgramId() > 0) {
            holder.titleTextView.setText(channel.getProgramTitle());

            holder.subtitleTextView.setText(channel.getProgramSubtitle());
            holder.subtitleTextView.setVisibility(showSubtitle && !TextUtils.isEmpty(channel.getProgramSubtitle()) ? View.VISIBLE : View.GONE);

            String time = UIUtils.getTimeText(context, channel.getProgramStart()) + " - " + UIUtils.getTimeText(context, channel.getProgramStop());
            holder.timeTextView.setText(time);
            holder.timeTextView.setVisibility(View.VISIBLE);

            String durationTime = context.getString(R.string.minutes, (int) ((channel.getProgramStop() - channel.getProgramStart()) / 1000 / 60));
            holder.durationTextView.setText(durationTime);
            holder.durationTextView.setVisibility(View.VISIBLE);

            holder.progressbar.setProgress(getProgressPercentage(channel.getProgramStart(), channel.getProgramStop()));
            holder.progressbar.setVisibility(showProgressbar ? View.VISIBLE : View.GONE);

            if (showGenreColors) {
                int color = UIUtils.getGenreColor(context, channel.getProgramContentType(), 0);
                holder.genreTextView.setBackgroundColor(color);
                holder.genreTextView.setVisibility(View.VISIBLE);
            } else {
                holder.genreTextView.setVisibility(View.GONE);
            }

            Drawable stateDrawable = null;
            for (Recording recording : recordingList) {
                if (recording.getEventId() == channel.getProgramId()) {
                    stateDrawable = UIUtils.getRecordingState(context, recording);
                    break;
                }
            }
            holder.stateImageView.setImageDrawable(stateDrawable);

        } else {
            // The channel does not provide program data. Hide certain views
            holder.titleTextView.setText(R.string.no_data);
            holder.subtitleTextView.setVisibility(View.GONE);
            holder.progressbar.setVisibility(View.GONE);
            holder.timeTextView.setVisibility(View.GONE);
            holder.durationTextView.setVisibility(View.GONE);
            holder.genreTextView.setVisibility(View.GONE);
            holder.nextTitleTextView.setVisibility(View.GONE);
        }

        if (channel.getNextProgramId() > 0) {
            holder.nextTitleTextView.setVisibility(showNextProgramTitle ? View.VISIBLE : View.GONE);
            holder.nextTitleTextView.setText(context.getString(R.string.next_program, channel.getNextProgramTitle()));
        } else {
            holder.nextTitleTextView.setVisibility(View.GONE);
        }
    }

    void addItems(List<Channel> list) {
        channelList.clear();
        channelListFiltered.clear();

        if (list != null) {
            channelList = list;
            channelListFiltered = list;
        }

        if (list == null || selectedPosition > list.size()) {
            selectedPosition = 0;
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return channelListFiltered != null ? channelListFiltered.size() : 0;
    }

    public void setPosition(int pos) {
        selectedPosition = pos;
    }

    public Channel getItem(int position) {
        if (channelListFiltered.size() > position && position >= 0) {
            return channelListFiltered.get(position);
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
                    channelListFiltered = channelList;
                } else {
                    List<Channel> filteredList = new ArrayList<>();
                    // Iterate over the available channels. Use a copy on write
                    // array in case the channel list changes during filtering.
                    for (Channel channel : new CopyOnWriteArrayList<>(channelList)) {
                        // name match condition. this might differ depending on your requirement
                        // here we are looking for a channel name match
                        if (channel.getName().toLowerCase().contains(charString.toLowerCase())) {
                            filteredList.add(channel);
                        } else if (channel.getProgramTitle() != null
                                && channel.getProgramTitle().toLowerCase().contains(charString.toLowerCase())) {
                            filteredList.add(channel);
                        } else if (channel.getProgramSubtitle() != null
                                && channel.getProgramSubtitle().toLowerCase().contains(charString.toLowerCase())) {
                            filteredList.add(channel);
                        } else if (channel.getNextProgramTitle() != null
                                && channel.getNextProgramTitle().toLowerCase().contains(charString.toLowerCase())) {
                            filteredList.add(channel);
                        }
                    }
                    channelListFiltered = filteredList;
                }

                FilterResults filterResults = new FilterResults();
                filterResults.values = channelListFiltered;
                return filterResults;
            }

            @SuppressWarnings("unchecked")
            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                channelListFiltered = (ArrayList<Channel>) filterResults.values;
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
        recordingList.clear();
        recordingList = recordings;

        for (Recording recording : recordingList) {
            for (int i = 0; i < channelList.size(); i++) {
                Channel channel = channelList.get(i);
                if (recording.getEventId() == channel.getProgramId()) {
                    notifyItemChanged(i);
                    break;
                }
            }
        }
    }

    static class RecyclerViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.icon)
        ImageView iconImageView;
        @BindView(R.id.icon_text)
        TextView iconTextView;
        @BindView(R.id.title)
        TextView titleTextView;
        @BindView(R.id.subtitle)
        TextView subtitleTextView;
        @BindView(R.id.next_title)
        TextView nextTitleTextView;
        @BindView(R.id.channel)
        TextView channelTextView;
        @BindView(R.id.time)
        TextView timeTextView;
        @BindView(R.id.duration)
        TextView durationTextView;
        @BindView(R.id.progressbar)
        ProgressBar progressbar;
        @BindView(R.id.state)
        ImageView stateImageView;
        @BindView(R.id.genre)
        TextView genreTextView;
        @Nullable
        @BindView(R.id.dual_pane_list_item_selection)
        ImageView dualPaneListItemSelection;

        RecyclerViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }

    private int getProgressPercentage(long start, long stop) {
        // Get the start and end times to calculate the progress.
        double durationTime = (stop - start);
        double elapsedTime = new Date().getTime() - start;
        // Show the progress as a percentage
        double percentage = 0;
        if (durationTime > 0) {
            percentage = elapsedTime / durationTime;
        }
        return (int) Math.floor(percentage * 100);
    }
}
