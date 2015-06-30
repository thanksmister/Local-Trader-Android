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

import android.content.Intent;
import android.location.Address;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Spinner;
import android.widget.TextView;

import com.thanksmister.bitcoin.localtrader.BaseFragment;
import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.data.api.model.Method;
import com.thanksmister.bitcoin.localtrader.data.api.model.TradeType;
import com.thanksmister.bitcoin.localtrader.data.database.DbManager;
import com.thanksmister.bitcoin.localtrader.data.database.MethodItem;
import com.thanksmister.bitcoin.localtrader.data.services.GeoLocationService;
import com.thanksmister.bitcoin.localtrader.ui.MainActivity;
import com.thanksmister.bitcoin.localtrader.ui.misc.PredictAdapter;
import com.thanksmister.bitcoin.localtrader.ui.misc.MethodAdapter;
import com.thanksmister.bitcoin.localtrader.ui.misc.SpinnerAdapter;
import com.thanksmister.bitcoin.localtrader.utils.Strings;
import com.thanksmister.bitcoin.localtrader.utils.TradeUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func2;
import rx.subscriptions.CompositeSubscription;

import static rx.android.app.AppObservable.bindFragment;

public class SearchFragment extends BaseFragment
{
    private static final String EXTRA_ADDRESS = "com.thanksmister.extra.EXTRA_ADDRESS";
    private static final String EXTRA_TRADE_TYPE = "com.thanksmister.extra.EXTRA_TRADE_TYPE";

    @Inject
    DbManager dbManager;

    @Inject
    LocationManager locationManager;
    
    @InjectView(R.id.swipeLayout)
    SwipeRefreshLayout swipeLayout;

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

    @OnClick(R.id.clearButton)
    public void clearButtonClicked()
    {
        showSearchLayout();
        stopLocationCheck();
    }

    @OnClick(R.id.mapButton)
    public void mapButtonClicked()
    {
        showMapLayout();
        stopLocationCheck();
        currentLocation.setText("- - - -");
        startLocationCheck(); // get location
    }

    @OnClick(R.id.searchButton)
    public void searchButtonClicked()
    {
        showSearchResultsScreen();
    }
    
    private Address address;
    private PredictAdapter predictAdapter;
    private TradeType tradeType;
    private Subscription subscription;
    private Observable<List<MethodItem>> methodObservable;
    private Observable<List<Address>> geoLocationObservable;
    private Observable<List<Address>> geoDecodeObservable;
    private CompositeSubscription subscriptions = new CompositeSubscription();
    
    private class AddressData {
        public List<Address> addresses;
        public List<MethodItem> methods;
    }
    
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

        if(savedInstanceState != null) {
            if(savedInstanceState.containsKey(EXTRA_ADDRESS)) {
                address = savedInstanceState.getParcelable(EXTRA_ADDRESS);
                tradeType = (TradeType) savedInstanceState.getSerializable(EXTRA_TRADE_TYPE);
            }
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState)
    {
        super.onSaveInstanceState(outState);
        if(address != null) {
            outState.putParcelable(EXTRA_ADDRESS, address);
            outState.putSerializable(EXTRA_TRADE_TYPE, tradeType);
        }
    }

    @Override
    public void onResume()
    {
        if(address != null) {
            setAddress(address);
        } else {
            startLocationCheck();
        }

        super.onResume();
    }

    @Override
    public void onDetach()
    {
        ButterKnife.reset(this);

        if(subscriptions != null)
            subscriptions.unsubscribe();

        if(subscription != null)
            subscription.unsubscribe();

        super.onDetach();
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
        swipeLayout.setEnabled(false);
        
        typeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long arg3)
            {
                switch (position) {
                    case 0:
                        tradeType  = (locationSpinner.getSelectedItemPosition() == 0 ? TradeType.LOCAL_BUY : TradeType.ONLINE_BUY);
                        break;
                    case 1:
                        tradeType = (locationSpinner.getSelectedItemPosition() == 0 ? TradeType.LOCAL_SELL : TradeType.ONLINE_SELL);
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0)
            {
            }
        });

        locationSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long arg3) {
                switch (position) {
                    case 0:
                        tradeType = (typeSpinner.getSelectedItemPosition() == 0? TradeType.LOCAL_BUY:TradeType.LOCAL_SELL);
                        break;
                    case 1:
                        tradeType = (typeSpinner.getSelectedItemPosition() == 0? TradeType.ONLINE_BUY:TradeType.ONLINE_SELL);
                        break;
                }

                paymentMethodLayout.setVisibility(position == 0 ? View.GONE : View.VISIBLE);
            }
            @Override
            public void onNothingSelected(AdapterView<?> arg0){
            }
        });
        
        String[] locationTitles = getResources().getStringArray(R.array.list_location_spinner);
        List<String> locationList = new ArrayList<String>(Arrays.asList(locationTitles));

        SpinnerAdapter locationAdapter = new SpinnerAdapter(getActivity(), R.layout.spinner_layout, locationList);
        locationSpinner.setAdapter(locationAdapter);

        String[] typeTitles = getResources().getStringArray(R.array.list_types_spinner);
        List<String> typeList = new ArrayList<String>(Arrays.asList(typeTitles));

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

                stopLocationCheck();
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

        methodObservable = bindFragment(this, dbManager.methodQuery().cache());

        setupToolbar();
        showProgress();
    }

    public void showProgress()
    {
        progress.setVisibility(View.VISIBLE);
        content.setVisibility(View.GONE);
    }

    public void hideProgress()
    {
        progress.setVisibility(View.GONE);
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
    
    private void setMethods(List<MethodItem> methods)
    {
        MethodAdapter typeAdapter = new MethodAdapter(getActivity(), R.layout.spinner_layout, methods);
        paymentMethodSpinner.setAdapter(typeAdapter);
    }
    
    public void setAddress(Address address)
    {
        this.address = address;
        
        if (address != null)
            currentLocation.setText(TradeUtils.getAddressShort(address));
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
        if(!geoLocationService.isGooglePlayServicesAvailable()) {
            hideProgress();
            missingGooglePlayServices();
            getAddressFromLocation(null);
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

                    hideProgress();
                    
                    if (e.getMessage().equals("1")) {
                        showEnableLocation();
                        getAddressFromLocation(null);
                    } else {
                        handleError(new Throwable(getString(R.string.error_unable_load_address)), true);
                    }
                }

                @Override
                public void onNext(Location location) {
                    geoLocationService.stop();
                    getAddressFromLocation(location);
                }
            });
        } else {
            showEnableLocation();
        }
    }

    public void stopLocationCheck()
    {
        geoLocationService.stop();
    }

    private void showEnableLocation()
    {
        createAlert(getString(R.string.warning_no_location_active), false);
    }

    private void missingGooglePlayServices()
    {
        createAlert(getString(R.string.warning_no_google_play_services), true);
    }

    public void createAlert( String message, final boolean googlePlay)
    {
        Snackbar.make(getActivity().findViewById(R.id.coordinatorLayout), message, Snackbar.LENGTH_LONG)
                .setAction("Install", new View.OnClickListener() {
                    @Override
                    public void onClick(View view)
                    {
                        if(googlePlay) {
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

    public boolean hasLocationServices()
    {
        return (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER));
    }
    
    public void getAddressFromLocation(final Location location)
    {
        if (location == null) {

            subscriptions.add(methodObservable.subscribe(new Action1<List<MethodItem>>()
            {
                @Override
                public void call(List<MethodItem> methods)
                {
                    setMethods(methods);
                }
            }, new Action1<Throwable>()
            {
                @Override
                public void call(Throwable throwable)
                {
                    reportError(throwable);
                }
            }));
            
        } else {
            geoDecodeObservable = bindFragment(this, geoLocationService.geoDecodeLocation(location));
            subscriptions.add(Observable.combineLatest(methodObservable, geoDecodeObservable, new Func2<List<MethodItem>, List<Address>, AddressData>()
            {
                @Override
                public AddressData call(List<MethodItem> methods, List<Address> addresses)
                {
                    AddressData data = new AddressData();
                    data.methods = methods;
                    data.addresses = addresses;
                    return data;
                }
            }).subscribe(new Action1<AddressData>()
            {
                @Override
                public void call(AddressData data)
                {
                    setMethods(data.methods);

                    if (!data.addresses.isEmpty()) {
                        setAddress(data.addresses.get(0));
                    }
                }
            }, new Action1<Throwable>()
            {
                @Override
                public void call(Throwable throwable)
                {
                    handleError(new Throwable(getString(R.string.error_unable_load_address)), true);
                }
            }));
        }
    }
    
    private void doAddressLookup(String locationName)
    {
        geoLocationObservable = bindFragment(this, geoLocationService.geoGetLocationFromName(locationName));
        geoLocationObservable.subscribe(new Action1<List<Address>>() {
            @Override
            public void call(List<Address> addresses) {
                if (!addresses.isEmpty()) {
                    getEditLocationAdapter().replaceWith(addresses);
                }
            }
        });
    }
    
    private MethodItem getPaymentMethod()
    {
        int position = paymentMethodSpinner.getSelectedItemPosition();
        return (MethodItem) paymentMethodSpinner.getAdapter().getItem(position);
    }
    
    private void showSearchResultsScreen()
    {
        if(!geoLocationService.isGooglePlayServicesAvailable()) {
            missingGooglePlayServices();
            return;
        }
        
        MethodItem paymentMethod = getPaymentMethod();
        
        if(hasLocationServices()) {
            String methodCode = paymentMethod.code();
            Intent intent = SearchResultsActivity.createStartIntent(getActivity(), tradeType, address, methodCode);
            startActivity(intent);
        } else {
            showEnableLocation();
        }
    }
}
