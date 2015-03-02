package com.thanksmister.bitcoin.localtrader.ui.edit;

import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.os.Bundle;
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

import com.thanksmister.bitcoin.localtrader.BaseActivity;
import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.constants.Constants;
import com.thanksmister.bitcoin.localtrader.data.api.model.Advertisement;
import com.thanksmister.bitcoin.localtrader.data.api.model.Currency;
import com.thanksmister.bitcoin.localtrader.data.api.model.Method;
import com.thanksmister.bitcoin.localtrader.data.api.model.TradeType;
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
import timber.log.Timber;

public class EditActivity extends BaseActivity implements EditView
{
    public static final String EXTRA_ADDRESS = "com.thanksmister.extras.EXTRA_ADDRESS";
    public static final String EXTRA_CREATE = "com.thanksmister.extras.EXTRA_CREATE";
  
    
    public static final int REQUEST_CODE = 10937;
    
    public static final int RESULT_UPDATED = 72322;
    public static final int RESULT_CANCELED = 72321;

    @Inject
    EditPresenter presenter;

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
    TextView editPriceEquation;

    @InjectView(R.id.editMinimumAmount)
    TextView editMinimumAmount;

    @InjectView(R.id.editMaximumAmount)
    TextView editMaximumAmount;

    @InjectView(R.id.editPaymentDetails)
    TextView editPaymentDetails;

    @InjectView(R.id.editBankNameText)
    TextView editBankNameText;

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

    @InjectView(R.id.paymentMethodSpinner)
    Spinner paymentMethodSpinner;

    @InjectView(R.id.typeSpinner)
    Spinner typeSpinner;

    @OnClick(R.id.clearButton)
    public void clearButtonClicked()
    {
        showSearchLayout();
        presenter.stopLocationCheck(); // stop current check
    }

    @OnClick(R.id.mapButton)
    public void mapButtonClicked()
    {
        showMapLayout();
        currentLocation.setText("- - - -");
        presenter.stopLocationCheck(); // stop current check
        presenter.startLocationCheck(); // get new location
    }

    private boolean create;
    private PredictAdapter predictAdapter;
    private Address address;

    public static Intent createStartIntent(Context context, Boolean create)
    {
        Intent intent = new Intent(context, EditActivity.class);
        intent.putExtra(EXTRA_CREATE, create);
        return intent;
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_edit);

        ButterKnife.inject(this);

        if (savedInstanceState == null) {
            //advertisement =  getIntent().getStringExtra(EXTRA_ADVERTISEMENT);
            create = getIntent().getBooleanExtra(EXTRA_CREATE, false);
        } else {
            address = savedInstanceState.getParcelable(EXTRA_ADDRESS);
            create = savedInstanceState.getBoolean(EXTRA_CREATE);
        }
        
        if(toolbar != null) {
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle((create)? "Post new trade": "Edit trade");
            setToolBarMenu(toolbar);
        }

        String[] typeTitles = getResources().getStringArray(R.array.list_advertisement_type_spinner);
        List<String> typeList = new ArrayList<String>(Arrays.asList(typeTitles));

        SpinnerAdapter typeAdapter = new SpinnerAdapter(getContext(), R.layout.spinner_layout, typeList);
        typeSpinner.setAdapter(typeAdapter);
        typeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l){
                presenter.setAdvertisementType(TradeType.values()[i]);
                if(create) setPriceEquation();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView){}
        });

        currencySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l){
                Timber.d("Currency Selected: " + i);
                Currency exchange = (Currency) currencySpinner.getAdapter().getItem(i);
                editMinimumAmountCurrency.setText(exchange.ticker);
                editMaximumAmountCurrency.setText(exchange.ticker);
                if(create)
                    setPriceEquation();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView){}
        });

        editLocation.setOnItemClickListener((parent, view, position, id) -> {
            presenter.stopLocationCheck();
            Address address1 = predictAdapter.getItem(position);
            showMapLayout();
            setAddress(address1);
            editLocation.setText("");
        });

        editLocation.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3){}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                if (!Strings.isBlank(charSequence)) {
                    presenter.doAddressLookup(charSequence.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable editable){}
        });
        
        marginText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3){}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                setPriceEquation();
            }

            @Override
            public void afterTextChanged(Editable editable){
                
            }
        });

        predictAdapter = new PredictAdapter(getContext(), Collections.emptyList());
        setEditLocationAdapter(predictAdapter);
    }

    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putParcelable(EXTRA_ADDRESS, address);
        outState.putBoolean(EXTRA_CREATE, create);
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
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {

                switch (menuItem.getItemId()) {
                    case R.id.action_cancel:
                        presenter.cancelChanges(create);
                        return true;
                    case R.id.action_save:
                        presenter.validateChanges();
                        return true;
                }
                return false;
            }
        });
    }
    
    @Override
    protected List<Object> getModules()
    {
        return Arrays.<Object>asList(new EditModule(this));
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

        presenter.onResume(create);
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();

        ButterKnife.reset(this);

        presenter.onDestroy();
    }

    @Override 
    public Context getContext() 
    {
        return this;
    }


    @Override
    public void setTradeType(TradeType tradeType)
    {
        typeSpinner.setSelection(tradeType.ordinal());
    }

    @Override
    public void setMethods(List<Method> methods)
    {
        MethodAdapter typeAdapter = new MethodAdapter(getContext(), R.layout.spinner_layout, methods);
        paymentMethodSpinner.setAdapter(typeAdapter);
    }

    @Override
    public void setCurrencies(List<Currency> currencies, Advertisement advertisement)
    {
        CurrencyAdapter typeAdapter = new CurrencyAdapter(getContext(), R.layout.spinner_layout, currencies);
        currencySpinner.setAdapter(typeAdapter);

        int i = 0;
        String defaultCurrency = (create)? getContext().getString(R.string.usd):advertisement.currency;
        for (Currency currency : currencies) {
            if(currency.ticker.equals(defaultCurrency)) {
                currencySpinner.setSelection(i);
                break;
            }
            i++;
        }
    }

    @Override
    public void setAdvertisement(Advertisement advertisement)
    {
        if(!create) {
            currentLocation.setText(advertisement.location);
        }

        Timber.d("Max Amount: " + advertisement.track_max_amount);
        Timber.d("SMS Verified: " + advertisement.sms_verification_required);
        Timber.d("Trusted: " + advertisement.trusted_required);
       
        liquidityCheckBox.setChecked(advertisement.track_max_amount);
        smsVerifiedCheckBox.setChecked(advertisement.sms_verification_required);
        trustedCheckBox.setChecked(advertisement.trusted_required);
        activeCheckBox.setChecked(advertisement.visible);
        
        
        editBankNameText.setText(advertisement.bank_name);
        editMinimumAmount.setText(advertisement.min_amount);
        editMaximumAmount.setText(advertisement.max_amount);
        editPaymentDetails.setText(advertisement.account_info);
        advertisementTypeLayout.setVisibility(create?View.VISIBLE:View.GONE);

        if(advertisement.trade_type == TradeType.LOCAL_SELL || advertisement.trade_type == TradeType.LOCAL_BUY) {
            editPaymentDetailsLayout.setVisibility(View.GONE);
            bankNameLayout.setVisibility(View.GONE);
            paymentMethodLayout.setVisibility(View.GONE);
        } else {
            editPaymentDetailsLayout.setVisibility(View.VISIBLE);
            paymentMethodLayout.setVisibility(create?View.VISIBLE:View.GONE);
            bankNameLayout.setVisibility(create?View.VISIBLE:View.GONE);
        }

        if(create) {
            editMinimumAmountCurrency.setText(getContext().getString(R.string.usd));
            editMaximumAmountCurrency.setText(getContext().getString(R.string.usd));
            activeLayout.setVisibility(View.GONE);
        } else {
            editMinimumAmountCurrency.setText(advertisement.currency);
            editMaximumAmountCurrency.setText(advertisement.currency);
            editPriceEquation.setText(advertisement.price_equation);
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

    @Override
    public void validateChangesAndSend(Advertisement advertisement)
    {
        String min = editMinimumAmount.getText().toString();
        String bankName = editBankNameText.getText().toString();
        String max = editMaximumAmount.getText().toString();
        String equation = editPriceEquation.getText().toString();
        String accountInfo = editPaymentDetails.getText().toString();

        if (TextUtils.isEmpty(equation)) {
            Toast.makeText(getContext(), "Price equation can't be blank.", Toast.LENGTH_SHORT).show();
            return;
        } else  if (Strings.isBlank(min)) {
            Toast.makeText(getContext(), "Enter a valid minimum amount.", Toast.LENGTH_SHORT).show();
            return;
        } else if (Strings.isBlank(max)) {
            Toast.makeText(getContext(), "Enter a valid maximum amount.", Toast.LENGTH_SHORT).show();
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

        presenter.updateAdvertisement(advertisement, create);
    }

    @Override
    public void setAddress(Address address)
    {
        if (address == null) return;

        this.address = address;

        currentLocation.setText(TradeUtils.getAddressShort(address));
    }

    public void showSearchLayout()
    {
        mapLayout.startAnimation(AnimationUtils.loadAnimation(getContext(), android.R.anim.slide_out_right));
        searchLayout.startAnimation(AnimationUtils.loadAnimation(getContext(), android.R.anim.slide_in_left));
        mapLayout.setVisibility(View.GONE);
        searchLayout.setVisibility(View.VISIBLE);
    }

    public void showMapLayout()
    {
        mapLayout.startAnimation(AnimationUtils.loadAnimation(getContext(), android.R.anim.slide_in_left));
        searchLayout.startAnimation(AnimationUtils.loadAnimation(getContext(), android.R.anim.slide_out_right));
        mapLayout.setVisibility(View.VISIBLE);
        searchLayout.setVisibility(View.GONE);
    }

    protected void setEditLocationAdapter(PredictAdapter adapter)
    {
        if (editLocation != null)
            editLocation.setAdapter(adapter);
    }

    @Override
    public PredictAdapter getEditLocationAdapter()
    {
        return predictAdapter;
    }
}
