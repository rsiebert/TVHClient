package org.tvheadend.tvhclient.features.shared.adapter;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.afollestad.materialdialogs.MaterialDialog;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.entity.ChannelTag;

import java.util.List;

public class ChannelTagRecyclerViewAdapter extends RecyclerView.Adapter<ChannelTagViewHolder> {

    private final boolean isMultiChoice;
    private List<ChannelTag> channelTagList;
    private Integer[] selectedChannelTagIds;
    private final int channelCount;
    private MaterialDialog dialog;

    public ChannelTagRecyclerViewAdapter(@NonNull List<ChannelTag> channelTagList, @NonNull Integer[] selectedChannelTagIds, int channelCount, boolean isMultiChoice) {
        this.channelTagList = channelTagList;
        this.selectedChannelTagIds = selectedChannelTagIds;
        this.channelCount = channelCount;
        this.isMultiChoice = isMultiChoice;
    }

    @NonNull
    @Override
    public ChannelTagViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final View view = LayoutInflater.from(parent.getContext()).inflate(viewType, parent, false);
        return new ChannelTagViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChannelTagViewHolder holder, int position) {
        if (channelTagList.size() > position) {
            ChannelTag channelTag = channelTagList.get(position);
            // Check if the tag id is in the selected tag list.
            boolean isSelected = false;
            for (int tagId : selectedChannelTagIds) {
                if (tagId == channelTag.getTagId()) {
                    isSelected = true;
                    break;
                }
            }
            holder.bindData(channelTag, channelCount, isSelected);
            if (isMultiChoice) {
                if (holder.selectedCheckBox != null) {
                    if (holder.selectedCheckBox.isChecked()) {
                        selectedChannelTagIds[position] = -1;
                    } else {
                        selectedChannelTagIds[position] = channelTagList.get(position).getTagId();
                    }
                }
            } else {
                if (holder.selectedRadioButton != null) {
                    holder.selectedRadioButton.setOnClickListener(v -> {
                        selectedChannelTagIds[0] = channelTagList.get(position).getTagId();
                        dialog.dismiss();
                    });
                }
                holder.itemView.setOnClickListener(v -> {
                    selectedChannelTagIds[0] = channelTagList.get(position).getTagId();
                    dialog.dismiss();
                });
            }
        }
    }

    @Override
    public int getItemCount() {
        return channelTagList != null ? channelTagList.size() : 0;
    }

    @Override
    public int getItemViewType(final int position) {
        return isMultiChoice ? R.layout.channeltag_list_multiple_choice_adapter : R.layout.channeltag_list_single_choice_adapter;
    }

    public int getSelectedTagId() {
        return selectedChannelTagIds[0];
    }

    public Integer[] getSelectedTagIds() {
        return selectedChannelTagIds;
    }

    public void setCallback(MaterialDialog dialog) {
        this.dialog = dialog;
    }
}
