package com.thanksmister.bitcoin.localtrader.ui.wallet;

import android.content.Context;

import com.thanksmister.bitcoin.localtrader.data.api.model.Advertisement;
import com.thanksmister.bitcoin.localtrader.data.api.model.Dashboard;
import com.thanksmister.bitcoin.localtrader.data.api.model.Method;
import com.thanksmister.bitcoin.localtrader.data.api.model.Wallet;

import java.util.ArrayList;
import java.util.List;

/**
 * Author: Michael Ritchie
 * Date: 12/30/14
 * Copyright 2013, ThanksMister LLC
 */
public interface WalletView
{
    Context getContext();

    public void showProgress();

    public void hideProgress();

    public void setWallet(Wallet wallet);
}