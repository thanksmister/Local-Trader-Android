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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;

import com.thanksmister.bitcoin.localtrader.BaseActivity;
import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.data.api.model.ContactRequest;
import com.thanksmister.bitcoin.localtrader.data.services.DataService;
import com.thanksmister.bitcoin.localtrader.utils.Calculations;
import com.thanksmister.bitcoin.localtrader.utils.Conversions;
import com.thanksmister.bitcoin.localtrader.utils.Doubles;
import com.thanksmister.bitcoin.localtrader.utils.Strings;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

public class TradeRequestActivity extends BaseActivity
{
    public static final String EXTRA_AD_ID = "com.thanksmister.bitcoin.localtrader.EXTRA_AD_ID";
    public static final String EXTRA_AD_PRICE = "com.thanksmister.bitcoin.localtrader.EXTRA_AD_PRICE";
    public static final String EXTRA_AD_MIN_AMOUNT = "com.thanksmister.bitcoin.localtrader.EXTRA_AD_MIN_AMOUNT";
    public static final String EXTRA_AD_MAX_AMOUNT = "com.thanksmister.bitcoin.localtrader.EXTRA_AD_MAX_AMOUNT";
    public static final String EXTRA_AD_CURRENCY = "com.thanksmister.bitcoin.localtrader.EXTRA_AD_CURRENCY";
    public static final String EXTRA_AD_PROFILE_NAME = "com.thanksmister.bitcoin.localtrader.EXTRA_AD_PROFILE_NAME";

    @Inject
    DataService dataService;
    
    @InjectView(R.id.tradeRequestToolbar)
    Toolbar toolbar;

    @InjectView(R.id.editAmountText)
    EditText editAmountText;

    @InjectView(R.id.editBitcoinText)
    EditText editBitcoinText;

    @InjectView(R.id.messageText)
    EditText messageText;

    @InjectView(R.id.tradeAmountTitle)
    TextView tradeAmountTitle;

    @InjectView(R.id.tradeLimit)
    TextView tradeLimit;

    @InjectView(R.id.tradeCurrency)
    TextView tradeCurrency;

    @OnClick(R.id.sendButton)
    public void sendButtonClicked()
    {
        validateChangesAndSend();
    }

    private String adId;
    private String adPrice;
    private String adMin;
    private String adMax;
    private String currency;
    private String profileName;

    public static Intent createStartIntent(Context context, String adId, String adPrice, String adMin, String adMax, String currency, String profileName)
    {
        Intent intent = new Intent(context, TradeRequestActivity.class);
        intent.putExtra(EXTRA_AD_ID, adId);
        intent.putExtra(EXTRA_AD_PRICE, adPrice);
        intent.putExtra(EXTRA_AD_MIN_AMOUNT, adMin);
        intent.putExtra(EXTRA_AD_MAX_AMOUNT, adMax);
        intent.putExtra(EXTRA_AD_CURRENCY, currency);
        intent.putExtra(EXTRA_AD_PROFILE_NAME, profileName);
        return intent;
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.view_trade_request);

        ButterKnife.inject(this);

        if (savedInstanceState == null) {
            adId = getIntent().getStringExtra(EXTRA_AD_ID);
            adPrice = getIntent().getStringExtra(EXTRA_AD_PRICE);
            adMin = getIntent().getStringExtra(EXTRA_AD_MIN_AMOUNT);
            adMax = getIntent().getStringExtra(EXTRA_AD_MAX_AMOUNT);
            currency = getIntent().getStringExtra(EXTRA_AD_CURRENCY);
            profileName = getIntent().getStringExtra(EXTRA_AD_PROFILE_NAME);
        } else {
            adId = savedInstanceState.getString(EXTRA_AD_ID);
            adPrice = savedInstanceState.getString(EXTRA_AD_PRICE);
            adMin = savedInstanceState.getString(EXTRA_AD_MIN_AMOUNT);
            adMax = savedInstanceState.getString(EXTRA_AD_MAX_AMOUNT);
            currency = savedInstanceState.getString(EXTRA_AD_CURRENCY);
            profileName = savedInstanceState.getString(EXTRA_AD_PROFILE_NAME);
        }

        if(toolbar != null) {
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Request trade with " + profileName);
        }

        tradeAmountTitle.setText(getString(R.string.trade_request_title, currency));
        tradeLimit.setText(getString(R.string.trade_request_limit, adMin, adMax, currency));
        tradeCurrency.setText(currency);

        editAmountText.setFilters(new InputFilter[]{new Calculations.DecimalPlacesInputFilter(2)});
        editAmountText.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3)
            {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3)
            {
            }

            @Override
            public void afterTextChanged(Editable editable)
            {
                if (editAmountText.hasFocus()) {
                    String amount = editable.toString();
                    calculateBitcoinAmount(amount);
                }
            }
        });

        editBitcoinText.setFilters(new InputFilter[]{new Calculations.DecimalPlacesInputFilter(8)});
        editBitcoinText.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3)
            {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3)
            {
            }

            @Override
            public void afterTextChanged(Editable editable)
            {
                if (editBitcoinText.hasFocus()) {
                    String bitcoin = editable.toString();
                    calculateCurrencyAmount(bitcoin);
                }
            }
        });
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
    
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putString(EXTRA_AD_ID, adId);
        outState.putString(EXTRA_AD_PRICE, adPrice);
        outState.putString(EXTRA_AD_MIN_AMOUNT, adMin);
        outState.putString(EXTRA_AD_MAX_AMOUNT, adMax);
        outState.putString(EXTRA_AD_CURRENCY, currency);
        outState.putString(EXTRA_AD_PROFILE_NAME, profileName);
    }

    private void validateChangesAndSend()
    {
        String amount = editAmountText.getText().toString();
        String message = messageText.getText().toString();
        
        if(message.isEmpty())  {
            message = null;
        }

        boolean cancel = false;
        if (Strings.isBlank(amount)) {
            toast("Enter a valid amount for the trade.");
            cancel = true;
        } else if ( Doubles.convertToDouble(amount) > Doubles.convertToDouble(adMax)) {
            toast("Enter an amount lower than " + adMax + " " + currency);
            cancel = true;
        } else if (Doubles.convertToDouble(amount) < Doubles.convertToDouble(adMin)) {
            toast("Enter an amount greater than " + adMin + " " + currency);
            cancel = true;
        }

        if (!cancel) {
            sendTradeRequest(adId, amount, message);
        }
    }

    public void sendTradeRequest(String adId, String amount, String message)
    {
        Observable<ContactRequest> contactRequestObservable = dataService.createContact(adId, amount, message);
        contactRequestObservable
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<ContactRequest>()
                {
                    @Override
                    public void call(ContactRequest contactRequest)
                    {
                        snack("Contact request sent to " + profileName + "!", false);
                        finish();
                    }
                }, new Action1<Throwable>()
                {
                    @Override
                    public void call(Throwable throwable)
                    {
                        handleError(new Throwable("Unable to send trade request."), false);
                    }
                });
    }

    private void calculateBitcoinAmount(String amount)
    {
        if(Doubles.convertToDouble(amount) == 0) {
            editBitcoinText.setText("");
            return;
        }

        double value = (Doubles.convertToDouble(amount) / Doubles.convertToDouble(adPrice));
        editBitcoinText.setText(Conversions.formatBitcoinAmount(value));

        if( (Doubles.convertToDouble(amount) < Doubles.convertToDouble(adMin)) || (Doubles.convertToDouble(amount) > Doubles.convertToDouble(adMax)) ) {
            editAmountText.setTextColor(getResources().getColorStateList(R.color.red_light_up));
        } else {
            editAmountText.setTextColor(getResources().getColorStateList(R.color.light_green));
        }
    }

    private void calculateCurrencyAmount(String bitcoin)
    {
        if( Doubles.convertToDouble(bitcoin) == 0) {
            editAmountText.setText("");
            return;
        }

        double value = Doubles.convertToDouble(bitcoin) * Doubles.convertToDouble(adPrice);
        String amount = Conversions.formatCurrencyAmount(value);
        editAmountText.setText(amount);

        if( (Doubles.convertToDouble(amount) <  Doubles.convertToDouble(adMin)) || (Doubles.convertToDouble(amount) > Doubles.convertToDouble(adMax)) ) {
            editAmountText.setTextColor(getResources().getColorStateList(R.color.red_light_up));
        } else {
            editAmountText.setTextColor(getResources().getColorStateList(R.color.light_green));
        }
    }
}