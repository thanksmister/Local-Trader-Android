package com.thanksmister.bitcoin.localtrader.ui.request;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;

import com.thanksmister.bitcoin.localtrader.data.api.model.Wallet;

/**
 * Author: Michael Ritchie
 * Date: 12/30/14
 * Copyright 2013, ThanksMister LLC
 */
public interface RequestView
{
    void promptForPin(String bitcoinAddress, String bitcoinAmount);

    void showGeneratedQrCodeActivity(String bitcoinAddress, String bitcoinAmount);

    Context getContext();

    Fragment getFragmentContext();

    public void showError(String message);

    public void showProgress();

    public void hideProgress();

    void setBitcoinAddress(String btcAddress);

    void setAmount(String btcAmount);

    void resetWallet();

    void setWallet(Wallet wallet);
}