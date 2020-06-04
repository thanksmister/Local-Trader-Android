/*
 * Copyright (c) 2020 ThanksMister LLC
 * www.ThanksMister.com
 * mister@thanksmister.com
 * Mozilla Public License Version 2.0
 */

package com.thanksmister.bitcoin.localtrader.utils;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

public final class Doubles {
    private Doubles() {
        // No instances.
    }

    public static double convertToDouble(String value) {
        try {
            return convertToNumber(value);
        } catch (Exception e) {
            return 0;
        }
    }

    public static double convertToNumber(String value) {
        try {
            Locale locUS = new Locale("en_US");
            NumberFormat numberFormat = NumberFormat.getInstance(locUS);
            return numberFormat.parse(value).doubleValue();
        } catch (Exception e) {
            return 0.00;
        }
    }

    public static double convertToNumberLocal(String value, String local) throws NumberFormatException, ParseException {
        try {
            Locale loc = new Locale(local);
            NumberFormat numberFormat = NumberFormat.getNumberInstance(loc);
            return numberFormat.parse(value).doubleValue();
        } catch (Exception e) {
            return 0;
        }
    }
}
