package org.tvheadend.tvhclient.ui.features.programs

import android.content.Context
import android.os.Bundle
import android.view.MenuItem
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.ui.common.interfaces.LayoutControlInterface
import org.tvheadend.tvhclient.ui.common.interfaces.ToolbarInterface
import org.tvheadend.tvhclient.ui.common.onAttach
import org.tvheadend.tvhclient.util.extensions.gone
import org.tvheadend.tvhclient.util.extensions.visible
import org.tvheadend.tvhclient.util.getThemeId
import timber.log.Timber

class ProgramDetailsActivity : AppCompatActivity(), ToolbarInterface, LayoutControlInterface {

    lateinit var toolbar: Toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(getThemeId(this))
        super.onCreate(savedInstanceState)
        setContentView(R.layout.misc_content_activity)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        if (savedInstanceState == null) {
            val fragment = ProgramDetailsFragment.newInstance(
                    intent.getIntExtra("eventId", 0),
                    intent.getIntExtra("channelId", 0))
            supportFragmentManager.beginTransaction().add(R.id.main, fragment).commit()
        }
    }

    override fun attachBaseContext(context: Context) {
        super.attachBaseContext(onAttach(context))
    }

    override fun setTitle(title: String) {
        supportActionBar?.title = title
    }

    override fun setSubtitle(subtitle: String) {
        supportActionBar?.subtitle = subtitle
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun enableSingleScreenLayout() {
        Timber.d("Dual pane is not active, hiding details layout")
        val mainFrameLayout: FrameLayout = findViewById(R.id.main)
        val detailsFrameLayout: FrameLayout? = findViewById(R.id.details)
        detailsFrameLayout?.gone()
        mainFrameLayout.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1.0f)
    }

    override fun enableDualScreenLayout() {
        Timber.d("Dual pane is active, showing details layout")
        val mainFrameLayout: FrameLayout = findViewById(R.id.main)
        val detailsFrameLayout: FrameLayout? = findViewById(R.id.details)
        detailsFrameLayout?.visible()
        mainFrameLayout.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT,
                0.65f)
    }

    override fun forceSingleScreenLayout() {
        enableSingleScreenLayout()
    }
}
