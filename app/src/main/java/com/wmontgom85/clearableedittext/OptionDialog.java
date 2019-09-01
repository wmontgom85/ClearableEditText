package com.wmontgom85.clearableedittext;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.view.Window;
import android.widget.NumberPicker;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

public class OptionDialog extends Dialog {
    private TextView title, message, cancelButton, submitButton;
    private NumberPicker attributePicker;
    private OptionDialogListener mListener;

    private List<String> options;
    private String optionChosen;

    private int maxQty;
    private int qtyChosen;

    public void setListener(OptionDialogListener mListener) {
        this.mListener = mListener;
    }

    public String getOptionChosen() {
        return optionChosen;
    }

    public int getQtyChosen() {
        return qtyChosen;
    }

    public OptionDialog(@NonNull Context context) {
        super(context);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        init();
    }

    public OptionDialog(@NonNull Context context, int maxQty) {
        super(context);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.maxQty = maxQty;
        init();
    }

    public OptionDialog(@NonNull Context context, List<String> options) {
        super(context);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.options = options;
        init();
    }

    private void init() {
        setContentView(R.layout.dialog_option_selection);

        this.attributePicker = findViewById(R.id.options);
        attributePicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        attributePicker.setWrapSelectorWheel(false);

        this.title = findViewById(R.id.title);
        this.message = findViewById(R.id.message);
        this.cancelButton = findViewById(R.id.cancel_button);
        this.submitButton = findViewById(R.id.submit_button);

        cancelButton.setOnClickListener((View v) -> mListener.onSelectionDialogCancel(this));
        submitButton.setOnClickListener((View v) -> {
            if (options != null && options.size() > 0) {
                optionChosen = options.get(attributePicker.getValue());
            } else {
                qtyChosen = attributePicker.getValue();
            }

            mListener.onSelectionDialogSubmit(this);
        });

        if (options != null && options.size() > 0) {
            String[] namesArr = options.toArray(new String[options.size()]);

            attributePicker.setMinValue(0);
            attributePicker.setMaxValue(options.size() - 1);
            attributePicker.setDisplayedValues(namesArr);
            attributePicker.setValue(0);
        } else {
            attributePicker.setMinValue(0);
            attributePicker.setMaxValue(maxQty);
            attributePicker.setValue(0);
        }
    }

    public void setTitle(@Nullable CharSequence t) {
        if (t != null && t.length() > 0) {
            title.setText(t);
            title.setVisibility(View.VISIBLE);
        } else {
            title.setVisibility(View.GONE);
        }
    }

    public void setMessage(CharSequence m) {
        if (m.length() > 0) {
            message.setText(m);
            message.setVisibility(View.VISIBLE);
        } else {
            message.setVisibility(View.GONE);
        }
    }

    public void setSubmitButtonText(CharSequence t) {
        if (t != null && t.length() > 0) {
            submitButton.setText(t);
        }
    }

    public void setCancelButtonText(CharSequence t) {
        if (t != null && t.length() > 0) {
            cancelButton.setText(t);
        }
    }

    public void setSelectedOption(String oc) {
        for (int i = 0; i < options.size(); ++i) {
            if (options.get(i).equals(oc)) {
                optionChosen = oc;
                attributePicker.setValue(i);
                break;
            }
        }
    }

    public void setSelectedQty(int qty) {
        this.qtyChosen = qty;
        attributePicker.setValue(qty);
    }

    public interface OptionDialogListener {
        void onSelectionDialogSubmit(OptionDialog dialog);
        void onSelectionDialogCancel(OptionDialog dialog);
        void onSelectionDialogDismissed(OptionDialog dialog);
    }

    @Override
    public void dismiss() {
        super.dismiss();

        if (mListener != null) {
            mListener.onSelectionDialogDismissed(this);
        }
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        mListener = null;
        title = null;
        message = null;
        cancelButton = null;
        submitButton = null;
        attributePicker = null;
        options = null;
        optionChosen = null;
    }
}