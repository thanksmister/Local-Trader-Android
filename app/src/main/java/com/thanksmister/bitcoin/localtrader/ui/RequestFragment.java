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

package com.thanksmister.bitcoin.localtrader.ui;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
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

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import com.thanksmister.bitcoin.localtrader.BaseActivity;
import com.thanksmister.bitcoin.localtrader.BaseFragment;
import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.data.api.model.Exchange;
import com.thanksmister.bitcoin.localtrader.data.api.model.Wallet;
import com.thanksmister.bitcoin.localtrader.data.database.DbManager;
import com.thanksmister.bitcoin.localtrader.data.database.ExchangeItem;
import com.thanksmister.bitcoin.localtrader.data.database.WalletItem;
import com.thanksmister.bitcoin.localtrader.data.services.DataService;
import com.thanksmister.bitcoin.localtrader.events.ProgressDialogEvent;
import com.thanksmister.bitcoin.localtrader.events.RefreshEvent;
import com.thanksmister.bitcoin.localtrader.ui.misc.SpinnerAdapter;
import com.thanksmister.bitcoin.localtrader.ui.bitcoin.QRCodeActivity;
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
import rx.Observable;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func2;
import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;

import static rx.android.app.AppObservable.bindFragment;

public class RequestFragment extends BaseFragment implements SwipeRefreshLayout.OnRefreshListener
{
    public static final String EXTRA_ADDRESS = "com.thanksmister.extra.EXTRA_ADDRESS";
    public static final String EXTRA_AMOUNT = "com.thanksmister.extra.EXTRA_AMOUNT";
    public static final String EXTRA_WALLET_TYPE = "com.thanksmister.extra.EXTRA_WALLET_TYPE";

    public enum WalletTransactionType
    {
        REQUEST, SEND,
    }

    @Inject
    DataService dataService;
    
    @Inject
    DbManager dbManager;

    @Inject
    Bus bus;

    @InjectView(R.id.swipeLayout)
    SwipeRefreshLayout swipeLayout;

    @InjectView(R.id.toolbar)
    Toolbar toolbar;
    
    @InjectView(R.id.tradeContent)
    View content;

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

    private String address;
    private String amount;
    private WalletTransactionType transactionType = WalletTransactionType.SEND;
    private Menu menu;

    CompositeSubscription subscriptions = new CompositeSubscription();
    CompositeSubscription updateSubscriptions = new CompositeSubscription();
    
    private Observable<WalletItem> walletBalanceObservable;
    private Observable<ExchangeItem> exchangeObservable;
    private Observable<Wallet> updateWalletBalanceObservable;
    private Observable<Exchange> updateExchangeObservable;
    private Observable<Boolean> sendPinCodeMoneyObservable;

    private Handler handler;
    private WalletData walletData;

    private class WalletData {
        public WalletItem wallet;
        public ExchangeItem exchange;
    }

    private class WalletUpdateData {
        public Wallet wallet;
        public Exchange exchange;
    }

    public static RequestFragment newInstance(String address, String amount)
    {
        RequestFragment fragment = new RequestFragment();
        Bundle args = new Bundle();
        args.putString(EXTRA_ADDRESS, address);
        args.putString(EXTRA_AMOUNT, amount);
        
        fragment.setArguments(args);
        return fragment;
    }

    public static RequestFragment newInstance(WalletTransactionType transactionType)
    {
        RequestFragment fragment = new RequestFragment();
        Bundle args = new Bundle();
        args.putSerializable(EXTRA_WALLET_TYPE, transactionType);

        fragment.setArguments(args);
        return fragment;
    }

    public RequestFragment()
    {
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // refresh handler
        handler = new Handler();

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
        
        if(requestCode == PinCodeActivity.REQUEST_CODE) {
            if (resultCode == PinCodeActivity.RESULT_VERIFIED) {

                String pinCode = intent.getStringExtra(PinCodeActivity.EXTRA_PIN_CODE);
                String address = intent.getStringExtra(PinCodeActivity.EXTRA_ADDRESS);
                String amount = intent.getStringExtra(PinCodeActivity.EXTRA_AMOUNT);

                pinCodeEvent(pinCode, address, amount);
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
                    setAmountFromClipboard();
                } else {
                    setAddressFromClipboard();
                }
                return true;
            case R.id.action_scan:
                ((BaseActivity) getActivity()).launchScanner();
                return true;
            default:
                break;
        }

        return false;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.view_send_request, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        exchangeObservable = bindFragment(this, dbManager.exchangeQuery());
        walletBalanceObservable = bindFragment(this, dbManager.walletQuery());
        
        updateWalletBalanceObservable = bindFragment(this, dataService.getWalletBalance());
        updateExchangeObservable = bindFragment(this, dataService.getExchange());
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);

        swipeLayout.setOnRefreshListener(this);
        swipeLayout.setColorSchemeColors(getResources().getColor(R.color.red));

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
        SpinnerAdapter transactionAdapter = new SpinnerAdapter(getActivity(), R.layout.spinner_layout, stringList);
        setSpinnerAdapter(transactionAdapter);

        setLayout(transactionType);
        setupToolbar();
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
    public void onResume()
    {
        super.onResume();

        onRefreshStart();
        
        subscribeData();

        updateData();
    }
    
    @Override
    public void onPause()
    {
        super.onPause();

        handler.removeCallbacks(refreshRunnable);
        subscriptions.unsubscribe();
        updateSubscriptions.unsubscribe();
    }

    @Override
    public void onDetach()
    {
        ButterKnife.reset(this);
        
        super.onDetach();
    }

    @Override
    public void onRefresh()
    {
        onRefreshStart();
        updateData();
    }

    public void onRefreshStart()
    {
        handler = new Handler();
        handler.postDelayed(refreshRunnable, 1000);
    }

    protected void onRefreshStop()
    {
        handler.removeCallbacks(refreshRunnable);
        swipeLayout.setRefreshing(false);
    }

    private Runnable refreshRunnable = new Runnable()
    {
        @Override
        public void run() {
            swipeLayout.setRefreshing(true);
        }
    };

    private void setupToolbar()
    {
        ((MainActivity) getActivity()).setSupportActionBar(toolbar);

        // Show menu icon
        final ActionBar ab = ((MainActivity) getActivity()).getSupportActionBar();
        ab.setHomeAsUpIndicator(R.drawable.ic_action_navigation_menu);
        ab.setTitle(getString(R.string.view_title_request));
        ab.setDisplayHomeAsUpEnabled(true);
    }

    protected void subscribeData()
    {
        subscriptions = new CompositeSubscription(); 
        
        subscriptions.add(Observable.combineLatest(exchangeObservable, walletBalanceObservable, new Func2<ExchangeItem, WalletItem, WalletData>()
        {
            @Override
            public WalletData call(ExchangeItem exchange, WalletItem wallet)
            {
                WalletData walletData = new WalletData();
                walletData.exchange = exchange;
                walletData.wallet = wallet;
                return walletData;
            }
        }).subscribe(new Action1<WalletData>()
        {
            @Override
            public void call(WalletData results)
            {
                walletData = results;
                setWallet();
            }
        }, new Action1<Throwable>()
        {
            @Override
            public void call(Throwable throwable)
            {
                reportError(throwable);
            }
        }));
    }
    
    private void updateData()
    {
        updateSubscriptions = new CompositeSubscription();
        
        updateSubscriptions.add(Observable.combineLatest(updateExchangeObservable, updateWalletBalanceObservable, new Func2<Exchange, Wallet, WalletUpdateData>()
        {
            @Override
            public WalletUpdateData call(Exchange exchange, Wallet wallet)
            {
                WalletUpdateData updateData = new WalletUpdateData();
                updateData.exchange = exchange;
                updateData.wallet = wallet;
                return updateData;
            }
        }).subscribe(new Action1<WalletUpdateData>()
        {
            @Override
            public void call(WalletUpdateData walletData)
            {
                onRefreshStop();
                updateWallet(walletData.wallet);
                updateExchange(walletData.exchange);
            }
        }, new Action1<Throwable>()
        {
            @Override
            public void call(Throwable throwable)
            {
                onRefreshStop();
                handleError(throwable, true);
            }
        }, new Action0()
        {
            @Override
            public void call()
            {
                onRefreshStop();
            }
        }));
    }
    
    private void updateExchange(Exchange exchange)
    {
        dbManager.updateExchange(exchange);
    }

    private void updateWallet(Wallet wallet)
    {
        dbManager.updateWallet(wallet);
    }

    private void promptForPin(String bitcoinAddress, String bitcoinAmount)
    {
        Intent intent = PinCodeActivity.createStartIntent(getActivity(), bitcoinAddress, bitcoinAmount);
        startActivityForResult(intent, PinCodeActivity.REQUEST_CODE); // be sure to do this from fragment context
    }

    private void showGeneratedQrCodeActivity(String bitcoinAddress, String bitcoinAmount)
    {
        assert bitcoinAddress != null;
        Intent intent = QRCodeActivity.createStartIntent(getActivity(), bitcoinAddress, bitcoinAmount);
        startActivity(intent);
    }
    
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    @SuppressWarnings("deprecation")
    public void setAddressFromClipboard()
    {
        String clipText = getClipboardText();
        if(Strings.isBlank(clipText)) {
            toast(R.string.toast_clipboard_empty);
            return;
        }

        String btcAddress = "";
        if (clipText.toLowerCase().contains("bitcoin:")) {
            btcAddress = WalletUtils.parseBitcoinAddress(clipText);
        } else if (WalletUtils.validBitcoinAddress(clipText)) {
            btcAddress = clipText;
        }

        String btcAmount = "";
        if(WalletUtils.validAmount(btcAmount)) {
            btcAmount = WalletUtils.parseBitcoinAmount(clipText);
        }

        if (!Strings.isBlank(btcAddress)) {
            setBitcoinAddress(btcAddress);
            if(!Strings.isBlank(btcAmount)) {
                setAmount(btcAmount);
            }
        } else if (!Strings.isBlank(btcAmount)) {
            setAmount(btcAmount);
        } else {
            toast(R.string.toast_invalid_address);
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    @SuppressWarnings("deprecation")
    public void setAmountFromClipboard()
    {
        String clipText = getClipboardText();
        if(Strings.isBlank(clipText)) {
            toast(R.string.toast_clipboard_empty);
            return;
        }

        if(WalletUtils.validAmount(clipText)) {
            setAmount(WalletUtils.parseBitcoinAmount(clipText));
        } else {
            toast(R.string.toast_invalid_amount);
        }
    }

    private String getClipboardText()
    {
        String clipText = "";
        ClipboardManager clipboardManager = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            ClipData clip = clipboardManager.getPrimaryClip();
            if(clip != null) {
                ClipData.Item item = clip.getItemAt(0);
                if(item.getText() != null)
                    clipText = item.getText().toString();
            }
        } else {
            clipText = clipboardManager.getText().toString();
        }

        return  clipText;
    }
    
    public void pinCodeEvent(String pinCode, String address, String amount)
    {
        ((BaseActivity) getActivity()).showProgressDialog(new ProgressDialogEvent("Sending bitcoin..."));

        sendPinCodeMoneyObservable = bindFragment(this, dataService.sendPinCodeMoney(pinCode, address, amount));
        sendPinCodeMoneyObservable.subscribe(new Action1<Boolean>()
        {
            @Override
            public void call(Boolean aBoolean)
            {
                ((BaseActivity) getActivity()).hideProgressDialog();

                subscribeData();

                resetWallet();

                toast(R.string.toast_transaction_success);
            }
        }, new Action1<Throwable>()
        {
            @Override
            public void call(Throwable throwable)
            {
                ((BaseActivity) getActivity()).hideProgressDialog();

                handleError(throwable);
            }
        });
    }
    
    public void setBitcoinAddress(String bitcoinAddress)
    {
        if(!Strings.isBlank(bitcoinAddress)) {
            addressText.setText(bitcoinAddress);
        }
    }

    public void setAmount(String bitcoinAmount)
    {
        if(!Strings.isBlank(bitcoinAmount)) {
            amountText.setText(bitcoinAmount);
            calculateCurrencyAmount(bitcoinAmount);
        }
    }

    public void resetWallet()
    {
        amountText.setText("");
        addressText.setText("");
        calculateCurrencyAmount("0.00");
    }
    
    public void setWallet()
    {
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
        if(walletData == null) return;
        
        WalletItem wallet = walletData.wallet;
        ExchangeItem exchange = walletData.exchange;
        
        boolean cancel = false;

        if(Strings.isBlank(amountText.getText())) {
            toast(getString(R.string.error_missing_address_amount));
            return;
        }

        String bitcoinAmount = Conversions.formatBitcoinAmount(amountText.getText().toString(), Conversions.MAXIMUM_BTC_DECIMALS, Conversions.MINIMUM_BTC_DECIMALS);
     
        if(transactionType == WalletTransactionType.SEND) {

            String bitcoinAddress = addressText.getText().toString();
            
            if(Strings.isBlank(addressText.getText())) {
                toast(getString(R.string.error_missing_address_amount));
                return;
            }

            if(bitcoinAmount == null) {
                toast(getString(R.string.toast_invalid_btc_amount));
                cancel = true;
            } else if (!WalletUtils.validAmount(bitcoinAmount)) {
                toast(getString(R.string.toast_invalid_btc_amount));
                cancel = true;
            } else if (!WalletUtils.validBitcoinAddress(bitcoinAddress)) {
                toast(getString(R.string.toast_invalid_address));
                cancel = true;
            }

            if (!cancel) { // There was an error
                //String usd = Calculations.computedValueOfBitcoin(exchange.ask(), exchange.bid(), bitcoinAmount);
                promptForPin(bitcoinAddress, bitcoinAmount);
            }
        } else { // we are requesting money
            
            showGeneratedQrCodeActivity(wallet.address(), bitcoinAmount);
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
        if(walletData == null) return;
        WalletItem wallet = walletData.wallet;
        ExchangeItem exchange = walletData.exchange;
        
        double balanceAmount = Conversions.convertToDouble(wallet.balance());
        String btcBalance = Conversions.formatBitcoinAmount(balanceAmount - btcAmount);
        
        String value = WalletUtils.getBitcoinValue(exchange, wallet);

        if(balanceAmount < btcAmount) {
            balance.setText(Html.fromHtml(getString(R.string.form_balance_negative, btcBalance, value)));
        } else {
            balance.setText(Html.fromHtml(getString(R.string.form_balance_positive, btcBalance, value)));
        }
    }

    private void calculateBitcoinAmount(String usd)
    {
        if(walletData == null) return;
        //WalletItem wallet = walletData.wallet;
        ExchangeItem exchange = walletData.exchange;
        
        if(Doubles.convertToDouble(usd) == 0) {
            computeBalance(0);
            amountText.setText("");
            return;
        }
        
        String exchangeValue = WalletUtils.getExchangeValue(exchange);

        double btc = Math.abs(Doubles.convertToDouble(usd) / Doubles.convertToDouble(exchangeValue));
        String amount = Conversions.formatBitcoinAmount(btc);
        amountText.setText(amount);

        computeBalance(Doubles.convertToDouble(amount));
    }

    private void calculateCurrencyAmount(String bitcoin)
    {
        if(walletData == null) return;
        ExchangeItem exchange = walletData.exchange;
        
        if( Doubles.convertToDouble(bitcoin) == 0) {
            usdEditText.setText("");
            computeBalance(0);
            return;
        }

        computeBalance(Doubles.convertToDouble(bitcoin));
        String value = Calculations.computedValueOfBitcoin(exchange.ask(), exchange.bid(), bitcoin);
        usdEditText.setText(value);
    }
}