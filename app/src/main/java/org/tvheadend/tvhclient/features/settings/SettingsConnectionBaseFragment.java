package org.tvheadend.tvhclient.features.settings;

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
import org.tvheadend.tvhclient.features.shared.callbacks.BackPressedInterface;
import org.tvheadend.tvhclient.features.shared.callbacks.ToolbarInterface;

public abstract class SettingsConnectionBaseFragment extends PreferenceFragment implements BackPressedInterface, Preference.OnPreferenceChangeListener {

    protected boolean connectionValuesChanged;
    protected ToolbarInterface toolbarInterface;
    protected AppCompatActivity activity;
    protected Connection connection;
    protected ConnectionRepository repository;
    protected ConnectionViewModel viewModel;

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
        addPreferencesFromResource(R.xml.preferences_add_connection);

        activity = (AppCompatActivity) getActivity();
        if (activity instanceof ToolbarInterface) {
            toolbarInterface = (ToolbarInterface) activity;
        }

        repository = new ConnectionRepository(activity);
        viewModel = ViewModelProviders.of(activity).get(ConnectionViewModel.class);
        setHasOptionsMenu(true);

        // Get the connectivity preferences for later usage
        namePreference = (EditTextPreference) findPreference("name");
        hostnamePreference = (EditTextPreference) findPreference("hostname");
        htspPortPreference = (EditTextPreference) findPreference("htsp_port");
        streamingPortPreference = (EditTextPreference) findPreference("streaming_port");
        usernamePreference = (EditTextPreference) findPreference("username");
        passwordPreference = (EditTextPreference) findPreference("password");
        activeEnabledPreference = (CheckBoxPreference) findPreference("active_enabled");
        wolEnabledPreference = (CheckBoxPreference) findPreference("wol_enabled");
        wolMacAddressPreference = (EditTextPreference) findPreference("wol_mac_address");
        wolPortPreference = (EditTextPreference) findPreference("wol_port");
        wolUseBroadcastEnabled = (CheckBoxPreference) findPreference("wol_broadcast_enabled");

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
    public void onResume() {
        super.onResume();
        setPreferenceDefaultValues();
    }

    private void setPreferenceDefaultValues() {
        String name = connection.getName();
        namePreference.setText(name);
        namePreference.setSummary(name.isEmpty() ? getString(R.string.pref_name_sum) : name);

        String address = connection.getHostname();
        hostnamePreference.setText(address);
        hostnamePreference.setSummary(address.isEmpty() ? getString(R.string.pref_host_sum) : address);

        String port = String.valueOf(connection.getPort());
        htspPortPreference.setText(port);
        htspPortPreference.setSummary(port);

        streamingPortPreference.setText(String.valueOf(connection.getPort()));
        streamingPortPreference.setSummary(getString(R.string.pref_streaming_port_sum, connection.getPort()));

        String username = connection.getUsername();
        usernamePreference.setText(username);
        usernamePreference.setSummary(username.isEmpty() ? getString(R.string.pref_user_sum) : username);

        String password = connection.getPassword();
        passwordPreference.setText(password);
        passwordPreference.setSummary(password.isEmpty() ? getString(R.string.pref_pass_sum) : getString(R.string.pref_pass_set_sum));

        activeEnabledPreference.setChecked(connection.isActive());
        wolEnabledPreference.setChecked(connection.isWolEnabled());

        if (!connection.isWolEnabled()) {
            String macAddress = connection.getWolMacAddress();
            wolMacAddressPreference.setText(macAddress);
            wolMacAddressPreference.setSummary(macAddress.isEmpty() ? getString(R.string.pref_wol_address_sum) : macAddress);
            wolPortPreference.setText(String.valueOf(connection.getWolPort()));
            wolPortPreference.setSummary(getString(R.string.pref_wol_port_sum, connection.getWolPort()));
            wolUseBroadcastEnabled.setChecked(connection.isWolUseBroadcast());
        }
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
            case "name":
                if (connection.isNameValid(value)) {
                    connection.setName(value);
                    namePreference.setText(value);
                    namePreference.setSummary(value.isEmpty() ? getString(R.string.pref_name_sum) : value);
                } else {
                    namePreference.setText(connection.getName());
                    if (getView() != null) {
                        showMessage(getString(R.string.pref_name_error_invalid));
                    }
                }
                break;
            case "hostname":
                if (connection.isIpAddressValid(value)) {
                    connection.setHostname(value);
                    hostnamePreference.setText(value);
                    hostnamePreference.setSummary(value.isEmpty() ? getString(R.string.pref_host_sum) : value);
                } else {
                    hostnamePreference.setText(connection.getHostname());
                    if (getView() != null) {
                        showMessage(getString(R.string.pref_host_error_invalid));
                    }
                }
                break;
            case "htsp_port":
                try {
                    int port = Integer.parseInt(value);
                    if (connection.isPortValid(port)) {
                        connection.setPort(port);
                        htspPortPreference.setText(String.valueOf(port));
                        htspPortPreference.setSummary(String.valueOf(port));
                    } else {
                        htspPortPreference.setText(String.valueOf(connection.getPort()));
                        if (getView() != null) {
                            showMessage(getString(R.string.pref_port_error_invalid));
                        }
                    }
                } catch (NumberFormatException nex) {
                    // NOP
                }
                break;
            case "streaming_port":
                try {
                    int port = Integer.parseInt(value);
                    if (connection.isPortValid(port)) {
                        connection.setStreamingPort(port);
                        streamingPortPreference.setText(value);
                        streamingPortPreference.setSummary(getString(R.string.pref_streaming_port_sum, port));
                    } else {
                        streamingPortPreference.setText(String.valueOf(connection.getStreamingPort()));
                        if (getView() != null) {
                            showMessage(getString(R.string.pref_port_error_invalid));
                        }
                    }
                } catch (NumberFormatException e) {
                    // NOP
                }
                break;
            case "username":
                connection.setUsername(value);
                usernamePreference.setText(value);
                usernamePreference.setSummary(value.isEmpty() ? getString(R.string.pref_user_sum) : value);
                break;
            case "password":
                connection.setPassword(value);
                passwordPreference.setText(value);
                passwordPreference.setSummary(value.isEmpty() ? getString(R.string.pref_pass_sum) : getString(R.string.pref_pass_set_sum));
                break;
            case "active_enabled":
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
                    if (getView() != null) {
                        showMessage(getString(R.string.pref_wol_address_invalid));
                    }
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
                        showMessage(getString(R.string.pref_port_error_invalid));
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

    protected void showMessage(String msgid) {
        if (getView() != null) {
            Snackbar.make(getView(), msgid, Snackbar.LENGTH_SHORT).show();
        }
    }
}
