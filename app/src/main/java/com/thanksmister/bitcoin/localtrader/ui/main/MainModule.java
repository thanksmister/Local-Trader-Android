package com.thanksmister.bitcoin.localtrader.ui.main;

/**
 * Author: Michael Ritchie
 * Date: 12/30/14
 * Copyright 2013, ThanksMister LLC
 */

import com.thanksmister.bitcoin.localtrader.ApplicationModule;

import dagger.Module;
import dagger.Provides;

import javax.inject.Singleton;

/**
 * This module represents objects which exist only for the scope of a single activity. We can
 * safely create singletons using the activity instance because the entire object graph will only
 * ever exist inside of that activity.
 */
@Module(
        injects = {MainActivity.class},
        addsTo = ApplicationModule.class
)
public class MainModule
{
    private MainView view;

    public MainModule(MainView view) 
    {
        this.view = view;
    }

    @Provides 
    @Singleton 
    public MainView provideView() {
        return view;
    }

    @Provides @Singleton
    public MainPresenter providePresenter(MainView mainView) 
    {
        return new MainPresenterImpl(mainView);
    }
}