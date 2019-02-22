package org.tvheadend.tvhclient.ui.features.dialogs;

import android.content.Context;
import android.preference.PreferenceManager;

import com.afollestad.materialdialogs.MaterialDialog;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.domain.entity.ChannelTag;
import org.tvheadend.tvhclient.ui.features.channels.ChannelDisplayOptionListener;

import java.util.List;

import androidx.annotation.NonNull;

public class ChannelTagSelectionDialog {

    public static boolean showDialog(Context context, List<ChannelTag> channelTags, int channelCount, @NonNull ChannelDisplayOptionListener callback) {

        boolean isMultipleChoice = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("multiple_channel_tags_enabled",
                context.getResources().getBoolean(R.bool.pref_default_multiple_channel_tags_enabled));

        // Create a default tag (All channels)
        if (!isMultipleChoice) {
            ChannelTag tag = new ChannelTag();
            tag.setTagId(0);
            tag.setTagName(context.getString(R.string.all_channels));
            tag.setChannelCount(channelCount);
            boolean allChannelsSelected = true;
            for (ChannelTag channelTag : channelTags) {
                if (channelTag.isSelected()) {
                    allChannelsSelected = false;
                    break;
                }
            }
            tag.setSelected(allChannelsSelected);
            channelTags.add(0, tag);
        }

        ChannelTagSelectionAdapter adapter = new ChannelTagSelectionAdapter(channelTags, isMultipleChoice);

        // Show the dialog that shows all available channel tags. When the
        // user has selected a tag, restart the loader to loadRecordingById the updated channel list
        MaterialDialog.Builder builder = new MaterialDialog.Builder(context)
                .title(R.string.tags)
                .adapter(adapter, null);

        if (isMultipleChoice) {
            builder.content("Select one or more channel tags for a subset of channels. Otherwise all channels will be displayed.")
                    .positiveText(R.string.save)
                    .onPositive((dialog, which) -> callback.onChannelTagIdsSelected(adapter.getSelectedTagIds()));
        } else {
            builder.dismissListener(dialog -> callback.onChannelTagIdsSelected(adapter.getSelectedTagIds()));
        }

        MaterialDialog dialog = builder.build();
        adapter.setCallback(dialog);
        dialog.show();
        return true;
    }
}
