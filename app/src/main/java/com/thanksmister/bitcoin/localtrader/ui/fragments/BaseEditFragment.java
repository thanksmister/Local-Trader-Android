/*
 * Copyright (c) 2017 ThanksMister LLC
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

package com.thanksmister.bitcoin.localtrader.ui.fragments;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Address;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.inputmethod.InputMethodManager;

import com.google.android.gms.location.LocationRequest;
import com.google.gson.Gson;
import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.constants.Constants;
import com.thanksmister.bitcoin.localtrader.network.api.model.Advertisement;
import com.thanksmister.bitcoin.localtrader.network.api.model.ExchangeCurrency;
import com.thanksmister.bitcoin.localtrader.data.database.CurrencyItem;
import com.thanksmister.bitcoin.localtrader.data.database.DbManager;
import com.thanksmister.bitcoin.localtrader.data.database.ExchangeCurrencyItem;
import com.thanksmister.bitcoin.localtrader.data.database.MethodItem;
import com.thanksmister.bitcoin.localtrader.network.services.ExchangeService;
import com.thanksmister.bitcoin.localtrader.network.services.SyncProvider;
import com.thanksmister.bitcoin.localtrader.events.AlertDialogEvent;
import com.thanksmister.bitcoin.localtrader.events.ProgressDialogEvent;
import com.thanksmister.bitcoin.localtrader.ui.BaseFragment;
import com.thanksmister.bitcoin.localtrader.ui.adapters.PredictAdapter;
import com.thanksmister.bitcoin.localtrader.utils.NetworkUtils;
import com.thanksmister.bitcoin.localtrader.utils.TradeUtils;
import com.trello.rxlifecycle.FragmentEvent;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.ButterKnife;
import dpreference.DPreference;
import pl.charmas.android.reactivelocation.ReactiveLocationProvider;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import timber.log.Timber;

import static com.thanksmister.bitcoin.localtrader.network.services.GeoLocationService.MAX_ADDRESSES;
import static com.thanksmister.bitcoin.localtrader.ui.fragments.SearchFragment.REQUEST_CHECK_SETTINGS;

public abstract class BaseEditFragment extends BaseFragment implements LoaderManager.LoaderCallbacks<Cursor> {
    
    public static final String ARG_PARAM_ADVERTISEMENT = "param_advertisement";
    public static final String ARG_PARAM_CREATE = "param_create";
    public static final int METHOD_LOADER_ID = 0;

    @Inject
    public DbManager dbManager;

    @Inject
    protected DPreference preference;
    
    @Inject
    public LocationManager locationManager;

    @Inject
    public ExchangeService exchangeService;

    public Advertisement editAdvertisement;
    public OnFragmentInteractionListener mListener;
    public PredictAdapter predictAdapter;
    private Subscription geoLocationSubscription;

    public interface OnFragmentInteractionListener {
        //void onAdvertisementValidated(Advertisement editAdvertisement);
    }
    
    protected abstract void setMethods(List<MethodItem> methods);
    protected abstract void showAddressError();
    protected abstract void displayAddress(Address address);
    protected abstract void onAddresses(List<Address> addresses);
    protected abstract void onCurrencies(List<ExchangeCurrency> currencies);
    protected abstract void setAdvertisementOnView(@NonNull Advertisement editAdvertisement);

    public abstract boolean validateChangesAndSave();
    
    public Advertisement getEditAdvertisement(){
        Advertisement advertisement = new Advertisement();
        String advertisementJson = preference.getString("editAdvertisement", null);
        if(!TextUtils.isEmpty(advertisementJson)) {
            advertisement = new Gson().fromJson(advertisementJson, Advertisement.class);
        }
        return advertisement;
    }
    
    public void setEditAdvertisement(Advertisement advertisement) {
        Timber.d("setEditAdvertisement: " + advertisement);
        String editString = new Gson().toJson(advertisement);
        preference.putString("editAdvertisement", editString);
    }

    public BaseEditFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if(isAdded()) {
            getLoaderManager().initLoader(METHOD_LOADER_ID, null, this);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        ButterKnife.reset(this);
        mListener = null;

        if (geoLocationSubscription != null) {
            geoLocationSubscription.unsubscribe();
            geoLocationSubscription = null;
        }
    }
   
    @Override
    public CursorLoader onCreateLoader(int id, Bundle args) {
        if (id == METHOD_LOADER_ID) {
            return new CursorLoader(getActivity(), SyncProvider.METHOD_TABLE_URI, null, null, null, null);
        }
        return null;
    }
    
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        switch (loader.getId()) {
            case METHOD_LOADER_ID:
                List<MethodItem> methodItems = MethodItem.getModelList(cursor);
                if(methodItems != null && !methodItems.isEmpty()) {
                    setMethods(TradeUtils.sortMethods(methodItems));
                }
                break;
            default:
                throw new Error("Incorrect loader Id");
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        Timber.d("onRequestPermissionsResult: " + requestCode);
        switch (requestCode) {
            case Constants.REQUEST_PERMISSIONS: {
                if (grantResults.length > 0) {
                    boolean permissionsDenied = false;
                    for (int permission : grantResults) {
                        if (permission != PackageManager.PERMISSION_GRANTED) {
                            permissionsDenied = true;
                            break;
                        }
                    }
                    if (permissionsDenied) {
                        toast((editAdvertisement == null) ? getString(R.string.toast_new_ad_canceled) : getString(R.string.toast_edit_ad_canceled));
                        getActivity().finish();
                    } else {
                        startLocationMonitoring();
                    }
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    public void checkLocationEnabled() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, Constants.REQUEST_PERMISSIONS);
                return;
            }
        }
        if (!NetworkUtils.hasLocationServices(locationManager)) {
            showNoLocationServicesWarning();
            return;
        }
        startLocationMonitoring();
    }
    
    // TODO async task
    public void startLocationMonitoring() {
        
        showProgressDialog(new ProgressDialogEvent(getString(R.string.dialog_progress_location)), true);
       
        LocationRequest request = LocationRequest.create() //standard GMS LocationRequest
                .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)
                .setNumUpdates(5)
                .setInterval(100);

        final ReactiveLocationProvider locationProvider = new ReactiveLocationProvider(getActivity());
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, Constants.REQUEST_PERMISSIONS);
            return;
        }
        geoLocationSubscription = locationProvider.getUpdatedLocation(request)
                .subscribe(new Action1<Location>() {
                    @Override
                    public void call(Location location) {
                        if (location != null) {
                            reverseLocationLookup(location);
                        } else {
                            hideProgressDialog();
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        if (isAdded()) {
                            Timber.e("Location manager error", throwable.getMessage());
                            getLocationFromLocationManager ();
                        }
                    }
                });
    }
    
    private void getLocationFromLocationManager() {
        locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_COARSE);
        try {
            LocationListener locationListener = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    if (location != null) {
                        locationManager.removeUpdates(this);
                        reverseLocationLookup(location);
                    } else {
                        hideProgressDialog();
                    }
                }
                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {
                    Timber.d("onStatusChanged: " + status);
                }
                @Override
                public void onProviderEnabled(String provider) {
                    Timber.d("onProviderEnabled");
                }
                @Override
                public void onProviderDisabled(String provider) {
                    Timber.d("onProviderDisabled");
                }
            };
            locationManager.requestLocationUpdates(locationManager.getBestProvider(criteria, true), 100, 10, locationListener);
        } catch (SecurityException e) {
            hideProgressDialog();
            Timber.e("Location manager could not use network provider", e);
            showAlertDialog(new AlertDialogEvent(getString(R.string.error_location_title), getString(R.string.error_location_message)));
        }
    }
    
    // TODO place in async task
    private void reverseLocationLookup(Location location) {
        final ReactiveLocationProvider locationProvider = new ReactiveLocationProvider(getActivity());
        locationProvider.getReverseGeocodeObservable(location.getLatitude(), location.getLongitude(), 1)
                .observeOn(Schedulers.io())
                .subscribeOn(AndroidSchedulers.mainThread())
                .compose(this.<List<Address>>bindUntilEvent(FragmentEvent.PAUSE))
                .doOnUnsubscribe(new Action0() {
                    @Override
                    public void call() {
                        Timber.i("Address lookup subscription safely unsubscribed");
                    }
                })
                .map(new Func1<List<Address>, Address>() {
                    @Override
                    public Address call(List<Address> addresses) {
                        Timber.d("addresses: " + addresses);
                        return (addresses != null && !addresses.isEmpty()) ? addresses.get(0) : null;
                    }
                }).subscribe(new Action1<Address>() {
                    @Override
                    public void call(final Address address) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                hideProgressDialog();
                                Timber.d("address: " + address);
                                if(address == null) {
                                    showAddressError();
                                } else {
                                    displayAddress(address);
                                }
                            }
                        });
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Timber.e(throwable.getMessage());
                        if(isAdded()) {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    showAlertDialog(new AlertDialogEvent(getString(R.string.error_address_lookup_title), getString(R.string.error_address_lookup_description)));
                                }
                            });
                        }
                    }
                });
    }
    
   public void doAddressLookup(String locationName) {
       final ReactiveLocationProvider locationProvider = new ReactiveLocationProvider(getContext().getApplicationContext());
       locationProvider.getGeocodeObservable(locationName, MAX_ADDRESSES)
                .doOnUnsubscribe(new Action0() {
                    @Override
                    public void call() {
                        Timber.i("Address lookup subscription safely unsubscribed");
                    }
                })
                .compose(this.<List<Address>>bindUntilEvent(FragmentEvent.PAUSE))
                .observeOn(Schedulers.computation())
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<List<Address>>() {
                    @Override
                    public void call(final List<Address> addresses) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (!addresses.isEmpty()) {
                                    onAddresses(addresses);
                                } 
                            }
                        });
                }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(final Throwable throwable) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                                    imm.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
                                } catch (NullPointerException e) {
                                    Timber.w("Error closing keyboard");
                                }
                                showAddressError();
                            }
                        });
                    }
                });
    }

    private void showNoLocationServicesWarning() {
        showAlertDialog(new AlertDialogEvent(getString(R.string.warning_no_location_services_title), getString(R.string.warning_no_location_active)), new Action0() {
            @Override
            public void call() {
                Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivityForResult(myIntent, REQUEST_CHECK_SETTINGS);
            }
        }, new Action0() {
            @Override
            public void call() {
                toast((editAdvertisement == null) ? getString(R.string.toast_new_ad_canceled) : getString(R.string.toast_edit_ad_canceled));
                getActivity().finish();
            }
        });
    }

    /**
     * Load the currencies
     */
    public void loadCurrencies() {
        dbManager.currencyQuery()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnUnsubscribe(new Action0() {
                    @Override
                    public void call() {
                        Timber.i("Currency subscription safely unsubscribed");
                    }
                })
                .compose(this.<List<CurrencyItem>>bindUntilEvent(FragmentEvent.PAUSE))
                .subscribe(new Action1<List<CurrencyItem>>() {
                    @Override
                    public void call(final List<CurrencyItem> currencies) {
                        if(currencies != null && !currencies.isEmpty()) {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    List<ExchangeCurrency> exchangeCurrencies = new ArrayList<ExchangeCurrency>();
                                    exchangeCurrencies = ExchangeCurrencyItem.getCurrencies(currencies);
                                    onCurrencies(exchangeCurrencies);
                                }
                            });
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Timber.e(throwable.getMessage());
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showAlertDialog(new AlertDialogEvent(getString(R.string.error_title), getString(R.string.error_unable_to_load_currencies)), new Action0() {
                                    @Override
                                    public void call() {
                                        getActivity().finish();
                                    }
                                });
                            }
                        });
                    }
                });
    }
}