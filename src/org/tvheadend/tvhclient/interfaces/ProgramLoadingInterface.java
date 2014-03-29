package org.tvheadend.tvhclient.interfaces;

import org.tvheadend.tvhclient.model.Channel;

public interface ProgramLoadingInterface {
    public void loadMorePrograms(int tabIndex, Channel channel);
}
