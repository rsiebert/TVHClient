package org.tvheadend.tvhclient.features.dvr;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import android.widget.DatePicker;

import org.tvheadend.tvhclient.features.shared.callbacks.DateTimePickerCallback;

import java.util.Calendar;

public class DatePickerFragment extends DialogFragment implements DatePickerDialog.OnDateSetListener {

    private int hourOfDay;
    private int minute;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        final Calendar c = Calendar.getInstance();
        // Get the given milliseconds, if non are given then the current time is used
        Bundle bundle = getArguments();
        if (bundle != null) {
            c.setTimeInMillis(bundle.getLong("milliSeconds"));
        }
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);
        hourOfDay = c.get(Calendar.HOUR_OF_DAY);
        minute = c.get(Calendar.MINUTE);
        return new DatePickerDialog(getActivity(), this, year, month, day);
    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int day) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month);
        calendar.set(Calendar.DAY_OF_MONTH, day);
        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
        calendar.set(Calendar.MINUTE, minute);

        Fragment fragment = getTargetFragment();
        if (fragment instanceof DateTimePickerCallback) {
            ((DateTimePickerCallback) fragment).onDateSelected(calendar.getTimeInMillis(), getTag());
        }
    }
}