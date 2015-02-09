package com.thanksmister.bitcoin.localtrader.ui.edit;

/**
 * Author: Michael Ritchie
 * Date: 12/30/14
 * Copyright 2013, ThanksMister LLC
 */

import android.location.LocationManager;

import com.thanksmister.bitcoin.localtrader.ApplicationModule;
import com.thanksmister.bitcoin.localtrader.data.DataModel;
import com.thanksmister.bitcoin.localtrader.data.services.DataService;
import com.thanksmister.bitcoin.localtrader.data.services.GeoLocationService;
import com.thanksmister.bitcoin.localtrader.ui.search.SearchFragment;
import com.thanksmister.bitcoin.localtrader.ui.search.SearchPresenter;
import com.thanksmister.bitcoin.localtrader.ui.search.SearchPresenterImpl;
import com.thanksmister.bitcoin.localtrader.ui.search.SearchView;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * This module represents objects which exist only for the scope of a single activity. We can
 * safely create singletons using the activity instance because the entire object graph will only
 * ever exist inside of that activity.
 */
@Module(
        injects = {EditActivity.class},
        addsTo = ApplicationModule.class
)
public class EditModule
{
    private EditView view;

    public EditModule(EditView view) 
    {
        this.view = view;
    }

    @Provides 
    @Singleton 
    public EditView provideView() 
    {
        return view;
    }

    @Provides @Singleton
    public EditPresenter providePresenter(EditView view, DataService dataService, GeoLocationService geoLocationService, LocationManager locationManager, DataModel dataModel) 
    {
        return new EditPresenterImpl(view, dataService, geoLocationService, locationManager, dataModel);
    }
}