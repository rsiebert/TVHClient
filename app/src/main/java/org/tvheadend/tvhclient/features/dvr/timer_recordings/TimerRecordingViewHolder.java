package org.tvheadend.tvhclient.features.dvr.timer_recordings;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
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
import org.tvheadend.tvhclient.data.entity.TimerRecording;
import org.tvheadend.tvhclient.utils.UIUtils;
import org.tvheadend.tvhclient.features.shared.callbacks.RecyclerViewClickCallback;

import butterknife.BindView;
import butterknife.ButterKnife;

public class TimerRecordingViewHolder extends RecyclerView.ViewHolder {

    @BindView(R.id.icon)
    ImageView iconImageView;
    @BindView(R.id.icon_text)
    TextView iconTextView;
    @BindView(R.id.title)
    TextView titleTextView;
    @BindView(R.id.channel)
    TextView channelTextView;
    @BindView(R.id.days_of_week)
    TextView daysOfWeekTextView;
    @BindView(R.id.start)
    TextView startTimeTextView;
    @BindView(R.id.stop)
    TextView stopTimeTextView;
    @BindView(R.id.duration)
    TextView durationTextView;
    @BindView(R.id.enabled)
    TextView isEnabledTextView;
    @Nullable
    @BindView(R.id.dual_pane_list_item_selection)
    ImageView dualPaneListItemSelection;

    TimerRecordingViewHolder(View view) {
        super(view);
        ButterKnife.bind(this, view);
    }

    public void bindData(Context context, final TimerRecording recording, boolean selected, int htspVersion, RecyclerViewClickCallback clickCallback) {
        itemView.setTag(recording);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        itemView.setTag(recording);

        boolean lightTheme = sharedPreferences.getBoolean("light_theme_enabled", true);

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

        if (recording != null) {
            String title = !TextUtils.isEmpty(recording.getTitle()) ? recording.getTitle() : recording.getName();
            titleTextView.setText(title);

            TextViewCompat.setAutoSizeTextTypeWithDefaults(iconTextView, TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM);

            if (!TextUtils.isEmpty(recording.getChannelName())) {
                iconTextView.setText(recording.getChannelName());
                channelTextView.setText(recording.getChannelName());
            } else {
                iconTextView.setText(R.string.all_channels);
                channelTextView.setText(R.string.all_channels);
            }

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

            String daysOfWeek = UIUtils.getDaysOfWeekText(context, recording.getDaysOfWeek());
            daysOfWeekTextView.setText(daysOfWeek);

            startTimeTextView.setText(UIUtils.getTimeText(context, recording.getStart()));
            stopTimeTextView.setText(UIUtils.getTimeText(context, recording.getStop()));

            String duration = context.getString(R.string.minutes, recording.getDuration());
            durationTextView.setText(duration);

            String isEnabled = context.getString((recording.getEnabled() > 0) ? R.string.recording_enabled : R.string.recording_disabled);
            isEnabledTextView.setVisibility(htspVersion >= 19 ? View.VISIBLE : View.GONE);
            isEnabledTextView.setText(isEnabled);
        }
    }
}
