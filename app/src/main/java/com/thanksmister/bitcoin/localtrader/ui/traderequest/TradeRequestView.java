package com.thanksmister.bitcoin.localtrader.ui.traderequest;

import android.content.Context;
import android.support.v7.widget.Toolbar;

import com.thanksmister.bitcoin.localtrader.data.api.model.Wallet;

/**
 * Author: Michael Ritchie
 * Date: 12/30/14
 * Copyright 2013, ThanksMister LLC
 */
public interface TradeRequestView
{
    void setToolBarMenu(Toolbar toolbar);

    Context getContext();

    public void showError(String message);

    public void showProgress();

    public void hideProgress();
}