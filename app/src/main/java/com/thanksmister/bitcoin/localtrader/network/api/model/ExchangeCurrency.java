/*
 * Copyright (c) 2020 ThanksMister LLC
 * www.ThanksMister.com
 * mister@thanksmister.com
 * Mozilla Public License Version 2.0
 */

package com.thanksmister.bitcoin.localtrader.network.api.model;

public class ExchangeCurrency {
    private String currency;

    public ExchangeCurrency(String name) {
        this.currency = name;
    }

    public String getCurrency() {
        return currency;
    }
}
