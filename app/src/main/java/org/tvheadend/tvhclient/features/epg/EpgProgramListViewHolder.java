package org.tvheadend.tvhclient.features.epg;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.entity.Program;
import org.tvheadend.tvhclient.features.shared.UIUtils;
import org.tvheadend.tvhclient.features.shared.callbacks.RecyclerViewClickCallback;

import javax.annotation.Nullable;

import butterknife.BindView;
import butterknife.ButterKnife;

class EpgProgramListViewHolder extends RecyclerView.ViewHolder {
    private final float pixelsPerMinute;
    private final long fragmentStartTime;
    private final long fragmentStopTime;

    @BindView(R.id.program_item_layout)
    ConstraintLayout constraintLayout;
    @Nullable
    @BindView(R.id.title)
    TextView titleTextView;
    @Nullable
    @BindView(R.id.duration)
    TextView durationTextView;
    @Nullable
    @BindView(R.id.subtitle)
    TextView subtitleTextView;
    @Nullable
    @BindView(R.id.state)
    ImageView stateTextView;
    @Nullable
    @BindView(R.id.genre)
    TextView genreTextView;

    EpgProgramListViewHolder(View view, float pixelsPerMinute, long fragmentStartTime, long fragmentStopTime) {
        super(view);
        this.pixelsPerMinute = pixelsPerMinute;
        this.fragmentStartTime = fragmentStartTime;
        this.fragmentStopTime = fragmentStopTime;
        ButterKnife.bind(this, view);
    }

    public void bindData(Context context, final Program program, RecyclerViewClickCallback clickCallback) {
        //Timber.d("bindData program is null " + (program == null));

        if (program != null) {
            itemView.setTag(program);

            long startTime = (program.getStart() < fragmentStartTime) ? fragmentStartTime : program.getStart();
            long stopTime = (program.getStop() > fragmentStopTime) ? fragmentStopTime : program.getStop();

            int duration = (int) ((stopTime - startTime) / 1000 / 60 * pixelsPerMinute);
            constraintLayout.setMaxWidth(duration);
            constraintLayout.setMinWidth(duration);

            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            boolean showProgramSubtitle = sharedPreferences.getBoolean("program_subtitle_enabled", true);
            boolean showGenreColors = sharedPreferences.getBoolean("genre_colors_for_programs_enabled", false);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (clickCallback != null) {
                        clickCallback.onClick(view, getAdapterPosition());
                    }
                }
            });
            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    if (clickCallback != null) {
                        clickCallback.onLongClick(view, getAdapterPosition());
                    }
                    return true;
                }
            });

            if (titleTextView != null) {
                titleTextView.setText(program.getTitle());
            }
            if (stateTextView != null) {
                Drawable drawable = UIUtils.getRecordingState(context, program.getRecording());
                stateTextView.setVisibility(drawable != null ? View.VISIBLE : View.GONE);
                stateTextView.setImageDrawable(drawable);
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
                    int color = UIUtils.getGenreColor(context, program.getContentType(), 0);
                    genreTextView.setBackgroundColor(color);
                    genreTextView.setVisibility(View.VISIBLE);
                } else {
                    genreTextView.setVisibility(View.GONE);
                }
            }
        }
    }
}
