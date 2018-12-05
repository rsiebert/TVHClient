package org.tvheadend.tvhclient.features.dvr;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.widget.TimePicker;

import org.tvheadend.tvhclient.features.shared.callbacks.DateTimePickerCallback;

import java.util.Calendar;

public class TimePickerFragment extends DialogFragment implements TimePickerDialog.OnTimeSetListener {

    private int year;
    private int month;
    private int day;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        final Calendar c = Calendar.getInstance();
        // Get the given milliseconds, if non are given then the current time is used
        Bundle bundle = getArguments();
        if (bundle != null) {
            c.setTimeInMillis(bundle.getLong("milliSeconds"));
        }
        year = c.get(Calendar.YEAR);
        month = c.get(Calendar.MONTH);
        day = c.get(Calendar.DAY_OF_MONTH);
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);

        return new TimePickerDialog(getActivity(), this, hour, minute, true);
    }

    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month);
        calendar.set(Calendar.DAY_OF_MONTH, day);
        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
        calendar.set(Calendar.MINUTE, minute);

        Fragment fragment = getTargetFragment();
        if (fragment instanceof DateTimePickerCallback) {
            ((DateTimePickerCallback) fragment).onTimeSelected(calendar.getTimeInMillis(), getTag());
        }
    }
}