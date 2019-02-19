package org.tvheadend.tvhclient.ui.features.streaming.external;

import android.os.Bundle;
import android.view.Menu;

import com.google.android.gms.cast.framework.CastButtonFactory;
import com.google.android.gms.cast.framework.media.widget.ExpandedControllerActivity;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.utils.MiscUtils;

public class ExpandedControlsActivity extends ExpandedControllerActivity {

    @Override
    protected void onCreate(Bundle bundle) {
        setTheme(MiscUtils.getThemeId(this));
        super.onCreate(bundle);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.expanded_controller_menu, menu);
        CastButtonFactory.setUpMediaRouteButton(this, menu, R.id.media_route_menu_item);
        return true;
    }
}
