package com.thanksmister.bitcoin.localtrader.ui.promo;

/**
 * Author: Michael Ritchie
 * Date: 12/30/14
 * Copyright 2013, ThanksMister LLC
 */
public interface PromoPresenter
{
    void onDestroy();

    void onResume();

    void showLoginView();

    void showRegistration();
}
