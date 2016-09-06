package org.tvheadend.tvhclient.adapter;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.model.ChannelTag;

import java.util.List;

public class ChannelTagListAdapter extends RecyclerView.Adapter<ChannelTagListAdapter.ViewHolder> {

    private Callback mCallback;
    private List<ChannelTag> list;
    private boolean showIcons;

    public interface Callback {
        void onItemClicked(int index);
    }

    public ChannelTagListAdapter(List<ChannelTag> list) {
        this.list = list;
    }

    public void setCallback(Callback mCallback) {
        this.mCallback = mCallback;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(parent.getContext());
        showIcons = prefs.getBoolean("showTagIconPref", true);

        final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.channeltag_list_item, parent, false);
        return new ViewHolder(view, this);
    }

    @Override
    /**
     * Applies the values to the available layout items
     */
    public void onBindViewHolder(ViewHolder holder, int position) {
        final ChannelTag item = list.get(position);
        if (item != null) {
            if (holder.icon != null) {
                holder.icon.setImageBitmap((item.iconBitmap != null) ? item.iconBitmap : null);
                holder.icon.setVisibility(showIcons ? ImageView.VISIBLE : ImageView.GONE);
            }
            if (holder.title != null) {
                holder.title.setText(item.name);
                holder.title.setTag(position);
            }
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        final ImageView icon;
        final TextView title;
        final ChannelTagListAdapter adapter;

        public ViewHolder(View view, ChannelTagListAdapter adapter) {
            super(view);
            this.icon = (ImageView) view.findViewById(R.id.icon);
            this.title = (TextView) view.findViewById(R.id.title);

            this.adapter = adapter;
            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (adapter.mCallback == null) {
                return;
            }
            adapter.mCallback.onItemClicked(getAdapterPosition());
        }
    }
}
