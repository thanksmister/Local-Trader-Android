package com.thanksmister.bitcoin.localtrader.ui.edit;

import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.provider.Settings;
import android.widget.Toast;

import com.thanksmister.bitcoin.localtrader.BaseActivity;
import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.data.DataModel;
import com.thanksmister.bitcoin.localtrader.data.api.model.Advertisement;
import com.thanksmister.bitcoin.localtrader.data.api.model.Currency;
import com.thanksmister.bitcoin.localtrader.data.api.model.Method;
import com.thanksmister.bitcoin.localtrader.data.api.model.TradeType;
import com.thanksmister.bitcoin.localtrader.data.services.DataService;
import com.thanksmister.bitcoin.localtrader.data.services.GeoLocationService;
import com.thanksmister.bitcoin.localtrader.events.ConfirmationDialogEvent;
import com.thanksmister.bitcoin.localtrader.events.ProgressDialogEvent;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.functions.Action0;
import timber.log.Timber;

/**
 * Author: Michael Ritchie
 * Date: 12/31/14
 * Copyright 2014, ThanksMister LLC
 */
public class EditPresenterImpl 
{
    
    private DataService service;
    private GeoLocationService geoLocationService;
    LocationManager locationManager;
    private Subscription subscription;
    private DataModel dataModel;

    /*public EditPresenterImpl( DataService dataService, GeoLocationService geoLocationService, LocationManager locationManager, DataModel dataModel) 
    {

        this.service = dataService;
        this.geoLocationService = geoLocationService;
        this.locationManager = locationManager;
        this.dataModel = dataModel;
    }

    @Override
    public void onDestroy()
    {
        if(subscription != null)
            subscription.unsubscribe();
    }

    @Override
    public void onResume(boolean create)
    {
        if(create) {
            startLocationCheck();
            getOnlineProviders();
            getCurrencies();
            setAdvertisement(new Advertisement());
        } else {
            getCurrencies();
            setAdvertisement(getAdvertisement());
        }
    }

    private void setAdvertisement(Advertisement advertisement)
    {
        getView().setTradeType(advertisement.trade_type);
        getView().setAdvertisement(advertisement);
    }

    public void getOnlineProviders()
    {
        Observable<List<Method>> methods = service.getOnlineProviders();
        subscription = methods.subscribe(new Observer<List<Method>>() {
            @Override
            public void onCompleted() {
            }

            @Override
            public void onError(Throwable e) {
                Timber.e("Error getting providers!");
                getView().setMethods(new ArrayList<Method>());
            }

            @Override
            public void onNext(List<Method> methods){
                getView().setMethods(methods);
            }
        });
    }

    public void getCurrencies()
    {
        subscription = service.getCurrencies(new Observer<List<Currency>>() {
            @Override
            public void onCompleted()
            {
            }

            @Override
            public void onError(Throwable e)
            {
            }

            @Override
            public void onNext(List<Currency> exchanges)
            {
                getView().setCurrencies(exchanges, getAdvertisement());
            }
        });
    }

    public void updateAdvertisement(Advertisement advertisement, Boolean create)
    {
        if(create) {

            ((BaseActivity) getContext()).showProgressDialog(new ProgressDialogEvent("Posting trade..."));
            
            subscription = service.createAdvertisement(new Observer<Advertisement>()
            {
                @Override
                public void onCompleted()
                {
                    ((BaseActivity) getContext()).hideProgressDialog();
                }

                @Override
                public void onError(Throwable throwable)
                {
                    Timber.e("Error creating: " + throwable.getMessage());
                    Toast.makeText(getView().getContext(), throwable.getMessage(), Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onNext(Advertisement advertisement)
                {
                    Timber.d("Advertisement created!");
                    String message = "New trade posted!";
                    Toast.makeText(getView().getContext(), message, Toast.LENGTH_SHORT).show();
                    returnAdvertisement(advertisement);

                }
            }, advertisement);
        } else {

            ((BaseActivity) getContext()).showProgressDialog(new ProgressDialogEvent("Saving changes..."));
            
            subscription = service.updateAdvertisement(new Observer<Advertisement>() {
                @Override
                public void onCompleted() {
                    ((BaseActivity) getContext()).hideProgressDialog();
                }

                @Override
                public void onError(Throwable throwable) {
                    Timber.e("Error updating: " + throwable.getMessage());
                    Toast.makeText(getView().getContext(), "Error updating advertisement.", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onNext(Advertisement advertisement) {
                    Toast.makeText(getView().getContext(), "Trade updated", Toast.LENGTH_SHORT).show();
                    returnAdvertisement(advertisement);
                }
            }, advertisement);
        }
    }

    private void returnAdvertisement(Advertisement updatedAdvertisement)
    {
        Intent intent = ((BaseActivity) getView().getContext()).getIntent();
        ((BaseActivity) getView().getContext()).setResult(EditActivity.RESULT_UPDATED, intent);
        ((BaseActivity) getView().getContext()).finish();
    }

    public void doAddressLookup(String locationName)
    {
        subscription = geoLocationService.geoGetLocationFromName(locationName).subscribe(new Observer<List<Address>>() {
            @Override
            public void onCompleted() {
                Timber.d("Address Lookup!");
            }

            @Override
            public void onError(Throwable e) {
                // TODO handle error
                if (e.getMessage() != null)
                    Timber.e(e.getMessage());
            }

            @Override
            public void onNext(List<Address> addresses) {
                if (getView() != null && addresses != null && !addresses.isEmpty()) {
                    getView().getEditLocationAdapter().replaceWith(addresses);
                }
            }
        });
    }

    @Override
    public void setAdvertisementType(TradeType tradeType)
    {
        getAdvertisement().trade_type = tradeType;
        setAdvertisement(getAdvertisement());
    }

    @Override
    public void validateChanges()
    {
        getView().validateChangesAndSend(getAdvertisement());
    }

    public void getAddressFromLocation(Location location)
    {
        subscription = geoLocationService.geoDecodeLocation(location).subscribe(new Observer<List<Address>>() {
            @Override
            public void onCompleted() {
                // all done address lookup
                Timber.d("Address From Location!");
            }

            @Override
            public void onError(Throwable e) {
                // TODO handle error
                Timber.e(e.getMessage());
            }

            @Override
            public void onNext(List<Address> addresses) {
                if (getView() != null && !addresses.isEmpty()) {
                    getView().setAddress(addresses.get(0));
                }
            }
        });
    }

    public void startLocationCheck()
    {
        if(!geoLocationService.isGooglePlayServicesAvailable()) {
            missingGooglePlayServices();
            return;
        }

        if(hasLocationServices()) {
            geoLocationService.start();
            subscription = geoLocationService.subscribeToLocation(new Observer<Location>() {
                @Override
                public void onCompleted(){
                }

                @Override
                public void onError(Throwable e) {
                    Timber.e(e.getMessage());
                    if (e.getMessage().equals("1")) {
                        showEnableLocationDialog();
                    }
                }

                @Override
                public void onNext(Location location) {
                    Timber.d("Location: " + location.toString());
                    geoLocationService.stop();
                    getAddressFromLocation(location);
                }
            });
        } else {
            showEnableLocationDialog();
        }
    }

    @Override
    public void cancelChanges(Boolean create)
    {
        String message = (create)? "New trade canceled":"Trade update canceled";
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        ((BaseActivity)getContext()).finish();
    }

    public void createAlert(String title, String message, final boolean googlePlay)
    {
        int positiveButton = (googlePlay)? R.string.button_install:R.string.button_enable;
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

    private void showEnableLocationDialog()
    {
        createAlert(getView().getContext().getString(R.string.warning_no_location_services_title), getView().getContext().getString(R.string.warning_no_location_active), false);
    }

    private void missingGooglePlayServices()
    {
        createAlert(getView().getContext().getString(R.string.warning_no_google_play_services_title), getView().getContext().getString(R.string.warning_no_google_play_services), true);
    }

    public boolean hasLocationServices()
    {
        return (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER));
    }

    public void stopLocationCheck()
    {
        geoLocationService.stop();
    }

    private Advertisement getAdvertisement()
    {
        if(dataModel.advertisement == null) {
            dataModel.advertisement = new Advertisement();
        }
        
        return dataModel.advertisement;
    }*/

   
}
