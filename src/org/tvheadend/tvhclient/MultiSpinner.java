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

    private List<String> items;
    private List<String> textItems;
    private boolean[] selected;
    private MultiSpinnerListener listener;

    public MultiSpinner(Context context) {
        super(context);
    }

    public MultiSpinner(Context arg0, AttributeSet arg1) {
        super(arg0, arg1);
    }

    public MultiSpinner(Context arg0, AttributeSet arg1, int arg2) {
        super(arg0, arg1, arg2);
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
        StringBuffer spinnerBuffer = new StringBuffer();
        for (int i = 0; i < textItems.size(); i++) {
            if (selected[i] == true) {
                spinnerBuffer.append(textItems.get(i));
                spinnerBuffer.append(", ");
            }
        }

        String spinnerText = spinnerBuffer.toString();
        if (spinnerText.length() > 2) {
            spinnerText = spinnerText.substring(0, spinnerText.length() - 2);
        }
        return spinnerText;
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

    public void setItems(List<String> items, List<String> textItems, boolean[] checked, MultiSpinnerListener listener) {
        this.items = items;
        this.textItems = textItems;
        this.listener = listener;

        // all selected by default
        selected = checked;

        // all text on the spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getContext(), 
                android.R.layout.simple_spinner_item, new String[] { getSpinnerText() });
        setAdapter(adapter);
    }

    public interface MultiSpinnerListener {
        public void onItemsSelected(boolean[] selected);
    }
}