package org.tvheadend.tvhclient.ui.recordings.common;

public interface DateTimePickerCallback {

    void onTimeSelected(long milliSeconds, String tag);

    void onDateSelected(long milliSeconds, String tag);
}
