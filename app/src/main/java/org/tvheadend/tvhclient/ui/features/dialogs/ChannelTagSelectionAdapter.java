package org.tvheadend.tvhclient.ui.features.dialogs;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.afollestad.materialdialogs.MaterialDialog;

import org.tvheadend.tvhclient.BR;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.domain.entity.ChannelTag;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.recyclerview.widget.RecyclerView;

// TODO use a viewmodel or change how the callback is handled in the menu utils class

public class ChannelTagSelectionAdapter extends RecyclerView.Adapter<ChannelTagSelectionAdapter.ViewHolder> {

    private final boolean isMultiChoice;
    private final List<ChannelTag> channelTagList;
    private final Set<Integer> selectedChannelTagIds;
    private MaterialDialog dialog;

    public ChannelTagSelectionAdapter(@NonNull List<ChannelTag> channelTagList, boolean isMultiChoice) {
        this.channelTagList = channelTagList;
        this.isMultiChoice = isMultiChoice;
        this.selectedChannelTagIds = new HashSet<>();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        ViewDataBinding binding = DataBindingUtil.inflate(layoutInflater, viewType, parent, false);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(parent.getContext());
        boolean showChannelTagIcons = sharedPreferences.getBoolean("channel_tag_icons_enabled",
                parent.getContext().getResources().getBoolean(R.bool.pref_default_channel_tag_icons_enabled));

        return new ChannelTagSelectionAdapter.ViewHolder(binding, showChannelTagIcons, this);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (channelTagList.size() > position) {
            ChannelTag channelTag = channelTagList.get(position);
            holder.bind(channelTag, position);

            if (channelTag.isSelected()) {
                selectedChannelTagIds.add(channelTag.getTagId());
            }
        }
    }

    @Override
    public int getItemCount() {
        return channelTagList.size();
    }

    @Override
    public int getItemViewType(final int position) {
        return isMultiChoice ? R.layout.channeltag_list_multiple_choice_adapter : R.layout.channeltag_list_single_choice_adapter;
    }

    Set<Integer> getSelectedTagIds() {
        return selectedChannelTagIds;
    }

    public void setCallback(MaterialDialog dialog) {
        this.dialog = dialog;
    }

    public void onChecked(@SuppressWarnings("unused") View view, int position, boolean isChecked) {
        int tagId = channelTagList.get(position).getTagId();
        if (isChecked) {
            selectedChannelTagIds.add(tagId);
        } else {
            selectedChannelTagIds.remove(tagId);
        }
    }

    public void onSelected(int position) {
        int tagId = channelTagList.get(position).getTagId();
        selectedChannelTagIds.clear();
        if (position != 0) {
            selectedChannelTagIds.add(tagId);
        }
        dialog.dismiss();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ViewDataBinding binding;
        private final boolean showChannelTagIcons;
        private final ChannelTagSelectionAdapter callback;

        ViewHolder(ViewDataBinding binding, boolean showChannelTagIcons, ChannelTagSelectionAdapter callback) {
            super(binding.getRoot());
            this.binding = binding;
            this.showChannelTagIcons = showChannelTagIcons;
            this.callback = callback;
        }

        void bind(ChannelTag channelTag, int position) {
            binding.setVariable(BR.channelTag, channelTag);
            binding.setVariable(BR.position, position);
            binding.setVariable(BR.callback, callback);
            binding.setVariable(BR.showChannelTagIcons, showChannelTagIcons);
        }
    }
}

