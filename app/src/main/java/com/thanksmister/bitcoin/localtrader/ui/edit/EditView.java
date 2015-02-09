package com.thanksmister.bitcoin.localtrader.ui.edit;

import android.content.Context;
import android.location.Address;
import android.support.v7.widget.Toolbar;

import com.thanksmister.bitcoin.localtrader.data.api.model.Advertisement;
import com.thanksmister.bitcoin.localtrader.data.api.model.Currency;
import com.thanksmister.bitcoin.localtrader.data.api.model.Method;
import com.thanksmister.bitcoin.localtrader.data.api.model.TradeType;
import com.thanksmister.bitcoin.localtrader.ui.PredictAdapter;

import java.util.List;

/**
 * Author: Michael Ritchie
 * Date: 12/30/14
 * Copyright 2013, ThanksMister LLC
 */
public interface EditView
{
    void setToolBarMenu(Toolbar toolbar);

    Context getContext();

    void setTradeType(TradeType trade_type);

    void setMethods(List<Method> methods);

    void validateChangesAndSend(Advertisement advertisement);

    void setAddress(Address address);

    PredictAdapter getEditLocationAdapter();

    void setCurrencies(List<Currency> exchanges, Advertisement advertisement);

    void setAdvertisement(Advertisement advertisement);
}