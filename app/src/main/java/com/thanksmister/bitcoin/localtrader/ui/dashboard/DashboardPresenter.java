package com.thanksmister.bitcoin.localtrader.ui.dashboard;

import com.thanksmister.bitcoin.localtrader.data.api.model.Advertisement;
import com.thanksmister.bitcoin.localtrader.data.api.model.Contact;
import com.thanksmister.bitcoin.localtrader.data.api.model.Method;

import java.util.List;

/**
 * Author: Michael Ritchie
 * Date: 12/30/14
 * Copyright 2013, ThanksMister LLC
 */
public interface DashboardPresenter
{
    public void onResume();
    
    public void onDestroy();

    public void scanQrCode();

    public void showSendScreen();

    void showContact(Contact contact);

    void showAdvertisement(Advertisement advertisement);

    void createAdvertisementScreen();

    void logOut();

    void endRefresh();

    void refreshError(String message);

    void showTradesScreen();

    void showSearchScreen();
}
