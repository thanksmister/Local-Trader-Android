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
