package com.thanksmister.bitcoin.localtrader.ui.searchresults;

/**
 * Author: Michael Ritchie
 * Date: 12/30/14
 * Copyright 2013, ThanksMister LLC
 */

import com.squareup.otto.Bus;
import com.thanksmister.bitcoin.localtrader.ApplicationModule;
import com.thanksmister.bitcoin.localtrader.BaseApplication;
import com.thanksmister.bitcoin.localtrader.data.services.DataService;
import com.thanksmister.bitcoin.localtrader.data.services.GeoLocationService;
import com.thanksmister.bitcoin.localtrader.ui.contact.ContactActivity;
import com.thanksmister.bitcoin.localtrader.ui.contact.ContactPresenter;
import com.thanksmister.bitcoin.localtrader.ui.contact.ContactPresenterImpl;
import com.thanksmister.bitcoin.localtrader.ui.contact.ContactView;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * This module represents objects which exist only for the scope of a single activity. We can
 * safely create singletons using the activity instance because the entire object graph will only
 * ever exist inside of that activity.
 */
@Module(
        injects = {SearchResultsActivity.class},
        addsTo = ApplicationModule.class
)
public class SearchResultsModule
{
    private SearchResultsView view;

    public SearchResultsModule(SearchResultsView view) 
    {
        this.view = view;
    }

    @Provides 
    @Singleton 
    public SearchResultsView provideView() {
        return view;
    }

    @Provides @Singleton
    public SearchResultsPresenter providePresenter(SearchResultsView view, GeoLocationService service, DataService dataService) 
    {
        return new SearchResultsPresenterImpl(view, service, dataService);
    }
}