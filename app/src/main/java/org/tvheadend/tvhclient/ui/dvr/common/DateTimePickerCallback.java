package org.tvheadend.tvhclient.ui.dvr.common;

public interface DateTimePickerCallback {

    void onTimeSelected(int hour, int minute, String tag);

    void onDateSelected(int year, int month, int day, String tag);
}
