package org.tvheadend.tvhclient.ui.features.playback.internal;

interface HtspDataSourceInterface {

    long getTimeshiftOffsetPts();

    void setSpeed(int tvhSpeed);

    long getTimeshiftStartTime();

    long getTimeshiftStartPts();

    void resume();

    void pause();
}
