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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.data.api.model.Advertisement;
import com.thanksmister.bitcoin.localtrader.data.api.model.ExchangeCurrency;
import com.thanksmister.bitcoin.localtrader.data.database.MethodItem;

import java.util.List;

import butterknife.InjectView;

public class EditSecurityFragment extends BaseEditFragment {

    @InjectView(R.id.liquidityCheckBox)
    CheckBox liquidityCheckBox;

    @InjectView(R.id.trustedCheckBox)
    CheckBox trustedCheckBox;

    @InjectView(R.id.smsVerifiedCheckBox)
    CheckBox smsVerifiedCheckBox;

    @InjectView(R.id.identifiedCheckBox)
    CheckBox identifiedCheckBox;
    
    public EditSecurityFragment() {
        // Required empty public constructor
    }

    public static EditSecurityFragment newInstance() {
        EditSecurityFragment fragment = new EditSecurityFragment();
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
        setHasOptionsMenu(false);
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ad_security_options, container, false);
    }

    @Override
    public void onViewCreated(View fragmentView, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(fragmentView, savedInstanceState);
        editAdvertisement = getEditAdvertisement();
        setAdvertisementOnView(editAdvertisement);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if(isAdded() &&  actionBar != null) {
            actionBar.setTitle(R.string.text_title_security_options);
        };
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public boolean validateChangesAndSave() {
        editAdvertisement.track_max_amount = liquidityCheckBox.isChecked();
        editAdvertisement.sms_verification_required = smsVerifiedCheckBox.isChecked();
        editAdvertisement.require_identification = identifiedCheckBox.isChecked();
        editAdvertisement.trusted_required = trustedCheckBox.isChecked();
        setEditAdvertisement(editAdvertisement);
        return true;
    }
    
    @Override
    protected void setAdvertisementOnView(@NonNull Advertisement editAdvertisement) {
        liquidityCheckBox.setChecked(editAdvertisement.track_max_amount);
        smsVerifiedCheckBox.setChecked(editAdvertisement.sms_verification_required);
        identifiedCheckBox.setChecked(editAdvertisement.require_identification);
        trustedCheckBox.setChecked(editAdvertisement.trusted_required);
    }

    // not implemented for this fragment
    @Override
    protected void setMethods(List<MethodItem> methods) {}
    @Override
    protected  void showAddressError() {}
    @Override
    protected  void displayAddress(Address address) {}
    @Override
    protected void onAddresses(List<Address> addresses) {}
    @Override
    protected void onCurrencies(List<ExchangeCurrency> currencies) {}
}