package org.tvheadend.tvhclient.features.epg;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.entity.ChannelSubset;
import org.tvheadend.tvhclient.features.shared.callbacks.RecyclerViewClickCallback;
import org.tvheadend.tvhclient.utils.UIUtils;

import butterknife.BindView;
import butterknife.ButterKnife;

public class EpgChannelViewHolder extends RecyclerView.ViewHolder {

    @BindView(R.id.icon)
    ImageView iconImageView;
    @BindView(R.id.icon_text)
    TextView iconTextView;

    EpgChannelViewHolder(View view) {
        super(view);
        ButterKnife.bind(this, view);
    }

    public void bindData(Context context, final ChannelSubset channel, RecyclerViewClickCallback clickCallback) {
        itemView.setTag(channel);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean playUponChannelClick = sharedPreferences.getBoolean("channel_icon_starts_playback_enabled", true);

        itemView.setOnClickListener(view -> clickCallback.onClick(view, getAdapterPosition()));
        itemView.setOnLongClickListener(view -> {
            clickCallback.onLongClick(view, getAdapterPosition());
            return true;
        });
        iconImageView.setOnClickListener(view -> {
            if (playUponChannelClick) {
                clickCallback.onClick(view, getAdapterPosition());
            }
        });
        iconTextView.setOnClickListener(view -> {
            if (playUponChannelClick) {
                clickCallback.onClick(view, getAdapterPosition());
            }
        });

        //TextViewCompat.setAutoSizeTextTypeWithDefaults(iconTextView, TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM);
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
                        iconTextView.setVisibility(View.VISIBLE);
                        iconImageView.setVisibility(View.INVISIBLE);
                    }
                });
    }
}
