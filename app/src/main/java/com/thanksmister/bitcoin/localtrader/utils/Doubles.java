/*
 * Copyright (c) 2018 ThanksMister LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.thanksmister.bitcoin.localtrader.utils;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

public final class Doubles {
    private Doubles() {
        // No instances.
    }

    public static double valueOrDefault(String value, double defaultValue) {
        try {
            if (Strings.isBlank(value))
                return defaultValue;

            return convertToNumber(value);

        } catch (Exception e) {
            return defaultValue;
        }
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
