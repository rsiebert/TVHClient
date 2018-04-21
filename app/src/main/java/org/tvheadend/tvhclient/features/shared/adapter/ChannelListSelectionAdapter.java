package org.tvheadend.tvhclient.features.shared.adapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.entity.Channel;
import org.tvheadend.tvhclient.features.shared.UIUtils;

import java.util.List;

public class ChannelListSelectionAdapter extends RecyclerView.Adapter<ChannelListSelectionAdapter.ViewHolder> {

    private final Context context;
    private Callback callback;
    private List<Channel> channelList;
    private boolean showChannelIcons;

    public interface Callback {
        void onItemClicked(Channel channel);
    }

    public ChannelListSelectionAdapter(Context context, List<Channel> channelList) {
        this.context = context;
        this.channelList = channelList;
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(parent.getContext());
        showChannelIcons = Boolean.valueOf(sharedPreferences.getString("channel_icons_enabled", "true"));

        final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.channel_list_selection_dialog_adapter, parent, false);
        return new ViewHolder(view, this);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final Channel channel = channelList.get(position);
        if (channel != null) {
            holder.itemView.setTag(channel);
            if (holder.iconImageView != null) {

                Glide.with(context).load(UIUtils.getIconUrl(context, channel.getIcon())).into(holder.iconImageView);

                //Bitmap iconBitmap = UIUtils.getCachedIcon(context, channel.getIcon());
                //holder.icon.setImageBitmap(iconBitmap);
                //holder.iconImageView.setVisibility(iconBitmap != null && showChannelIcons ? ImageView.VISIBLE : ImageView.GONE);
            }
            if (holder.titleTextView != null) {
                holder.titleTextView.setText(channel.getName());
                holder.titleTextView.setTag(position);
            }
        }
    }

    @Override
    public int getItemCount() {
        return channelList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        final ImageView iconImageView;
        final TextView titleTextView;
        final ChannelListSelectionAdapter channelListAdapter;

        ViewHolder(View view, ChannelListSelectionAdapter adapter) {
            super(view);
            iconImageView = view.findViewById(R.id.icon);
            titleTextView = view.findViewById(R.id.title);
            channelListAdapter = adapter;
            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (channelListAdapter != null && channelListAdapter.callback != null) {
                Channel channel = channelListAdapter.channelList.get(getAdapterPosition());
                channelListAdapter.callback.onItemClicked(channel);
            }
        }
    }
}
