package com.thanksmister.bitcoin.localtrader.ui.searchresults;

import android.content.Context;
import android.support.v7.widget.Toolbar;

import com.thanksmister.bitcoin.localtrader.data.api.model.Advertisement;
import com.thanksmister.bitcoin.localtrader.data.api.model.Contact;
import com.thanksmister.bitcoin.localtrader.data.api.model.Method;
import com.thanksmister.bitcoin.localtrader.data.api.model.TradeType;

import java.util.List;

/**
 * Author: Michael Ritchie
 * Date: 12/30/14
 * Copyright 2013, ThanksMister LLC
 */
public interface SearchResultsView
{
    Context getContext();

    public void onError(String message);
    
    public void showProgress();

    public void hideProgress();

    void setData(List<Advertisement> advertisements, List<Method> methods, TradeType tradeType, String address);
}