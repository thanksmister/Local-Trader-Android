package com.thanksmister.bitcoin.localtrader.ui.about;

import android.content.Context;
import android.location.LocationManager;

import com.thanksmister.bitcoin.localtrader.data.DataModel;
import com.thanksmister.bitcoin.localtrader.data.services.DataService;
import com.thanksmister.bitcoin.localtrader.data.services.GeoLocationService;

import rx.Subscription;

/**
 * Author: Michael Ritchie
 * Date: 12/31/14
 * Copyright 2014, ThanksMister LLC
 */
public class AboutPresenterImpl implements AboutPresenter
{
    private AboutView view;

    public AboutPresenterImpl(AboutView view) 
    {
        this.view = view;

    }

    @Override
    public void onDestroy()
    {
    }

    @Override
    public void onResume()
    {
    }
    
    private AboutView getView()      
    {
        return view;
    }

    private Context getContext()
    {
        return getView().getContext();
    }
}
