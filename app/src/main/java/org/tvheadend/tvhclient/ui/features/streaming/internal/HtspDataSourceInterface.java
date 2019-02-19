package org.tvheadend.tvhclient.ui.features.streaming.internal;

interface HtspDataSourceInterface {

    long getTimeshiftOffsetPts();

    void setSpeed(int tvhSpeed);

    long getTimeshiftStartTime();

    long getTimeshiftStartPts();

    void resume();

    void pause();
}
