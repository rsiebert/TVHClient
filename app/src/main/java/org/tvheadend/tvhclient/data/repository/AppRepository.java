package org.tvheadend.tvhclient.data.repository;

import javax.inject.Inject;

public class AppRepository implements RepositoryInterface {

    private final ChannelData channelData;
    private final ProgramData programData;
    private final RecordingData recordingData;
    private final SeriesRecordingData seriesRecordingData;
    private final TimerRecordingData timerRecordingData;
    private final ConnectionData connectionData;
    private final ChannelTagData channelTagData;

    @Inject
    public AppRepository(ChannelData channelData, ProgramData programData, RecordingData recordingData, SeriesRecordingData seriesRecordingData, TimerRecordingData timerRecordingData, ConnectionData connectionData, ChannelTagData channelTagData) {
        this.channelData = channelData;
        this.programData = programData;
        this.recordingData = recordingData;
        this.seriesRecordingData = seriesRecordingData;
        this.timerRecordingData = timerRecordingData;
        this.connectionData = connectionData;
        this.channelTagData = channelTagData;
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
}
