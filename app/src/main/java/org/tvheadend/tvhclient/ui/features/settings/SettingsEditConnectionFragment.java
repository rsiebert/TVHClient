package org.tvheadend.tvhclient.ui.features.settings;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.tvheadend.tvhclient.R;

public class SettingsEditConnectionFragment extends SettingsConnectionBaseFragment {

    private int connectionId;

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        toolbarInterface.setTitle(getString(R.string.edit_connection));

        if (savedInstanceState == null) {
            connectionValuesChanged = false;
            Bundle bundle = getArguments();
            if (bundle != null) {
                connectionId = bundle.getInt("connection_id", -1);
            }
        } else {
            connectionId = savedInstanceState.getInt("connection_id");
            connectionValuesChanged = savedInstanceState.getBoolean("connection_values_changed");
        }

        connection = appRepository.getConnectionData().getItemById(connectionId);
    }

    @Override
    protected void save() {
        appRepository.getConnectionData().updateItem(connection);
        viewModel.setConnectionHasChanged(connectionValuesChanged);
        activity.finish();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt("connection_id", connectionId);
        outState.putBoolean("connection_values_changed", connectionValuesChanged);
        super.onSaveInstanceState(outState);
    }
}
