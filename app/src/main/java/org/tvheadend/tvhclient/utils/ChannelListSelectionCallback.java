package org.tvheadend.tvhclient.utils;

import org.tvheadend.tvhclient.data.entity.Channel;

public interface ChannelListSelectionCallback {
    void onChannelIdSelected(Channel channel);
}
