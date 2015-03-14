package org.tvheadend.tvhclient.fragments;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.tvheadend.tvhclient.Constants;
import org.tvheadend.tvhclient.DatabaseHelper;
import org.tvheadend.tvhclient.PreferenceFragment;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.interfaces.ActionBarInterface;
import org.tvheadend.tvhclient.interfaces.SettingsInterface;
import org.tvheadend.tvhclient.model.Connection;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Patterns;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

public class SettingsManageConnectionFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {

    private final static String TAG = SettingsManageConnectionFragment.class.getSimpleName();
    
    private ActionBarActivity activity;
    private ActionBarInterface actionBarInterface;
    private SettingsInterface settingsInterface;
    
    // Preference widgets
    private EditTextPreference prefName;
    private EditTextPreference prefAddress;
    private EditTextPreference prefPort;
    private EditTextPreference prefStreamingPort;
    private EditTextPreference prefUsername;
    private EditTextPreference prefPassword;
    private CheckBoxPreference prefSelected;
    private EditTextPreference prefWolAddress;
    private EditTextPreference prefWolPort;
    private CheckBoxPreference prefWolBroadcast;
    
    private static final String CONNECTION_ID = "conn_id";
    private static final String CONNECTION_NAME = "conn_name";
    private static final String CONNECTION_ADDRESS = "conn_address";
    private static final String CONNECTION_STREAMING_PORT = "conn_streaming_port";
    private static final String CONNECTION_PORT = "conn_port";
    private static final String CONNECTION_USERNAME = "conn_username";
    private static final String CONNECTION_PASSWORD = "conn_password";
    private static final String CONNECTION_WOL_ADDRESS = "conn_wol_address";
    private static final String CONNECTION_WOL_PORT = "conn_wol_port";
    private static final String CONNECTION_WOL_BROADCAST = "conn_wol_broadcast";
    private static final String CONNECTION_CHANGED = "conn_changed";

    private static final String IP_ADDRESS = 
            "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
            "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
            "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
            "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";
    
    Connection conn = null;
    private boolean connectionChanged;
    
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences_add_connection);

        // Get the connectivity preferences for later usage
        prefName = (EditTextPreference) findPreference("pref_name");
        prefAddress = (EditTextPreference) findPreference("pref_address");
        prefPort = (EditTextPreference) findPreference("pref_port");
        prefStreamingPort = (EditTextPreference) findPreference("pref_streaming_port");
        prefUsername = (EditTextPreference) findPreference("pref_username");
        prefPassword = (EditTextPreference) findPreference("pref_password");
        prefSelected = (CheckBoxPreference) findPreference("pref_selected");
        prefWolAddress = (EditTextPreference) findPreference("pref_wol_address");
        prefWolPort = (EditTextPreference) findPreference("pref_wol_port");
        prefWolBroadcast = (CheckBoxPreference) findPreference("pref_wol_broadcast");
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = (ActionBarActivity) activity;
    }

    @Override
    public void onDetach() {
        actionBarInterface = null;
        settingsInterface = null;
        super.onDetach();
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (activity instanceof ActionBarInterface) {
            actionBarInterface = (ActionBarInterface) activity;
        }
        if (activity instanceof SettingsInterface) {
            settingsInterface = (SettingsInterface) activity;
        }
        if (actionBarInterface != null) {
            actionBarInterface.setActionBarTitle(getString(R.string.add_connection), TAG);
        }
        
        // Initially the connection has no been changed
        connectionChanged = false;

        // If the state is null then this activity has been started for
        // the first time. If the state is not null then the screen has
        // been rotated and we have to reuse the values.
        if (savedInstanceState == null) {
            
            // Check if an id has been passed
            long connId = 0;
            Bundle bundle = getArguments();
            if (bundle != null) {
                connId = bundle.getLong(Constants.BUNDLE_CONNECTION_ID, 0);
            }

            // If an index is given then we want to edit this connection
            // Otherwise create a new connection with default values.
            if (connId > 0) {
                conn = DatabaseHelper.getInstance().getConnection(connId);
            } else {
                setPreferenceDefaults();
            }
        } else {

            // Get the connection id and the object
            conn = new Connection();
            conn.id = savedInstanceState.getLong(CONNECTION_ID);
            conn.name = savedInstanceState.getString(CONNECTION_NAME);
            conn.address = savedInstanceState.getString(CONNECTION_ADDRESS);
            conn.port = savedInstanceState.getInt(CONNECTION_PORT);
            conn.streaming_port = savedInstanceState.getInt(CONNECTION_STREAMING_PORT);
            conn.username = savedInstanceState.getString(CONNECTION_USERNAME);
            conn.password = savedInstanceState.getString(CONNECTION_PASSWORD);
            conn.wol_address = savedInstanceState.getString(CONNECTION_WOL_ADDRESS);
            conn.wol_port = savedInstanceState.getInt(CONNECTION_WOL_PORT);
            conn.wol_broadcast = savedInstanceState.getBoolean(CONNECTION_WOL_BROADCAST);
            connectionChanged = savedInstanceState.getBoolean(CONNECTION_CHANGED);
        }
        setHasOptionsMenu(true);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putLong(CONNECTION_ID, conn.id);
        outState.putString(CONNECTION_NAME, conn.name);
        outState.putString(CONNECTION_ADDRESS, conn.address);
        outState.putInt(CONNECTION_PORT, conn.port);
        outState.putInt(CONNECTION_STREAMING_PORT, conn.streaming_port);
        outState.putString(CONNECTION_USERNAME, conn.username);
        outState.putString(CONNECTION_PASSWORD, conn.password);
        outState.putString(CONNECTION_WOL_ADDRESS, conn.wol_address);
        outState.putInt(CONNECTION_WOL_PORT, conn.wol_port);
        outState.putBoolean(CONNECTION_WOL_BROADCAST, conn.wol_broadcast);
        outState.putBoolean(CONNECTION_CHANGED, connectionChanged);
        super.onSaveInstanceState(outState);
    }

    /**
     * 
     */
    private void setPreferenceDefaults() {
        // Create a new connection with these defaults 
        conn = new Connection();
        conn.id = 0;
        conn.name = "";
        conn.address = "";
        conn.port = 9982;
        conn.streaming_port = 9981;
        conn.username = "";
        conn.password = "";
        conn.wol_address = "";
        conn.wol_port = 9;
        conn.wol_broadcast = false;
        // If this is the first connection make it active
        conn.selected = (DatabaseHelper.getInstance().getConnections().size() == 0) ? true : false;
    }

    @Override
    public void onResume() {
        super.onResume();
        showPreferenceValues();
        showPreferenceSummary();

        // Now register for any changes
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        prefs.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        prefs.unregisterOnSharedPreferenceChangeListener(this);
    }

    /**
     * 
     */
    private void showPreferenceValues() {
        prefName.setText(conn.name);
        prefAddress.setText(conn.address);
        prefPort.setText(String.valueOf(conn.port));
        prefStreamingPort.setText(String.valueOf(conn.streaming_port));
        prefUsername.setText(conn.username);
        prefPassword.setText(conn.password);
        prefSelected.setChecked(conn.selected);
        prefWolAddress.setText(conn.wol_address);
        prefWolPort.setText(String.valueOf(conn.wol_port));
        prefWolBroadcast.setChecked(conn.wol_broadcast);
    }
    
    /**
     * 
     */
    private void showPreferenceSummary() {
        prefName.setSummary(conn.name.length() == 0 ? 
                getString(R.string.pref_name_sum) : conn.name);
        prefAddress.setSummary(conn.address.length() == 0 ? 
                getString(R.string.pref_host_sum) : conn.address);
        prefPort.setSummary(conn.port == 0 ? 
                getString(R.string.pref_port_sum) : String.valueOf(conn.port));
        prefStreamingPort.setSummary(
                getString(R.string.pref_streaming_port_sum, conn.streaming_port));
        prefUsername.setSummary(conn.username.length() == 0 ? 
                getString(R.string.pref_user_sum) : conn.username);
        prefPassword.setSummary(conn.password.length() == 0 ? 
                getString(R.string.pref_pass_sum) : getString(R.string.pref_pass_set_sum));
        prefWolAddress.setSummary((conn.wol_address != null && conn.wol_address.length() == 0) ? 
                getString(R.string.pref_wol_address_sum) : conn.wol_address);
        prefWolPort.setSummary(getString(R.string.pref_wol_port_sum, conn.wol_port));
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
        connectionChanged = true;

        // Update the connection object with the new values
        conn.name = prefName.getText();
        conn.address = prefAddress.getText();
        try {
            conn.port = Integer.parseInt(prefPort.getText());
        } catch (NumberFormatException nex) {
            conn.port = 9982;
        }
        try {
            conn.streaming_port = Integer.parseInt(prefStreamingPort.getText());
        } catch (NumberFormatException nex) {
            conn.port = 9981;
        }
        conn.username = prefUsername.getText();
        conn.password = prefPassword.getText();
        conn.selected = prefSelected.isChecked();
        conn.wol_address = prefWolAddress.getText();
        try {
            conn.wol_port = Integer.parseInt(prefWolPort.getText());
        } catch (NumberFormatException nex) {
            conn.port = 9;
        }
        conn.wol_broadcast = prefWolBroadcast.isChecked();

        // Show the values from the connection object
        // in the summary text of the preferences
        showPreferenceSummary();
    }
    
    /**
     * Asks the user to confirm canceling the current activity. If no is
     * chosen the user can continue to add or edit the connection. Otherwise
     * the input will be discarded and the activity will be closed.
     */
    public void cancel() {
        // Do not show the cancel dialog if nothing has changed
        if (!connectionChanged) {
            settingsInterface.manageConnections();
            return;
        }
        // Show confirmation dialog to cancel
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage(getString(R.string.cancel));
        builder.setTitle(getString(R.string.menu_cancel));

        // Define the action of the yes button
        builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                // Delete the connection so that we start fresh when
                // the settings activity is called again.
                if (settingsInterface != null) {
                    settingsInterface.manageConnections();
                }
            }
        });
        // Define the action of the no button
        builder.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }
    
    /**
     * Validates the name, host name and port number. If they are valid the
     * values will be saved as a new connection or the existing will be
     * updated.
     */
    private void save() {
        if (conn == null 
                || !validateName() 
                || !validatePort() 
                || !validateIpAddress()
                || !validateMacAddress()) {
            return;
        }
        
        // If the current connection is set as selected
        // we need to unselect the previous one.
        if (prefSelected.isChecked()) {
            Connection prevSelectedConn = DatabaseHelper.getInstance().getSelectedConnection();
            if (prevSelectedConn != null) {
                prevSelectedConn.selected = false;
                DatabaseHelper.getInstance().updateConnection(prevSelectedConn);
            }
        }

        // If we have an id then the connection shall be updated
        if (conn.id > 0) {
            DatabaseHelper.getInstance().updateConnection(conn);
        } else {
            DatabaseHelper.getInstance().addConnection(conn);
        }
        
        // Nullify the connection so that we start fresh when
        // the settings activity is called again.
        conn = null;
        if (settingsInterface != null) {
            settingsInterface.reconnect();
            settingsInterface.manageConnections();
        }
    }
    
    /**
     * Checks if the MAC address syntax is correct.
     * 
     * @return
     */
    private boolean validateMacAddress() {
        // Initialize in case it's somehow null
        if (conn.wol_address == null) {
            conn.wol_address = "";
        }
        // Allow an empty address
        if (conn.wol_address.length() == 0) {
            return true;
        }
        // Check if the MAC address is valid
        Pattern pattern = Pattern.compile("([0-9a-fA-F]{2}(?::|-|$)){6}");
        Matcher matcher = pattern.matcher(conn.wol_address);
        if (!matcher.matches()) {
            Toast.makeText(getActivity(), getString(R.string.pref_wol_address_invalid), Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    /**
     * Checks if the given name is not empty or does not contain special
     * characters which are not allowed in the database
     * 
     * @return
     */
    private boolean validateName() {
        // Initialize in case it's somehow null
        if (conn.name == null) {
            conn.name = "";
        }
        // Do not allow an empty address
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
    @SuppressLint("NewApi")
    private boolean validateIpAddress() {
        // Initialize in case it's somehow null
        if (conn.address == null) {
            conn.address = "";
        }
        // Do not allow an empty address
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
            pattern = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) ? Patterns.IP_ADDRESS : Pattern.compile(IP_ADDRESS);
            matcher = pattern.matcher(conn.address);
            if (!matcher.matches()) {
                Toast.makeText(getActivity(), getString(R.string.pref_host_error_invalid), Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        return true;
    }

    /**
     * Validates the port numbers. It must not be empty and the value must be
     * between the allowed port range of zero to 65535.
     * 
     * @return
     */
    private boolean validatePort() {
        if (prefPort.getText().length() == 0 || 
                prefStreamingPort.getText().length() == 0) {
            Toast.makeText(getActivity(), getString(R.string.pref_port_error_empty), Toast.LENGTH_SHORT).show();
            return false;
        }
        if ((conn.port <= 0 || conn.port > 65535) ||
                (conn.streaming_port <= 0 || conn.streaming_port > 65535)) {
            Toast.makeText(getActivity(), getString(R.string.pref_port_error_invalid), Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }
}
