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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Address;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.squareup.sqlbrite.BriteDatabase;
import com.thanksmister.bitcoin.localtrader.BaseActivity;
import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.constants.Constants;
import com.thanksmister.bitcoin.localtrader.data.api.model.Advertisement;
import com.thanksmister.bitcoin.localtrader.data.api.model.ExchangeCurrency;
import com.thanksmister.bitcoin.localtrader.data.api.model.RetroError;
import com.thanksmister.bitcoin.localtrader.data.api.model.TradeType;
import com.thanksmister.bitcoin.localtrader.data.database.AdvertisementItem;
import com.thanksmister.bitcoin.localtrader.data.database.DbManager;
import com.thanksmister.bitcoin.localtrader.data.database.MethodItem;
import com.thanksmister.bitcoin.localtrader.data.services.DataService;
import com.thanksmister.bitcoin.localtrader.data.services.DataServiceUtils;
import com.thanksmister.bitcoin.localtrader.data.services.ExchangeService;
import com.thanksmister.bitcoin.localtrader.data.services.GeoLocationService;
import com.thanksmister.bitcoin.localtrader.events.AlertDialogEvent;
import com.thanksmister.bitcoin.localtrader.events.ProgressDialogEvent;
import com.thanksmister.bitcoin.localtrader.ui.components.CurrencyAdapter;
import com.thanksmister.bitcoin.localtrader.ui.components.MethodAdapter;
import com.thanksmister.bitcoin.localtrader.ui.components.PredictAdapter;
import com.thanksmister.bitcoin.localtrader.ui.components.SpinnerAdapter;
import com.thanksmister.bitcoin.localtrader.utils.Doubles;
import com.thanksmister.bitcoin.localtrader.utils.Parser;
import com.thanksmister.bitcoin.localtrader.utils.Strings;
import com.thanksmister.bitcoin.localtrader.utils.TradeUtils;
import com.trello.rxlifecycle.ActivityEvent;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import butterknife.Optional;
import retrofit.RetrofitError;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;
import timber.log.Timber;

public class EditActivity extends BaseActivity
{
    public static final String EXTRA_ADDRESS = "com.thanksmister.extras.EXTRA_ADDRESS";
    public static final String EXTRA_CREATE = "com.thanksmister.extras.EXTRA_CREATE";
    public static final String EXTRA_AD_ID = "com.thanksmister.extras.EXTRA_AD_ID";
    public static final String EXTRA_ADVERTISEMENT = "com.thanksmister.extras.EXTRA_ADVERTISEMENT";

    public static final int REQUEST_CODE = 10937;
    public static final int RESULT_UPDATED = 72322;
    public static final int RESULT_CREATED = 72323;

    @Inject
    DataService dataService;

    @Inject
    ExchangeService exchangeService;

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
    
    @InjectView(R.id.currencyLayout)
    View currencyLayout;

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
        startLocationCheck(); // get new location
    }

    private boolean create;
    private PredictAdapter predictAdapter;
    private Address address;
    private String adId;
    private AdvertisementItem advertisementItem;

    private CompositeSubscription subscriptions = new CompositeSubscription();
    private Subscription geoLocalSubscription = Subscriptions.empty();
    private Subscription geoDecodeSubscription = Subscriptions.empty();
    private Subscription advertisementSubscription = Subscriptions.empty();
    private Observable<List<ExchangeCurrency>> currencyObservable;
    
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
            if(savedInstanceState.containsKey(EXTRA_ADVERTISEMENT))
                advertisementItem = savedInstanceState.getParcelable(EXTRA_ADVERTISEMENT);
        }

        if (toolbar != null) {
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle((create) ? "Post new advertisement" : "Edit advertisement");
            setToolBarMenu(toolbar);
        }

        String[] typeTitles = getResources().getStringArray(R.array.list_advertisement_type_spinner);
        List<String> typeList = new ArrayList<>(Arrays.asList(typeTitles));

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
                ExchangeCurrency exchange = (ExchangeCurrency) currencySpinner.getAdapter().getItem(i);
                editMinimumAmountCurrency.setText(exchange.getName());
                editMaximumAmountCurrency.setText(exchange.getName());
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
        
        swipeLayout.setEnabled(false); // freeze swipe ability

        currencyObservable = exchangeService.getMarketTickers().cache();
    }

    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putParcelable(EXTRA_ADDRESS, address);
        outState.putBoolean(EXTRA_CREATE, create);
        outState.putString(EXTRA_AD_ID, adId);
        
        if(advertisementItem != null) {
            outState.putParcelable(EXTRA_ADVERTISEMENT, advertisementItem); 
        }
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
        if (toolbar != null)
            toolbar.inflateMenu(R.menu.edit);

        return true;
    }

    @Override
    public void onResume()
    {
        super.onResume();
        
        if (create) {
            if (geoLocationService.isGooglePlayServicesAvailable()) {
                subScribeData();
            } else {
                startLocationCheck();
            }
        } else {
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

        if(progress != null)
            progress.clearAnimation();

        if(content != null)
            content.clearAnimation();
    }

    public void subScribeData()
    {
        subscriptions = new CompositeSubscription(); // must initiate each time
      
        subscriptions.add(dbManager.methodSubSetQuery().subscribe(new Action1<List<MethodItem>>()
        {
            @Override
            public void call(List<MethodItem> methodItems)
            {
                setMethods(methodItems);
            }
        }));

        if (create) {
            
            subscriptions.add(currencyObservable
                    .timeout(20, TimeUnit.SECONDS)
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Action1<List<ExchangeCurrency>>()
                    {
                        @Override
                        public void call(List<ExchangeCurrency> currencies)
                        {
                            //showContent(true);
                            createAdvertisement();
                            setCurrencies(currencies, null);
                            startLocationCheck(); // start location check
                        }
                    }, new Action1<Throwable>()
                    {
                        @Override
                        public void call(Throwable throwable)
                        {
                            snackError("Unable to load currencies, using default...");
                            createAdvertisement();
                            setCurrencies(new ArrayList<ExchangeCurrency>(), null);
                        }
                    }));
            
        } else {

            subscriptions.add(dbManager.advertisementItemQuery(adId).subscribe(new Action1<AdvertisementItem>()
            {
                @Override
                public void call(AdvertisementItem advertisement)
                {
                    showContent(true);
                    advertisementItem = advertisement; // save reference
                    setAdvertisement(advertisementItem);
                }
            }, new Action1<Throwable>()
            {
                @Override
                public void call(Throwable throwable)
                {
                    snackError("No advertisement data.");
                    reportError(throwable);
                }
            }));
        }
    }

    public void showContent(final boolean show)
    {
        if (progress == null || content == null)
            return;

        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

        progress.setVisibility(show ? View.GONE : View.VISIBLE);
        progress.animate().setDuration(shortAnimTime).alpha(show ? 0 : 1).setListener(new AnimatorListenerAdapter()
        {
            @Override
            public void onAnimationEnd(Animator animation)
            {
                if (progress != null)
                    progress.setVisibility(show ? View.GONE : View.VISIBLE);
            }
        });

        content.setVisibility(show ? View.VISIBLE : View.GONE);
        content.animate().setDuration(shortAnimTime).alpha(show ? 1 : 0).setListener(new AnimatorListenerAdapter()
        {
            @Override
            public void onAnimationEnd(Animator animation)
            {
                if (content != null)
                    content.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });
    }

    private void setTradeType(TradeType tradeType)
    {
        typeSpinner.setSelection(tradeType.ordinal());
    }

    private void setMethods(List<MethodItem> methods)
    {
        // TODO remove methods
        MethodAdapter typeAdapter = new MethodAdapter(this, R.layout.spinner_layout, methods);
        paymentMethodSpinner.setAdapter(typeAdapter);
    }

    /**
     * Set the currencies to be used for a new advertisement
     * @param currencies
     * @param advertisement
     */
    private void setCurrencies(List<ExchangeCurrency> currencies, AdvertisementItem advertisement)
    {
        // handle error case
        String currencyPreference = exchangeService.getExchangeCurrency();
        String defaultCurrency = (create) ? currencyPreference : (advertisement != null) ? advertisement.currency() : this.getString(R.string.usd);

        if(currencies.isEmpty()) {
            ExchangeCurrency exchangeCurrency = new ExchangeCurrency(currencyPreference, "https://api.bitcoinaverage.com/ticker/" + defaultCurrency);
            currencies.add(exchangeCurrency); // just revert back to USD if we can
        }

        int i = 0;
        for (ExchangeCurrency currency : currencies) {
            if (currency.getName().equals(defaultCurrency)) {
                currencySpinner.setSelection(i);
                break;
            }
            i++;
        }
        
        CurrencyAdapter typeAdapter = new CurrencyAdapter(this, R.layout.spinner_layout, currencies);
        currencySpinner.setAdapter(typeAdapter);
    }

    /**
     * Show the view for new advertisement
     */
    private void createAdvertisement()
    {
        liquidityCheckBox.setChecked(false);
        smsVerifiedCheckBox.setChecked(false);
        trustedCheckBox.setChecked(false);
        activeCheckBox.setChecked(true);

        advertisementTypeLayout.setVisibility(View.VISIBLE);
        currencyLayout.setVisibility(View.VISIBLE);
        
        setAdvertisementType(TradeType.LOCAL_SELL);

        editMinimumAmountCurrency.setText(this.getString(R.string.usd));
        editMaximumAmountCurrency.setText(this.getString(R.string.usd));
        activeLayout.setVisibility(View.GONE);
    }

    /**
     * Set the advertisement from the database
     * @param advertisement
     */
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
        
        advertisementTypeLayout.setVisibility(View.GONE);
        currencyLayout.setVisibility(View.GONE);

        TradeType tradeType = TradeType.valueOf(advertisement.trade_type());

        if (tradeType == TradeType.LOCAL_SELL || tradeType == TradeType.LOCAL_BUY) {
            editPaymentDetailsLayout.setVisibility(View.GONE);
            bankNameLayout.setVisibility(View.GONE);
            paymentMethodLayout.setVisibility(View.GONE);
        } else {
            editPaymentDetailsLayout.setVisibility(View.VISIBLE);
            paymentMethodLayout.setVisibility(View.GONE);
            bankNameLayout.setVisibility(View.GONE);
        }

        editMinimumAmountCurrency.setText(advertisement.currency());
        editMaximumAmountCurrency.setText(advertisement.currency());
        editPriceEquation.setText(advertisement.price_equation());
        marginLayout.setVisibility(View.GONE);
    }

    // TODO save trade type to survive through rotation
    protected void setAdvertisementType(TradeType tradeType)
    {
        if (tradeType == TradeType.LOCAL_SELL || tradeType == TradeType.LOCAL_BUY) {
            editPaymentDetailsLayout.setVisibility(View.GONE);
            bankNameLayout.setVisibility(View.GONE);
            paymentMethodLayout.setVisibility(View.GONE);
        } else {
            editPaymentDetailsLayout.setVisibility(View.VISIBLE);
            paymentMethodLayout.setVisibility(create ? View.VISIBLE : View.GONE);
            bankNameLayout.setVisibility(create ? View.VISIBLE : View.GONE);
        }
    }

    protected void setPriceEquation()
    {
        TradeType tradeType = TradeType.values()[typeSpinner.getSelectedItemPosition()];

        String equation = Constants.DEFAULT_PRICE_EQUATION;
        ExchangeCurrency currency = (ExchangeCurrency) currencySpinner.getSelectedItem();
        if (currency == null) return; // currency values may not yet be set

        if (!currency.getName().equals(Constants.DEFAULT_CURRENCY)) {
            equation = equation + "*" + Constants.DEFAULT_CURRENCY + "_in_" + currency.getName();
        }

        String margin = marginText.getText().toString();
        if (!Strings.isBlank(margin)) {

            double marginValue = 1.0;
            try {
                marginValue = Doubles.convertToDouble(margin);
            } catch (Exception e) {
                reportError(e);
            }
            
            double marginPercent = 1.0;
            if (tradeType == TradeType.LOCAL_BUY || tradeType == TradeType.ONLINE_BUY) {
                marginPercent = 1 - marginValue / 100;
            } else {
                marginPercent = 1 + marginValue / 100;
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
            snackError(getString(R.string.error_no_play_services));
            return;
        }

        String min = editMinimumAmount.getText().toString();
        String bankName = editBankNameText.getText().toString();
        String max = editMaximumAmount.getText().toString();
        String equation = editPriceEquation.getText().toString();
        String accountInfo = editPaymentDetails.getText().toString();
        String msg = messageText.getText().toString();

        if (TextUtils.isEmpty(equation)) {
            toast("Price equation can't be blank.");
            return;
        } else if (Strings.isBlank(min)) {
            toast("Enter a valid minimum amount.");
            return;
        } else if (Strings.isBlank(max)) {
            toast("Enter a valid maximum amount.");
            return;
        }

        // used to store values for service call
        Advertisement editedAdvertisement = new Advertisement();

        TradeType tradeType = TradeType.values()[typeSpinner.getSelectedItemPosition()];

        if (create) {

            if (address == null) {
                snackError("Unable to save changes.");
                return;
            }
          
            editedAdvertisement.visible = true;
            editedAdvertisement.currency =  ((ExchangeCurrency) currencySpinner.getSelectedItem()).getName();
            editedAdvertisement.trade_type = tradeType;
            
            Timber.d("Trade Type: " + tradeType.name());
            
            String onlineProvider = ((MethodItem) paymentMethodSpinner.getSelectedItem()).code();
            
            Timber.d("Online Provider: " + onlineProvider);
            
            if(tradeType == TradeType.ONLINE_BUY || tradeType ==TradeType.ONLINE_SELL) {
                editedAdvertisement.online_provider = onlineProvider;
            } else {
                editedAdvertisement.online_provider = "NATIONAL_BANK";
            }
            
        } else {

            if (advertisementItem == null) {
                snackError("Unable to save changes.");
                return;
            }
            
            // convert data to editable advertisement if not creating new advertisement
            editedAdvertisement = editedAdvertisement.convertAdvertisementItemToAdvertisement(advertisementItem);
            editedAdvertisement.ad_id = adId;
            editedAdvertisement.visible = activeCheckBox.isChecked();
        }

        editedAdvertisement.message = msg;
        editedAdvertisement.price_equation = equation;
        editedAdvertisement.min_amount = String.valueOf(TradeUtils.convertCurrencyAmount(min));
        editedAdvertisement.max_amount = String.valueOf(TradeUtils.convertCurrencyAmount(max));
        
        Timber.d("Min: " +  editedAdvertisement.min_amount);
        Timber.d("Max: " + editedAdvertisement.max_amount);

        // only online trades have these values
        if(tradeType == TradeType.ONLINE_BUY || tradeType ==TradeType.ONLINE_SELL) {
            editedAdvertisement.bank_name = bankName;
            editedAdvertisement.account_info = accountInfo;
        } 

        editedAdvertisement.sms_verification_required = smsVerifiedCheckBox.isChecked();
        editedAdvertisement.track_max_amount = liquidityCheckBox.isChecked();
        editedAdvertisement.trusted_required = trustedCheckBox.isChecked();

        // we can only add the address if its available and it should be
        if (address != null) {
            
            editedAdvertisement.location = TradeUtils.getAddressShort(address);
            editedAdvertisement.city = address.getLocality();
            editedAdvertisement.country_code = address.getCountryCode();
            editedAdvertisement.lon = address.getLongitude();
            editedAdvertisement.lat = address.getLatitude();
            
        }

        updateAdvertisement(editedAdvertisement, create);
    }

    public void setAddress(Address address)
    {
        if (address == null || currentLocation == null) return;

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
        String message = (create) ? "New advertisement canceled" : "Advertisement update canceled";
        toast(message);
        finish();
    }

    public void startLocationCheck()
    {
        if (!geoLocationService.isGooglePlayServicesAvailable()) {
            showGooglePlayServicesError();
            return;
        }

        if (!isNetworkConnected()) {
            handleError(new Throwable(getString(R.string.error_no_internet)), true);
            return;
        }

        if (hasLocationServices()) {
            getCurrentLocation();
        } else {
            showEnableLocationDialog();
        }
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
                .compose(this.<Location>bindUntilEvent(ActivityEvent.PAUSE))
                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.newThread())
                .subscribe(new Action1<Location>()
                {
                    @Override
                    public void call(final Location location)
                    {

                        runOnUiThread(new Runnable()
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

                        runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                reportError(throwable);
                                handleError(new Throwable(getString(R.string.error_unable_load_address)), true);
                            }
                        });

                    }
                });
    }

    private void getAddressFromLocation(final Location location)
    {
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
                .compose(this.<Address>bindUntilEvent(ActivityEvent.PAUSE))
                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.newThread())
                .subscribe(new Action1<Address>()
                {
                    @Override
                    public void call(final Address address)
                    {
                        runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                if(address != null) {
                                    showContent(true);
                                    setAddress(address);
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
                        runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
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
                .compose(this.<Address>bindUntilEvent(ActivityEvent.PAUSE))
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Address>()
                {
                    @Override
                    public void call(Address address)
                    {
                        if (address != null) {
                            showContent(true);
                            setAddress(address);
                        } else {
                            handleError(new Throwable(getString(R.string.error_unable_load_address)), true);
                        }
                    }
                }, new Action1<Throwable>()
                {
                    @Override
                    public void call(Throwable throwable)
                    {
                        reportError(throwable);
                        handleError(new Throwable(getString(R.string.error_unable_load_address)), true);
                    }
                });
    }

    private void showEnableLocationDialog()
    {
        createAlert(getString(R.string.warning_no_location_services_title), getString(R.string.warning_no_location_active), false);
    }

    private boolean isNetworkConnected()
    {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        return networkInfo != null;
    }

    private void missingGooglePlayServices()
    {
        createAlert(getString(R.string.warning_no_google_play_services_title), getString(R.string.warning_no_google_play_services), true);
    }

    public void createAlert(String title, String message, final boolean googlePlay)
    {
        int positiveButton = (googlePlay) ? R.string.button_install : R.string.button_enable;
        new AlertDialog.Builder(this)
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

    public boolean hasLocationServices()
    {
        return (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER));
    }

    public void updateAdvertisement(final Advertisement advertisement, final Boolean create)
    {
        if (create) {

            showProgressDialog(new ProgressDialogEvent("Posting advertisement..."));

            Observable<JSONObject> createAdvertisementObservable = dataService.createAdvertisement(advertisement);
            advertisementSubscription = createAdvertisementObservable
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<JSONObject>()
                    {
                        @Override
                        public void onCompleted()
                        {
                            hideProgressDialog();
                        }

                        @Override
                        public void onError(final Throwable e)
                        {
                            runOnUiThread(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    hideProgressDialog();
                                    handleLocalError(e);
                                }
                            });
                        }

                        @Override
                        public void onNext(final JSONObject jsonObject)
                        {
                            runOnUiThread(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    toast("New advertisement posted!");
                                    
                                    if (Parser.containsError(jsonObject)) {

                                        RetroError error = Parser.parseError(jsonObject);
                                        showAlertDialog(new AlertDialogEvent("Error Updating Advertisement", error.getMessage()));
                                        
                                    } else {
                                        setResult(RESULT_CREATED); // hard refresh
                                        finish();
                                    }
                                }
                            });
                        }
                    });

        } else {

            showProgressDialog(new ProgressDialogEvent("Saving changes..."));

            Observable<JSONObject> updateAdvertisementObservable = dataService.updateAdvertisement(advertisement);
            advertisementSubscription = updateAdvertisementObservable
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<JSONObject>()
                    {
                        @Override
                        public void onCompleted()
                        {
                            hideProgressDialog();
                        }

                        @Override
                        public void onError(final Throwable e)
                        {
                            runOnUiThread(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    hideProgressDialog();
                                    handleLocalError(e);
                                }
                            });
                        }

                        @Override
                        public void onNext(final JSONObject jsonObject)
                        {
                            Timber.d("Updated JSON: " + jsonObject.toString());

                            runOnUiThread(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    if (Parser.containsError(jsonObject)) {
                                        
                                        RetroError error = Parser.parseError(jsonObject);

                                        showAlertDialog(new AlertDialogEvent("Error Updating Advertisement", error.getMessage()));
                                        
                                    } else {
                                        
                                        updateAdvertisement(advertisement);
                                    }
                                }
                            });
                        }
                    });
        }
    }
    
    // TODO let's clean this up and move to central location to get the errors
    private void handleLocalError(Throwable throwable)
    {
        Timber.d("handleLocalError 404: " + DataServiceUtils.isHttp404Error(throwable));
        
        if(DataServiceUtils.isHttp400Error(throwable)) {

            Timber.d("handleLocalError");
            
            if (throwable instanceof RetrofitError) {
                
                RetrofitError retroError = (RetrofitError) throwable;
                
                if(retroError.getResponse() != null) {
                    
                    JSONObject jsonObject = Parser.parseResponseToJsonObject(retroError.getResponse());
                    RetroError error = Parser.parseError(jsonObject);

                    showAlertDialog(new AlertDialogEvent("Error Updating Advertisement", error.getMessage()));
                    Timber.e(jsonObject.toString());
                }
            }
            
        } else {
            
            handleError(throwable); // hand off to global error handling
        }
    }

    private void updateAdvertisement(Advertisement advertisement)
    {
        int updated = dbManager.updateAdvertisement(advertisement);

        if (updated > 0) {

            hideProgressDialog();

            toast(getString(R.string.message_advertisement_changed));

            Intent returnIntent = new Intent();
            returnIntent.putExtra(AdvertisementActivity.EXTRA_AD_ID, adId);
            setResult(RESULT_UPDATED, returnIntent);

            finish();
        }
    }

    public void doAddressLookup(String locationName)
    {
        Observable<List<Address>> geoLocationObservable = geoLocationService.geoGetLocationFromName(locationName);
        geoLocalSubscription = geoLocationObservable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<List<Address>>()
                {
                    @Override
                    public void call(final List<Address> addresses)
                    {
                        runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                if (!addresses.isEmpty()) {
                                    getEditLocationAdapter().replaceWith(addresses);
                                }
                            }
                        });
                    }
                }, new Action1<Throwable>()
                {
                    @Override
                    public void call(final Throwable throwable)
                    {
                        runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                Timber.d(throwable.getLocalizedMessage());
                                handleError(new Throwable(getString(R.string.error_unable_load_address)), true);
                            }
                        });
                    }
                });
    }

    private void showGooglePlayServicesError()
    {
        int result = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);

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
