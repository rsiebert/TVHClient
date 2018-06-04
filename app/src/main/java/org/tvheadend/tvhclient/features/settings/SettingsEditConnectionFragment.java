package org.tvheadend.tvhclient.features.settings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;

import org.tvheadend.tvhclient.R;

import timber.log.Timber;

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
        }

        connection = viewModel.getConnectionByIdSync(connectionId);
    }

    @Override
    protected void save() {
        appRepository.getConnectionData().updateItem(connection);

        Timber.d("setting result");
        Intent intent = new Intent();
        intent.putExtra("connection_values_changed", true);
        activity.setResult(Activity.RESULT_OK, intent);
        activity.finish();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt("connection_id", connectionId);
        super.onSaveInstanceState(outState);
    }
}
