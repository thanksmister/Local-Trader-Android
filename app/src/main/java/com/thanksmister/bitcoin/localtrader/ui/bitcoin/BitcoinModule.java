package com.thanksmister.bitcoin.localtrader.ui.bitcoin;

/**
 * Author: Michael Ritchie
 * Date: 12/30/14
 * Copyright 2013, ThanksMister LLC
 */

import com.thanksmister.bitcoin.localtrader.ApplicationModule;
import com.thanksmister.bitcoin.localtrader.ui.main.MainActivity;
import com.thanksmister.bitcoin.localtrader.ui.main.MainPresenter;
import com.thanksmister.bitcoin.localtrader.ui.main.MainPresenterImpl;
import com.thanksmister.bitcoin.localtrader.ui.main.MainView;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * This module represents objects which exist only for the scope of a single activity. We can
 * safely create singletons using the activity instance because the entire object graph will only
 * ever exist inside of that activity.
 */
@Module(
        injects = {BitcoinHandler.class},
        addsTo = ApplicationModule.class
)
public class BitcoinModule
{
    private BitcoinView view;

    public BitcoinModule(BitcoinView view) 
    {
        this.view = view;
    }

    @Provides 
    @Singleton 
    public BitcoinView provideView() {
        return view;
    }

    @Provides @Singleton
    public BitcoinPresenter providePresenter(BitcoinView view) 
    {
        return new BitcoinPresenterImpl(view);
    }
}