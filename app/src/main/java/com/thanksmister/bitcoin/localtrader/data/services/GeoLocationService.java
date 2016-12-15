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

import com.thanksmister.bitcoin.localtrader.BaseApplication;
import com.thanksmister.bitcoin.localtrader.data.api.LocalBitcoins;
import com.thanksmister.bitcoin.localtrader.data.api.model.Advertisement;
import com.thanksmister.bitcoin.localtrader.data.api.model.Place;
import com.thanksmister.bitcoin.localtrader.data.api.model.TradeType;
import com.thanksmister.bitcoin.localtrader.data.api.transforms.ResponseToAds;
import com.thanksmister.bitcoin.localtrader.data.api.transforms.ResponseToPlace;
import com.thanksmister.bitcoin.localtrader.utils.Doubles;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;

import pl.charmas.android.reactivelocation.ReactiveLocationProvider;
import rx.Observable;
import rx.functions.Func1;

public class GeoLocationService
{
    public final static int MAX_ADDRESSES = 5;

    private BaseApplication application;
    private LocalBitcoins localBitcoins;

    @Inject
    public GeoLocationService(BaseApplication application, LocalBitcoins localBitcoins)
    {
        this.application = application;
        this.localBitcoins = localBitcoins;
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

    /**
     * Compares distance as a double value to list advertisements by shortest to longest distance from user
     */
    private class AdvertisementNameComparator implements Comparator<Advertisement>
    {
        @Override
        public int compare(Advertisement a1, Advertisement a2)
        {
            try {
                return Double.compare(Doubles.convertToDouble(a1.distance), Doubles.convertToDouble(a2.distance)); 
            } catch (Exception e) {
                return 0;
            }
        }
    }
}
