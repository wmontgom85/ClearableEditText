package com.wmontgom85.clearableedittext;

import java.util.ArrayList;

public class Validator {
    public static final int VALIDATE_EMPTY = 1;
    public static final int VALIDATE_EMAIL = 2;
    public static final int VALIDATE_PHONE = 3;
    public static final int VALIDATE_CUSTOM = 4;

    private String customRegex;

    private ArrayList<Integer> validations = new ArrayList<>();

    public Validator(String regex, Integer... options) {
        for (int i : options) {
            validations.add(i);
        }
        this.customRegex = regex;
    }

    public Validator(Integer... options) {
        for (int i : options) {
            validations.add(i);
        }
    }

    public boolean isValid(String val) {
        boolean isValid = true;

        for (Integer i: validations) {
            switch (i) {
                case VALIDATE_EMPTY:
                    if (val == null || val.length() < 1) {
                        isValid = false;
                    }
                    break;
                case VALIDATE_EMAIL:
                    if (val != null && val.length() > 0) {
                        isValid = android.util.Patterns.EMAIL_ADDRESS.matcher(val).matches();
                    }
                    break;
                case VALIDATE_PHONE:
                    break;
                case VALIDATE_CUSTOM:
                    if (customRegex != null && customRegex.length() > 0) {
                        isValid = val.matches(customRegex);
                    }
                    break;
            }

            if (!isValid) break;
        }

        return isValid;
    }
}

