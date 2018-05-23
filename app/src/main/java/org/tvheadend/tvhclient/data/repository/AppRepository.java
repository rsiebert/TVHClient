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

import javax.inject.Inject;

public class AppRepository implements RepositoryInterface {

    private final ChannelData channelData;
    private final ProgramData programData;
    private final RecordingData recordingData;
    private final SeriesRecordingData seriesRecordingData;
    private final TimerRecordingData timerRecordingData;
    private final ConnectionData connectionData;
    private final ChannelTagData channelTagData;
    private final ServerStatusData serverStatusData;
    private final ServerProfileData serverProfileData;
    private final TagAndChannelData tagAndChannelData;
    private final MiscData miscData;

    @Inject
    public AppRepository(ChannelData channelData,
                         ProgramData programData,
                         RecordingData recordingData,
                         SeriesRecordingData seriesRecordingData,
                         TimerRecordingData timerRecordingData,
                         ConnectionData connectionData,
                         ChannelTagData channelTagData,
                         ServerStatusData serverStatusData,
                         ServerProfileData serverProfileData,
                         TagAndChannelData tagAndChannelData,
                         MiscData miscData) {
        this.channelData = channelData;
        this.programData = programData;
        this.recordingData = recordingData;
        this.seriesRecordingData = seriesRecordingData;
        this.timerRecordingData = timerRecordingData;
        this.connectionData = connectionData;
        this.channelTagData = channelTagData;
        this.serverStatusData = serverStatusData;
        this.serverProfileData = serverProfileData;
        this.tagAndChannelData = tagAndChannelData;
        this.miscData = miscData;
    }

    @Override
    public TimerRecordingData getTimerRecordingData() {
        return timerRecordingData;
    }

    @Override
    public SeriesRecordingData getSeriesRecordingData() {
        return seriesRecordingData;
    }

    @Override
    public RecordingData getRecordingData() {
        return recordingData;
    }

    @Override
    public ChannelData getChannelData() {
        return channelData;
    }

    @Override
    public ChannelTagData getChannelTagData() {
        return channelTagData;
    }

    @Override
    public ProgramData getProgramData() {
        return programData;
    }

    @Override
    public ConnectionData getConnectionData() {
        return connectionData;
    }

    @Override
    public ServerStatusData getServerStatusData() {
        return serverStatusData;
    }

    @Override
    public ServerProfileData getServerProfileData() {
        return serverProfileData;
    }

    @Override
    public TagAndChannelData getTagAndChannelData() {
        return tagAndChannelData;
    }

    @Override
    public MiscData getMiscData() {
        return miscData;
    }
}
