package com.thanksmister.bitcoin.localtrader.ui.dashboard;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
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
    private Subscription subscription;
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

        getData();
    }

    @Override
    public void onDestroy()
    {
        if(subscription != null)
            subscription.unsubscribe();
 
        bus.unregister(this);
        
       // cancelCheck();
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

    private void getData()
    {
        subscription = service.getDashboardInfo(new Observer<Dashboard>() {
            @Override
            public void onCompleted()
            {
               
            }

            @Override
            public void onError(Throwable throwable)
            {
                RetroError retroError = DataServiceUtils.convertRetroError(throwable, getContext());
                
                Timber.e("Error: " + retroError.getMessage());
                Timber.e("Code: " + retroError.getCode());
                
                if (retroError.isAuthenticationError()) {
                    logOut();
                } else if (retroError.isNetworkError()) {
                    refreshError(getContext().getString(R.string.error_no_internet));  
                } else {
                    refreshError(retroError.getMessage());
                }

                endRefresh();
            }

            @Override
            public void onNext(Dashboard results)
            {
                dashboard = results;
                if (methods != null) {
                    getView().setDashboard(dashboard, methods);
                    endRefresh();  
                } else {
                    getOnlineProviders(dashboard); 
                }
            }
        });
    }

    public void getOnlineProviders(final Dashboard dashboard)
    {
        Observable<List<Method>> observable = service.getOnlineProviders();
        subscription = observable.subscribe(new Observer<List<Method>>() {
            @Override
            public void onCompleted() {
               
                endRefresh();
            }

            @Override
            public void onError(Throwable e) {
                methods = new ArrayList<Method>();
                getView().setDashboard(dashboard, methods);
                endRefresh();
            }

            @Override
            public void onNext(List<Method> results) {
                methods = results;
                getView().setDashboard(dashboard, methods);
            }
        });
    }

    @Override
    public void logOut()
    {
        ((BaseActivity) getContext()).logOutConfirmation();
    }

    @Override
    public void endRefresh()
    {
        ((BaseActivity) getContext()).onRefreshStop();
    }

    @Override
    public void refreshError(String message)
    {
        ((BaseActivity) getContext()).onError(message);
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
            getData();
        }
    };
}
