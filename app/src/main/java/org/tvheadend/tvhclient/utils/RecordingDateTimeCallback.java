package org.tvheadend.tvhclient.utils;


public interface RecordingDateTimeCallback {
    void timeSelected(int hour, int minute, String tag);
    void dateSelected(int year, int month, int day, String tag);
}
