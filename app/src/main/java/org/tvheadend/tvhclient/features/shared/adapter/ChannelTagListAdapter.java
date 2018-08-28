package org.tvheadend.tvhclient.features.shared.adapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.entity.ChannelTag;
import org.tvheadend.tvhclient.utils.UIUtils;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class ChannelTagListAdapter extends RecyclerView.Adapter<ChannelTagListAdapter.ViewHolder> {

    private final Context context;
    private final int channelCount;
    private Callback callback;
    private final List<ChannelTag> channelTagList;
    private final int channelTagId;
    private boolean showChannelTagIcons;

    public interface Callback {
        void onItemClicked(int index);
    }

    public ChannelTagListAdapter(Context context, List<ChannelTag> channelTagList, int channelTagId, int channelCount) {
        this.context = context;
        this.channelTagList = channelTagList;
        this.channelTagId = channelTagId;
        this.channelCount = channelCount;
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(parent.getContext());
        showChannelTagIcons = prefs.getBoolean("channel_tag_icons_enabled", true);
        final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.channeltag_list_selection_dialog_adapter, parent, false);
        return new ViewHolder(view, this);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final ChannelTag channelTag = channelTagList.get(position);
        if (channelTag != null) {

            if (channelTag.getTagId() == channelTagId) {
                holder.radioButton.setChecked(true);
            } else {
                holder.radioButton.setChecked(false);
            }

            holder.titleTextView.setText(channelTag.getTagName());

            holder.itemView.setTag(channelTag);
            if (!TextUtils.isEmpty(channelTag.getTagIcon()) && showChannelTagIcons) {
                holder.iconImageView.setVisibility(View.VISIBLE);
                Picasso.get()
                        .load(UIUtils.getIconUrl(context, channelTag.getTagIcon()))
                        .into(holder.iconImageView);
            } else {
                holder.iconImageView.setVisibility(View.GONE);
            }

            if (channelTag.getTagId() > 0) {
                holder.countTextView.setText(String.valueOf(channelTag.getChannelCount()));
            } else {
                holder.countTextView.setText(String.valueOf(channelCount));
            }
        }
    }

    @Override
    public int getItemCount() {
        return channelTagList.size();
    }

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        @BindView(R.id.icon)
        ImageView iconImageView;
        @BindView(R.id.title)
        TextView titleTextView;
        @BindView(R.id.radioButton)
        RadioButton radioButton;
        @BindView(R.id.count)
        TextView countTextView;

        final ChannelTagListAdapter channelTagListAdapter;

        ViewHolder(View view, ChannelTagListAdapter adapter) {
            super(view);
            ButterKnife.bind(this, view);
            channelTagListAdapter = adapter;
            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            onClick();
        }

        @OnClick(R.id.radioButton)
        public void onClick() {
            if (channelTagListAdapter != null && channelTagListAdapter.callback != null) {
                ChannelTag channelTag = channelTagListAdapter.channelTagList.get(getAdapterPosition());
                channelTagListAdapter.callback.onItemClicked(channelTag.getTagId());
            }
        }
    }
}
