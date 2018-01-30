package org.tvheadend.tvhclient.ui.settings;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.entity.Connection;
import org.tvheadend.tvhclient.data.repository.ConnectionDataRepository;
import org.tvheadend.tvhclient.ui.base.ToolbarInterface;
import org.tvheadend.tvhclient.ui.common.BackPressedInterface;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SettingsManageConnectionFragment extends PreferenceFragment implements BackPressedInterface, Preference.OnPreferenceChangeListener {

    private ToolbarInterface toolbarInterface;
    private Connection connection;
    private boolean connectionValuesChanged;
    private ConnectionDataRepository repository;
    private AppCompatActivity activity;

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

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences_add_connection);

        activity = (AppCompatActivity) getActivity();
        if (activity instanceof ToolbarInterface) {
            toolbarInterface = (ToolbarInterface) activity;
        }
        toolbarInterface.setTitle(getString(R.string.add_connection));
        setHasOptionsMenu(true);

        repository = new ConnectionDataRepository(activity);

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

        // Set the default values
        prefName.setOnPreferenceChangeListener(this);
        prefAddress.setOnPreferenceChangeListener(this);
        prefPort.setOnPreferenceChangeListener(this);
        prefStreamingPort.setOnPreferenceChangeListener(this);
        prefUsername.setOnPreferenceChangeListener(this);
        prefPassword.setOnPreferenceChangeListener(this);
        prefSelected.setOnPreferenceChangeListener(this);
        prefWolAddress.setOnPreferenceChangeListener(this);
        prefWolPort.setOnPreferenceChangeListener(this);
        prefWolBroadcast.setOnPreferenceChangeListener(this);

        // Initially the connection has no been changed
        connectionValuesChanged = false;

        // Restore the added connection information after an orientation change
        if (savedInstanceState != null) {
            connection = new Connection();
            connection.setId(savedInstanceState.getInt("id"));
            connection.setName(savedInstanceState.getString("name"));
            connection.setHostname(savedInstanceState.getString("address"));
            connection.setPort(savedInstanceState.getInt("port"));
            connection.setStreamingPort(savedInstanceState.getInt("streaming_port"));
            connection.setUsername(savedInstanceState.getString("username"));
            connection.setPassword(savedInstanceState.getString("password"));
            connection.setWolMacAddress(savedInstanceState.getString("wol_address"));
            connection.setWolPort(savedInstanceState.getInt("wol_port"));
            connection.setWolUseBroadcast(savedInstanceState.getBoolean("wol_use_broadcast"));
            connectionValuesChanged = savedInstanceState.getBoolean("connection_changed");
        } else {
            // If the bundle is null then a new connection shall be added.
            // Otherwise edit the connection with the given id
            Bundle bundle = getArguments();
            if (bundle == null) {
                connection = new Connection();
            } else {
                int connectionId = bundle.getInt("connection_id", 0);
                connection = repository.getConnectionSync(connectionId);
            }
        }

        toolbarInterface.setTitle(connection.getId() > 0 ?
                getString(R.string.edit_connection) :
                getString(R.string.add_connection));
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt("id", connection.getId());
        outState.putString("name", connection.getName());
        outState.putString("address", connection.getHostname());
        outState.putInt("port", connection.getPort());
        outState.putInt("streaming_port", connection.getStreamingPort());
        outState.putString("username", connection.getUsername());
        outState.putString("password", connection.getPassword());
        outState.putString("wol_address", connection.getWolMacAddress());
        outState.putInt("wol_port", connection.getWolPort());
        outState.putBoolean("wol_use_broadcast", connection.isWolUseBroadcast());
        outState.putBoolean("connection_changed", connectionValuesChanged);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onResume() {
        super.onResume();
        showPreferenceValues();
    }

    private void showPreferenceValues() {
        onPreferenceChange(prefName, connection.getName());
        onPreferenceChange(prefAddress, connection.getHostname());
        onPreferenceChange(prefPort, String.valueOf(connection.getPort()));
        onPreferenceChange(prefStreamingPort, String.valueOf(connection.getStreamingPort()));
        onPreferenceChange(prefUsername, connection.getUsername());
        onPreferenceChange(prefPassword, connection.getPassword());
        onPreferenceChange(prefSelected, connection.isActive());
        onPreferenceChange(prefWolAddress, connection.getWolMacAddress());
        onPreferenceChange(prefWolPort, connection.getWolPort());
        onPreferenceChange(prefWolBroadcast, connection.isWolUseBroadcast());
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.save_cancel_options_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.menu_save:
                repository.updateConnectionSync(connection);
                activity.finish();
                return true;
            case R.id.menu_cancel:
                cancel();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Asks the user to confirm canceling the current activity. If no is
     * chosen the user can continue to add or edit the connection. Otherwise
     * the input will be discarded and the activity will be closed.
     */
    private void cancel() {
        if (!connectionValuesChanged) {
            activity.finish();
        } else {
            // Show confirmation dialog to cancel
            new MaterialDialog.Builder(activity)
                    .content(R.string.confirm_discard_connection)
                    .positiveText(getString(R.string.discard))
                    .negativeText(getString(R.string.cancel))
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            activity.finish();
                        }
                    })
                    .onNegative(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            dialog.dismiss();
                        }
                    }).show();
        }
    }

    @Override
    public void onBackPressed() {
        cancel();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object o) {
        connectionValuesChanged = true;

        String value = String.valueOf(o);
        switch (preference.getKey()) {
            case "pref_name":
                if (validateName(value)) {
                    connection.setName(value);
                    prefName.setSummary(value.isEmpty() ?
                            getString(R.string.pref_name_sum) : value);
                }
                break;
            case "pref_address":
                if (validateIpAddress(value)) {
                    connection.setHostname(value);
                    prefAddress.setSummary(value.isEmpty() ?
                            getString(R.string.pref_host_sum) : value);
                }
                break;
            case "pref_port":
                try {
                    int port = Integer.parseInt(value);
                    if (validatePort(port)) {
                        connection.setPort(Integer.parseInt(value));
                        prefPort.setSummary(value.isEmpty() ?
                                getString(R.string.pref_port_sum) : value);
                    }
                } catch (NumberFormatException nex) {
                    // NOP
                }
                break;
            case "pref_streaming_port":
                try {
                    int port = Integer.parseInt(value);
                    if (validatePort(port)) {
                        connection.setStreamingPort(Integer.parseInt(value));
                        prefStreamingPort.setSummary(getString(R.string.pref_streaming_port_sum,
                                Integer.valueOf(value)));
                    }
                } catch (NumberFormatException e) {
                    // NOP
                }
                break;
            case "pref_username":
                connection.setUsername(value);
                prefUsername.setSummary(value.isEmpty() ?
                        getString(R.string.pref_user_sum) : value);
                break;
            case "pref_password":
                connection.setPassword(value);
                prefPassword.setSummary(value.isEmpty() ?
                        getString(R.string.pref_pass_sum) :
                        getString(R.string.pref_pass_set_sum));
                break;
            case "pref_selected":
                connection.setActive(Boolean.valueOf(value));
                break;
            case "pref_wol_address":
                if (validateMacAddress(value)) {
                    connection.setWolMacAddress(value);
                    prefWolAddress.setSummary(value.isEmpty() ?
                            getString(R.string.pref_wol_address_sum) : value);
                }
                break;
            case "pref_wol_port":
                try {
                    int port = Integer.parseInt(value);
                    if (validatePort(port)) {
                        connection.setWolPort(port);
                        prefWolPort.setSummary(getString(R.string.pref_wol_port_sum,
                                Integer.valueOf(value)));
                    }
                } catch (NumberFormatException e) {
                    // NOP
                }
                break;
            case "pref_wol_broadcast":
                connection.setWolUseBroadcast(Boolean.valueOf(value));
                break;
        }
        return true;
    }

    /**
     * Checks if the MAC address syntax is correct.
     *
     * @param macAddress The MAC address that shall be validated
     * @return True if MAC address is valid, otherwise false
     */
    private boolean validateMacAddress(String macAddress) {
        // Allow an empty address
        if (TextUtils.isEmpty(macAddress)) {
            return true;
        }
        // Check if the MAC address is valid
        Pattern pattern = Pattern.compile("([0-9a-fA-F]{2}(?::|-|$)){6}");
        Matcher matcher = pattern.matcher(macAddress);
        if (!matcher.matches()) {
            if (getView() != null) {
                Snackbar.make(getView(), R.string.pref_wol_address_invalid, Snackbar.LENGTH_SHORT).show();
            }
            return false;
        }
        return true;
    }

    /**
     * Checks if the given name is not empty or does not contain special
     * characters which are not allowed in the database
     *
     * @param name The name to be validated
     * @return True if name is valid, otherwise false
     */
    private boolean validateName(String name) {
        // Do not allow an empty address
        if (TextUtils.isEmpty(name)) {
            if (getView() != null) {
                Snackbar.make(getView(), R.string.pref_name_error_empty, Snackbar.LENGTH_SHORT).show();
            }
            return false;
        }
        // Check if the name contains only valid characters.
        Pattern pattern = Pattern.compile("^[0-9a-zA-Z_\\-\\.]*$");
        Matcher matcher = pattern.matcher(name);
        if (!matcher.matches()) {
            if (getView() != null) {
                Snackbar.make(getView(), R.string.pref_name_error_invalid, Snackbar.LENGTH_SHORT).show();
            }
            return false;
        }
        return true;
    }

    /**
     * Checks the given address for validity. It must not be empty and if it
     * is an IP address it must only contain numbers between 1 and 255 and
     * dots. If it is an host name it must contain only valid characters.
     *
     * @param address The address that shall be validated
     * @return True if IP address is valid, otherwise false
     */
    private boolean validateIpAddress(String address) {
        // Do not allow an empty address
        if (TextUtils.isEmpty(address)) {
            if (getView() != null) {
                Snackbar.make(getView(), R.string.pref_host_error_empty, Snackbar.LENGTH_SHORT).show();
            }
            return false;
        }

        // Check if the name contains only valid characters.
        Pattern pattern = Pattern.compile("^[0-9a-zA-Z_\\-\\.]*$");
        Matcher matcher = pattern.matcher(address);
        if (!matcher.matches()) {
            if (getView() != null) {
                Snackbar.make(getView(), R.string.pref_host_error_invalid, Snackbar.LENGTH_SHORT).show();
            }
            return false;
        }

        // Check if the address has only numbers and dots in it.
        pattern = Pattern.compile("^[0-9\\.]*$");
        matcher = pattern.matcher(address);

        // Now validate the IP address
        if (matcher.matches()) {
            pattern = Patterns.IP_ADDRESS;
            matcher = pattern.matcher(address);
            if (!matcher.matches()) {
                if (getView() != null) {
                    Snackbar.make(getView(), R.string.pref_host_error_invalid, Snackbar.LENGTH_SHORT).show();
                }
                return false;
            }
        }
        return true;
    }

    /**
     * Validates the port numbers. It must not be empty and the value must be
     * between the allowed port range of zero to 65535.
     *
     * @param port The port number
     * @return True if port is valid, otherwise false
     */
    private boolean validatePort(int port) {
        if (port > 0 && port <= 65535) {
            return true;
        }
        if (getView() != null) {
            Snackbar.make(getView(), R.string.pref_port_error_invalid,
                    Snackbar.LENGTH_SHORT).show();
        }
        return false;
    }

}
