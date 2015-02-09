package com.thanksmister.bitcoin.localtrader.ui.traderequest;

import android.widget.Toast;

import com.squareup.otto.Subscribe;
import com.thanksmister.bitcoin.localtrader.BaseActivity;
import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.data.api.model.ContactRequest;
import com.thanksmister.bitcoin.localtrader.data.api.model.RetroError;
import com.thanksmister.bitcoin.localtrader.data.api.model.TradeType;
import com.thanksmister.bitcoin.localtrader.data.api.model.Wallet;
import com.thanksmister.bitcoin.localtrader.data.services.DataService;
import com.thanksmister.bitcoin.localtrader.events.NetworkEvent;
import com.thanksmister.bitcoin.localtrader.ui.request.RequestPresenter;
import com.thanksmister.bitcoin.localtrader.ui.request.RequestView;

import rx.Observer;
import rx.Subscription;
import timber.log.Timber;

/**
 * Author: Michael Ritchie
 * Date: 12/31/14
 * Copyright 2013, ThanksMister LLC
 */
public class TradeRequestPresenterImpl implements TradeRequestPresenter
{
    private TradeRequestView view;
    private DataService service;
    private Subscription subscription;

    public TradeRequestPresenterImpl(TradeRequestView view, DataService service) 
    {
        this.view = view;
        this.service = service;
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
    public void sendTradeRequest(String ad_id, String amount, String message)
    {
        subscription = service.createContact(new Observer<ContactRequest>() {
            @Override
            public void onCompleted() {
                // TODO hide progress
            }

            @Override
            public void onError(Throwable throwable) {
                if (throwable instanceof RetroError) {
                    Timber.e("Error message: " + throwable.getMessage());
                    Timber.e("Error code: " + ((RetroError) throwable).getCode());
                } else {
                    Toast.makeText(getView().getContext(), "Unable to send trade request", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onNext(ContactRequest contactRequest) {
                Toast.makeText(getView().getContext(), "Contact made: " + contactRequest.contact_id, Toast.LENGTH_SHORT).show();
                // TODO load main activity trades drawer and show success dialog
            }
        }, ad_id, amount, message);
    }

    private TradeRequestView getView()
    {
        return view;
    }

    @Subscribe
    public void onNetworkEvent(NetworkEvent event)
    {
        Timber.d("onNetworkEvent: " + event.name());

        if(event == NetworkEvent.DISCONNECTED) {
            //cancelCheck(); // stop checking we have no network
        } else  {
            //startCheck();
        }
    }
}
