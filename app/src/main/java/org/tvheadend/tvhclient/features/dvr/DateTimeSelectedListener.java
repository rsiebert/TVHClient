package org.tvheadend.tvhclient.features.dvr;

public interface DateTimeSelectedListener {

    void onTimeSelected(long milliSeconds, String tag);

    void onDateSelected(long milliSeconds, String tag);
}
