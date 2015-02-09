package com.thanksmister.bitcoin.localtrader.ui.contact;

/**
 * Author: Michael Ritchie
 * Date: 12/30/14
 * Copyright 2013, ThanksMister LLC
 */

import com.squareup.otto.Bus;
import com.thanksmister.bitcoin.localtrader.ApplicationModule;
import com.thanksmister.bitcoin.localtrader.BaseApplication;
import com.thanksmister.bitcoin.localtrader.data.services.DataService;
import com.thanksmister.bitcoin.localtrader.ui.advertisement.AdvertisementActivity;
import com.thanksmister.bitcoin.localtrader.ui.advertisement.AdvertisementPresenter;
import com.thanksmister.bitcoin.localtrader.ui.advertisement.AdvertisementPresenterImpl;
import com.thanksmister.bitcoin.localtrader.ui.advertisement.AdvertisementView;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * This module represents objects which exist only for the scope of a single activity. We can
 * safely create singletons using the activity instance because the entire object graph will only
 * ever exist inside of that activity.
 */
@Module(
        injects = {ContactActivity.class},
        addsTo = ApplicationModule.class
)
public class ContactModule
{
    private ContactView view;

    public ContactModule(ContactView view) 
    {
        this.view = view;
    }

    @Provides 
    @Singleton 
    public ContactView provideView() {
        return view;
    }

    @Provides @Singleton
    public ContactPresenter providePresenter(ContactView view, DataService dataService, Bus bus) 
    {
        return new ContactPresenterImpl(view, dataService, bus);
    }
}