package org.tvheadend.tvhclient.ui.features.dvr

import android.content.IntentFilter
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.ui.base.BaseActivity
import org.tvheadend.tvhclient.ui.common.SnackbarMessageReceiver
import org.tvheadend.tvhclient.ui.common.callbacks.BackPressedInterface
import org.tvheadend.tvhclient.ui.features.dvr.recordings.RecordingAddEditFragment
import org.tvheadend.tvhclient.ui.features.dvr.series_recordings.SeriesRecordingAddEditFragment
import org.tvheadend.tvhclient.ui.features.dvr.timer_recordings.TimerRecordingAddEditFragment
import org.tvheadend.tvhclient.util.getThemeId

// TODO split into 3 activities

class RecordingAddEditActivity : BaseActivity() {

    private lateinit var snackbarMessageReceiver: SnackbarMessageReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(getThemeId(this))
        super.onCreate(savedInstanceState)
        setContentView(R.layout.misc_content_activity)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        snackbarMessageReceiver = SnackbarMessageReceiver(this)

        if (savedInstanceState == null) {
            var fragment: Fragment? = null
            val type = intent.getStringExtra("type")
            when (type) {
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

    public override fun onStart() {
        super.onStart()
        LocalBroadcastManager.getInstance(this).registerReceiver(snackbarMessageReceiver,
                IntentFilter(SnackbarMessageReceiver.ACTION))
    }

    public override fun onStop() {
        super.onStop()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(snackbarMessageReceiver)
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
