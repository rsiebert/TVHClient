package org.tvheadend.tvhguide;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class SettingsActivity extends PreferenceActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        
        // Apply the specified theme
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Boolean theme = prefs.getBoolean("lightThemePref", false);
        setTheme(theme ? R.style.CustomTheme_Light : R.style.CustomTheme);
        
        super.onCreate(savedInstanceState);
        
        setTitle(R.string.menu_settings);
        
        // Display the fragment as the main content.
        getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();
    }
}
