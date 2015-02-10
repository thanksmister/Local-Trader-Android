package com.thanksmister.bitcoin.localtrader.ui.request;

/**
 * Author: Michael Ritchie
 * Date: 12/30/14
 * Copyright 2013, ThanksMister LLC
 */
public interface RequestPresenter
{
    public void onDestroy();
    public void onResume();

    void setAmountFromClipboard();

    void scanQrCode();

    void getWalletBalance();

    void setAddressFromClipboard();

    void pinCodeEvent(String pinCode, String address, String amount);
}
