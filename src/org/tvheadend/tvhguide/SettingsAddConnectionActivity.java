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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import android.util.Patterns;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

public class SettingsAddConnectionActivity extends PreferenceActivity {

    // Contains the currently set connection values
    // and keeps them during orientation changes
    static Connection conn = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(Utils.getThemeId(this));
        super.onCreate(savedInstanceState);
        Log.i("SettingsAddConnectionActivity", "onCreate");

        // Setup the action bar and show the title
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);
        getActionBar().setTitle(R.string.add_connection);

        getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsAddConnectionFragment())
                .commit();
    }

    public void onStop() {
        super.onStop();
        Log.i("SettingsAddConnectionActivity", "onStop");
        conn = null;
    }

    public static class SettingsAddConnectionFragment extends PreferenceFragment implements
            OnSharedPreferenceChangeListener {

        private final static String TAG = SettingsAddConnectionFragment.class.getSimpleName();

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

        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setHasOptionsMenu(true);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences_add_connection);

            // Get the connectivity preferences for later usage
            prefName = (EditTextPreference) findPreference(PREF_NAME_KEY);
            prefAddress = (EditTextPreference) findPreference(PREF_ADDRESS_KEY);
            prefPort = (EditTextPreference) findPreference(PREF_PORT_KEY);
            prefUsername = (EditTextPreference) findPreference(PREF_USERNAME_KEY);
            prefPassword = (EditTextPreference) findPreference(PREF_PASSWORD_KEY);
            prefSelected = (CheckBoxPreference) findPreference(PREF_SELECTED_KEY);

            // If the connection is null then this activity has been started for
            // the first time. If the connection is not null then the screen has
            // been rotated and we have to reuse the values.
            if (conn == null) {
                Log.i(TAG, "Connection is null");

                // If an index is given then we want to edit this connection
                // Otherwise create a new connection with default values.
                long id = getActivity().getIntent().getLongExtra("id", 0);
                if (id > 0) {
                    Log.i(TAG, "Existing connection with id " + id + " was given");
                    conn = DatabaseHelper.getInstance().getConnection(id);
                }
                else {
                    Log.i(TAG, "New connection");
                    conn = new Connection();
                    conn.name = "Default";
                    conn.address = "localhost";
                    conn.port = 9981;
                    conn.username = "";
                    conn.password = "";
                    conn.selected = false;
                }
            }

            Log.i(TAG, "Setting preferences from connection values");
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
            prefs.edit().putString(PREF_NAME_KEY, conn.name).commit();
            prefs.edit().putString(PREF_ADDRESS_KEY, conn.address).commit();
            prefs.edit().putString(PREF_PORT_KEY, String.valueOf(conn.port)).commit();
            prefs.edit().putString(PREF_USERNAME_KEY, conn.username).commit();
            prefs.edit().putString(PREF_PASSWORD_KEY, conn.password).commit();
        }

        public void onResume() {
            super.onResume();
            Log.i(TAG, "onResume");

            // Show the values from the connection object
            // in the summary text of the preferences
            showPreferenceSummary();

            // Now register for any changes
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
            prefs.registerOnSharedPreferenceChangeListener(this);
        }

        /**
         * Displays the values from the
         */
        private void showPreferenceSummary() {
            Log.i(TAG, "showPreferenceSummary");

            // Set the values only if they differ from the defaults
            prefName.setSummary(conn.name.isEmpty() ? getString(R.string.pref_name_sum) : conn.name);
            prefAddress.setSummary(conn.address.isEmpty() ? getString(R.string.pref_host_sum) : conn.address);
            prefPort.setSummary(conn.port == 0 ? getString(R.string.pref_host_sum) : String.valueOf(conn.port));
            prefUsername.setSummary(conn.username.isEmpty() ? getString(R.string.pref_user_sum) : conn.username);
            prefPassword.setSummary(conn.password.isEmpty() ? getString(R.string.pref_pass_sum)
                    : getString(R.string.pref_pass_set_sum));
        }

        @Override
        public void onPause() {
            super.onPause();
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
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

            // Update the connection object with the new values
            conn.name = pref.getString(PREF_NAME_KEY, "Default");
            conn.address = pref.getString(PREF_ADDRESS_KEY, "localhost");
            conn.port = Integer.parseInt(pref.getString(PREF_PORT_KEY, "9981"));
            conn.username = pref.getString(PREF_USERNAME_KEY, "");
            conn.password = pref.getString(PREF_PASSWORD_KEY, "");
            conn.selected = pref.getBoolean(PREF_SELECTED_KEY, false);

            // Show the values from the connection object
            // in the summary text of the preferences
            showPreferenceSummary();
        }

        /**
         * Validates the name, host name and port number. If they are valid the
         * values will be saved as a new connection or the existing will be
         * updated.
         */
        private void save() {

            if (!validateName() && !validatePort() && !validateAddress())
                return;

            // If the current connection is set as selected
            // we need to unselect the previous one.
            if (prefSelected.isChecked()) {
                Connection prevSelectedConn = DatabaseHelper.getInstance().getSelectedConnection();
                if (prevSelectedConn != null) {
                    prevSelectedConn.selected = false;
                    DatabaseHelper.getInstance().updateConnection(prevSelectedConn);
                }
            }

            // If the connection shall be edited, get its id
            long id = getActivity().getIntent().getLongExtra("id", 0);
            if (id > 0) {
                Log.i(TAG, "Saving existing connection with id " + id);
                DatabaseHelper.getInstance().updateConnection(conn);
            }
            else {
                Log.i(TAG, "Saving new connection");
                DatabaseHelper.getInstance().addConnection(conn);
            }

            getActivity().finish();
        }

        /**
         * Checks if the given name is not empty or does not contain special
         * characters which are not allowed in the database
         * 
         * @return
         */
        private boolean validateName() {

            if (conn.name.length() == 0) {
                Toast.makeText(getActivity(), getString(R.string.pref_name_error_empty), Toast.LENGTH_SHORT).show();
                return false;
            }

            // Check if the name contains only valid characters.
            Pattern pattern = Pattern.compile("^[0-9a-zA-Z_\\-\\.]*$");
            Matcher matcher = pattern.matcher(conn.name);
            if (!matcher.matches()) {
                Toast.makeText(getActivity(), getString(R.string.pref_name_error_invalid), Toast.LENGTH_SHORT).show();
                return false;
            }

            return true;
        }

        /**
         * Checks the given address for validity. It must not be empty and if it
         * is an IP address it must only contain numbers between 1 and 255 and
         * dots. If it is an host name it must contain only valid characters.
         * 
         * @return
         */
        private boolean validateAddress() {

            if (conn.address.length() == 0) {
                Toast.makeText(getActivity(), getString(R.string.pref_host_error_empty), Toast.LENGTH_SHORT).show();
                return false;
            }

            // Check if the name contains only valid characters.
            Pattern pattern = Pattern.compile("^[0-9a-zA-Z_\\-\\.]*$");
            Matcher matcher = pattern.matcher(conn.address);
            if (!matcher.matches()) {
                Toast.makeText(getActivity(), getString(R.string.pref_host_error_invalid), Toast.LENGTH_SHORT).show();
                return false;
            }

            // Check if the address has only numbers and dots in it.
            pattern = Pattern.compile("^[0-9\\.]*$");
            matcher = pattern.matcher(conn.address);

            // Now validate the IP address
            if (matcher.matches()) {
                pattern = Patterns.IP_ADDRESS;
                matcher = pattern.matcher(conn.address);
                if (!matcher.matches()) {
                    Toast.makeText(getActivity(), getString(R.string.pref_host_error_invalid), Toast.LENGTH_SHORT)
                            .show();
                    return false;
                }
            }
            return true;
        }

        /**
         * Validates the port number. It must not be empty and the value must be
         * between the allowed port range of zero to 65535.
         * 
         * @return
         */
        private boolean validatePort() {

            if (prefPort.getText().length() == 0) {
                Toast.makeText(getActivity(), getString(R.string.pref_port_error_empty), Toast.LENGTH_SHORT).show();
                return false;
            }
            if (conn.port <= 0 || conn.port > 65535) {
                Toast.makeText(getActivity(), getString(R.string.pref_port_error_invalid), Toast.LENGTH_SHORT).show();
                return false;
            }
            return true;
        }

        /**
         * Asks the user to confirm canceling the current activity. If no is
         * chosen the user can continue to add or edit the connection. Otherwise
         * the input will be discarded and the activity will be closed.
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
    }
}
