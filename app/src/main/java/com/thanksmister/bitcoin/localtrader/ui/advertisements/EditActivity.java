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

package com.thanksmister.bitcoin.localtrader.ui.advertisements;

import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.otto.Subscribe;
import com.squareup.sqlbrite.BriteDatabase;
import com.squareup.sqlbrite.SqlBrite;
import com.thanksmister.bitcoin.localtrader.BaseActivity;
import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.constants.Constants;
import com.thanksmister.bitcoin.localtrader.data.api.model.Advertisement;
import com.thanksmister.bitcoin.localtrader.data.api.model.Currency;
import com.thanksmister.bitcoin.localtrader.data.api.model.Method;
import com.thanksmister.bitcoin.localtrader.data.api.model.TradeType;
import com.thanksmister.bitcoin.localtrader.data.database.AdvertisementItem;
import com.thanksmister.bitcoin.localtrader.data.database.DbManager;
import com.thanksmister.bitcoin.localtrader.data.database.MethodItem;
import com.thanksmister.bitcoin.localtrader.data.services.DataService;
import com.thanksmister.bitcoin.localtrader.data.services.GeoLocationService;
import com.thanksmister.bitcoin.localtrader.events.ConfirmationDialogEvent;
import com.thanksmister.bitcoin.localtrader.events.ProgressDialogEvent;
import com.thanksmister.bitcoin.localtrader.events.RefreshEvent;
import com.thanksmister.bitcoin.localtrader.ui.misc.PredictAdapter;
import com.thanksmister.bitcoin.localtrader.ui.misc.CurrencyAdapter;
import com.thanksmister.bitcoin.localtrader.ui.misc.MethodAdapter;
import com.thanksmister.bitcoin.localtrader.ui.misc.SpinnerAdapter;
import com.thanksmister.bitcoin.localtrader.utils.Doubles;
import com.thanksmister.bitcoin.localtrader.utils.Parser;
import com.thanksmister.bitcoin.localtrader.utils.Strings;
import com.thanksmister.bitcoin.localtrader.utils.TradeUtils;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import butterknife.Optional;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func2;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;
import timber.log.Timber;

import static rx.android.app.AppObservable.bindActivity;

public class EditActivity extends BaseActivity
{
    public static final String EXTRA_ADDRESS = "com.thanksmister.extras.EXTRA_ADDRESS";
    public static final String EXTRA_CREATE = "com.thanksmister.extras.EXTRA_CREATE";
    public static final String EXTRA_AD_ID = "com.thanksmister.extras.EXTRA_AD_ID";

    public static final int REQUEST_CODE = 10937;
    public static final int RESULT_UPDATED = 72322;
    public static final int RESULT_CANCELED = 72321;

    @Inject
    DataService dataService;
    
    @Inject
    DbManager dbManager;

    @Inject
    BriteDatabase db;

    @InjectView(R.id.swipeLayout)
    SwipeRefreshLayout swipeLayout;

    @Inject
    GeoLocationService geoLocationService;

    @Inject
    LocationManager locationManager;
    
    @Optional
    @InjectView(R.id.editToolBar)
    Toolbar toolbar;

    @InjectView(R.id.liquidityCheckBox)
    CheckBox liquidityCheckBox;

    @InjectView(R.id.trustedCheckBox)
    CheckBox trustedCheckBox;

    @InjectView(R.id.smsVerifiedCheckBox)
    CheckBox smsVerifiedCheckBox;

    @InjectView(R.id.activeCheckBox)
    CheckBox activeCheckBox;

    @InjectView(R.id.currentLocation)
    TextView currentLocation;

    @InjectView(R.id.mapLayout)
    View mapLayout;

    @InjectView(R.id.searchLayout)
    View searchLayout;

    @InjectView(R.id.editLocation)
    AutoCompleteTextView editLocation;

    @InjectView(R.id.editPriceEquation)
    EditText editPriceEquation;

    @InjectView(R.id.editMinimumAmount)
    EditText editMinimumAmount;

    @InjectView(R.id.editMaximumAmount)
    EditText editMaximumAmount;

    @InjectView(R.id.editPaymentDetails)
    EditText editPaymentDetails;

    @InjectView(R.id.editBankNameText)
    EditText editBankNameText;

    @InjectView(R.id.paymentMethodLayout)
    View paymentMethodLayout;

    @InjectView(R.id.editPaymentDetailsLayout)
    View editPaymentDetailsLayout;

    @InjectView(R.id.editMinimumAmountCurrency)
    TextView editMinimumAmountCurrency;

    @InjectView(R.id.editMaximumAmountCurrency)
    TextView editMaximumAmountCurrency;

    @InjectView(R.id.bankNameLayout)
    View bankNameLayout;
    
    @InjectView(R.id.marginLayout)
    View marginLayout;

    @InjectView(R.id.advertisementTypeLayout)
    View advertisementTypeLayout;

    @InjectView(R.id.activeLayout)
    View activeLayout;

    @InjectView(R.id.currencySpinner)
    Spinner currencySpinner;

    @InjectView(R.id.marginText)
    EditText marginText;

    @InjectView(R.id.messageText)
    EditText messageText;

    @InjectView(R.id.paymentMethodSpinner)
    Spinner paymentMethodSpinner;

    @InjectView(R.id.typeSpinner)
    Spinner typeSpinner;

    @InjectView(R.id.editProgress)
    View progress;
    
    @InjectView(R.id.editContent)
    View content;

    @InjectView(R.id.editEmpty)
    View empty;

    @InjectView(R.id.emptyTextView)
    TextView emptyTextView;

    @OnClick(R.id.clearButton)
    public void clearButtonClicked()
    {
        showSearchLayout();
        stopLocationCheck(); // stop current check
    }

    @OnClick(R.id.mapButton)
    public void mapButtonClicked()
    {
        showMapLayout();
        currentLocation.setText("- - - -");
        stopLocationCheck(); // stop current check
        startLocationCheck(); // get new location
    }

    private boolean create;
    private PredictAdapter predictAdapter;
    private Address address;
    private String adId;
    
    private Observable<List<MethodItem>> methodObservable;
    private Observable<List<Address>> geoDecodeObservable;
    private Observable<List<Address>> geoLocationObservable;
    private Observable<AdvertisementItem> advertisementItemObservable;
    private Observable<Advertisement> createAdvertisementObservable;
    private Observable<JSONObject> updateAdvertisementObservable;
    private Observable<List<Currency>> currencyObservable;
    
    private CompositeSubscription subscriptions = new CompositeSubscription();
    private Subscription geoLocalSubscription = Subscriptions.empty();
    private Subscription geoDecodeSubscription = Subscriptions.empty();
    private Subscription advertisementSubscription = Subscriptions.empty();
    
    private class AdvertisementData {
        public AdvertisementItem advertisement;
        public List<Currency> currencies;
    }

    private AdvertisementData advertisementData;
    
    public static Intent createStartIntent(Context context, Boolean create, String adId)
    {
        Intent intent = new Intent(context, EditActivity.class);
        intent.putExtra(EXTRA_CREATE, create);
        intent.putExtra(EXTRA_AD_ID, adId);
        
        return intent;
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.view_edit);

        ButterKnife.inject(this);

        if (savedInstanceState == null) {
            adId = getIntent().getStringExtra(EXTRA_AD_ID);
            create = getIntent().getBooleanExtra(EXTRA_CREATE, false);
        } else {
            address = savedInstanceState.getParcelable(EXTRA_ADDRESS);
            create = savedInstanceState.getBoolean(EXTRA_CREATE);
            adId = savedInstanceState.getString(EXTRA_AD_ID);
        }
        
        if(toolbar != null) {
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle((create)? "Post new advertisement": "Edit advertisement");
            setToolBarMenu(toolbar);
        }

        String[] typeTitles = getResources().getStringArray(R.array.list_advertisement_type_spinner);
        List<String> typeList = new ArrayList<String>(Arrays.asList(typeTitles));

        SpinnerAdapter typeAdapter = new SpinnerAdapter(this, R.layout.spinner_layout, typeList);
        typeSpinner.setAdapter(typeAdapter);
        typeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l)
            {
                setPriceEquation();
                setAdvertisementType(TradeType.values()[i]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView)
            {
            }
        });

        currencySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l)
            {
                Currency exchange = (Currency) currencySpinner.getAdapter().getItem(i);
                editMinimumAmountCurrency.setText(exchange.ticker);
                editMaximumAmountCurrency.setText(exchange.ticker);
                if (create)
                    setPriceEquation();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView)
            {
            }
        });
        
        editLocation.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l)
            {
                Address address1 = predictAdapter.getItem(i);
                showMapLayout();
                setAddress(address1);
                editLocation.setText("");
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
        
        marginText.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3)
            {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3)
            {
                setPriceEquation();
            }

            @Override
            public void afterTextChanged(Editable editable)
            {

            }
        });

        predictAdapter = new PredictAdapter(EditActivity.this, new ArrayList<Address>());
        setEditLocationAdapter(predictAdapter);

        currencyObservable = bindActivity(this, dataService.getCurrencies().cache());
        methodObservable = bindActivity(this, db.createQuery(MethodItem.TABLE, MethodItem.QUERY).map(MethodItem.MAP).cache());
        advertisementItemObservable = bindActivity(this, db.createQuery(AdvertisementItem.TABLE, AdvertisementItem.QUERY_ITEM, adId).map(AdvertisementItem.MAP_SINGLE));

        swipeLayout.setEnabled(false);
        showProgress();
    }

    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putParcelable(EXTRA_ADDRESS, address);
        outState.putBoolean(EXTRA_CREATE, create);
        outState.putString(EXTRA_AD_ID, adId);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    
    public void setToolBarMenu(Toolbar toolbar)
    {
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener()
        {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem)
            {
                switch (menuItem.getItemId()) {
                    case R.id.action_cancel:
                        cancelChanges(create);
                        return true;
                    case R.id.action_save:
                        validateChangesAndSend();
                        return true;
                }
                return false;
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        if(toolbar != null)
            toolbar.inflateMenu(R.menu.edit);

        return true;
    }

    @Override
    public void onResume()
    {
        super.onResume();
        
        if(create) {
            if (geoLocationService.isGooglePlayServicesAvailable()) {
                subScribeData();
            }
            startLocationCheck();
        }  else {
            subScribeData();
        }
    }

    @Override
    public void onPause()
    {
        super.onPause();
        
        subscriptions.unsubscribe();
        geoLocalSubscription.unsubscribe();
        geoDecodeSubscription.unsubscribe();
        advertisementSubscription.unsubscribe();
    }
    
    public void subScribeData()
    {
        subscriptions = new CompositeSubscription();
        
        subscriptions.add(methodObservable.subscribe(new Action1<List<MethodItem>>()
        {
            @Override
            public void call(List<MethodItem> methodItems)
            {
                setMethods(methodItems);
            }
        }));
        
        if(create) {
            subscriptions.add(currencyObservable
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Action1<List<Currency>>()
                    {
                        @Override
                        public void call(List<Currency> currencies)
                        {
                            hideProgress();
                            setCurrencies(currencies, null);
                            createAdvertisement();
                        }
                    }, new Action1<Throwable>()
                    {
                        @Override
                        public void call(Throwable throwable)
                        {
                            hideProgress();
                            showError("No advertisement data.");
                            handleError(throwable);
                        }
                    }));
        } else {

            subscriptions.add(Observable.combineLatest(currencyObservable, advertisementItemObservable, new Func2<List<Currency>, AdvertisementItem, AdvertisementData>()
            {
                @Override
                public AdvertisementData call(List<Currency> currencies, AdvertisementItem advertisementItem)
                {
                    AdvertisementData advertisementData = new AdvertisementData();
                    advertisementData.currencies = currencies;
                    advertisementData.advertisement = advertisementItem;
                    return advertisementData;
                }
            })
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Action1<AdvertisementData>()
            {
                @Override
                public void call(AdvertisementData data)
                {
                    advertisementData = data;
                    setAdvertisement(advertisementData.advertisement);
                    setCurrencies(advertisementData.currencies, advertisementData.advertisement);
                    hideProgress();
                }
            }, new Action1<Throwable>()
            {
                @Override
                public void call(Throwable throwable)
                {
                    hideProgress();
                    showError("No advertisement data.");
                    handleError(throwable);
                }
            }));
        }
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
        content.setVisibility(View.VISIBLE);
        progress.setVisibility(View.GONE);
    }

    private void setTradeType(TradeType tradeType)
    {
        typeSpinner.setSelection(tradeType.ordinal());
    }

    private void setMethods(List<MethodItem> methods)
    {
        MethodAdapter typeAdapter = new MethodAdapter(this, R.layout.spinner_layout, methods);
        paymentMethodSpinner.setAdapter(typeAdapter);
    }

    private void setCurrencies(List<Currency> currencies, AdvertisementItem advertisement)
    {
        CurrencyAdapter typeAdapter = new CurrencyAdapter(this, R.layout.spinner_layout, currencies);
        currencySpinner.setAdapter(typeAdapter);

        int i = 0;
        String defaultCurrency = (create)? this.getString(R.string.usd):(advertisement != null)? advertisement.currency():this.getString(R.string.usd);
        for (Currency currency : currencies) {
            if(currency.ticker.equals(defaultCurrency)) {
                currencySpinner.setSelection(i);
                break;
            }
            i++;
        }
    }

    private void createAdvertisement()
    {
        liquidityCheckBox.setChecked(false);
        smsVerifiedCheckBox.setChecked(false);
        trustedCheckBox.setChecked(false);
        activeCheckBox.setChecked(true);
        
        advertisementTypeLayout.setVisibility(View.VISIBLE);
        
        setAdvertisementType(TradeType.LOCAL_SELL);
        
        editMinimumAmountCurrency.setText(this.getString(R.string.usd));
        editMaximumAmountCurrency.setText(this.getString(R.string.usd));
        activeLayout.setVisibility(View.GONE);
    }

    private void setAdvertisement(AdvertisementItem advertisement)
    {
        currentLocation.setText(advertisement.location_string());
  
        liquidityCheckBox.setChecked(advertisement.track_max_amount());
        smsVerifiedCheckBox.setChecked(advertisement.sms_verification_required());
        trustedCheckBox.setChecked(advertisement.trusted_required());
        activeCheckBox.setChecked(advertisement.visible());
        
        messageText.setText(advertisement.message());
        editBankNameText.setText(advertisement.bank_name());
        editMinimumAmount.setText(advertisement.min_amount());
        editMaximumAmount.setText(advertisement.max_amount());
        editPaymentDetails.setText(advertisement.account_info());
        advertisementTypeLayout.setVisibility(create ? View.VISIBLE : View.GONE);

        TradeType tradeType = TradeType.valueOf(advertisement.trade_type());
        
        if(tradeType == TradeType.LOCAL_SELL || tradeType == TradeType.LOCAL_BUY) {
            editPaymentDetailsLayout.setVisibility(View.GONE);
            bankNameLayout.setVisibility(View.GONE);
            paymentMethodLayout.setVisibility(View.GONE);
        } else {
            editPaymentDetailsLayout.setVisibility(View.VISIBLE);
            paymentMethodLayout.setVisibility(create?View.VISIBLE:View.GONE);
            bankNameLayout.setVisibility(create?View.VISIBLE:View.GONE);
        }
        
        editMinimumAmountCurrency.setText(advertisement.currency());
        editMaximumAmountCurrency.setText(advertisement.currency());
        editPriceEquation.setText(advertisement.price_equation());
        marginLayout.setVisibility(View.GONE);
    }
    
    // TODO save trade type to survive through rotation
    protected void setAdvertisementType(TradeType tradeType)
    {
        if(tradeType == TradeType.LOCAL_SELL || tradeType == TradeType.LOCAL_BUY) {
            editPaymentDetailsLayout.setVisibility(View.GONE);
            bankNameLayout.setVisibility(View.GONE);
            paymentMethodLayout.setVisibility(View.GONE);
        } else {
            editPaymentDetailsLayout.setVisibility(View.VISIBLE);
            paymentMethodLayout.setVisibility(create?View.VISIBLE:View.GONE);
            bankNameLayout.setVisibility(create?View.VISIBLE:View.GONE);
        }
    }

    //bitfinexusd*USD_in_XAR*1.01
    protected void setPriceEquation()
    {
        TradeType tradeType = TradeType.values()[typeSpinner.getSelectedItemPosition()];

        String equation = Constants.DEFAULT_PRICE_EQUATION;
        Currency currency = (Currency) currencySpinner.getSelectedItem();
        if(currency == null) return; // currency values may not yet be set
        
        if(!currency.ticker.equals(Constants.DEFAULT_CURRENCY)) {
            equation = equation + "*" + Constants.DEFAULT_CURRENCY + "_in_" + currency.ticker;
        }
        
        String margin = marginText.getText().toString();
        if(!Strings.isBlank(margin)) {
            double marginValue = Doubles.convertToDouble(margin);
            double marginPercent = 1.0;
            if(tradeType == TradeType.LOCAL_BUY || tradeType == TradeType.ONLINE_BUY) {
                marginPercent = 1 - marginValue/100;
            } else {
                marginPercent = 1 + marginValue/100;
            }   
            equation = equation + "*" + marginPercent;
        } else {
            equation = equation + "*" + Constants.DEFAULT_MARGIN;
        }
        
        editPriceEquation.setText(equation);
    }
    
    public void validateChangesAndSend()
    {
        if (create && !geoLocationService.isGooglePlayServicesAvailable()) {
            return;
        }
        
        Advertisement advertisement = new Advertisement(); // used to store values 
        advertisement = advertisement.convertAdvertisementItemToAdvertisement(advertisementData.advertisement);
        
        String min = editMinimumAmount.getText().toString();
        String bankName = editBankNameText.getText().toString();
        String max = editMaximumAmount.getText().toString();
        String equation = editPriceEquation.getText().toString();
        String accountInfo = editPaymentDetails.getText().toString();
        String msg = messageText.getText().toString();

        if (TextUtils.isEmpty(equation)) {
            Toast.makeText(this, "Price equation can't be blank.", Toast.LENGTH_SHORT).show();
            return;
        } else  if (Strings.isBlank(min)) {
            Toast.makeText(this, "Enter a valid minimum amount.", Toast.LENGTH_SHORT).show();
            return;
        } else if (Strings.isBlank(max)) {
            Toast.makeText(this, "Enter a valid maximum amount.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (create) {
            advertisement.visible = true;
            advertisement.trade_type = TradeType.values()[typeSpinner.getSelectedItemPosition()];
            advertisement.online_provider = ((Method) paymentMethodSpinner.getSelectedItem()).code; // TODO code or name?
        } else {
            advertisement.ad_id = adId;
            advertisement.visible = activeCheckBox.isChecked();
        }

        advertisement.price_equation = equation;
        advertisement.min_amount = min;
        advertisement.max_amount = max;
        advertisement.bank_name = bankName;
        advertisement.message = msg;
        advertisement.account_info = accountInfo;

        advertisement.sms_verification_required = smsVerifiedCheckBox.isChecked();
        advertisement.track_max_amount = liquidityCheckBox.isChecked();
        advertisement.trusted_required = trustedCheckBox.isChecked();

        if(address == null && advertisementData == null) {
            toast("Unable to save changes, please try again");
            return;
        }
  
        if(address != null) {
            advertisement.location = TradeUtils.getAddressShort(address);
            advertisement.city = address.getLocality();
            advertisement.country_code = address.getCountryCode();
            advertisement.lon = address.getLongitude();
            advertisement.lat = address.getLatitude(); 
        }

        updateAdvertisement(advertisement, create);
    }
    
    public void setAddress(Address address)
    {
        if (address == null) return;

        this.address = address;

        currentLocation.setText(TradeUtils.getAddressShort(address));
    }

    public void showSearchLayout()
    {
        mapLayout.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.slide_out_right));
        searchLayout.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left));
        mapLayout.setVisibility(View.GONE);
        searchLayout.setVisibility(View.VISIBLE);
    }

    public void showMapLayout()
    {
        mapLayout.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left));
        searchLayout.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.slide_out_right));
        mapLayout.setVisibility(View.VISIBLE);
        searchLayout.setVisibility(View.GONE);
    }

    protected void setEditLocationAdapter(PredictAdapter adapter)
    {
        if (editLocation != null)
            editLocation.setAdapter(adapter);
    }
    
    public PredictAdapter getEditLocationAdapter()
    {
        return predictAdapter;
    }

    public void cancelChanges(Boolean create)
    {
        String message = (create)? "New advertisement canceled":"Advertisement update canceled";
        toast(message);
        finish();
    }

    public void startLocationCheck()
    {
        if(!geoLocationService.isGooglePlayServicesAvailable()) {
            missingGooglePlayServices();
            return;
        }

        if(hasLocationServices()) {

            geoLocationService.start();
            
            geoLocalSubscription = geoLocationService.subscribeToLocation(new Observer<Location>() {
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
        if(create){
            showError(getString(R.string.error_no_play_services));
        }
        
        createAlert(getString(R.string.warning_no_google_play_services_title), getString(R.string.warning_no_google_play_services), true);
    }

    public void createAlert(String title, String message, final boolean googlePlay)
    {
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
        geoDecodeObservable = bindActivity(this, geoLocationService.geoDecodeLocation(location));
        geoDecodeSubscription = geoDecodeObservable.subscribe(new Action1<List<Address>>()
        {
            @Override
            public void call(List<Address> addresses)
            {
                if (!addresses.isEmpty()) {
                    setAddress(addresses.get(0));
                }
            }
        });
    }

    public void updateAdvertisement(final Advertisement advertisement, final Boolean create)
    {
        if(create) {

            showProgressDialog(new ProgressDialogEvent("Posting advertisement..."));
            
            createAdvertisementObservable = bindActivity(this, dataService.createAdvertisement(advertisement));
            advertisementSubscription = createAdvertisementObservable.subscribe(new Observer<Advertisement>()
            {
                @Override
                public void onCompleted()
                {
                    hideProgressDialog();
                }

                @Override
                public void onError(Throwable e)
                {
                    hideProgressDialog();
                    handleError(e);
                }

                @Override
                public void onNext(Advertisement advertisement)
                {
                    toast("New advertisement posted!");
           
                    db.insert(AdvertisementItem.TABLE, AdvertisementItem.createBuilder(advertisement).build());
                            
                    finish();
                }
            });
            
        } else {

            showProgressDialog(new ProgressDialogEvent("Saving changes..."));

            updateAdvertisementObservable = bindActivity(this, dataService.updateAdvertisement(advertisement));
            advertisementSubscription = updateAdvertisementObservable.subscribe(new Observer<JSONObject>()
            {
                @Override
                public void onCompleted()
                {
                    hideProgressDialog();
                }

                @Override
                public void onError(Throwable e)
                {
                    hideProgressDialog();
                    handleError(e);
                }

                @Override
                public void onNext(JSONObject jsonObject)
                {
                    Timber.d("Updated JSON: " + jsonObject.toString());

                    if (Parser.containsError(jsonObject)) {

                        toast("Error updating advertisement visibility");

                    } else {
                        
                        updateAdvertisement(advertisement);
                    }
                }
            });
        }
    }
    
    private void updateAdvertisement(Advertisement advertisement)
    {
        //int updated = db.update(AdvertisementItem.TABLE, AdvertisementItem.createBuilder(advertisement).build(), AdvertisementItem.AD_ID + " = ?", adId);
        int updated = dbManager.updateAdvertisement(advertisement);
       
        if(updated > 0) {
            
            hideProgressDialog();
            
            //snack(getString(R.string.message_advertisement_changed), false);
            
            Intent returnIntent = new Intent();
            returnIntent.putExtra(AdvertisementActivity.EXTRA_AD_ID, adId);
            setResult(RESULT_UPDATED, returnIntent);
            
            finish();
        }
    }

    public void doAddressLookup(String locationName)
    {
        geoLocationObservable = bindActivity(this, geoLocationService.geoGetLocationFromName(locationName));
        geoLocalSubscription = geoLocationObservable.subscribe(new Action1<List<Address>>()
        {
            @Override
            public void call(List<Address> addresses)
            {
                if (!addresses.isEmpty()) {
                    getEditLocationAdapter().replaceWith(addresses);
                }
            }
        }, new Action1<Throwable>()
        {
            @Override
            public void call(Throwable throwable)
            {
                handleError(new Throwable(getString(R.string.error_unable_load_address)), false);
            }
        });
    }
}
