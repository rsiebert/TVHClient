package org.tvheadend.tvhclient.ui.features.dialogs

import androidx.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import org.tvheadend.tvhclient.BR
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.domain.entity.ChannelTag
import java.util.*

// TODO use a viewmodel or change how the callback is handled in the menu utils class

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

