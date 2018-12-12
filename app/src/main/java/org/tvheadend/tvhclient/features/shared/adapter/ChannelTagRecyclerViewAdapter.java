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
import java.util.Set;

public class ChannelTagRecyclerViewAdapter extends RecyclerView.Adapter<ChannelTagViewHolder> {

    private final boolean isMultiChoice;
    private List<ChannelTag> channelTagList;
    private Set<Integer> selectedChannelTagIds;
    private final int channelCount;
    private MaterialDialog dialog;

    public ChannelTagRecyclerViewAdapter(@NonNull List<ChannelTag> channelTagList, @NonNull Set<Integer> selectedChannelTagIds, int channelCount, boolean isMultiChoice) {
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

            boolean isSelected;
            if (isMultiChoice) {
                // There is no all channels checkbox in the multiple
                // selection mode, so the tag is either in the list or not
                isSelected = selectedChannelTagIds.contains(channelTag.getTagId());
            } else {
                // There is a all channels radio button in the single selection mode.
                // It is considered active if the tag list is empty. If not then one
                // of the other tags are selected
                isSelected = (position == 0 ?
                        selectedChannelTagIds.isEmpty() :
                        selectedChannelTagIds.contains(channelTag.getTagId()));
            }
            holder.bindData(channelTag, channelCount, isSelected);

            // Add the click listeners
            int tagId = channelTagList.get(position).getTagId();
            if (isMultiChoice) {
                if (holder.selectedCheckBox != null) {
                    holder.selectedCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                        if (isChecked) {
                            selectedChannelTagIds.add(tagId);
                        } else {
                            selectedChannelTagIds.remove(tagId);
                        }
                    });
                }
            } else {
                if (holder.selectedRadioButton != null) {
                    holder.selectedRadioButton.setOnClickListener(v -> {
                        selectedChannelTagIds.clear();
                        if (position != 0) {
                            selectedChannelTagIds.add(tagId);
                        }
                        dialog.dismiss();
                    });
                }
                holder.itemView.setOnClickListener(v -> {
                    selectedChannelTagIds.clear();
                    if (position != 0) {
                        selectedChannelTagIds.add(tagId);
                    }
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

    public Set<Integer> getSelectedTagIds() {
        return selectedChannelTagIds;
    }

    public void setCallback(MaterialDialog dialog) {
        this.dialog = dialog;
    }
}
