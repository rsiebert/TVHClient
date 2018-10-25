package org.tvheadend.tvhclient.features.navigation;

import android.view.View;
import android.widget.TextView;

import com.mikepenz.materialdrawer.holder.StringHolder;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;

import org.tvheadend.tvhclient.R;

import java.util.List;

class CustomProfileDrawerItem extends ProfileDrawerItem {

    private boolean emailShown = true;

    public CustomProfileDrawerItem withEmailShown(boolean emailShown) {
        this.emailShown = emailShown;
        return this;
    }

    public boolean isEmailShown() {
        return emailShown;
    }

    public void bindView(ViewHolder viewHolder, List payloads) {
        super.bindView(viewHolder, payloads);

        TextView name = viewHolder.itemView.findViewById(R.id.material_drawer_name);
        if (nameShown) {
            name.setVisibility(View.VISIBLE);
            StringHolder.applyTo(this.getName(), name);
        } else {
            name.setVisibility(View.GONE);
        }

        TextView email = viewHolder.itemView.findViewById(R.id.material_drawer_email);
        if (emailShown) {
            email.setVisibility(View.VISIBLE);
            StringHolder.applyTo(this.getEmail(), email);
        } else {
            email.setVisibility(View.GONE);
        }

        //call the onPostBindView method to trigger post bind view actions (like the listener to modify the item if required)
        onPostBindView(this, viewHolder.itemView);
    }
}
