package com.thanksmister.bitcoin.localtrader.ui.login;

import android.content.Context;

/**
 * Author: Michael Ritchie
 * Date: 12/30/14
 * Copyright 2013, ThanksMister LLC
 */
public interface LoginView
{
    public void showProgress();

    public void hideProgress();
    
    public void showError(String message);
    
    public void showMain();

    Context getContext();
}