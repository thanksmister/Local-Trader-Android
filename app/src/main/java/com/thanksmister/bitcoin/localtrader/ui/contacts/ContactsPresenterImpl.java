package com.thanksmister.bitcoin.localtrader.ui.contacts;

import android.content.Context;
import android.content.Intent;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import com.thanksmister.bitcoin.localtrader.data.api.model.Contact;
import com.thanksmister.bitcoin.localtrader.data.api.model.DashboardType;
import com.thanksmister.bitcoin.localtrader.data.services.DataService;
import com.thanksmister.bitcoin.localtrader.events.NetworkEvent;
import com.thanksmister.bitcoin.localtrader.ui.contact.ContactActivity;

import java.util.List;

import rx.Observer;
import rx.Subscription;

/**
 * Author: Michael Ritchie
 * Date: 12/31/14
 * Copyright 2014, ThanksMister LLC
 */
public class ContactsPresenterImpl implements ContactsPresenter
{
    private ContactsView view;
    private DataService service;
    private Bus bus;
    private Subscription subscription;

    public ContactsPresenterImpl(ContactsView view, DataService service, Bus bus) 
    {
        this.view = view;
        this.service = service;
        this.bus = bus;
    }

    @Override
    public void onResume()
    {
        bus.register(this);
    }

    @Override
    public void onDestroy()
    {
        if(subscription != null)
            subscription.unsubscribe();

        bus.unregister(this);
    }

    @Override
    public void getContacts(final DashboardType dashboardType)
    {
        // TODO show refresh progress
        getView().setTitle(dashboardType);
        getView().showProgress();
        
        subscription = service.getDashboardByType(new Observer<List<Contact>>()
        {
            @Override
            public void onCompleted()
            {
                getView().hideProgress();
            }

            @Override
            public void onError(Throwable e)
            {
                getView().showError(e.getMessage());
            }

            @Override
            public void onNext(List<Contact> contacts)
            {
                // TODO set data on view
                getView().setContacts(contacts);
            }
        }, dashboardType);
    }

    @Override
    public void getContact(final Contact contact)
    {
        Intent intent = ContactActivity.createStartIntent(getView().getContext(), contact.contact_id);
        intent.setClass(getView().getContext(), ContactActivity.class);
        getView().getContext().startActivity(intent);
    }

    private ContactsView getView()
    {
        return view;
    }
    
    private Context getContext()
    {
        return getView().getContext();
    }

    @Subscribe
    public void onNetworkEvent(NetworkEvent event)
    {
        //Timber.d("onNetworkEvent: " + event.name());

        if(event == NetworkEvent.DISCONNECTED) {
            //cancelCheck(); // stop checking we have no network
        } else  {
            //startCheck();
        }
    }
}
