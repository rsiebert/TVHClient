package org.tvheadend.tvhclient.ui.common

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.tvheadend.data.entity.ChannelTag
import org.tvheadend.tvhclient.BR
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.databinding.GenreColorListAdapterBinding
import org.tvheadend.tvhclient.ui.common.interfaces.ChannelTagIdsSelectedInterface
import org.tvheadend.tvhclient.ui.common.interfaces.ChannelTimeSelectedInterface
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

/**
 * Creates a [RecyclerView] that hosts the given adapter so it can be used as the
 * custom view of a [MaterialAlertDialogBuilder].
 *
 * [MaterialAlertDialogBuilder] has no direct RecyclerView adapter support (unlike the
 * former material-dialogs library). The existing adapters are kept and simply wrapped in
 * a RecyclerView that is passed via [MaterialAlertDialogBuilder.setView].
 */
fun buildRecyclerView(context: Context, adapter: RecyclerView.Adapter<*>): RecyclerView {
    val recyclerView = RecyclerView(context)
    recyclerView.layoutManager = LinearLayoutManager(context)
    recyclerView.adapter = adapter
    // Deliberately no setHasFixedSize(true): the RecyclerView is sized wrap_content, so its size
    // does change with the adapter contents and claiming otherwise is both wrong and flagged by
    // lint (InvalidSetHasFixedSize).
    // Explicit layout params are required so the RecyclerView is shown when it is added to
    // the alert dialog's custom panel (which is a FrameLayout inside a scroll view).
    recyclerView.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT)
    return recyclerView
}

fun showChannelTagSelectionDialog(context: Context, channelTags: MutableList<ChannelTag>, channelCount: Int, callback: ChannelTagIdsSelectedInterface): Boolean {
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
    val builder = MaterialAlertDialogBuilder(context)
            .setTitle(if (isMultipleChoice) R.string.select_multiple_channel_tags else R.string.filter_channel_list)
            .setView(buildRecyclerView(context, adapter))

    if (isMultipleChoice) {
        // In multiple choice mode the selection is confirmed via the "save" button.
        builder.setPositiveButton(R.string.save) { _, _ -> callback.onChannelTagIdsSelected(adapter.selectedTagIds) }
    }

    val dialog = builder.create()
    if (!isMultipleChoice) {
        // In single choice mode the adapter dismisses the dialog and the
        // selection is reported when the dialog is dismissed.
        dialog.setOnDismissListener { callback.onChannelTagIdsSelected(adapter.selectedTagIds) }
    }

    adapter.setCallback(dialog)
    dialog.show()
    return true
}

class ChannelTagSelectionAdapter(private val channelTagList: List<ChannelTag>, private val isMultiChoice: Boolean) : RecyclerView.Adapter<ChannelTagSelectionAdapter.ViewHolder>() {

    private val selectedChannelTagIds: MutableSet<Int>
    private lateinit var dialog: AlertDialog

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

    fun setCallback(dialog: AlertDialog) {
        this.dialog = dialog
    }

    fun onChecked(@Suppress("UNUSED_PARAMETER") view: View, position: Int, isChecked: Boolean) {
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
 * Shows the genre colors next to the genre names, so it is clear which color stands for which
 * kind of program.
 *
 * The explanation is part of the custom view rather than a dialog message, because
 * [MaterialAlertDialogBuilder] offers no supporting text API and setting a message alongside a
 * custom view is not guaranteed to render both.
 */
fun showGenreColorDialog(context: Context): Boolean {
    val listAdapter = GenreColorListAdapter(context.resources.getStringArray(R.array.pr_content_type0))
    val legend = LayoutInflater.from(context).inflate(R.layout.genre_color_legend, null, false)
    legend.findViewById<RecyclerView>(R.id.genre_color_list).apply {
        layoutManager = LinearLayoutManager(context)
        // See buildRecyclerView: the list is wrap_content, so setHasFixedSize would be a lie.
        // Qualified because an unqualified "adapter" resolves to RecyclerView.adapter here.
        this.adapter = listAdapter
    }
    // An inflated root has no layout params of its own; without these the dialog's custom panel
    // can end up measuring the legend against the wrong bounds.
    legend.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT)
    MaterialAlertDialogBuilder(context)
            .setTitle(R.string.genre_color_info)
            .setView(legend)
            .show()
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

fun showProgramTimeframeSelectionDialog(context: Context, currentSelection: Int, intervalInHours: Int, maxIntervalsToShow: Int, callback: ChannelTimeSelectedInterface?): AlertDialog {

    val startDateFormat = SimpleDateFormat("dd.MM.yyyy - HH:00", Locale.US)
    val endDateFormat = SimpleDateFormat("HH:00", Locale.US)

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

    val dialog = MaterialAlertDialogBuilder(context)
            .setTitle(R.string.select_time)
            .setSingleChoiceItems(times.toTypedArray(), currentSelection) { _, index ->
                callback?.onTimeSelected(index)
            }
            .create()
    dialog.show()
    return dialog
}

fun showChannelSortOrderSelectionDialog(context: Context): Boolean {

    val channelSortOrder = Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(context).getString("channel_sort_order", context.resources.getString(R.string.pref_default_channel_sort_order))!!)
    val sortOrderNames = context.resources.getStringArray(R.array.pref_sort_channels_names)
    MaterialAlertDialogBuilder(context)
            .setTitle(R.string.pref_sort_channels)
            .setSingleChoiceItems(sortOrderNames, channelSortOrder) { dialog, index ->
                Timber.d("New selected channel sort order changed from $channelSortOrder to $index")
                val editor = PreferenceManager.getDefaultSharedPreferences(context).edit()
                editor.putString("channel_sort_order", index.toString())
                editor.apply()
                // The order is applied as soon as an entry is picked, so there is nothing left to
                // confirm. Closing here also matches the ListPreference for this same setting.
                dialog.dismiss()
            }
            .show()
    return true
}

fun showCompletedRecordingSortOrderSelectionDialog(context: Context): Boolean {

    val sortOrder = Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(context).getString("completed_recording_sort_order", context.resources.getString(R.string.pref_default_completed_recording_sort_order))!!)
    val sortOrderNames = context.resources.getStringArray(R.array.pref_sort_completed_recordings_names)
    MaterialAlertDialogBuilder(context)
            .setTitle(R.string.pref_sort_completed_recordings)
            .setSingleChoiceItems(sortOrderNames, sortOrder) { dialog, index ->
                Timber.d("New selected completed recording sort order changed from $sortOrder to $index")
                val editor = PreferenceManager.getDefaultSharedPreferences(context).edit()
                editor.putString("completed_recording_sort_order", index.toString())
                editor.apply()
                // See showChannelSortOrderSelectionDialog: the choice applies immediately.
                dialog.dismiss()
            }
            .show()
    return true
}
