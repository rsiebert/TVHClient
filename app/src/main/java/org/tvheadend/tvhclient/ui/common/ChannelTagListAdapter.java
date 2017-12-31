package org.tvheadend.tvhclient.ui.common;

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
import org.tvheadend.tvhclient.data.model.ChannelTag;
import org.tvheadend.tvhclient.utils.MiscUtils;

import java.util.List;

public class ChannelTagListAdapter extends RecyclerView.Adapter<ChannelTagListAdapter.ViewHolder> {

    private final Context context;
    private Callback mCallback;
    private List<ChannelTag> mTagList;
    private long mSelectedTagId;
    private boolean mShowChannelTagIcons;

    public interface Callback {
        void onItemClicked(int index);
    }

    public ChannelTagListAdapter(Context context, List<ChannelTag> mChannelTagList, long selectedTagId) {
        this.context = context;
        mTagList = mChannelTagList;
        mSelectedTagId = selectedTagId;
    }

    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(parent.getContext());
        mShowChannelTagIcons = prefs.getBoolean("showTagIconPref", true);

        final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.channeltag_list_item, parent, false);
        return new ViewHolder(view, this);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final ChannelTag item = mTagList.get(position);
        if (item != null) {
            // TODO highlight the selected tag using mSelectedTagId, its -1 if not set

            if (holder.icon != null) {
                Bitmap iconBitmap = MiscUtils.getCachedIcon(context, item.tagIcon);
                holder.icon.setImageBitmap(iconBitmap);
                holder.icon.setVisibility(iconBitmap != null && mShowChannelTagIcons ? ImageView.VISIBLE : ImageView.GONE);
            }
            if (holder.title != null) {
                holder.title.setText(item.tagName);
                holder.title.setTag(position);
            }
        }
    }

    @Override
    public int getItemCount() {
        return mTagList.size();
    }

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        final ImageView icon;
        final TextView title;
        final ChannelTagListAdapter channelTagListAdapter;

        ViewHolder(View view, ChannelTagListAdapter adapter) {
            super(view);
            icon = view.findViewById(R.id.icon);
            title = view.findViewById(R.id.title);
            channelTagListAdapter = adapter;
            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (channelTagListAdapter != null && channelTagListAdapter.mCallback != null) {
                int id = channelTagListAdapter.mTagList.get(getAdapterPosition()).tagId;
                channelTagListAdapter.mCallback.onItemClicked(id);
            }
        }
    }
}
