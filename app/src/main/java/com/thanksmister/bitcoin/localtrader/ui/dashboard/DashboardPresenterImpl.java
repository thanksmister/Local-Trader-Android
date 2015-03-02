package com.thanksmister.bitcoin.localtrader.ui.dashboard;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.widget.Toast;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import com.thanksmister.bitcoin.localtrader.BaseActivity;
import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.data.api.model.Advertisement;
import com.thanksmister.bitcoin.localtrader.data.api.model.Contact;
import com.thanksmister.bitcoin.localtrader.data.api.model.Dashboard;
import com.thanksmister.bitcoin.localtrader.data.api.model.DashboardType;
import com.thanksmister.bitcoin.localtrader.data.api.model.Method;
import com.thanksmister.bitcoin.localtrader.data.api.model.RetroError;
import com.thanksmister.bitcoin.localtrader.data.database.ContactContract;
import com.thanksmister.bitcoin.localtrader.data.services.DataService;
import com.thanksmister.bitcoin.localtrader.events.NavigateEvent;
import com.thanksmister.bitcoin.localtrader.events.NetworkEvent;
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
    private static final int CHECK_DATA = 5*60*1000;// 5 minutes
    
    private DashboardView view;
    private Bus bus;
    private DataService service;
    private Subscription subscriptionInfo;
    private Subscription subscriptionCached;
    private Dashboard dashboard;
    private List<Method> methods;

    public DashboardPresenterImpl(DashboardView view, DataService service, Bus bus) 
    {
        this.view = view;
        this.service = service;
        this.bus = bus;
        methods = new ArrayList<Method>();
    }

    @Override
    public void onResume()
    {
        bus.register(this);

        Timber.d("Resume");

        if (methods == null) {
            getOnlineProviders();
        } else {
            getDashboardCached(methods);
        }
    }

    @Override
    public void onRefresh()
    {
        Timber.d("Manual Refresh");
        
        if (methods == null) {
            getOnlineProviders();
        } else {
            getDashboard(methods, true);
        }
    }

    @Override
    public void onDestroy()
    {
        if(subscriptionInfo != null)
            subscriptionInfo.unsubscribe();

        if(subscriptionCached != null)
            subscriptionCached.unsubscribe();
 
        bus.unregister(this);
    }

    @Override
    public void scanQrCode()
    {
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
        Intent intent = AdvertisementActivity.createStartIntent(getView().getContext(), advertisement.ad_id);
        intent.setClass(getView().getContext(), AdvertisementActivity.class);
        getView().getContext().startActivity(intent);
    }

    private void getDashboardCached(List<Method> methods)
    {
        Timber.d("getDashboardCached");
        
        Observable<Dashboard> observable = service.getDashboardCached();
        subscriptionCached = observable.subscribe(new Observer<Dashboard>() {
            @Override
            public void onCompleted() {
            }

            @Override
            public void onError(Throwable throwable) {
                
                // should be no errors
                
                getView().hideProgress();
                
                getDashboard(methods, true);
            }

            @Override
            public void onNext(Dashboard results)
            {
                Timber.d("Dashboard cache returned");
                
                getView().hideProgress();
                
                dashboard = results;
                
                getView().setDashboard(dashboard, methods);
                
                getDashboard(methods, false);
            }
        });
    }

    private void getDashboard(List<Method> methods, boolean manualRefresh)
    {
        Timber.d("getDashboard refresh: " + manualRefresh);
        
        subscriptionInfo = service.getDashboardInfo(new Observer<Dashboard>() {
            @Override
            public void onCompleted() {
                getView().onRefreshStop();
            }

            @Override
            public void onError(Throwable throwable)
            {
                RetroError retroError = DataServiceUtils.convertRetroError(throwable, getContext());
                if (retroError.isAuthenticationError()) {
                    logOut();
                } else if (retroError.isNetworkError()) {
                    getView().onRetry(getContext().getString(R.string.error_no_internet));
                } else {
                    if (dashboard == null) {
                        getView().onError(getContext().getString(R.string.error_service_error));
                    } else {
                        getView().onRetry(getContext().getString(R.string.error_service_error));
                    }
                }

                getView().onRefreshStop();
            }

            @Override
            public void onNext(Dashboard results)
            {
                Timber.d("Dashboard data returned");
                
                dashboard = results;
                getView().setDashboard(dashboard, methods);
            }
        }, manualRefresh);
    }

    private void getOnlineProviders()
    {
        Observable<List<Method>> observable = service.getOnlineProviders();
        subscriptionInfo = observable.subscribe(new Observer<List<Method>>() {
            @Override
            public void onCompleted() {
                //getView().onRefreshStop();
            }

            @Override
            public void onError(Throwable e) {
                methods = new ArrayList<Method>();
                getDashboard(methods, false);
            }

            @Override
            public void onNext(List<Method> results) {
                methods = results;
                getDashboard(methods, false);
            }
        });
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
    public void logOut()
    {
        ((BaseActivity) getContext()).logOut();
    }

    @Override
    public void createAdvertisementScreen()
    {
        Intent intent = EditActivity.createStartIntent(getView().getContext(), true);
        intent.setClass(getView().getContext(), EditActivity.class);
        getView().getContext().startActivity(intent);
    }

    private Context getContext()
    {
        return getView().getContext();
    }

    private DashboardView getView() 
    {
        return view;
    }

    @Subscribe
    public void onNetworkEvent(NetworkEvent event)
    {
        //Timber.d("onNetworkEvent: " + event.name());
    }

    private Handler delayHandler;

    private void startCheck()
    {
        cancelCheck();
        Timber.d("startCheck");
        if(delayHandler == null) {
            delayHandler = new Handler();
            delayHandler.postDelayed(doRunnable, CHECK_DATA);
        }
    }

    private void cancelCheck()
    {
        Timber.d("cancelCheck");
        if(delayHandler != null) {
            delayHandler.removeCallbacks(doRunnable);
            delayHandler = null;
        }
    }

    Runnable doRunnable = new Runnable() {
        @Override
        public void run(){
            //getData();
        }
    };
}
