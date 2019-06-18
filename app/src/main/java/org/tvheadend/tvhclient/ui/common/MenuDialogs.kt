package org.tvheadend.tvhclient.ui.common

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.callbacks.onDismiss
import com.afollestad.materialdialogs.list.customListAdapter
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import org.tvheadend.tvhclient.BR
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.databinding.GenreColorListAdapterBinding
import org.tvheadend.tvhclient.domain.entity.ChannelTag
import org.tvheadend.tvhclient.ui.features.channels.ChannelDisplayOptionListener
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*


fun showChannelTagSelectionDialog(context: Context, channelTags: MutableList<ChannelTag>, channelCount: Int, callback: ChannelDisplayOptionListener): Boolean {
    val isMultipleChoice = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context).getBoolean("multiple_channel_tags_enabled",
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
    val dialog: MaterialDialog = MaterialDialog(context)
            .title(R.string.tags)
            .customListAdapter(adapter)

    if (isMultipleChoice) {
        dialog.title(R.string.select_multiple_channel_tags)
                .positiveButton(R.string.save) { callback.onChannelTagIdsSelected(adapter.selectedTagIds) }
    } else {
        dialog.onDismiss { callback.onChannelTagIdsSelected(adapter.selectedTagIds) }
    }

    adapter.setCallback(dialog)
    dialog.show()
    return true
}

class ChannelTagSelectionAdapter(private val channelTagList: List<ChannelTag>, private val isMultiChoice: Boolean) : RecyclerView.Adapter<ChannelTagSelectionAdapter.ViewHolder>() {

    private val selectedChannelTagIds: MutableSet<Int>
    private lateinit var dialog: MaterialDialog

    internal val selectedTagIds: Set<Int>
        get() = selectedChannelTagIds

    init {
        this.selectedChannelTagIds = HashSet()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = DataBindingUtil.inflate<ViewDataBinding>(layoutInflater, viewType, parent, false)

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(parent.context)
        val showChannelTagIcons = sharedPreferences.getBoolean("channel_tag_icons_enabled",
                parent.context.resources.getBoolean(R.bool.pref_default_channel_tag_icons_enabled))

        return ViewHolder(binding, showChannelTagIcons, this)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (channelTagList.size > position) {
            val channelTag = channelTagList[position]
            holder.bind(channelTag, position)

            if (channelTag.isSelected) {
                selectedChannelTagIds.add(channelTag.tagId)
            }
        }
    }

    override fun getItemCount(): Int {
        return channelTagList.size
    }

    override fun getItemViewType(position: Int): Int {
        return if (isMultiChoice) R.layout.channeltag_list_multiple_choice_adapter else R.layout.channeltag_list_single_choice_adapter
    }

    fun setCallback(dialog: MaterialDialog) {
        this.dialog = dialog
    }

    fun onChecked(view: View, position: Int, isChecked: Boolean) {
        val tagId = channelTagList[position].tagId
        if (isChecked) {
            selectedChannelTagIds.add(tagId)
        } else {
            selectedChannelTagIds.remove(tagId)
        }
    }

    fun onSelected(position: Int) {
        val tagId = channelTagList[position].tagId
        selectedChannelTagIds.clear()
        if (position != 0) {
            selectedChannelTagIds.add(tagId)
        }
        dialog.dismiss()
    }

    class ViewHolder(private val binding: ViewDataBinding, private val showChannelTagIcons: Boolean, private val callback: ChannelTagSelectionAdapter) : RecyclerView.ViewHolder(binding.root) {

        fun bind(channelTag: ChannelTag, position: Int) {
            binding.setVariable(BR.channelTag, channelTag)
            binding.setVariable(BR.position, position)
            binding.setVariable(BR.callback, callback)
            binding.setVariable(BR.showChannelTagIcons, showChannelTagIcons)
        }
    }
}


/**
 * Prepares a dialog that shows the available genre colors and the names. In
 * here the data for the adapter is created and the dialog prepared which
 * can be shown later.
 */
fun showGenreColorDialog(context: Context): Boolean {
    val adapter = GenreColorListAdapter(context.resources.getStringArray(R.array.pr_content_type0))
    MaterialDialog(context).show {
        title(R.string.genre_color_list)
        customListAdapter(adapter)
    }
    return true
}

class GenreColorListAdapter internal constructor(private val contentInfo: Array<String>) : RecyclerView.Adapter<GenreColorListAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val itemBinding = GenreColorListAdapterBinding.inflate(layoutInflater, parent, false)
        return ViewHolder(itemBinding)
    }

    override fun getItemCount(): Int {
        return contentInfo.size
    }

    /**
     * Applies the values to the available layout items
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind((position + 1) * 16, contentInfo[position])
    }

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    class ViewHolder(private val binding: GenreColorListAdapterBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(contentType: Int, contentName: String) {
            binding.contentType = contentType
            binding.contentName = contentName
        }
    }
}


fun showProgramTimeframeSelectionDialog(context: Context, currentSelection: Int, intervalInHours: Int, maxIntervalsToShow: Int, callback: ChannelDisplayOptionListener?): Boolean {

    val startDateFormat = SimpleDateFormat("dd.MM.yyyy - HH.00", Locale.US)
    val endDateFormat = SimpleDateFormat("HH.00", Locale.US)

    val times = ArrayList<String>()
    times.add(context.getString(R.string.current_time))

    // Set the time that shall be shown next in the dialog. This is the
    // current time plus the value of the intervalInHours in milliseconds
    var timeInMillis = System.currentTimeMillis() + 1000 * 60 * 60 * intervalInHours

    // Add the date and time to the list. Remove Increase the time in
    // milliseconds for each iteration by the defined intervalInHours
    for (i in 1 until maxIntervalsToShow) {
        val startTime = startDateFormat.format(timeInMillis)
        timeInMillis += (1000 * 60 * 60 * intervalInHours).toLong()
        val endTime = endDateFormat.format(timeInMillis)
        times.add("$startTime - $endTime")
    }

    MaterialDialog(context).show {
        title(R.string.select_time)
        listItemsSingleChoice(items = times, initialSelection = currentSelection) { _, index, _ -> callback?.onTimeSelected(index) }
    }
    return true
}

fun showChannelSortOrderSelectionDialog(context: Context, callback: ChannelDisplayOptionListener): Boolean {

    val channelSortOrder = Integer.valueOf(androidx.preference.PreferenceManager.getDefaultSharedPreferences(context).getString("channel_sort_order", context.resources.getString(R.string.pref_default_channel_sort_order))!!)
    MaterialDialog(context).show {
        title(R.string.select_dvr_config)
        listItemsSingleChoice(R.array.pref_sort_channels_names, initialSelection = channelSortOrder) { _, index, _ ->
            Timber.d("New selected channel sort order changed from $channelSortOrder to $index")
            val editor = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context).edit()
            editor.putString("channel_sort_order", index.toString())
            editor.apply()
            callback.onChannelSortOrderSelected(index)
        }
    }
    return false
}
