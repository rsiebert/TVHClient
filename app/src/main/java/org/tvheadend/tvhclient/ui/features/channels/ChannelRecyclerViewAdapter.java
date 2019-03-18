package org.tvheadend.tvhclient.ui.features.channels;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.domain.entity.Channel;
import org.tvheadend.tvhclient.domain.entity.Recording;
import org.tvheadend.tvhclient.databinding.ChannelListAdapterBinding;
import org.tvheadend.tvhclient.ui.common.callbacks.RecyclerViewClickCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

public class ChannelRecyclerViewAdapter extends RecyclerView.Adapter<ChannelRecyclerViewAdapter.ChannelViewHolder> implements Filterable {

    private final RecyclerViewClickCallback clickCallback;
    private final boolean isDualPane;
    private List<Recording> recordingList = new ArrayList<>();
    private final List<Channel> channelList = new ArrayList<>();
    private List<Channel> channelListFiltered = new ArrayList<>();
    private int selectedPosition = 0;

    ChannelRecyclerViewAdapter(boolean isDualPane, RecyclerViewClickCallback clickCallback) {
        this.clickCallback = clickCallback;
        this.isDualPane = isDualPane;
    }

    @NonNull
    @Override
    public ChannelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        ChannelListAdapterBinding itemBinding = ChannelListAdapterBinding.inflate(layoutInflater, parent, false);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(parent.getContext());
        boolean showChannelName = sharedPreferences.getBoolean("channel_name_enabled", parent.getContext().getResources().getBoolean(R.bool.pref_default_channel_name_enabled));
        boolean showProgressbar = sharedPreferences.getBoolean("program_progressbar_enabled", parent.getContext().getResources().getBoolean(R.bool.pref_default_program_progressbar_enabled));
        boolean showProgramSubtitle = sharedPreferences.getBoolean("program_subtitle_enabled", parent.getContext().getResources().getBoolean(R.bool.pref_default_program_subtitle_enabled));
        boolean showNextProgramTitle = sharedPreferences.getBoolean("next_program_title_enabled", parent.getContext().getResources().getBoolean(R.bool.pref_default_next_program_title_enabled));
        boolean showGenreColors = sharedPreferences.getBoolean("genre_colors_for_channels_enabled", parent.getContext().getResources().getBoolean(R.bool.pref_default_genre_colors_for_channels_enabled));

        return new ChannelViewHolder(itemBinding, showChannelName, showProgramSubtitle, showNextProgramTitle, showProgressbar, showGenreColors, isDualPane);
    }

    @Override
    public void onBindViewHolder(@NonNull ChannelViewHolder holder, int position) {
        if (channelListFiltered.size() > position) {
            Channel channel = channelListFiltered.get(position);
            holder.bind(channel, position, (selectedPosition == position), clickCallback);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ChannelViewHolder holder, int position, @NonNull List<Object> payloads) {
        onBindViewHolder(holder, position);
    }

    void addItems(@NonNull List<Channel> newItems) {
        updateRecordingState(newItems, recordingList);

        List<Channel> oldItems = new ArrayList<>(channelListFiltered);
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new ChannelListDiffCallback(oldItems, newItems));

        channelList.clear();
        channelListFiltered.clear();
        channelList.addAll(newItems);
        channelListFiltered.addAll(newItems);
        diffResult.dispatchUpdatesTo(this);

        if (selectedPosition > channelListFiltered.size()) {
            selectedPosition = 0;
        }
    }

    @Override
    public int getItemCount() {
        return channelListFiltered != null ? channelListFiltered.size() : 0;
    }

    @Override
    public int getItemViewType(final int position) {
        return R.layout.channel_list_adapter;
    }

    public void setPosition(int pos) {
        notifyItemChanged(selectedPosition);
        selectedPosition = pos;
        notifyItemChanged(pos);
    }

    public Channel getItem(int position) {
        if (channelListFiltered.size() > position && position >= 0) {
            return channelListFiltered.get(position);
        } else {
            return null;
        }
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {
                String charString = charSequence.toString();
                if (charString.isEmpty()) {
                    channelListFiltered = channelList;
                } else {
                    List<Channel> filteredList = new ArrayList<>();
                    // Iterate over the available channels. Use a copy on write
                    // array in case the channel list changes during filtering.
                    for (@NonNull Channel channel : new CopyOnWriteArrayList<>(channelList)) {
                        // name match condition. this might differ depending on your requirement
                        // here we are looking for a channel name match
                        if (channel.getName() != null
                                && channel.getName().toLowerCase().contains(charString.toLowerCase())) {
                            filteredList.add(channel);
                        } else if (channel.getProgramTitle() != null
                                && channel.getProgramTitle().toLowerCase().contains(charString.toLowerCase())) {
                            filteredList.add(channel);
                        } else if (channel.getProgramSubtitle() != null
                                && channel.getProgramSubtitle().toLowerCase().contains(charString.toLowerCase())) {
                            filteredList.add(channel);
                        } else if (channel.getNextProgramTitle() != null
                                && channel.getNextProgramTitle().toLowerCase().contains(charString.toLowerCase())) {
                            filteredList.add(channel);
                        }
                    }
                    channelListFiltered = filteredList;
                }

                FilterResults filterResults = new FilterResults();
                filterResults.values = channelListFiltered;
                return filterResults;
            }

            @SuppressWarnings("unchecked")
            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                channelListFiltered = (ArrayList<Channel>) filterResults.values;
                notifyDataSetChanged();
            }
        };
    }

    /**
     * Whenever a recording changes in the database the list of available recordings are
     * saved in this recycler view. The previous list is cleared to avoid showing outdated
     * recording states. Each recording is checked if it belongs to the
     * currently shown program. If yes then its state is updated.
     *
     * @param list List of recordings
     */
    void addRecordings(@NonNull List<Recording> list) {
        recordingList.clear();
        recordingList.addAll(list);
        updateRecordingState(channelListFiltered, recordingList);
    }

    private void updateRecordingState(@NonNull List<Channel> channels, @NonNull List<Recording> recordings) {
        for (int i = 0; i < channels.size(); i++) {
            Channel channel = channels.get(i);
            boolean recordingExists = false;

            for (Recording recording : recordings) {
                if (channel.getProgramId() > 0 && channel.getProgramId() == recording.getEventId()) {
                    Recording oldRecording = channel.getRecording();
                    channel.setRecording(recording);

                    // Do a full update only when a new recording was added or the recording
                    // state has changed which results in a different recording state icon
                    // Otherwise do not update the UI
                    if (oldRecording == null
                            || (!TextUtils.equals(oldRecording.getError(), recording.getError())
                            || !TextUtils.equals(oldRecording.getState(), recording.getState()))) {
                        notifyItemChanged(i);
                    }
                    recordingExists = true;
                    break;
                }
            }
            if (!recordingExists && channel.getRecording() != null) {
                channel.setRecording(null);
                notifyItemChanged(i);
            }
            channels.set(i, channel);
        }
    }

    static class ChannelViewHolder extends RecyclerView.ViewHolder {

        private final ChannelListAdapterBinding binding;
        private boolean showChannelName;
        private boolean showProgramSubtitle;
        private boolean showProgressBar;
        private boolean showNextProgramTitle;
        private final boolean showGenreColors;
        private final boolean isDualPane;

        ChannelViewHolder(ChannelListAdapterBinding binding,
                          boolean showChannelName,
                          boolean showProgramSubtitle,
                          boolean showNextProgramTitle,
                          boolean showProgressBar,
                          boolean showGenreColors,
                          boolean isDualPane) {
            super(binding.getRoot());
            this.binding = binding;
            this.showChannelName = showChannelName;
            this.showNextProgramTitle = showNextProgramTitle;
            this.showProgramSubtitle = showProgramSubtitle;
            this.showProgressBar = showProgressBar;
            this.showGenreColors = showGenreColors;
            this.isDualPane = isDualPane;
        }

        void bind(Channel channel, int position, boolean isSelected, RecyclerViewClickCallback clickCallback) {
            binding.setChannel(channel);
            binding.setPosition(position);
            binding.setIsSelected(isSelected);
            binding.setShowChannelName(showChannelName);
            binding.setShowProgramSubtitle(showProgramSubtitle);
            binding.setShowProgressBar(showProgressBar);
            binding.setShowNextProgramTitle(showNextProgramTitle);
            binding.setShowGenreColor(showGenreColors);
            binding.setIsDualPane(isDualPane);
            binding.setCallback(clickCallback);
            binding.executePendingBindings();
        }
    }
}
