package org.tvheadend.tvhclient.ui.channels;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.model.Channel;
import org.tvheadend.tvhclient.data.model.Program;
import org.tvheadend.tvhclient.ui.common.RecyclerViewClickCallback;
import org.tvheadend.tvhclient.utils.MiscUtils;
import org.tvheadend.tvhclient.utils.UIUtils;

import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ChannelRecyclerViewAdapter extends RecyclerView.Adapter<ChannelRecyclerViewAdapter.RecyclerViewHolder> {

    private final ChannelClickCallback channelClickCallback;
    private List<Channel> channelList;
    private RecyclerViewClickCallback clickCallback;
    private SharedPreferences sharedPreferences;
    private Context context;
    private int selectedPosition = 0;

    ChannelRecyclerViewAdapter(Context context, List<Channel> channelList, RecyclerViewClickCallback clickCallback, ChannelClickCallback channelClickCallback) {
        this.context = context;
        this.channelList = channelList;
        this.clickCallback = clickCallback;
        this.channelClickCallback = channelClickCallback;
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    @Override
    public RecyclerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new RecyclerViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.channel_list_adapter, parent, false));
    }

    @Override
    public void onBindViewHolder(RecyclerViewHolder holder, int position) {
        Channel channel = channelList.get(position);
        holder.itemView.setTag(channel);
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

        boolean showChannelName = sharedPreferences.getBoolean("showChannelNamePref", true);
        boolean showProgressbar = sharedPreferences.getBoolean("showProgramProgressbarPref", true);
        boolean showSubtitle = sharedPreferences.getBoolean("showProgramSubtitlePref", true);
        boolean showNextProgramTitle = sharedPreferences.getBoolean("showNextProgramPref", true);
        boolean showChannelIcons = sharedPreferences.getBoolean("showIconPref", true);
        boolean showGenreColors = sharedPreferences.getBoolean("showGenreColorsChannelsPref", false);
        boolean playUponChannelClick = sharedPreferences.getBoolean("playWhenChannelIconSelectedPref", true);

        // Sets the correct indication when the dual pane mode is active
        // If the item is selected the the arrow will be shown, otherwise
        // only a vertical separation line is displayed.
        if (holder.dualPaneListItemSelection != null) {
            boolean lightTheme = sharedPreferences.getBoolean("lightThemePref", true);
            if (selectedPosition == position) {
                final int icon = (lightTheme) ? R.drawable.dual_pane_selector_active_light : R.drawable.dual_pane_selector_active_dark;
                holder.dualPaneListItemSelection.setBackgroundResource(icon);
            } else {
                final int icon = R.drawable.dual_pane_selector_inactive;
                holder.dualPaneListItemSelection.setBackgroundResource(icon);
            }
        }

        if (channel != null) {

            // Set the initial values
            holder.progressbar.setProgress(0);
            holder.progressbar.setVisibility(showProgressbar ? View.VISIBLE : View.GONE);

            holder.channelTextView.setText(channel.getChannelName());
            holder.channelTextView.setVisibility(showChannelName ? View.VISIBLE : View.GONE);

            // Show the regular or large channel icons. Otherwise show the channel name only
            // Assign the channel icon image or a null image
            Bitmap iconBitmap = MiscUtils.getCachedIcon(context, channel.getChannelIcon());
            holder.iconImageView.setImageBitmap(iconBitmap);
            holder.iconTextView.setText(channel.getChannelName());

            if (showChannelIcons) {
                holder.iconImageView.setVisibility(iconBitmap != null ? ImageView.VISIBLE : ImageView.INVISIBLE);
                holder.iconTextView.setVisibility(iconBitmap == null ? ImageView.VISIBLE : ImageView.INVISIBLE);
            } else {
                holder.iconImageView.setVisibility(View.GONE);
                holder.iconTextView.setVisibility(View.GONE);
            }

            if (playUponChannelClick) {
                holder.iconImageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        channelClickCallback.onChannelClick(channel.getChannelId());
                    }
                });
                holder.iconTextView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        channelClickCallback.onChannelClick(channel.getChannelId());
                    }
                });
            }
        }

        Program program = null;
        if (program != null) {
            holder.titleTextView.setText(program.getTitle());
            holder.subtitleTextView.setText(program.getSubtitle());
            holder.subtitleTextView.setVisibility(showSubtitle ? View.VISIBLE : View.GONE);

            String time = UIUtils.getTime(context, program.getStart()) + " - " + UIUtils.getTime(context, program.getStop());
            holder.timeTextView.setText(time);

            String durationTime = context.getString(R.string.minutes, (int) ((program.getStop() - program.getStart()) / 1000 / 60));
            holder.durationTextView.setText(durationTime);

            holder.progressbar.setProgress(getProgressPercentage(program.getStart(), program.getStop()));
            holder.progressbar.setVisibility(showProgressbar ? View.VISIBLE : View.GONE);

            if (showGenreColors) {
                int color = UIUtils.getGenreColor(context, program.getContentType(), 0);
                holder.genreTextView.setBackgroundColor(color);
                holder.genreTextView.setVisibility(View.VISIBLE);
            } else {
                holder.genreTextView.setVisibility(View.GONE);
            }
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

        Program nextProgram = null;
        if (nextProgram != null) {
            holder.nextTitleTextView.setVisibility(showNextProgramTitle ? View.VISIBLE : View.GONE);
            holder.nextTitleTextView.setText(context.getString(R.string.next_program, nextProgram.getTitle()));
        }
    }

    void addItems(List<Channel> channelList) {
        this.channelList = channelList;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return channelList.size();
    }

    public void setPosition(int pos) {
        selectedPosition = pos;
    }

    public Channel getItem(int position) {
        return channelList.get(position);
    }

    public Channel getSelectedItem() {
        // TODO
        return channelList.get(selectedPosition);
    }

    public List<Channel> getItems() {
        return channelList;
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
