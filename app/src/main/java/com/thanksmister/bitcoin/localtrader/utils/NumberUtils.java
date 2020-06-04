/*
 * Copyright (c) 2020 ThanksMister LLC
 *  http://www.thanksmister.com
 *
 *  Mozilla Public License 2.0
 *
 *  Permissions of this weak copyleft license are conditioned on making
 *  available source code of licensed files and modifications of those files
 *  under the same license (or in certain cases, one of the GNU licenses).
 *  Copyright and license notices must be preserved. Contributors provide
 *  an express grant of patent rights. However, a larger work using the
 *  licensed work may be distributed under different terms and without source
 *  code for files added in the larger work.
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
