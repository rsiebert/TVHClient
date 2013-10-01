package org.tvheadend.tvhguide;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class SettingsActivity extends PreferenceActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        
        // Apply the specified theme
        setTheme(Utils.getThemeId(this));
        
        super.onCreate(savedInstanceState);
        
        setTitle(R.string.menu_settings);
        
        // Display the fragment as the main content.
        getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();
    }
}
