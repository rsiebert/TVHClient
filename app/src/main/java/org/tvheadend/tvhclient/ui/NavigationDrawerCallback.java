package org.tvheadend.tvhclient.ui;

import com.mikepenz.materialdrawer.model.interfaces.IProfile;

public interface NavigationDrawerCallback {

    void onNavigationProfileSelected(IProfile profile);

    void onNavigationMenuSelected(int id);
}
