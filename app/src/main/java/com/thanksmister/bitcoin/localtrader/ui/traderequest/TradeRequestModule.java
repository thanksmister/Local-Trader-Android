package com.thanksmister.bitcoin.localtrader.ui.traderequest;

/**
 * Author: Michael Ritchie
 * Date: 12/30/14
 * Copyright 2013, ThanksMister LLC
 */

import com.thanksmister.bitcoin.localtrader.ApplicationModule;
import com.thanksmister.bitcoin.localtrader.data.services.DataService;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * This module represents objects which exist only for the scope of a single activity. We can
 * safely create singletons using the activity instance because the entire object graph will only
 * ever exist inside of that activity.
 */
@Module(
        injects = {TradeRequestActivity.class},
        addsTo = ApplicationModule.class
)
public class TradeRequestModule
{
    private TradeRequestView view;

    public TradeRequestModule(TradeRequestView view) 
    {
        this.view = view;
    }

    @Provides 
    @Singleton 
    public TradeRequestView provideView() 
    {
        return view;
    }

    @Provides @Singleton
    public TradeRequestPresenter providePresenter(TradeRequestView view, DataService dataService) 
    {
        return new TradeRequestPresenterImpl(view, dataService);
    }
}