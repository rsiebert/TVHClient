/*
 * Copyright 2012 Jay Weisskopf
 *
 * Licensed under the MIT License (see LICENSE.txt)
 */

package org.tvheadend.tvhclient;

import org.tvheadend.tvhclient.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

/**
 * @author Robert Siebert
 * @author Jay Weisskopf
 */
public class SliderPreference extends DialogPreference {

    private final static String TAG = SliderPreference.class.getSimpleName();

    protected int mValue;
    protected int mSeekBarValue;
    protected int mSeekBarMinValue;
    protected int mSeekBarMaxValue;
    protected int mSeekBarResolution;

    /**
     * Constructor that initializes the slider preference
     * 
     * @param context
     * @param attrs
     */
    public SliderPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setup(context, attrs);
    }

    /**
     * Constructor that initializes the slider preference
     *  
     * @param context
     * @param attrs
     * @param defStyle
     */
    public SliderPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setup(context, attrs);
    }

    /**
     * Initializes the slider preference by reading the values defined in the XML resource file.
     * 
     * @param context
     * @param attrs
     */
    private void setup(Context context, AttributeSet attrs) {
        setDialogLayoutResource(R.layout.slider_preference_dialog);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SliderPreference);
        try {
            setSummary(getContext().getResources().getString(R.string.pref_genre_colors_visibility_value, mValue));
            mSeekBarMinValue = a.getInteger(R.styleable.SliderPreference_minimumValue, 0);
            mSeekBarMaxValue = a.getInteger(R.styleable.SliderPreference_maximumValue, 100);
            mSeekBarResolution = mSeekBarMaxValue - mSeekBarMinValue;
        }
        catch (Exception e) {
            Log.d(TAG, e.toString());
        }
        a.recycle();
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInt(index, 0);
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        setValue(restoreValue ? getPersistedInt(mValue) : (Integer) defaultValue);
    }

    @Override
    public CharSequence getSummary() {
        return getContext().getResources().getString(R.string.pref_genre_colors_visibility_value, mValue);
    }

    /**
     * Assures that the given value is within the defined minimum and maximum
     * range. The value is then stored in the persistent storage.
     * 
     * @param value
     */
    public void setValue(int value) {
        value = Math.max(mSeekBarMinValue, Math.min(value, mSeekBarMaxValue));
        if (shouldPersist()) {
            persistInt(value);
        }
        if (value != mValue) {
            mValue = value;
            notifyChanged();
        }
    }

    @Override
    protected View onCreateDialogView() {
        mSeekBarValue = mValue;
        View view = super.onCreateDialogView();

        // Show the value of the slider as a text
        final TextView seekbarValue = (TextView) view.findViewById(R.id.slider_preference_value);
        seekbarValue.setText(String.valueOf(mSeekBarValue));

        // Set the actual value of the slider and create the listener that
        // updates the text and saves the value when the slider is moved.
        final SeekBar seekbar = (SeekBar) view.findViewById(R.id.slider_preference_seekbar);
        seekbar.setMax(mSeekBarResolution);
        seekbar.setProgress(mSeekBarValue - mSeekBarMinValue);
        seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    SliderPreference.this.mSeekBarValue = progress;
                    seekbarValue.setText(String.valueOf(progress + mSeekBarMinValue));
                }
            }
        });
        return view;
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        final int newValue = mSeekBarValue + mSeekBarMinValue;
        if (positiveResult && callChangeListener(newValue)) {
            setValue(newValue);
        }
        super.onDialogClosed(positiveResult);
    }
}
