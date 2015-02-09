package com.thanksmister.bitcoin.localtrader.ui.bitcoin;

import com.thanksmister.bitcoin.localtrader.ui.main.MainPresenter;
import com.thanksmister.bitcoin.localtrader.ui.main.MainView;

/**
 * Author: Michael Ritchie
 * Date: 12/31/14
 * Copyright 2013, ThanksMister LLC
 */
public class BitcoinPresenterImpl implements BitcoinPresenter
{
    private final BitcoinView mainView;

    public BitcoinPresenterImpl(BitcoinView mainView) 
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
