package com.thanksmister.bitcoin.localtrader.ui.edit;

import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
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

import com.squareup.sqlbrite.SqlBrite;
import com.thanksmister.bitcoin.localtrader.BaseActivity;
import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.constants.Constants;
import com.thanksmister.bitcoin.localtrader.data.api.model.Advertisement;
import com.thanksmister.bitcoin.localtrader.data.api.model.Contact;
import com.thanksmister.bitcoin.localtrader.data.api.model.Currency;
import com.thanksmister.bitcoin.localtrader.data.api.model.Method;
import com.thanksmister.bitcoin.localtrader.data.api.model.TradeType;
import com.thanksmister.bitcoin.localtrader.data.database.AdvertisementItem;
import com.thanksmister.bitcoin.localtrader.data.database.DbManager;
import com.thanksmister.bitcoin.localtrader.data.services.GeoLocationService;
import com.thanksmister.bitcoin.localtrader.events.ConfirmationDialogEvent;
import com.thanksmister.bitcoin.localtrader.ui.MethodAdapter;
import com.thanksmister.bitcoin.localtrader.ui.PredictAdapter;
import com.thanksmister.bitcoin.localtrader.ui.misc.SpinnerAdapter;
import com.thanksmister.bitcoin.localtrader.utils.Doubles;
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
import butterknife.Optional;
import rx.Observable;
import rx.Observer;
import rx.functions.Action0;
import rx.functions.Action1;
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
    SqlBrite db;
    
    @Inject
    DbManager dbManager;

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

    private Observable<Location> locationObservable;
    private Observable<AdvertisementItem> advertisementItemObservable;

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
            getSupportActionBar().setTitle((create)? "Post new trade": "Edit trade");
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
                //presenter.setAdvertisementType(TradeType.values()[i]);
                if (create) setPriceEquation();
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
                Timber.d("Currency Selected: " + i);
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

        editLocation.setOnItemClickListener((parent, view, position, id) -> {
            //presenter.stopLocationCheck();
            Address address1 = predictAdapter.getItem(position);
            showMapLayout();
            setAddress(address1);
            editLocation.setText("");
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
                    //presenter.doAddressLookup(charSequence.toString());
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

        predictAdapter = new PredictAdapter(this, Collections.emptyList());
        setEditLocationAdapter(predictAdapter);
        
        advertisementItemObservable = bindActivity(this, dbManager.advertisementItemQuery(adId));
        
        //subScribeData
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

    public void addToolbarView(Toolbar toolbar)
    {
        View container = LayoutInflater.from(this).inflate(R.layout.actionbar_custom_view_done_discard, toolbar, false);
        Toolbar.LayoutParams layoutParams = new Toolbar.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        layoutParams.gravity = Gravity.BOTTOM;
        toolbar.addView(container, layoutParams);
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
                        //validateChanges();
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
    }
    
    public void subScribeData()
    {
        advertisementItemObservable.subscribe(new Action1<AdvertisementItem>()
        {
            @Override
            public void call(AdvertisementItem advertisementItem)
            {
                setAdvertisement(advertisementItem);
            }
        });
    }
    
    public void setTradeType(TradeType tradeType)
    {
        typeSpinner.setSelection(tradeType.ordinal());
    }
    
    public void setMethods(List<Method> methods)
    {
        MethodAdapter typeAdapter = new MethodAdapter(this, R.layout.spinner_layout, methods);
        paymentMethodSpinner.setAdapter(typeAdapter);
    }
    
    public void setCurrencies(List<Currency> currencies, Advertisement advertisement)
    {
        CurrencyAdapter typeAdapter = new CurrencyAdapter(this, R.layout.spinner_layout, currencies);
        currencySpinner.setAdapter(typeAdapter);

        int i = 0;
        String defaultCurrency = (create)? this.getString(R.string.usd):advertisement.currency;
        for (Currency currency : currencies) {
            if(currency.ticker.equals(defaultCurrency)) {
                currencySpinner.setSelection(i);
                break;
            }
            i++;
        }
    }
    
    public void setAdvertisement(AdvertisementItem advertisement)
    {
        if(!create) {
            currentLocation.setText(advertisement.location_string());
        }

        Timber.d("Max Amount: " + advertisement.track_max_amount());
        Timber.d("SMS Verified: " + advertisement.sms_verification_required());
        Timber.d("Trusted: " + advertisement.trusted_required());
       
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

        if(create) {
            editMinimumAmountCurrency.setText(this.getString(R.string.usd));
            editMaximumAmountCurrency.setText(this.getString(R.string.usd));
            activeLayout.setVisibility(View.GONE);
        } else {
            editMinimumAmountCurrency.setText(advertisement.currency());
            editMaximumAmountCurrency.setText(advertisement.currency());
            editPriceEquation.setText(advertisement.price_equation());
            marginLayout.setVisibility(View.GONE);
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
    
    public void validateChangesAndSend(Advertisement advertisement)
    {
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
            advertisement.visible = activeCheckBox.isChecked();
        }

        advertisement.price_equation = equation;
        advertisement.min_amount = min;
        advertisement.max_amount = max;
        advertisement.bank_name = bankName;
        advertisement.message = msg;

        advertisement.sms_verification_required = smsVerifiedCheckBox.isChecked();
        advertisement.track_max_amount = liquidityCheckBox.isChecked();
        advertisement.trusted_required = trustedCheckBox.isChecked();

        String location = (address != null)? TradeUtils.getAddressShort(address):advertisement.location;
        String city = (address != null)? address.getLocality():advertisement.city;
        String code = (address != null)? address.getCountryCode():advertisement.country_code;
        double lon = (address != null)? address.getLongitude():advertisement.lon;
        double lat = (address != null)? address.getLatitude():advertisement.lat;

        advertisement.location = location;
        advertisement.city = city;
        advertisement.lon = lon;
        advertisement.lat = lat;
        advertisement.country_code = code;
        advertisement.account_info = accountInfo;

        //updateAdvertisement(advertisement, create);
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
        String message = (create)? "New trade canceled":"Trade update canceled";
        toast(message);
        finish();
    }

    public void startLocationCheck()
    {
       /* if(!geoLocationService.isGooglePlayServicesAvailable()) {
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
        }*/
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
    
    public void getCurrencies()
    {
        /*subscription = service.getCurrencies(new Observer<List<Currency>>() {
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
                setCurrencies(exchanges, getAdvertisement());
            }
        });*/
    }
    
    public void getAddressFromLocation(Location location)
    {
        geoLocationService.geoDecodeLocation(location).subscribe(new Observer<List<Address>>()
        {
            @Override
            public void onCompleted()
            {
            }

            @Override
            public void onError(Throwable throwable)
            {
                handleError(throwable);
            }

            @Override
            public void onNext(List<Address> addresses)
            {
                if (!addresses.isEmpty()) {
                    setAddress(addresses.get(0));
                }
            }
        });
        
    }
}
