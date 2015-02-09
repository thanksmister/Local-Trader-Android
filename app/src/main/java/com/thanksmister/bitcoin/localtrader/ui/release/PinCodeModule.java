package com.thanksmister.bitcoin.localtrader.ui.release;

/**
 * Author: Michael Ritchie
 * Date: 12/30/14
 * Copyright 2013, ThanksMister LLC
 */

import com.squareup.otto.Bus;
import com.thanksmister.bitcoin.localtrader.ApplicationModule;
import com.thanksmister.bitcoin.localtrader.BaseApplication;
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
        injects = {PinCodeActivity.class},
        addsTo = ApplicationModule.class
)
public class PinCodeModule
{
    private PinCodeView view;

    public PinCodeModule(PinCodeView view) 
    {
        this.view = view;
    }

    @Provides 
    @Singleton 
    public PinCodeView provideView() {
        return view;
    }

    @Provides @Singleton
    public PinCodePresenter providePresenter(PinCodeView view, DataService dataService) 
    {
        return new PinCodePresenterImpl(view, dataService);
    }
}