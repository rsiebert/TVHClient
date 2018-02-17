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
import android.widget.RadioButton;
import android.widget.TextView;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.entity.ChannelTag;
import org.tvheadend.tvhclient.utils.MiscUtils;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ChannelTagListAdapter extends RecyclerView.Adapter<ChannelTagListAdapter.ViewHolder> {

    private final Context context;
    private Callback callback;
    private List<ChannelTag> channelTagList;
    private ChannelTag selectedChannelTag;
    private boolean showChannelTagIcons;

    public interface Callback {
        void onItemClicked(int index);
    }

    public ChannelTagListAdapter(Context context, List<ChannelTag> channelTagList, ChannelTag selectedChannelTag) {
        this.context = context;
        this.channelTagList = channelTagList;
        this.selectedChannelTag = selectedChannelTag;
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(parent.getContext());
        showChannelTagIcons = prefs.getBoolean("showTagIconPref", true);

        final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.channeltag_list_selection_dialog_adapter, parent, false);
        return new ViewHolder(view, this);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final ChannelTag channelTag = channelTagList.get(position);
        if (channelTag != null) {

            if ((selectedChannelTag == null && position == 0)
                    || (selectedChannelTag != null && channelTag.getTagId() == selectedChannelTag.getTagId())) {
                holder.radioButton.setChecked(true);
            } else {
                holder.radioButton.setChecked(false);
            }

            Bitmap iconBitmap = MiscUtils.getCachedIcon(context, channelTag.getTagIcon());
            holder.iconImageView.setImageBitmap(iconBitmap);
            holder.iconImageView.setVisibility(iconBitmap != null && showChannelTagIcons ? ImageView.VISIBLE : ImageView.GONE);
            holder.titleTextView.setText(channelTag.getTagName());
            if (channelTag.getTagId() > 0) {
                holder.countTextView.setText(String.valueOf(channelTag.getChannelCount()));
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
            if (channelTagListAdapter != null && channelTagListAdapter.callback != null) {
                int id = channelTagListAdapter.channelTagList.get(getAdapterPosition()).getTagId();
                channelTagListAdapter.callback.onItemClicked(id);
            }
        }
    }
}
