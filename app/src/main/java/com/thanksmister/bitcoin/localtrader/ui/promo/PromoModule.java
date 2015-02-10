package com.thanksmister.bitcoin.localtrader.ui.promo;

/**
 * Author: Michael Ritchie
 * Date: 12/30/14
 * Copyright 2013, ThanksMister LLC
 */

import com.thanksmister.bitcoin.localtrader.ApplicationModule;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * This module represents objects which exist only for the scope of a single activity. We can
 * safely create singletons using the activity instance because the entire object graph will only
 * ever exist inside of that activity.
 */
@Module(
        injects = {PromoActivity.class},
        addsTo = ApplicationModule.class
)
public class PromoModule
{
    private PromoView view;

    public PromoModule(PromoView view) 
    {
        this.view = view;
    }

    @Provides 
    @Singleton 
    public PromoView provideView() 
    {
        return view;
    }

    @Provides @Singleton
    public PromoPresenter providePresenter(PromoView view) 
    {
        return new PromoPresenterImpl(view);
    }
}