package org.tvheadend.tvhclient.ui.features.dvr;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.domain.entity.Channel;
import org.tvheadend.tvhclient.util.MiscUtils;

import java.lang.ref.WeakReference;
import java.util.List;

// TODO convert to data binding

public class ChannelListSelectionAdapter extends RecyclerView.Adapter<ChannelListSelectionAdapter.ViewHolder> {

    private final WeakReference<Context> context;
    private Callback callback;
    private final List<Channel> channelList;

    interface Callback {
        void onItemClicked(Channel channel);
    }

    ChannelListSelectionAdapter(Context context, List<Channel> channelList) {
        this.context = new WeakReference<>(context);
        this.channelList = channelList;
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.channel_list_selection_dialog_adapter, parent, false);
        return new ViewHolder(view, this);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final Channel channel = channelList.get(position);
        if (channel != null) {
            holder.itemView.setTag(channel);
            Context context = this.context.get();
            if (context != null && holder.iconImageView != null && !TextUtils.isEmpty(channel.getIcon())) {
                Picasso.get()
                        .load(MiscUtils.getIconUrl(context, channel.getIcon()))
                        .into(holder.iconImageView);
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
