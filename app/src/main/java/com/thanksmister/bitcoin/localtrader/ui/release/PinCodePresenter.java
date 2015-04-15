package com.thanksmister.bitcoin.localtrader.ui.release;

/**
 * Author: Michael Ritchie
 * Date: 12/30/14
 * Copyright 2013, ThanksMister LLC
 */
public interface PinCodePresenter
{
    public void onDestroy();
    public void onResume();
    void validatePinCode(String pinCode, String address, String amount);
}
