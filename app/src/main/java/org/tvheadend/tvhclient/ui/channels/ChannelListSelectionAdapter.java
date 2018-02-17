package org.tvheadend.tvhclient.ui.channels;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.preference.PreferenceManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.entity.Channel;
import org.tvheadend.tvhclient.utils.MiscUtils;

import java.util.List;

public class ChannelListSelectionAdapter extends RecyclerView.Adapter<ChannelListSelectionAdapter.ViewHolder> {

    private final Context context;
    private Callback callback;
    private List<Channel> channelList;
    private long selectedChannelId;
    private boolean showChannelIcons;

    public interface Callback {
        void onItemClicked(int index);
    }

    public ChannelListSelectionAdapter(Context context, List<Channel> channelList, long selectedChannelId) {
        this.context = context;
        this.channelList = channelList;
        this.selectedChannelId = selectedChannelId;
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(parent.getContext());
        showChannelIcons = prefs.getBoolean("showChannelIconPref", true);

        final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.channel_list_selection_dialog_adapter, parent, false);
        return new ViewHolder(view, this);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final Channel channel = channelList.get(position);
        if (channel != null) {
            holder.itemView.setTag(channel);
            if (holder.icon != null) {
                Bitmap iconBitmap = MiscUtils.getCachedIcon(context, channel.getChannelIcon());
                holder.icon.setImageBitmap(iconBitmap);
                holder.icon.setVisibility(iconBitmap != null && showChannelIcons ? ImageView.VISIBLE : ImageView.GONE);
            }
            if (holder.title != null) {
                holder.title.setText(channel.getChannelName());
                holder.title.setTag(position);
            }
        }
    }

    @Override
    public int getItemCount() {
        return channelList.size();
    }

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        final ImageView icon;
        final TextView title;
        final ChannelListSelectionAdapter channelListAdapter;

        ViewHolder(View view, ChannelListSelectionAdapter adapter) {
            super(view);
            icon = view.findViewById(R.id.icon);
            title = view.findViewById(R.id.title);
            channelListAdapter = adapter;
            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (channelListAdapter != null && channelListAdapter.callback != null) {
                Channel channel = channelListAdapter.channelList.get(getAdapterPosition());
                channelListAdapter.callback.onItemClicked(channel.getChannelId());
            }
        }
    }
}
