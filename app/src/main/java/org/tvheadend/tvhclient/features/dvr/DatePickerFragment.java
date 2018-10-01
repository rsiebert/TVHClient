package org.tvheadend.tvhclient.features.dvr;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.widget.DatePicker;

import org.tvheadend.tvhclient.features.shared.callbacks.DateTimePickerCallback;

import java.util.Calendar;

public class DatePickerFragment extends DialogFragment implements DatePickerDialog.OnDateSetListener {

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

        return new DatePickerDialog(getActivity(), this, year, month, day);
    }

    public void onDateSet(DatePicker view, int year, int month, int day) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month);
        calendar.set(Calendar.DAY_OF_MONTH, day);

        Fragment fragment = getTargetFragment();
        if (fragment instanceof DateTimePickerCallback) {
            ((DateTimePickerCallback) fragment).onDateSelected(calendar.getTimeInMillis(), getTag());
        }
    }
}