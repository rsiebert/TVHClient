package org.tvheadend.tvhclient;

import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.util.AttributeSet;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

public class MultiSpinner extends Spinner implements OnMultiChoiceClickListener, OnCancelListener {

    @SuppressWarnings("unused")
    private final static String TAG = MultiSpinner.class.getSimpleName();

    private Context context;
    private List<String> items;
    private List<String> textItems;
    private boolean[] selected;
    private int maxSelectionCount;
    private MultiSpinnerListener listener;

    public MultiSpinner(Context context) {
        super(context);
    }

    public MultiSpinner(Context context, AttributeSet attrSet) {
        super(context, attrSet);
        this.context = context;
    }

    public MultiSpinner(Context context, AttributeSet attrSet, int arg2) {
        super(context, attrSet, arg2);
        this.context = context;
    }

    @Override
    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
        if (isChecked) {
            selected[which] = true;
        } else {
            selected[which] = false;
        }
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getContext(), 
                android.R.layout.simple_spinner_item, new String[] { getSpinnerText() });
        setAdapter(adapter);
        listener.onItemsSelected(selected);
    }

    private String getSpinnerText() {
        int selectionCount = 0;
        String spinnerText = "";
        for (int i = 0; i < maxSelectionCount; i++) {
            if (selected[i] == true) {
                spinnerText += textItems.get(i) + ",";
                selectionCount++;
            }
        }
        // Remove the last comma or set the default value
        final int idx = spinnerText.lastIndexOf(',');
        if (idx > 0) {
            spinnerText = spinnerText.substring(0, idx);
        }
        // Use strings for all or no selected days
        if (selectionCount == maxSelectionCount) {
            spinnerText = context.getString(R.string.all_days);
        } else if (selectionCount == 0) {
            spinnerText = context.getString(R.string.no_days);
        }
        return spinnerText;
    }

    public int getSpinnerValue() {
        int value = 0;
        for (int i = 0; i < maxSelectionCount; i++) {
            if (selected[i] == true) {
                value += (1 << i);
            }
        }
        return value;
    }
    
    @Override
    public boolean performClick() {
        super.performClick();

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setMultiChoiceItems(items.toArray(new CharSequence[items.size()]), selected, this);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.setOnCancelListener(this);
        builder.show();
        return true;
    }

    public void setItems(List<String> items, List<String> textItems, long selectionValue, MultiSpinnerListener listener) {
        this.items = items;
        this.textItems = textItems;
        this.listener = listener;
        this.maxSelectionCount = textItems.size();

        selected = new boolean[maxSelectionCount];
        for (int i = 0; i < maxSelectionCount; ++i) {
            selected[i] = ((selectionValue & 1) == 1);
            selectionValue = (selectionValue >> 1);
        }

        // all text on the spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getContext(), 
                android.R.layout.simple_spinner_item, new String[] { getSpinnerText() });
        setAdapter(adapter);
    }

    public interface MultiSpinnerListener {
        public void onItemsSelected(boolean[] selected);
    }
}