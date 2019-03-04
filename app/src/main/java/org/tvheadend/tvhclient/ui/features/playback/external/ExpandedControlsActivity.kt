package org.tvheadend.tvhclient.ui.features.playback.external

import android.os.Bundle
import android.view.Menu

import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.gms.cast.framework.media.widget.ExpandedControllerActivity

import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.util.MiscUtils

class ExpandedControlsActivity : ExpandedControllerActivity() {

    override fun onCreate(bundle: Bundle?) {
        setTheme(MiscUtils.getThemeId(this))
        super.onCreate(bundle)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.expanded_controller_menu, menu)
        CastButtonFactory.setUpMediaRouteButton(this, menu, R.id.media_route_menu_item)
        return true
    }
}
