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

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
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
import android.widget.TextView;

import com.thanksmister.bitcoin.localtrader.BaseActivity;
import com.thanksmister.bitcoin.localtrader.BaseFragment;
import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.data.api.model.Exchange;
import com.thanksmister.bitcoin.localtrader.data.api.model.Wallet;
import com.thanksmister.bitcoin.localtrader.data.api.model.WalletData;
import com.thanksmister.bitcoin.localtrader.data.database.DbManager;
import com.thanksmister.bitcoin.localtrader.data.database.ExchangeItem;
import com.thanksmister.bitcoin.localtrader.data.database.WalletItem;
import com.thanksmister.bitcoin.localtrader.data.services.DataService;
import com.thanksmister.bitcoin.localtrader.data.services.ExchangeService;
import com.thanksmister.bitcoin.localtrader.events.ProgressDialogEvent;
import com.thanksmister.bitcoin.localtrader.utils.Calculations;
import com.thanksmister.bitcoin.localtrader.utils.Conversions;
import com.thanksmister.bitcoin.localtrader.utils.Doubles;
import com.thanksmister.bitcoin.localtrader.utils.Strings;
import com.thanksmister.bitcoin.localtrader.utils.WalletUtils;

import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func2;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

public class SendFragment extends BaseFragment implements SwipeRefreshLayout.OnRefreshListener
{
    public static final String EXTRA_ADDRESS = "com.thanksmister.extra.EXTRA_ADDRESS";
    public static final String EXTRA_AMOUNT = "com.thanksmister.extra.EXTRA_AMOUNT";
    public static final String EXTRA_WALLET_DATA = "com.thanksmister.extra.EXTRA_WALLET_DATA";
    
    @Inject
    DataService dataService;

    @Inject
    ExchangeService exchangeService;
    
    @Inject
    DbManager dbManager;

    @InjectView(R.id.swipeLayout)
    SwipeRefreshLayout swipeLayout;

    @InjectView(R.id.toolbar)
    Toolbar toolbar;
  
    @InjectView(R.id.amountText)
    TextView amountText;

    @InjectView(R.id.fiatEditText)
    TextView fiatEditText;

    @InjectView(R.id.balanceText)
    TextView balance;
    
    @InjectView(R.id.balanceTitle)
    TextView balanceTitle;

    @InjectView(R.id.address)
    TextView addressText;
    
    @InjectView(R.id.currencyText)
    TextView currencyText;
    
    @OnClick(R.id.sendButton)
    public void sendButtonClicked()
    {
        validateForm();
    }

    private String address;
    private String amount;
    private WalletData walletData;
   
    CompositeSubscription dataSubscriptions = new CompositeSubscription();
    CompositeSubscription updateSubscriptions = new CompositeSubscription();
    

    private Handler handler;

    public static SendFragment newInstance(String address, String amount)
    {
        SendFragment fragment = new SendFragment();
        Bundle args = new Bundle();
        args.putString(EXTRA_ADDRESS, address);
        args.putString(EXTRA_AMOUNT, amount);
        
        fragment.setArguments(args);
        return fragment;
    }

    public static SendFragment newInstance()
    {
        return new SendFragment();
    }

    public SendFragment()
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
            
        } else if (savedInstanceState != null){
            
            if(savedInstanceState.containsKey(EXTRA_ADDRESS))
                address = savedInstanceState.getString(EXTRA_ADDRESS);

            if(savedInstanceState.containsKey(EXTRA_AMOUNT))
                amount = savedInstanceState.getString(EXTRA_AMOUNT);
            
            if(savedInstanceState.containsKey(EXTRA_WALLET_DATA))
                walletData = savedInstanceState.getParcelable(EXTRA_WALLET_DATA);
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
        inflater.inflate(R.menu.send, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
            case R.id.action_paste:
                setAddressFromClipboard();
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
        return inflater.inflate(R.layout.view_send, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);
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

        fiatEditText.addTextChangedListener(new TextWatcher()
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

                if (fiatEditText.hasFocus()) {
                    String amount = editable.toString();
                    calculateBitcoinAmount(amount);
                }
            }
        });
        
        if(!Strings.isBlank(amount)) {
            amountText.setText(amount);
        }

        if(!Strings.isBlank(address)) {
            addressText.setText(address);
        }

        setCurrency();
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

        if(walletData != null)
            outState.putParcelable(EXTRA_WALLET_DATA, walletData);
    }
    
    @Override
    public void onResume()
    {
        super.onResume();

        subscribeData();

        setCurrency();
    }
    
    @Override
    public void onPause()
    {
        super.onPause();

        handler.removeCallbacks(refreshRunnable);
        dataSubscriptions.unsubscribe();
        updateSubscriptions.unsubscribe();
    }

    @Override
    public void onDetach()
    {
        super.onDetach();

        ButterKnife.reset(this);
        
        //http://stackoverflow.com/questions/15207305/getting-the-error-java-lang-illegalstateexception-activity-has-been-destroyed
        try {
            Field childFragmentManager = Fragment.class.getDeclaredField("mChildFragmentManager");
            childFragmentManager.setAccessible(true);
            childFragmentManager.set(this, null);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
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
        ab.setTitle(getString(R.string.view_title_send));
        ab.setDisplayHomeAsUpEnabled(true);
    }
    
    private void setCurrency()
    {
        String currency = exchangeService.getExchangeCurrency();
        
        if(currencyText != null)
            currencyText.setText(currency);
    }

    protected void subscribeData()
    {
        // this must be set each time
        dataSubscriptions = new CompositeSubscription();
        dataSubscriptions.add(Observable.combineLatest(dbManager.exchangeQuery(), dbManager.walletQuery(), new Func2<ExchangeItem, WalletItem, WalletData>()
        {
            @Override
            public WalletData call(ExchangeItem exchange, WalletItem wallet)
            {
                WalletData walletData = null;
                if (exchange != null && wallet != null) {
                    walletData = new WalletData();
                    walletData.setAddress(wallet.address());
                    walletData.setBalance(wallet.balance());
                    walletData.setRate(Calculations.calculateAverageBidAskFormatted(exchange.ask(), exchange.bid()));
                }

                return walletData;
            }
        })
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<WalletData>()
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
        updateSubscriptions.add(dataService.getWalletBalance()
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Wallet>()
                {
                    @Override
                    public void call(Wallet wallet)
                    {
                        onRefreshStop();
                        dbManager.updateWallet(wallet);
                    }

                }, new Action1<Throwable>()
                {
                    @Override
                    public void call(Throwable throwable)
                    {
                        onRefreshStop();
                        handleError(throwable, true);
                    }
                }));

        updateSubscriptions.add(exchangeService.getMarket(true)
                .timeout(10, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Exchange>()
                {
                    @Override
                    public void call(Exchange exchange)
                    {
                        dbManager.updateExchange(exchange);
                    }
                }, new Action1<Throwable>()
                {
                    @Override
                    public void call(Throwable throwable)
                    {
                        snackError("Unable to update exchange data...");
                    }
                }));
    }
    
    private void promptForPin(String bitcoinAddress, String bitcoinAmount)
    {
        Intent intent = PinCodeActivity.createStartIntent(getActivity(), bitcoinAddress, bitcoinAmount);
        startActivityForResult(intent, PinCodeActivity.REQUEST_CODE); // be sure to do this from fragment context
    }
    
    public void setAddressFromClipboard()
    {
        String clipText = getClipboardText();
        if(Strings.isBlank(clipText)) {
            toast(R.string.toast_clipboard_empty);
            return;
        }

        String bitcoinAddress = WalletUtils.parseBitcoinAddress(clipText);
        String bitcoinAmount = WalletUtils.parseBitcoinAmount(clipText);

        if (!WalletUtils.validBitcoinAddress(bitcoinAddress)) {
            toast(getString(R.string.toast_invalid_address));
            return;
        }
        
        if(bitcoinAmount != null && !WalletUtils.validAmount(bitcoinAmount)) {
            toast(getString(R.string.toast_invalid_btc_amount));
            bitcoinAmount = null; // set it to null
        }
       
        if (!Strings.isBlank(bitcoinAddress)) {
            setBitcoinAddress(bitcoinAddress);
            
            if(!Strings.isBlank(bitcoinAmount)) {
                setAmount(bitcoinAmount);
            }
            
        } else if (!Strings.isBlank(bitcoinAmount)) {
            setAmount(bitcoinAmount);
        } 
    }
    
    private String getClipboardText()
    {
        String clipText = "";
        ClipboardManager clipboardManager = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
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
        showProgressDialog(new ProgressDialogEvent("Sending bitcoin..."));

        Observable<Boolean> sendPinCodeMoneyObservable = dataService.sendPinCodeMoney(pinCode, address, amount);
        sendPinCodeMoneyObservable
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Boolean>()
                {
                    @Override
                    public void call(Boolean aBoolean)
                    {
                        hideProgressDialog();
                        resetWallet();
                        toast(R.string.toast_transaction_success);
                    }
                }, new Action1<Throwable>()
                {
                    @Override
                    public void call(Throwable throwable)
                    {
                        hideProgressDialog();
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
        updateData(); // refresh wallet data
        amountText.setText("");
        addressText.setText("");
        calculateCurrencyAmount("0.00");
    }
    
    public void setWallet()
    {
        computeBalance(0);
        setCurrency(); // update currency if there were any changes

        if(Strings.isBlank(amountText.getText())) {
            calculateCurrencyAmount("0.00");
        } else {
            calculateCurrencyAmount(amountText.getText().toString());
        }
    }

    protected void validateForm()
    {
        boolean cancel = false;

        if(Strings.isBlank(amountText.getText())) {
            toast(getString(R.string.error_missing_address_amount));
            return;
        }

        String bitcoinAmount = Conversions.formatBitcoinAmount(amountText.getText().toString(), Conversions.MAXIMUM_BTC_DECIMALS, Conversions.MINIMUM_BTC_DECIMALS);
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
    }
    
    protected void computeBalance(double btcAmount)
    {
        if(walletData == null) return;

        double balanceAmount = Conversions.convertToDouble(walletData.getBalance());
        String btcBalance = Conversions.formatBitcoinAmount(balanceAmount - btcAmount);
        String value = Calculations.computedValueOfBitcoin(walletData.getRate(), walletData.getBalance());
        String currency = exchangeService.getExchangeCurrency();
        if(balanceAmount < btcAmount) {
            balance.setText(Html.fromHtml(getString(R.string.form_balance_negative, btcBalance, value, currency)));
        } else {
            balance.setText(Html.fromHtml(getString(R.string.form_balance_positive, btcBalance, value, currency)));
        }

        balanceTitle.setText(getString(R.string.form_balance_label));
    }

    private void calculateBitcoinAmount(String fiat)
    {
        if(walletData == null) return;
     
        if(Doubles.convertToDouble(fiat) == 0) {
            computeBalance(0);
            amountText.setText("");
            return;
        }
        
        String exchangeValue = walletData.getRate();

        double btc = Math.abs(Doubles.convertToDouble(fiat) / Doubles.convertToDouble(exchangeValue));
        String amount = Conversions.formatBitcoinAmount(btc);
        amountText.setText(amount);

        computeBalance(Doubles.convertToDouble(amount));
    }

    private void calculateCurrencyAmount(String bitcoin)
    {
        if(walletData == null) return;
      
        if( Doubles.convertToDouble(bitcoin) == 0) {
            fiatEditText.setText("");
            computeBalance(0);
            return;
        }

        computeBalance(Doubles.convertToDouble(bitcoin));
        String value = Calculations.computedValueOfBitcoin(walletData.getRate(), bitcoin);
        fiatEditText.setText(value);
    }
}