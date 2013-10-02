package org.tvheadend.tvhguide;

import org.tvheadend.tvhguide.htsp.HTSService;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;

public class SettingsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {

    private String hostname;
    private int port;
    private String username;
    private String password;
    private EditTextPreference prefHostname;
    private EditTextPreference prefPort;
    private EditTextPreference prefUsername;
    private EditTextPreference prefPassword;
    private boolean connectionPreferencesChanged = false;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the default values
        PreferenceManager.setDefaultValues(getActivity(), R.xml.preferences, false);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
        
        // Get the connectivity preferences for later usage
        prefHostname = (EditTextPreference) findPreference("serverHostPref");
        prefPort = (EditTextPreference) findPreference("serverPortPref");
        prefUsername = (EditTextPreference) findPreference("usernamePref");
        prefPassword = (EditTextPreference) findPreference("passwordPref");
    }

    public void onResume() {
        super.onResume();

        connectionPreferencesChanged = false;
        updateConnectionPreferenceSummary();
        
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        prefs.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        prefs.unregisterOnSharedPreferenceChangeListener(this);

        // If the connectivity setting have changed then initiate
        // a reconnect by sending the forced reconnect action to the service
        if (connectionPreferencesChanged) {
            
            // Create the intent with the required connection information
            Intent intent = new Intent(getActivity(), HTSService.class);
            intent.setAction(HTSService.ACTION_CONNECT);
            intent.putExtra("hostname", hostname);
            intent.putExtra("port",     port);
            intent.putExtra("username", username);
            intent.putExtra("password", password);
            intent.putExtra("force", true);
            getActivity().startService(intent);
        }
    }

    /**
     * Shows the values for each connection preference so the user knows what 
     * he has specified. In case no value was specified, the default summary 
     * text will be displayed.
     */
    private void updateConnectionPreferenceSummary() {

        // Get the currently set values from the preferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        hostname = prefs.getString("serverHostPref", "");
        port = Integer.parseInt(prefs.getString("serverPortPref", ""));
        username = prefs.getString("usernamePref", "");
        password = prefs.getString("passwordPref", "");
        
        // Update the summary text of the connectivity preferences with these values
        prefHostname.setSummary(hostname.isEmpty() ? getString(R.string.pref_host_sum) : hostname);
        prefPort.setSummary(port == 0 ? getString(R.string.pref_host_sum) : String.valueOf(port));
        prefUsername.setSummary(username.isEmpty() ? getString(R.string.pref_user_sum) : username);
        prefPassword.setSummary(password.isEmpty() ? getString(R.string.pref_pass_sum) : getString(R.string.pref_pass_set_sum));
    }
    
    @Override
    public void onSharedPreferenceChanged(SharedPreferences pref, String key) {

     // The connectivity settings have changed.
        if (key.equals("serverHostPref") || 
            key.equals("serverPortPref") || 
            key.equals("usernamePref") ||
            key.equals("passwordPref")) {
            
            Log.i("SettingsActivity", "Connectivity settings changed, forcing a reconnect");
            connectionPreferencesChanged = true;
        }

        // Disallow an empty port setting to avoid an exception.
        if (key.equals("serverPortPref") && prefPort.getText().isEmpty()) {
            prefPort.setText("0");
        }

        // Update the summary fields in the settings 
        updateConnectionPreferenceSummary();
    }
}
