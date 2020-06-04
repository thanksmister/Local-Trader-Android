/*
 * Copyright (c) 2020 ThanksMister LLC
 * www.ThanksMister.com
 * mister@thanksmister.com
 * Mozilla Public License Version 2.0
 */

package com.thanksmister.bitcoin.localtrader.utils;


import android.text.TextUtils;

import androidx.annotation.NonNull;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

public final class NumberUtils {
    private NumberUtils() {
    }

    public static double parseDouble(@NonNull String value, double defaultValue) {
        if (!TextUtils.isEmpty(value)) {
            try {
                return Double.parseDouble(value);
            } catch (Exception e) {
                return defaultValue;
            }
        } else {
            return defaultValue;
        }
    }

    public static double convertToNumberLocal(@NonNull String value, Locale locale) throws NumberFormatException, ParseException {
        try {
            NumberFormat numberFormat = NumberFormat.getNumberInstance(locale);
            return numberFormat.parse(value).doubleValue();
        } catch (Exception e) {
            throw new NumberFormatException("Unable to convert number " + value + " to double: " + e.getMessage());
        }
    }
}
