package com.thanksmister.bitcoin.localtrader.ui.dashboard;

import android.content.Context;

import com.thanksmister.bitcoin.localtrader.data.api.model.Advertisement;
import com.thanksmister.bitcoin.localtrader.data.api.model.Dashboard;
import com.thanksmister.bitcoin.localtrader.data.api.model.Method;
import com.thanksmister.bitcoin.localtrader.data.api.model.Wallet;

import java.util.List;

/**
 * Author: Michael Ritchie
 * Date: 12/30/14
 * Copyright 2013, ThanksMister LLC
 */
public interface DashboardView
{
    Context getContext();

    boolean hasDataSet();

    void onRefreshStop();

    void onRetry(String message);

    public void onError(String message);
    
    public void showProgress();

    public void hideProgress();

    void setDashboard(Dashboard dashboard, List<Method> methods);
}