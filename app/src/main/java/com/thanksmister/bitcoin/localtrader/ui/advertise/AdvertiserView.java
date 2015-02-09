package com.thanksmister.bitcoin.localtrader.ui.advertise;

import android.content.Context;
import android.support.v7.widget.Toolbar;

import com.thanksmister.bitcoin.localtrader.data.api.model.Advertisement;
import com.thanksmister.bitcoin.localtrader.data.api.model.Method;
import com.thanksmister.bitcoin.localtrader.data.api.model.TradeType;

import java.util.List;

/**
 * Author: Michael Ritchie
 * Date: 12/30/14
 * Copyright 2013, ThanksMister LLC
 */
public interface AdvertiserView
{
    void setToolBarMenu(Toolbar toolbar);

    Context getContext();

    public void showError(String message);
    
    public void showProgress();

    public void hideProgress();

    void setAdvertisement(Advertisement advertisement, Method method);

    void setHeader(TradeType tradeType);
}