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
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Spinner;
import android.widget.TextView;

import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.data.api.model.Advertisement;
import com.thanksmister.bitcoin.localtrader.data.api.model.ExchangeCurrency;
import com.thanksmister.bitcoin.localtrader.data.api.model.TradeType;
import com.thanksmister.bitcoin.localtrader.data.database.MethodItem;
import com.thanksmister.bitcoin.localtrader.events.AlertDialogEvent;
import com.thanksmister.bitcoin.localtrader.ui.adapters.MethodAdapter;
import com.thanksmister.bitcoin.localtrader.ui.adapters.PredictAdapter;
import com.thanksmister.bitcoin.localtrader.ui.adapters.SpinnerAdapter;
import com.thanksmister.bitcoin.localtrader.utils.SearchUtils;
import com.thanksmister.bitcoin.localtrader.utils.TradeUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import butterknife.InjectView;
import butterknife.OnClick;
import timber.log.Timber;

import static android.view.View.GONE;

public class EditTypeFragment extends BaseEditFragment {
    
    @InjectView(R.id.paymentMethodLayout)
    View paymentMethodLayout;

    @InjectView(R.id.paymentMethodSpinner)
    Spinner paymentMethodSpinner;
    
    @InjectView(R.id.typeSpinner)
    Spinner typeSpinner;

    @InjectView(R.id.autoCompleteTextView)
    AutoCompleteTextView editLocationText;

    @InjectView(R.id.locationText)
    TextView locationText;
    
    @OnClick(R.id.clearButton)
    public void clearButtonClicked() {
        showEditTextLayout();
    }
    
    public EditTypeFragment() {
    }
    
    public static EditTypeFragment newInstance() {
        EditTypeFragment fragment = new EditTypeFragment();
        return fragment;
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
                return false;
            default:
                break;
        }

        return false;
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ad_type, container, false);
    }

    @Override
    public void onViewCreated(View fragmentView, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(fragmentView, savedInstanceState);
        
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
        
        String[] typeTitles = getResources().getStringArray(R.array.list_advertisement_type_spinner);
        List<String> typeList = new ArrayList<>(Arrays.asList(typeTitles));
        SpinnerAdapter typeAdapter = new SpinnerAdapter(getActivity(), R.layout.spinner_layout, typeList);
        typeSpinner.setAdapter(typeAdapter);
        typeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                editAdvertisement.trade_type = TradeType.values()[i];
                setPaymentMethodLayout(editAdvertisement.trade_type);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
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
            actionBar.setTitle(R.string.text_title_trade_type);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }
    
    public void setMethods(List<MethodItem> methods) {
        List<MethodItem> removedAllFromMethods = new ArrayList<>();
        for (MethodItem methodItem : methods) {
            if(!methodItem.code().equals("all")) {
                removedAllFromMethods.add(methodItem); 
            }
        }
        MethodAdapter typeAdapter = new MethodAdapter(getActivity(), R.layout.spinner_layout, removedAllFromMethods);
        paymentMethodSpinner.setAdapter(typeAdapter);
    }
    
    private void setPaymentMethodLayout(TradeType tradeType) {
        if (TradeUtils.isLocalTrade(tradeType)) {
            paymentMethodLayout.setVisibility(GONE);
        } else {
            paymentMethodLayout.setVisibility(View.VISIBLE);
        }
    }

    protected void showEditTextLayout() {
        locationText.setVisibility(View.INVISIBLE);
        editLocationText.setVisibility(View.VISIBLE);
        editLocationText.setText("");
        editLocationText.requestFocus();
    }

    protected void showLocationLayout() {
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
            locationText.setText(shortAddress);
            editAdvertisement.location = shortAddress;
            editAdvertisement.city = address.getLocality();
            editAdvertisement.country_code = address.getCountryCode();
            editAdvertisement.lon = address.getLongitude();
            editAdvertisement.lat = address.getLatitude();
            showLocationLayout();
        } else {
            showEditTextLayout();
        }

        try {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
        } catch (NullPointerException e) {
            Timber.w("Error closing keyboard");
        }
    }

    /**
     * Return from auto-type address lookup
     * @param addresses List of <code>Address</code>
     */
    @Override
    public void onAddresses(List<Address> addresses) {
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
    protected void onCurrencies(List<ExchangeCurrency> currencies) {}
    
    @Override
    protected void setAdvertisementOnView(@NonNull Advertisement editAdvertisement) {
        setPaymentMethodLayout(editAdvertisement.trade_type);
        if(!TextUtils.isEmpty(editAdvertisement.location)) {
            locationText.setText(editAdvertisement.location);
            showLocationLayout();
        } else {
            showEditTextLayout();
        }
    }

    /**
     * Validate changes and commit them 
     */
    @Override
    public boolean validateChangesAndSave() {
        String countryCode = editAdvertisement.country_code;
        String location = editAdvertisement.location;
        if(TextUtils.isEmpty(location) || TextUtils.isEmpty(countryCode)) {
            toast(getString(R.string.toast_valid_address_required));
            return false;
        }
        
        editAdvertisement.online_provider = ((MethodItem) paymentMethodSpinner.getSelectedItem()).code();
        setEditAdvertisement(editAdvertisement);
        return true;
    }
}