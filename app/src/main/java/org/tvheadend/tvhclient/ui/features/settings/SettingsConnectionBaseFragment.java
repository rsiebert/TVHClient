package org.tvheadend.tvhclient.ui.features.settings;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.afollestad.materialdialogs.MaterialDialog;

import org.tvheadend.tvhclient.MainApplication;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.repository.AppRepository;
import org.tvheadend.tvhclient.domain.entity.Connection;
import org.tvheadend.tvhclient.ui.base.callbacks.BackPressedInterface;
import org.tvheadend.tvhclient.ui.base.callbacks.ToolbarInterface;
import org.tvheadend.tvhclient.ui.base.utils.SnackbarUtils;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProviders;
import androidx.preference.CheckBoxPreference;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

public abstract class SettingsConnectionBaseFragment extends PreferenceFragmentCompat implements BackPressedInterface, Preference.OnPreferenceChangeListener {

    @Inject
    AppRepository appRepository;

    AppCompatActivity activity;
    ToolbarInterface toolbarInterface;
    boolean connectionValuesChanged;
    Connection connection;
    ConnectionViewModel viewModel;

    private EditTextPreference namePreference;
    private EditTextPreference hostnamePreference;
    private EditTextPreference htspPortPreference;
    private EditTextPreference streamingPortPreference;
    private EditTextPreference usernamePreference;
    private EditTextPreference passwordPreference;
    private CheckBoxPreference activeEnabledPreference;
    private EditTextPreference wolMacAddressPreference;
    private EditTextPreference wolPortPreference;
    private CheckBoxPreference wolEnabledPreference;
    private CheckBoxPreference wolUseBroadcastEnabled;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        activity = (AppCompatActivity) getActivity();
        if (activity instanceof ToolbarInterface) {
            toolbarInterface = (ToolbarInterface) activity;
        }
        MainApplication.getComponent().inject(this);

        viewModel = ViewModelProviders.of(activity).get(ConnectionViewModel.class);
        setHasOptionsMenu(true);

        // Get the connectivity preferences for later usage
        namePreference = findPreference("name");
        hostnamePreference = findPreference("hostname");
        htspPortPreference = findPreference("htsp_port");
        streamingPortPreference = findPreference("streaming_port");
        usernamePreference = findPreference("username");
        passwordPreference = findPreference("password");
        activeEnabledPreference = findPreference("active_enabled");
        wolEnabledPreference = findPreference("wol_enabled");
        wolMacAddressPreference = findPreference("wol_mac_address");
        wolPortPreference = findPreference("wol_port");
        wolUseBroadcastEnabled = findPreference("wol_broadcast_enabled");

        namePreference.setOnPreferenceChangeListener(this);
        hostnamePreference.setOnPreferenceChangeListener(this);
        htspPortPreference.setOnPreferenceChangeListener(this);
        streamingPortPreference.setOnPreferenceChangeListener(this);
        usernamePreference.setOnPreferenceChangeListener(this);
        passwordPreference.setOnPreferenceChangeListener(this);
        activeEnabledPreference.setOnPreferenceChangeListener(this);
        wolEnabledPreference.setOnPreferenceChangeListener(this);
        wolMacAddressPreference.setOnPreferenceChangeListener(this);
        wolPortPreference.setOnPreferenceChangeListener(this);
        wolUseBroadcastEnabled.setOnPreferenceChangeListener(this);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences_add_connection, rootKey);
    }

    @Override
    public void onResume() {
        super.onResume();
        setPreferenceDefaultValues();
    }

    private void setPreferenceDefaultValues() {
        String name = connection.getName();
        namePreference.setText(name);
        namePreference.setSummary(TextUtils.isEmpty(name) ? getString(R.string.pref_name_sum) : name);

        String address = connection.getHostname();
        hostnamePreference.setText(address);
        hostnamePreference.setSummary(TextUtils.isEmpty(address) ? getString(R.string.pref_host_sum) : address);

        String port = String.valueOf(connection.getPort());
        htspPortPreference.setText(port);
        htspPortPreference.setSummary(port);

        streamingPortPreference.setText(String.valueOf(connection.getStreamingPort()));
        streamingPortPreference.setSummary(getString(R.string.pref_streaming_port_sum, connection.getStreamingPort()));

        String username = connection.getUsername();
        usernamePreference.setText(username);
        usernamePreference.setSummary(TextUtils.isEmpty(username) ? getString(R.string.pref_user_sum) : username);

        String password = connection.getPassword();
        passwordPreference.setText(password);
        passwordPreference.setSummary(TextUtils.isEmpty(password) ? getString(R.string.pref_pass_sum) : getString(R.string.pref_pass_set_sum));

        activeEnabledPreference.setChecked(connection.isActive());
        wolEnabledPreference.setChecked(connection.isWolEnabled());

        if (!connection.isWolEnabled()) {
            String macAddress = connection.getWolMacAddress();
            wolMacAddressPreference.setText(macAddress);
            wolMacAddressPreference.setSummary(TextUtils.isEmpty(macAddress) ? getString(R.string.pref_wol_address_sum) : macAddress);
            wolPortPreference.setText(String.valueOf(connection.getWolPort()));
            wolPortPreference.setSummary(getString(R.string.pref_wol_port_sum, connection.getWolPort()));
            wolUseBroadcastEnabled.setChecked(connection.isWolUseBroadcast());
        }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.save_cancel_options_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
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

    protected abstract void save();

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
                    .onPositive((dialog, which) -> activity.finish())
                    .onNegative((dialog, which) -> dialog.dismiss())
                    .show();
        }
    }

    @Override
    public void onBackPressed() {
        cancel();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object o) {
        String value = String.valueOf(o);
        switch (preference.getKey()) {
            case "name":
                if (connection.isNameValid(value)) {
                    connection.setName(value);
                    namePreference.setText(value);
                    namePreference.setSummary(value.isEmpty() ? getString(R.string.pref_name_sum) : value);
                } else {
                    namePreference.setText(connection.getName());
                    SnackbarUtils.sendSnackbarMessage(activity, R.string.pref_name_error_invalid);
                }
                break;
            case "hostname":
                connectionValuesChanged = true;
                if (connection.isIpAddressValid(value)) {
                    connection.setHostname(value);
                    hostnamePreference.setText(value);
                    hostnamePreference.setSummary(value.isEmpty() ? getString(R.string.pref_host_sum) : value);
                } else {
                    hostnamePreference.setText(connection.getHostname());
                    SnackbarUtils.sendSnackbarMessage(activity, R.string.pref_host_error_invalid);
                }
                break;
            case "htsp_port":
                connectionValuesChanged = true;
                try {
                    int port = Integer.parseInt(value);
                    if (connection.isPortValid(port)) {
                        connection.setPort(port);
                        htspPortPreference.setText(String.valueOf(port));
                        htspPortPreference.setSummary(String.valueOf(port));
                    } else {
                        htspPortPreference.setText(String.valueOf(connection.getPort()));
                        SnackbarUtils.sendSnackbarMessage(activity, R.string.pref_port_error_invalid);
                    }
                } catch (NumberFormatException nex) {
                    // NOP
                }
                break;
            case "streaming_port":
                connectionValuesChanged = true;
                try {
                    int port = Integer.parseInt(value);
                    if (connection.isPortValid(port)) {
                        connection.setStreamingPort(port);
                        streamingPortPreference.setText(value);
                        streamingPortPreference.setSummary(getString(R.string.pref_streaming_port_sum, port));
                    } else {
                        streamingPortPreference.setText(String.valueOf(connection.getStreamingPort()));
                        SnackbarUtils.sendSnackbarMessage(activity, R.string.pref_port_error_invalid);
                    }
                } catch (NumberFormatException e) {
                    // NOP
                }
                break;
            case "username":
                connectionValuesChanged = true;
                connection.setUsername(value);
                usernamePreference.setText(value);
                usernamePreference.setSummary(value.isEmpty() ? getString(R.string.pref_user_sum) : value);
                break;
            case "password":
                connectionValuesChanged = true;
                connection.setPassword(value);
                passwordPreference.setText(value);
                passwordPreference.setSummary(value.isEmpty() ? getString(R.string.pref_pass_sum) : getString(R.string.pref_pass_set_sum));
                break;
            case "active_enabled":
                connectionValuesChanged = true;
                // When the connection was set as the new active
                // connection, an initial sync is required
                boolean isActive = Boolean.valueOf(value);
                if (!connection.isActive() && isActive) {
                    connection.setSyncRequired(true);
                    connection.setLastUpdate(0);
                }
                connection.setActive(Boolean.valueOf(value));
                break;
            case "wol_enabled":
                connection.setWolEnabled(Boolean.valueOf(value));
                break;
            case "wol_mac_address":
                if (connection.isWolMacAddressValid(value)) {
                    connection.setWolMacAddress(value);
                    wolMacAddressPreference.setText(value);
                    wolMacAddressPreference.setSummary(value.isEmpty() ? getString(R.string.pref_wol_address_sum) : value);
                } else {
                    wolMacAddressPreference.setText(connection.getWolMacAddress());
                    SnackbarUtils.sendSnackbarMessage(activity, R.string.pref_wol_address_invalid);
                }
                break;
            case "wol_port":
                try {
                    int port = Integer.parseInt(value);
                    if (connection.isPortValid(port)) {
                        connection.setWolPort(port);
                        wolPortPreference.setText(value);
                        wolPortPreference.setSummary(getString(R.string.pref_wol_port_sum, port));
                    } else {
                        wolPortPreference.setText(String.valueOf(connection.getWolPort()));
                        SnackbarUtils.sendSnackbarMessage(activity, R.string.pref_port_error_invalid);
                    }
                } catch (NumberFormatException e) {
                    // NOP
                }
                break;
            case "wol_broadcast_enabled":
                connection.setWolUseBroadcast(Boolean.valueOf(value));
                break;
        }
        return true;
    }
}
