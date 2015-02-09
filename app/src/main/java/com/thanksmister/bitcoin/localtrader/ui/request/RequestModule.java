package com.thanksmister.bitcoin.localtrader.ui.request;

/**
 * Author: Michael Ritchie
 * Date: 12/30/14
 * Copyright 2013, ThanksMister LLC
 */

import com.squareup.otto.Bus;
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
        injects = {RequestFragment.class},
        addsTo = ApplicationModule.class
)
public class RequestModule
{
    private RequestView view;

    public RequestModule(RequestView view) 
    {
        this.view = view;
    }

    @Provides 
    @Singleton 
    public RequestView provideView() {
        return view;
    }

    @Provides @Singleton
    public RequestPresenter providePresenter(RequestView view, DataService dataService, Bus bus) 
    {
        return new RequestPresenterImpl(view, dataService, bus);
    }
}