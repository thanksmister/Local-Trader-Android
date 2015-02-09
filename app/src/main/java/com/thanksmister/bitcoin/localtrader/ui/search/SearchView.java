package com.thanksmister.bitcoin.localtrader.ui.search;

import android.content.Context;
import android.location.Address;

import com.thanksmister.bitcoin.localtrader.data.api.model.Method;
import com.thanksmister.bitcoin.localtrader.ui.PredictAdapter;

import java.util.List;

/**
 * Author: Michael Ritchie
 * Date: 12/30/14
 * Copyright 2013, ThanksMister LLC
 */
public interface SearchView
{
    Context getContext();

    public void showError(String message);
    
    public void showProgress();

    public void hideProgress();

    PredictAdapter getEditLocationAdapter();

    void setMethods(List<Method> results);

    void setAddress(Address address);
}