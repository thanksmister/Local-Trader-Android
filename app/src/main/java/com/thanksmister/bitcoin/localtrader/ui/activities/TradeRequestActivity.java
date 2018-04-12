/*
 * Copyright (c) 2018 ThanksMister LLC
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
 *
 */

package com.thanksmister.bitcoin.localtrader.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.Html;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;
import com.thanksmister.bitcoin.localtrader.BuildConfig;
import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.events.AlertDialogEvent;
import com.thanksmister.bitcoin.localtrader.events.ProgressDialogEvent;
import com.thanksmister.bitcoin.localtrader.network.api.model.ContactRequest;
import com.thanksmister.bitcoin.localtrader.network.api.model.TradeType;
import com.thanksmister.bitcoin.localtrader.network.services.DataService;
import com.thanksmister.bitcoin.localtrader.ui.BaseActivity;
import com.thanksmister.bitcoin.localtrader.utils.Calculations;
import com.thanksmister.bitcoin.localtrader.utils.Conversions;
import com.thanksmister.bitcoin.localtrader.utils.Doubles;
import com.thanksmister.bitcoin.localtrader.utils.TradeUtils;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import timber.log.Timber;

public class TradeRequestActivity extends BaseActivity {

    public static final String EXTRA_AD_ID = "com.thanksmister.bitcoin.localtrader.EXTRA_AD_ID";
    public static final String EXTRA_AD_PRICE = "com.thanksmister.bitcoin.localtrader.EXTRA_AD_PRICE";
    public static final String EXTRA_AD_COUNTRY_CODE = "com.thanksmister.bitcoin.localtrader.EXTRA_AD_COUNTRY_CODE";
    public static final String EXTRA_AD_ONLINE_PROVIDER = "com.thanksmister.bitcoin.localtrader.EXTRA_AD_ONLINE_PROVIDER";
    public static final String EXTRA_AD_TRADE_TYPE = "com.thanksmister.bitcoin.localtrader.EXTRA_AD_TRADE_TYPE";
    public static final String EXTRA_AD_MIN_AMOUNT = "com.thanksmister.bitcoin.localtrader.EXTRA_AD_MIN_AMOUNT";
    public static final String EXTRA_AD_MAX_AMOUNT = "com.thanksmister.bitcoin.localtrader.EXTRA_AD_MAX_AMOUNT";
    public static final String EXTRA_AD_CURRENCY = "com.thanksmister.bitcoin.localtrader.EXTRA_AD_CURRENCY";
    public static final String EXTRA_AD_PROFILE_NAME = "com.thanksmister.bitcoin.localtrader.EXTRA_AD_PROFILE_NAME";

    @Inject
    DataService dataService;

    @BindView(R.id.tradeRequestToolbar)
    Toolbar toolbar;

    @BindView(R.id.editAmountText)
    EditText editAmountText;

    @BindView(R.id.editBitcoinText)
    EditText editBitcoinText;

    @BindView(R.id.tradeAmountTitle)
    TextView tradeAmountTitle;

    @BindView(R.id.tradeLimit)
    TextView tradeLimit;

    @BindView(R.id.tradeCurrency)
    TextView tradeCurrency;

    @BindView(R.id.detailsEthereumAddress)
    EditText detailsEthereumAddress;

    @BindView(R.id.detailsSortCode)
    EditText detailsSortCode;

    @BindView(R.id.detailsBSB)
    EditText detailsBSB;

    @BindView(R.id.detailsAccountNumber)
    EditText detailsAccountNumber;

    @BindView(R.id.detailsBillerCode)
    EditText detailsBillerCode;

    @BindView(R.id.detailsEthereumAddressLayout)
    TextInputLayout detailsEthereumAddressLayout;

    @BindView(R.id.detailsSortCodeLayout)
    TextInputLayout detailsSortCodeLayout;

    @BindView(R.id.detailsBSBLayout)
    TextInputLayout detailsBSBLayout;

    @BindView(R.id.detailsAccountNumberLayout)
    TextInputLayout detailsAccountNumberLayout;

    @BindView(R.id.detailsBillerCodeLayout)
    TextInputLayout detailsBillerCodeLayout;

    @BindView(R.id.detailsPhoneNumberLayout)
    TextInputLayout detailsPhoneNumberLayout;

    @BindView(R.id.detailsPhoneNumber)
    EditText detailsPhoneNumber;

    @BindView(R.id.detailsReceiverEmailLayout)
    TextInputLayout detailsReceiverEmailLayout;

    @BindView(R.id.detailsReceiverEmail)
    EditText detailsReceiverEmail;

    @BindView(R.id.detailsReceiverNameLayout)
    TextInputLayout detailsReceiverNameLayout;

    @BindView(R.id.detailsReceiverName)
    EditText detailsReceiverName;

    @BindView(R.id.detailsIbanLayout)
    TextInputLayout detailsIbanLayout;

    @BindView(R.id.detailsIbanName)
    EditText detailsIbanName;

    @BindView(R.id.detailsSwiftBicLayout)
    View detailsSwiftBicLayout;

    @BindView(R.id.detailsSwiftBic)
    EditText detailsSwiftBic;

    @BindView(R.id.detailsReferenceLayout)
    View detailsReferenceLayout;

    @BindView(R.id.detailsReference)
    EditText detailsReference;

    @BindView(R.id.tradeMessage)
    EditText tradeMessage;

    @BindView(R.id.tradeMessageLayout)
    TextInputLayout tradeMessageLayout;

    @BindView(R.id.ethereumAmountText)
    EditText editEtherAmountText;

    @BindView(R.id.bitcoinLayout)
    LinearLayout bitcoinLayout;

    @BindView(R.id.ethereumLayout)
    LinearLayout ethereumLayout;

    @BindView(R.id.fiatLayout)
    LinearLayout fiatLayout;

    @OnClick(R.id.sendButton)
    public void sendButtonClicked() {
        validateChangesAndSend();
    }

    private String adId;
    private String adPrice;
    private String adMin;
    private String adMax;
    private String currency;
    private String profileName;
    private TradeType tradeType = TradeType.NONE;
    private String countryCode;
    private String onlineProvider;

    public static Intent createStartIntent(Context context, String adId, @NonNull TradeType tradeType, String countryCode, String onlineProvider,
                                           String adPrice, String adMin, String adMax, String currency, String profileName) {

        Intent intent = new Intent(context, TradeRequestActivity.class);
        intent.putExtra(EXTRA_AD_ID, adId);
        intent.putExtra(EXTRA_AD_TRADE_TYPE, tradeType.name());
        intent.putExtra(EXTRA_AD_COUNTRY_CODE, countryCode);
        intent.putExtra(EXTRA_AD_ONLINE_PROVIDER, onlineProvider);
        intent.putExtra(EXTRA_AD_PRICE, adPrice);
        intent.putExtra(EXTRA_AD_MIN_AMOUNT, adMin);
        intent.putExtra(EXTRA_AD_MAX_AMOUNT, adMax);
        intent.putExtra(EXTRA_AD_CURRENCY, currency);
        intent.putExtra(EXTRA_AD_PROFILE_NAME, profileName);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.view_trade_request);

        ButterKnife.bind(this);

        if (savedInstanceState == null) {
            adId = getIntent().getStringExtra(EXTRA_AD_ID);
            String tradeTypeString = getIntent().getStringExtra(EXTRA_AD_TRADE_TYPE);
            if (!TextUtils.isEmpty(tradeTypeString)) {
                tradeType = TradeType.valueOf(getIntent().getStringExtra(EXTRA_AD_TRADE_TYPE));
            }
            countryCode = getIntent().getStringExtra(EXTRA_AD_COUNTRY_CODE);
            onlineProvider = getIntent().getStringExtra(EXTRA_AD_ONLINE_PROVIDER);
            adPrice = getIntent().getStringExtra(EXTRA_AD_PRICE);
            adMin = getIntent().getStringExtra(EXTRA_AD_MIN_AMOUNT);
            adMax = getIntent().getStringExtra(EXTRA_AD_MAX_AMOUNT);
            currency = getIntent().getStringExtra(EXTRA_AD_CURRENCY);
            profileName = getIntent().getStringExtra(EXTRA_AD_PROFILE_NAME);
        } else {
            adId = savedInstanceState.getString(EXTRA_AD_ID);
            String tradeTypeString = savedInstanceState.getString(EXTRA_AD_TRADE_TYPE);
            if (!TextUtils.isEmpty(tradeTypeString)) {
                tradeType = TradeType.valueOf(tradeTypeString);
            }
            countryCode = savedInstanceState.getString(EXTRA_AD_COUNTRY_CODE);
            onlineProvider = savedInstanceState.getString(EXTRA_AD_ONLINE_PROVIDER);
            adPrice = savedInstanceState.getString(EXTRA_AD_PRICE);
            adMin = savedInstanceState.getString(EXTRA_AD_MIN_AMOUNT);
            adMax = savedInstanceState.getString(EXTRA_AD_MAX_AMOUNT);
            currency = savedInstanceState.getString(EXTRA_AD_CURRENCY);
            profileName = savedInstanceState.getString(EXTRA_AD_PROFILE_NAME);
        }

        if (tradeType == null || TradeType.NONE.name().equals(tradeType.name())) {
            showAlertDialog(new AlertDialogEvent(getString(R.string.error_title), getString(R.string.error_invalid_trade_type)), new Action0() {
                @Override
                public void call() {
                    if (!BuildConfig.DEBUG) {
                        Crashlytics.logException(new Throwable("Bad trade type for requested trade: " + tradeType + " advertisement Id: " + adId));
                    }
                    finish();
                }
            });
            return;
        }

        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle(getString(R.string.text_trade_with, profileName));
            }
        }

        TextView tradeDescription = (TextView) findViewById(R.id.tradeDescription);
        tradeDescription.setText(Html.fromHtml(getString(R.string.trade_request_description)));
        tradeDescription.setMovementMethod(LinkMovementMethod.getInstance());

        tradeAmountTitle.setText(getString(R.string.trade_request_title, currency));

        if (adMin == null) {
            tradeLimit.setText("");
        } else if (adMax == null) {
            tradeLimit.setText(getString(R.string.trade_limit_min, adMin, currency));
        } else { // no maximum set
            tradeLimit.setText(getString(R.string.trade_limit, adMin, adMax, currency));
        }

        tradeCurrency.setText(currency);

        editAmountText.setFilters(new InputFilter[]{new Calculations.DecimalPlacesInputFilter(2)});
        editAmountText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (editAmountText.hasFocus()) {
                    String amount = editable.toString();
                    calculateBitcoinAmount(amount);
                }
            }
        });

        editBitcoinText.setFilters(new InputFilter[]{new Calculations.DecimalPlacesInputFilter(8)});
        editBitcoinText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void afterTextChanged(Editable editable) {

                if (editBitcoinText.hasFocus()) {
                    String bitcoin = editable.toString();
                    if (onlineProvider.equals(TradeUtils.ALTCOIN_ETH)) {
                        String ether = Calculations.calculateBitcoinToEther(bitcoin, adPrice);
                        boolean withinRange = Calculations.calculateEthereumWithinRange(ether, adMin, adMax);
                        if (!withinRange) {
                            editEtherAmountText.setTextColor(getResources().getColorStateList(R.color.red_light_up));
                        } else {
                            editEtherAmountText.setTextColor(getResources().getColorStateList(R.color.light_green));
                        }
                        editEtherAmountText.setText(ether);

                    } else {
                        calculateCurrencyAmount(bitcoin);
                    }
                }
            }
        });

        editEtherAmountText.setFilters(new InputFilter[]{new Calculations.DecimalPlacesInputFilter(18)});
        editEtherAmountText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (editEtherAmountText.hasFocus()) {
                    String ether = editable.toString();
                    boolean withinRange = Calculations.calculateEthereumWithinRange(ether, adMin, adMax);
                    if (!withinRange) {
                        editEtherAmountText.setTextColor(getResources().getColorStateList(R.color.red_light_up));
                    } else {
                        editEtherAmountText.setTextColor(getResources().getColorStateList(R.color.light_green));
                    }
                    String bitcoin = Calculations.calculateEtherToBitcoin(ether, adPrice);
                    editBitcoinText.setText(bitcoin);
                }
            }
        });

        showOptions();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(EXTRA_AD_ID, adId);
        outState.putString(EXTRA_AD_PRICE, adPrice);
        outState.putString(EXTRA_AD_MIN_AMOUNT, adMin);
        outState.putString(EXTRA_AD_MAX_AMOUNT, adMax);
        outState.putString(EXTRA_AD_CURRENCY, currency);
        outState.putString(EXTRA_AD_PROFILE_NAME, profileName);
        outState.putString(EXTRA_AD_TRADE_TYPE, tradeType.name());
        outState.putString(EXTRA_AD_COUNTRY_CODE, countryCode);
        outState.putString(EXTRA_AD_ONLINE_PROVIDER, onlineProvider);
    }

    private void showOptions() {

        if (tradeType == TradeType.ONLINE_BUY) {
            switch (onlineProvider) {
                case TradeUtils.NATIONAL_BANK:
                    switch (countryCode) {
                        case "UK":
                            detailsAccountNumberLayout.setVisibility(View.VISIBLE);
                            detailsSortCodeLayout.setVisibility(View.VISIBLE);
                            detailsReceiverNameLayout.setVisibility(View.VISIBLE);
                            detailsReferenceLayout.setVisibility(View.VISIBLE);
                            break;
                        case "AU":
                            detailsAccountNumberLayout.setVisibility(View.VISIBLE);
                            detailsBSBLayout.setVisibility(View.VISIBLE);
                            detailsReceiverNameLayout.setVisibility(View.VISIBLE);
                            detailsReferenceLayout.setVisibility(View.VISIBLE);
                            break;
                        case "FI":
                            detailsAccountNumberLayout.setVisibility(View.VISIBLE);
                            detailsIbanLayout.setVisibility(View.VISIBLE);
                            detailsSwiftBicLayout.setVisibility(View.VISIBLE);
                            detailsReceiverNameLayout.setVisibility(View.VISIBLE);
                            detailsReferenceLayout.setVisibility(View.VISIBLE);
                            break;
                    }
                    break;
                case TradeUtils.VIPPS:
                case TradeUtils.EASYPAISA:
                case TradeUtils.HAL_CASH:
                case TradeUtils.QIWI:
                case TradeUtils.LYDIA:
                case TradeUtils.SWISH:
                    detailsPhoneNumberLayout.setVisibility(View.VISIBLE);
                    break;
                case TradeUtils.PAYPAL:
                case TradeUtils.NETELLER:
                case TradeUtils.INTERAC:
                case TradeUtils.ALIPAY:
                    detailsReceiverEmailLayout.setVisibility(View.VISIBLE);
                    break;
                case TradeUtils.SEPA:
                    detailsReceiverNameLayout.setVisibility(View.VISIBLE);
                    detailsIbanLayout.setVisibility(View.VISIBLE);
                    detailsSwiftBicLayout.setVisibility(View.VISIBLE);
                    detailsReferenceLayout.setVisibility(View.VISIBLE);
                    break;
                case TradeUtils.BPAY:
                    detailsBillerCodeLayout.setVisibility(View.VISIBLE);
                    detailsReferenceLayout.setVisibility(View.VISIBLE);
                    break;
                case TradeUtils.PAYTM:
                    detailsPhoneNumberLayout.setVisibility(View.VISIBLE);
                    break;
                case TradeUtils.ALTCOIN_ETH:
                    fiatLayout.setVisibility(View.GONE);
                    ethereumLayout.setVisibility(View.VISIBLE);
                    detailsEthereumAddressLayout.setVisibility(View.VISIBLE);
                    break;
                case TradeUtils.MOBILEPAY_DANSKE_BANK:
                case TradeUtils.MOBILEPAY_DANSKE_BANK_DK:
                case TradeUtils.MOBILEPAY_DANSKE_BANK_NO:
                    detailsPhoneNumberLayout.setVisibility(View.VISIBLE);
                    break;
            }
        } else if (tradeType == TradeType.ONLINE_SELL) {
            switch (onlineProvider) {
                case TradeUtils.PAYTM:
                case TradeUtils.QIWI:
                case TradeUtils.SWISH:
                    detailsPhoneNumberLayout.setVisibility(View.VISIBLE);
                    break;
                case TradeUtils.MOBILEPAY_DANSKE_BANK:
                case TradeUtils.MOBILEPAY_DANSKE_BANK_DK:
                case TradeUtils.MOBILEPAY_DANSKE_BANK_NO:
                    detailsPhoneNumberLayout.setVisibility(View.VISIBLE);
                    break;
                case TradeUtils.ALTCOIN_ETH:
                    fiatLayout.setVisibility(View.GONE);
                    ethereumLayout.setVisibility(View.VISIBLE);
                    detailsEthereumAddressLayout.setVisibility(View.VISIBLE);
                    break;
            }
        }
    }

    private void validateChangesAndSend() {

        String amount = editAmountText.getText().toString();
        if (onlineProvider.equals(TradeUtils.ALTCOIN_ETH)) {
            amount = editEtherAmountText.getText().toString();
        }
        boolean cancel = false;
        try {
            if (TextUtils.isEmpty(amount)) {
                toast(getString(R.string.toast_valid_trade_amount));
                cancel = true;
            } else if (!TextUtils.isEmpty(adMax) && Doubles.convertToDouble(amount) > Doubles.convertToDouble(adMax)) {
                toast(getString(R.string.toast_enter_lower_amount, adMax, currency));
                cancel = true;
            } else if (Doubles.convertToDouble(amount) < Doubles.convertToDouble(adMin)) {
                toast(getString(R.string.toast_enter_higher_amount, adMin, currency));
                cancel = true;
            }
        } catch (Exception e) {
            Timber.e(e.getMessage());
            cancel = true;
        }

        String phone = detailsPhoneNumber.getText().toString();
        String receiverEmail = detailsReceiverEmail.getText().toString();
        String receiverName = detailsReceiverName.getText().toString();
        String reference = detailsReference.getText().toString();
        String bic = detailsSwiftBic.getText().toString();
        String iban = detailsIbanName.getText().toString();
        String ethereumAddress = detailsEthereumAddress.getText().toString();
        String accountNumber = detailsAccountNumber.getText().toString();
        String sortCode = detailsSortCode.getText().toString();
        String billerCode = detailsBillerCode.getText().toString();
        String bsb = detailsBSB.getText().toString();

        if (tradeType == TradeType.ONLINE_BUY) {
            switch (onlineProvider) {
                case TradeUtils.NATIONAL_BANK:
                    switch (countryCode) {
                        case "UK":
                            if (TextUtils.isEmpty(sortCode)
                                    || TextUtils.isEmpty(receiverName)
                                    || TextUtils.isEmpty(reference)
                                    || TextUtils.isEmpty(accountNumber)) {
                                toast(getString(R.string.toast_complete_all_fields));
                            }
                            break;
                        case "AU":
                            if (TextUtils.isEmpty(bsb)
                                    || TextUtils.isEmpty(receiverName)
                                    || TextUtils.isEmpty(reference)
                                    || TextUtils.isEmpty(accountNumber)) {
                                toast(getString(R.string.toast_complete_all_fields));
                            }
                            break;
                        case "FI":
                            if (TextUtils.isEmpty(iban)
                                    || TextUtils.isEmpty(receiverName)
                                    || TextUtils.isEmpty(bic)
                                    || TextUtils.isEmpty(reference)
                                    || TextUtils.isEmpty(accountNumber)) {
                                toast(getString(R.string.toast_complete_all_fields));
                                cancel = true;
                            }
                            break;
                    }
                    break;
                case TradeUtils.ALTCOIN_ETH:
                    if (TextUtils.isEmpty(ethereumAddress)) {
                        toast(getString(R.string.toast_complete_all_fields));
                        cancel = true;
                    }
                    break;
                case TradeUtils.VIPPS:
                case TradeUtils.EASYPAISA:
                case TradeUtils.HAL_CASH:
                case TradeUtils.QIWI:
                case TradeUtils.LYDIA:
                case TradeUtils.SWISH:
                    if (TextUtils.isEmpty(phone)) {
                        toast(getString(R.string.toast_complete_all_fields));
                        cancel = true;
                    }
                    break;
                case TradeUtils.PAYPAL:
                case TradeUtils.NETELLER:
                case TradeUtils.INTERAC:
                case TradeUtils.ALIPAY:
                    if (TextUtils.isEmpty(receiverEmail)) {
                        toast(getString(R.string.toast_complete_all_fields));
                        cancel = true;
                    }
                    break;
                case TradeUtils.MOBILEPAY_DANSKE_BANK:
                case TradeUtils.MOBILEPAY_DANSKE_BANK_DK:
                case TradeUtils.MOBILEPAY_DANSKE_BANK_NO:
                    if (TextUtils.isEmpty(phone)) {
                        toast(getString(R.string.toast_complete_all_fields));
                        cancel = true;
                    }
                    break;
                case TradeUtils.BPAY:
                    if (TextUtils.isEmpty(billerCode) || TextUtils.isEmpty(reference)) {
                        toast(getString(R.string.toast_complete_all_fields));
                        cancel = true;
                    }
                    break;
                case TradeUtils.SEPA:
                    if (TextUtils.isEmpty(receiverName) || TextUtils.isEmpty(iban) || TextUtils.isEmpty(bic) || TextUtils.isEmpty(reference)) {
                        toast(getString(R.string.toast_complete_all_fields));
                        cancel = true;
                    }
                    break;
            }
        } else if (tradeType == TradeType.ONLINE_SELL) {
            switch (onlineProvider) {
                case TradeUtils.QIWI:
                case TradeUtils.SWISH:
                case TradeUtils.MOBILEPAY_DANSKE_BANK:
                case TradeUtils.MOBILEPAY_DANSKE_BANK_DK:
                case TradeUtils.MOBILEPAY_DANSKE_BANK_NO:
                    if (TextUtils.isEmpty(phone)) {
                        toast(getString(R.string.toast_complete_all_fields));
                        cancel = true;
                    }
                    break;
            }
        }
        String message = "";
        if (!TextUtils.isEmpty(tradeMessage.getText().toString())) {
            message = tradeMessage.getText().toString();
        }

        if (!cancel) {
            sendTradeRequest(adId, amount, receiverName, phone, receiverEmail, iban, bic, reference, message, sortCode, billerCode, accountNumber, bsb, ethereumAddress);
        }
    }

    public void sendTradeRequest(String adId, String amount, String name, String phone,
                                 String email, String iban, String bic, String reference, String message,
                                 String sortCode, String billerCode, String accountNumber, String bsb, String ethereumAddress) {

        showProgressDialog(new ProgressDialogEvent(getString(R.string.progress_sending_trade_request)));

        dataService.createContact(
                adId, tradeType, countryCode, onlineProvider,
                amount, name, phone, email,
                iban, bic, reference, message,
                sortCode, billerCode, accountNumber,
                bsb, ethereumAddress)

                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<ContactRequest>() {
                    @Override
                    public void call(ContactRequest contactRequest) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                hideProgressDialog();
                                toast(getString(R.string.toast_trade_request_sent) + profileName + "!");
                                finish();
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
                                handleError(throwable);
                            }
                        });
                    }

                });
    }

    private void calculateBitcoinAmount(String amount) {
        try {
            if (TextUtils.isEmpty(amount) || amount.equals("0")) {
                editBitcoinText.setText("");
                return;
            }
        } catch (Exception e) {
            reportError(e);
            return;
        }

        try {
            double value = (Doubles.convertToDouble(amount) / Doubles.convertToDouble(adPrice));
            editBitcoinText.setText(Conversions.formatBitcoinAmount(value));

            if ((Doubles.convertToDouble(amount) < Doubles.convertToDouble(adMin)) || (Doubles.convertToDouble(amount) > Doubles.convertToDouble(adMax))) {
                editAmountText.setTextColor(getResources().getColorStateList(R.color.red_light_up));
            } else {
                editAmountText.setTextColor(getResources().getColorStateList(R.color.light_green));
            }
        } catch (Exception e) {
            Timber.e(e.getMessage());
        }
    }

    private void calculateCurrencyAmount(String bitcoin) {
        if (TextUtils.isEmpty(bitcoin) || bitcoin.equals("0")) {
            editAmountText.setText("");
            return;
        }

        try {
            double value = Doubles.convertToDouble(bitcoin) * Doubles.convertToDouble(adPrice);
            String amount = Conversions.formatCurrencyAmount(value);
            editAmountText.setText(amount);

            if ((Doubles.convertToDouble(amount) < Doubles.convertToDouble(adMin)) || (Doubles.convertToDouble(amount) > Doubles.convertToDouble(adMax))) {
                editAmountText.setTextColor(getResources().getColorStateList(R.color.red_light_up));
            } else {
                editAmountText.setTextColor(getResources().getColorStateList(R.color.light_green));
            }
        } catch (Exception e) {
            Timber.e(e.getMessage());
        }
    }
}