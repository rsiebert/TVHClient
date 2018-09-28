package org.tvheadend.tvhclient.features.settings;

import android.os.Bundle;
import android.support.annotation.Nullable;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.entity.Connection;

import timber.log.Timber;

public class SettingsAddConnectionFragment extends SettingsConnectionBaseFragment {

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        toolbarInterface.setTitle(getString(R.string.add_connection));
        connection = new Connection();
    }

    @Override
    protected void save() {
        Timber.d("Validating input before saving");
        if (!connection.isNameValid(connection.getName())) {
            showMessage(getString(R.string.pref_name_error_invalid));
        } else if (!connection.isIpAddressValid(connection.getHostname())) {
            showMessage(getString(R.string.pref_host_error_invalid));
        } else if (!connection.isPortValid(connection.getPort())) {
            showMessage(getString(R.string.pref_port_error_invalid));
        } else if (!connection.isPortValid(connection.getStreamingPort())) {
            showMessage(getString(R.string.pref_port_error_invalid));
        } else {
            appRepository.getConnectionData().addItem(connection);
            // Save the information in the view model that a new connection is active.
            // This will then trigger a reconnect when the user leaves the connection list screen
            if (connection.isActive()) {
                viewModel.setConnectionHasChanged(true);
            }
            activity.finish();
        }
    }
}
