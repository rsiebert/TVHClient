package org.tvheadend.tvhclient.features.dvr;

import org.tvheadend.tvhclient.data.entity.Channel;

public interface RecordingConfigSelectedListener {

    void onChannelSelected(Channel channel);
    
    void onProfileSelected(int which);

    void onPrioritySelected(int which);

    void onDaysSelected(int selectedDays);
}
