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

package com.thanksmister.bitcoin.localtrader.ui.search;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationSettingsStates;
import com.thanksmister.bitcoin.localtrader.BaseFragment;
import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.constants.Constants;
import com.thanksmister.bitcoin.localtrader.data.api.model.Method;
import com.thanksmister.bitcoin.localtrader.data.api.model.TradeType;
import com.thanksmister.bitcoin.localtrader.data.database.DbManager;
import com.thanksmister.bitcoin.localtrader.data.database.MethodItem;
import com.thanksmister.bitcoin.localtrader.data.services.DataService;
import com.thanksmister.bitcoin.localtrader.data.services.GeoLocationService;
import com.thanksmister.bitcoin.localtrader.events.AlertDialogEvent;
import com.thanksmister.bitcoin.localtrader.ui.MainActivity;
import com.thanksmister.bitcoin.localtrader.ui.components.MethodAdapter;
import com.thanksmister.bitcoin.localtrader.ui.components.PredictAdapter;
import com.thanksmister.bitcoin.localtrader.ui.components.SpinnerAdapter;
import com.thanksmister.bitcoin.localtrader.utils.Strings;
import com.thanksmister.bitcoin.localtrader.utils.TradeUtils;
import com.trello.rxlifecycle.FragmentEvent;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import timber.log.Timber;

public class SearchFragment extends BaseFragment
{
    private static final String EXTRA_ADDRESS = "com.thanksmister.extra.EXTRA_ADDRESS";
    private static final String EXTRA_TRADE_TYPE = "com.thanksmister.extra.EXTRA_TRADE_TYPE";
    private static final int REQUEST_CHECK_SETTINGS = 0;

    @Inject
    DbManager dbManager;

    @Inject
    DataService dataService;
    
    @Inject
    LocationManager locationManager;
    
    @Inject
    GeoLocationService geoLocationService;

    @InjectView(R.id.toolbar)
    Toolbar toolbar;

    @InjectView(R.id.searchProgress)
    View progress;

    @InjectView(R.id.searchContent)
    View content;

    @InjectView(R.id.currentLocation)
    TextView currentLocation;

    @InjectView(R.id.mapLayout)
    View mapLayout;

    @InjectView(R.id.searchLayout)
    View searchLayout;

    @InjectView(R.id.editLocation)
    AutoCompleteTextView editLocation;

    @InjectView(R.id.locationSpinner)
    Spinner locationSpinner;

    @InjectView(R.id.typeSpinner)
    Spinner typeSpinner;

    @InjectView(R.id.paymentMethodSpinner)
    Spinner paymentMethodSpinner;

    @InjectView(R.id.paymentMethodLayout)
    View paymentMethodLayout;

    @InjectView(R.id.searchButton)
    Button searchButton;

    @InjectView(R.id.clearButton)
    ImageButton clearButton;

    @OnClick(R.id.clearButton)
    public void clearButtonClicked()
    {
        showSearchLayout();
    }

    @OnClick(R.id.mapButton)
    public void mapButtonClicked()
    {
        showMapLayout();
        currentLocation.setText("- - - -");
        startLocationCheck();
    }

    @OnClick(R.id.searchButton)
    public void searchButtonClicked()
    {
        showSearchResultsScreen();
    }

    private Address address;
    private PredictAdapter predictAdapter;
    private TradeType tradeType = TradeType.NONE;
    
    private Handler handler;
    
    public static SearchFragment newInstance()
    {
        return new SearchFragment();
    }

    public SearchFragment()
    {
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        handler = new Handler();

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(EXTRA_ADDRESS)) {
                address = savedInstanceState.getParcelable(EXTRA_ADDRESS);
                tradeType = (TradeType) savedInstanceState.getSerializable(EXTRA_TRADE_TYPE);
            }
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState)
    {
        super.onSaveInstanceState(outState);

        if (address != null) {
            outState.putParcelable(EXTRA_ADDRESS, address);
            outState.putSerializable(EXTRA_TRADE_TYPE, tradeType);
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();
        
        if(!checkPermissions()){
            showRequestPermissionsDialog();
            return;
        }

        subscribeData();
        updateData();
        
        if (address != null) {
            setAddress(address);
        } else {
            delayLocationCheck();
        }
    }

    @Override
    public void onDetach()
    {
        super.onDetach();
        
        ButterKnife.reset(this);

        handler.removeCallbacks(locationRunnable);
        
        //http://stackoverflow.com/questions/15207305/getting-the-error-java-lang-illegalstateexception-activity-has-been-destroyed
        try {
            Field childFragmentManager = Fragment.class.getDeclaredField("mChildFragmentManager");
            childFragmentManager.setAccessible(true);
            childFragmentManager.set(this, null);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.view_search, container, false);
    }

    @Override
    public void onViewCreated(View fragmentView, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(fragmentView, savedInstanceState);

        // hack for not being able to have the NestedScrollView match width height of container
        //swipeLayout.setEnabled(false);

        typeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long arg3)
            {
                switch (position) {
                    case 0:
                        tradeType = (locationSpinner.getSelectedItemPosition() == 0 ? TradeType.LOCAL_BUY : TradeType.ONLINE_BUY);
                        break;
                    case 1:
                        tradeType = (locationSpinner.getSelectedItemPosition() == 0 ? TradeType.LOCAL_SELL : TradeType.ONLINE_SELL);
                        break;
                    default:
                        tradeType = TradeType.NONE;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0)
            {
            }
        });

        locationSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long arg3)
            {
                switch (position) {
                    case 0:
                        tradeType = (typeSpinner.getSelectedItemPosition() == 0 ? TradeType.LOCAL_BUY : TradeType.LOCAL_SELL);
                        break;
                    case 1:
                        tradeType = (typeSpinner.getSelectedItemPosition() == 0 ? TradeType.ONLINE_BUY : TradeType.ONLINE_SELL);
                        break;
                    default:
                        tradeType = TradeType.NONE;
                }

                paymentMethodLayout.setVisibility(position == 0 ? View.GONE : View.VISIBLE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0)
            {
            }
        });

        String[] locationTitles = getResources().getStringArray(R.array.list_location_spinner);
        List<String> locationList = new ArrayList<>(Arrays.asList(locationTitles));

        SpinnerAdapter locationAdapter = new SpinnerAdapter(getActivity(), R.layout.spinner_layout, locationList);
        locationSpinner.setAdapter(locationAdapter);

        String[] typeTitles = getResources().getStringArray(R.array.list_types_spinner);
        List<String> typeList = new ArrayList<>(Arrays.asList(typeTitles));

        SpinnerAdapter typeAdapter = new SpinnerAdapter(getActivity(), R.layout.spinner_layout, typeList);
        typeSpinner.setAdapter(typeAdapter);

        editLocation.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l)
            {
                Address address = predictAdapter.getItem(i);
                showMapLayout();
                editLocation.setText("");
                setAddress(address);
            }
        });

        editLocation.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3)
            {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3)
            {
                if (!Strings.isBlank(charSequence)) {
                    doAddressLookup(charSequence.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable editable)
            {
            }
        });

        predictAdapter = new PredictAdapter(getActivity(), new ArrayList<Address>());
        setEditLocationAdapter(predictAdapter);

        if(searchButton != null) {
            clearButton.setEnabled(false);
            searchButton.setEnabled(false); // not enabled until we have valid address
        }
        
        setupToolbar();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        final LocationSettingsStates states = LocationSettingsStates.fromIntent(data);//intent);

        switch (requestCode) {
            
            case REQUEST_CHECK_SETTINGS:
                //Refrence: https://developers.google.com/android/reference/com/google/android/gms/location/SettingsApi
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        // All required changes were successfully made
                        Timber.d("User enabled location");
                        break;
                    case Activity.RESULT_CANCELED:
                        // The user was asked to change settings, but chose not to
                        Timber.d("User Cancelled enabling location");
                        toast("Search canceled...");
                        ((MainActivity) getActivity()).setContentFragment(MainActivity.DRAWER_DASHBOARD);
                        break;
                    default:
                        break;
                }
                break;
        }
        
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults)
    {
        switch (requestCode) {
            case Constants.REQUEST_PERMISSIONS: {
                if (grantResults.length > 0) {
                    boolean permissionsDenied = false;
                    for (int permission : grantResults) {
                        if(permission != PackageManager.PERMISSION_GRANTED) {
                            permissionsDenied = true;
                            break;
                        }
                    }

                    if(permissionsDenied) {
                        toast("Search canceled...");
                        ((MainActivity) getActivity()).setContentFragment(MainActivity.DRAWER_DASHBOARD);
                    } else {
                        onResume(); // load our stuff
                    }
                }
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    public boolean checkPermissions()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    || ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    public void showRequestPermissionsDialog()
    {
        showAlertDialog(new AlertDialogEvent(getString(R.string.alert_permission_required), getString(R.string.require_location_permission)), new Action0() {
            @Override
            public void call() {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, Constants.REQUEST_PERMISSIONS);
            }
        });
    }
    
    public void onRefresh()
    {
        subscribeData();
        updateData();
        
        if (address != null) {
            setAddress(address);
        } else {
            delayLocationCheck();
        }
    }

    private void delayLocationCheck()
    {
        handler.removeCallbacks(locationRunnable);
        handler.postDelayed(locationRunnable, 500);
    }

    private Runnable locationRunnable = new Runnable()
    {
        @Override
        public void run()
        {
            startLocationCheck();
        }
    };


    public void hideProgress()
    {
        if(progress != null)
            progress.setVisibility(View.GONE);
        
        if(content != null)
            content.setVisibility(View.VISIBLE);
    }

    private void setupToolbar()
    {
        ((MainActivity) getActivity()).setSupportActionBar(toolbar);

        // Show menu icon
        final ActionBar ab = ((MainActivity) getActivity()).getSupportActionBar();
        ab.setHomeAsUpIndicator(R.drawable.ic_action_navigation_menu);
        ab.setTitle(getString(R.string.view_title_buy_sell));
        ab.setDisplayHomeAsUpEnabled(true);
    }

    private PredictAdapter getEditLocationAdapter()
    {
        return predictAdapter;
    }
    
    private void subscribeData()
    {
        dbManager.methodQuery().cache()
                .doOnUnsubscribe(new Action0()
                {
                    @Override
                    public void call()
                    {
                        Timber.i("Method subscription safely unsubscribed");
                    }
                })
                .compose(this.<List<MethodItem>>bindUntilEvent(FragmentEvent.PAUSE))
                .subscribe(new Action1<List<MethodItem>>()
                {
                    @Override
                    public void call(List<MethodItem> methodItems)
                    {
                        setMethods(methodItems);
                    }
                }, new Action1<Throwable>()
                {
                    @Override
                    public void call(Throwable throwable)
                    {
                        handleError(new Throwable("Unable to load online payment methods."));
                    }
                });
    }

    private void updateMethods(List<Method> methods)
    {
        dbManager.updateMethods(methods);
    }
    
    private void setMethods(List<MethodItem> methods)
    {
        MethodAdapter typeAdapter = new MethodAdapter(getActivity(), R.layout.spinner_layout, methods);
        paymentMethodSpinner.setAdapter(typeAdapter);
        toggleSearchButton();
    }

    public void setAddress(Address address)
    {
        if(address == null) {
            snackError(getString(R.string.error_unable_load_address));
            return;
        }
        
        this.address = address;
        
        if(currentLocation != null)
            currentLocation.setText(TradeUtils.getAddressShort(address));

        toggleSearchButton();
    }
    
    private void toggleSearchButton()
    {
        if(searchButton == null)
            return;
        
        clearButton.setEnabled(getPaymentMethod() != null && address != null);
        searchButton.setEnabled(getPaymentMethod() != null && address != null);
    }

    private void disableSearchButton()
    {
        if(searchButton == null)
            return;

        clearButton.setEnabled(false);
        searchButton.setEnabled(false);
    }

    protected void showSearchLayout()
    {
        mapLayout.startAnimation(AnimationUtils.loadAnimation(getActivity(), android.R.anim.slide_out_right));
        searchLayout.startAnimation(AnimationUtils.loadAnimation(getActivity(), android.R.anim.slide_in_left));
        mapLayout.setVisibility(View.GONE);
        searchLayout.setVisibility(View.VISIBLE);
    }

    protected void showMapLayout()
    {
        mapLayout.startAnimation(AnimationUtils.loadAnimation(getActivity(), android.R.anim.slide_in_left));
        searchLayout.startAnimation(AnimationUtils.loadAnimation(getActivity(), android.R.anim.slide_out_right));
        mapLayout.setVisibility(View.VISIBLE);
        searchLayout.setVisibility(View.GONE);
    }

    protected void setEditLocationAdapter(PredictAdapter adapter)
    {
        if (editLocation != null)
            editLocation.setAdapter(adapter);
    }

    public void startLocationCheck()
    {
        Timber.d("startLocationCheck");
        if (!geoLocationService.isGooglePlayServicesAvailable()) {
            hideProgress();
            showGooglePlayServicesError();
            return;
        }

        if (!isNetworkConnected()) {
            handleError(new Throwable(getString(R.string.error_no_internet)), true);
            return;
        }
        
        if (hasLocationServices()) {
            Timber.d("hasLocationServices");
            getCurrentLocation();
        } else {
            showNoLocationServicesWarning();
        }
    }
    
    private void getLastKnownLocation()
    {
        Timber.d("getLastKnownLocation");
        
        geoLocationService.getLastKnownLocation()
                .timeout(5, TimeUnit.SECONDS, Observable.<Address>just(null))
                .doOnUnsubscribe(new Action0()
                {
                    @Override
                    public void call()
                    {
                        Timber.i("Last Location subscription safely unsubscribed");
                    }
                })
                .compose(this.<Address>bindUntilEvent(FragmentEvent.PAUSE))
                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.newThread())
                .subscribe(new Action1<Address>()
                {
                    @Override
                    public void call(final Address address)
                    {
                            getActivity().runOnUiThread(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    if(address != null) {
                                        setAddress(address);
                                        hideProgress();
                                    } else {
                                        getCurrentLocation();
                                    }
                                }
                            });
                    }
                }, new Action1<Throwable>()
                {
                    @Override
                    public void call(final Throwable throwable)
                    {
                       
                            getActivity().runOnUiThread(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    reportError(throwable);
                                    getCurrentLocation(); // back up plan is to get current location
                                }
                            });
                        
                    }
                });
    }
    
    private void getCurrentLocation()
    {
        Timber.d("getCurrentLocation");
        
        geoLocationService.getUpdatedLocation()
                .doOnUnsubscribe(new Action0()
                {
                    @Override
                    public void call()
                    {
                        Timber.i("Current Location subscription safely unsubscribed");
                    }
                })
                .compose(this.<Location>bindUntilEvent(FragmentEvent.PAUSE))
                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.newThread())
                .subscribe(new Action1<Location>()
                {
                    @Override
                    public void call(final Location location)
                    {
                        
                            getActivity().runOnUiThread(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    if(location != null) {
                                        getAddressFromLocation(location);
                                    } else {
                                        handleError(new Throwable(getString(R.string.error_unable_load_address)), true);
                                    }
                                }
                            });
                        
                    }
                }, new Action1<Throwable>()
                {
                    @Override
                    public void call(final Throwable throwable)
                    {
                        
                            getActivity().runOnUiThread(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    hideProgress();
                                    reportError(throwable);
                                    handleError(new Throwable(getString(R.string.error_unable_load_address)), true);
                                }
                            });
                        
                    }
                });
    }

    private void getAddressFromLocation(final Location location)
    {
        Timber.d("getAddressFromLocation");

        geoLocationService.getAddressFromLocation(location)
                .timeout(20, TimeUnit.SECONDS, Observable.<Address>just(null))
                .doOnUnsubscribe(new Action0()
                {
                    @Override
                    public void call()
                    {
                        Timber.i("Get Address From Location subscription safely unsubscribed");
                    }
                })
                .compose(this.<Address>bindUntilEvent(FragmentEvent.PAUSE))
                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.newThread())
                .subscribe(new Action1<Address>()
                {
                    @Override
                    public void call(final Address address)
                    {
                        getActivity().runOnUiThread(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    if(address != null) {
                                        setAddress(address);
                                        hideProgress();
                                    } else {
                                        getAddressFromLocationFallback(location);
                                    }
                                }
                            });
                        
                    }
                }, new Action1<Throwable>()
                {
                    @Override
                    public void call(final Throwable throwable)
                    {
                        getActivity().runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                hideProgress();
                                reportError(throwable);
                                getAddressFromLocationFallback(location);
                            }
                        });
                        
                    }
                });
    }
    
    private void getAddressFromLocationFallback(final Location location)
    {
        Timber.d("getAddressFromLocationFallback Location: " + location.getLongitude());
        
        geoLocationService.getUpdatedAddressFallback(location)
                .doOnUnsubscribe(new Action0()
                {
                    @Override
                    public void call()
                    {
                        Timber.i("Get Address From Location Fallback subscription safely unsubscribed");
                    }
                })
                .compose(this.<Address>bindUntilEvent(FragmentEvent.PAUSE))
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Address>()
                {
                    @Override
                    public void call(Address address)
                    {
                        if (address != null) {
                            setAddress(address);
                            hideProgress();
                        } else {
                            handleError(new Throwable(getString(R.string.error_unable_load_address)), true);
                        }
                    }
                }, new Action1<Throwable>()
                {
                    @Override
                    public void call(Throwable throwable)
                    {
                        hideProgress();
                        reportError(throwable);
                        handleError(new Throwable(getString(R.string.error_unable_load_address)), true);
                    }
                });
    }
    
    private void updateData()
    {
        Timber.d("UpdateData");

        dataService.getMethods()
                .doOnUnsubscribe(new Action0()
                {
                    @Override
                    public void call()
                    {
                        Timber.i("Get Method subscription safely unsubscribed");
                    }
                })
                .compose(this.<List<Method>>bindUntilEvent(FragmentEvent.PAUSE))
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<List<Method>>()
                {
                    @Override
                    public void call(List<Method> methods)
                    {
                        if (!methods.isEmpty()) {
                            Method method = new Method();
                            method.code = "all";
                            method.name = "All";
                            methods.add(0, method);
                            updateMethods(methods);
                        }
                    }
                }, new Action1<Throwable>()
                {
                    @Override
                    public void call(Throwable throwable)
                    {
                        reportError(throwable);
                    }
                });
    }

    private void showNoLocationServicesWarning()
    {
        createAlert(getString(R.string.warning_no_location_services_title), getString(R.string.warning_no_location_active), false);
    }

    public void createAlert(String title, String message, final boolean googlePlay)
    {
        int positiveButton = (googlePlay) ? R.string.button_install : R.string.button_enable;
        new AlertDialog.Builder(getActivity())
                .setTitle(title)
                .setMessage(message)
                .setNegativeButton(R.string.button_cancel, null)
                .setPositiveButton(positiveButton, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i)
                    {
                        if (googlePlay) {
                            installGooglePlayServices();
                        } else {
                            openLocationServices();
                        }
                    }
                })
                .show();
    }

    private void openLocationServices()
    {
        Intent viewIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivity(viewIntent);
    }

    private void installGooglePlayServices()
    {
        final String appPackageName = "com.google.android.gms"; // getPackageName() from Context or Activity object
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
        } catch (android.content.ActivityNotFoundException anfe) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + appPackageName)));
        }
    }
    
    private boolean hasLocationServices()
    {
        return (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER));
    }

    private boolean isNetworkConnected()
    {
        ConnectivityManager cm = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        return networkInfo != null;
    }

    private void doAddressLookup(String locationName)
    {
        geoLocationService.geoGetLocationFromName(locationName)
                .doOnUnsubscribe(new Action0()
                {
                    @Override
                    public void call()
                    {
                        Timber.i("Get Method subscription safely unsubscribed");
                    }
                })
                .compose(this.<List<Address>>bindUntilEvent(FragmentEvent.PAUSE))
                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.newThread())
                .subscribe(new Action1<List<Address>>()
                {
                    @Override
                    public void call(final List<Address> addresses)
                    {
                        if (!addresses.isEmpty()) {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    getEditLocationAdapter().replaceWith(addresses);
                                }
                            });
                        }
                        
                    }
                }, new Action1<Throwable>()
                {
                    @Override
                    public void call(Throwable throwable)
                    {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                handleError(new Throwable(getString(R.string.error_unable_load_address)), true);
                            }
                        });
                    }
                });
    }

    private MethodItem getPaymentMethod()
    {
        try {
            return (MethodItem) paymentMethodSpinner.getSelectedItem();
        } catch (NullPointerException e) {
            Timber.e(e.getLocalizedMessage());
        }
        
        return null;
    }
    
    private void showSearchResultsScreen()
    {
        if(address == null) {
            showNoLocationServicesWarning();
            return;
        }
        
        if (!geoLocationService.isGooglePlayServicesAvailable()) {
            showGooglePlayServicesError();
            return;
        }

        if (hasLocationServices()) {
            MethodItem paymentMethod = getPaymentMethod();
            String methodCode = (paymentMethod != null) ? paymentMethod.code() : "all";
            Intent intent = SearchResultsActivity.createStartIntent(getActivity(), tradeType, address, methodCode);
            startActivity(intent);
        } else {
            showNoLocationServicesWarning();
        }
    }
    
    private void showGooglePlayServicesError()
    {
        int result = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getActivity());

        switch (result) {
            case ConnectionResult.SERVICE_MISSING:
                createAlert(getString(R.string.warning_no_google_play_services_title), getString(R.string.warning_no_google_play_services), true);
                break;
            case ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED:
                createAlert(getString(R.string.warning_no_google_play_services_title), getString(R.string.warning_no_location_active), true);
                break;
            case ConnectionResult.SERVICE_DISABLED:
                createAlert(getString(R.string.warning_no_google_play_services_title), getString(R.string.warning_disabled_google_play_services), false);
                break;
        }
    }
}
