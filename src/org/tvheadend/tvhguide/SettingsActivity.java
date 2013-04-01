/*
 *  Copyright (C) 2011 John Törnblom
 *
 * This file is part of TVHGuide.
 *
 * TVHGuide is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * TVHGuide is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with TVHGuide.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.tvheadend.tvhguide;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Window;
import org.tvheadend.tvhguide.R;
import org.tvheadend.tvhguide.htsp.HTSService;

/**
 *
 * @author john-tornblom
 */
public class SettingsActivity extends PreferenceActivity {

    private int oldPort;
    private String oldHostname;
    private String oldUser;
    private String oldPw;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Boolean theme = prefs.getBoolean("lightThemePref", false);
        setTheme(theme ? android.R.style.Theme_Light : android.R.style.Theme);

        requestWindowFeature(Window.FEATURE_LEFT_ICON);
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);
        setTitle(getString(R.string.app_name) + " - " + getString(R.string.menu_settings));
        setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.logo_72);
    }

    @Override
    protected void onStart() {
        super.onStart();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        oldHostname = prefs.getString("serverHostPref", "");
        oldPort = Integer.parseInt(prefs.getString("serverPortPref", ""));
        oldUser = prefs.getString("usernamePref", "");
        oldPw = prefs.getString("passwordPref", "");
    }

    @Override
    protected void onPause() {
        super.onPause();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean reconnect = false;
        reconnect |= !oldHostname.equals(prefs.getString("serverHostPref", ""));
        reconnect |= oldPort != Integer.parseInt(prefs.getString("serverPortPref", ""));
        reconnect |= !oldUser.equals(prefs.getString("usernamePref", ""));
        reconnect |= !oldPw.equals(prefs.getString("passwordPref", ""));

        if (reconnect) {
            Log.d("SettingsActivity", "Connectivity settings chaned, forcing a reconnect");
            Intent intent = new Intent(SettingsActivity.this, HTSService.class);
            intent.setAction(HTSService.ACTION_CONNECT);
            intent.putExtra("hostname", prefs.getString("serverHostPref", ""));
            intent.putExtra("port", Integer.parseInt(prefs.getString("serverPortPref", "")));
            intent.putExtra("username", prefs.getString("usernamePref", ""));
            intent.putExtra("password", prefs.getString("passwordPref", ""));
            intent.putExtra("force", true);
            startService(intent);
        }
    }
}
