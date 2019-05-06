package org.tvheadend.tvhclient.ui.features.dialogs

import android.content.Context
import androidx.preference.PreferenceManager

import com.afollestad.materialdialogs.MaterialDialog

import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.domain.entity.ChannelTag
import org.tvheadend.tvhclient.ui.features.channels.ChannelDisplayOptionListener

fun showChannelTagSelectionDialog(context: Context, channelTags: MutableList<ChannelTag>, channelCount: Int, callback: ChannelDisplayOptionListener): Boolean {

    val isMultipleChoice = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("multiple_channel_tags_enabled",
            context.resources.getBoolean(R.bool.pref_default_multiple_channel_tags_enabled))

    // Create a default tag (All channels)
    if (!isMultipleChoice) {
        val tag = ChannelTag()
        tag.tagId = 0
        tag.tagName = context.getString(R.string.all_channels)
        tag.channelCount = channelCount
        var allChannelsSelected = true
        for (channelTag in channelTags) {
            if (channelTag.isSelected) {
                allChannelsSelected = false
                break
            }
        }
        tag.isSelected = allChannelsSelected
        channelTags.add(0, tag)
    }

    val adapter = ChannelTagSelectionAdapter(channelTags, isMultipleChoice)

    // Show the dialog that shows all available channel tags. When the
    // user has selected a tag, restart the loader to loadRecordingById the updated channel list
    val builder = MaterialDialog.Builder(context)
            .title(R.string.tags)
            .adapter(adapter, null)

    if (isMultipleChoice) {
        builder.content("Select one or more channel tags for a subset of channels. Otherwise all channels will be displayed.")
                .positiveText(R.string.save)
                .onPositive { _, _ -> callback.onChannelTagIdsSelected(adapter.selectedTagIds) }
    } else {
        builder.dismissListener { callback.onChannelTagIdsSelected(adapter.selectedTagIds) }
    }

    val dialog = builder.build()
    adapter.setCallback(dialog)
    dialog.show()
    return true
}

