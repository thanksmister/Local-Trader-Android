/*
 * Copyright (c) 2019 ThanksMister LLC
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

import android.support.annotation.NonNull;
import android.text.TextUtils;

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
