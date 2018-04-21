package org.tvheadend.tvhclient.features.shared.callbacks;

import org.tvheadend.tvhclient.data.entity.Channel;

public interface ChannelListSelectionCallback {
    void onChannelIdSelected(Channel channel);
}
