package org.tvheadend.tvhclient.data.repository;

import org.tvheadend.tvhclient.data.source.ChannelData;
import org.tvheadend.tvhclient.data.source.ChannelTagData;
import org.tvheadend.tvhclient.data.source.ConnectionData;
import org.tvheadend.tvhclient.data.source.MiscData;
import org.tvheadend.tvhclient.data.source.ProgramData;
import org.tvheadend.tvhclient.data.source.RecordingData;
import org.tvheadend.tvhclient.data.source.SeriesRecordingData;
import org.tvheadend.tvhclient.data.source.ServerProfileData;
import org.tvheadend.tvhclient.data.source.ServerStatusData;
import org.tvheadend.tvhclient.data.source.TagAndChannelData;
import org.tvheadend.tvhclient.data.source.TimerRecordingData;

public interface RepositoryInterface {

    TimerRecordingData getTimerRecordingData();

    SeriesRecordingData getSeriesRecordingData();

    RecordingData getRecordingData();

    ChannelData getChannelData();

    ChannelTagData getChannelTagData();

    ProgramData getProgramData();

    ConnectionData getConnectionData();

    ServerStatusData getServerStatusData();

    ServerProfileData getServerProfileData();

    TagAndChannelData getTagAndChannelData();

    MiscData getMiscData();
}
