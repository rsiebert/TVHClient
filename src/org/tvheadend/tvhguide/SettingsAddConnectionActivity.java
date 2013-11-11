/*
 *  Copyright (C) 2013 Robert Siebert
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

import org.tvheadend.tvhguide.model.Connection;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public class SettingsAddConnectionActivity extends PreferenceActivity {

    private final static String TAG = SettingsAddConnectionActivity.class.getSimpleName();
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        // Apply the specified theme
        setTheme(Utils.getThemeId(this));
        super.onCreate(savedInstanceState);

        // Setup the action bar and show the title
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);
        getActionBar().setTitle(R.string.add_connection);
        
        getFragmentManager().beginTransaction()
            .replace(android.R.id.content, new SettingsAddConnectionFragment())
            .commit();
    }

    public static class SettingsAddConnectionFragment extends PreferenceFragment implements
            OnSharedPreferenceChangeListener {

        private static final String PREF_NAME_KEY = "pref_name";
        private static final String PREF_ADDRESS_KEY = "pref_address";
        private static final String PREF_PORT_KEY = "pref_port";
        private static final String PREF_USERNAME_KEY = "pref_username";
        private static final String PREF_PASSWORD_KEY = "pref_password";
        private static final String PREF_SELECTED_KEY = "pref_selected";

        // Preference widgets
        private EditTextPreference prefName;
        private EditTextPreference prefAddress;
        private EditTextPreference prefPort;
        private EditTextPreference prefUsername;
        private EditTextPreference prefPassword;
        private CheckBoxPreference prefSelected;

        private SharedPreferences prefs;

        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            Log.i(TAG, "onCreate");

            setHasOptionsMenu(true);
            
            // Get the preferences
            prefs = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
            
            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences_add_connection);
            
            // Get the connectivity preferences for later usage
            prefName = (EditTextPreference) findPreference(PREF_NAME_KEY);
            prefAddress = (EditTextPreference) findPreference(PREF_ADDRESS_KEY);
            prefPort = (EditTextPreference) findPreference(PREF_PORT_KEY);
            prefUsername = (EditTextPreference) findPreference(PREF_USERNAME_KEY);
            prefPassword = (EditTextPreference) findPreference(PREF_PASSWORD_KEY);
            prefSelected = (CheckBoxPreference) findPreference(PREF_SELECTED_KEY);
        }

        public void onResume() {
            super.onResume();
            Log.i(TAG, "onResume");
            
            // If an index is given then an existing connection shall be edited.
            long id = getActivity().getIntent().getLongExtra("id", 0);
            if (id > 0) {
                Log.i(TAG, "onResume, editing existing connection " + id);

                // Load the connection data and assign the values
                Connection conn = DatabaseHelper.getInstance().getConnection(id);
                if (conn != null) {
                    Log.i(TAG, "onResume, connection != null");
                    prefs.edit().putString(PREF_NAME_KEY, conn.name).commit();
                    prefs.edit().putString(PREF_ADDRESS_KEY, conn.address).commit();
                    prefs.edit().putString(PREF_PORT_KEY, String.valueOf(conn.port)).commit();
                    prefs.edit().putString(PREF_USERNAME_KEY, conn.username).commit();
                    prefs.edit().putString(PREF_PASSWORD_KEY, conn.password).commit();
                    
                    prefName.setDefaultValue(conn.name);
                    prefAddress.setDefaultValue(conn.address);
                }
            }
            else
                removePreferenceValues();
            
            // Show the default values with the summary 
            // text or the already set values 
            showPreferenceValues();
            
            // Now register for any changes
            prefs.registerOnSharedPreferenceChangeListener(this);
        }

        private void showPreferenceValues() {
            Log.i(TAG, "showPreferenceValues");
            
            // Set the values only if they differ from the defaults
            String name = prefs.getString(PREF_NAME_KEY, "");
            prefName.setSummary(name.isEmpty() ? getString(R.string.pref_name_sum) : name);
            
            String address = prefs.getString(PREF_ADDRESS_KEY, "");
            prefAddress.setSummary(address.isEmpty() ? getString(R.string.pref_host_sum) : address);
            
            String port = prefs.getString(PREF_PORT_KEY, "9981");
            prefPort.setSummary(port.isEmpty() ? getString(R.string.pref_host_sum) : port);
            
            String username = prefs.getString(PREF_USERNAME_KEY, "");
            prefUsername.setSummary(username.isEmpty() ? getString(R.string.pref_user_sum) : username);
            
            String password = prefs.getString(PREF_PASSWORD_KEY, "");
            prefPassword.setSummary(password.isEmpty() ? getString(R.string.pref_pass_sum) : getString(R.string.pref_pass_set_sum));
        }
        
        @Override
        public void onPause() {
            super.onPause();
            Log.i(TAG, "onPause");
            prefs.unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            super.onCreateOptionsMenu(menu, inflater);
            inflater.inflate(R.menu.preference_edit_connection, menu);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            switch (item.getItemId()) {
            case android.R.id.home:
                cancel();
                return true;

            case R.id.menu_save:
                save();
                return true;

            case R.id.menu_cancel:
                cancel();
                return true;

            default:
                return super.onOptionsItemSelected(item);
            }
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences pref, String key) {
            showPreferenceValues();
        }

        /**
         * Validates the name, host name and port number. If they are valid the
         * values will be saved in a model. This server model is then added as
         * new in the database or an existing server will be updated.
         */
        private void save() {

            // Get the data from the preferences
            Connection conn = new Connection();
            conn.name = prefName.getText();
            conn.address = prefAddress.getText();
            conn.port = Integer.parseInt(prefPort.getText());
            conn.username = prefUsername.getText();
            conn.password = prefPassword.getText();
            conn.selected = prefSelected.isChecked();

            // If the connection shall be edited, get its id
            long id = getActivity().getIntent().getLongExtra("id", 0);
            if (id > 0) {
                Log.i(TAG, "save, saving existing connection with id " + id);

                // if this connection is now set as selected, we need
                // to remove the selection from the current one.
                if (prefSelected.isChecked()) {
                    Connection selConn = DatabaseHelper.getInstance().getSelectedConnection();
                    if (selConn != null) {
                        selConn.selected = false;
                        DatabaseHelper.getInstance().updateConnection(selConn);
                    }
                }

                // Update the connection with the data from the preferences and
                // the selected status
                DatabaseHelper.getInstance().updateConnection(conn);
            }
            else {
                Log.i(TAG, "save, saving new connection");
                DatabaseHelper.getInstance().addConnection(conn);
            }

            getActivity().finish();
        }

        /**
         * Asks the user to confirm canceling the current activity. If no is
         * pressed then the user can continue to add or edit the connection
         * data. If yes was chosen the activity is closed.
         */
        private void cancel() {

            // Show confirmation dialog to cancel
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(getString(R.string.confirm_cancel));

            // Define the action of the yes button
            builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    getActivity().finish();
                }
            });
            // Define the action of the no button
            builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();
                }
            });
            AlertDialog alert = builder.create();
            alert.show();
        }
        
        private void removePreferenceValues() {
            
            prefs.unregisterOnSharedPreferenceChangeListener(this);
            
            // Remove all previously set values when we did not rotate the activity
            prefs.edit().remove(PREF_NAME_KEY).commit();
            prefs.edit().remove(PREF_ADDRESS_KEY).commit();
            prefs.edit().remove(PREF_PORT_KEY).commit();
            prefs.edit().remove(PREF_USERNAME_KEY).commit();
            prefs.edit().remove(PREF_PASSWORD_KEY).commit();
        }
    }
}
