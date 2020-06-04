/*
 * Copyright (c) 2020 ThanksMister LLC
 * www.ThanksMister.com
 * mister@thanksmister.com
 * Mozilla Public License Version 2.0
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
