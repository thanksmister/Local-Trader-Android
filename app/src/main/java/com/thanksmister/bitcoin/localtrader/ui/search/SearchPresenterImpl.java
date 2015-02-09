package com.thanksmister.bitcoin.localtrader.ui.search;

import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.provider.Settings;

import com.thanksmister.bitcoin.localtrader.BaseActivity;
import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.data.api.model.Method;
import com.thanksmister.bitcoin.localtrader.data.api.model.TradeType;
import com.thanksmister.bitcoin.localtrader.data.services.DataService;
import com.thanksmister.bitcoin.localtrader.data.services.GeoLocationService;
import com.thanksmister.bitcoin.localtrader.events.ConfirmationDialogEvent;
import com.thanksmister.bitcoin.localtrader.ui.searchresults.SearchResultsActivity;
import com.thanksmister.bitcoin.localtrader.utils.TradeUtils;

import java.util.List;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.functions.Action0;
import timber.log.Timber;

/**
 * Author: Michael Ritchie
 * Date: 12/31/14
 * Copyright 2013, ThanksMister LLC
 */
public class SearchPresenterImpl implements SearchPresenter
{
    private SearchView view;
    private GeoLocationService service;
    private DataService dataService;
    private Subscription subscription;
    private LocationManager locationManager;

    private Address address;
    private TradeType tradeType;
    private Method paymentMethod;

    public SearchPresenterImpl(SearchView view, LocationManager locationManager, DataService dataService, GeoLocationService service) 
    {
        this.view = view;
        this.service = service;
        this.dataService = dataService;
        this.locationManager = locationManager;
    }

    @Override
    public void resume()
    {
    }

    @Override
    public void pause()
    {
        if(subscription != null)
            subscription.unsubscribe();
    }

    @Override
    public void doAddressLookup(String locationName)
    {
        subscription = service.geoGetLocationFromName(locationName).subscribe(new Observer<List<Address>>() {
            @Override
            public void onCompleted() {
            }

            @Override
            public void onError(Throwable e) {
               Timber.e(e.getMessage());
                getView().showError("Unable to get current address.");
            }

            @Override
            public void onNext(List<Address> addresses) {
                if (addresses != null && !addresses.isEmpty()) {
                    getView().getEditLocationAdapter().replaceWith(addresses);
                }
            }
        });
    }

    public void getAddressFromLocation(Location location)
    {
        subscription = service.geoDecodeLocation(location).subscribe(new Observer<List<Address>>() {
            @Override
            public void onCompleted() {
            }

            @Override
            public void onError(Throwable e) {
                Timber.e(e.getMessage());
                getView().showError("Unable to get current address.");
            }

            @Override
            public void onNext(List<Address> addresses) {
                if (!addresses.isEmpty()) {
                    address = addresses.get(0);
                    getView().setAddress(address);
                    getPaymentMethods(address.getCountryName(), address.getCountryCode()); // get payment methods
                }
            }
        });
    }

    @Override
    public void startLocationCheck()
    {
        if(!service.isGooglePlayServicesAvailable()) {
            missingGooglePlayServices();
            return;
        }
        if (hasLocationServices()) {
            service.start();
            subscription = service.subscribeToLocation(new Observer<Location>() {
                @Override
                public void onCompleted() {
                }

                @Override
                public void onError(Throwable e) {
                    Timber.e(e.getMessage());
                    if (e.getMessage().equals("1")) {
                        showEnableLocationDialog();
                    }
                }

                @Override
                public void onNext(Location location) 
                {
                    service.stop();
                    getAddressFromLocation(location);
                }
            });
        } else {
            getView().hideProgress();
            showEnableLocationDialog();
        }
    }

    protected void getPaymentMethods(String countryName, String countryCode)
    {
        Observable<List<Method>> observable = dataService.getOnlineProviders(countryName, countryCode);
        subscription = observable.subscribe(new Observer<List<Method>>() {
            @Override
            public void onCompleted() {
               getView().hideProgress();
            }

            @Override
            public void onError(Throwable e) {
                Timber.e(e.getMessage());
                getView().hideProgress();
            }

            @Override
            public void onNext(List<Method> results){
                getView().setMethods(results);
                getView().hideProgress();
            }
        });
    }

    @Override
    public void stopLocationCheck()
    {
        service.stop();
    }

    @Override
    public void setTradeType(TradeType tradeType)
    {
        this.tradeType = tradeType;
    }

    @Override
    public void setPaymentMethod(Method paymentMethod)
    {
        this.paymentMethod = paymentMethod;
    }

    @Override
    public void setAddress(Address address)
    {
        this.address = address;
    }

    private void showEnableLocationDialog()
    {
        createAlert(getView().getContext().getString(R.string.warning_no_location_services_title), getView().getContext().getString(R.string.warning_no_location_active), false);
    }

    public boolean hasLocationServices()
    {
        return (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER));
    }

    @Override
    public void showSearchResultsScreen()
    {
        if(!service.isGooglePlayServicesAvailable()) {
            missingGooglePlayServices();
            return;
        }

        if(hasLocationServices()) {
            String methodCode = (paymentMethod == null)? null: paymentMethod.code;
            Intent intent = SearchResultsActivity.createStartIntent(getView().getContext(), tradeType, address, methodCode);
            intent.setClass(getView().getContext(), SearchResultsActivity.class);
            getView().getContext().startActivity(intent);
        } else {
            showEnableLocationDialog();
        }
    }

    public void createAlert(String title, String message, final boolean googlePlay)
    {
        getView().showError("No Google Play Services");
        int positiveButton = (googlePlay)?R.string.button_install:R.string.button_enable;
        Context context = getView().getContext();
        ConfirmationDialogEvent event = new ConfirmationDialogEvent(title, message, getView().getContext().getString(positiveButton), getView().getContext().getString(R.string.button_cancel), new Action0() {
            @Override
            public void call() {
                if(googlePlay) {
                    installGooglePlayServices();
                } else {
                    openLocationServices();
                }
            }
        });

        ((BaseActivity) context).showConfirmationDialog(event);
    }

    private void openLocationServices()
    {
        Intent viewIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        getView().getContext().startActivity(viewIntent);
    }

    private void installGooglePlayServices()
    {
        final String appPackageName = "com.google.android.gms"; // getPackageName() from Context or Activity object
        try {
            getView().getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
        } catch (android.content.ActivityNotFoundException anfe) {
            getView().getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + appPackageName)));
        }
    }

    private void missingGooglePlayServices()
    {
        createAlert(getView().getContext().getString(R.string.warning_no_google_play_services_title), getView().getContext().getString(R.string.warning_no_google_play_services), true);
    }

    private SearchView getView()      
    {
        return view;
    }
}
