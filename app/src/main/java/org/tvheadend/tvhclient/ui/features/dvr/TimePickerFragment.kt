package org.tvheadend.tvhclient.ui.features.dvr

import android.app.Dialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.TimePicker
import androidx.fragment.app.DialogFragment
import java.util.*

class TimePickerFragment : DialogFragment(), TimePickerDialog.OnTimeSetListener {

    private var year: Int = 0
    private var month: Int = 0
    private var day: Int = 0

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val c = Calendar.getInstance()
        // Get the given milliseconds, if non are given then the current time is used
        c.timeInMillis = arguments?.getLong("milliSeconds") ?: 0

        year = c.get(Calendar.YEAR)
        month = c.get(Calendar.MONTH)
        day = c.get(Calendar.DAY_OF_MONTH)
        val hour = c.get(Calendar.HOUR_OF_DAY)
        val minute = c.get(Calendar.MINUTE)

        return TimePickerDialog(context, this, hour, minute, true)
    }

    override fun onTimeSet(view: TimePicker, hourOfDay: Int, minute: Int) {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.YEAR, year)
        calendar.set(Calendar.MONTH, month)
        calendar.set(Calendar.DAY_OF_MONTH, day)
        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
        calendar.set(Calendar.MINUTE, minute)

        val fragment = targetFragment
        if (fragment is Listener) {
            fragment.onTimeSelected(calendar.timeInMillis, tag)
        }
    }

    interface Listener {
        fun onTimeSelected(milliSeconds: Long, tag: String?)
    }
}