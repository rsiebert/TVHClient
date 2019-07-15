package org.tvheadend.tvhclient.ui.features.dvr

import android.os.Bundle
import androidx.fragment.app.Fragment
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.ui.base.BaseActivity
import org.tvheadend.tvhclient.ui.common.callbacks.BackPressedInterface
import org.tvheadend.tvhclient.ui.features.dvr.recordings.RecordingAddEditFragment
import org.tvheadend.tvhclient.ui.features.dvr.series_recordings.SeriesRecordingAddEditFragment
import org.tvheadend.tvhclient.ui.features.dvr.timer_recordings.TimerRecordingAddEditFragment

class RecordingAddEditActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            var fragment: Fragment? = null
            when (intent.getStringExtra("type")) {
                "recording" -> fragment = RecordingAddEditFragment()
                "series_recording" -> fragment = SeriesRecordingAddEditFragment()
                "timer_recording" -> fragment = TimerRecordingAddEditFragment()
            }

            fragment?.let {
                it.arguments = intent.extras
                supportFragmentManager.beginTransaction().add(R.id.main, it).commit()
            }
        }
    }

    override fun onBackPressed() {
        val fragment = supportFragmentManager.findFragmentById(R.id.main)
        if (fragment is BackPressedInterface) {
            fragment.onBackPressed()
        } else {
            super.onBackPressed()
        }
    }
}
