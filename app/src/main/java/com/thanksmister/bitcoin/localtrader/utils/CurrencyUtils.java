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
