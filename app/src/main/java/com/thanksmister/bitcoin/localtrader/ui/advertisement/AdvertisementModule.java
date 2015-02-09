package com.thanksmister.bitcoin.localtrader.ui.advertisement;

/**
 * Author: Michael Ritchie
 * Date: 12/30/14
 * Copyright 2013, ThanksMister LLC
 */

import com.squareup.otto.Bus;
import com.thanksmister.bitcoin.localtrader.ApplicationModule;
import com.thanksmister.bitcoin.localtrader.BaseApplication;
import com.thanksmister.bitcoin.localtrader.data.DataModel;
import com.thanksmister.bitcoin.localtrader.data.services.DataService;
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
        injects = {AdvertisementActivity.class},
        addsTo = ApplicationModule.class
)
public class AdvertisementModule
{
    private AdvertisementView view;

    public AdvertisementModule(AdvertisementView view) 
    {
        this.view = view;
    }

    @Provides 
    @Singleton 
    public AdvertisementView provideView() {
        return view;
    }

    @Provides @Singleton
    public AdvertisementPresenter providePresenter(AdvertisementView view, DataService dataService, Bus bus, DataModel dataModel) 
    {
        return new AdvertisementPresenterImpl(view, dataService, bus, dataModel);
    }
}