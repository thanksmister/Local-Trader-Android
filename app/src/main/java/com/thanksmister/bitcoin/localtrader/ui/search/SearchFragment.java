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
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationRequest;
import com.squareup.otto.Bus;
import com.thanksmister.bitcoin.localtrader.BaseFragment;
import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.constants.Constants;
import com.thanksmister.bitcoin.localtrader.data.NetworkConnectionException;
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
import com.thanksmister.bitcoin.localtrader.utils.NetworkUtils;
import com.thanksmister.bitcoin.localtrader.utils.SearchUtils;
import com.trello.rxlifecycle.FragmentEvent;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import pl.charmas.android.reactivelocation.ReactiveLocationProvider;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import timber.log.Timber;

import static com.thanksmister.bitcoin.localtrader.data.services.GeoLocationService.MAX_ADDRESSES;

public class SearchFragment extends BaseFragment
{
    private static final String EXTRA_ADDRESS = "com.thanksmister.extra.EXTRA_ADDRESS";
    private static final String EXTRA_TRADE_TYPE = "com.thanksmister.extra.EXTRA_TRADE_TYPE";
    public static final int REQUEST_CHECK_SETTINGS = 0;
    public static final int REQUEST_GOOGLE_PLAY_SERVICES = 1972;

    @Inject
    DbManager dbManager;

    @Inject
    Bus bus;

    @Inject
    DataService dataService;

    @Inject
    GeoLocationService geoLocationService;

    @Inject
    SharedPreferences sharedPreferences;

    @Inject
    LocationManager locationManager;

    @InjectView(R.id.toolbar)
    Toolbar toolbar;

    @InjectView(R.id.editLocationText)
    AutoCompleteTextView editLocation;

    @InjectView(R.id.locationText)
    TextView locationText;

    @InjectView(R.id.editLocationLayout)
    View editLocationLayout;

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
        showEditTextLayout();
        editLocation.setText("");
    }

    @OnClick(R.id.searchButton)
    public void searchButtonClicked()
    {
        showSearchResultsScreen();
    }

    private PredictAdapter predictAdapter;
    private TradeType tradeType = TradeType.LOCAL_BUY;
    private Subscription geoSubscription;
    private Subscription dataServiceSubscription;
    private Subscription geoLocationSubscription;
  
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

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(EXTRA_ADDRESS)) {
                tradeType = (TradeType) savedInstanceState.getSerializable(EXTRA_TRADE_TYPE);
            }
        }

        setHasOptionsMenu(true);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putSerializable(EXTRA_TRADE_TYPE, tradeType);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
        inflater.inflate(R.menu.search, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
            case R.id.action_location:
                getLasKnownLocation();
                return true;
        }
        return false;
    }

    @Override
    public void onResume()
    {
        super.onResume();

        if (!isNetworkConnected()) {
            handleError(new Throwable(getString(R.string.error_no_internet)), true);
        }

        if (!checkPlayServices()) {
            showGoogleAPIResolveError();
        }

        subscribeData();
        updateData();
    }

    @Override
    public void onDetach()
    {
        super.onDetach();

        ButterKnife.reset(this);

        if (geoSubscription != null) {
            geoSubscription.unsubscribe();
            geoSubscription = null;
        }
        
        if (geoLocationSubscription != null) {
            geoLocationSubscription.unsubscribe();
            geoLocationSubscription = null;
        }

        if (dataServiceSubscription != null) {
            dataServiceSubscription.unsubscribe();
            dataServiceSubscription = null;
        }

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

        String[] locationTitles = getResources().getStringArray(R.array.list_location_spinner);
        List<String> locationList = new ArrayList<>(Arrays.asList(locationTitles));

        SpinnerAdapter locationAdapter = new SpinnerAdapter(getActivity(), R.layout.spinner_layout, locationList);
        locationSpinner.setAdapter(locationAdapter);

        String[] typeTitles = getResources().getStringArray(R.array.list_types_spinner);
        List<String> typeList = new ArrayList<>(Arrays.asList(typeTitles));

        SpinnerAdapter typeAdapter = new SpinnerAdapter(getActivity(), R.layout.spinner_layout, typeList);
        typeSpinner.setAdapter(typeAdapter);

        tradeType = TradeType.valueOf(SearchUtils.getSearchTradeType(sharedPreferences));
        Timber.e("Trade Type: " + tradeType.name());

        switch (tradeType) {
            case LOCAL_BUY:
                typeSpinner.setSelection(0);
                locationSpinner.setSelection(0);
                break;
            case LOCAL_SELL:
                typeSpinner.setSelection(1);
                locationSpinner.setSelection(0);
                break;
            case ONLINE_BUY:
                typeSpinner.setSelection(0);
                locationSpinner.setSelection(1);
                break;
            case ONLINE_SELL:
                typeSpinner.setSelection(1);
                locationSpinner.setSelection(1);
                break;
        }

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
                }

                SearchUtils.setSearchTradeType(sharedPreferences, tradeType.name());
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
                        paymentMethodLayout.setVisibility(View.GONE);
                        break;
                    case 1:
                        tradeType = (typeSpinner.getSelectedItemPosition() == 0 ? TradeType.ONLINE_BUY : TradeType.ONLINE_SELL);
                        paymentMethodLayout.setVisibility(View.VISIBLE);
                        break;
                }

                SearchUtils.setSearchTradeType(sharedPreferences, tradeType.name());
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0)
            {
            }
        });

        editLocation.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l)
            {
                Address address = predictAdapter.getItem(i);
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
                if (!TextUtils.isEmpty(charSequence)) {
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

        String addressString = SearchUtils.getSearchAddress(sharedPreferences);
        if (!TextUtils.isEmpty(addressString)) {
            Address address = SearchUtils.stringToAddress(addressString);
            setAddress(address);
        }

        setupToolbar();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        switch (requestCode) {
            case REQUEST_CHECK_SETTINGS:
                //Refrence: https://developers.google.com/android/reference/com/google/android/gms/location/SettingsApi
                getLasKnownLocation();
                break;
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode == Activity.RESULT_OK) {
                    // nothing
                } else {
                    toast("Search canceled...");
                    ((MainActivity) getActivity()).setContentFragment(MainActivity.DRAWER_DASHBOARD);
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }


    public void onRefresh()
    {
        subscribeData();
        updateData();
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
        if (tradeType == TradeType.LOCAL_BUY || tradeType == TradeType.LOCAL_SELL) {
            paymentMethodLayout.setVisibility(View.GONE);
        } else {
            paymentMethodLayout.setVisibility(View.VISIBLE);
        }

        MethodAdapter typeAdapter = new MethodAdapter(getActivity(), R.layout.spinner_layout, methods);
        paymentMethodSpinner.setAdapter(typeAdapter);

        String methodCode = SearchUtils.getSearchPaymentMethod(sharedPreferences);
        int position = 0;
        for (MethodItem methodItem : methods) {
            if (methodItem.code().equals(methodCode)) {
                break;
            }
            position++;
        }

        paymentMethodSpinner.setSelection(position);
        paymentMethodSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long arg3)
            {
                MethodItem methodItem = (MethodItem) paymentMethodSpinner.getAdapter().getItem(position);
                SearchUtils.setSearchPaymentMethod(sharedPreferences, methodItem.code());
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0)
            {
            }
        });
    }

    public void setAddress(Address address)
    {
        if (address != null) {
            String addressString = SearchUtils.addressToString(address);
            if(addressString == null) {
                showAddressError();
            } else {
                SearchUtils.setSearchAddress(sharedPreferences, addressString);
                locationText.setText(SearchUtils.getAddressShort(address));
                searchButton.setEnabled(true);
                showLocationLayout();
            }
        } else {
            showAddressError();
        }
    }

    private void showAddressError()
    {
        // TODO allow lat lon manually entered
        showAlertDialog(new AlertDialogEvent("Address Error", getString(R.string.error_dialog_bad_address)), new Action0()
        {
            @Override
            public void call()
            {
                getLasKnownLocation();
            }
        }, new Action0()
        {
            @Override
            public void call()
            {
                showEditTextLayout();
                editLocation.setText("");
            }
        });
    }

    protected void showEditTextLayout()
    {
        if (locationText.isShown()) {
            locationText.startAnimation(AnimationUtils.loadAnimation(getActivity(), android.R.anim.slide_out_right));
            editLocationLayout.startAnimation(AnimationUtils.loadAnimation(getActivity(), android.R.anim.slide_in_left));
            locationText.setVisibility(View.GONE);
            editLocationLayout.setVisibility(View.VISIBLE);
            editLocation.requestFocus();
            try {
                if (isAdded()) {
                    InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.showSoftInput(editLocation, InputMethodManager.SHOW_IMPLICIT);
                }
            } catch (NullPointerException e) {
                Timber.w("Error opening keyboard");
            }
        }
    }

    protected void showLocationLayout()
    {
        if (editLocationLayout.isShown()) {
            editLocationLayout.startAnimation(AnimationUtils.loadAnimation(getActivity(), android.R.anim.slide_out_right));
            locationText.startAnimation(AnimationUtils.loadAnimation(getActivity(), android.R.anim.slide_in_left));
            editLocationLayout.setVisibility(View.GONE);
            locationText.setVisibility(View.VISIBLE);
            try {
                if (isAdded()) {
                    InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
                }
            } catch (NullPointerException e) {
                Timber.w("Error closing keyboard");
            }
        }
    }

    protected void setEditLocationAdapter(PredictAdapter adapter)
    {
        if (editLocation != null)
            editLocation.setAdapter(adapter);
    }

    private void updateData()
    {
        if (!NetworkUtils.isNetworkConnected(getActivity())) {
            handleError(new NetworkConnectionException(), true);
            return;
        }

        if (dataServiceSubscription != null)
            return;

        dataServiceSubscription = dataService.getMethods()
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

                        dataServiceSubscription = null;
                    }
                }, new Action1<Throwable>()
                {
                    @Override
                    public void call(Throwable throwable)
                    {
                        handleError(throwable);
                        dataServiceSubscription = null;
                    }
                });
    }

    private boolean isNetworkConnected()
    {
        ConnectivityManager cm = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        return networkInfo != null;
    }

    protected void doAddressLookup(String locationName)
    {
        geoSubscription = geoGetLocationFromName(locationName)
                .doOnUnsubscribe(new Action0()
                {
                    @Override
                    public void call()
                    {
                        Timber.i("geoGetLocationFromName Method subscription safely unsubscribed");
                    }
                })
                .compose(this.<List<Address>>bindUntilEvent(FragmentEvent.PAUSE))
                .observeOn(Schedulers.computation())
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<List<Address>>()
                {
                    @Override
                    public void call(final List<Address> addresses)
                    {
                        if (!addresses.isEmpty()) {
                            getActivity().runOnUiThread(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    predictAdapter.replaceWith(addresses);
                                }
                            });
                        }

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
                                try {
                                    InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                                    imm.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
                                } catch (NullPointerException e) {
                                    Timber.w("Error closing keyboard");
                                }
                                //reportError(throwable);
                                showAlertDialog(new AlertDialogEvent("Address Error", getString(R.string.error_unable_load_address)), new Action0()
                                {
                                    @Override
                                    public void call()
                                    {
                                        getLasKnownLocation();
                                    }
                                }, new Action0()
                                {
                                    @Override
                                    public void call()
                                    {
                                        toast("Search canceled...");
                                        ((MainActivity) getActivity()).setContentFragment(MainActivity.DRAWER_DASHBOARD);
                                    }
                                });
                            }
                        });
                    }
                });
    }

    protected Observable<List<Address>> geoGetLocationFromName(final String searchQuery)
    {
        final ReactiveLocationProvider locationProvider = new ReactiveLocationProvider(getContext().getApplicationContext());
        return locationProvider.getGeocodeObservable(searchQuery, MAX_ADDRESSES);
    }

    private void showSearchResultsScreen()
    {
        String methodCode = SearchUtils.getSearchPaymentMethod(sharedPreferences);
        TradeType tradeType = TradeType.valueOf(SearchUtils.getSearchTradeType(sharedPreferences));
        Address address = SearchUtils.stringToAddress(SearchUtils.getSearchAddress(sharedPreferences));
        if (address == null) {
            snackError("Please enter a valid address.");
            return;
        }

        Intent intent = SearchResultsActivity.createStartIntent(getActivity(), tradeType, address, methodCode);
        startActivity(intent);
    }

    private void closeView()
    {
        toast("Search canceled...");
        ((MainActivity) getActivity()).setContentFragment(MainActivity.DRAWER_DASHBOARD);
    }

    // ------  GOOGLE SERVICES ---------

    private boolean checkPlayServices()
    {
        GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
        int result = googleAPI.isGooglePlayServicesAvailable(getActivity());
        return result == ConnectionResult.SUCCESS;
    }

    private void showGoogleAPIResolveError()
    {
        GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
        int result = googleAPI.isGooglePlayServicesAvailable(getActivity());
        if (googleAPI.isUserResolvableError(result)) {
            showGooglePlayServicesError();
        } else {
            toast(getString(R.string.warning_no_google_play_services));
            getActivity().finish();
        }
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

    private void showGooglePlayServicesError()
    {
        int result = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getActivity());

        switch (result) {
            case ConnectionResult.SERVICE_MISSING:
                showAlertDialog(new AlertDialogEvent(getString(R.string.warning_no_google_play_services_title), getString(R.string.warning_no_google_play_services)), new Action0()
                {
                    @Override
                    public void call()
                    {
                        installGooglePlayServices();
                    }
                }, new Action0()
                {
                    @Override
                    public void call()
                    {
                        toast(getString(R.string.warning_no_google_play_services));
                        getActivity().finish();
                    }
                });
                break;
            case ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED:
                showAlertDialog(new AlertDialogEvent(getString(R.string.warning_no_google_play_services_title), getString(R.string.warning_update_google_play_services)), new Action0()
                {
                    @Override
                    public void call()
                    {
                        installGooglePlayServices();
                    }
                }, new Action0()
                {
                    @Override
                    public void call()
                    {
                        toast(getString(R.string.warning_no_google_play_services));
                        getActivity().finish();
                    }
                });
                break;
            case ConnectionResult.SERVICE_DISABLED:
                showAlertDialog(new AlertDialogEvent(getString(R.string.warning_no_google_play_services_title), getString(R.string.warning_no_google_play_services)), new Action0()
                {
                    @Override
                    public void call()
                    {
                        installGooglePlayServices();
                    }
                }, new Action0()
                {
                    @Override
                    public void call()
                    {
                        toast(getString(R.string.warning_no_google_play_services));
                        getActivity().finish();
                    }
                });
                break;
        }
    }

    // ------  LOCATION SERVICES ---------

    @TargetApi(Build.VERSION_CODES.M)
    private void getLasKnownLocation()
    {
        Timber.d("getLasKnownLocation");
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                showRequestPermissionsDialog();
                return;
            }
        }
        
        if (!hasLocationServices()) {
            showNoLocationServicesWarning();
            return;
        }
        
        LocationRequest request = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)
                .setNumUpdates(3)
                .setInterval(100);

        final ReactiveLocationProvider locationProvider = new ReactiveLocationProvider(getActivity());
        geoLocationSubscription = locationProvider.getUpdatedLocation(request)
                .doOnUnsubscribe(new Action0()
                {
                    @Override
                    public void call()
                    {
                        Timber.i("getUpdatedLocation Method subscription safely unsubscribed");
                    }
                })
                .compose(this.<Location>bindUntilEvent(FragmentEvent.PAUSE))
                .flatMap(new Func1<Location, Observable<List<Address>>>()
                {
                    @Override
                    public Observable<List<Address>> call(Location location)
                    {
                        try {
                            return locationProvider.getReverseGeocodeObservable(location.getLatitude(), location.getLongitude(), 1)
                                    .observeOn(Schedulers.io())
                                    .subscribeOn(AndroidSchedulers.mainThread());
                        } catch (Exception exception) {
                            return Observable.just(null);
                        }
                    }
                })
                .map(new Func1<List<Address>, Address>()
                {
                    @Override
                    public Address call(List<Address> addresses)
                    {
                        return (addresses != null && !addresses.isEmpty()) ? addresses.get(0) : null;
                    }
                })
                .subscribe(new Action1<Address>()
                {
                    @Override
                    public void call(final Address address)
                    {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    if (address != null) {
                                        setAddress(address);
                                    } else {
                                        handleError(new Throwable(getString(R.string.error_unable_load_address)), true);
                                    }

                                    if(geoLocationSubscription != null) {
                                        geoLocationSubscription.unsubscribe();
                                        geoLocationSubscription = null; 
                                    }
                                }
                            });
                        }
                    }
                }, new Action1<Throwable>()
                {
                    @Override
                    public void call(final Throwable throwable)
                    {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    reportError(throwable);
                                    handleError(new Throwable(getString(R.string.error_unable_load_address)), true);
                                    
                                    if(geoLocationSubscription != null) {
                                        geoLocationSubscription.unsubscribe();
                                        geoLocationSubscription = null;
                                    }
                                }
                            });
                        }
                    }
                });
    }
    
    private boolean hasLocationServices()
    {
        return (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER));
    }

    public void showRequestPermissionsDialog()
    {
        showAlertDialog(new AlertDialogEvent(getString(R.string.alert_permission_required), getString(R.string.require_location_permission)),
                new Action0()
                {
                    @Override
                    public void call()
                    {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, Constants.REQUEST_PERMISSIONS);
                        }
                    }
                }, new Action0()
                {
                    @Override
                    public void call()
                    {
                        closeView();
                    }
                });
    }

    private void showNoLocationServicesWarning()
    {
        showAlertDialog(new AlertDialogEvent(getString(R.string.warning_no_location_services_title), getString(R.string.warning_no_location_active)), new Action0()
        {
            @Override
            public void call()
            {
                Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivityForResult(myIntent, REQUEST_CHECK_SETTINGS);
            }
        }, new Action0()
        {
            @Override
            public void call()
            {
                closeView();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults)
    {
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
                        closeView();
                    } else {
                        getLasKnownLocation();
                    }
                }
            }
        }
    }
}