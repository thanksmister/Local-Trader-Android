package com.thanksmister.bitcoin.localtrader.ui.advertisement;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import com.squareup.otto.Bus;
import com.thanksmister.bitcoin.localtrader.BaseActivity;
import com.thanksmister.bitcoin.localtrader.BaseApplication;
import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.data.DataModel;
import com.thanksmister.bitcoin.localtrader.data.api.model.Advertisement;
import com.thanksmister.bitcoin.localtrader.data.api.model.Method;
import com.thanksmister.bitcoin.localtrader.data.api.model.RetroError;
import com.thanksmister.bitcoin.localtrader.data.api.model.TradeType;
import com.thanksmister.bitcoin.localtrader.data.services.DataService;
import com.thanksmister.bitcoin.localtrader.events.ConfirmationDialogEvent;
import com.thanksmister.bitcoin.localtrader.ui.contact.ContactActivity;
import com.thanksmister.bitcoin.localtrader.ui.edit.EditActivity;
import com.thanksmister.bitcoin.localtrader.utils.DataServiceUtils;
import com.thanksmister.bitcoin.localtrader.utils.TradeUtils;

import org.json.JSONObject;

import java.util.List;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import timber.log.Timber;

/**
 * Author: Michael Ritchie
 * Date: 12/31/14
 * Copyright 2013, ThanksMister LLC
 */
public class AdvertisementPresenterImpl implements AdvertisementPresenter
{
    private final AdvertisementView view;
    private final DataService service;
    private final Bus bus;
    private Subscription subscription;
    private final DataModel dataModel;

    public AdvertisementPresenterImpl(AdvertisementView view, DataService service, Bus bus, DataModel dataModel) 
    {
        this.view = view;
        this.service = service;
        this.bus = bus;
        this.dataModel = dataModel;
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

    public void getAdvertisement(String adId)
    {
        subscription = service.getAdvertisement(new Observer<Advertisement>() {
            @Override
            public void onCompleted() { 
                //getView().hideProgress();
            }

            @Override
            public void onError(Throwable throwable) {
                RetroError retroError = DataServiceUtils.convertRetroError(throwable, getContext());
                if(retroError.isAuthenticationError()) {
                    Toast.makeText(getContext(), retroError.getMessage(), Toast.LENGTH_SHORT).show();
                    ((BaseActivity) getContext()).logOut();
                } else if (retroError.isNetworkError()) {
                    getView().onError(retroError.getMessage());
                } else {
                    getView().onError("Unable to retrieve advertisement data.");
                }
            }

            @Override
            public void onNext(Advertisement advertisement) {

                if(TradeUtils.isOnlineTrade(advertisement)) {
                    getOnlineProviders(advertisement); // get methods
                } else {
                    setAdvertisement(advertisement, null);
                    getView().hideProgress();
                }
            }
        }, adId);
    }

    public void getOnlineProviders(final Advertisement advertisement)
    {
        Observable<List<Method>> methods = service.getOnlineProviders();
        subscription = methods.subscribe(new Observer<List<Method>>() {
            @Override
            public void onCompleted() {
                getView().hideProgress();
            }

            @Override
            public void onError(Throwable e) {
                getView().hideProgress();
                setAdvertisement(advertisement, null);
            }

            @Override
            public void onNext(List<Method> methods){
                Method method = TradeUtils.getMethodForAdvertisement(advertisement, methods);
                setAdvertisement(advertisement, method);
            }
        });
    }

    public void setAdvertisement(Advertisement advertisement, Method method)
    {
        dataModel.advertisement = advertisement;
        getView().setAdvertisement(advertisement, method);
    }

    @Override
    public void editAdvertisement()
    {
        Intent intent = EditActivity.createStartIntent(getView().getContext(), false);
        intent.setClass(getView().getContext(), EditActivity.class);
        ((BaseActivity) getContext()).startActivityForResult(intent, EditActivity.REQUEST_CODE);
    }

    @Override
    public void updateAdvertisementVisibility()
    {
        getAdvertisement().visible = !getAdvertisement().visible;
        subscription = service.updateAdvertisement(new Observer<Advertisement>() {
            @Override
            public void onCompleted() {
            }

            @Override
            public void onError(Throwable throwable) {
                Timber.e("Error updating: " + throwable.getMessage());
                Toast.makeText(getView().getContext(), "Error updating visibility!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNext(Advertisement advertisement) {
                getView().updateAdvertisement(advertisement);
                Toast.makeText(getView().getContext(), "Visibility updated!", Toast.LENGTH_SHORT).show();
            }
        }, getAdvertisement());
    }

    @Override
    public void deleteAdvertisement()
    {
        Context context = getView().getContext();
        ConfirmationDialogEvent event = new ConfirmationDialogEvent("Delete Advertisement", 
                context.getString(R.string.advertisement_delete_confirm), 
                context.getString(R.string.button_delete), 
                context.getString(R.string.button_cancel), new Action0() {
            @Override
            public void call() {
                deleteAdvertisementConfirmed(getAdvertisement().ad_id);
            }
        });

        ((BaseActivity) context).showConfirmationDialog(event);
    }

    private void deleteAdvertisementConfirmed(String adId)
    {
        Context context = getView().getContext();
        
        // TODO send back deleted item
        Observable<JSONObject> observable = service.deleteAdvertisement(adId);
        subscription = observable.subscribe(new Action1<JSONObject>() {
            @Override
            public void call(JSONObject jsonObject) {
                //{"data": {"message": "Ad deleted successfully!"}}
                Toast.makeText(getView().getContext(), "Advertisement deleted!", Toast.LENGTH_SHORT).show();
                ((BaseActivity) context).finish();
            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                Timber.e("Error deleting: " + throwable.getMessage());
                Toast.makeText(getView().getContext(), "Error deleting advertisement!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void shareAdvertisement()
    {
        Context context = getView().getContext();
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");

        String message = "";
        String buyOrSell = (getAdvertisement().trade_type == TradeType.ONLINE_SELL || getAdvertisement().trade_type == TradeType.LOCAL_SELL)? "Buy":"Sell";
        String prep = (getAdvertisement().trade_type == TradeType.ONLINE_SELL || getAdvertisement().trade_type == TradeType.LOCAL_SELL)? " from ":" to ";
        String onlineOrLocal = (getAdvertisement().trade_type == TradeType.LOCAL_SELL || getAdvertisement().trade_type == TradeType.LOCAL_BUY)? "locally":"online";

        if(getAdvertisement().trade_type == TradeType.LOCAL_SELL || getAdvertisement().trade_type == TradeType.LOCAL_BUY) {
            message = buyOrSell + " bitcoins " + onlineOrLocal + " in " + getAdvertisement().location + prep + getAdvertisement().profile.username  + ": " + getAdvertisement().actions.public_view;
        } else {
            String provider = TradeUtils.parsePaymentServiceTitle(getAdvertisement().online_provider);
            message = buyOrSell + " bitcoins " + onlineOrLocal + " in " + getAdvertisement().location + prep + getAdvertisement().profile.username  + " via "  + provider + ": " + getAdvertisement().actions.public_view;
        }

        shareIntent.putExtra(Intent.EXTRA_TEXT, message);
        shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, buyOrSell + " bitcoins in " + getAdvertisement().location);
        context.startActivity(Intent.createChooser(shareIntent, "Share to:"));
    }

    @Override
    public void viewOnlineAdvertisement()
    {
        Context context = getView().getContext();
        if(getAdvertisement().actions.public_view == null) return;
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getAdvertisement().actions.public_view));
        context.startActivity(intent);
    }

    @Override
    public void showAdvertisementOnMap()
    {
        Advertisement advertisement = dataModel.advertisement;
        Context context = getView().getContext();
        String geoUri = "";
        if(advertisement.trade_type == TradeType.LOCAL_BUY || advertisement.trade_type == TradeType.LOCAL_SELL) {
            geoUri = "http://maps.google.com/maps?q=loc:" + advertisement.lat + "," + advertisement.lon + " (" + advertisement.location + ")";
        } else {
            geoUri = "geo:0,0?q=" + advertisement.location;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(geoUri));
        context.startActivity(intent);
    }
    
    private Advertisement getAdvertisement()
    {
        return dataModel.advertisement;
    }

 

    private AdvertisementView getView()
    {
        return view;
    }

    public Context getContext()
    {
        return getView().getContext();
    }
}
