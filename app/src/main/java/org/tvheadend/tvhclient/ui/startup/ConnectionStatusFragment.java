package org.tvheadend.tvhclient.ui.startup;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.ui.settings.SettingsActivity;
import org.tvheadend.tvhclient.ui.base.ToolbarInterface;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class ConnectionStatusFragment extends Fragment {

    @BindView(R.id.status_text)
    TextView statusTextView;
    @BindView(R.id.status_background)
    ImageView statusImageView;
    @BindView(R.id.fab)
    FloatingActionButton floatingActionButton;
    private Unbinder unbinder;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_startup_layout, null);
        unbinder = ButterKnife.bind(this, view);
        return view;
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (getActivity() instanceof ToolbarInterface) {
            ToolbarInterface toolbarInterface = (ToolbarInterface) getActivity();
            toolbarInterface.setTitle("Connection Status");
        }

        // The connection status that was passed to this fragment
        String status = getArguments().getString("type");
        if (status != null) {
            floatingActionButton.setVisibility(View.VISIBLE);
            switch (status) {
                case "no_connections":
                    statusTextView.setText("No connection defined...");
                    floatingActionButton.setOnClickListener(v -> {
                        showSettingsActivity();
                    });
                    break;
                case "no_active_connection":
                    statusTextView.setText("At least one connection is defined but not active...");
                    floatingActionButton.setOnClickListener(v -> {
                        showSettingsActivity();
                    });
                    break;
                case "no_network":
                    statusTextView.setText("No network available, please activate wifi or mobile data...");
                    floatingActionButton.setOnClickListener(v -> {
                        Intent intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
                        startActivity(intent);
                    });
                    break;
            }
        }

        // Allow showing the toolbar menu with the settings menu
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.options_menu_startup, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_settings:
                showSettingsActivity();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showSettingsActivity() {
        Intent intent = new Intent(getActivity(), SettingsActivity.class);
        intent.putExtra("setting_type", "list_connections");
        intent.putExtra("initial_setup", true);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        startActivity(intent);
        getActivity().finish();
    }
}
