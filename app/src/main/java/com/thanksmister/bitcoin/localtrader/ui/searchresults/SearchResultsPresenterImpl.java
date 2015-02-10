package com.thanksmister.bitcoin.localtrader.ui.searchresults;

import android.content.Intent;
import android.location.Address;

import com.squareup.otto.Subscribe;
import com.thanksmister.bitcoin.localtrader.BaseActivity;
import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.data.api.model.Advertisement;
import com.thanksmister.bitcoin.localtrader.data.api.model.Method;
import com.thanksmister.bitcoin.localtrader.data.api.model.TradeType;
import com.thanksmister.bitcoin.localtrader.data.services.DataService;
import com.thanksmister.bitcoin.localtrader.data.services.GeoLocationService;
import com.thanksmister.bitcoin.localtrader.events.NetworkEvent;
import com.thanksmister.bitcoin.localtrader.ui.advertise.AdvertiserActivity;
import com.thanksmister.bitcoin.localtrader.utils.TradeUtils;

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
public class SearchResultsPresenterImpl implements SearchResultsPresenter
{
    private SearchResultsView view;
    private GeoLocationService service;
    private DataService dataService;
    private Subscription subscription;
    private List<Method> methods;
    private List<Advertisement> advertisements;

    public SearchResultsPresenterImpl(SearchResultsView view, GeoLocationService service, DataService dataService) 
    {
        this.view = view;
        this.service = service;
        this.dataService = dataService;
    }

    @Override
    public void onResume()
    {  
    }

    @Override
    public void onDestroy()
    {
        if(subscription != null)
            subscription.unsubscribe();
    }

    @Override
    public void getAdvertisements(final TradeType tradeType, final Address address, final String paymentMethod)
    {
        assert address != null;
        
        Timber.d("TradeType: " + tradeType.name());
        Timber.d("PaymentMethod: " + paymentMethod);

        if(tradeType == TradeType.LOCAL_BUY || tradeType == TradeType.LOCAL_SELL) {

            if(advertisements != null) {
                getView().setData(advertisements, methods, tradeType, TradeUtils.getAddressShort(address));
                return;
            }
            
            subscription = service.getLocalAdvertisements(new Observer<List<Advertisement>>() {
                
                @Override
                public void onCompleted() {
                    
                }

                @Override
                public void onError(Throwable e) 
                {
                    Timber.e(e.getMessage());
                }

                @Override
                public void onNext(List<Advertisement> advertisements) {
                    if (getView() != null) {
                        getView().setData(advertisements, null, tradeType, TradeUtils.getAddressShort(address));
                    }
                }
            }, address.getLatitude(), address.getLongitude(), tradeType);
            
        } else {  
            
            assert paymentMethod != null;
            
            if(advertisements != null && methods != null) {
                getView().setData(advertisements, methods, tradeType, TradeUtils.getAddressShort(address));
                return;
            }
            
            if(methods == null) {
                getPaymentMethods(tradeType, address, paymentMethod);
            } else {
                Method method = TradeUtils.getPaymentMethod(paymentMethod, methods);
                getOnlineAdvertisements(methods, tradeType, address, method);
            }
        }
    }

    protected void getOnlineAdvertisements(List<Method> methods, TradeType tradeType, Address address, Method paymentMethod)
    {
        Timber.d("getOnlineAdvertisements");

        Timber.d("TradeType: " + tradeType.name());
        Timber.d("PaymentMethod: " + paymentMethod);
        
        subscription = service.getOnlineAdvertisements(new Observer<List<Advertisement>>() {
            @Override
            public void onCompleted() {
            }

            @Override
            public void onError(Throwable e) {
                Timber.e(e.getMessage());

                Timber.d("getOnlineAdvertisements :: onError");
               // getView().showError(e.getMessage());
            }

            @Override
            public void onNext(List<Advertisement> advertisements)
            {
                getView().setData(advertisements, methods, tradeType, TradeUtils.getAddressShort(address));
            }
        }, address.getCountryCode(), address.getCountryName(), tradeType, paymentMethod);
    }

    protected void getPaymentMethods(TradeType tradeType, Address address, final String paymentMethod)
    {
        Timber.d("getPaymentMethods");
        
        Observable<List<Method>> observable = dataService.getOnlineProviders();
        subscription = observable.subscribe(new Observer<List<Method>>() {
            @Override
            public void onCompleted() {
                Timber.d("getPaymentMethods :: onComplete");
            }

            @Override
            public void onError(Throwable e) {
                
                if(e.getMessage() != null) {
                    Timber.d("getPaymentMethods :: onError");
                    Timber.e("getPaymentMethods Error: " + e.getMessage());
                }
                
                //getView().showError(e.getMessage());
            }

            @Override
            public void onNext(List<Method> results) {
                
                Timber.d("getPaymentMethods :: onNext: " + results);
                
                methods = results;
                Method method = TradeUtils.getPaymentMethod(paymentMethod, methods);
                getOnlineAdvertisements(methods, tradeType, address, method);
            }
        });
    }

    @Override
    public void showAdvertiser(Advertisement advertisement)
    {
        Intent intent = AdvertiserActivity.createStartIntent(getView().getContext(), advertisement.ad_id);
        intent.setClass(getView().getContext(), AdvertiserActivity.class);
        getView().getContext().startActivity(intent);
    }

    private SearchResultsView getView()
    {
        return view;
    }
}
