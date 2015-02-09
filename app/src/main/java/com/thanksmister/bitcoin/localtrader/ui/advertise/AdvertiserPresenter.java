package com.thanksmister.bitcoin.localtrader.ui.advertise;

import android.location.Address;

import com.thanksmister.bitcoin.localtrader.data.api.model.Advertisement;
import com.thanksmister.bitcoin.localtrader.data.api.model.TradeType;

/**
 * Author: Michael Ritchie
 * Date: 12/30/14
 * Copyright 2013, ThanksMister LLC
 */
public interface AdvertiserPresenter
{
    public void onDestroy();
    public void onResume();

    void getAdvertisement(String adId);

    void showTradeRequest();

    void showPublicAdvertisement();

    void showProfile();

    void showAdvertisementOnMap();
}
