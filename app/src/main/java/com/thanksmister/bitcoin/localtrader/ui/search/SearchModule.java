package com.thanksmister.bitcoin.localtrader.ui.search;

/**
 * Author: Michael Ritchie
 * Date: 12/30/14
 * Copyright 2013, ThanksMister LLC
 */

import android.location.LocationManager;

import com.squareup.otto.Bus;
import com.thanksmister.bitcoin.localtrader.ApplicationModule;
import com.thanksmister.bitcoin.localtrader.data.services.DataService;
import com.thanksmister.bitcoin.localtrader.data.services.GeoLocationService;
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
        injects = {SearchFragment.class},
        addsTo = ApplicationModule.class
)
public class SearchModule
{
    private SearchView view;

    public SearchModule(SearchView view) 
    {
        this.view = view;
    }

    @Provides 
    @Singleton 
    public SearchView provideView() 
    {
        return view;
    }

    @Provides @Singleton
    public SearchPresenter providePresenter(SearchView view, LocationManager locationManager, DataService dataService, GeoLocationService service) 
    {
        return new SearchPresenterImpl(view, locationManager, dataService, service);
    }
}