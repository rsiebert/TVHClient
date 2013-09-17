package org.tvheadend.tvhguide;

import org.tvheadend.tvhguide.htsp.HTSService;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;

public class SettingsFragment extends PreferenceFragment {

    private String hostname;
    private int port;
    private String username;
    private String password;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the default values
        PreferenceManager.setDefaultValues(getActivity(), R.xml.preferences, false);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
    }

    public void onResume() {
        super.onResume();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        hostname = prefs.getString("serverHostPref", "");
        port = Integer.parseInt(prefs.getString("serverPortPref", ""));
        username = prefs.getString("usernamePref", "");
        password = prefs.getString("passwordPref", "");
    }

    @Override
    public void onPause() {
        super.onPause();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        boolean reconnect = false;

        // Check if the connectivity settings have changed.
        reconnect |= !hostname.equals(prefs.getString("serverHostPref", ""));
        reconnect |= (port != Integer.parseInt(prefs.getString("serverPortPref", "")));
        reconnect |= !username.equals(prefs.getString("usernamePref", ""));
        reconnect |= !password.equals(prefs.getString("passwordPref", ""));

        // If the connectivity setting have changed then initiate
        // a reconnect by sending the forced reconnect action to the service
        if (reconnect) {
            Log.i("SettingsActivity", "Connectivity settings chaned, forcing a reconnect");
            
            // Create the intent with the required connection information
            Intent intent = new Intent(getActivity(), HTSService.class);
            intent.setAction(HTSService.ACTION_CONNECT);
            intent.putExtra("hostname", prefs.getString("serverHostPref", ""));
            intent.putExtra("port", Integer.parseInt(prefs.getString("serverPortPref", "")));
            intent.putExtra("username", prefs.getString("usernamePref", ""));
            intent.putExtra("password", prefs.getString("passwordPref", ""));
            intent.putExtra("force", true);
            getActivity().startService(intent);
        }
    }
}
