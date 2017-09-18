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
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
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
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.gms.location.LocationRequest;
import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.constants.Constants;
import com.thanksmister.bitcoin.localtrader.data.api.model.ExchangeCurrency;
import com.thanksmister.bitcoin.localtrader.data.api.model.Method;
import com.thanksmister.bitcoin.localtrader.data.api.model.TradeType;
import com.thanksmister.bitcoin.localtrader.data.database.CurrencyItem;
import com.thanksmister.bitcoin.localtrader.data.database.DbManager;
import com.thanksmister.bitcoin.localtrader.data.database.ExchangeCurrencyItem;
import com.thanksmister.bitcoin.localtrader.data.database.MethodItem;
import com.thanksmister.bitcoin.localtrader.data.services.DataService;
import com.thanksmister.bitcoin.localtrader.data.services.ExchangeService;
import com.thanksmister.bitcoin.localtrader.data.services.GeoLocationService;
import com.thanksmister.bitcoin.localtrader.events.AlertDialogEvent;
import com.thanksmister.bitcoin.localtrader.events.ProgressDialogEvent;
import com.thanksmister.bitcoin.localtrader.ui.BaseFragment;
import com.thanksmister.bitcoin.localtrader.ui.activities.MainActivity;
import com.thanksmister.bitcoin.localtrader.ui.activities.SearchResultsActivity;
import com.thanksmister.bitcoin.localtrader.ui.adapters.CurrencyAdapter;
import com.thanksmister.bitcoin.localtrader.ui.adapters.MethodAdapter;
import com.thanksmister.bitcoin.localtrader.ui.adapters.PredictAdapter;
import com.thanksmister.bitcoin.localtrader.ui.adapters.SpinnerAdapter;
import com.thanksmister.bitcoin.localtrader.utils.CurrencyUtils;
import com.thanksmister.bitcoin.localtrader.utils.Doubles;
import com.thanksmister.bitcoin.localtrader.utils.NetworkUtils;
import com.thanksmister.bitcoin.localtrader.utils.SearchUtils;
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
import pl.charmas.android.reactivelocation.ReactiveLocationProvider;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subscriptions.Subscriptions;
import timber.log.Timber;

import static com.thanksmister.bitcoin.localtrader.data.services.GeoLocationService.MAX_ADDRESSES;

public class SearchFragment extends BaseFragment {

    private static final String EXTRA_ADDRESS = "com.thanksmister.extra.EXTRA_ADDRESS";
    private static final String EXTRA_TRADE_TYPE = "com.thanksmister.extra.EXTRA_TRADE_TYPE";
    public static final int REQUEST_CHECK_SETTINGS = 0;
    public static final int REQUEST_GOOGLE_PLAY_SERVICES = 1972;

    @Inject
    DbManager dbManager;

    @Inject
    DataService dataService;

    @Inject
    ExchangeService exchangeService;

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

    @InjectView(R.id.editLatitude)
    EditText editLatitude;

    @InjectView(R.id.editLongitude)
    EditText editLongitude;

    @InjectView(R.id.editLocationLayout)
    View editLocationLayout;

    @InjectView(R.id.locationSpinner)
    Spinner locationSpinner;

    @InjectView(R.id.typeSpinner)
    Spinner typeSpinner;

    @InjectView(R.id.countrySpinner)
    Spinner countrySpinner;

    @InjectView(R.id.paymentMethodSpinner)
    Spinner paymentMethodSpinner;

    @InjectView(R.id.onlineOptionsLayout)
    View onlineOptionsLayout;

    @InjectView(R.id.localOptionsLayout)
    View localOptionsLayout;

    @InjectView(R.id.searchButton)
    Button searchButton;

    @InjectView(R.id.clearButton)
    ImageButton clearButton;

    @InjectView(R.id.currencySpinner)
    Spinner currencySpinner;

    @OnClick(R.id.clearButton)
    public void clearButtonClicked() {
        SearchUtils.setSearchLatitude(sharedPreferences, 0);
        SearchUtils.setSearchLongitude(sharedPreferences, 0);
        SearchUtils.clearSearchLocationAddress(sharedPreferences);
        editLocation.setText("");
        editLatitude.setText("");
        editLongitude.setText("");
        showEditTextLayout();
    }

    @OnClick(R.id.searchButton)
    public void searchButtonClicked() {
        showSearchResultsScreen();
    }

    private PredictAdapter predictAdapter;
    private TradeType tradeType = TradeType.LOCAL_BUY;
    private Subscription geoSubscription;
    private Subscription dataServiceSubscription;
    private Subscription geoLocationSubscription;
    private Subscription currencySubscription = Subscriptions.empty();
    private MenuItem locationMenuItem;

    public static SearchFragment newInstance() {
        return new SearchFragment();
    }

    public SearchFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(EXTRA_ADDRESS)) {
                tradeType = (TradeType) savedInstanceState.getSerializable(EXTRA_TRADE_TYPE);
            }
        }
        setHasOptionsMenu(true);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(EXTRA_TRADE_TYPE, tradeType);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.search, menu);
        locationMenuItem = menu.findItem(R.id.action_location);
        if (locationMenuItem != null) {
            locationMenuItem.setVisible((tradeType == TradeType.LOCAL_BUY || tradeType == TradeType.LOCAL_SELL));
        }

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_location:
                checkLocationEnabled();
                return true;
        }
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();
        subscribeData();
    }

    @Override
    public void onDetach() {
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

        if (currencySubscription != null) {
            currencySubscription.unsubscribe();
            currencySubscription = null;
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.view_search, container, false);
    }

    @Override
    public void onViewCreated(View fragmentView, @Nullable Bundle savedInstanceState) {

        super.onViewCreated(fragmentView, savedInstanceState);

        String[] countryNames = getResources().getStringArray(R.array.country_names);
        List<String> countryNamesList = new ArrayList<>(Arrays.asList(countryNames));
        countryNamesList.add(0, "Any");
        SpinnerAdapter countryAdapter = new SpinnerAdapter(getActivity(), R.layout.spinner_layout, countryNamesList);
        countrySpinner.setAdapter(countryAdapter);

        int i = 0;
        String countryName = SearchUtils.getSearchCountryName(sharedPreferences);
        for (String name : countryNamesList) {
            if (name.equals(countryName)) {
                countrySpinner.setSelection(i);
                break;
            }
            i++;
        }

        String[] locationTitles = getResources().getStringArray(R.array.list_location_spinner);
        List<String> locationList = new ArrayList<>(Arrays.asList(locationTitles));

        SpinnerAdapter locationAdapter = new SpinnerAdapter(getActivity(), R.layout.spinner_layout, locationList);
        locationSpinner.setAdapter(locationAdapter);

        String[] typeTitles = getResources().getStringArray(R.array.list_types_spinner);
        List<String> typeList = new ArrayList<>(Arrays.asList(typeTitles));

        SpinnerAdapter typeAdapter = new SpinnerAdapter(getActivity(), R.layout.spinner_layout, typeList);
        typeSpinner.setAdapter(typeAdapter);

        currencySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                ExchangeCurrency exchange = (ExchangeCurrency) currencySpinner.getAdapter().getItem(i);
                SearchUtils.setSearchCurrency(sharedPreferences, exchange.getCurrency());
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        tradeType = TradeType.valueOf(SearchUtils.getSearchTradeType(sharedPreferences));

        if (locationMenuItem != null) {
            locationMenuItem.setVisible((tradeType == TradeType.LOCAL_BUY || tradeType == TradeType.LOCAL_SELL));
        }

        if (tradeType == TradeType.LOCAL_BUY || tradeType == TradeType.LOCAL_SELL) {
            onlineOptionsLayout.setVisibility(View.GONE);
            localOptionsLayout.setVisibility(View.VISIBLE);
        } else {
            onlineOptionsLayout.setVisibility(View.VISIBLE);
            localOptionsLayout.setVisibility(View.GONE);
        }

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

        typeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long arg3) {
                switch (position) {
                    case 0:
                        tradeType = (locationSpinner.getSelectedItemPosition() == 0 ? TradeType.LOCAL_BUY : TradeType.ONLINE_BUY);
                        break;
                    case 1:
                        tradeType = (locationSpinner.getSelectedItemPosition() == 0 ? TradeType.LOCAL_SELL : TradeType.ONLINE_SELL);
                        break;
                }
                SearchUtils.setSearchTradeType(sharedPreferences, tradeType.name());
                if (locationMenuItem != null) {
                    locationMenuItem.setVisible((tradeType == TradeType.LOCAL_BUY || tradeType == TradeType.LOCAL_SELL));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

        countrySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long arg3) {
                String[] countryCodes = getResources().getStringArray(R.array.country_codes);

                String selectedCountryName = (String) countrySpinner.getAdapter().getItem(position);
                String selectedCountryCode = (position == 0) ? "" : countryCodes[position - 1];

                Timber.d("Selected Country Name: " + selectedCountryName);
                Timber.d("Selected Country Code: " + selectedCountryCode);

                SearchUtils.setSearchCountryName(sharedPreferences, selectedCountryName);
                SearchUtils.setSearchCountryCode(sharedPreferences, selectedCountryCode);
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

        locationSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long arg3) {
                switch (position) {
                    case 0:
                        tradeType = (typeSpinner.getSelectedItemPosition() == 0 ? TradeType.LOCAL_BUY : TradeType.LOCAL_SELL);
                        onlineOptionsLayout.setVisibility(View.GONE);
                        localOptionsLayout.setVisibility(View.VISIBLE);
                        break;
                    case 1:
                        tradeType = (typeSpinner.getSelectedItemPosition() == 0 ? TradeType.ONLINE_BUY : TradeType.ONLINE_SELL);
                        onlineOptionsLayout.setVisibility(View.VISIBLE);
                        localOptionsLayout.setVisibility(View.GONE);
                        break;
                }
                Timber.d("TradeType: " + tradeType);
                SearchUtils.setSearchTradeType(sharedPreferences, tradeType.name());
                if (locationMenuItem != null) {
                    locationMenuItem.setVisible((tradeType == TradeType.LOCAL_BUY || tradeType == TradeType.LOCAL_SELL));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

        editLocation.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Address address = predictAdapter.getItem(i);
                editLocation.setText("");
                saveAddress(address);
                displayAddress(address);
            }
        });

        editLocation.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                if (!TextUtils.isEmpty(charSequence)) {
                    doAddressLookup(charSequence.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        predictAdapter = new PredictAdapter(getActivity(), new ArrayList<Address>());
        setEditLocationAdapter(predictAdapter);

        Address address = SearchUtils.getSearchLocationAddress(sharedPreferences);
        displayAddress(address);
        setupToolbar();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CHECK_SETTINGS:
                checkLocationEnabled();
                break;
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode != Activity.RESULT_OK) {
                    toast(R.string.toast_search_canceled);
                    if (isAdded()) {
                        ((MainActivity) getActivity()).navigateDashboardView();
                    }
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
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
                        startLocationMonitoring();
                    }
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void setupToolbar() {
        ((MainActivity) getActivity()).setSupportActionBar(toolbar);
        final ActionBar ab = ((MainActivity) getActivity()).getSupportActionBar();
        if (ab != null) {
            ab.setHomeAsUpIndicator(R.drawable.ic_action_navigation_menu);
            ab.setTitle(getString(R.string.view_title_buy_sell));
            ab.setDisplayHomeAsUpEnabled(true);
        }
    }

    private void subscribeData() {
        dbManager.currencyQuery()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .compose(this.<List<CurrencyItem>>bindUntilEvent(FragmentEvent.PAUSE))
                .subscribe(new Action1<List<CurrencyItem>>() {
                    @Override
                    public void call(List<CurrencyItem> currencyItems) {
                        if(currencyItems == null || currencyItems.isEmpty()) {
                            fetchCurrencies();
                        } else {
                            List<ExchangeCurrency> exchangeCurrencies = new ArrayList<ExchangeCurrency>();
                            exchangeCurrencies = ExchangeCurrencyItem.getCurrencies(currencyItems);
                            setCurrencies(exchangeCurrencies); 
                        }
                    }
                });
        dbManager.methodQuery()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnUnsubscribe(new Action0() {
                    @Override
                    public void call() {
                        Timber.i("Method subscription safely unsubscribed");
                    }
                })
                .compose(this.<List<MethodItem>>bindUntilEvent(FragmentEvent.PAUSE))
                .subscribe(new Action1<List<MethodItem>>() {
                    @Override
                    public void call(List<MethodItem> methodItems) {
                        if(methodItems == null || methodItems.isEmpty()) {
                            fetchMethods();
                        } else {
                            Method method = new Method();
                            method.code = "all";
                            method.name = getString(R.string.text_method_name_all);
                            MethodItem methodItem = MethodItem.getModelItem(method);
                            methodItems.add(0, methodItem);
                            setMethods(methodItems);
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        handleError(new Throwable(getString(R.string.error_unable_load_payment_methods)));
                    }
                });
    }

    private void fetchCurrencies() {

        dataService.getCurrencies()
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnUnsubscribe(new Action0() {
                    @Override
                    public void call() {
                        Timber.i("Currencies network subscription safely unsubscribed");
                    }
                })
                .compose(this.<List<ExchangeCurrency>>bindUntilEvent(FragmentEvent.PAUSE))
                .subscribe(new Action1<List<ExchangeCurrency>>() {
                    @Override
                    public void call(List<ExchangeCurrency> currencies) {
                        if(currencies != null) {
                            dbManager.insertCurrencies(currencies);
                            dataService.setCurrencyExpireTime();
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        handleError(throwable);

                    }
                });
    }

    private void fetchMethods() {
        
        Timber.d("getMethods");
        
        dataService.getMethods()
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnUnsubscribe(new Action0() {
                    @Override
                    public void call() {
                        Timber.i("Method network subscription safely unsubscribed");
                    }
                })
                .compose(this.<List<Method>>bindUntilEvent(FragmentEvent.PAUSE))
                .subscribe(new Action1<List<Method>>() {
                    @Override
                    public void call(List<Method> methods) {
                        if(methods != null) {
                            dbManager.updateMethods(methods);
                            dataService.setMethodsExpireTime();
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        toast(getString(R.string.toast_loading_methods));
                    }
                });
    }

    private void setMethods(@NonNull List<MethodItem> methods) {
        
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

        if (position <= methods.size()) {
            paymentMethodSpinner.setSelection(position);
        }

        paymentMethodSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long arg3) {
                try {
                    MethodItem methodItem = (MethodItem) paymentMethodSpinner.getAdapter().getItem(position);
                    SearchUtils.setSearchPaymentMethod(sharedPreferences, methodItem.code());
                } catch (IndexOutOfBoundsException e) {
                    Timber.e("Error setting methods: " + e.getMessage());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });
    }

    /**
     * Set the currencies to be used for a new editAdvertisement
     * @param currencies
     */
    private void setCurrencies(@NonNull List<ExchangeCurrency> currencies) {
        
        currencies = CurrencyUtils.sortCurrencies(currencies);
        String searchCurrency = SearchUtils.getSearchCurrency(sharedPreferences);
        if (currencies.isEmpty()) {
            ExchangeCurrency exchangeCurrency = new ExchangeCurrency(searchCurrency);
            currencies.add(exchangeCurrency); // just revert back to default
        }

        // TODO this is temporary fix for issues with Any as default currency
        boolean containsAny = false;
        for (ExchangeCurrency currency : currencies) {
            if (currency.getCurrency().toLowerCase().equals(getString(R.string.text_currency_any).toLowerCase())) {
                containsAny = true;
                break;
            }
        }

        if (!containsAny) {
            // add "any" for search option
            ExchangeCurrency exchangeCurrency = new ExchangeCurrency(getString(R.string.text_currency_any));
            currencies.add(0, exchangeCurrency);
        }

        CurrencyAdapter typeAdapter = new CurrencyAdapter(getActivity(), R.layout.spinner_layout, currencies);
        currencySpinner.setAdapter(typeAdapter);

        int i = 0;
        for (ExchangeCurrency currency : currencies) {
            if (currency.getCurrency().equals(searchCurrency)) {
                currencySpinner.setSelection(i);
                break;
            }
            i++;
        }
    }

    /**
     * Saves the current address to user preferences
     *
     * @param address Address
     */
    public void saveAddress(Address address) {
        
        Timber.d("SaveAddress: " + address.toString());
        
        SearchUtils.setSearchLocationAddress(sharedPreferences, address);
        if (address.hasLatitude()) {
            SearchUtils.setSearchLatitude(sharedPreferences, address.getLatitude());
        }
        if (address.hasLongitude()) {
            SearchUtils.setSearchLongitude(sharedPreferences, address.getLongitude());
        }
        if (!TextUtils.isEmpty(address.getCountryCode())) {
            SearchUtils.setSearchCountryCode(sharedPreferences, address.getCountryCode());
        }
        if (!TextUtils.isEmpty(address.getAddressLine(0))) {
            SearchUtils.setSearchCountryName(sharedPreferences, address.getAddressLine(0));
        }
    }

    /**
     * Displays the shortened version of the current address
     *
     * @param address Address
     */
    public void displayAddress(Address address) {
        String shortAddress = SearchUtils.getDisplayAddress(address);
        if (!TextUtils.isEmpty(shortAddress)) {
            if (address.hasLatitude()) {
                editLatitude.setText(String.valueOf(address.getLatitude()));
            }
            if (address.hasLongitude()) {
                editLongitude.setText(String.valueOf(address.getLongitude()));
            }
            locationText.setText(shortAddress);
            searchButton.setEnabled(true);
            showLocationLayout();
        }
    }

    private void showAddressError() {
        showAlertDialog(new AlertDialogEvent(getString(R.string.error_address_title), getString(R.string.error_dialog_no_address_edit)));
    }

    private void showEditTextLayout() {
        if (locationText.isShown()) {
            locationText.startAnimation(AnimationUtils.loadAnimation(getActivity(), android.R.anim.slide_out_right));
            editLocationLayout.startAnimation(AnimationUtils.loadAnimation(getActivity(), android.R.anim.slide_in_left));
            locationText.setVisibility(View.GONE);
            editLocationLayout.setVisibility(View.VISIBLE);
            editLocation.requestFocus();
        }
    }

    private void showLocationLayout() {
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

    private void setEditLocationAdapter(PredictAdapter adapter) {
        if (editLocation != null) {
            editLocation.setAdapter(adapter);
        }
    }

    private void doAddressLookup(String locationName) {
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
                        if (isAdded()) {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (!addresses.isEmpty()) {
                                        predictAdapter.replaceWith(addresses);
                                    }
                                }
                            });
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(final Throwable throwable) {
                        if (isAdded()) {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                                        imm.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
                                    } catch (NullPointerException e) {
                                        Timber.w("Error closing keyboard");
                                    }
                                    Timber.e("Address lookup error: " + throwable.getMessage());
                                    showAddressError();
                                }
                            });
                        }
                    }
                });
    }

    private void showSearchResultsScreen() {
        TradeType tradeType = TradeType.valueOf(SearchUtils.getSearchTradeType(sharedPreferences));
        if (TradeUtils.isLocalTrade(tradeType)) {

            String latitude = editLatitude.getText().toString();
            String longitude = editLongitude.getText().toString();

            if (TextUtils.isEmpty(latitude) || TextUtils.isEmpty(longitude)) {
                SearchUtils.setSearchLatitude(sharedPreferences, 0);
                SearchUtils.setSearchLongitude(sharedPreferences, 0);
            } else if (!SearchUtils.coordinatesValid(latitude, longitude)) {
                showAlertDialog(new AlertDialogEvent("Error", "Invalid longitude or latitude entered. Latitude should be between -90 and 90.  Longitude should be between -180 and 180."));
                return;
            } else if (SearchUtils.coordinatesValid(latitude, longitude)) {
                SearchUtils.setSearchLatitude(sharedPreferences, Doubles.convertToDouble(latitude));
                SearchUtils.setSearchLongitude(sharedPreferences, Doubles.convertToDouble(longitude));
            }

            double lat = SearchUtils.getSearchLatitude(sharedPreferences);
            double lon = SearchUtils.getSearchLongitude(sharedPreferences);
            if (lon == 0 && lat == 0) {
                showAlertDialog(new AlertDialogEvent("Error", "To search local advertisers, enter valid latitude and longitude values."));
                return;
            }
        }
        Intent intent = SearchResultsActivity.createStartIntent(getActivity());
        startActivity(intent);
    }

    private void closeView() {
        if (isAdded()) {
            toast(getString(R.string.toast_search_canceled));
            ((MainActivity) getActivity()).setContentFragment(MainActivity.DRAWER_DASHBOARD);
        }
    }

    // ------  LOCATION SERVICES ---------

    public void checkLocationEnabled() {
        Timber.d("checkLocationEnabled");
        if (isAdded() && getActivity() != null) {
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
    }

    /**
     * Using the ReactiveLocationProvider to do location lookup because its more convenient than using
     * the LocationManager and seems to perform better. 
     */
    private void startLocationMonitoring() {

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
                .timeout(20000, TimeUnit.MILLISECONDS)
                .onErrorResumeNext(new Func1<Throwable, Observable<Location>>() {
                    @Override
                    public Observable<Location> call(Throwable throwable) {
                        return Observable.just(null);
                    }
                })
                .doOnUnsubscribe(new Action0() {
                    @Override
                    public void call() {
                        Timber.i("geoLocationSubscription lookup subscription safely unsubscribed");
                    }
                })
                .compose(this.<Location>bindUntilEvent(FragmentEvent.PAUSE))
                .observeOn(Schedulers.computation())
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Location>() {
                    @Override
                    public void call(final Location location) {
                        if (location != null) {
                            if (isAdded()) {
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        reverseLocationLookup(location);
                                    }
                                });
                            }
                        } else {
                            if (isAdded()) {
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        getLocationFromLocationManager();
                                    }
                                });
                            }
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(final Throwable throwable) {
                        if (isAdded()) {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    getLocationFromLocationManager();
                                }
                            });
                        }
                    }
                });
    }
    
    // TODO move these to base fragment
    /**
     * Used as a backup location service in case the first one fails, which 
     * switch to using the LocationManager instead. 
     */
    private void getLocationFromLocationManager () {
        Timber.d("getLocationFromLocationManager");
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
            locationManager.requestLocationUpdates(locationManager.getBestProvider(criteria, true), 100, 5, locationListener);
        } catch (SecurityException e) {
            if (isAdded()) {
                hideProgressDialog();
                Timber.e("Location manager could not use network provider", e);
                showAlertDialog(new AlertDialogEvent(getString(R.string.error_location_title), getString(R.string.error_location_message)));
            }
        } 
    }

    /**
     * Used from the location manager to do a reverse lookup of the address from the coordinates. 
     * @param location
     */
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
                        return (addresses != null && !addresses.isEmpty()) ? addresses.get(0) : null;
                    }
                })
                .subscribe(new Action1<Address>() {
                    @Override
                    public void call(final Address address) {
                        if (isAdded()) {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    hideProgressDialog();
                                    if (address == null) {
                                        Timber.d("Address reverseLocationLookup error");
                                        showAddressError();
                                    } else {
                                        saveAddress(address);
                                        displayAddress(address);
                                    }
                                }
                            });
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(final Throwable throwable) {
                        if (isAdded()) {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Timber.e("reverseLocationLookup: " + throwable.getMessage());
                                    hideProgressDialog();
                                    showAlertDialog(new AlertDialogEvent(getString(R.string.error_address_lookup_title), getString(R.string.error_address_lookup_description)));
                                }
                            });
                        }
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
                closeView();
            }
        });
    }
}