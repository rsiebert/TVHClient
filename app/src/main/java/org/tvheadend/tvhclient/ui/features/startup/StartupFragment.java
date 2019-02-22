package org.tvheadend.tvhclient.ui.features.startup;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.tvheadend.tvhclient.MainApplication;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.repository.AppRepository;
import org.tvheadend.tvhclient.ui.features.MainActivity;
import org.tvheadend.tvhclient.ui.features.settings.SettingsActivity;
import org.tvheadend.tvhclient.data.service.SyncStateReceiver;
import org.tvheadend.tvhclient.util.menu.MenuUtils;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import timber.log.Timber;

// TODO add nice background image

public class StartupFragment extends Fragment {

    @BindView(R.id.progress_bar)
    ProgressBar progressBar;
    @BindView(R.id.state)
    TextView stateTextView;
    @BindView(R.id.details)
    TextView detailsTextView;
    @BindView(R.id.add_connection_button)
    ImageButton addConnectionButton;
    @BindView(R.id.settings_button)
    ImageButton settingsButton;

    private Unbinder unbinder;
    private AppCompatActivity activity;
    @Inject
    protected AppRepository appRepository;
    @Inject
    protected SharedPreferences sharedPreferences;
    private String stateText;
    private String detailsText;
    private SyncStateReceiver.State state;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.startup_fragment, container, false);
        unbinder = ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        MainApplication.getComponent().inject(this);
        activity = (AppCompatActivity) getActivity();
        setHasOptionsMenu(true);

        if (savedInstanceState != null) {
            state = ((SyncStateReceiver.State) savedInstanceState.getSerializable("state"));
            stateText = savedInstanceState.getString("stateText");
            detailsText = savedInstanceState.getString("detailsText");
        } else {
            state = SyncStateReceiver.State.IDLE;
            stateText = getString(R.string.initializing);
            detailsText = "";
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putSerializable("state", state);
        outState.putString("stateText", stateTextView.getText().toString());
        outState.putString("detailsText", detailsTextView.getText().toString());
        super.onSaveInstanceState(outState);
    }

    private void handleStartupProcedure() {

        if (appRepository.getConnectionData().getItems().size() == 0) {
            Timber.d("No connection available, showing settings button");
            stateText = getString(R.string.no_connection_available);
            progressBar.setVisibility(View.INVISIBLE);
            addConnectionButton.setVisibility(View.VISIBLE);
            addConnectionButton.setOnClickListener(v -> showSettingsAddNewConnection());

        } else if (appRepository.getConnectionData().getActiveItem() == null) {
            Timber.d("No active connection available, showing settings button");
            stateText = getString(R.string.no_connection_active_advice);
            progressBar.setVisibility(View.INVISIBLE);
            settingsButton.setVisibility(View.VISIBLE);
            settingsButton.setOnClickListener(v -> showConnectionListSettings());

        } else {
            Timber.d("Connection is available and active, showing contents");
            showContentScreen();
        }

        stateTextView.setText(stateText);
        detailsTextView.setText(detailsText);
    }

    @Override
    public void onResume() {
        super.onResume();
        handleStartupProcedure();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.startup_options_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_settings:
                showConnectionListSettings();
                return true;
            case R.id.menu_refresh:
                new MenuUtils(activity).handleMenuReconnectSelection();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showSettingsAddNewConnection() {
        Intent intent = new Intent(activity, SettingsActivity.class);
        intent.putExtra("setting_type", "add_connection");
        startActivity(intent);
    }

    private void showConnectionListSettings() {
        Intent intent = new Intent(activity, SettingsActivity.class);
        intent.putExtra("setting_type", "list_connections");
        startActivity(intent);
    }

    /**
     * Shows the main fragment like the channel list when the startup is complete.
     * Which fragment shall be shown is determined by a preference.
     * The connection to the server will be established by the network status
     * which is monitored in the base activity which the main activity inherits.
     */
    private void showContentScreen() {
        Intent intent = new Intent(activity, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.startActivity(intent);
        activity.finish();
    }
}
