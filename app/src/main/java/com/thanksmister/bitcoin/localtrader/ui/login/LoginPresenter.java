package com.thanksmister.bitcoin.localtrader.ui.login;

/**
 * Author: Michael Ritchie
 * Date: 12/30/14
 * Copyright 2013, ThanksMister LLC
 */
public interface LoginPresenter
{
    public void onResume();

    public void onDestroy();

    public void setAuthorizationCode(String s);
}
