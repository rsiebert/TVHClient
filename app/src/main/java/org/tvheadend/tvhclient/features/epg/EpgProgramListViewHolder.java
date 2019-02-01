package org.tvheadend.tvhclient.features.epg;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.entity.EpgProgram;
import org.tvheadend.tvhclient.data.entity.Recording;
import org.tvheadend.tvhclient.features.shared.callbacks.RecyclerViewClickCallback;
import org.tvheadend.tvhclient.utils.UIUtils;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

class EpgProgramListViewHolder extends RecyclerView.ViewHolder {
    private final float pixelsPerMinute;
    private final long fragmentStartTime;
    private final long fragmentStopTime;

    @BindView(R.id.program_item_layout)
    ConstraintLayout constraintLayout;
    @BindView(R.id.title)
    TextView titleTextView;
    @BindView(R.id.duration)
    TextView durationTextView;
    @BindView(R.id.subtitle)
    TextView subtitleTextView;
    @BindView(R.id.state)
    ImageView stateImageView;
    @BindView(R.id.genre)
    TextView genreTextView;

    EpgProgramListViewHolder(View view, float pixelsPerMinute, long fragmentStartTime, long fragmentStopTime) {
        super(view);
        this.pixelsPerMinute = pixelsPerMinute;
        this.fragmentStartTime = fragmentStartTime;
        this.fragmentStopTime = fragmentStopTime;
        ButterKnife.bind(this, view);
    }

    public void bindData(@NonNull final EpgProgram program, @NonNull List<Recording> recordingList, @NonNull RecyclerViewClickCallback clickCallback) {
        itemView.setTag(program);
        Context context = itemView.getContext();

        long startTime = (program.getStart() < fragmentStartTime) ? fragmentStartTime : program.getStart();
        long stopTime = (program.getStop() > fragmentStopTime) ? fragmentStopTime : program.getStop();
        int duration = (int) (((stopTime - startTime) / 1000 / 60) * pixelsPerMinute);

        RecyclerView.LayoutParams layoutParams = (RecyclerView.LayoutParams) constraintLayout.getLayoutParams();
        layoutParams.width = duration;
        constraintLayout.setLayoutParams(layoutParams);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean showProgramSubtitle = sharedPreferences.getBoolean("program_subtitle_enabled", true);
        boolean showGenreColors = sharedPreferences.getBoolean("genre_colors_for_program_guide_enabled", false);

        itemView.setOnClickListener(view -> clickCallback.onClick(view, getAdapterPosition()));
        itemView.setOnLongClickListener(view -> {
            clickCallback.onLongClick(view, getAdapterPosition());
            return true;
        });

        if (titleTextView != null) {
            titleTextView.setText(program.getTitle());
        }
        if (stateImageView != null) {
            Drawable stateDrawable = null;
            for (Recording recording : recordingList) {
                if (recording.getEventId() == program.getEventId()) {
                    stateDrawable = UIUtils.getRecordingState(context, recording);
                    break;
                }
            }
            stateImageView.setVisibility(stateDrawable != null ? View.VISIBLE : View.GONE);
            stateImageView.setImageDrawable(stateDrawable);
        }
        if (durationTextView != null) {
            String durationTime = context.getString(R.string.minutes, (int) ((program.getStop() - program.getStart()) / 1000 / 60));
            durationTextView.setText(durationTime);
        }
        if (subtitleTextView != null) {
            subtitleTextView.setVisibility(showProgramSubtitle && !TextUtils.isEmpty(program.getSubtitle()) ? View.VISIBLE : View.GONE);
            subtitleTextView.setText(program.getSubtitle());
        }
        if (genreTextView != null) {
            if (showGenreColors) {
                int offset = sharedPreferences.getInt("genre_color_transparency", 0);
                // The offset in the setting has a range from 30 to 100% opacity.
                // Reduce this a bit for the epg otherwise the colors would be to aggressive
                int reducedOffset = ((offset - 25 > 0) ? offset - 25 : offset);
                int color = UIUtils.getGenreColor(context, program.getContentType(), reducedOffset);
                genreTextView.setBackgroundColor(color);
                genreTextView.setVisibility(View.VISIBLE);
            } else {
                genreTextView.setVisibility(View.GONE);
            }
        }
    }
}
