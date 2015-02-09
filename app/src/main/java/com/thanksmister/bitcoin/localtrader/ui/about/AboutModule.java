package com.thanksmister.bitcoin.localtrader.ui.about;

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
import com.thanksmister.bitcoin.localtrader.ui.edit.EditActivity;
import com.thanksmister.bitcoin.localtrader.ui.edit.EditPresenter;
import com.thanksmister.bitcoin.localtrader.ui.edit.EditPresenterImpl;
import com.thanksmister.bitcoin.localtrader.ui.edit.EditView;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * This module represents objects which exist only for the scope of a single activity. We can
 * safely create singletons using the activity instance because the entire object graph will only
 * ever exist inside of that activity.
 */
@Module(
        injects = {AboutActivity.class},
        addsTo = ApplicationModule.class
)
public class AboutModule
{
    private AboutView view;

    public AboutModule(AboutView view) 
    {
        this.view = view;
    }

    @Provides 
    @Singleton 
    public AboutView provideView() 
    {
        return view;
    }

    @Provides @Singleton
    public AboutPresenter providePresenter(AboutView view) 
    {
        return new AboutPresenterImpl(view);
    }
}