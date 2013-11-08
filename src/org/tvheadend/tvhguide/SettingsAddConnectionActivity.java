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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
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
            OnSharedPreferenceChangeListener, OnPreferenceClickListener {

        private SharedPreferences prefs;
        private EditTextPreference prefName;
        private EditTextPreference prefAddress;
        private EditTextPreference prefPort;
        private EditTextPreference prefUsername;
        private EditTextPreference prefPassword;
        private CheckBoxPreference prefSelected;

        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setHasOptionsMenu(true);
            
            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences_add_connection);

            // Get the connectivity preferences for later usage
            prefName = (EditTextPreference) findPreference("pref_name");
            prefAddress = (EditTextPreference) findPreference("pref_address");
            prefPort = (EditTextPreference) findPreference("pref_port");
            prefUsername = (EditTextPreference) findPreference("pref_username");
            prefPassword = (EditTextPreference) findPreference("pref_password");
            prefSelected = (CheckBoxPreference) findPreference("pref_selected");
            
            
        }

        public void onResume() {
            super.onResume();
            Log.i(TAG, "onResume");

            // If an index is given then an existing connection shall be edited.
            long id = getActivity().getIntent().getLongExtra("id", 0);
            if (id > 0) {
                Log.i(TAG, "onResume, editing connection with id " + id);

                // Load the connection data and assign the values to the
                // preferences.
                Connection conn = DatabaseHelper.getInstance().getSelectedConnection();
                if (conn != null) {

                    
                    PreferenceManager.getDefaultSharedPreferences(getActivity());
                    SharedPreferences.Editor e = prefs.edit();
                    e.putString("pref_name", conn.name);
                    e.putString("pref_address", conn.address);
                    e.putInt("pref_port", conn.port);
                    e.putString("pref_username", conn.username);
                    e.putString("pref_password", conn.password);
                    e.putBoolean("pref_selected", conn.selected);
                    e.commit();
                    
                    
                }
            }
            else {
                Log.i(TAG, "onResume, adding new connection with default values");

                // Load the default values, these are empty
                PreferenceManager.getDefaultSharedPreferences(getActivity());
                PreferenceManager.setDefaultValues(getActivity(), R.xml.preferences_add_connection, true);
            }

            setPreferenceValues();

            // Now register for any changes
            prefs.registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onPause() {
            super.onPause();
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

        /**
         * Assigns the values of the given connection to the preferences.
         * 
         * @param model
         */
        private void setPreferenceValues() {

            // Get the currently set values from the preferences
            String name = prefs.getString("pref_name", "");
            String address = prefs.getString("pref_address", "");
            int port = prefs.getInt("pref_port", 9982);
            String username = prefs.getString("pref_username", "");
            String password = prefs.getString("pref_password", "");
            int isSelected = prefs.getInt("pref_selected", 0);

            // Update the summary text of the connectivity preferences with these values
            prefName.setSummary(name.isEmpty() ? getString(R.string.pref_name_sum) : name);
            prefAddress.setSummary(address.isEmpty() ? getString(R.string.pref_host_sum) : address);
            prefPort.setSummary(port == 0 ? getString(R.string.pref_host_sum) : String.valueOf(port));
            prefUsername.setSummary(username.isEmpty() ? getString(R.string.pref_user_sum) : username);
            prefPassword.setSummary(password.isEmpty() ? getString(R.string.pref_pass_sum) : getString(R.string.pref_pass_set_sum));
            prefSelected.setChecked((isSelected == 1));
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences pref, String key) {
            Log.i("Add host", "changed pref " + key);

            setPreferenceValues();
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

            // If the server shall be edited, get the id of the server from the
            // intent.
            int id = getActivity().getIntent().getIntExtra("connection_id", 0);
            if (id > 0) {
                Log.i(TAG, "save, saving existing connection with id " + id);

                // if this connection is now set as selected, we need
                // to remove the selection from the current one.
                if (prefSelected.isChecked()) {
                    Connection selConn = DatabaseHelper.getInstance().getSelectedConnection();
                    selConn.selected = false;
                    DatabaseHelper.getInstance().updateConnection(selConn);
                }

                // Update the connection with the data from the preferences and
                // the selected status
                DatabaseHelper.getInstance().updateConnection(conn);
            }
            else {
                Log.i(TAG, "save, saving new connection");
                DatabaseHelper.getInstance().addConnection(conn);
            }

            // TODO Return to the previous activity indicating success
            Intent resultIntent = new Intent();
            getActivity().setResult(Activity.RESULT_OK, resultIntent);
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

        @Override
        public boolean onPreferenceClick(Preference pref) {
            Log.i("Add host", "clicked on pref " + pref.getKey());
            return false;
        }
    }
}
