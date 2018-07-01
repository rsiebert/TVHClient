package org.tvheadend.tvhclient.features.epg.other;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.entity.Program;
import org.tvheadend.tvhclient.features.shared.UIUtils;
import org.tvheadend.tvhclient.features.shared.callbacks.RecyclerViewClickCallback;

import butterknife.BindView;
import butterknife.ButterKnife;

class EpgHorizontalProgramListViewHolder extends RecyclerView.ViewHolder {
    @BindView(R.id.title)
    TextView titleTextView;
    @BindView(R.id.time)
    TextView timeTextView;
    @BindView(R.id.date)
    TextView dateTextView;
    @BindView(R.id.duration)
    TextView durationTextView;
    @BindView(R.id.subtitle)
    TextView subtitleTextView;
    @BindView(R.id.state)
    ImageView stateTextView;
    @BindView(R.id.genre)
    TextView genreTextView;

    EpgHorizontalProgramListViewHolder(View view) {
        super(view);
        ButterKnife.bind(this, view);
    }

    public void bindData(Context context, final Program program, RecyclerViewClickCallback clickCallback) {
        itemView.setTag(program);

        //Timber.d("Getting layout parameters for program " + program.getTitle());
        //ViewGroup.LayoutParams layoutParams = itemView.getLayoutParams();

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean showProgramSubtitle = sharedPreferences.getBoolean("program_subtitle_enabled", true);
        boolean showGenreColors = sharedPreferences.getBoolean("genre_colors_for_programs_enabled", false);

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

        if (program != null) {
            titleTextView.setText(program.getTitle());

            Drawable drawable = UIUtils.getRecordingState(context, program.getRecording());
            stateTextView.setVisibility(drawable != null ? View.VISIBLE : View.GONE);
            stateTextView.setImageDrawable(drawable);

            dateTextView.setText(UIUtils.getDate(context, program.getStart()));

            String time = UIUtils.getTimeText(context, program.getStart()) + " - " + UIUtils.getTimeText(context, program.getStop());
            timeTextView.setText(time);

            String durationTime = context.getString(R.string.minutes, (int) ((program.getStop() - program.getStart()) / 1000 / 60));
            durationTextView.setText(durationTime);

            subtitleTextView.setVisibility(showProgramSubtitle && !TextUtils.isEmpty(program.getSubtitle()) ? View.VISIBLE : View.GONE);
            subtitleTextView.setText(program.getSubtitle());

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
