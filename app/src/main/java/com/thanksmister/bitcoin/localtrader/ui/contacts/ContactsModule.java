package com.thanksmister.bitcoin.localtrader.ui.contacts;

/**
 * Author: Michael Ritchie
 * Date: 12/30/14
 * Copyright 2013, ThanksMister LLC
 */

import com.squareup.otto.Bus;
import com.thanksmister.bitcoin.localtrader.ApplicationModule;
import com.thanksmister.bitcoin.localtrader.data.services.DataService;
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
        injects = {ContactsActivity.class},
        addsTo = ApplicationModule.class
)
public class ContactsModule
{
    private ContactsView view;

    public ContactsModule(ContactsView view) 
    {
        this.view = view;
    }

    @Provides 
    @Singleton 
    public ContactsView provideView() {
        return view;
    }

    @Provides @Singleton
    public ContactsPresenter providePresenter(ContactsView view, DataService dataService, Bus bus) 
    {
        return new ContactsPresenterImpl(view, dataService, bus);
    }
}