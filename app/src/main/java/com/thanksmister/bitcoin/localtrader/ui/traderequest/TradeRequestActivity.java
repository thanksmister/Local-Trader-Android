/*
 * Copyright (c) 2014. ThanksMister LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thanksmister.bitcoin.localtrader.ui.traderequest;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.thanksmister.bitcoin.localtrader.BaseActivity;
import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.utils.Calculations;
import com.thanksmister.bitcoin.localtrader.utils.Conversions;
import com.thanksmister.bitcoin.localtrader.utils.Doubles;
import com.thanksmister.bitcoin.localtrader.utils.Strings;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

public class TradeRequestActivity extends BaseActivity implements TradeRequestView
{
    public static final String EXTRA_AD_ID = "EXTRA_AD_ID";
    public static final String EXTRA_AD_PRICE = "EXTRA_AD_PRICE";
    public static final String EXTRA_AD_MIN_AMOUNT = "EXTRA_AD_MIN_AMOUNT";
    public static final String EXTRA_AD_MAX_AMOUNT = "EXTRA_AD_MAX_AMOUNT";
    public static final String EXTRA_AD_CURRENCY = "EXTRA_AD_CURRENCY";
    public static final String EXTRA_AD_PROFILE_NAME = "EXTRA_AD_PROFILE_NAME";

    @Inject
    TradeRequestPresenter presenter;

    @InjectView(android.R.id.progress)
    View progress;

    @InjectView(R.id.requestContent)
    ScrollView content;

    @InjectView(android.R.id.empty)
    View empty;

    @InjectView(R.id.retryTextView)
    TextView emptyTextView;

    @InjectView(R.id.toolbar)
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
            setToolBarMenu(toolbar);
        }

        tradeAmountTitle.setText(getContext().getString(R.string.trade_request_title, currency));
        tradeLimit.setText(getContext().getString(R.string.trade_request_limit, adMin, adMax, currency));
        tradeCurrency.setText(currency);

        editAmountText.setFilters(new InputFilter[] {new Calculations.DecimalPlacesInputFilter(2)});
        editAmountText.addTextChangedListener(new TextWatcher(){
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3)
            {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3)
            {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if(editAmountText.hasFocus()) {
                    String amount = editable.toString();
                    calculateBitcoinAmount(amount);
                }
            }
        });

        editBitcoinText.setFilters(new InputFilter[] {new Calculations.DecimalPlacesInputFilter(8)});
        editBitcoinText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3)
            {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3)
            {
            }

            @Override
            public void afterTextChanged(Editable editable) {
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
    public void setToolBarMenu(Toolbar toolbar)
    {
        
    }

    @Override
    public void onResume()
    {
        super.onResume();

        presenter.onResume();
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();

        ButterKnife.reset(this);

        presenter.onDestroy();
    }

    @Override
    public void onRefreshStop()
    {
        // TODO implement refresh
    }

    @Override
    public void onError(String message)
    {
        // TODO implement
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

    @Override
    protected List<Object> getModules()
    {
        return Arrays.<Object>asList(new TradeRequestModule(this));
    }

    @Override
    public Context getContext()
    {
        return this;
    }

    @Override
    public void showError(String message)
    {
        progress.setVisibility(View.GONE);
        content.setVisibility(View.GONE);
        empty.setVisibility(View.VISIBLE);
        emptyTextView.setText(message);
    }

    @Override
    public void showProgress()
    {
        progress.setVisibility(View.VISIBLE);
        content.setVisibility(View.GONE);
    }

    @Override
    public void hideProgress()
    {
        progress.setVisibility(View.GONE);
        content.setVisibility(View.VISIBLE);
    }

    private void validateChangesAndSend()
    {
        String amount = editAmountText.getText().toString();
       // String bitcoin = editBitcoinText.getText().toString();
        String message = messageText.getText().toString();
        if(message.isEmpty())  {
            message = null;
        }

        boolean cancel = false;
        if (Strings.isBlank(amount)) {
            Toast.makeText(getContext(), "Enter a valid amount for the trade.", Toast.LENGTH_SHORT).show();
            cancel = true;
        } else if ( Doubles.convertToDouble(amount) > Doubles.convertToDouble(adMax)) {
            Toast.makeText(getContext(), "Enter an amount lower than " + adMax + " " + currency, Toast.LENGTH_SHORT).show();
            cancel = true;
        } else if (Doubles.convertToDouble(amount) < Doubles.convertToDouble(adMin)) {
            Toast.makeText(getContext(), "Enter an amount greater than " + adMin + " " + currency, Toast.LENGTH_SHORT).show();
            cancel = true;
        }

        if (!cancel) {
            presenter.sendTradeRequest(adId, amount, message);
        }
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