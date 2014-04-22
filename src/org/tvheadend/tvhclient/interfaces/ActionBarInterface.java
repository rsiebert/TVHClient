package org.tvheadend.tvhclient.interfaces;

import org.tvheadend.tvhclient.model.Channel;

public interface ActionBarInterface {

    void setActionBarTitle(final String string, final String tag);

    void setActionBarSubtitle(final String string, final String tag);

    void setActionBarIcon(final Channel channel, final String tag);
}
