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

package com.thanksmister.bitcoin.localtrader.ui.adapters;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
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
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationRequest;
import com.squareup.sqlbrite.BriteDatabase;
import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.constants.Constants;
import com.thanksmister.bitcoin.localtrader.data.api.model.Advertisement;
import com.thanksmister.bitcoin.localtrader.data.api.model.ExchangeCurrency;
import com.thanksmister.bitcoin.localtrader.data.api.model.RetroError;
import com.thanksmister.bitcoin.localtrader.data.api.model.TradeType;
import com.thanksmister.bitcoin.localtrader.data.database.AdvertisementItem;
import com.thanksmister.bitcoin.localtrader.data.database.DbManager;
import com.thanksmister.bitcoin.localtrader.data.database.ExchangeCurrencyItem;
import com.thanksmister.bitcoin.localtrader.data.database.MethodItem;
import com.thanksmister.bitcoin.localtrader.data.services.DataService;
import com.thanksmister.bitcoin.localtrader.data.services.DataServiceUtils;
import com.thanksmister.bitcoin.localtrader.data.services.ExchangeService;
import com.thanksmister.bitcoin.localtrader.data.services.GeoLocationService;
import com.thanksmister.bitcoin.localtrader.events.AlertDialogEvent;
import com.thanksmister.bitcoin.localtrader.events.ProgressDialogEvent;
import com.thanksmister.bitcoin.localtrader.ui.BaseActivity;
import com.thanksmister.bitcoin.localtrader.utils.CurrencyUtils;
import com.thanksmister.bitcoin.localtrader.utils.Doubles;
import com.thanksmister.bitcoin.localtrader.utils.Parser;
import com.thanksmister.bitcoin.localtrader.utils.SearchUtils;
import com.thanksmister.bitcoin.localtrader.utils.Strings;
import com.thanksmister.bitcoin.localtrader.utils.TradeUtils;
import com.trello.rxlifecycle.ActivityEvent;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import butterknife.Optional;
import pl.charmas.android.reactivelocation.ReactiveLocationProvider;
import retrofit.RetrofitError;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;
import timber.log.Timber;

import static android.view.View.GONE;
import static com.thanksmister.bitcoin.localtrader.data.services.GeoLocationService.MAX_ADDRESSES;
import static com.thanksmister.bitcoin.localtrader.ui.fragments.SearchFragment.REQUEST_CHECK_SETTINGS;

public class EditActivity extends BaseActivity {
    public static final String EXTRA_ADDRESS = "com.thanksmister.extras.EXTRA_ADDRESS";
    public static final String EXTRA_CREATE = "com.thanksmister.extras.EXTRA_CREATE";
    public static final String EXTRA_AD_ID = "com.thanksmister.extras.EXTRA_AD_ID";
    public static final String EXTRA_ADVERTISEMENT = "com.thanksmister.extras.EXTRA_ADVERTISEMENT";
    public static final int REQUEST_GOOGLE_PLAY_SERVICES = 1972;
    public static final int REQUEST_CODE = 10937;
    public static final int RESULT_UPDATED = 72322;
    public static final int RESULT_CREATED = 72323;

    @Inject
    DataService dataService;

    @Inject
    ExchangeService exchangeService;

    @Inject
    GeoLocationService geoLocationService;

    @Inject
    DbManager dbManager;

    @Inject
    LocationManager locationManager;

    @Inject
    BriteDatabase db;

    @Optional
    @InjectView(R.id.editToolBar)
    Toolbar toolbar;

    @InjectView(R.id.liquidityCheckBox)
    CheckBox liquidityCheckBox;

    @InjectView(R.id.trustedCheckBox)
    CheckBox trustedCheckBox;

    @InjectView(R.id.smsVerifiedCheckBox)
    CheckBox smsVerifiedCheckBox;

    @InjectView(R.id.identifiedCheckBox)
    CheckBox identifiedCheckBox;

    @InjectView(R.id.activeCheckBox)
    CheckBox activeCheckBox;

    @InjectView(R.id.currencyLayout)
    View currencyLayout;

    @InjectView(R.id.editLocationTextAutoComplete)
    AutoCompleteTextView editLocation;

    @InjectView(R.id.locationText)
    TextView locationText;
    
    @InjectView(R.id.bankNameTitle)
    TextView bankNameTitle;

    @InjectView(R.id.editPriceEquation)
    EditText editPriceEquation;

    @InjectView(R.id.editMinimumAmount)
    EditText editMinimumAmount;

    @InjectView(R.id.editMaximumAmount)
    EditText editMaximumAmount;

    @InjectView(R.id.editPaymentDetails)
    EditText editPaymentDetails;

    @InjectView(R.id.editBankName)
    EditText editBankNameText;

    @InjectView(R.id.paymentMethodLayout)
    View paymentMethodLayout;

    @InjectView(R.id.editPaymentDetailsLayout)
    View editPaymentDetailsLayout;

    @InjectView(R.id.newBuyerLimitLayout)
    View newBuyerLimitLayout;

    @InjectView(R.id.newBuyerLimitText)
    EditText newBuyerLimitText;

    @InjectView(R.id.minimumFeedbackLayout)
    View minimumFeedbackLayout;

    @InjectView(R.id.minimumFeedbackText)
    EditText minimumFeedbackText;

    @InjectView(R.id.minimumVolumeLayout)
    View minimumVolumeLayout;

    @InjectView(R.id.minimumVolumeText)
    EditText minimumVolumeText;

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

    @InjectView(R.id.editMessageText)
    EditText messageText;

    @InjectView(R.id.detailsPhoneNumberLayout)
    View detailsPhoneNumberLayout;

    @InjectView(R.id.detailsPhoneNumberDescription)
    EditText detailsPhoneNumberDescription;
    
    @InjectView(R.id.detailsPhoneNumber)
    EditText detailsPhoneNumber;

    @InjectView(R.id.paymentMethodSpinner)
    Spinner paymentMethodSpinner;

    @InjectView(R.id.typeSpinner)
    Spinner typeSpinner;

    @InjectView(R.id.editProgress)
    View progress;

    @InjectView(R.id.editContent)
    View content;

    @InjectView(R.id.saveButton)
    Button saveButton;

    @OnClick(R.id.clearButton)
    public void clearButtonClicked() {
        showEditTextLayout();
        editLocation.setText("");
    }

    @OnClick(R.id.saveButton)
    public void saveButtonClicked() {
        validateChangesAndSend();
    }

    private boolean create;
    private PredictAdapter predictAdapter;
    private Address address;
    private String adId;
    private AdvertisementItem advertisementItem;
    
    private double latitude;
    private double longitude;
    private String location;
    private String city;
    private String countryCode;
    private String onlineProvider;
    private TradeType tradeType;

    private CompositeSubscription subscriptions = new CompositeSubscription();
    private Subscription geoLocationFromNameSubscription = Subscriptions.empty();
    private Subscription geoLocationSubscription;
    private Subscription advertisementSubscription = Subscriptions.empty();
    private Subscription currencySubscription = Subscriptions.empty();

    public static Intent createStartIntent(Context context, Boolean create, String adId) {
        Intent intent = new Intent(context, EditActivity.class);
        intent.putExtra(EXTRA_CREATE, create);
        intent.putExtra(EXTRA_AD_ID, adId);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        
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
            if (savedInstanceState.containsKey(EXTRA_ADVERTISEMENT))
                advertisementItem = savedInstanceState.getParcelable(EXTRA_ADVERTISEMENT);
        }

        if (toolbar != null) {
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle((create) ? "Post trade" : "Edit trade");
        }

        String[] typeTitles = getResources().getStringArray(R.array.list_advertisement_type_spinner);
        List<String> typeList = new ArrayList<>(Arrays.asList(typeTitles));

        SpinnerAdapter typeAdapter = new SpinnerAdapter(this, R.layout.spinner_layout, typeList);
        typeSpinner.setAdapter(typeAdapter);
        typeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                setPriceEquation();
                tradeType = TradeType.values()[i];
                setAdvertisementType(tradeType);
                if(TradeUtils.isOnlineTrade(tradeType)) {
                    showOnlineOptions(tradeType);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        currencySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                ExchangeCurrency exchange = (ExchangeCurrency) currencySpinner.getAdapter().getItem(i);
                editMinimumAmountCurrency.setText(exchange.getCurrency());
                editMaximumAmountCurrency.setText(exchange.getCurrency());
                if (create) {
                    if(TradeUtils.ALTCOIN_ETH.equals(onlineProvider)) {
                        setEthereumPriceEquation();
                    } else {
                        setPriceEquation(); 
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        paymentMethodSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

                onlineProvider = ((MethodItem) paymentMethodSpinner.getSelectedItem()).code();
                
                if(TradeUtils.ALTCOIN_ETH.equals(onlineProvider)) {
                    currencyLayout.setVisibility(GONE);
                    bankNameLayout.setVisibility(GONE);
                    editPaymentDetailsLayout.setVisibility(View.GONE);
                    editMinimumAmountCurrency.setText(getString(R.string.eth));
                    editMaximumAmountCurrency.setText(getString(R.string.eth));
                } else {
                    String currency = ((ExchangeCurrency) currencySpinner.getSelectedItem()).getCurrency();
                    editMinimumAmountCurrency.setText(currency);
                    editMaximumAmountCurrency.setText(currency);
                    currencyLayout.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        editLocation.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Address address = predictAdapter.getItem(i);
                editLocation.setText("");
                displayAddress(address);
                saveAddress(address);
            }
        });

        editLocation.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                if (!Strings.isBlank(charSequence)) {
                    doAddressLookup(charSequence.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        marginText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                if(TradeUtils.ALTCOIN_ETH.equals(onlineProvider)) {
                    setEthereumPriceEquation();
                } else {
                    setPriceEquation();
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        predictAdapter = new PredictAdapter(EditActivity.this, new ArrayList<Address>());
        setEditLocationAdapter(predictAdapter);

        if (address != null && create) {
            displayAddress(address);
        } else if (create) {
            editLocation.setVisibility(View.VISIBLE);
            editLocation.requestFocus();
        }

        saveButton.setText(create ? "CREATE" : "UPDATE");

        // set filter to restrict minimum feedback score
        minimumFeedbackText.setFilters(new InputFilter[]{new InputFilterMinMax("0", "100")});
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(EXTRA_ADDRESS, address);
        outState.putBoolean(EXTRA_CREATE, create);
        outState.putString(EXTRA_AD_ID, adId);

        if (advertisementItem != null) {
            outState.putParcelable(EXTRA_ADVERTISEMENT, advertisementItem);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else if (item.getItemId() == R.id.action_location) {
            getLasKnownLocation();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        cancelChanges(create);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (toolbar != null)
            toolbar.inflateMenu(R.menu.edit);

        return true;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!isNetworkConnected()) {
            handleError(new Throwable(getString(R.string.error_no_internet)));
            return;
        }

        if (!checkPlayServices()) {
            showGoogleAPIResolveError();
            return;
        }

        subScribeData();
    }

    @Override
    public void onPause() {
        super.onPause();

        if (subscriptions != null) {
            subscriptions.unsubscribe();
            subscriptions = null;
        }

        if (currencySubscription != null) {
            currencySubscription.unsubscribe();
            currencySubscription = null;
        }

        if (geoLocationSubscription != null) {
            geoLocationSubscription.unsubscribe();
            geoLocationSubscription = null;
        }

        if (geoLocationFromNameSubscription != null) {
            geoLocationFromNameSubscription.unsubscribe();
            geoLocationFromNameSubscription = null;
        }

        if (advertisementSubscription != null) {
            advertisementSubscription.unsubscribe();
            advertisementSubscription = null;
        }

        if (progress != null)
            progress.clearAnimation();

        if (content != null)
            content.clearAnimation();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode == Activity.RESULT_OK) {
                    //
                } else {
                    toast(getString(R.string.toast_edit_canceled));
                    finish();
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    /**
     * Show only if its an online trade type
     * @param tradeType
     */
    private void showOnlineOptions(TradeType tradeType) {
        
        detailsPhoneNumberLayout.setVisibility(View.GONE);
        
        if (tradeType == TradeType.ONLINE_SELL) {
            switch (onlineProvider) {
                case TradeUtils.INTERNATIONAL_WIRE_SWIFT:
                    bankNameTitle.setText("Bank SWIFT");
                    break;
                case TradeUtils.CASH_DEPOSIT:
                case TradeUtils.SPECIFIC_BANK:
                case TradeUtils.NATIONAL_BANK:
                    bankNameTitle.setText("Bank name (required)");
                    break;
                case TradeUtils.OTHER:
                case TradeUtils.OTHER_REMITTANCE:
                case TradeUtils.OTHER_PRE_PAID_DEBIT:
                case TradeUtils.OTHER_ONLINE_WALLET_GLOBAL:
                case TradeUtils.OTHER_ONLINE_WALLET:
                    bankNameTitle.setText("Payment method name");
                    break;
                case TradeUtils.GIFT_CARD_CODE:
                    bankNameTitle.setText("Gift Card Issuer: [AMC Theatres, Airbnb, American Express, Best Buy, Dell, GA2, GameStop, Google Play, Groupon, Home Depot, Lowe, Lyft, Microsoft Windows Store, Netflix, Other, Papa John's Pizza, PlayStation Store, Regal Cinemas, Skype Credit, Target, Uber, Whole Foods Market, Wolt, Xbox]");
                    break;
                case TradeUtils.QIWI:
                case TradeUtils.MOBILEPAY_DANSKE_BANK_NO:
                case TradeUtils.MOBILEPAY_DANSKE_BANK:
                case TradeUtils.MOBILEPAY_DANSKE_BANK_DK:
                case TradeUtils.PAYTM:
                case TradeUtils.SWISH:
                    bankNameLayout.setVisibility(GONE);
                    detailsPhoneNumberLayout.setVisibility(View.VISIBLE);
                    detailsPhoneNumberDescription.setText("Used for receiving payments.");
                    break;
                default:
                    bankNameLayout.setVisibility(GONE);
                    break;
            }
        } else if (tradeType == TradeType.ONLINE_BUY) {
            switch (onlineProvider) {
                case TradeUtils.NATIONAL_BANK:
                case TradeUtils.CASH_DEPOSIT:
                case TradeUtils.SPECIFIC_BANK:
                    bankNameTitle.setText("Bank name (required)");
                    break;
                case TradeUtils.OTHER:
                case TradeUtils.OTHER_REMITTANCE:
                case TradeUtils.OTHER_PRE_PAID_DEBIT:
                case TradeUtils.OTHER_ONLINE_WALLET_GLOBAL:
                case TradeUtils.OTHER_ONLINE_WALLET:
                    bankNameTitle.setText("Payment method name");
                    break;
                case TradeUtils.INTERNATIONAL_WIRE_SWIFT:
                    bankNameTitle.setText("Bank SWIFT");
                    break;
                case TradeUtils.GIFT_CARD_CODE:
                    bankNameTitle.setText("Gift Card Issuer: [AMC Theatres, Airbnb, American Express, Best Buy, Dell, GA2, GameStop, Google Play, Groupon, Home Depot, Lowe, Lyft, Microsoft Windows Store, Netflix, Other, Papa John's Pizza, PlayStation Store, Regal Cinemas, Skype Credit, Target, Uber, Whole Foods Market, Wolt, Xbox]");
                    break;
                default:
                    bankNameLayout.setVisibility(GONE);
                    break;
            }
        }
    }

    protected void showEditTextLayout() {
        if (locationText.isShown()) {
            locationText.setVisibility(GONE);
            editLocation.setVisibility(View.VISIBLE);
            editLocation.requestFocus();
            try {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(editLocation, InputMethodManager.SHOW_IMPLICIT);
            } catch (NullPointerException e) {
                Timber.w("Error opening keyboard");
            }
        }
    }

    protected void showLocationLayout() {
        editLocation.setVisibility(GONE);
        locationText.setVisibility(View.VISIBLE);
        try {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        } catch (NullPointerException e) {
            Timber.w("Error closing keyboard");
        }
    }

    public void subScribeData() {
        subscriptions = new CompositeSubscription(); // must initiate each time
        subscriptions.add(dbManager.methodSubSetQuery().subscribe(new Action1<List<MethodItem>>() {
            @Override
            public void call(List<MethodItem> methodItems) {
                setMethods(methodItems);
            }
        }));

        if (create) {
            subscriptions.add(dbManager.exchangeCurrencyQuery()
                    .subscribe(new Action1<List<ExchangeCurrencyItem>>() {
                        @Override
                        public void call(final List<ExchangeCurrencyItem> currencyItems) {
                            if (currencyItems == null || currencyItems.isEmpty()) {
                                currencySubscription = exchangeService.getCurrencies()
                                        .subscribeOn(Schedulers.newThread())
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribe(new Action1<List<ExchangeCurrency>>() {
                                            @Override
                                            public void call(List<ExchangeCurrency> currencies) {
                                                dbManager.insertExchangeCurrencies(currencies);
                                                updateCurrencies(currencies, null);
                                                showContent(true);
                                                createAdvertisement();
                                                List<ExchangeCurrency> exchangeCurrencies = new ArrayList<ExchangeCurrency>();
                                                exchangeCurrencies = ExchangeCurrencyItem.getCurrencies(currencyItems);
                                                updateCurrencies(exchangeCurrencies, null);
                                            }
                                        }, new Action1<Throwable>() {
                                            @Override
                                            public void call(Throwable throwable) {
                                                Timber.e(throwable.getMessage());
                                                Toast.makeText(EditActivity.this, "Unable to load currencies...", Toast.LENGTH_LONG).show();
                                                showContent(true);
                                                createAdvertisement();
                                                updateCurrencies(new ArrayList<ExchangeCurrency>(), null);
                                            }
                                        });
                            } else {
                                showContent(true);
                                createAdvertisement();
                                List<ExchangeCurrency> exchangeCurrencies = new ArrayList<ExchangeCurrency>();
                                exchangeCurrencies = ExchangeCurrencyItem.getCurrencies(currencyItems);
                                updateCurrencies(exchangeCurrencies, null);
                            }
                        }
                    }));
        } else {
            subscriptions.add(dbManager.advertisementItemQuery(adId).subscribe(new Action1<AdvertisementItem>() {
                @Override
                public void call(AdvertisementItem advertisement) {
                    showContent(true);
                    advertisementItem = advertisement; // save reference
                    setAdvertisement(advertisementItem);
                }
            }, new Action1<Throwable>() {
                @Override
                public void call(Throwable throwable) {
                    snackError(getString(R.string.snack_no_advertisement_data));
                    reportError(throwable);
                    finish();
                }
            }));
        }
    }

    public void showContent(final boolean show) {
        if (progress == null || content == null)
            return;

        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

        progress.setVisibility(show ? GONE : View.VISIBLE);
        progress.animate().setDuration(shortAnimTime).alpha(show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (progress != null)
                    progress.setVisibility(show ? GONE : View.VISIBLE);
            }
        });

        content.setVisibility(show ? View.VISIBLE : GONE);
        content.animate().setDuration(shortAnimTime).alpha(show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (content != null)
                    content.setVisibility(show ? View.VISIBLE : GONE);
            }
        });
    }
    

    private void setMethods(List<MethodItem> methods) {
        MethodAdapter typeAdapter = new MethodAdapter(this, R.layout.spinner_layout, methods);
        paymentMethodSpinner.setAdapter(typeAdapter);
    }

    /**
     * Set the currencies to be used for a new advertisement
     *
     * @param currencies
     * @param advertisement
     */
    private void updateCurrencies(List<ExchangeCurrency> currencies, AdvertisementItem advertisement) {

        currencies = CurrencyUtils.sortCurrencies(currencies);

        // handle error case
        String currencyPreference = exchangeService.getExchangeCurrency();
        String defaultCurrency = (create) ? currencyPreference : (advertisement != null) ? advertisement.currency() : this.getString(R.string.usd);

        if (currencies.isEmpty()) {
            ExchangeCurrency exchangeCurrency = new ExchangeCurrency(currencyPreference);
            currencies.add(exchangeCurrency); // just revert back to USD if we can
        }

        CurrencyAdapter typeAdapter = new CurrencyAdapter(this, R.layout.spinner_layout, currencies);
        currencySpinner.setAdapter(typeAdapter);

        int i = 0;
        for (ExchangeCurrency currency : currencies) {
            if (currency.getCurrency().equals(defaultCurrency)) {
                currencySpinner.setSelection(i);
                break;
            }
            i++;
        }
    }

    /**
     * Show the view for new advertisement
     */
    private void createAdvertisement() {
        liquidityCheckBox.setChecked(false);
        smsVerifiedCheckBox.setChecked(false);
        identifiedCheckBox.setChecked(false);
        trustedCheckBox.setChecked(false);
        activeCheckBox.setChecked(true);
        advertisementTypeLayout.setVisibility(View.VISIBLE);
        currencyLayout.setVisibility(View.VISIBLE);
        setAdvertisementType(TradeType.LOCAL_SELL);
        editMinimumAmountCurrency.setText(this.getString(R.string.usd));
        editMaximumAmountCurrency.setText(this.getString(R.string.usd));
        activeLayout.setVisibility(GONE);
    }

    /**
     * Set the advertisement from the database
     *
     * @param advertisement
     */
    private void setAdvertisement(AdvertisementItem advertisement) {
        
        showLocationLayout();

        // store these to pass back
        this.latitude = advertisement.lat();
        this.longitude = advertisement.lon();
        this.location = advertisement.location_string();
        this.city = advertisement.city();
        this.countryCode = advertisement.country_code();
        this.onlineProvider = advertisement.online_provider();

        liquidityCheckBox.setChecked(advertisement.track_max_amount());
        smsVerifiedCheckBox.setChecked(advertisement.sms_verification_required());
        identifiedCheckBox.setChecked(advertisement.require_identification());
        trustedCheckBox.setChecked(advertisement.trusted_required());
        activeCheckBox.setChecked(advertisement.visible());
        locationText.setText(advertisement.location_string());
        messageText.setText(advertisement.message());
        editBankNameText.setText(advertisement.bank_name());
        editMinimumAmount.setText(advertisement.min_amount());
        editMaximumAmount.setText(advertisement.max_amount());
        editPaymentDetails.setText(advertisement.account_info());

        if (!TextUtils.isEmpty(advertisement.require_feedback_score())) {
            minimumFeedbackText.setText(advertisement.require_feedback_score());
        }

        if (!Strings.isBlank(advertisement.require_trade_volume())) {
            minimumVolumeText.setText(advertisement.require_trade_volume());
        }

        if (!Strings.isBlank(advertisement.first_time_limit_btc())) {
            newBuyerLimitText.setText(advertisement.first_time_limit_btc());
        }

        advertisementTypeLayout.setVisibility(GONE);
        currencyLayout.setVisibility(GONE);

        this.tradeType = TradeType.valueOf(advertisement.trade_type());
        if(typeSpinner != null && typeSpinner.getAdapter().getCount() >= tradeType.ordinal()) {
            typeSpinner.setSelection(tradeType.ordinal()); 
        }
        
        if(TradeUtils.isOnlineTrade(advertisement)) {
            showOnlineOptions(tradeType);
        }

        if (tradeType == TradeType.LOCAL_SELL || tradeType == TradeType.LOCAL_BUY) {
            editPaymentDetailsLayout.setVisibility(GONE);
            bankNameLayout.setVisibility(GONE);
            paymentMethodLayout.setVisibility(GONE);
            newBuyerLimitLayout.setVisibility(GONE);
            minimumVolumeLayout.setVisibility(GONE);
            minimumFeedbackLayout.setVisibility(GONE);
        } else {
            editPaymentDetailsLayout.setVisibility(View.VISIBLE);
            paymentMethodLayout.setVisibility(create ? View.VISIBLE : GONE);
            bankNameLayout.setVisibility(create ? View.VISIBLE : GONE);
            newBuyerLimitLayout.setVisibility((tradeType == TradeType.ONLINE_SELL) ? View.VISIBLE : GONE);
            minimumVolumeLayout.setVisibility((tradeType == TradeType.ONLINE_SELL) ? View.VISIBLE : GONE);
            minimumFeedbackLayout.setVisibility((tradeType == TradeType.ONLINE_SELL) ? View.VISIBLE : GONE);
        }
        
        if(TradeUtils.ALTCOIN_ETH.equals(onlineProvider)) {
            currencyLayout.setVisibility(GONE);
            bankNameLayout.setVisibility(GONE);
            editPaymentDetailsLayout.setVisibility(View.GONE);
        } else {
            currencyLayout.setVisibility(View.VISIBLE);
            editMinimumAmountCurrency.setText(advertisement.currency());
            editMaximumAmountCurrency.setText(advertisement.currency());
        }

        editMinimumAmountCurrency.setText(advertisement.currency());
        editMaximumAmountCurrency.setText(advertisement.currency());
        editPriceEquation.setText(advertisement.price_equation());
        marginLayout.setVisibility(GONE);
    }
    
    protected void setAdvertisementType(TradeType tradeType) {
        
        if (tradeType == TradeType.LOCAL_SELL || tradeType == TradeType.LOCAL_BUY) {
            editPaymentDetailsLayout.setVisibility(GONE);
            bankNameLayout.setVisibility(GONE);
            paymentMethodLayout.setVisibility(GONE);
        } else {
            editPaymentDetailsLayout.setVisibility(View.VISIBLE);
            paymentMethodLayout.setVisibility(create ? View.VISIBLE : GONE);
            bankNameLayout.setVisibility(create ? View.VISIBLE : GONE);
            newBuyerLimitLayout.setVisibility((tradeType == TradeType.ONLINE_SELL) ? View.VISIBLE : GONE);
            minimumVolumeLayout.setVisibility((tradeType == TradeType.ONLINE_SELL) ? View.VISIBLE : GONE);
            minimumFeedbackLayout.setVisibility((tradeType == TradeType.ONLINE_SELL) ? View.VISIBLE : GONE);
        }
    }
    
    private void setEthereumPriceEquation() {
        
        TradeType tradeType = TradeType.values()[typeSpinner.getSelectedItemPosition()];

        String equation = "btc_in_eth";

        String margin = marginText.getText().toString();
        
        if (!Strings.isBlank(margin)) {
            double marginValue = 1.0;
            try {
                marginValue = Doubles.convertToDouble(margin);
            } catch (Exception e) {
                Timber.e(e.getMessage());
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

    protected void setPriceEquation() {
        
        TradeType tradeType = TradeType.values()[typeSpinner.getSelectedItemPosition()];

        String equation = Constants.DEFAULT_PRICE_EQUATION;
        ExchangeCurrency currency = (ExchangeCurrency) currencySpinner.getSelectedItem();
        if (currency == null) return; // currency values may not yet be set

        if (!currency.getCurrency().equals(Constants.DEFAULT_CURRENCY)) {
            equation = equation + "*" + Constants.DEFAULT_CURRENCY + "_in_" + currency.getCurrency();
        }

        String margin = marginText.getText().toString();
        if (!Strings.isBlank(margin)) {

            double marginValue = 1.0;
            try {
                marginValue = Doubles.convertToDouble(margin);
            } catch (Exception e) {
                Timber.e(e.getMessage());
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

    /**
     * Validate the form changes and send update or create request
     */
    public void validateChangesAndSend() {

        // used to store values for service call
        Advertisement editedAdvertisement = new Advertisement();
        String min = editMinimumAmount.getText().toString();
        String bankName = editBankNameText.getText().toString();
        String max = editMaximumAmount.getText().toString();
        String equation = editPriceEquation.getText().toString();
        String accountInfo = editPaymentDetails.getText().toString();
        String phoneNumber = detailsPhoneNumber.getText().toString();

        if (tradeType == TradeType.ONLINE_SELL) {
            switch (onlineProvider) {
                case TradeUtils.QIWI:
                case TradeUtils.SWISH:
                case TradeUtils.MOBILEPAY_DANSKE_BANK:
                case TradeUtils.MOBILEPAY_DANSKE_BANK_DK:
                case TradeUtils.MOBILEPAY_DANSKE_BANK_NO:
                    if (TextUtils.isEmpty(phoneNumber)) {
                        toast("Phone number must not be blank.");
                        return;
                    }
                    editedAdvertisement.phone_number = phoneNumber;
                    break;
            }
        } else if (tradeType == TradeType.ONLINE_BUY) {
            switch (onlineProvider) {
                case TradeUtils.NATIONAL_BANK:
                case TradeUtils.CASH_DEPOSIT:
                case TradeUtils.SPECIFIC_BANK:
                    if (TextUtils.isEmpty(bankName)) {
                        toast("Bank name can't be blank.");
                        return;
                    }
                    editedAdvertisement.bank_name = bankName;
                    break;
                case TradeUtils.OTHER:
                case TradeUtils.OTHER_REMITTANCE:
                case TradeUtils.OTHER_PRE_PAID_DEBIT:
                case TradeUtils.OTHER_ONLINE_WALLET_GLOBAL:
                case TradeUtils.OTHER_ONLINE_WALLET:
                    if (TextUtils.isEmpty(bankName)) {
                        toast("Payment method can't be blank.");
                        return;
                    }
                    editedAdvertisement.bank_name = bankName;
                    break;
                case TradeUtils.INTERNATIONAL_WIRE_SWIFT:
                     if (TextUtils.isEmpty(bankName)) {
                        toast("SWIFT or Bic can't be blank.");
                        return;
                    }
                    editedAdvertisement.bank_name = bankName;
                    break;
                case TradeUtils.GIFT_CARD_CODE:
                    if (TextUtils.isEmpty(bankName)) {
                        toast("Gift card issuer can't be blank.");
                        return;
                    }
                    editedAdvertisement.bank_name = bankName;
                    break;
            }
        }
        
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
        
        if (create) {
            if (address == null) {
                showAlertDialog(new AlertDialogEvent("Error", "Unable to save changes without a valid address which is required for creating new advertisements."));
                return;
            } else {
                editedAdvertisement.location = SearchUtils.getDisplayAddress(address);
                editedAdvertisement.city = address.getLocality();
                editedAdvertisement.country_code = address.getCountryCode();
                editedAdvertisement.lon = address.getLongitude();
                editedAdvertisement.lat = address.getLatitude();
            }

            editedAdvertisement.visible = true;
            editedAdvertisement.trade_type = tradeType;
            
            if(TradeUtils.ALTCOIN_ETH.equals(onlineProvider)) {
                editedAdvertisement.currency = getString(R.string.eth);
            } else {
                editedAdvertisement.currency = ((ExchangeCurrency) currencySpinner.getSelectedItem()).getCurrency();
            }
            
            if (tradeType == TradeType.ONLINE_BUY || tradeType == TradeType.ONLINE_SELL) {
                editedAdvertisement.online_provider = onlineProvider;
            } else {
                editedAdvertisement.online_provider = TradeUtils.NATIONAL_BANK;
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
            
            // reset these for editing advertisements as they are not updated on edit but required to be sent back to service
            editedAdvertisement.location = location;
            editedAdvertisement.city = city;
            editedAdvertisement.country_code = countryCode;
            editedAdvertisement.lon = longitude;
            editedAdvertisement.lat = latitude;
        }
        
        editedAdvertisement.message = messageText.getText().toString();
        editedAdvertisement.price_equation = equation;
        editedAdvertisement.min_amount = String.valueOf(TradeUtils.convertCurrencyAmount(min));
        editedAdvertisement.max_amount = String.valueOf(TradeUtils.convertCurrencyAmount(max));

        // only online trades have these values
        if (TradeUtils.isOnlineTrade(tradeType)) {
            
            editedAdvertisement.account_info = accountInfo;

            if (tradeType == TradeType.ONLINE_SELL) {
                if (!TextUtils.isEmpty(newBuyerLimitText.getText().toString())) {
                    editedAdvertisement.first_time_limit_btc = newBuyerLimitText.getText().toString();
                }

                if (!TextUtils.isEmpty(minimumFeedbackText.getText().toString())) {
                    editedAdvertisement.require_feedback_score = minimumFeedbackText.getText().toString();
                }

                if (!TextUtils.isEmpty(minimumVolumeText.getText().toString())) {
                    editedAdvertisement.require_trade_volume = minimumVolumeText.getText().toString();
                }
            }

            editedAdvertisement.sms_verification_required = smsVerifiedCheckBox.isChecked();
            editedAdvertisement.require_identification = identifiedCheckBox.isChecked();
            editedAdvertisement.track_max_amount = liquidityCheckBox.isChecked();
            editedAdvertisement.trusted_required = trustedCheckBox.isChecked();

            try {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
            } catch (NullPointerException e) {
                Timber.w("Error closing keyboard");
            }

            updateAdvertisement(editedAdvertisement, create);
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
            locationText.setText(shortAddress);
            showLocationLayout();
        } else {
            editLocation.setVisibility(View.VISIBLE);
            editLocation.requestFocus();
        }
    }

    /**
     * Saves the current address to user preferences
     *
     * @param address Address
     */
    public void saveAddress(Address address) {
        if (address != null) {
            this.address = address;
        } else {
            showAddressError();
        }
    }

    private void showAddressError() {
        showAlertDialog(new AlertDialogEvent("Address Error", getString(R.string.error_dialog_no_address_edit)), new Action0() {
            @Override
            public void call() {
                getLasKnownLocation();
            }
        }, new Action0() {
            @Override
            public void call() {
                showEditTextLayout();
                editLocation.setText("");
            }
        });
    }

    protected void setEditLocationAdapter(PredictAdapter adapter) {
        if (editLocation != null)
            editLocation.setAdapter(adapter);
    }

    public PredictAdapter getEditLocationAdapter() {
        return predictAdapter;
    }

    public void cancelChanges(Boolean create) {
        toast((create) ? "New advertisement canceled" : "Advertisement update canceled");
        setResult(RESULT_CANCELED);
        finish();
    }

    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        return networkInfo != null;
    }

    public void updateAdvertisement(final Advertisement advertisement, final Boolean create) {
        if (create) {

            showProgressDialog(new ProgressDialogEvent("Posting advertisement..."));

            Observable<JSONObject> createAdvertisementObservable = dataService.createAdvertisement(advertisement);
            advertisementSubscription = createAdvertisementObservable
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<JSONObject>() {
                        @Override
                        public void onCompleted() {
                            hideProgressDialog();
                        }

                        @Override
                        public void onError(final Throwable e) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    hideProgressDialog();
                                    handleLocalError(e);
                                }
                            });
                        }

                        @Override
                        public void onNext(final JSONObject jsonObject) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
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
                    .subscribe(new Action1<JSONObject>() {
                        @Override
                        public void call(final JSONObject jsonObject) {
                            hideProgressDialog();

                            if (Parser.containsError(jsonObject)) {
                                RetroError error = Parser.parseError(jsonObject);
                                showAlertDialog(new AlertDialogEvent("Error Updating Advertisement", error.getMessage()));
                            } else {
                                updateAdvertisement(advertisement);
                            }
                        }
                    }, new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            hideProgressDialog();
                            handleLocalError(throwable);
                        }
                    });
        }
    }

    // TODO let's clean this up and move to central location to get the errors
    private void handleLocalError(Throwable throwable) {
        if (DataServiceUtils.isHttp400Error(throwable)) {

            if (throwable instanceof RetrofitError) {

                RetrofitError retroError = (RetrofitError) throwable;

                if (retroError.getResponse() != null) {

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

    private void updateAdvertisement(Advertisement advertisement) {
        /*int updated = dbManager.updateAdvertisement(advertisement);

        if (updated > 0) {

            hideProgressDialog();

            toast(getString(R.string.message_advertisement_changed));

            Intent returnIntent = new Intent();
            returnIntent.putExtra(AdvertisementActivity.EXTRA_AD_ID, adId);
            setResult(RESULT_UPDATED, returnIntent);
            finish();
        }*/
    }

    protected void doAddressLookup(String locationName) {
        
        Timber.d("doAddressLookup");

        geoLocationFromNameSubscription = geoGetLocationFromName(locationName)
                .doOnUnsubscribe(new Action0() {
                    @Override
                    public void call() {
                        Timber.i("geoGetLocationFromName Method subscription safely unsubscribed");
                    }
                })
                .compose(this.<List<Address>>bindUntilEvent(ActivityEvent.PAUSE))
                .observeOn(Schedulers.computation())
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<List<Address>>() {
                    @Override
                    public void call(final List<Address> addresses) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (!addresses.isEmpty()) {
                                    getEditLocationAdapter().replaceWith(addresses);
                                }
                            }
                        });
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(final Throwable throwable) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                                    imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
                                } catch (NullPointerException e) {
                                    Timber.w("Error closing keyboard");
                                }

                                reportError(throwable);
                                showAlertDialog(new AlertDialogEvent("Address Error", getString(R.string.error_unable_load_address_edit)), new Action0() {
                                    @Override
                                    public void call() {
                                        getLasKnownLocation();
                                    }
                                }, new Action0() {
                                    @Override
                                    public void call() {
                                        toast((create) ? "New advertisement canceled" : "Advertisement update canceled");
                                        finish();
                                    }
                                });
                            }
                        });
                    }
                });
    }

    protected Observable<List<Address>> geoGetLocationFromName(final String searchQuery) {
        final ReactiveLocationProvider locationProvider = new ReactiveLocationProvider(EditActivity.this);
        return locationProvider.getGeocodeObservable(searchQuery, MAX_ADDRESSES);
    }

    private boolean checkPlayServices() {
        GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
        int result = googleAPI.isGooglePlayServicesAvailable(EditActivity.this);
        return result == ConnectionResult.SUCCESS;
    }

    private void showGoogleAPIResolveError() {
        GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
        int result = googleAPI.isGooglePlayServicesAvailable(EditActivity.this);
        if (googleAPI.isUserResolvableError(result)) {
            showGooglePlayServicesError();
        } else {
            toast(getString(R.string.warning_no_google_play_services));
            finish();
        }
    }

    private void installGooglePlayServices() {
        final String appPackageName = "com.google.android.gms"; // getPackageName() from Context or Activity object
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
        } catch (android.content.ActivityNotFoundException anfe) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + appPackageName)));
        }
    }

    private void showGooglePlayServicesError() {
        int result = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);

        switch (result) {
            case ConnectionResult.SERVICE_MISSING:
                showAlertDialog(new AlertDialogEvent(getString(R.string.warning_no_google_play_services_title), getString(R.string.warning_no_google_play_services)), new Action0() {
                    @Override
                    public void call() {
                        installGooglePlayServices();
                    }
                }, new Action0() {
                    @Override
                    public void call() {
                        toast(getString(R.string.warning_no_google_play_services));
                        finish();
                    }
                });
                break;
            case ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED:
                showAlertDialog(new AlertDialogEvent(getString(R.string.warning_no_google_play_services_title), getString(R.string.warning_update_google_play_services)), new Action0() {
                    @Override
                    public void call() {
                        installGooglePlayServices();
                    }
                }, new Action0() {
                    @Override
                    public void call() {
                        toast(getString(R.string.warning_no_google_play_services));
                        //finish();
                    }
                });
                break;
            case ConnectionResult.SERVICE_DISABLED:
                showAlertDialog(new AlertDialogEvent(getString(R.string.warning_no_google_play_services_title), getString(R.string.warning_no_google_play_services)), new Action0() {
                    @Override
                    public void call() {
                        installGooglePlayServices();
                    }
                }, new Action0() {
                    @Override
                    public void call() {
                        toast(getString(R.string.warning_no_google_play_services));
                        finish();
                    }
                });
                break;
        }
    }

    // ------  LOCATION SERVICES ---------

    @TargetApi(Build.VERSION_CODES.M)
    private void getLasKnownLocation() {
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(EditActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(EditActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                showRequestPermissionsDialog();
                return;
            }
        }

        if (!hasLocationServices()) {
            showNoLocationServicesWarning();
            return;
        }

        LocationRequest request = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setNumUpdates(10)
                .setInterval(100);

        final ReactiveLocationProvider locationProvider = new ReactiveLocationProvider(EditActivity.this);
        
        Timber.d("getLasKnownLocation");
        
        geoLocationSubscription = locationProvider.getUpdatedLocation(request)
                .doOnUnsubscribe(new Action0() {
                    @Override
                    public void call() {
                        Timber.i("geoGetLocationFromName Method subscription safely unsubscribed");
                    }
                })
                .compose(this.<Location>bindUntilEvent(ActivityEvent.PAUSE))
                .doOnUnsubscribe(new Action0() {
                    @Override
                    public void call() {
                        Timber.i("geoGetLocationFromName Method subscription safely unsubscribed");
                    }
                })
                .compose(this.<Location>bindUntilEvent(ActivityEvent.DESTROY))
                .flatMap(new Func1<Location, Observable<List<Address>>>() {
                    @Override
                    public Observable<List<Address>> call(Location location) {
                        try {
                            return locationProvider.getReverseGeocodeObservable(location.getLatitude(), location.getLongitude(), 1)
                                    .observeOn(Schedulers.io())
                                    .subscribeOn(AndroidSchedulers.mainThread());
                        } catch (Exception exception) {
                            return Observable.just(null);
                        }
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
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (address != null) {
                                    saveAddress(address);
                                    displayAddress(address);
                                } else {
                                    showAddressError();
                                }

                                geoLocationSubscription.unsubscribe();
                                geoLocationSubscription = null;
                            }
                        });
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(final Throwable throwable) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                reportError(throwable);
                                showAddressError();
                                geoLocationSubscription.unsubscribe();
                                geoLocationSubscription = null;
                            }
                        });
                    }
                });
    }

    private boolean hasLocationServices() {
        return (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER));
    }

    public void showRequestPermissionsDialog() {
        showAlertDialog(new AlertDialogEvent(getString(R.string.alert_permission_required), getString(R.string.require_location_permission)),
                new Action0() {
                    @Override
                    public void call() {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, Constants.REQUEST_PERMISSIONS);
                        }
                    }
                }, new Action0() {
                    @Override
                    public void call() {
                        toast((create) ? "New advertisement canceled" : "Advertisement update canceled");
                        finish();
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
                toast((create) ? "New advertisement canceled" : "Advertisement update canceled");
                finish();
            }
        });
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
                        toast((create) ? getString(R.string.toast_new_ad_canceled) : getString(R.string.toast_edit_ad_canceled));
                        finish();
                    } else {
                        getLasKnownLocation();
                    }
                }
            }
        }
    }

    public class InputFilterMinMax implements InputFilter {
        private int min, max;

        public InputFilterMinMax(int min, int max) {
            this.min = min;
            this.max = max;
        }

        public InputFilterMinMax(String min, String max) {
            this.min = Integer.parseInt(min);
            this.max = Integer.parseInt(max);
        }

        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            try {
                int input = Integer.parseInt(dest.toString() + source.toString());
                if (isInRange(min, max, input))
                    return null;
            } catch (NumberFormatException nfe) {
            }
            return "";
        }

        private boolean isInRange(int a, int b, int c) {
            return b > a ? c >= a && c <= b : c >= b && c <= a;
        }
    }
}