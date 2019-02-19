package org.tvheadend.tvhclient.ui.features.dvr;

import org.tvheadend.tvhclient.domain.entity.Channel;

public interface RecordingConfigSelectedListener {

    void onChannelSelected(Channel channel);
    
    void onProfileSelected(int which);

    void onPrioritySelected(int which);

    void onDaysSelected(int selectedDays);
}
