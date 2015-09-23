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

import android.location.Address;

import android.location.Location;
import android.support.annotation.NonNull;

import com.doctoror.geocoder.Geocoder;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.thanksmister.bitcoin.localtrader.BaseApplication;
import com.thanksmister.bitcoin.localtrader.data.api.LocalBitcoins;
import com.thanksmister.bitcoin.localtrader.data.api.model.Advertisement;
import com.thanksmister.bitcoin.localtrader.data.api.model.Place;
import com.thanksmister.bitcoin.localtrader.data.api.model.TradeType;
import com.thanksmister.bitcoin.localtrader.data.api.transforms.ResponseToAds;
import com.thanksmister.bitcoin.localtrader.data.api.transforms.ResponseToPlace;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import pl.charmas.android.reactivelocation.ReactiveLocationProvider;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Func1;

public class GeoLocationService
{
    public final static int MAX_ADDRESSES = 5;
    public final static int NUMBER_UPDATES = 30;
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
                        try {
                            return locationProvider.getReverseGeocodeObservable(location.getLatitude(), location.getLongitude(), 1);
                        } catch (Exception exception) {
                            return Observable.just(null);
                        }

                    }
                }).map(new Func1<List<Address>, Address>()
                {
                    @Override
                    public Address call(List<Address> addresses)
                    {
                        return (addresses != null && !addresses.isEmpty()) ? addresses.get(0) : null;
                    }
                });
    }

    public Observable<Location> getUpdatedLocation()
    {
        final ReactiveLocationProvider locationProvider = new ReactiveLocationProvider(application.getApplicationContext());

        final LocationRequest locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setNumUpdates(5)
                .setInterval(100);

        return locationProvider.getUpdatedLocation(locationRequest);
    }

    public Observable<Address> getAddressFromLocation(Location location)
    {
        final ReactiveLocationProvider locationProvider = new ReactiveLocationProvider(application.getApplicationContext());

        return locationProvider
                .getReverseGeocodeObservable(location.getLatitude(), location.getLongitude(), 5)
                .map(new Func1<List<Address>, Address>()
                {
                    @Override
                    public Address call(List<Address> addresses)
                    {
                        return addresses != null && !addresses.isEmpty() ? addresses.get(0) : null;
                    }
                });
    }

    public Observable<Address> getUpdatedAddressFallback(final Location location)
    {
        final Geocoder geocoder = new Geocoder(application.getApplicationContext(), Locale.getDefault());

        return Observable.create(new Observable.OnSubscribe<Address>()
        {
            @Override
            public void call(Subscriber<? super Address> subscriber)
            {
                try {

                    List<com.doctoror.geocoder.Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 5, true);
                    com.doctoror.geocoder.Address fallBackAddress = !addresses.isEmpty() ? addresses.get(0) : null;

                    Address address = null;

                    if (fallBackAddress != null) {
                        address = new Address(Locale.getDefault());
                        address.setAddressLine(0, fallBackAddress.getStreetAddress());
                        address.setCountryName(fallBackAddress.getCountry());
                        address.setLocality(fallBackAddress.getLocality());
                        address.setLongitude(fallBackAddress.getLocation().longitude);
                        address.setLatitude(fallBackAddress.getLocation().latitude);
                    }

                    subscriber.onNext(address);
                    subscriber.onCompleted();

                } catch (Geocoder.LimitExceededException e) {
                    subscriber.onError(e);
                } catch (IOException e) {
                    e.printStackTrace();
                    subscriber.onError(e);
                }
            }
        });
    }
    
    /*public Observable<Address> getAddressFromLocationRaw(final Location location) {
        
        final Geocoder geocoder = new Geocoder(application.getApplicationContext());
      
        return Observable.create(new Observable.OnSubscribe<Address>()
        {
            @Override
            public void call(Subscriber<? super Address> subscriber)
            {
                try {
                    
                    List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 5);
                    
                    Address address = addresses != null && !addresses.isEmpty() ? addresses.get(0) : null;;
                    subscriber.onNext(address);
                    subscriber.onCompleted();
                } catch (IOException e) {
                    subscriber.onError(e);
                }
            }
        });
    }*/

    public Observable<LocationSettingsResult> requestLocationUpdates()
    {
        final ReactiveLocationProvider locationProvider = new ReactiveLocationProvider(application.getApplicationContext());

        final LocationRequest locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setNumUpdates(5)
                .setInterval(100);

        return locationProvider.checkLocationSettings(
                new LocationSettingsRequest.Builder()
                        .addLocationRequest(locationRequest)
                        .setAlwaysShow(true)  //Refrence: http://stackoverflow.com/questions/29824408/google-play-services-locationservices-api-new-option-never
                        .build()
        );
    }
    
    /*public void removeLocationUpdates()
    {
        final ReactiveLocationProvider locationProvider = new ReactiveLocationProvider(application.getApplicationContext());
        locationProvider.removeLocationUpdates()
    }*/

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
        if (type == TradeType.ONLINE_BUY) {
            url = "buy-bitcoins-online";
        } else {
            url = "sell-bitcoins-online";
        }

        String countryNameFix = countryName.replace(" ", "-");
        if (paymentMethod.equals("all")) {
            return localBitcoins.searchOnlineAds(url, countryCode, countryNameFix)
                    .map(new ResponseToAds())
                    .flatMap(new Func1<List<Advertisement>, Observable<List<Advertisement>>>()
                    {
                        @Override
                        public Observable<List<Advertisement>> call(List<Advertisement> advertisements)
                        {
                            if (advertisements == null)
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
                            if (advertisements == null)
                                advertisements = Collections.emptyList();

                            return Observable.just(advertisements);
                        }
                    });
        }
    }

    public Observable<List<Advertisement>> getAdvertisementsInPlace(Place place, TradeType type)
    {
        String url;
        if (type == TradeType.LOCAL_BUY) {
            url = place.buy_local_url.replace("https://localbitcoins.com/", "");
        } else {
            url = place.sell_local_url.replace("https://localbitcoins.com/", "");
        }

        String[] split = url.split("/");
        return localBitcoins.searchAdsByPlace(split[0], split[1], split[2])
                .map(new ResponseToAds())
                .flatMap(new Func1<List<Advertisement>, Observable<List<Advertisement>>>()
                {
                    @Override
                    public Observable<List<Advertisement>> call(List<Advertisement> advertisements)
                    {
                        Collections.sort(advertisements, new AdvertisementNameComparator());
                        return Observable.just(advertisements);
                    }
                });
    }

    private class AdvertisementNameComparator implements Comparator<Advertisement>
    {
        @Override
        public int compare(Advertisement o1, Advertisement o2)
        {
            return o1.distance.toLowerCase().compareTo(o2.distance.toLowerCase());
        }
    }

    /*public Observable<List<Address>> geoDecodeLocation(final Location location)
    {
        final Geocoder geocoder = new Geocoder(application, Locale.getDefault());
        
        return Observable.create(new Observable.OnSubscribe<List<Address>>()
        {
            @Override
            public void call(Subscriber<? super List<Address>> subscriber)
            {
                try {
                    subscriber.onNext(geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 5, true));
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                } 
            }
        });
    }*/
}
