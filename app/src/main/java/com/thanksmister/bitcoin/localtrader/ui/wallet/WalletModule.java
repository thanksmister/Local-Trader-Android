package com.thanksmister.bitcoin.localtrader.ui.wallet;

/**
 * Author: Michael Ritchie
 * Date: 12/30/14
 * Copyright 2013, ThanksMister LLC
 */

import com.squareup.otto.Bus;
import com.thanksmister.bitcoin.localtrader.ApplicationModule;
import com.thanksmister.bitcoin.localtrader.data.services.DataService;
import com.thanksmister.bitcoin.localtrader.ui.dashboard.DashboardFragment;
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
        injects = {WalletFragment.class},
        addsTo = ApplicationModule.class
)
public class WalletModule
{
    private WalletView view;

    public WalletModule(WalletView view) 
    {
        this.view = view;
    }

    @Provides 
    @Singleton 
    public WalletView provideView() 
    {
        return view;
    }

    @Provides @Singleton
    public WalletPresenter providePresenter(WalletView view, DataService dataService, Bus bus) 
    {
        return new WalletPresenterImpl(view, dataService, bus);
    }

    /**
     * Allow the activity context to be injected but require that it be annotated with
     * {@link com.thanksmister.bitcoin.localtrader.ForActivity @ForActivity} to explicitly differentiate it from application context.
     */
    /*@Provides
    @Singleton
    @ForActivity
    Context provideActivityContext()
    {
        return activity;
    }*/

    /*@Provides
    @Singleton
    ActivityTitleController provideTitleController()
    {
        return new ActivityTitleController(activity);
    }*/
}