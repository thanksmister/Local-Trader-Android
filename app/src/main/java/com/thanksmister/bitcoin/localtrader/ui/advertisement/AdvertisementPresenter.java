package com.thanksmister.bitcoin.localtrader.ui.advertisement;

import android.content.Context;

import com.thanksmister.bitcoin.localtrader.data.api.model.Advertisement;

/**
 * Author: Michael Ritchie
 * Date: 12/30/14
 * Copyright 2013, ThanksMister LLC
 */
public interface AdvertisementPresenter
{
    public void onDestroy();
    public void onResume();
    void getAdvertisement(String adId);
    void editAdvertisement();
    void updateAdvertisementVisibility();
    void deleteAdvertisement();
    void shareAdvertisement();
    void viewOnlineAdvertisement();
    void showAdvertisementOnMap();
}
