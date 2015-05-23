package org.tvheadend.tvhclient.fragments;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.tvheadend.tvhclient.Constants;
import org.tvheadend.tvhclient.DatabaseHelper;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.htsp.HTSService;
import org.tvheadend.tvhclient.interfaces.FragmentStatusInterface;
import org.tvheadend.tvhclient.model.Channel;
import org.tvheadend.tvhclient.model.Connection;
import org.tvheadend.tvhclient.model.Profile;
import org.tvheadend.tvhclient.model.Recording;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.github.jjobes.slidedatetimepicker.SlideDateTimeListener;
import com.github.jjobes.slidedatetimepicker.SlideDateTimePicker;

@SuppressWarnings("deprecation")
public class RecordingAddFragment extends DialogFragment {

    private final static String TAG = RecordingAddFragment.class.getSimpleName();

    private ActionBarActivity activity;
    private Recording rec;
    private Toolbar toolbar;

    private TextView startTime;
    private TextView stopTime;
    private TextView priority;
    private EditText startExtra;
    private EditText stopExtra;
    private EditText title;
    private EditText description;
    private TextView channelName;

    private long startExtraValue;
    private long stopExtraValue;
    private long startTimeValue;
    private long stopTimeValue;
    private long priorityValue;
    private String titleValue;
    private String descriptionValue;
    private int channelSelectionValue;

    String[] channelList;
    String[] priorityList;

    private static final int DEFAULT_START_EXTRA = 2;
    private static final int DEFAULT_STOP_EXTRA = 2;

    public static RecordingAddFragment newInstance(Bundle args) {
        RecordingAddFragment f = new RecordingAddFragment();
        f.setArguments(args);
        return f;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = (ActionBarActivity) activity;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getDialog() != null) {
            getDialog().getWindow().getAttributes().windowAnimations = R.style.dialog_animation_fade;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        getValues();
        outState.putLong("priorityValue", priorityValue);
        outState.putLong("startTimeValue", startTimeValue);
        outState.putLong("stopTimeValue", stopTimeValue);
        outState.putLong("startExtraValue", startExtraValue);
        outState.putLong("stopExtraValue", stopExtraValue);
        outState.putString("titleValue", titleValue);
        outState.putString("descriptionValue", descriptionValue);
        outState.putInt("channelNameValue", channelSelectionValue);
        super.onSaveInstanceState(outState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        // Initialize all the widgets from the layout
        View v = inflater.inflate(R.layout.recording_add_layout, container, false);
        channelName = (TextView) v.findViewById(R.id.channel);
        title = (EditText) v.findViewById(R.id.title);
        description = (EditText) v.findViewById(R.id.description);
        startExtra = (EditText) v.findViewById(R.id.start_extra);
        stopExtra = (EditText) v.findViewById(R.id.stop_extra);
        startTime = (TextView) v.findViewById(R.id.start_time);
        stopTime = (TextView) v.findViewById(R.id.stop_time);
        priority = (TextView) v.findViewById(R.id.priority);
        toolbar = (Toolbar) v.findViewById(R.id.toolbar);

        // Create the list of channels that the user can select. The very first
        // entry is a placeholder to select no record on all channels
        TVHClientApplication app = (TVHClientApplication) activity.getApplication();
        channelList = new String[app.getChannels().size()];
        for (int i = 0; i < app.getChannels().size(); i++) {
            channelList[i] = app.getChannels().get(i).name;
        }

        priorityList = activity.getResources().getStringArray(R.array.dvr_priorities);

        // If the savedInstanceState is null then the fragment was created for
        // the first time. Either get the given id to edit the recording or
        // create new one. Otherwise an orientation change has occurred and the
        // saved values must be applied to the user input elements.
        if (savedInstanceState == null) {
            long recId = 0;
            Bundle bundle = getArguments();
            if (bundle != null) {
                recId = bundle.getLong(Constants.BUNDLE_RECORDING_ID);
            }

            // Get the recording so we can show its detail
            rec = app.getRecording(recId);
            if (rec != null) {
                priorityValue = rec.priority;
                startExtraValue = rec.startExtra;
                stopExtraValue = rec.stopExtra;
                startTimeValue = rec.start.getTime();
                stopTimeValue = rec.stop.getTime();
                titleValue = rec.title;
                descriptionValue = rec.description;
                int pos = app.getChannels().indexOf(rec.channel);
                channelSelectionValue = (pos >= 0 ? pos : 0);
            }
        } else {
            // Restore the values before the orientation change
            priorityValue = savedInstanceState.getLong("priorityValue");
            startExtraValue = savedInstanceState.getLong("startExtraValue");
            stopExtraValue = savedInstanceState.getLong("stopExtraValue");
            startTimeValue = savedInstanceState.getLong("startTimeValue");
            stopTimeValue = savedInstanceState.getLong("stopTimeValue");
            titleValue = savedInstanceState.getString("titleValue");
            descriptionValue = savedInstanceState.getString("descriptionValue");
            channelSelectionValue = savedInstanceState.getInt("channelNameValue");
        }

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (channelName != null && rec != null && rec.channel != null) {
            channelName.setText(rec.channel.name);
        }
        if (title != null) {
            title.setText(titleValue);
        }
        if (description != null) {
            description.setText(descriptionValue);
        }
        if (channelName != null) {
            channelName.setText(channelList[channelSelectionValue]);
            channelName.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    new MaterialDialog.Builder(activity)
                    .title(R.string.select_channel)
                    .items(channelList)
                    .itemsCallbackSingleChoice(channelSelectionValue, new MaterialDialog.ListCallbackSingleChoice() {
                        @Override
                        public boolean onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                            channelName.setText(channelList[which]);
                            channelSelectionValue = which;
                            return true;
                        }
                    })
                    .show();
                }
            });
        }
        if (priority != null) {
            priority.setText(priorityList[(int) priorityValue]);
            priority.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View view) {
					new MaterialDialog.Builder(activity)
		            .title(R.string.select_priority)
		            .items(priorityList)
		            .itemsCallbackSingleChoice((int) priorityValue, new MaterialDialog.ListCallbackSingleChoice() {
		                @Override
		                public boolean onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
		                	priority.setText(priorityList[which]);
		                	priorityValue = which;
		                    return true;
		                }
		            })
		            .show();
				}
            });
        }
        if (startTime != null) {
            startTime.setText(getTimeStringFromValue(startTimeValue));
            // Show the time picker dialog so the user can select a new starting time
            startTime.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    new SlideDateTimePicker.Builder(activity
                            .getSupportFragmentManager())
                            .setListener(new SlideDateTimeListener() {
                                @Override
                                public void onDateTimeSet(Date date) {
                                    startTimeValue = date.getTime();
                                    startTime.setText(getTimeStringFromValue(startTimeValue));
                                }
                                @Override
                                public void onDateTimeCancel() {
                                    // NOP
                                }
                            })
                            .setInitialDate(new Date(startTimeValue))
                            .setIs24HourTime(true)
                            .build().show();
                }
            });
        }
        if (stopTime != null) {
            stopTime.setText(getTimeStringFromValue(stopTimeValue));
            // Show the time picker dialog so the user can select a new starting time
            stopTime.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    new SlideDateTimePicker.Builder(activity
                            .getSupportFragmentManager())
                            .setListener(new SlideDateTimeListener() {
                                @Override
                                public void onDateTimeSet(Date date) {
                                    stopTimeValue = date.getTime();
                                    stopTime.setText(getTimeStringFromValue(stopTimeValue));
                                }
                                @Override
                                public void onDateTimeCancel() {
                                    // NOP
                                }
                            })
                            .setInitialDate(new Date(stopTimeValue))
                            .setIs24HourTime(true)
                            .build().show();
                }
            });
        }
        if (startExtra != null) {
            startExtra.setText(String.valueOf(startExtraValue));
        }
        if (stopExtra != null) {
            stopExtra.setText(String.valueOf(stopExtraValue));
        }
        if (toolbar != null) {
            toolbar.inflateMenu(R.menu.save_cancel_menu);
            toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    return onToolbarItemSelected(item);
                }
            });
        }
        if (getDialog() != null) {
            getDialog().setTitle(R.string.edit_recording);
            getDialog().setCanceledOnTouchOutside(false);
        }
    }

	@Override
	public void onResume() {
		super.onResume();
		getDialog().setOnKeyListener(new OnKeyListener() {
			@Override
			public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
				if ((keyCode == android.view.KeyEvent.KEYCODE_BACK)) {
					getDialog().setOnKeyListener(null);
					cancel();
				}
				return true;
			}
		});
	}

    /**
     * 
     * @param item
     * @return
     */
    protected boolean onToolbarItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_save:
            save();
            return true;

        case R.id.menu_cancel:
            cancel();
            return true;
        }
        return false;
    }

    /**
     * Retrieves and checks the values from the user input elements and stores
     * them in internal variables. These are used to remember the values during
     * an orientation change or when the recording shall be saved.
     */
    private void getValues() {
        try {
            startExtraValue = Long.valueOf(startExtra.getText().toString());
        } catch (NumberFormatException ex) {
            startExtraValue = DEFAULT_START_EXTRA;
        }
        try {
            stopExtraValue = Long.valueOf(stopExtra.getText().toString());
        } catch (NumberFormatException ex) {
            stopExtraValue = DEFAULT_STOP_EXTRA;
        }
        titleValue = title.getText().toString();
        descriptionValue = description.getText().toString();
    }

    /**
     * Checks certain given values for plausibility and if everything is fine
     * creates the intent that will be passed to the service to save the newly
     * created recording.
     */
    private void save() {
        getValues();

        // Add the new or edited series recording
        Intent intent = new Intent(activity, HTSService.class);
        intent.setAction(Constants.ACTION_UPDATE_DVR_ENTRY);
        intent.putExtra("title", titleValue);
        intent.putExtra("description", descriptionValue);
        intent.putExtra("startExtra", startExtraValue);
        intent.putExtra("stopExtra", stopExtraValue);
        intent.putExtra("start", startTimeValue);
        intent.putExtra("stop", stopTimeValue);
        intent.putExtra("priority", priorityValue);

        TVHClientApplication app = (TVHClientApplication) activity.getApplication();

        // The id must be passed on to the server, not the name. So go through
        // all available channels and get the id for the selected channel name.
        for (Channel c : app.getChannels()) {
            if (c.name.equals(channelName.getText().toString())) {
                intent.putExtra("channelId", c.id);
                break;
            }
        }

        // Add the recording profile if available and enabled
        final Connection conn = DatabaseHelper.getInstance().getSelectedConnection();
        final Profile p = DatabaseHelper.getInstance().getProfile(conn.recording_profile_id);
        if (p != null 
                && p.enabled
                && app.getProtocolVersion() >= Constants.MIN_API_VERSION_PROFILES
                && app.isUnlocked()) {
            intent.putExtra("configName", p.name);
        }

        activity.startService(intent);

        if (getDialog() != null) {
            ((FragmentStatusInterface) activity).listDataInvalid(TAG);
            getDialog().dismiss();
        }
    }

    /**
     * Asks the user to confirm canceling the current activity. If no is
     * chosen the user can continue to add or edit the recording. Otherwise
     * the input will be discarded and the activity will be closed.
     */
    private void cancel() {
        // Show confirmation dialog to cancel
        new MaterialDialog.Builder(activity)
                .content(R.string.cancel_edit_recording)
                .positiveText(getString(R.string.discard))
                .negativeText(getString(R.string.cancel))
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        if (getDialog() != null) {
                            getDialog().dismiss();
                        }
                    }
                    @Override
                    public void onNegative(MaterialDialog dialog) {
                        dialog.cancel();
                    }
                }).show();
    }

    /**
     * Set the time from the long value. Prepend leading zeros to the hours or
     * minutes in case they are lower then ten.
     * 
     * @return time in hh:mm format
     */
    private String getTimeStringFromValue(long time) {
        SimpleDateFormat smf =  new SimpleDateFormat("HH:mm, dd.MM.yyyy", Locale.getDefault());
        return smf.format(new Date(time));
    }
}
