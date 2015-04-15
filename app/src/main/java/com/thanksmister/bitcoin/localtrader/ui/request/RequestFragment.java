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

package com.thanksmister.bitcoin.localtrader.ui.request;

import android.app.Activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.thanksmister.bitcoin.localtrader.BaseFragment;
import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.data.api.model.Wallet;
import com.thanksmister.bitcoin.localtrader.ui.MainActivity;
import com.thanksmister.bitcoin.localtrader.ui.misc.SpinnerAdapter;
import com.thanksmister.bitcoin.localtrader.ui.qrcode.QRCodeActivity;
import com.thanksmister.bitcoin.localtrader.ui.release.PinCodeActivity;
import com.thanksmister.bitcoin.localtrader.utils.Calculations;
import com.thanksmister.bitcoin.localtrader.utils.Conversions;
import com.thanksmister.bitcoin.localtrader.utils.Doubles;
import com.thanksmister.bitcoin.localtrader.utils.Strings;
import com.thanksmister.bitcoin.localtrader.utils.WalletUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import timber.log.Timber;

public class RequestFragment extends BaseFragment implements RequestView
{
    public static final String ARG_SECTION_NUMBER = "ARG_SECTION_NUMBER";
    public static final String EXTRA_ADDRESS = "com.thanksmister.extra.KEY_ADDRESS";
    public static final String EXTRA_AMOUNT = "com.thanksmister.extra.KEY_AMOUNT";
    public static final String EXTRA_WALLET_TYPE = "com.thanksmister.extra.EXTRA_WALLET_TYPE";

    public enum WalletTransactionType
    {
        REQUEST, SEND,
    }

    @Inject
    RequestPresenter presenter;

    @InjectView(android.R.id.progress)
    View progress;

    @InjectView(R.id.tradeContent)
    View content;

   /* @InjectView(R.id.toolbar)
    Toolbar toolbar;*/

    @InjectView(R.id.amountText)
    TextView amountText;

    @InjectView(R.id.usdEditText)
    TextView usdEditText;

    @InjectView(R.id.balanceText)
    TextView balance;

    @InjectView(R.id.address)
    TextView addressText;

    @InjectView(R.id.addressLayout)
    View addressLayout;

    @InjectView(R.id.qrButton)
    Button qrButton;

    @InjectView(R.id.sendButton)
    Button sendButton;

    @InjectView(R.id.balanceLayout)
    View balanceLayout;

    @InjectView(R.id.spinner)
    Spinner transactionSpinner;

    @InjectView(android.R.id.empty)
    View empty;

    @InjectView(R.id.emptyTextView)
    TextView emptyTextView;

    @OnClick(R.id.qrButton)
    public void qrButtonClicked()
    {
        validateForm();
    }

    @OnClick(R.id.sendButton)
    public void sendButtonClicked()
    {
        validateForm();
    }

    @OnClick(R.id.emptyRetryButton)
    public void emptyButtonClicked()
    {
        presenter.onResume();
    }

    private String address;
    private String amount;
    private WalletTransactionType transactionType = WalletTransactionType.SEND;
    private Wallet wallet;
    private Menu menu;

    public static RequestFragment newInstance(int sectionNumber, String address, String amount)
    {
        RequestFragment fragment = new RequestFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        args.putString(EXTRA_ADDRESS, address);
        args.putString(EXTRA_AMOUNT, amount);
        
        fragment.setArguments(args);
        return fragment;
    }

    public static RequestFragment newInstance(int sectionNumber, WalletTransactionType transactionType)
    {
        RequestFragment fragment = new RequestFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        args.putSerializable(EXTRA_WALLET_TYPE, transactionType);

        fragment.setArguments(args);
        return fragment;
    }

    public RequestFragment()
    {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            
            if (getArguments().containsKey(EXTRA_ADDRESS))
                address = getArguments().getString(EXTRA_ADDRESS);

            if (getArguments().containsKey(EXTRA_AMOUNT))
                amount = getArguments().getString(EXTRA_AMOUNT);

            if (getArguments().containsKey(EXTRA_WALLET_TYPE))
                transactionType = (WalletTransactionType) getArguments().getSerializable(EXTRA_WALLET_TYPE);
        } else {
            if(savedInstanceState.containsKey(EXTRA_ADDRESS))
                address = savedInstanceState.getString(EXTRA_ADDRESS);

            if(savedInstanceState.containsKey(EXTRA_AMOUNT))
                amount = savedInstanceState.getString(EXTRA_AMOUNT);

            if(savedInstanceState.containsKey(EXTRA_WALLET_TYPE))
                transactionType = (WalletTransactionType) savedInstanceState.getSerializable(EXTRA_WALLET_TYPE);
        }
        
        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent)
    {
        super.onActivityResult(requestCode, resultCode, intent);

        Timber.d("Result Code: " + resultCode);
        Timber.d("Request Code: " + requestCode);
        
        if(requestCode == PinCodeActivity.REQUEST_CODE) {
            if (resultCode == PinCodeActivity.RESULT_VERIFIED) {

                String pinCode = intent.getStringExtra(PinCodeActivity.EXTRA_PIN_CODE);
                String address = intent.getStringExtra(PinCodeActivity.EXTRA_ADDRESS);
                String amount = intent.getStringExtra(PinCodeActivity.EXTRA_AMOUNT);

                presenter.pinCodeEvent(pinCode, address, amount);
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
        this.menu = menu;
        inflater.inflate(R.menu.request, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
            case R.id.action_paste:
                if(transactionType == WalletTransactionType.SEND) {
                    presenter.setAmountFromClipboard();
                } else {
                    presenter.setAddressFromClipboard();
                }
                return true;
            case R.id.action_scan:
                presenter.scanQrCode();
                return true;
            default:
                break;
        }

        return false;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.view_send_request, container, false);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onAttach(Activity activity)
    {
        super.onAttach(activity);

        ((MainActivity) activity).onSectionAttached(getArguments().getInt(ARG_SECTION_NUMBER));
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);

        ButterKnife.inject(this, getActivity());
        
        amountText.addTextChangedListener(new TextWatcher(){
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3){
            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3){
            }
            @Override
            public void afterTextChanged(Editable editable){

                if (amountText.hasFocus()) {
                    String bitcoin = editable.toString();
                    calculateCurrencyAmount(bitcoin);
                }
            }
        });

        usdEditText.addTextChangedListener(new TextWatcher(){
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3){
            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3){
            }
            @Override
            public void afterTextChanged(Editable editable){

                if(usdEditText.hasFocus()) {
                    String amount = editable.toString();
                    calculateBitcoinAmount(amount);
                }
            }
        });

        transactionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                switch (arg2) {
                    case 0:
                        transactionType = WalletTransactionType.SEND;
                        setLayout(transactionType);
                        break;
                    case 1:
                        transactionType = WalletTransactionType.REQUEST;
                        setLayout(transactionType);
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0){
            }
        });


        if(!Strings.isBlank(amount)) {
            amountText.setText(amount);
        }

        if(!Strings.isBlank(address)) {
            addressText.setText(address);
        }

        String[] spinnerTitles = getResources().getStringArray(R.array.list_transaction_types);
        List<String> stringList = new ArrayList<String>(Arrays.asList(spinnerTitles));
        SpinnerAdapter transactionAdapter = new SpinnerAdapter(getContext(), R.layout.spinner_layout, stringList);
        setSpinnerAdapter(transactionAdapter);

        setLayout(transactionType);
    }

    @Override
    public void onResume()
    {
        super.onResume();

        presenter.onResume();
    }

    public void onDetach()
    {
        super.onDetach();

        ButterKnife.reset(this);

        presenter.onDestroy();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState)
    {
        super.onSaveInstanceState(outState);
        if(address != null)
            outState.putString(EXTRA_ADDRESS, address);

        if(amount != null)
            outState.putString(EXTRA_AMOUNT, amount);
        
        outState.putSerializable(EXTRA_WALLET_TYPE, transactionType);
    }

    @Override
    public void promptForPin(String bitcoinAddress, String bitcoinAmount)
    {
        Intent intent = PinCodeActivity.createStartIntent(getActivity(), bitcoinAddress, bitcoinAmount);
        startActivityForResult(intent, PinCodeActivity.REQUEST_CODE); // be sure to do this from fragment context
    }

    @Override
    public void showGeneratedQrCodeActivity(String bitcoinAddress, String bitcoinAmount)
    {
        Intent intent = QRCodeActivity.createStartIntent(getActivity(), bitcoinAddress, bitcoinAmount);
        startActivity(intent);
    }

    @Override
    public Context getContext()
    {
        return getActivity();
    }

    @Override
    public Fragment getFragmentContext()
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
        empty.setVisibility(View.GONE);
    }

    @Override
    public void hideProgress()
    {
        progress.setVisibility(View.GONE);
        content.setVisibility(View.VISIBLE);
        empty.setVisibility(View.GONE);
    }

    @Override
    public void setBitcoinAddress(String bitcoinAddress)
    {
        if(!Strings.isBlank(bitcoinAddress)) {
            addressText.setText(bitcoinAddress);
        }
    }

    @Override
    public void setAmount(String bitcoinAmount)
    {
        if(!Strings.isBlank(bitcoinAmount)) {
            amountText.setText(bitcoinAmount);
            calculateCurrencyAmount(bitcoinAmount);
        }
    }

    @Override
    public void resetWallet()
    {
        amountText.setText("");
        addressText.setText("");
        calculateCurrencyAmount("0.00");
    }

    @Override
    public void setWallet(Wallet wallet)
    {
        this.wallet = wallet;
        
        computeBalance(0);

        if(Strings.isBlank(amountText.getText())) {
            calculateCurrencyAmount("0.00");
        } else {
            calculateCurrencyAmount(amountText.getText().toString());
        }

        transactionSpinner.setSelection(transactionType == WalletTransactionType.SEND? 0: 1);
    }

    protected void validateForm()
    {
        boolean cancel = false;

        if(Strings.isBlank(amountText.getText())) {
            Toast.makeText(getContext(), getContext().getString(R.string.error_missing_address_amount), Toast.LENGTH_LONG).show();
            return;
        }

        String bitcoinAmount = Conversions.formatBitcoinAmount(amountText.getText().toString(), Conversions.MAXIMUM_BTC_DECIMALS, Conversions.MINIMUM_BTC_DECIMALS);
        String bitcoinAddress = (transactionType == WalletTransactionType.SEND)? addressText.getText().toString():wallet.address.address;

        if(transactionType == WalletTransactionType.SEND) {
            if(Strings.isBlank(addressText.getText())) {
                Toast.makeText(getContext(), getContext().getString(R.string.error_missing_address_amount), Toast.LENGTH_LONG).show();
                return;
            }

            if(bitcoinAmount == null) {
                Toast.makeText(getContext(), getContext().getString(R.string.toast_invalid_btc_amount), Toast.LENGTH_LONG).show();
                cancel = true;
            } else if (!WalletUtils.validAmount(bitcoinAmount)) {
                Toast.makeText(getContext(), getContext().getString(R.string.toast_invalid_btc_amount), Toast.LENGTH_LONG).show();
                cancel = true;
            } else if (!WalletUtils.validBitcoinAddress(bitcoinAddress)) {
                Toast.makeText(getContext(), getContext().getString(R.string.toast_invalid_address), Toast.LENGTH_LONG).show();
                cancel = true;
            }

            if (!cancel) { // There was an error
                String usd = Calculations.computedValueOfBitcoin(wallet.exchange.ask, wallet.exchange.bid, bitcoinAmount);
                promptForPin(bitcoinAddress, bitcoinAmount);
            }
        } else {
            showGeneratedQrCodeActivity(bitcoinAddress, bitcoinAmount);
        }
    }

    protected void setLayout(WalletTransactionType transactionType)
    {
        addressLayout.setVisibility(transactionType == WalletTransactionType.SEND?View.VISIBLE:View.GONE);
        qrButton.setVisibility(transactionType == WalletTransactionType.SEND?View.GONE:View.VISIBLE);
        sendButton.setVisibility(transactionType == WalletTransactionType.SEND?View.VISIBLE:View.GONE);
        balanceLayout.setVisibility(transactionType == WalletTransactionType.SEND?View.VISIBLE:View.GONE);

        if(menu != null) {
            MenuItem item = menu.findItem(R.id.action_scan);
            item.setVisible(transactionType == WalletTransactionType.SEND);
        }
    }

    protected void setSpinnerAdapter(SpinnerAdapter adapter)
    {
        transactionSpinner.setAdapter(adapter);
    }

    protected void computeBalance(double btcAmount)
    {
        double balanceAmount = Conversions.convertToDouble(wallet.total.balance);
        String btcBalance = Conversions.formatBitcoinAmount(balanceAmount - btcAmount);
        String value = wallet.getBitcoinValue();

        if(balanceAmount < btcAmount) {
            balance.setText(Html.fromHtml(getContext().getString(R.string.form_balance_negative, btcBalance, value)));
        } else {
            balance.setText(Html.fromHtml(getContext().getString(R.string.form_balance_positive, btcBalance, value)));
        }
    }

    private void calculateBitcoinAmount(String usd)
    {
        if(Doubles.convertToDouble(usd) == 0) {
            computeBalance(0);
            amountText.setText("");
            return;
        }

        double btc = Math.abs(Doubles.convertToDouble(usd) / Doubles.convertToDouble(wallet.getBitstampValue()));
        String amount = Conversions.formatBitcoinAmount(btc);
        amountText.setText(amount);

        computeBalance(Doubles.convertToDouble(amount));
    }

    private void calculateCurrencyAmount(String bitcoin)
    {
        if( Doubles.convertToDouble(bitcoin) == 0) {
            usdEditText.setText("");
            computeBalance(0);
            return;
        }

        computeBalance(Doubles.convertToDouble(bitcoin));
        String value = Calculations.computedValueOfBitcoin(wallet.exchange.ask, wallet.exchange.bid, bitcoin);
        usdEditText.setText(value);
    }
}