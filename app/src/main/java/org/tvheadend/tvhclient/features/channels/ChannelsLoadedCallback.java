package org.tvheadend.tvhclient.features.channels;

import org.tvheadend.tvhclient.data.entity.Channel;

import java.util.List;

public interface ChannelsLoadedCallback {
    void onChannelsLoaded(List<Channel> channels);
}
