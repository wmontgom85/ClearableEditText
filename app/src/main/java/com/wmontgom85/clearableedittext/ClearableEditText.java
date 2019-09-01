package com.wmontgom85.clearableedittext;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.jakewharton.rxbinding2.view.RxView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.disposables.CompositeDisposable;

public class ClearableEditText extends RelativeLayout implements OptionDialog.OptionDialogListener {
    private static final int DEFAULT_TYPE = 0;
    private static final int TYPE_TEXT = 0;
    private static final int TYPE_SELECT = 1;

    private static final int TYPE_SELECT_THROTTLE = 500; // How long to throttle the click when the type is TYPE_SELECT

    private Validator validator;
    private EditText textField;
    private TextView textView;
    private ImageView clearButton;
    private ImageView chevron;
    private String suppressionMessage;
    private List<String> options = new ArrayList<>();
    private OptionSelectedListener mListener;

    private boolean suppressDialog = false;
    private boolean canFormat = true;
    private boolean isValid = true;
    private boolean isErred = false;
    private int type = DEFAULT_TYPE;

    private CompositeDisposable mDisposables = new CompositeDisposable(); // Composite Disposable to hold all the subscriptions that could be used

    public boolean canFormat() {
        return canFormat;
    }

    public void setValidator(Validator validator) {
        this.validator = validator;
    }

    public void setCanFormat(boolean canFormat) {
        this.canFormat = canFormat;
    }

    public void setListener(OptionSelectedListener mListener) {
        this.mListener = mListener;
    }

    public void setSuppressDialog(boolean suppressDialog) {
        this.suppressDialog = suppressDialog;
    }

    public void setSuppressionMessage(String suppressionMessage) {
        this.suppressionMessage = suppressionMessage;
    }

    public boolean isErred() {
        return isErred;
    }

    public void setErred(boolean erred) {
        isErred = erred;
    }

    public ClearableEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, 1, attrs);
    }

    public ClearableEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, defStyleAttr, attrs);
    }

    public ClearableEditText(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, defStyleAttr, attrs);
    }

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }

    @SuppressLint("ClickableViewAccessibility")
    private void init(Context context, int defStyle, AttributeSet attrs) {
        inflate(context, R.layout.custom_clearable_edit_text, this);

        // add some dummy options
        options.add("Option 1");
        options.add("Option 2");
        options.add("Option 3");

        this.textField = findViewById(R.id.my_edit_text);
        this.textView = findViewById(R.id.my_text_view);
        this.clearButton = findViewById(R.id.clear_button);
        this.chevron = findViewById(R.id.chevron);

        TypedArray attributesArray = context.obtainStyledAttributes(attrs, R.styleable.ClearableEditText, defStyle, 0);

        try {
            type = attributesArray.getInt(R.styleable.ClearableEditText_cet_type, type);

            if (type == TYPE_SELECT) {
                textView.setOnTouchListener((View v, MotionEvent event) -> {
                    resetState();
                    return false;
                });
            } else {
                textField.setOnTouchListener((View v, MotionEvent event) -> {
                    resetState();
                    return false;
                });
            }

            float textSize = attributesArray.getDimension(R.styleable.ClearableEditText_android_textSize, -1);
            if (textSize > -1) {
                textField.setTextSize(textSize);
                textView.setTextSize(textSize);
            }

            int textColor = attributesArray.getColor(R.styleable.ClearableEditText_android_textColor, -1);
            if (textColor > -1) {
                textField.setTextColor(textColor);
                textView.setTextColor(textColor);
            }

            int hintColor = attributesArray.getColor(R.styleable.ClearableEditText_android_textColorHint, -1);
            if (hintColor > -1) {
                textField.setHintTextColor(hintColor);
                textView.setHintTextColor(hintColor);
            }

            int editTextBG = attributesArray.getColor(R.styleable.ClearableEditText_cet_edittext_bg, getResources().getColor(R.color.white));
            textField.setBackgroundColor(editTextBG);
            textView.setBackgroundColor(editTextBG);

            int inputType = attributesArray.getInt(R.styleable.ClearableEditText_android_inputType, -1);
            if (inputType > -1) {
                textField.setInputType(inputType);
                textView.setInputType(inputType);
            }

            String digits = attributesArray.getString(R.styleable.ClearableEditText_android_digits);
            if (digits != null && digits.length() > 0) {
                textField.setKeyListener(DigitsKeyListener.getInstance(digits));
            }

            String hint = attributesArray.getString(R.styleable.ClearableEditText_android_hint);
            if (hint != null && hint.length() > 0) {
                textField.setHint(hint);
                textView.setHint(hint);
            }

            int maxLines = attributesArray.getInt(R.styleable.ClearableEditText_android_maxLines, 1);
            textField.setMaxLines(maxLines);

            int imeOptions = attributesArray.getInt(R.styleable.ClearableEditText_android_imeOptions, EditorInfo.IME_ACTION_DONE);
            textField.setImeOptions(imeOptions);

            suppressDialog = attributesArray.getBoolean(R.styleable.ClearableEditText_cet_suppress_dialog, false);

            if (type == TYPE_SELECT) { // If the View is for selecting something
                textField.setVisibility(View.GONE); // Remove the EditText
                clearButton.setVisibility(View.GONE); // Remove the clear button
                chevron.setVisibility(View.VISIBLE); // Show the chevron
                textView.setVisibility(View.VISIBLE); // Show the TextView

                String oTitle = attributesArray.getString(R.styleable.ClearableEditText_cet_title); // Set the title of the dialog from the attributes array
                if (oTitle == null || oTitle.length() < 1) { // If no title was specified or it was empty
                    oTitle = textField.getHint().toString(); // Set te title to be whatever the hint is
                }

                final String _title = oTitle; // Create a final variable so it can be used inside inner classes

                mDisposables.add(RxView.clicks(this) // Begin creating the disposable that will handle the view being clicked and add it to the CompositeDisposable
                    .throttleFirst(TYPE_SELECT_THROTTLE, TimeUnit.MILLISECONDS) // Prevent the view from being clicked again until some time has passed
                    .subscribe(o -> { // Create the subscription
                        if (!suppressDialog && options != null) { // If nothing is suppressing the dialog and there are options being provided
                            String currentVal = textField.getText().toString(); // Get the current value of the text field

                            OptionDialog od = new OptionDialog(getContext(), options); // Create the OptionDialog that will be shown
                            od.setTitle(_title); // Set the title of the dialog
                            od.setListener(this); // Set the callback
                            if (currentVal.length() > 0) { // If the current value wasn't empty
                                od.setSelectedOption(currentVal); // Set the initially selected option
                            }

                            od.show(); // Show the dialog
                        } else if (suppressionMessage != null && suppressionMessage.length() > 0) { // If the dialog was suppressed and there is a message
                            AlertDialog.Builder error = new AlertDialog.Builder(getContext()); // Create the AlertDialog Builder
                            error.setMessage(suppressionMessage); // Set the message of the dialog
                            error.setCancelable(true); // Dialog can be cancelled
                            error.setPositiveButton("Ok", (DialogInterface dialog, int id) -> dialog.cancel()); // Cancel the dialog when the button is clicked since it's just to notify

                            AlertDialog reportError = error.create(); // Create the actual dialog

                            reportError.show(); // Show the dialog
                        }
                    }));

                // Set to not be clickable since the entire view should be clickable and we want clicks to simply pass through these components
                textView.setClickable(false);
                chevron.setClickable(false);

            } else {
                textField.setVisibility(View.VISIBLE);
                clearButton.setVisibility(View.INVISIBLE);
                textView.setVisibility(View.GONE);
                chevron.setVisibility(View.GONE);
                clearButton.setOnClickListener((View v) -> textField.setText(""));

                textField.setOnFocusChangeListener((View view, boolean hasFocus) -> {
                    if (hasFocus) {
                        clearButton.setVisibility(View.VISIBLE);
                    } else {
                        clearButton.setVisibility(View.INVISIBLE);
                    }
                });
            }
        } catch (Throwable tx) {
        } finally {
            attributesArray.recycle();
        }
    }

    private void resetState() {
        if (!isValid) {
            isValid = true;

            textField.setTextColor(getResources().getColor(R.color.dark_grey));
            textView.setTextColor(getResources().getColor(R.color.dark_grey));
            textField.setHintTextColor(getResources().getColor(R.color.grey));
            textView.setHintTextColor(getResources().getColor(R.color.grey));
        }
    }

    public EditText getTextField() {
        return textField;
    }

    public Editable getText() {
        return textField.getText();
    }

    public void setText(String text) {
        updateValue(text);
    }

    public void setSelection(int sel) {
        textField.setSelection(sel);
    }

    public void updateValue(String val) {
        textField.setText(val);
        textView.setText(val);
    }

    public String getValue() {
        String v = (type == TYPE_TEXT ? textField.getText().toString() : textView.getText().toString());

        if (v != null) {
            v = v.trim();
        } else {
            v = "";
        }

        return v;
    }

    public boolean validate() {
        if (validator != null) {
            isValid = validator.isValid(getValue());

            if (!isValid) {
                textField.setTextColor(getResources().getColor(R.color.red));
                textView.setTextColor(getResources().getColor(R.color.red));
                textField.setHintTextColor(getResources().getColor(R.color.red));
                textView.setHintTextColor(getResources().getColor(R.color.red));
            }
        }

        return isValid;
    }

    @Override
    public void onSelectionDialogSubmit(OptionDialog dialog) {
        updateValue(dialog.getOptionChosen());
        if (mListener != null) mListener.onSelectionDialogSubmit(dialog, this);
        dialog.dismiss();
    }

    @Override
    public void onSelectionDialogCancel(OptionDialog dialog) {
        dialog.dismiss();
    }

    @Override
    public void onSelectionDialogDismissed(OptionDialog dialog) {

    }

    public void addTextChangedListener(TextWatcher t) {
        textField.addTextChangedListener(t);
    }


    public interface OptionSelectedListener {
        void onSelectionDialogSubmit(OptionDialog dialog, ClearableEditText cet);
    }

    public void clearFocus() {
        textField.clearFocus();
        clearButton.setVisibility(View.INVISIBLE);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        return new SavedState(superState, getValue());
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        SavedState savedState = (SavedState) state;
        super.onRestoreInstanceState(savedState.getSuperState());

        textField.setText(savedState.data);
        textView.setText(savedState.data);
    }

    @Override
    protected void dispatchSaveInstanceState(SparseArray<Parcelable> container) {
        super.dispatchFreezeSelfOnly(container);
    }

    @Override
    protected void dispatchRestoreInstanceState(SparseArray<Parcelable> container) {
        super.dispatchThawSelfOnly(container);
    }

    private static class SavedState extends BaseSavedState {
        public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };

        private String data;

        public SavedState(Parcel source) {
            super(source);
            data = source.readString();
        }

        public SavedState(Parcelable superState, String data) {
            super(superState);
            this.data = data;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            dest.writeString(data);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        validator = null;
        textField = null;
        textView = null;
        clearButton = null;
        chevron = null;
        suppressionMessage = null;
        options = null;
        mListener = null;

        // Dispose of the subscriptions when no longer needed
        if (mDisposables != null && !mDisposables.isDisposed()) {
            mDisposables.dispose();
        }
    }
}
