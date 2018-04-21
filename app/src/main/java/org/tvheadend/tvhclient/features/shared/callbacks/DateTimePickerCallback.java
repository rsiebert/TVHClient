package org.tvheadend.tvhclient.features.shared.callbacks;

public interface DateTimePickerCallback {

    void onTimeSelected(long milliSeconds, String tag);

    void onDateSelected(long milliSeconds, String tag);
}
