package com.thanksmister.bitcoin.localtrader.ui.searchresults;

import android.location.Address;

import com.thanksmister.bitcoin.localtrader.data.api.model.Advertisement;
import com.thanksmister.bitcoin.localtrader.data.api.model.Message;
import com.thanksmister.bitcoin.localtrader.data.api.model.Method;
import com.thanksmister.bitcoin.localtrader.data.api.model.TradeType;

/**
 * Author: Michael Ritchie
 * Date: 12/30/14
 * Copyright 2013, ThanksMister LLC
 */
public interface SearchResultsPresenter
{
    public void onDestroy();
    public void onResume();
    void getAdvertisements(TradeType tradeType, Address address, String paymentMethod);
    void showAdvertiser(Advertisement advertisement);
}
