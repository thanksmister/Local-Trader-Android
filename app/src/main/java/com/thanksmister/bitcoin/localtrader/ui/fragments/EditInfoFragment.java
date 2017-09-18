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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.data.api.model.Advertisement;
import com.thanksmister.bitcoin.localtrader.data.api.model.ExchangeCurrency;
import com.thanksmister.bitcoin.localtrader.data.api.model.TradeType;
import com.thanksmister.bitcoin.localtrader.data.database.MethodItem;
import com.thanksmister.bitcoin.localtrader.events.AlertDialogEvent;
import com.thanksmister.bitcoin.localtrader.ui.adapters.CurrencyAdapter;
import com.thanksmister.bitcoin.localtrader.ui.adapters.PredictAdapter;
import com.thanksmister.bitcoin.localtrader.utils.CurrencyUtils;
import com.thanksmister.bitcoin.localtrader.utils.SearchUtils;
import com.thanksmister.bitcoin.localtrader.utils.Strings;
import com.thanksmister.bitcoin.localtrader.utils.TradeUtils;

import java.util.ArrayList;
import java.util.List;

import butterknife.InjectView;
import butterknife.OnClick;

import static android.view.View.GONE;

public class EditInfoFragment extends BaseEditFragment {

    @InjectView(R.id.currencySpinner)
    Spinner currencySpinner;
    
    @InjectView(R.id.locationText)
    TextView locationText;
    
    @InjectView(R.id.locationDescriptionText)
    TextView locationDescriptionText;

    @InjectView(R.id.bankNameTitle)
    TextView bankNameTitle;
    
    @InjectView(R.id.bankNameLayout)
    View bankNameLayout;
    
    @InjectView(R.id.currencyLayout)
    View currencyLayout;

    @InjectView(R.id.editBankName)
    EditText editBankName;

    @InjectView(R.id.editMaximumAmountCurrency)
    TextView editMaximumAmountCurrency;

    @InjectView(R.id.editMinimumAmountCurrency)
    TextView editMinimumAmountCurrency;

    @InjectView(R.id.editMaximumAmount)
    EditText editMaximumAmount;

    @InjectView(R.id.editMinimumAmount)
    EditText editMinimumAmount;

    @InjectView(R.id.editPriceEquation)
    EditText editPriceEquation; 
    
    @InjectView(R.id.editMessageText)
    EditText editMessageText;

    @InjectView(R.id.activeCheckBox)
    CheckBox activeCheckBox;

    @InjectView(R.id.autoCompleteTextView)
    AutoCompleteTextView editLocationText;
    
    @OnClick(R.id.clearButton)
    public void clearButtonClicked() {
        showEditTextLayout();
    }
    
    public EditInfoFragment() {
        // Required empty public constructor
    }

    @Override
    protected void setMethods(List<MethodItem> methods) {
        // nothing needed
    }

    public static EditInfoFragment newInstance() {
        EditInfoFragment fragment = new EditInfoFragment();
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
    public void onResume() {
        super.onResume();
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.edit, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_location:
                checkLocationEnabled();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ad_info, container, false);
    }

    @Override
    public void onViewCreated(View fragmentView, @Nullable Bundle savedInstanceState) {
        
        super.onViewCreated(fragmentView, savedInstanceState);
        
        locationDescriptionText.setText(R.string.text_location_online_description);

        editLocationText.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Address address = predictAdapter.getItem(i);
                displayAddress(address);
            }
        });

        editLocationText.addTextChangedListener(new TextWatcher() {
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
        editLocationText.setAdapter(predictAdapter);
        
        currencySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                ExchangeCurrency exchange = (ExchangeCurrency) currencySpinner.getAdapter().getItem(i);
                editMinimumAmountCurrency.setText(exchange.getCurrency());
                editMaximumAmountCurrency.setText(exchange.getCurrency());
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        editAdvertisement = getEditAdvertisement();
        setAdvertisementOnView(editAdvertisement);
        showLocationLayout();
        loadCurrencies();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if(isAdded() &&  actionBar != null) {
            actionBar.setTitle(R.string.text_title_advertisement_info);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }
    
    private void showEditTextLayout() {
        locationText.setVisibility(GONE);
        locationText.setVisibility(View.INVISIBLE);
        editLocationText.setVisibility(View.VISIBLE);
        editLocationText.setText("");
        editLocationText.requestFocus();
    }

    private void showLocationLayout() {
        editLocationText.setVisibility(View.INVISIBLE);
        locationText.setVisibility(View.VISIBLE);
    }
    
    @Override
    public void showAddressError() {
        showAlertDialog(new AlertDialogEvent(getString(R.string.error_address_title), getString(R.string.error_dialog_no_address_edit)));
    }

    @Override
    public void displayAddress(Address address) {
        String shortAddress = SearchUtils.getDisplayAddress(address);
        if (!TextUtils.isEmpty(shortAddress)) {
            editAdvertisement.location = shortAddress;
            editAdvertisement.city = address.getLocality();
            editAdvertisement.country_code = address.getCountryCode();
            editAdvertisement.lon = address.getLongitude();
            editAdvertisement.lat = address.getLatitude();
            locationText.setText(shortAddress);
            showLocationLayout();
        } else {
            showEditTextLayout();
        }
    }

    @Override
    protected void onAddresses(List<Address> addresses) {
        List<Address> validAddresses = new ArrayList<>();
        for(Address address : addresses) {
            if(!TextUtils.isEmpty(address.getCountryCode())) {
                validAddresses.add(address);
            }
        }
        if(predictAdapter != null) {
            predictAdapter.replaceWith(validAddresses);
        }
    }

    @Override
    protected void onCurrencies(List<ExchangeCurrency> currencies) {
        if(editAdvertisement != null) {
            currencies = CurrencyUtils.sortCurrencies(currencies);
            CurrencyAdapter typeAdapter = new CurrencyAdapter(getActivity(), R.layout.spinner_layout, currencies);
            currencySpinner.setAdapter(typeAdapter);
            int i = 0;
            for (ExchangeCurrency currency : currencies) {
                if (currency.getCurrency().equals(editAdvertisement.currency)) {
                    currencySpinner.setSelection(i);
                    break;
                }
                i++;
            } 
        }
    }

    @Override
    public boolean validateChangesAndSave() {
        
        if(TextUtils.isEmpty(locationText.getText())) {
            toast(getString(R.string.toast_valid_address_required));
            return false;
        }
        
        editAdvertisement.visible = activeCheckBox.isChecked();
       
        String min = editMinimumAmount.getText().toString();
        String bankName = editBankName.getText().toString();
        String max = editMaximumAmount.getText().toString();
        String equation = editPriceEquation.getText().toString();
      
        TradeType tradeType = editAdvertisement.trade_type;
        
        if (tradeType == TradeType.ONLINE_SELL) {
            switch (editAdvertisement.online_provider) {
                case TradeUtils.QIWI:
                case TradeUtils.SWISH:
                case TradeUtils.MOBILEPAY_DANSKE_BANK:
                case TradeUtils.MOBILEPAY_DANSKE_BANK_DK:
                case TradeUtils.MOBILEPAY_DANSKE_BANK_NO:
                    break;
            }
        } else if (tradeType == TradeType.ONLINE_BUY) {
            switch (editAdvertisement.online_provider) {
                case TradeUtils.NATIONAL_BANK:
                case TradeUtils.CASH_DEPOSIT:
                case TradeUtils.SPECIFIC_BANK:
                    if (TextUtils.isEmpty(bankName)) {
                        toast("Bank name can't be blank.");
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
                        toast("Payment method can't be blank.");
                        return false;
                    }
                    editAdvertisement.bank_name = bankName;
                    break;
                case TradeUtils.INTERNATIONAL_WIRE_SWIFT:
                     if (TextUtils.isEmpty(bankName)) {
                        toast("SWIFT or Bic can't be blank.");
                         return false;
                    }
                    editAdvertisement.bank_name = bankName;
                    break;
                case TradeUtils.GIFT_CARD_CODE:
                    if (TextUtils.isEmpty(bankName)) {
                        toast("Gift card issuer can't be blank.");
                        return false;
                    }
                    editAdvertisement.bank_name = bankName;
                    break;
            }
        }

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
        
        editAdvertisement.message = editMessageText.getText().toString();
        editAdvertisement.price_equation = equation;
        editAdvertisement.min_amount = String.valueOf(TradeUtils.convertCurrencyAmount(min));
        editAdvertisement.max_amount = String.valueOf(TradeUtils.convertCurrencyAmount(max));
        
        setEditAdvertisement(editAdvertisement);

        return true;
    }
    

    @Override
    public void setAdvertisementOnView(@NonNull Advertisement editAdvertisement) {
        
        TradeType tradeType = editAdvertisement.trade_type;

        activeCheckBox.setChecked(editAdvertisement.visible);
        
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
            editMinimumAmountCurrency.setText(getString(R.string.eth));
            editMaximumAmountCurrency.setText(getString(R.string.eth));
        } else {
            currencyLayout.setVisibility(View.VISIBLE);
            editMinimumAmountCurrency.setText(editAdvertisement.currency);
            editMaximumAmountCurrency.setText(editAdvertisement.currency);
        }

        if(!TextUtils.isEmpty(editAdvertisement.min_amount)) {
            editMinimumAmount.setText(editAdvertisement.min_amount);
        }

        if(!TextUtils.isEmpty(editAdvertisement.max_amount)) {
            editMaximumAmount.setText(editAdvertisement.max_amount);
        }

        if(!TextUtils.isEmpty(editAdvertisement.price_equation)) {
            editPriceEquation.setText(editAdvertisement.price_equation);
        }
        
        if(!TextUtils.isEmpty(editAdvertisement.message)) {
            editMessageText.setText(editAdvertisement.message);
        }
        
        if(!TextUtils.isEmpty(editAdvertisement.location)) {
            locationText.setText(editAdvertisement.location);
        }
    }
}