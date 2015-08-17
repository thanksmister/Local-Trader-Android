/*
 * Copyright (c) 2015 ThanksMister LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed 
 * under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 */

package com.thanksmister.bitcoin.localtrader.data.services;

import android.content.IntentSender;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.thanksmister.bitcoin.localtrader.BaseApplication;
import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.data.api.LocalBitcoins;
import com.thanksmister.bitcoin.localtrader.data.api.model.Advertisement;
import com.thanksmister.bitcoin.localtrader.data.api.model.Method;
import com.thanksmister.bitcoin.localtrader.data.api.model.Place;
import com.thanksmister.bitcoin.localtrader.data.api.model.TradeType;
import com.thanksmister.bitcoin.localtrader.data.api.transforms.AddressToStringFunc;
import com.thanksmister.bitcoin.localtrader.data.api.transforms.ResponseToAds;
import com.thanksmister.bitcoin.localtrader.data.api.transforms.ResponseToPlace;
import com.thanksmister.bitcoin.localtrader.data.database.MethodItem;
import com.thanksmister.bitcoin.localtrader.data.rx.EndObserver;
import com.thanksmister.bitcoin.localtrader.ui.MainActivity;
import com.thanksmister.bitcoin.localtrader.utils.Strings;
import com.thanksmister.bitcoin.localtrader.utils.WalletUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import pl.charmas.android.reactivelocation.ReactiveLocationProvider;
import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.http.GET;
import retrofit.http.Query;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subjects.BehaviorSubject;
import rx.subjects.PublishSubject;
import timber.log.Timber;

public class GeoLocationService
{
    public final static int MAX_ADDRESSES = 5;
    public final static int NUMBER_UPDATES = 5;
        public final static int UPDATE_INTERVAL = 1000;

    private BaseApplication application;
    private LocalBitcoins localBitcoins;
   
    @Inject
    public GeoLocationService(BaseApplication application, LocalBitcoins localBitcoins)
    {
        this.application = application;
        this.localBitcoins = localBitcoins;
    }
    
    public Observable<Address> getLastKnownLocation()
    {
        final ReactiveLocationProvider locationProvider = new ReactiveLocationProvider(application.getApplicationContext());
        return locationProvider.getLastKnownLocation()
                .flatMap(new Func1<Location, Observable<List<Address>>>()
                {
                    @Override
                    public Observable<List<Address>> call(Location location)
                    {
                        return locationProvider.getReverseGeocodeObservable(location.getLatitude(), location.getLongitude(), 1);
                    }
                })
                .map(new Func1<List<Address>, Address>()
                {
                    @Override
                    public Address call(List<Address> addresses)
                    {
                        return addresses != null && !addresses.isEmpty() ? addresses.get(0) : null;
                    }
                });
    }
    
    public Observable<Address> getUpdatedLocation()
    {
        final ReactiveLocationProvider locationProvider = new ReactiveLocationProvider(application.getApplicationContext());
        
        final LocationRequest locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setNumUpdates(NUMBER_UPDATES)
                .setInterval(UPDATE_INTERVAL);

        return locationProvider.getUpdatedLocation(locationRequest)
                .flatMap(new Func1<Location, Observable<List<Address>>>() {
                    @Override
                    public Observable<List<Address>> call(Location location) {
                        return locationProvider.getReverseGeocodeObservable(location.getLatitude(), location.getLongitude(), 1);
                    }
                })
                .map(new Func1<List<Address>, Address>() {
                    @Override
                    public Address call(List<Address> addresses) {
                        return addresses != null && !addresses.isEmpty() ? addresses.get(0) : null;
                    }
                });
    }
    
    public Observable<List<Address>> geoGetLocationFromName(String userQuery)
    {
        final ReactiveLocationProvider locationProvider = new ReactiveLocationProvider(application.getApplicationContext());
        return locationProvider.getGeocodeObservable(userQuery, MAX_ADDRESSES);
    }
    
    public boolean isGooglePlayServicesAvailable()
    {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(application.getApplicationContext());
        return ConnectionResult.SUCCESS == resultCode;
    }

    public Observable<List<Advertisement>> getLocalAdvertisements(final double latitude, final double longitude, final TradeType tradeType)
    {
        return localBitcoins.getPlaces(latitude, longitude)
                   .map(new ResponseToPlace())
                    .flatMap(new Func1<Place, Observable<List<Advertisement>>>()
                    {
                        @Override
                        public Observable<List<Advertisement>> call(Place place)
                        {
                            return getAdvertisementsInPlace(place, tradeType);
                        }
                    });
    }

    public Observable<List<Advertisement>> getOnlineAdvertisements(final String countryCode, final String countryName, final TradeType type, @NonNull final String paymentMethod)
    {
        String url;
        if(type == TradeType.ONLINE_BUY) {
            url = "buy-bitcoins-online";
        } else {
            url = "sell-bitcoins-online";
        }

        String countryNameFix = countryName.replace(" ", "-");
        if(paymentMethod.equals("all")) {
            return localBitcoins.searchOnlineAds(url, countryCode, countryNameFix)
                    .map(new ResponseToAds())
                    .flatMap(new Func1<List<Advertisement>, Observable<List<Advertisement>>>()
                    {
                        @Override
                        public Observable<List<Advertisement>> call(List<Advertisement> advertisements)
                        {
                            if(advertisements == null)
                                advertisements = Collections.emptyList();
                            
                            return Observable.just(advertisements);
                        }
                    });
        } else {
            return localBitcoins.searchOnlineAds(url, countryCode, countryNameFix, paymentMethod)
                    .map(new ResponseToAds())
                    .flatMap(new Func1<List<Advertisement>, Observable<List<Advertisement>>>()
                    {
                        @Override
                        public Observable<List<Advertisement>> call(List<Advertisement> advertisements)
                        {
                            if(advertisements == null)
                                advertisements = Collections.emptyList();
                            
                            return Observable.just(advertisements);
                        }
                    });
        }
    }

    public Observable<List<Advertisement>> getAdvertisementsInPlace(Place place, TradeType type)
    {
        String url;
        if(type == TradeType.LOCAL_BUY) {
            url = place.buy_local_url.replace("https://localbitcoins.com/", "");
        } else {
            url = place.sell_local_url.replace("https://localbitcoins.com/", "");
        }
        
        String[] split = url.split("/");
        return localBitcoins.searchAdsByPlace(split[0], split[1], split[2])
                .map(new ResponseToAds());
    }

    public Observable<List<Address>> geoDecodeLocation(final Location location)
    {
        final Geocoder geocoder = new Geocoder(application);
        
        return Observable.create(new Observable.OnSubscribe<List<Address>>()
        {
            @Override
            public void call(Subscriber<? super List<Address>> subscriber)
            {
                try {
                    subscriber.onNext(geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1));
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                } 
            }
        });
    }
}
