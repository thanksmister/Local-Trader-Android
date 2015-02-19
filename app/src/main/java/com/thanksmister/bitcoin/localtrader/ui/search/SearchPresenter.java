package com.thanksmister.bitcoin.localtrader.ui.search;

import android.location.Address;

import com.thanksmister.bitcoin.localtrader.data.api.model.Method;
import com.thanksmister.bitcoin.localtrader.data.api.model.TradeType;

/**
 * Author: Michael Ritchie
 * Date: 12/30/14
 * Copyright 2013, ThanksMister LLC
 */
public interface SearchPresenter
{
    void resume();

    void pause();

    void showSearchResultsScreen();

    void startLocationCheck();

    void stopLocationCheck();

    void setPaymentMethod(Method method);


    void setTradeType(TradeType tradeType);

    void setAddress(Address address1);

    void doAddressLookup(String s);
}
