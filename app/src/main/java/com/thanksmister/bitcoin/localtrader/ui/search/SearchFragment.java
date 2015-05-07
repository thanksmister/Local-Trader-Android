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

import android.app.Activity;
import android.content.Intent;
import android.location.Address;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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

import com.afollestad.materialdialogs.MaterialDialog;
import com.thanksmister.bitcoin.localtrader.BaseFragment;
import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.data.api.model.Method;
import com.thanksmister.bitcoin.localtrader.data.api.model.TradeType;
import com.thanksmister.bitcoin.localtrader.data.database.DbManager;
import com.thanksmister.bitcoin.localtrader.data.database.MethodItem;
import com.thanksmister.bitcoin.localtrader.data.services.GeoLocationService;
import com.thanksmister.bitcoin.localtrader.events.ConfirmationDialogEvent;
import com.thanksmister.bitcoin.localtrader.ui.MainActivity;
import com.thanksmister.bitcoin.localtrader.ui.misc.PredictAdapter;
import com.thanksmister.bitcoin.localtrader.ui.misc.MethodAdapter;
import com.thanksmister.bitcoin.localtrader.ui.misc.SpinnerAdapter;
import com.thanksmister.bitcoin.localtrader.utils.Strings;
import com.thanksmister.bitcoin.localtrader.utils.TradeUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func2;
import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;

import static rx.android.app.AppObservable.bindFragment;

public class SearchFragment extends BaseFragment
{
    private static final String ARG_SECTION_NUMBER = "section_number";
    private static final String EXTRA_ADDRESS = "com.thanksmister.extra.EXTRA_ADDRESS";
    private static final String EXTRA_TRADE_TYPE = "com.thanksmister.extra.EXTRA_TRADE_TYPE";

    @Inject
    DbManager dbManager;

    @Inject
    LocationManager locationManager;

    @Inject
    GeoLocationService geoLocationService;

    @InjectView(android.R.id.progress)
    View progress;

    @InjectView(android.R.id.content)
    View content;

    @InjectView(android.R.id.empty)
    View empty;

    @InjectView(R.id.emptyTextView)
    TextView emptyTextView;

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

    @OnClick(R.id.emptyRetryButton)
    public void emptyButtonClicked()
    {
        onResume();
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
    
    public static SearchFragment newInstance(int sectionNumber)
    {
        SearchFragment fragment = new SearchFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
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
            hideProgress();
        } else {
            startLocationCheck();
        }

        super.onResume();
    }

    @Override
    public void onAttach(Activity activity)
    {
        super.onAttach(activity);

        ((MainActivity) activity).onSectionAttached(getArguments().getInt(ARG_SECTION_NUMBER));
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

        editLocation.setOnItemClickListener((parent, view, position, id) -> {
            Address address = predictAdapter.getItem(position);
            showMapLayout();
            editLocation.setText("");

            stopLocationCheck();
            setAddress(address);
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

        predictAdapter = new PredictAdapter(getActivity(), Collections.emptyList());
        setEditLocationAdapter(predictAdapter);

        methodObservable = bindFragment(this, dbManager.methodQuery().cache());

        showProgress();
    }
    
    private void showError(String message)
    {
        progress.setVisibility(View.GONE);
        content.setVisibility(View.GONE);
        empty.setVisibility(View.VISIBLE);
        emptyTextView.setText(message);
    }

    private void showProgress()
    {
        empty.setVisibility(View.GONE);
        progress.setVisibility(View.VISIBLE);
        content.setVisibility(View.GONE);
    }

    private void hideProgress()
    {
        empty.setVisibility(View.GONE);
        progress.setVisibility(View.GONE);
        content.setVisibility(View.VISIBLE);
    }

    private PredictAdapter getEditLocationAdapter()
    {
        return predictAdapter;
    }

    private List<Method> filterPaymentMethods(List<Method> methods, String countryName, String countryCode)
    {
        Method method = new Method();
        method.code = "ALL";
        method.name = ("All in " + countryName);
        method.key = "all";
        methods.add(0, method);

        for (Method m : methods) {
            m.countryCode = countryCode;
            m.countryName = countryName;
        }

        return methods;
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
                    } else {
                        showError(e.getMessage());
                    }
                }

                @Override
                public void onNext(Location location) {
                    geoLocationService.stop();
                    getAddressFromLocation(location);
                }
            });
        } else {
            showEnableLocationDialog();
        }
    }

    public void stopLocationCheck()
    {
        geoLocationService.stop();
    }

    private void showEnableLocationDialog()
    {
        createAlert(getString(R.string.warning_no_location_services_title), getString(R.string.warning_no_location_active), false);
    }

    private void missingGooglePlayServices()
    {
        createAlert(getString(R.string.warning_no_google_play_services_title), getString(R.string.warning_no_google_play_services), true);
    }

    public void createAlert(String title, String message, final boolean googlePlay)
    {
        showError(getString(R.string.error_no_play_services));
        
        int positiveButton = (googlePlay)? R.string.button_install:R.string.button_enable;
        ConfirmationDialogEvent event = new ConfirmationDialogEvent(title, message, getString(positiveButton), getString(R.string.button_cancel), new Action0() {
            @Override
            public void call() {
                if(googlePlay) {
                    installGooglePlayServices();
                } else {
                    openLocationServices();
                }
            }
        });

        showConfirmationDialog(event);
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

    public void getAddressFromLocation(Location location)
    {
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
                
                hideProgress();
            }
        }, new Action1<Throwable>()
        {
            @Override
            public void call(Throwable throwable)
            {
                showError(getString(R.string.error_unable_load_address));
            }
        }));
    }

    public void showConfirmationDialog(ConfirmationDialogEvent event)
    {
        new MaterialDialog.Builder(getActivity())
                .callback(new MaterialDialog.SimpleCallback() {
                    @Override
                    public void onPositive(MaterialDialog materialDialog) {
                        event.action.call(); // call function 
                    }
                })
                .title(event.title)
                .content(event.message)
                .positiveText(event.positive)
                .negativeText(event.negative)
                .show();
    }

    public void doAddressLookup(String locationName)
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
    
    public void showSearchResultsScreen()
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
            showEnableLocationDialog();
        }
    }
}
