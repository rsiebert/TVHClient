package org.tvheadend.tvhclient.features.programs;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.entity.Program;
import org.tvheadend.tvhclient.features.shared.callbacks.RecyclerViewClickCallback;
import org.tvheadend.tvhclient.utils.UIUtils;

import androidx.annotation.NonNull;
import androidx.core.widget.TextViewCompat;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;

public class ProgramViewHolder extends RecyclerView.ViewHolder {

    @BindView(R.id.icon)
    ImageView iconImageView;
    @BindView(R.id.icon_text)
    TextView iconTextView;
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

    private final boolean showProgramChannelIcon;

    ProgramViewHolder(View view, boolean showProgramChannelIcon) {
        super(view);
        this.showProgramChannelIcon = showProgramChannelIcon;
        ButterKnife.bind(this, view);
    }

    public void bindData(@NonNull final Program program, RecyclerViewClickCallback clickCallback) {
        itemView.setTag(program);
        Context context = itemView.getContext();

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean showProgramSubtitle = sharedPreferences.getBoolean("program_subtitle_enabled", true);
        boolean showGenreColors = sharedPreferences.getBoolean("genre_colors_for_programs_enabled", false);

        itemView.setOnClickListener(view -> clickCallback.onClick(view, getAdapterPosition()));
        itemView.setOnLongClickListener(view -> {
            clickCallback.onLongClick(view, getAdapterPosition());
            return true;
        });

        if (showProgramChannelIcon) {
            TextViewCompat.setAutoSizeTextTypeWithDefaults(iconTextView, TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM);
            iconTextView.setText(program.getChannelName());
            iconTextView.setVisibility(View.VISIBLE);

            // Show the channel icons. Otherwise show the channel name only
            Picasso.get()
                    .load(UIUtils.getIconUrl(context, program.getChannelIcon()))
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
        }

        titleTextView.setText(program.getTitle());

        Drawable drawable = UIUtils.getRecordingState(context, program.getRecording());
        stateTextView.setVisibility(drawable != null ? View.VISIBLE : View.GONE);
        stateTextView.setImageDrawable(drawable);

        dateTextView.setText(UIUtils.getDate(context, program.getStart()));

        String time = UIUtils.getTimeText(context, program.getStart()) + " - " + UIUtils.getTimeText(context, program.getStop());
        timeTextView.setText(time);

        String durationTime = context.getString(R.string.minutes, (int) ((program.getStop() - program.getStart()) / 1000 / 60));
        durationTextView.setText(durationTime);

        String progressText = UIUtils.getProgressText(context, program.getStart(), program.getStop());
        progressTextView.setVisibility(!TextUtils.isEmpty(progressText) ? View.VISIBLE : View.GONE);
        progressTextView.setText(progressText);

        String contentType = UIUtils.getContentTypeText(context, program.getContentType());
        contentTypeTextView.setVisibility(!TextUtils.isEmpty(contentType) ? View.VISIBLE : View.GONE);
        contentTypeTextView.setText(contentType);

        String seriesInfo = UIUtils.getSeriesInfo(context, program);
        seriesInfoTextView.setVisibility(!TextUtils.isEmpty(seriesInfo) ? View.VISIBLE : View.GONE);
        seriesInfoTextView.setText(seriesInfo);

        subtitleTextView.setVisibility(showProgramSubtitle
                && !TextUtils.isEmpty(program.getSubtitle())
                && !TextUtils.equals(program.getSubtitle(), program.getTitle()) ? View.VISIBLE : View.GONE);
        subtitleTextView.setText(program.getSubtitle());

        descriptionTextView.setVisibility(!TextUtils.isEmpty(program.getDescription()) ? View.VISIBLE : View.GONE);
        descriptionTextView.setText(program.getDescription());

        if (!TextUtils.isEmpty(program.getSummary())
                && !TextUtils.equals(program.getSubtitle(), program.getSummary())) {
            summaryTextView.setVisibility(View.VISIBLE);
            summaryTextView.setText(program.getSummary());
        } else {
            summaryTextView.setVisibility(View.GONE);
        }

        if (showGenreColors) {
            int offset = sharedPreferences.getInt("genre_color_transparency", 0);
            int color = UIUtils.getGenreColor(context, program.getContentType(), offset);
            genreTextView.setBackgroundColor(color);
            genreTextView.setVisibility(View.VISIBLE);
        } else {
            genreTextView.setVisibility(View.GONE);
        }
    }
}
