package org.tvheadend.tvhclient.ui.settings;

import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.entity.Connection;
import org.tvheadend.tvhclient.data.repository.ConnectionRepository;
import org.tvheadend.tvhclient.ui.base.ToolbarInterface;
import org.tvheadend.tvhclient.ui.common.BackPressedInterface;

public class SettingsManageConnectionFragment extends PreferenceFragment implements BackPressedInterface, Preference.OnPreferenceChangeListener {
    @SuppressWarnings("unused")
    private String TAG = getClass().getSimpleName();

    private ToolbarInterface toolbarInterface;
    private Connection connection;
    private boolean connectionValuesChanged;
    private ConnectionRepository repository;
    private AppCompatActivity activity;

    private EditTextPreference prefName;
    private EditTextPreference prefAddress;
    private EditTextPreference prefPort;
    private EditTextPreference prefStreamingPort;
    private EditTextPreference prefUsername;
    private EditTextPreference prefPassword;
    private EditTextPreference prefWolAddress;
    private EditTextPreference prefWolPort;
    private CheckBoxPreference prefSelected;
    private CheckBoxPreference prefWolEnabled;
    private CheckBoxPreference prefWolBroadcast;
    private int connectionId;
    //private boolean isActive;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences_add_connection);

        activity = (AppCompatActivity) getActivity();
        if (activity instanceof ToolbarInterface) {
            toolbarInterface = (ToolbarInterface) activity;
        }
        toolbarInterface.setTitle(getString(R.string.add_connection));
        repository = new ConnectionRepository(activity);
        setHasOptionsMenu(true);

        // Get the connectivity preferences for later usage
        prefName = (EditTextPreference) findPreference("pref_name");
        prefAddress = (EditTextPreference) findPreference("pref_address");
        prefPort = (EditTextPreference) findPreference("pref_port");
        prefStreamingPort = (EditTextPreference) findPreference("pref_streaming_port");
        prefUsername = (EditTextPreference) findPreference("pref_username");
        prefPassword = (EditTextPreference) findPreference("pref_password");
        prefSelected = (CheckBoxPreference) findPreference("pref_selected");
        prefWolEnabled = (CheckBoxPreference) findPreference("pref_wol_enabled");
        prefWolAddress = (EditTextPreference) findPreference("pref_wol_address");
        prefWolPort = (EditTextPreference) findPreference("pref_wol_port");
        prefWolBroadcast = (CheckBoxPreference) findPreference("pref_wol_broadcast");

        prefName.setOnPreferenceChangeListener(this);
        prefAddress.setOnPreferenceChangeListener(this);
        prefPort.setOnPreferenceChangeListener(this);
        prefStreamingPort.setOnPreferenceChangeListener(this);
        prefUsername.setOnPreferenceChangeListener(this);
        prefPassword.setOnPreferenceChangeListener(this);
        prefSelected.setOnPreferenceChangeListener(this);
        prefWolEnabled.setOnPreferenceChangeListener(this);
        prefWolAddress.setOnPreferenceChangeListener(this);
        prefWolPort.setOnPreferenceChangeListener(this);
        prefWolBroadcast.setOnPreferenceChangeListener(this);

        if (savedInstanceState == null) {
            Bundle bundle = getArguments();
            if (bundle != null) {
                connectionId = bundle.getInt("connection_id", 0);
            }
        } else {
            connectionId = savedInstanceState.getInt("id");
            connectionValuesChanged = savedInstanceState.getBoolean("connectionValuesChanged");
        }

        // Create the view model that stores the connection model
        ConnectionViewModel viewModel = ViewModelProviders.of(activity).get(ConnectionViewModel.class);
        connection = viewModel.getConnectionByIdSync(connectionId);

        setPreferenceDefaultValues();

        toolbarInterface.setTitle(connection.getId() > 0 ?
                getString(R.string.edit_connection) :
                getString(R.string.add_connection));
    }

    private void setPreferenceDefaultValues() {
        String name = connection.getName();
        prefName.setText(name);
        prefName.setSummary(name.isEmpty() ? getString(R.string.pref_name_sum) : name);

        String address = connection.getHostname();
        prefAddress.setText(address);
        prefAddress.setSummary(address.isEmpty() ? getString(R.string.pref_host_sum) : address);

        String port = String.valueOf(connection.getPort());
        prefPort.setText(port);
        prefPort.setSummary(port);

        prefStreamingPort.setText(String.valueOf(connection.getPort()));
        prefStreamingPort.setSummary(getString(R.string.pref_streaming_port_sum, connection.getPort()));

        String username = connection.getUsername();
        prefUsername.setText(username);
        prefUsername.setSummary(username.isEmpty() ? getString(R.string.pref_user_sum) : username);

        String password = connection.getPassword();
        prefPassword.setText(password);
        prefPassword.setSummary(password.isEmpty() ? getString(R.string.pref_pass_sum) : getString(R.string.pref_pass_set_sum));

        //isActive = connection.isActive();
        prefSelected.setChecked(connection.isActive());
        prefWolEnabled.setChecked(connection.isWolEnabled());

        if (!connection.isWolEnabled()) {
            String macAddress = connection.getWolMacAddress();
            prefWolAddress.setText(macAddress);
            prefWolAddress.setSummary(macAddress.isEmpty() ? getString(R.string.pref_wol_address_sum) : macAddress);
            prefWolPort.setText(String.valueOf(connection.getWolPort()));
            prefWolPort.setSummary(getString(R.string.pref_wol_port_sum, connection.getWolPort()));
            prefWolBroadcast.setChecked(connection.isWolUseBroadcast());
        }


    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt("id", connection.getId());
        outState.putBoolean("connectionValuesChanged", connectionValuesChanged);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.save_cancel_options_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
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

    private void save() {
        repository.updateConnectionSync(connection);
        /*
        // Do a restart when a new connection was added an is set
        // as active or an existing connection is set as not active
        if (connection.getId() == 0 && isActive) {
            new MaterialDialog.Builder(activity)
                    .title("Connect to new server?")
                    .content("A new active connection was added. Do you want to connect to the server?\n" +
                            "The application will be restarted and a new initial sync will be performed.")
                    .negativeText(R.string.cancel)
                    .positiveText("Reconnect")
                    .onPositive((dialog, which) -> reconnect())
                    .onNegative(((dialog, which) -> activity.finish()))
                    .show();

        } else if (connection.getId() > 0 && !isActive) {
            new MaterialDialog.Builder(activity)
                    .title("Close connection?")
                    .content("An existing connection was marked as inactive. The connection to the server will be closed.")
                    .negativeText(R.string.cancel)
                    .positiveText("Close")
                    .onPositive((dialog, which) -> reconnect())
                    .onNegative(((dialog, which) -> activity.finish()))
                    .show();
        } else {
            activity.finish();
        }
        */
        activity.finish();
    }
/*
    private void reconnect() {
        // Save the information that a new sync is required
        // Then restart the application to show the sync fragment
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("initial_sync_done", false);
        editor.apply();
        Intent intent = new Intent(activity, StartupActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.finish();
    }
*/
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
                if (connection.isNameValid(value)) {
                    connection.setName(value);
                    prefName.setText(value);
                    prefName.setSummary(value.isEmpty() ? getString(R.string.pref_name_sum) : value);
                } else {
                    prefName.setText(connection.getName());
                    if (getView() != null) {
                        Snackbar.make(getView(), R.string.pref_name_error_invalid, Snackbar.LENGTH_SHORT).show();
                    }
                }
                break;
            case "pref_address":
                if (connection.isIpAddressValid(value)) {
                    connection.setHostname(value);
                    prefAddress.setText(value);
                    prefAddress.setSummary(value.isEmpty() ? getString(R.string.pref_host_sum) : value);
                } else {
                    prefAddress.setText(connection.getHostname());
                    if (getView() != null) {
                        Snackbar.make(getView(), R.string.pref_host_error_invalid, Snackbar.LENGTH_SHORT).show();
                    }
                }
                break;
            case "pref_port":
                try {
                    int port = Integer.parseInt(value);
                    if (connection.isPortValid(port)) {
                        connection.setPort(port);
                        prefPort.setText(String.valueOf(port));
                        prefPort.setSummary(String.valueOf(port));
                    } else {
                        prefPort.setText(String.valueOf(connection.getPort()));
                        if (getView() != null) {
                            Snackbar.make(getView(), R.string.pref_port_error_invalid, Snackbar.LENGTH_SHORT).show();
                        }
                    }
                } catch (NumberFormatException nex) {
                    // NOP
                }
                break;
            case "pref_streaming_port":
                try {
                    int port = Integer.parseInt(value);
                    if (connection.isPortValid(port)) {
                        connection.setStreamingPort(port);
                        prefStreamingPort.setText(value);
                        prefStreamingPort.setSummary(getString(R.string.pref_streaming_port_sum, port));
                    } else {
                        prefStreamingPort.setText(String.valueOf(connection.getStreamingPort()));
                        if (getView() != null) {
                            Snackbar.make(getView(), R.string.pref_port_error_invalid, Snackbar.LENGTH_SHORT).show();
                        }
                    }
                } catch (NumberFormatException e) {
                    // NOP
                }
                break;
            case "pref_username":
                connection.setUsername(value);
                prefUsername.setText(value);
                prefUsername.setSummary(value.isEmpty() ? getString(R.string.pref_user_sum) : value);
                break;
            case "pref_password":
                connection.setPassword(value);
                prefPassword.setText(value);
                prefPassword.setSummary(value.isEmpty() ? getString(R.string.pref_pass_sum) : getString(R.string.pref_pass_set_sum));
                break;
            case "pref_selected":
                //isActive = Boolean.valueOf(value);
                connection.setActive(Boolean.valueOf(value));
                break;
            case "pref_wol_enabled":
                connection.setWolEnabled(Boolean.valueOf(value));
                break;
            case "pref_wol_address":
                if (connection.isWolMacAddressValid(value)) {
                    connection.setWolMacAddress(value);
                    prefWolAddress.setText(value);
                    prefWolAddress.setSummary(value.isEmpty() ? getString(R.string.pref_wol_address_sum) : value);
                } else {
                    prefWolAddress.setText(connection.getWolMacAddress());
                    if (getView() != null) {
                        Snackbar.make(getView(), R.string.pref_wol_address_invalid, Snackbar.LENGTH_SHORT).show();
                    }
                }
                break;
            case "pref_wol_port":
                try {
                    int port = Integer.parseInt(value);
                    if (connection.isPortValid(port)) {
                        connection.setWolPort(port);
                        prefWolPort.setText(value);
                        prefWolPort.setSummary(getString(R.string.pref_wol_port_sum, port));
                    } else {
                        prefWolPort.setText(String.valueOf(connection.getWolPort()));
                        if (getView() != null) {
                            Snackbar.make(getView(), R.string.pref_port_error_invalid, Snackbar.LENGTH_SHORT).show();
                        }
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

}
