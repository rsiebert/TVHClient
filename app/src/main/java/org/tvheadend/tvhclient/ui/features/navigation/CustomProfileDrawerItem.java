package org.tvheadend.tvhclient.ui.features.navigation;

import android.view.View;
import android.widget.TextView;

import com.mikepenz.materialdrawer.holder.StringHolder;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;

import org.tvheadend.tvhclient.R;

import java.util.List;

class CustomProfileDrawerItem extends ProfileDrawerItem {

    public void bindView(ViewHolder viewHolder, List payloads) {
        super.bindView(viewHolder, payloads);

        TextView name = viewHolder.itemView.findViewById(R.id.material_drawer_name);
        name.setVisibility(View.VISIBLE);
        StringHolder.applyTo(this.getName(), name);

        TextView email = viewHolder.itemView.findViewById(R.id.material_drawer_email);
        email.setVisibility(View.VISIBLE);
        StringHolder.applyTo(this.getEmail(), email);

        //call the onPostBindView method to trigger post bind view actions (like the listener to modify the item if required)
        onPostBindView(this, viewHolder.itemView);
    }
}
