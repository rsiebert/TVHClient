package org.tvheadend.tvhclient.interfaces;

import org.tvheadend.tvhclient.model.Channel;

public interface ActionBarInterface {

    void setActionBarTitle(final String title, final String tag);

    void setActionBarSubtitle(final String subtitle, final String tag);

    void setActionBarIcon(final Channel channel, final String tag);
}
