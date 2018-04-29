package org.tvheadend.tvhclient.features.settings;

import android.os.Bundle;
import android.support.annotation.Nullable;

import org.tvheadend.tvhclient.R;

public class SettingsEditConnectionFragment extends SettingsConnectionBaseFragment {

    private int connectionId;

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        toolbarInterface.setTitle(getString(R.string.edit_connection));

        if (savedInstanceState == null) {
            Bundle bundle = getArguments();
            if (bundle != null) {
                connectionId = bundle.getInt("connection_id", -1);
            }
        } else {
            connectionId = savedInstanceState.getInt("connection_id");
            connectionValuesChanged = savedInstanceState.getBoolean("connection_settings_changed");
        }

        connection = viewModel.getConnectionByIdSync(connectionId);
    }

    @Override
    protected void save() {
        connectionRepository.updateConnectionSync(connection);
        activity.finish();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt("connection_id", connectionId);
        outState.putBoolean("connection_settings_changed", connectionValuesChanged);
        super.onSaveInstanceState(outState);
    }
}
