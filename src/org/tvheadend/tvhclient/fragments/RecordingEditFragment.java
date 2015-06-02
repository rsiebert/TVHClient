package org.tvheadend.tvhclient.fragments;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.tvheadend.tvhclient.Constants;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.htsp.HTSService;
import org.tvheadend.tvhclient.interfaces.FragmentStatusInterface;
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
public class RecordingEditFragment extends DialogFragment {

    private final static String TAG = RecordingEditFragment.class.getSimpleName();

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

    // Extra pre- and postrecording times in seconds
    private long startExtraValue;
    private long stopExtraValue;

    // Start and end recording times in milliseconds. Need to be converted to
    // seconds when passed to the service
    private long startTimeValue;
    private long stopTimeValue;
    private long priorityValue;
    private String titleValue;
    private String descriptionValue;

    String[] priorityList;

    private TVHClientApplication app;

    private static final int DEFAULT_START_EXTRA = 2;
    private static final int DEFAULT_STOP_EXTRA = 2;

    public static RecordingEditFragment newInstance(Bundle args) {
        RecordingEditFragment f = new RecordingEditFragment();
        f.setArguments(args);
        return f;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = (ActionBarActivity) activity;
        app = (TVHClientApplication) activity.getApplication();
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
        super.onSaveInstanceState(outState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

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
        }

        // Show only the title, stop and extra stop time when the recording is
        // already being recorded
        View v = inflater.inflate(
                (rec.isRecording() ? R.layout.recording_edit_recording_layout
                        : R.layout.recording_edit_scheduled_layout), container, false);

        // Initialize all the widgets from the layout
        title = (EditText) v.findViewById(R.id.title);
        description = (EditText) v.findViewById(R.id.description);
        startExtra = (EditText) v.findViewById(R.id.start_extra);
        stopExtra = (EditText) v.findViewById(R.id.stop_extra);
        startTime = (TextView) v.findViewById(R.id.start_time);
        stopTime = (TextView) v.findViewById(R.id.stop_time);
        priority = (TextView) v.findViewById(R.id.priority);
        toolbar = (Toolbar) v.findViewById(R.id.toolbar);

        priorityList = activity.getResources().getStringArray(R.array.dvr_priorities);
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final boolean editTitleSupported = (app.getProtocolVersion() >= Constants.MIN_API_VERSION_EDIT_RECORDING_TITLE);

        if (title != null) {
            title.setVisibility(editTitleSupported ? View.VISIBLE : View.GONE);
            title.setText(titleValue);
        }
        if (description != null) {
            description.setVisibility(editTitleSupported ? View.VISIBLE : View.GONE);
            description.setText(descriptionValue);
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
				return false;
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
            if (startExtra != null) {
                startExtraValue = Long.valueOf(startExtra.getText().toString());
            }
        } catch (NumberFormatException ex) {
            startExtraValue = DEFAULT_START_EXTRA;
        }
        try {
            if (stopExtra != null) {
                stopExtraValue = Long.valueOf(stopExtra.getText().toString());
            }
        } catch (NumberFormatException ex) {
            stopExtraValue = DEFAULT_STOP_EXTRA;
        }
        if (title != null) {
            titleValue = title.getText().toString();
        }
        if (description != null) {
            descriptionValue = description.getText().toString();
        }
    }

    /**
     * Checks certain given values for plausibility and if everything is fine
     * creates the intent that will be passed to the service to save the newly
     * created recording.
     */
    private void save() {
        getValues();

        Intent intent = new Intent(activity, HTSService.class);
        intent.setAction(Constants.ACTION_UPDATE_DVR_ENTRY);
        intent.putExtra("id", rec.id);
        intent.putExtra("title", titleValue);
        intent.putExtra("stopExtra", stopExtraValue);
        intent.putExtra("stop", stopTimeValue / 1000); // Pass on seconds not milliseconds

        // Only add the additional field when the recording is scheduled. When
        // it is already being recorded only the title, stop and stopExtra will
        // be accepted. All other entries will be ignored and not sent
        intent.putExtra("isRecording", rec.isRecording());

        intent.putExtra("title", titleValue);
        intent.putExtra("description", descriptionValue);
        intent.putExtra("startExtra", startExtraValue);
        intent.putExtra("start", startTimeValue / 1000); // Pass on seconds not milliseconds
        intent.putExtra("priority", priorityValue);

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
