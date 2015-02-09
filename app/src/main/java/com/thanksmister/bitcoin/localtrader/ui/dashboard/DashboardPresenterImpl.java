package com.thanksmister.bitcoin.localtrader.ui.dashboard;

import android.content.Intent;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import com.thanksmister.bitcoin.localtrader.BaseActivity;
import com.thanksmister.bitcoin.localtrader.data.api.model.Advertisement;
import com.thanksmister.bitcoin.localtrader.data.api.model.Contact;
import com.thanksmister.bitcoin.localtrader.data.api.model.Dashboard;
import com.thanksmister.bitcoin.localtrader.data.api.model.DashboardType;
import com.thanksmister.bitcoin.localtrader.data.api.model.Method;
import com.thanksmister.bitcoin.localtrader.data.services.DataService;
import com.thanksmister.bitcoin.localtrader.events.NavigateEvent;
import com.thanksmister.bitcoin.localtrader.events.NetworkEvent;
import com.thanksmister.bitcoin.localtrader.events.ProgressEvent;
import com.thanksmister.bitcoin.localtrader.events.ScannerEvent;
import com.thanksmister.bitcoin.localtrader.ui.advertisement.AdvertisementActivity;
import com.thanksmister.bitcoin.localtrader.ui.contact.ContactActivity;
import com.thanksmister.bitcoin.localtrader.ui.contacts.ContactsActivity;
import com.thanksmister.bitcoin.localtrader.ui.edit.EditActivity;
import com.thanksmister.bitcoin.localtrader.utils.DataServiceUtils;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import timber.log.Timber;

/**
 * Author: Michael Ritchie
 * Date: 12/31/14
 * Copyright 2013, ThanksMister LLC
 */
public class DashboardPresenterImpl implements DashboardPresenter
{
    private DashboardView view;
    private Bus bus;
    private DataService service;
    private Subscription subscription;

    public DashboardPresenterImpl(DashboardView view, DataService service, Bus bus) 
    {
        this.view = view;
        this.service = service;
        this.bus = bus;
    }

    @Override
    public void onResume()
    {
        bus.register(this);
        
        getData();
    }

    @Override
    public void onDestroy()
    {
        if(subscription != null)
            subscription.unsubscribe();
 
        bus.unregister(this);
    }

    @Override
    public void scanQrCode()
    {
        //bus.post(ScannerEvent.SCAN);
        ((BaseActivity) getView().getContext()).launchScanner();
    }

    @Override
    public void showContact(Contact contact)
    {
        Intent intent = ContactActivity.createStartIntent(getView().getContext(), contact.contact_id);
        intent.setClass(getView().getContext(), ContactActivity.class);
        getView().getContext().startActivity(intent);
    }

    @Override
    public void showAdvertisement(Advertisement advertisement)
    {
        //Method method = TradeUtils.getMethodForAdvertisement(advertisement, methods);
        //Intent intent = new Intent(application, AdvertisementActivity.class);
        Intent intent = AdvertisementActivity.createStartIntent(getView().getContext(), advertisement.ad_id);
        intent.setClass(getView().getContext(), AdvertisementActivity.class);
        getView().getContext().startActivity(intent);
    }

    private void getData()
    {
        subscription = service.getDashboardInfo(new Observer<Dashboard>() {
            @Override
            public void onCompleted(){
            }

            @Override
            public void onError(Throwable throwable) {
                if (DataServiceUtils.isHttp403Error(throwable)) {
                    logOut();
                } else {
                    getView().showError(throwable.getMessage());
                }
            }

            @Override
            public void onNext(Dashboard dashboard) {
                getOnlineProviders(dashboard);
            }
        });
    }

    public void getOnlineProviders(final Dashboard dashboard)
    {
        Observable<List<Method>> methods = service.getOnlineProviders();
        subscription = methods.subscribe(new Observer<List<Method>>() {
            @Override
            public void onCompleted() {
                getView().hideProgress();
            }

            @Override
            public void onError(Throwable e) {
                Timber.e("Error getting providers!");
                getView().setDashboard(dashboard, new ArrayList<Method>());
            }

            @Override
            public void onNext(List<Method> methods) {
                getView().setDashboard(dashboard, methods);
            }
        });
    }

    @Override
    public void logOut()
    {
        service.logOut();
        bus.post(ProgressEvent.LOGIN);
    }

    @Override
    public void showTradesScreen()
    {
        Intent intent = ContactsActivity.createStartIntent(getView().getContext(), DashboardType.RELEASED);
        intent.setClass(getView().getContext(), ContactsActivity.class);
        getView().getContext().startActivity(intent);
    }

    @Override
    public void showSendScreen()
    {
        bus.post(NavigateEvent.SEND);
    }

    @Override
    public void showSearchScreen()
    {
        bus.post(NavigateEvent.SEARCH);
    }

    @Override
    public void createAdvertisementScreen()
    {
        Intent intent = EditActivity.createStartIntent(getView().getContext(), true);
        intent.setClass(getView().getContext(), EditActivity.class);
        getView().getContext().startActivity(intent);
    }

    private DashboardView getView() 
    {
        return view;
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
