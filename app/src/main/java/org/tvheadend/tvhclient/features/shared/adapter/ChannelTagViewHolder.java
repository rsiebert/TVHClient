package org.tvheadend.tvhclient.features.shared.adapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.entity.ChannelTag;
import org.tvheadend.tvhclient.utils.UIUtils;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ChannelTagViewHolder extends RecyclerView.ViewHolder {

    @BindView(R.id.icon)
    ImageView iconImageView;
    @BindView(R.id.title)
    TextView titleTextView;
    @Nullable
    @BindView(R.id.selected)
    RadioButton selectedRadioButton;
    @Nullable
    @BindView(R.id.checked)
    CheckBox selectedCheckBox;
    @BindView(R.id.count)
    TextView countTextView;

    ChannelTagViewHolder(View view) {
        super(view);
        ButterKnife.bind(this, view);
    }

    public void bindData(@NonNull final ChannelTag channelTag, int channelCount, boolean selected) {
        itemView.setTag(channelTag);
        Context context = itemView.getContext();

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean showChannelTagIcons = sharedPreferences.getBoolean("channel_tag_icons_enabled", true);

        if (selectedRadioButton != null) {
            selectedRadioButton.setChecked(selected);
        }
        if (selectedCheckBox != null) {
            selectedCheckBox.setChecked(selected);
        }

        titleTextView.setText(channelTag.getTagName());

        itemView.setTag(channelTag);
        if (!TextUtils.isEmpty(channelTag.getTagIcon()) && showChannelTagIcons) {
            iconImageView.setVisibility(View.VISIBLE);
            Picasso.get()
                    .load(UIUtils.getIconUrl(context, channelTag.getTagIcon()))
                    .into(iconImageView);
        } else {
            iconImageView.setVisibility(View.GONE);
        }

        if (channelTag.getTagId() > 0) {
            countTextView.setText(String.valueOf(channelTag.getChannelCount()));
        } else {
            countTextView.setText(String.valueOf(channelCount));
        }
    }
}