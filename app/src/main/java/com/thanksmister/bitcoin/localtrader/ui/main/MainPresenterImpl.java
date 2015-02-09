package com.thanksmister.bitcoin.localtrader.ui.main;

import com.google.zxing.android.IntentIntegrator;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import com.thanksmister.bitcoin.localtrader.events.ScannerEvent;
import com.thanksmister.bitcoin.localtrader.ui.main.MainView;

import javax.inject.Inject;

/**
 * Author: Michael Ritchie
 * Date: 12/31/14
 * Copyright 2013, ThanksMister LLC
 */
public class MainPresenterImpl implements MainPresenter
{
    private final MainView mainView;

    public MainPresenterImpl(MainView mainView) 
    {
        this.mainView = mainView;
    }
    
    @Override
    public void onResume()
    {
        
    }

    @Override
    public void onPause()
    {

    }
}
