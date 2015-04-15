package com.thanksmister.bitcoin.localtrader.ui.advertise;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import com.thanksmister.bitcoin.localtrader.BaseActivity;
import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.data.api.model.Advertisement;
import com.thanksmister.bitcoin.localtrader.data.api.model.Method;
import com.thanksmister.bitcoin.localtrader.data.api.model.RetroError;
import com.thanksmister.bitcoin.localtrader.data.api.model.TradeType;
import com.thanksmister.bitcoin.localtrader.data.services.DataService;
import com.thanksmister.bitcoin.localtrader.ui.traderequest.TradeRequestActivity;
import com.thanksmister.bitcoin.localtrader.utils.DataServiceUtils;
import com.thanksmister.bitcoin.localtrader.utils.TradeUtils;

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
public class AdvertiserPresenterImpl implements AdvertiserPresenter
{
    private AdvertiserView view;
    private DataService service;
    private Subscription subscription;
    private Advertisement advertisement;

    public AdvertiserPresenterImpl(AdvertiserView view, DataService service) 
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
    public void getAdvertisement(String adId)
    {
        subscription = service.getAdvertisement(new Observer<Advertisement>() {
            @Override
            public void onCompleted() {
            }

            @Override
            public void onError(Throwable throwable) {
                RetroError retroError = DataServiceUtils.convertRetroError(throwable, getContext());
                if(retroError.isAuthenticationError()) {
                    Toast.makeText(getContext(), retroError.getMessage(), Toast.LENGTH_SHORT).show();
                    ((BaseActivity) getContext()).logOut();
                } else {
                    getView().showError(getContext().getString(R.string.error_no_trade_data));
                }
            }

            @Override
            public void onNext(Advertisement ad) {

                Timber.d("Advertisement: " + ad.ad_id);
                advertisement = ad;

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
        Timber.d("getOnlineProviders");
        
        Observable<List<Method>> methods = service.getOnlineProviders();
        subscription = methods.subscribe(new Observer<List<Method>>() {
            @Override
            public void onCompleted() {
            }

            @Override
            public void onError(Throwable e) {
                Timber.e("Error getting providers!");
                setAdvertisement(advertisement, null);
                getView().hideProgress();
            }

            @Override
            public void onNext(List<Method> methods){
                Method method = TradeUtils.getMethodForAdvertisement(advertisement, methods);
                setAdvertisement(advertisement, method);
                getView().hideProgress();
            }
        });
    }

    public void setAdvertisement(Advertisement advertisement, Method method)
    {
        getView().setAdvertisement(advertisement, method);
        getView().setHeader(advertisement.trade_type);
    }

    @Override
    public void showTradeRequest()
    {
        Intent intent = TradeRequestActivity.createStartIntent(getView().getContext(), advertisement.ad_id, advertisement.temp_price, advertisement.min_amount, advertisement.max_amount, advertisement.currency, advertisement.profile.username);
        intent.setClass(getView().getContext(), TradeRequestActivity.class);
        getView().getContext().startActivity(intent);
    }
    
    @Override
    public void showPublicAdvertisement()
    {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(advertisement.actions.public_view));
        getView().getContext().startActivity(browserIntent);
    }

    @Override
    public void showProfile()
    {
        String url = "https://localbitcoins.com/accounts/profile/" + advertisement.profile.username + "/";
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        getView().getContext().startActivity(browserIntent);
    }

    @Override
    public void showAdvertisementOnMap()
    {
        String geoUri = "";
        if(advertisement.trade_type == TradeType.LOCAL_BUY || advertisement.trade_type == TradeType.LOCAL_SELL) {
            geoUri = "http://maps.google.com/maps?q=loc:" + advertisement.lat + "," + advertisement.lon + " (" + advertisement.location + ")";
        } else {
            geoUri = "geo:0,0?q=" + advertisement.location;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(geoUri));
        getView().getContext().startActivity(intent);
    }

    private Context getContext()
    {
        return getView().getContext();
    }

    private AdvertiserView getView()
    {
        return view;
    }
}
