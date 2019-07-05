package org.tvheadend.tvhclient.ui.features.settings

import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.domain.entity.Connection
import org.tvheadend.tvhclient.util.getThemeId

internal class ConnectionListAdapter(private val context: AppCompatActivity) : ArrayAdapter<Connection>(context, R.layout.connection_list_adapter) {

    internal class ViewHolder {
        lateinit var title: TextView
        lateinit var summary: TextView
        lateinit var selected: ImageView
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View? {
        var view = convertView
        val holder: ViewHolder

        if (view == null) {
            view = context.layoutInflater.inflate(R.layout.connection_list_adapter, parent, false)
            holder = ViewHolder()
            holder.title = view.findViewById(R.id.title)
            holder.summary = view.findViewById(R.id.summary)
            holder.selected = view.findViewById(R.id.selected)
            view.tag = holder
        } else {
            holder = view.tag as ViewHolder
        }

        // Get the connection and assign all the values
        val c = getItem(position)
        if (c != null) {
            holder.title.text = c.name
            holder.summary.text = c.serverUrl

            // Set the active / inactive icon depending on the theme and selection status
            if (getThemeId(context) == R.style.CustomTheme_Light) {
                holder.selected.setImageResource(if (c.isActive) R.drawable.item_active_light else R.drawable.item_not_active_light)
            } else {
                holder.selected.setImageResource(if (c.isActive) R.drawable.item_active_dark else R.drawable.item_not_active_dark)
            }
        }
        return view
    }
}
