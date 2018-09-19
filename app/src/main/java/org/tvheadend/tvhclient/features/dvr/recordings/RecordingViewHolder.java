package org.tvheadend.tvhclient.features.dvr.recordings;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.TextViewCompat;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.entity.Recording;
import org.tvheadend.tvhclient.features.shared.callbacks.RecyclerViewClickCallback;
import org.tvheadend.tvhclient.utils.UIUtils;

import butterknife.BindView;
import butterknife.ButterKnife;

import static org.tvheadend.tvhclient.features.dvr.recordings.RecordingListFragment.REC_TYPE_COMPLETED;
import static org.tvheadend.tvhclient.features.dvr.recordings.RecordingListFragment.REC_TYPE_SCHEDULED;

public class RecordingViewHolder extends RecyclerView.ViewHolder {

    @BindView(R.id.icon)
    ImageView iconImageView;
    @BindView(R.id.icon_text)
    TextView iconTextView;
    @BindView(R.id.title)
    TextView titleTextView;
    @BindView(R.id.subtitle)
    TextView subtitleTextView;
    @BindView(R.id.summary)
    TextView summaryTextView;
    @BindView(R.id.is_series_recording)
    TextView isSeriesRecordingTextView;
    @BindView(R.id.is_timer_recording)
    TextView isTimerRecordingTextView;
    @BindView(R.id.channel)
    TextView channelTextView;
    @BindView(R.id.start)
    TextView startTimeTextView;
    @BindView(R.id.stop)
    TextView stopTimeTextView;
    @BindView(R.id.date)
    TextView dateTextView;
    @BindView(R.id.duration)
    TextView durationTextView;
    @Nullable
    @BindView(R.id.state)
    ImageView stateImageView;
    @BindView(R.id.description)
    TextView descriptionTextView;
    @BindView(R.id.failed_reason)
    TextView failedReasonTextView;
    @BindView(R.id.enabled)
    TextView isEnabledTextView;
    @BindView(R.id.duplicate)
    TextView isDuplicateTextView;
    @Nullable
    @BindView(R.id.dual_pane_list_item_selection)
    ImageView dualPaneListItemSelection;
    @BindView(R.id.data_size)
    TextView dataSizeTextView;
    @BindView(R.id.data_errors)
    TextView dataErrorsTextView;

    private final int recordingType;

    RecordingViewHolder(View view, int recordingType) {
        super(view);
        this.recordingType = recordingType;
        ButterKnife.bind(this, view);
    }

    public void bindData(Context context, @NonNull final Recording recording, boolean selected, int htspVersion, RecyclerViewClickCallback clickCallback) {
        itemView.setTag(recording);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean playOnChannelIcon = sharedPreferences.getBoolean("channel_icon_starts_playback_enabled", true);
        boolean lightTheme = sharedPreferences.getBoolean("light_theme_enabled", true);
        boolean showRecordingFileStatus = sharedPreferences.getBoolean("show_recording_file_status_enabled", false);

        itemView.setOnClickListener(view -> clickCallback.onClick(view, getAdapterPosition()));
        itemView.setOnLongClickListener(view -> {
            clickCallback.onLongClick(view, getAdapterPosition());
            return true;
        });
        iconImageView.setOnClickListener(view -> {
            if (playOnChannelIcon && (recording.isCompleted() || recording.isRecording())) {
                clickCallback.onClick(view, getAdapterPosition());
            }
        });

        if (dualPaneListItemSelection != null) {
            // Set the correct indication when the dual pane mode is active
            // If the item is selected the the arrow will be shown, otherwise
            // only a vertical separation line is displayed.
            if (selected) {
                final int icon = (lightTheme) ? R.drawable.dual_pane_selector_active_light : R.drawable.dual_pane_selector_active_dark;
                dualPaneListItemSelection.setBackgroundResource(icon);
            } else {
                final int icon = R.drawable.dual_pane_selector_inactive;
                dualPaneListItemSelection.setBackgroundResource(icon);
            }
        }

        titleTextView.setText(recording.getTitle());

        subtitleTextView.setVisibility(!TextUtils.isEmpty(recording.getSubtitle()) ? View.VISIBLE : View.GONE);
        subtitleTextView.setText(recording.getSubtitle());

        boolean hideSummaryView = TextUtils.isEmpty(recording.getSummary())
                || TextUtils.equals(recording.getSubtitle(), recording.getSummary());
        summaryTextView.setVisibility(hideSummaryView ? View.GONE : View.VISIBLE);
        summaryTextView.setText(recording.getSummary());

        channelTextView.setText(recording.getChannelName());

        TextViewCompat.setAutoSizeTextTypeWithDefaults(iconTextView, TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM);
        iconTextView.setText(recording.getChannelName());

        // Show the channel icon if available and set in the preferences.
        // If not chosen, hide the imageView and show the channel name.
        Picasso.get()
                .load(UIUtils.getIconUrl(context, recording.getChannelIcon()))
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

        dateTextView.setText(UIUtils.getDate(context, recording.getStart()));

        startTimeTextView.setText(UIUtils.getTimeText(context, recording.getStart()));
        stopTimeTextView.setText(UIUtils.getTimeText(context, recording.getStop()));

        String durationTime = context.getString(R.string.minutes, recording.getDuration());
        durationTextView.setText(durationTime);

        descriptionTextView.setVisibility(!TextUtils.isEmpty(recording.getDescription()) ? View.VISIBLE : View.GONE);
        descriptionTextView.setText(recording.getDescription());

        if (recordingType == REC_TYPE_COMPLETED) {
            failedReasonTextView.setVisibility(View.GONE);
        } else {
            String failedReasonText = UIUtils.getRecordingFailedReasonText(context, recording);
            failedReasonTextView.setVisibility(!TextUtils.isEmpty(failedReasonText) ? View.VISIBLE : View.GONE);
            failedReasonTextView.setText(failedReasonText);
        }

        // Show only the recording icon
        if (stateImageView != null) {
            if (recording.isRecording()) {
                stateImageView.setImageResource(R.drawable.ic_rec_small);
                stateImageView.setVisibility(ImageView.VISIBLE);
            } else {
                stateImageView.setVisibility(ImageView.GONE);
            }
        }

        // Show the information if the recording belongs to a series or timer recording
        isSeriesRecordingTextView.setVisibility(TextUtils.isEmpty(recording.getAutorecId()) ? View.GONE : ImageView.VISIBLE);
        isTimerRecordingTextView.setVisibility(TextUtils.isEmpty(recording.getTimerecId()) ? View.GONE : ImageView.VISIBLE);

        if (recordingType != REC_TYPE_SCHEDULED) {
            isEnabledTextView.setVisibility(View.GONE);
            isDuplicateTextView.setVisibility(View.GONE);
        } else {
            isEnabledTextView.setVisibility(htspVersion < 19 || recording.getEnabled() == 0 ? View.GONE : View.VISIBLE);
            isEnabledTextView.setText(recording.getEnabled() > 0 ? R.string.recording_enabled : R.string.recording_disabled);

            isDuplicateTextView.setVisibility(htspVersion < 33 || recording.getDuplicate() == 0 ? View.GONE : View.VISIBLE);
        }

        if (showRecordingFileStatus) {
            dataErrorsTextView.setVisibility(View.VISIBLE);
            dataSizeTextView.setVisibility(View.VISIBLE);

            if (recording.getDataSize() > 1048576) {
                dataSizeTextView.setText(context.getResources().getString(R.string.data_size, recording.getDataSize() / 1048576, "MB"));
            } else {
                dataSizeTextView.setText(context.getResources().getString(R.string.data_size, recording.getDataSize() / 1024, "KB"));
            }
            dataErrorsTextView.setText(context.getResources().getString(R.string.data_errors, recording.getDataErrors() == null ? "0" : recording.getDataErrors()));
        } else {
            dataErrorsTextView.setVisibility(View.GONE);
            dataSizeTextView.setVisibility(View.GONE);
        }
    }
}
