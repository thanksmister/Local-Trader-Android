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

import android.content.Context;
import android.location.Address;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
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
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Func1;
import rx.subscriptions.Subscriptions;
import timber.log.Timber;

public class GeoLocationService {
    public final static int MAX_ADDRESSES = 5;

    private static final long LOCATION_MIN_TIME_BETWEEN_UPDATES = 0L;
    private static final float LOCATION_MIN_DISTANCE_BETWEEN_UPDATES = 0f;

    private Observable<Location> locationObservable;

    private BaseApplication application;
    private LocalBitcoins localBitcoins;
    private LocationManager locationManager;

    @Inject
    public GeoLocationService(BaseApplication application, LocalBitcoins localBitcoins) {
        this.application = application;
        this.localBitcoins = localBitcoins;
        this.locationManager = (LocationManager) application.getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        this.locationObservable = createLocationObservable();
    }

    private Observable<Location> createLocationObservable() {
        return Observable.create(new Observable.OnSubscribe<Location>() {
            @Override
            public void call(final Subscriber<? super Location> subscriber) {
                Timber.d("Starting Location Services.");
                Criteria locationCriteria = new Criteria();
                locationCriteria.setSpeedAccuracy(Criteria.ACCURACY_HIGH);

                final LocationListener locationListener = new LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) {
                        subscriber.onNext(location);
                        Timber.i("New GPS Location - Accuracy=%d, Provider=%s, Speed=%d",
                                (int) (location.getAccuracy() + 0.5f), location.getProvider(), (int) (location.getSpeed() + 0.5f));
                    }

                    @Override
                    public void onStatusChanged(String provider, int status, Bundle extras) {
                    }

                    @Override
                    public void onProviderEnabled(String provider) {
                    }

                    @Override
                    public void onProviderDisabled(String provider) {
                    }
                };

                try {
                    locationManager.requestLocationUpdates(LOCATION_MIN_TIME_BETWEEN_UPDATES, LOCATION_MIN_DISTANCE_BETWEEN_UPDATES,
                            locationCriteria, locationListener, Looper.getMainLooper());
                } catch (SecurityException e) {
                    Timber.e("Location security exception: " + e.getMessage());
                }

                // On Unsubscribe
                subscriber.add(Subscriptions.create(new Action0() {
                    @Override
                    public void call() {
                        Timber.d("Stopping Location Services.");
                        locationManager.removeUpdates(locationListener);
                    }
                }));
            }
        }).publish().refCount();
    }

    /**
     * @return Observable that observes on UI Thread.
     */
    public Observable<Location> getLocationObservable() {
        return locationObservable.observeOn(AndroidSchedulers.mainThread());
    }

    public Observable<Address> getAddressFromLocation(Location location) {
        final ReactiveLocationProvider locationProvider = new ReactiveLocationProvider(application.getApplicationContext());

        return locationProvider
                .getReverseGeocodeObservable(location.getLatitude(), location.getLongitude(), 5)
                .map(new Func1<List<Address>, Address>() {
                    @Override
                    public Address call(List<Address> addresses) {
                        return addresses != null && !addresses.isEmpty() ? addresses.get(0) : null;
                    }
                });
    }

    public Observable<List<Advertisement>> getLocalAdvertisements(final double latitude, final double longitude, final TradeType tradeType) {
        return localBitcoins.getPlaces(latitude, longitude)
                .map(new ResponseToPlace())
                .flatMap(new Func1<Place, Observable<List<Advertisement>>>() {
                    @Override
                    public Observable<List<Advertisement>> call(Place place) {
                        return getAdvertisementsInPlace(place, tradeType);
                    }
                });
    }

   
    /**
     *  Get a list of online advertisements using currency, payment method, country code and country name.
     * @param type
     * @param countryName
     * @param countryCode
     * @param currency
     * @param paymentMethod
     * @return
     */
    public Observable<List<Advertisement>> getOnlineAdvertisements(@NonNull final TradeType type, 
                                                                   @NonNull final String countryName,
                                                                   @NonNull final String countryCode,
                                                                   @NonNull final String currency,
                                                                   @NonNull final String paymentMethod) {
        String url;
        if (type == TradeType.ONLINE_BUY) {
            url = "buy-bitcoins-online";
        } else {
            url = "sell-bitcoins-online";
        }
        
        if(!countryName.toLowerCase().equals("any") && paymentMethod.toLowerCase().equals("all")) {
            String countryNameFix = countryName.replace(" ", "-");
            return localBitcoins.searchOnlineAds(url, countryCode, countryNameFix)
                    .map(new ResponseToAds())
                    .flatMap(new Func1<List<Advertisement>, Observable<List<Advertisement>>>() {
                        @Override
                        public Observable<List<Advertisement>> call(List<Advertisement> advertisements) {
                            if (advertisements == null)
                                advertisements = Collections.emptyList();

                            return Observable.just(advertisements);
                        }
                    });
        } else if (!countryName.toLowerCase().equals("any") && !paymentMethod.toLowerCase().equals("all")) {
            String countryNameFix = countryName.replace(" ", "-");
            return localBitcoins.searchOnlineAds(url, countryCode, countryNameFix, paymentMethod)
                    .map(new ResponseToAds())
                    .flatMap(new Func1<List<Advertisement>, Observable<List<Advertisement>>>() {
                        @Override
                        public Observable<List<Advertisement>> call(List<Advertisement> advertisements) {
                            if (advertisements == null)
                                advertisements = Collections.emptyList();

                            return Observable.just(advertisements);
                        }
                    });
        } else if (paymentMethod.toLowerCase().equals("all") && !currency.toLowerCase().equals("any")) {
            return localBitcoins.searchOnlineAdsCurrency(url, currency)
                    .map(new ResponseToAds())
                    .flatMap(new Func1<List<Advertisement>, Observable<List<Advertisement>>>() {
                        @Override
                        public Observable<List<Advertisement>> call(List<Advertisement> advertisements) {
                            if (advertisements == null)
                                advertisements = Collections.emptyList();

                            return Observable.just(advertisements);
                        }
                    });
        } else if (!paymentMethod.toLowerCase().equals("all") && currency.toLowerCase().equals("any")) {
            return localBitcoins.searchOnlineAdsPayment(url, paymentMethod)
                    .map(new ResponseToAds())
                    .flatMap(new Func1<List<Advertisement>, Observable<List<Advertisement>>>() {
                        @Override
                        public Observable<List<Advertisement>> call(List<Advertisement> advertisements) {
                            if (advertisements == null)
                                advertisements = Collections.emptyList();

                            return Observable.just(advertisements);
                        }
                    });
        } else if (!paymentMethod.toLowerCase().equals("all") && !currency.toLowerCase().equals("any")) {
            return localBitcoins.searchOnlineAdsCurrencyPayment(url, currency, paymentMethod)
                    .map(new ResponseToAds())
                    .flatMap(new Func1<List<Advertisement>, Observable<List<Advertisement>>>() {
                        @Override
                        public Observable<List<Advertisement>> call(List<Advertisement> advertisements) {
                            if (advertisements == null)
                                advertisements = Collections.emptyList();

                            return Observable.just(advertisements);
                        }
                    });
        } else {
            return localBitcoins.searchOnlineAdsAll(url)
                    .map(new ResponseToAds())
                    .flatMap(new Func1<List<Advertisement>, Observable<List<Advertisement>>>() {
                        @Override
                        public Observable<List<Advertisement>> call(List<Advertisement> advertisements) {
                            if (advertisements == null)
                                advertisements = Collections.emptyList();

                            return Observable.just(advertisements);
                        }
                    });
        }
    }

    @Deprecated
    public Observable<List<Advertisement>> getOnlineAdvertisements(@NonNull final String countryCode, @NonNull final String countryName,
                                                                   @NonNull final TradeType type, @NonNull final String paymentMethod) {
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
                    .flatMap(new Func1<List<Advertisement>, Observable<List<Advertisement>>>() {
                        @Override
                        public Observable<List<Advertisement>> call(List<Advertisement> advertisements) {
                            if (advertisements == null)
                                advertisements = Collections.emptyList();

                            return Observable.just(advertisements);
                        }
                    });
        } else {
            return localBitcoins.searchOnlineAds(url, countryCode, countryNameFix, paymentMethod)
                    .map(new ResponseToAds())
                    .flatMap(new Func1<List<Advertisement>, Observable<List<Advertisement>>>() {
                        @Override
                        public Observable<List<Advertisement>> call(List<Advertisement> advertisements) {
                            if (advertisements == null)
                                advertisements = Collections.emptyList();

                            return Observable.just(advertisements);
                        }
                    });
        }
    }

    public Observable<List<Advertisement>> getAdvertisementsInPlace(Place place, TradeType type) {
        String url = "";
        if (type == TradeType.LOCAL_BUY) {
            if(place.buy_local_url.contains("https://localbitcoins.com/")) {
                url = place.buy_local_url.replace("https://localbitcoins.com/", "");
            } else if (place.buy_local_url.contains("https://localbitcoins.net/")) {
                url = place.buy_local_url.replace("https://localbitcoins.net/", "");
            }  else {
                url = place.buy_local_url;
            }
        } else if(type == TradeType.LOCAL_SELL) {
            if(place.sell_local_url.contains("https://localbitcoins.com/")) {
                url = place.sell_local_url.replace("https://localbitcoins.com/", "");
            } else if (place.sell_local_url.contains("https://localbitcoins.net/")) {
                url = place.sell_local_url.replace("https://localbitcoins.net/", ""); 
            }  else {
                url = place.sell_local_url;
            }
        }

        String[] split = url.split("/");
        return localBitcoins.searchAdsByPlace(split[0], split[1], split[2])
                .map(new ResponseToAds())
                .flatMap(new Func1<List<Advertisement>, Observable<List<Advertisement>>>() {
                    @Override
                    public Observable<List<Advertisement>> call(List<Advertisement> advertisements) {
                        Collections.sort(advertisements, new AdvertisementNameComparator());
                        return Observable.just(advertisements);
                    }
                });
    }
    
    /**
     * Compares distance as a double value to list advertisements by shortest to longest distance from user
     */
    private class AdvertisementNameComparator implements Comparator<Advertisement> {
        @Override
        public int compare(Advertisement a1, Advertisement a2) {
            try {
                return Double.compare(Doubles.convertToDouble(a1.distance), Doubles.convertToDouble(a2.distance));
            } catch (Exception e) {
                return 0;
            }
        }
    }
}
