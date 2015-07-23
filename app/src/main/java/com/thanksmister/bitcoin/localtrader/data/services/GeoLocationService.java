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
    private static final String API_URL = "http://maps.googleapis.com";

    public final static String MODE_DRIVING = "driving";
    public final static int MAX_ADDRESSES = 5;

    private BaseApplication application;
    private LocalBitcoins localBitcoins;
    private GetDirectionsTask getDirectionsTask;

    private BehaviorSubject<Location> behaviorSubject;
    private Location location;
    private GoogleApiClient googleApiClient;
   
    static class Direction
    {
        List<Route> routes;
    }

    static class Route
    {
        String copyrights;
        List<Leg> legs;
    }

    static class Leg
    {
        Distance distance;
        Duration duration;
    }

    static class Distance
    {
        String text;
        int value;
    }

    static class Duration
    {
        String text;
        int value;
    }

    interface GoogleDirection
    {
        @GET("/maps/api/directions/json?sensor=false&units=metric")
        void direction(
                @Query("origin") String origin,
                @Query("destination") String destination,
                @Query("mode") String mode,
                Callback<Direction> callback
        );
    }

    @Inject
    public GeoLocationService(BaseApplication application, LocalBitcoins localBitcoins)
    {
        this.application = application;
        this.localBitcoins = localBitcoins;
       
        //setupLocationProvider();
    }
    
    public Observable<Address> getUpdatedLocation()
    {
        final ReactiveLocationProvider locationProvider = new ReactiveLocationProvider(application.getApplicationContext());
        
        final LocationRequest locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setNumUpdates(1)
                .setInterval(1000);

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
    
    public void getTravelTimeToMeetingPlace(double originLat, double originLon, double destinationLat, double destinationLon)
    {
        if (getDirectionsTask == null) {
            getDirectionsTask = new GetDirectionsTask();
            getDirectionsTask.execute(originLat, originLon, destinationLat, destinationLon);
        }
    }

    private class GetDirectionsTask extends AsyncTask<Double, Void, Boolean>
    {
        protected Boolean doInBackground(Double... args)
        {
            double originLat = args[0];
            double originLon = args[1];
            double destinationLat = args[2];
            double destinationLon = args[3];

            RestAdapter restAdapter = new RestAdapter.Builder()
                    .setLogLevel(RestAdapter.LogLevel.BASIC)
                    .setEndpoint(API_URL)
                    .build();

            Callback<Direction> callback = new Callback<Direction>()
            {
                @Override
                public void success(Direction direction, Response response)
                {
                    if (direction.routes != null && !direction.routes.isEmpty()) {
                        Route route = direction.routes.get(0);
                        if (route.legs != null && !route.legs.isEmpty()) {
                            Leg leg = route.legs.get(0);
                            //bus.post(new MapEvent(MapEvent.MapEventType.DIRECTIONS, leg.duration.text, leg.duration.value));
                        }
                    }

                    //stopgap until we figure out what to do with directions
                    //bus.post(new MapEvent(MapEvent.MapEventType.DIRECTIONS, "Forever", 1004));
//                   bus.post(new MapErrorEvent(context.getString(R.string.map_no_teachers_available_marker_text), MapErrorEvent.MapErrorType.DIRECTIONS));
                }

                @Override
                public void failure(RetrofitError cause)
                {
                    Response r = cause.getResponse();
                    if (r != null && r.getStatus() == 401) {
                        Timber.e("Direction Error: " + r.getReason());
                        // bus.post(new MapErrorEvent(context.getString(R.string.map_no_teachers_available_marker_text), MapErrorEvent.MapErrorType.DIRECTIONS));
                    }
                }
            };

            // Create an instance of our Google Directions interface
            GoogleDirection googleDirection = restAdapter.create(GoogleDirection.class);
            String origin = originLat + "," + originLon;
            String destination = destinationLat + "," + destinationLon;
            googleDirection.direction(origin, destination, MODE_DRIVING, callback);
            return true;
        }

        protected void onPostExecute(Boolean result)
        {
            Timber.d(result.toString());
            //getDirectionsTask = null;
        }
    }

    private void setupLocationProvider()
    {
        
        final LocationRequest locationRequest = LocationRequest.create()
                .setInterval(30000)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        /*googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(mConnectionCallbacks)
                .addOnConnectionFailedListener(mOnConnectionFailedListener)
                .build();
        */

        googleApiClient = new GoogleApiClient.Builder(application)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle bundle) {
                        if(behaviorSubject != null)
                            behaviorSubject.onNext(LocationServices.FusedLocationApi.getLastLocation(googleApiClient));

                        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, new LocationListener() {
                            @Override
                            public void onLocationChanged(Location location)
                            {
                                if (behaviorSubject != null)
                                    behaviorSubject.onNext(location);
                            }
                        });
                    }

                    @Override
                    public void onConnectionSuspended(int i)
                    {
                        if(behaviorSubject != null)
                            behaviorSubject.onCompleted();
                    }
                }).addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult connectionResult) {
                        Timber.e("Error: " + connectionResult.toString());
                        if(behaviorSubject != null)
                            behaviorSubject.onError(new Error("2"));
                    }
                }).build();
    }

    public void start()
    {
        if (!googleApiClient.isConnected() || !googleApiClient.isConnecting()) {
            googleApiClient.connect();
        } 
    }

    public void stop()
    {
        /*if (googleApiClient.isConnected())
            googleApiClient.disconnect();

        if(behaviorSubject != null)
            behaviorSubject.onCompleted();*/
    }

    private Observable<Location> getLocation()
    {
        return Observable.create(new Observable.OnSubscribe<Location>()
        {
            @Override
            public void call(Subscriber<? super Location> subscriber)
            {
                try {
                    subscriber.onNext(new Location("initProvider"));
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        });
    }

    public Observable<Location> subscribeToLocation()
    {
        if(behaviorSubject != null || location != null) {
            return Observable.just(location);
        }

        behaviorSubject = BehaviorSubject.create(new Location("initProvider"));
        return behaviorSubject
                .filter(new Func1<Location, Boolean>() {
                    @Override
                    public Boolean call(Location location)
                    {
                        return (location != null && !location.getProvider().equals("initProvider"));
                    }
                }).doOnNext(new Action1<Location>()
                {
                    @Override
                    public void call(Location results)
                    {
                        behaviorSubject = null;
                        location = results;
                    }
                }).doOnError(new Action1<Throwable>()
                {
                    @Override
                    public void call(Throwable throwable)
                    {
                        behaviorSubject = null;
                    }
                });
    }
    
    public boolean isGooglePlayServicesAvailable()
    {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(application.getApplicationContext());
        if(ConnectionResult.SUCCESS == resultCode) {
            return  true;
        }
        
        return false;
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

    public Observable<List<Advertisement>> getOnlineAdvertisements(final String countryCode, final String countryName, final TradeType type, final MethodItem paymentMethod)
    {
        String url;
        if(type == TradeType.ONLINE_BUY) {
            url = "buy-bitcoins-online";
        } else {
            url = "sell-bitcoins-online";
        }

        //Subscription subscription = advertisementPublishSubject.subscribe(observer);
        if(paymentMethod.key().equals("all")) {
            return localBitcoins.searchOnlineAds(url, countryCode, countryName)
                    .map(new ResponseToAds())
                    .flatMap(new Func1<List<Advertisement>, Observable<List<Advertisement>>>()
                    {
                        @Override
                        public Observable<List<Advertisement>> call(List<Advertisement> advertisements)
                        {
                            return Observable.just(advertisements);
                        }
                    });
        } else {
            return localBitcoins.searchOnlineAds(url, countryCode, countryName, paymentMethod.key())
                    .map(new ResponseToAds())
                    .flatMap(new Func1<List<Advertisement>, Observable<List<Advertisement>>>()
                    {
                        @Override
                        public Observable<List<Advertisement>> call(List<Advertisement> advertisements)
                        {
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

    /*public Observable<List<Address>> geoGetLocationFromName(final String locationName)
    {
        if(Strings.isBlank(locationName) || locationName.length() < 3) {
            return Observable.empty();
        }
        
        final Geocoder geocoder = new Geocoder(application, Locale.getDefault());
        
        return Observable.create(new Observable.OnSubscribe<List<Address>>()
        {
            @Override
            public void call(Subscriber<? super List<Address>> subscriber)
            {
                try {
                    subscriber.onNext(geocoder.getFromLocationName(locationName, 5));
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                }  
            }
        });
    }*/
}
