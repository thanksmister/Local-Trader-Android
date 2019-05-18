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

import com.thanksmister.bitcoin.localtrader.network.api.model.Currency;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CurrencyUtils {

    public CurrencyUtils() {
    }

    public static List<Currency> sortCurrencies(List<Currency> currencies) {
        Collections.sort(currencies, new CurrencyComparator());
        return currencies;
    }

    private static class CurrencyComparator implements Comparator<Currency> {
        @Override
        public int compare(Currency o1, Currency o2) {
            return o1.getCode().compareTo(o2.getCode());
        }
    }
}
