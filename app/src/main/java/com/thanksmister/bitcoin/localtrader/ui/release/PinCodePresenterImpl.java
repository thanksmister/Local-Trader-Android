package com.thanksmister.bitcoin.localtrader.ui.release;

import android.content.Intent;
import android.widget.Toast;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import com.thanksmister.bitcoin.localtrader.BaseActivity;
import com.thanksmister.bitcoin.localtrader.BaseApplication;
import com.thanksmister.bitcoin.localtrader.data.api.model.RetroError;
import com.thanksmister.bitcoin.localtrader.data.services.DataService;
import com.thanksmister.bitcoin.localtrader.events.NetworkEvent;
import com.thanksmister.bitcoin.localtrader.utils.DataServiceUtils;

import retrofit.RetrofitError;
import rx.Observer;
import rx.Subscription;
import timber.log.Timber;

/**
 * Author: Michael Ritchie
 * Date: 12/31/14
 * Copyright 2013, ThanksMister LLC
 */
public class PinCodePresenterImpl implements PinCodePresenter
{
    private PinCodeView view;
    private DataService service;
    private Subscription subscription;

    public PinCodePresenterImpl(PinCodeView view, DataService service) 
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
    public void validatePinCode(final String pinCode, final String address, final String amount)
    {
        subscription = service.validatePinCode(new Observer<String>() {
            @Override
            public void onCompleted()
            {
            }

            @Override
            public void onError(Throwable throwable) {
                Toast.makeText(getView().getContext(),throwable.getMessage(), Toast.LENGTH_SHORT).show();
                ((BaseActivity) getView().getContext()).finish();
            }

            @Override
            public void onNext(String pinCode) {  
                Toast.makeText(getView().getContext(), "PIN verified!", Toast.LENGTH_SHORT).show();
                
                Intent intent = ((BaseActivity) getView().getContext()).getIntent();
                intent.putExtra(PinCodeActivity.EXTRA_PIN_CODE, pinCode);
                intent.putExtra(PinCodeActivity.EXTRA_ADDRESS, address);
                intent.putExtra(PinCodeActivity.EXTRA_AMOUNT, amount);
                
                ((BaseActivity) getView().getContext()).setResult(PinCodeActivity.RESULT_VERIFIED, intent);
                ((BaseActivity) getView().getContext()).finish();
            }
        }, pinCode);
    }

    private PinCodeView getView()
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
