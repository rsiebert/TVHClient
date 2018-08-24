package org.tvheadend.tvhclient.features.channels;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.widget.TextViewCompat;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.entity.Channel;
import org.tvheadend.tvhclient.data.entity.Recording;
import org.tvheadend.tvhclient.utils.UIUtils;
import org.tvheadend.tvhclient.features.shared.callbacks.RecyclerViewClickCallback;

import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ChannelViewHolder extends RecyclerView.ViewHolder {

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
    @BindView(R.id.no_programs)
    TextView noProgramsTextView;
    @Nullable
    @BindView(R.id.dual_pane_list_item_selection)
    ImageView dualPaneListItemSelection;

    ChannelViewHolder(View view) {
        super(view);
        ButterKnife.bind(this, view);
    }

    public void bindData(Context context, final Channel channel, boolean selected, List<Recording> recordingList, RecyclerViewClickCallback clickCallback) {
        itemView.setTag(channel);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean showChannelName = sharedPreferences.getBoolean("channel_name_enabled", true);
        boolean showProgressbar = sharedPreferences.getBoolean("program_progressbar_enabled", true);
        boolean showSubtitle = sharedPreferences.getBoolean("program_subtitle_enabled", true);
        boolean showNextProgramTitle = sharedPreferences.getBoolean("next_program_title_enabled", true);
        boolean showGenreColors = sharedPreferences.getBoolean("genre_colors_for_channels_enabled", false);
        boolean playUponChannelClick = sharedPreferences.getBoolean("channel_icon_starts_playback_enabled", true);

        // Sets the correct indication when the dual pane mode is active
        // If the item is selected the the arrow will be shown, otherwise
        // only a vertical separation line is displayed.
        if (dualPaneListItemSelection != null) {
            boolean lightTheme = sharedPreferences.getBoolean("light_theme_enabled", true);
            if (selected) {
                final int icon = (lightTheme) ? R.drawable.dual_pane_selector_active_light : R.drawable.dual_pane_selector_active_dark;
                dualPaneListItemSelection.setBackgroundResource(icon);
            } else {
                final int icon = R.drawable.dual_pane_selector_inactive;
                dualPaneListItemSelection.setBackgroundResource(icon);
            }
        }

        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clickCallback.onClick(view, getAdapterPosition());
            }
        });
        itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                clickCallback.onLongClick(view, getAdapterPosition());
                return true;
            }
        });
        iconImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (playUponChannelClick) {
                    clickCallback.onClick(view, getAdapterPosition());
                }
            }
        });
        iconTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (playUponChannelClick) {
                    clickCallback.onClick(view, getAdapterPosition());
                }
            }
        });

        // Set the initial values
        progressbar.setProgress(0);
        progressbar.setVisibility(showProgressbar ? View.VISIBLE : View.GONE);

        channelTextView.setText(channel.getName());
        channelTextView.setVisibility(showChannelName ? View.VISIBLE : View.GONE);

        TextViewCompat.setAutoSizeTextTypeWithDefaults(iconTextView, TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM);
        iconTextView.setText(channel.getName());

        // Show the channel icons. Otherwise show the channel name only
        Picasso.get()
                .load(UIUtils.getIconUrl(context, channel.getIcon()))
                .into(iconImageView, new Callback() {
                    @Override
                    public void onSuccess() {
                        iconTextView.setVisibility(View.INVISIBLE);
                        iconImageView.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onError(Exception e) {

                    }
                });

        if (channel.getProgramId() > 0) {
            noProgramsTextView.setVisibility(View.GONE);
            titleTextView.setText(channel.getProgramTitle());

            subtitleTextView.setText(channel.getProgramSubtitle());
            subtitleTextView.setVisibility(showSubtitle && !TextUtils.isEmpty(channel.getProgramSubtitle()) ? View.VISIBLE : View.GONE);

            String time = UIUtils.getTimeText(context, channel.getProgramStart()) + " - " + UIUtils.getTimeText(context, channel.getProgramStop());
            timeTextView.setText(time);
            timeTextView.setVisibility(View.VISIBLE);

            String durationTime = context.getString(R.string.minutes, (int) ((channel.getProgramStop() - channel.getProgramStart()) / 1000 / 60));
            durationTextView.setText(durationTime);
            durationTextView.setVisibility(View.VISIBLE);

            progressbar.setProgress(getProgressPercentage(channel.getProgramStart(), channel.getProgramStop()));
            progressbar.setVisibility(showProgressbar ? View.VISIBLE : View.GONE);

            if (showGenreColors) {
                int offset = sharedPreferences.getInt("genre_color_transparency", 0);
                int color = UIUtils.getGenreColor(context, channel.getProgramContentType(), offset);
                genreTextView.setBackgroundColor(color);
                genreTextView.setVisibility(View.VISIBLE);
            } else {
                genreTextView.setVisibility(View.GONE);
            }

            Drawable stateDrawable = UIUtils.getRecordingState(context, channel.getRecording());
            stateImageView.setVisibility(stateDrawable != null ? View.VISIBLE : View.GONE);
            stateImageView.setImageDrawable(stateDrawable);

        } else {
            // The channel does not provide program data. Hide certain views
            noProgramsTextView.setVisibility(View.VISIBLE);
            titleTextView.setVisibility(View.GONE);
            subtitleTextView.setVisibility(View.GONE);
            progressbar.setVisibility(View.GONE);
            timeTextView.setVisibility(View.GONE);
            durationTextView.setVisibility(View.GONE);
            genreTextView.setVisibility(View.GONE);
            nextTitleTextView.setVisibility(View.GONE);
        }

        if (channel.getNextProgramId() > 0) {
            nextTitleTextView.setVisibility(showNextProgramTitle ? View.VISIBLE : View.GONE);
            nextTitleTextView.setText(context.getString(R.string.next_program, channel.getNextProgramTitle()));
        } else {
            nextTitleTextView.setVisibility(View.GONE);
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
