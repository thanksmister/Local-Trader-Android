package com.thanksmister.bitcoin.localtrader.ui.advertise;

/**
 * Author: Michael Ritchie
 * Date: 12/30/14
 * Copyright 2013, ThanksMister LLC
 */

import com.thanksmister.bitcoin.localtrader.ApplicationModule;
import com.thanksmister.bitcoin.localtrader.data.services.DataService;
import com.thanksmister.bitcoin.localtrader.data.services.GeoLocationService;
import com.thanksmister.bitcoin.localtrader.ui.searchresults.SearchResultsActivity;
import com.thanksmister.bitcoin.localtrader.ui.searchresults.SearchResultsPresenter;
import com.thanksmister.bitcoin.localtrader.ui.searchresults.SearchResultsPresenterImpl;
import com.thanksmister.bitcoin.localtrader.ui.searchresults.SearchResultsView;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * This module represents objects which exist only for the scope of a single activity. We can
 * safely create singletons using the activity instance because the entire object graph will only
 * ever exist inside of that activity.
 */
@Module(
        injects = {AdvertiserActivity.class},
        addsTo = ApplicationModule.class
)
public class AdvertiserModule
{
    private AdvertiserView view;

    public AdvertiserModule(AdvertiserView view) 
    {
        this.view = view;
    }

    @Provides 
    @Singleton 
    public AdvertiserView provideView() {
        return view;
    }

    @Provides 
    @Singleton
    public AdvertiserPresenter providePresenter(AdvertiserView view,  DataService dataService) 
    {
        return new AdvertiserPresenterImpl(view, dataService);
    }
}