package org.tvheadend.tvhclient.ui.features.navigation

import android.view.View
import android.widget.TextView

import com.mikepenz.materialdrawer.holder.StringHolder
import com.mikepenz.materialdrawer.model.ProfileDrawerItem

import org.tvheadend.tvhclient.R

internal class CustomProfileDrawerItem : ProfileDrawerItem() {

    override fun bindView(viewHolder: ProfileDrawerItem.ViewHolder, payloads: List<*>?) {
        super.bindView(viewHolder, payloads as List<*>)

        val name = viewHolder.itemView.findViewById<TextView>(R.id.material_drawer_name)
        name.visibility = View.VISIBLE
        StringHolder.applyTo(this.getName(), name)

        val email = viewHolder.itemView.findViewById<TextView>(R.id.material_drawer_email)
        email.visibility = View.VISIBLE
        StringHolder.applyTo(this.getEmail(), email)

        //call the onPostBindView method to trigger post bind view actions (like the listener to modify the item if required)
        onPostBindView(this, viewHolder.itemView)
    }
}
