package com.thanksmister.bitcoin.localtrader.ui.edit;

import android.location.Address;

import com.thanksmister.bitcoin.localtrader.data.api.model.Advertisement;
import com.thanksmister.bitcoin.localtrader.data.api.model.Method;
import com.thanksmister.bitcoin.localtrader.data.api.model.TradeType;

/**
 * Author: Michael Ritchie
 * Date: 12/30/14
 * Copyright 2013, ThanksMister LLC
 */
public interface EditPresenter
{
    void onDestroy();

    void onResume(boolean create);

    void cancelChanges(Boolean create);

    void stopLocationCheck();

    void startLocationCheck();

    void updateAdvertisement(Advertisement advertisement, Boolean create);

    void doAddressLookup(String s);

    void setAdvertisementType(TradeType tradeType);

    void validateChanges();
}
