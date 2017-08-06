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

import android.content.Context;
import android.location.Address;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.data.api.model.Advertisement;
import com.thanksmister.bitcoin.localtrader.data.api.model.ExchangeCurrency;
import com.thanksmister.bitcoin.localtrader.data.api.model.TradeType;
import com.thanksmister.bitcoin.localtrader.data.database.MethodItem;
import com.thanksmister.bitcoin.localtrader.ui.adapters.CurrencyAdapter;
import com.thanksmister.bitcoin.localtrader.utils.CurrencyUtils;
import com.thanksmister.bitcoin.localtrader.utils.Strings;
import com.thanksmister.bitcoin.localtrader.utils.TradeUtils;

import java.util.List;

import butterknife.InjectView;

import static android.view.View.GONE;

public class EditMoreInfoFragment extends BaseEditFragment {

    private static String MARGIN = "1";
    
    @InjectView(R.id.currencyLayout)
    View currencyLayout;
    
    @InjectView(R.id.currencySpinner)
    Spinner currencySpinner;
    
    @InjectView(R.id.marginText)
    EditText marginText;
    
    @InjectView(R.id.editBankName)
    EditText editBankName;
    
    @InjectView(R.id.bankNameTitle)
    TextView bankNameTitle;
    
    @InjectView(R.id.bankNameLayout)
    View bankNameLayout;
    
    @InjectView(R.id.editPriceEquation)
    EditText editPriceEquation;
    
    @InjectView(R.id.editMaximumAmount)
    EditText editMaximumAmount;
    
    @InjectView(R.id.editMinimumAmount)
    EditText editMinimumAmount;

    @InjectView(R.id.editMinimumAmountCurrency)
    TextView editMinimumAmountCurrency;

    @InjectView(R.id.editMaximumAmountCurrency)
    TextView editMaximumAmountCurrency;
    
    @InjectView(R.id.editMessageText)
    EditText editMessageText;
    
    public EditMoreInfoFragment() {
        // Required empty public constructor
    }

    public static EditMoreInfoFragment newInstance() {
        EditMoreInfoFragment fragment = new EditMoreInfoFragment();
        return fragment;
    }
    
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(false);
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ad_more_info, container, false);
    }

    @Override
    public void onViewCreated(View fragmentView, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(fragmentView, savedInstanceState);
        currencySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                setEditPriceEquation();
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        
        marginText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                setEditPriceEquation();
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        editAdvertisement = getEditAdvertisement();
        setAdvertisementOnView(editAdvertisement);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if(isAdded() &&  actionBar != null) {
            actionBar.setTitle(R.string.text_title_more_info);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadCurrencies();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }
    
    // not implemented in this view
    @Override
    protected void onAddresses(List<Address> addresses) {}
    @Override
    protected void setMethods(List<MethodItem> methods) {}
    @Override
    protected void showAddressError() {}
    @Override
    protected void displayAddress(Address address) {}

    @Override
    protected void onCurrencies(List<ExchangeCurrency> currencies) {
        String currency = exchangeService.getExchangeCurrency();
        if(!TextUtils.isEmpty(editAdvertisement.currency)) {
            currency = editAdvertisement.currency;
        }
        currencies = CurrencyUtils.sortCurrencies(currencies);
        
        // we shouldn't have a blank currency, if so use the default 
        if(currencies.isEmpty()) {
            currencies.add(new ExchangeCurrency(currency));
        }
        
        CurrencyAdapter typeAdapter = new CurrencyAdapter(getActivity(), R.layout.spinner_layout, currencies);
        currencySpinner.setAdapter(typeAdapter);
        int i = 0;
        for (ExchangeCurrency exchangeCurrency : currencies) {
            if (exchangeCurrency.getCurrency().equals(currency)) {
                currencySpinner.setSelection(i);
                break;
            }
            i++;
        }
    }
    
    private void setEditPriceEquation() {
        String priceEquation;
        String margin = marginText.getText().toString();
        String currency = exchangeService.getExchangeCurrency();
        if(!TextUtils.isEmpty(editAdvertisement.currency)) {
            currency = editAdvertisement.currency;
        }
        if(currencySpinner.getAdapter() != null && !currencySpinner.getAdapter().isEmpty()) {
            currency = ((ExchangeCurrency) currencySpinner.getSelectedItem()).getCurrency(); 
        }
        if(TextUtils.isEmpty(margin)) {
            margin = MARGIN;
            marginText.setText(MARGIN);
        }
        if(TradeUtils.ALTCOIN_ETH.equals(editAdvertisement.online_provider)) {
            priceEquation = TradeUtils.getEthereumPriceEquation(editAdvertisement.trade_type, margin);
        } else {
            priceEquation = TradeUtils.getPriceEquation(editAdvertisement.trade_type, margin, currency);
        }
        editPriceEquation.setText(priceEquation);
    }

    @Override
    protected void setAdvertisementOnView(@NonNull Advertisement editAdvertisement) {
        
        editMessageText.setText(editAdvertisement.message);
        
        TradeType tradeType = editAdvertisement.trade_type;
        if (tradeType == TradeType.LOCAL_SELL || tradeType == TradeType.LOCAL_BUY) {
            bankNameLayout.setVisibility(GONE);
        } else if (!TextUtils.isEmpty((editAdvertisement.bank_name))) {
            String bankTitle = TradeUtils.getBankNameTitle(tradeType, editAdvertisement.online_provider);
            bankNameTitle.setText(bankTitle);
            bankNameLayout.setVisibility(View.VISIBLE);
            editBankName.setText(editAdvertisement.bank_name);
        }

        if(TradeUtils.ALTCOIN_ETH.equals(editAdvertisement.online_provider)) {
            currencyLayout.setVisibility(GONE);
            bankNameLayout.setVisibility(GONE);
        } else {
            currencyLayout.setVisibility(View.VISIBLE);
            editMinimumAmountCurrency.setText(editAdvertisement.currency);
            editMaximumAmountCurrency.setText(editAdvertisement.currency);
        }
        
        if(!TextUtils.isEmpty(editAdvertisement.min_amount)){
            editMinimumAmount.setText(editAdvertisement.min_amount);
        }

        if(!TextUtils.isEmpty(editAdvertisement.max_amount)){
            editMaximumAmount.setText(editAdvertisement.max_amount);
        }
        
        editMinimumAmountCurrency.setText(editAdvertisement.currency);
        editMaximumAmountCurrency.setText(editAdvertisement.currency);
        setEditPriceEquation();
    }

    @Override
    public boolean validateChangesAndSave() {
        
        String equation = editPriceEquation.getText().toString();
        String min = editMinimumAmount.getText().toString();
        String max = editMaximumAmount.getText().toString();
        String bankName = editBankName.getText().toString();

        if (TextUtils.isEmpty(equation)) {
            toast(getString(R.string.toast_price_equation_blank));
            return false;
        } else if (Strings.isBlank(min)) {
            toast(getString(R.string.toast_minimum_amount));
            return false;
        } else if (Strings.isBlank(max)) {
            toast(getString(R.string.toast_maximum_amount));
            return false;
        }

        TradeType tradeType = editAdvertisement.trade_type;
        if (tradeType == TradeType.ONLINE_SELL || tradeType == TradeType.ONLINE_BUY) {
            switch (editAdvertisement.online_provider) {
                case TradeUtils.NATIONAL_BANK:
                case TradeUtils.CASH_DEPOSIT:
                case TradeUtils.SPECIFIC_BANK:
                    if (TextUtils.isEmpty(bankName)) {
                        toast(getString(R.string.toast_bank_name_required));
                        return false;
                    }
                    editAdvertisement.bank_name = bankName;
                    break;
                case TradeUtils.OTHER:
                case TradeUtils.OTHER_REMITTANCE:
                case TradeUtils.OTHER_PRE_PAID_DEBIT:
                case TradeUtils.OTHER_ONLINE_WALLET_GLOBAL:
                case TradeUtils.OTHER_ONLINE_WALLET:
                    if (TextUtils.isEmpty(bankName)) {
                        toast(getString(R.string.toast_playment_method_blank));
                        return false;
                    }
                    editAdvertisement.bank_name = bankName;
                    break;
                case TradeUtils.INTERNATIONAL_WIRE_SWIFT:
                    if (TextUtils.isEmpty(bankName)) {
                        toast(getString(R.string.toast_swift_blank));
                        return false;
                    }
                    editAdvertisement.bank_name = bankName;
                    break;
                case TradeUtils.GIFT_CARD_CODE:
                    if (TextUtils.isEmpty(bankName)) {
                        toast(getString(R.string.toast_gift_card_blank));
                        return false;
                    }
                    editAdvertisement.bank_name = bankName;
                    break;
            }
        }
        
        String message = editMessageText.getText().toString();
        if(!TextUtils.isEmpty(message)) {
            editAdvertisement.message = message;
        }

        if(!TextUtils.isEmpty(equation)) {
            editAdvertisement.price_equation = equation;
        }

        if(!TextUtils.isEmpty(min)) {
            editAdvertisement.min_amount = min;
        }

        if(!TextUtils.isEmpty(max)) {
            editAdvertisement.max_amount = max;
        }
        
        setEditAdvertisement(editAdvertisement);
        
        return true;
    }
}