package org.tvheadend.tvhclient.data.repository;

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
}
