package com.thanksmister.bitcoin.localtrader.ui.dashboard;

/**
 * Author: Michael Ritchie
 * Date: 12/30/14
 * Copyright 2013, ThanksMister LLC
 */

import com.squareup.otto.Bus;
import com.thanksmister.bitcoin.localtrader.ApplicationModule;
import com.thanksmister.bitcoin.localtrader.BaseApplication;
import com.thanksmister.bitcoin.localtrader.data.services.DataService;
import com.thanksmister.bitcoin.localtrader.ui.wallet.WalletFragment;
import com.thanksmister.bitcoin.localtrader.ui.wallet.WalletPresenter;
import com.thanksmister.bitcoin.localtrader.ui.wallet.WalletPresenterImpl;
import com.thanksmister.bitcoin.localtrader.ui.wallet.WalletView;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * This module represents objects which exist only for the scope of a single activity. We can
 * safely create singletons using the activity instance because the entire object graph will only
 * ever exist inside of that activity.
 */
@Module(
        injects = {DashboardFragment.class},
        addsTo = ApplicationModule.class
)
public class DashboardModule
{
    private DashboardView view;

    public DashboardModule(DashboardView view) 
    {
        this.view = view;
    }

    @Provides 
    @Singleton 
    public DashboardView provideView() 
    {
        return view;
    }

    @Provides @Singleton
    public DashboardPresenter providePresenter(DashboardView view, DataService dataService, Bus bus) 
    {
        return new DashboardPresenterImpl(view, dataService, bus);
    }
}